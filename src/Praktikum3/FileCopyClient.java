package Praktikum3;

/* FileCopyClient.java
 Version 0.1 - Muss erg�nzt werden!!
 Praktikum 3 Rechnernetze BAI4 HAW Hamburg
 Autoren:
 */

// TODO: Sendbuffer (und allPackages?) durch Treemap ersetzen -> Synchronizieren mit https://docs.oracle.com/javase/8/docs/api/java/util/TreeMap.html

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;

public class FileCopyClient extends Thread {

    // -------- Constants
    public final static boolean TEST_OUTPUT_MODE = true;

    public final int SERVER_PORT = 23000;

    public final int UDP_PACKET_SIZE = 1008;

    // -------- Public parms
    public String servername;

    public String sourcePath;

    public String destPath;

    public int windowSize;

    public long serverErrorRate;

    // -------- Variables
    // current default timeout in nanoseconds
    private long timeoutValue = 1000000000L;

    // ... ToDo

    //private long sendBase = 0;
    private Long sendBase = 0L;
    private long nextSeqNum = 0;

    //private Set<FCpacket> sendBuffer = new HashSet<>();
    //private TreeMap<Long, FCpacket> sendBuffer = new TreeMap<>());

    private final SortedMap<Long, FCpacket> sendBuffer = Collections.synchronizedSortedMap(new TreeMap<>());

    DatagramSocket clientSocket;
    public final int CLIENT_PORT = 23400;

    long oldRTT = timeoutValue;
    long oldJitter = 0;
    Map<Long, FCpacket> allPackages; // Synchronisierte Liste aller Pakete
    long totalPackageCount = 0;

    // Hilfsliste für fehlersuche
    List<Long> ackListListenerThread = new ArrayList<>();
    List<Long> ackListackHandler = new ArrayList<>();


    // Constructor
    public FileCopyClient(String serverArg, String sourcePathArg,
                          String destPathArg, String windowSizeArg, String errorRateArg) {
        servername = serverArg;
        sourcePath = sourcePathArg;
        destPath = destPathArg;
        windowSize = Integer.parseInt(windowSizeArg);
        serverErrorRate = Long.parseLong(errorRateArg);

    }

    public void runFileCopyClient() throws SocketException, InterruptedException {

        // DONE:
        // 1. Datei einlesen
        //    1.1 check file exists
        // 2. Make Packages from File
        // Make List Of All Packages
        readFile(sourcePath);


        clientSocket = new DatagramSocket(CLIENT_PORT);


        //SendAllPackages();


        // Wait for ACK
        // Erzeuge Empfänger Thread für ACKs
        ListenerThread ackReceiver = new ListenerThread();
        ackReceiver.start();

        FCpacket controlPkg = makeControlPacket();
        DatagramPacket currentDataGramm;
        synchronized (sendBuffer) {
            sendBuffer.put(nextSeqNum, controlPkg);
            controlPkg.setTimestamp(System.nanoTime());
            startTimer(controlPkg);
        }
        try {
            currentDataGramm = new DatagramPacket(controlPkg.getSeqNumBytesAndData(),
                    controlPkg.getSeqNumBytesAndData().length,
                    InetAddress.getByName(servername),
                    SERVER_PORT);
            new SenderThread(currentDataGramm, this).start();
            // TODO ???- Zeitmessung, zeitstempel setzen wenn das Packet auf reisen geht und timer start
            nextSeqNum++;
        } catch (UnknownHostException uhe) {
            System.err.printf("Pity - fail to connect to given host %s\n%s\n", servername, uhe.getMessage());
        }

        while(!controlPkg.isValidACK()){

            System.out.println("Und wir warten");
            sleep(10);

            //warte bis das controllpakge acknowledg wurde....
        }


        // Für alle Pakete...
        //while (!allPackages.isEmpty()) {
        while (sendBase < totalPackageCount) {

            fillSendBuffer(); // Sende mit extra Threads

            synchronized (sendBuffer){
                if(sendBuffer.size()>=windowSize){
                    sendBuffer.wait();
                }
            }


            //waitACK(); // blocking
            //System.out.printf("FCC: runFileCopyClient() - Loop: Verbleibende Pakete: %d\n", allPackages.size());

            System.err.printf("FCC: runFileCopyClient() - Loop: sendBase is %d for %d Packages\n", sendBase, totalPackageCount);

            System.err.printf("Erhaltene ACKs: %s\n", ackListackHandler.stream().map(Object::toString).collect(Collectors.joining(", ")));


//            for (Map.Entry<Long,FCpacket> entry : allPackages.entrySet()){
//                System.err.printf("\tSeqNummer: %d in Map enthalten, ACK %b\n",entry.getKey(),entry.getValue().isValidACK());
//            }
//            sleep(500);
        }

        // Alles Gesendet und Bestättigt - beende den ackReceiver
        // ackReceiver.interrupt();
        clientSocket.close();

        System.out.printf("\t\t ERFOLG \t Alle Pakete überträgen und bestätigt. Beende Programm ablauf. \n");

    }


    private void waitACK() {
        byte[] rcvByteBuffer = new byte[UDP_PACKET_SIZE];
        DatagramPacket rcvDatagramm = new DatagramPacket(rcvByteBuffer, UDP_PACKET_SIZE);
        // 1. Socket anlegen
        // 2. Verbindung abwarten
        //    weiterer Thread:
        //    2.1 ACK für package x ein lesen
        //    2.2 in der Liste Aller Packages x als validACK setzen....

        FCpacket rcvFCpacket;

        try {
            clientSocket.receive(rcvDatagramm);
            rcvFCpacket = new FCpacket(rcvByteBuffer, UDP_PACKET_SIZE);

            System.err.printf("\t Received ACK for: %d\n", rcvFCpacket.getSeqNum());

            //handleACK(rcvFCpacket);

        } catch (IOException ioe) {
            throw new RuntimeException(String.format("Pity - ListenerSocket can not receive response\n%s\n", ioe.getMessage()));
        }

    }


    /**
     * Behandle den Empfang eines ACKs
     * Prüfe ob das bestättigte Packet im sendbuffer ist, prüfe ob es die/das bisherige Sendbase war,
     * Bereinige den SendBuffer = verschiebe das "Sender window"
     * Entferne die bestätigten packte
     * und lasse den sendbuffer neu befüllen
     * <p>
     * Synchronisierte Methode => Immer nur einer darf diese Methode auf einmal ausführe!
     *
     * @param ackPaket
     */
    private void handleACK(FCpacket ackPaket) {

        long ackSeqNum = ackPaket.getSeqNum();
        ackListackHandler.add(ackSeqNum);

        FCpacket currentPack;

        if (TEST_OUTPUT_MODE) {
            System.err.printf("\tFCC: handleACK(): behandle ACK für %d \n", ackSeqNum);
        }

        // Für das bestätigte Paket prüfe, ob es im window = sendBuffer ist
        //if (ackSeqNum >= sendBase && ackSeqNum < sendBase + windowSize) {
        if (sendBuffer.containsKey(ackSeqNum)) {

            // Hole, das Bestätige packet aus dem sendBuffer
            currentPack = sendBuffer.get(ackSeqNum);

            // Halte den Timer an,
            // TODO - ??? Zeitmessung, Zeitspanne vom Start bis zur ankunft hier messen => Für RRT berechnung ...
            computeTimeoutValue(System.nanoTime() - currentPack.getTimestamp());
            cancelTimer(currentPack);

            // Bestätige den Erfolgreichen Empfang
            currentPack.setValidACK(true);

            // Ist das bestätigte Packet gleich der sendBase?
            if (ackSeqNum == sendBase) {
                if (TEST_OUTPUT_MODE) {
                    System.err.printf("\t\tFCC: handleACK(): SeqNum %d ist sendBase \n", ackSeqNum);
                }
                /*
                Entferne alle Packte aus dem sendBuffer, welche
                1. bestätigt wurden,
                2. deren SeqNum kleiner sind als die gerade bestätigte SeqNum
                */
                /* IntelliSense generierter TreeMap-orientierter Code,==> Besser, FG,2024-06-09   */
                sendBuffer.entrySet().removeIf(bufferItem -> bufferItem.getValue().isValidACK() && bufferItem.getKey() <= ackSeqNum);

                // Hole aus dem sendBuffer die Kleinste SeqNum, die noch nicht bestätigt wurde
                long lowestSeqNum = nextSeqNum; // Für den Fall das der Sendbuffer gerade komplett leer ist, weil alle pakete gesendet und bestätigt wurden.
                if (!sendBuffer.isEmpty()) {
                    // Fall => Des Sendbuffer enthält noch pakete
                    lowestSeqNum = sendBuffer.firstKey();
                }
                sendBase= lowestSeqNum;
                // allPackages.remove(ackSeqNum); // Entferne das Bestätigte packet aus der Liste aller Paket.
            }

        } else {
            // Wirf ACK-Paket einfach weg
            if (TEST_OUTPUT_MODE) {
                System.err.printf("\t\tFCC: handleACK(): Paket %d war nicht im Sendbuffer\n", ackSeqNum);
            }
        }
    }

    /**
     * Befülle den sendBuffer mit einigen Packeten aus der liste aller Packages.
     * Sende die Packet dabei gleich ab,
     * starte die jeweiligen timer.
     */
    private void fillSendBuffer() {

        FCpacket currentPkg;
        DatagramPacket currentDataGramm;

        while (nextSeqNum < sendBase + windowSize && nextSeqNum < totalPackageCount) {
            currentPkg = allPackages.get(nextSeqNum);
            synchronized (sendBuffer) {
                sendBuffer.put(nextSeqNum, currentPkg);
                currentPkg.setTimestamp(System.nanoTime());
                startTimer(currentPkg);
            }
            try {
                currentDataGramm = new DatagramPacket(currentPkg.getSeqNumBytesAndData(),
                        currentPkg.getSeqNumBytesAndData().length,
                        InetAddress.getByName(servername),
                        SERVER_PORT);
                new SenderThread(currentDataGramm, this).start();

                // TODO ???- Zeitmessung, zeitstempel setzen wenn das Packet auf reisen geht und timer start
                nextSeqNum++;

            } catch (UnknownHostException uhe) {
                System.err.printf("Pity - fail to connect to given host %s\n%s\n", servername, uhe.getMessage());
            }
//            catch (IOException ioe) {
//                System.err.printf("Pity - fail to send to given host %s\n%s\n", servername, ioe.getMessage());
//            }
        }
    }

    /**
     * EIn Thread , der parallel zu allen anderen aufgaben, darauf lauscht ob uns der Server eine ACk zrücksendet.
     * Dieser ListenerThread, kümmert sich nicht um den Inhalt des Packets, oder um die SeqNum.
     * er nimmt das packet nur an und leitet es an die handleACK methode weiter.
     * Extra Thread, damit wir kein packet verpassen, das ankommt während der clientThread gerade etwas anderes macht.
     */
    private class ListenerThread extends Thread {
        DatagramPacket rcvDatagramm;

        byte[] rcvByteBuffer = new byte[8];

        @Override
        public void run() {

            // 1. Socket anlegen
            // 2. Verbindung abwarten
            //    weiterer Thread:
            //    2.1 ACK für package x ein lesen
            //    2.2 in der Liste Aller Packages x als validACK setzen....
            rcvByteBuffer = new byte[8];
            rcvDatagramm = new DatagramPacket(rcvByteBuffer, 8);



            FCpacket rcvFCpacket;
            long expSeqNum;
            while (!isInterrupted()) {

                try {
                    System.err.println("ListenerThread: Erwarte ACKs...");
                    clientSocket.receive(rcvDatagramm);


                    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
                    buffer.put(rcvByteBuffer);
                    buffer.flip();//need flip
                    expSeqNum = buffer.getLong();

                    rcvFCpacket = new FCpacket(rcvByteBuffer, rcvDatagramm.getLength());

                    System.err.printf("\tListenerThread: Packet vom Server erhalten (ACK for SeqNum:%d ...\n",expSeqNum);

                    rcvByteBuffer = new byte[8];
                } catch (IOException ioe) {
                    throw new RuntimeException(String.format("Pity - ListenerSocket can not receive response\n%s\n", ioe.getMessage()));
                }

                long ackSeqNum = expSeqNum;//rcvFCpacket.getSeqNum();

                synchronized (ackListListenerThread) {
                    ackListListenerThread.add(ackSeqNum);
                }

                FCpacket currentPack;

                synchronized (sendBuffer) {

                    if (TEST_OUTPUT_MODE) {
                        System.err.printf("\tFCC: handleACK(): behandle ACK für %d \n", ackSeqNum);
                    }

                    // Für das bestätigte Paket prüfe, ob es im window = sendBuffer ist
                    //if (ackSeqNum >= sendBase && ackSeqNum < sendBase + windowSize) {
                    if (sendBuffer.containsKey(ackSeqNum)) {

                        // Hole, das Bestätige packet aus dem sendBuffer
                        currentPack = sendBuffer.get(ackSeqNum);

                        // Halte den Timer an,
                        // TODO - ??? Zeitmessung, Zeitspanne vom Start bis zur ankunft hier messen => Für RRT berechnung ...
                        computeTimeoutValue(System.nanoTime() - currentPack.getTimestamp());
                        cancelTimer(currentPack);

                        // Bestätige den Erfolgreichen Empfang
                        currentPack.setValidACK(true);
                        sendBuffer.remove(ackSeqNum);

                        // Ist das bestätigte Packet gleich der sendBase?
                        if (ackSeqNum == sendBase) {
                            if (TEST_OUTPUT_MODE) {
                                System.err.printf("\t\tFCC: handleACK(): SeqNum %d ist sendBase \n", ackSeqNum);
                            }
                            /*
                            Entferne alle Packte aus dem sendBuffer, welche
                            1. bestätigt wurden,
                            2. deren SeqNum kleiner sind als die gerade bestätigte SeqNum
                            */
                            /* IntelliSense generierter TreeMap-orientierter Code,==> Besser, FG,2024-06-09   */
                            //sendBuffer.entrySet().removeIf(bufferItem -> bufferItem.getValue().isValidACK()  && bufferItem.getKey() <= ackSeqNum);
                            //sendBuffer.entrySet().removeIf(bufferItem -> bufferItem.getValue().isValidACK() );



                            // Hole aus dem sendBuffer die Kleinste SeqNum, die noch nicht bestätigt wurde
//                            long lowestSeqNum = nextSeqNum; // Für den Fall das der Sendbuffer gerade komplett leer ist, weil alle pakete gesendet und bestätigt wurden.
//                            if (!sendBuffer.isEmpty()) {
//                                // Fall => Des Sendbuffer enthält noch pakete
//                                //lowestSeqNum = sendBuffer.firstKey();
//                                lowestSeqNum = sendBuffer.entrySet().stream().filter(x->!x.getValue().isValidACK()).map(Map.Entry::getKey).findFirst().orElse(nextSeqNum);
//                            }

                            synchronized (sendBase){
                                //sendBase=(lowestSeqNum);
                                sendBase++;
                            }

                            // allPackages.remove(ackSeqNum); // Entferne das Bestätigte packet aus der Liste aller Paket.
                        }
                        if (TEST_OUTPUT_MODE) {
                            System.err.printf("\t\tFCC: handleACK(): sendBase ist %d\n", sendBase);
                        }

                    } else {
                        // Wirf ACK-Paket einfach weg
                        if (TEST_OUTPUT_MODE) {
                            System.err.printf("\t\tFCC: handleACK(): Paket %d war nicht im Sendbuffer\n", ackSeqNum);
                        }
                    }

                    sendBuffer.notifyAll();
                }


            }

            System.err.printf("ListenerThread interrupted \n");

        }

    }

// 1. Send Control Package (explicitly or implicitly)
// 2. Wait for Ack
// 3. Loop : for all Packages
// 3.1 SendPackage(pkg || sendBuffer[0] )
// 3.2 Start Timer
// 3.4 Wait for ACK ((OR Timeout))
// 3.4.1 Remove Pkg from SendBuffer <-- new Method?
// 3.4.2 No-ACK : TimeOut: resend (-> Loop Continue???))
// 3.4.2.1 Increase TimeOutCounter
// 3.5 : computeTimeoutValue
// 3.6 CancelTimer()
// 3.7 Put package into buffer and restart loop


// Schreibe Statistik ....


    /**
     * Ließt das Angebene File ein, und macht direkt packet daraus und sammelt alle packete in einer Liste.
     *
     * @param sourcePath
     */
    private void readFile(String sourcePath) {

        TreeMap<Long, FCpacket> packages = new TreeMap<Long, FCpacket>();

        byte[] byteArray = new byte[UDP_PACKET_SIZE - 8];

        try {
            InputStream inputStream = new FileInputStream(sourcePath);
            byte[] currentBuffer = new byte[UDP_PACKET_SIZE - 8];
            long pgkCounter = 1;
            int offset = 0;
            long cursor = 0;
            int actualLength;

            File file = new File(sourcePath);
            long fileLength = file.length();



            while (cursor < fileLength) {
                actualLength = inputStream.read(currentBuffer, offset, UDP_PACKET_SIZE - 8);
                cursor += actualLength;
                FCpacket currentPkg = new FCpacket(pgkCounter, currentBuffer, actualLength);
                packages.put(pgkCounter, currentPkg);
                pgkCounter++;
            }

            allPackages = Collections.synchronizedMap(packages);
            totalPackageCount = packages.size();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Timer Operations
     */
    public void startTimer(FCpacket packet) {
        /* Create, save and start timer for the given FCpacket */
        FC_Timer timer = new FC_Timer(timeoutValue, this, packet.getSeqNum());
        packet.setTimer(timer);
        timer.start();
    }

    public void cancelTimer(FCpacket packet) {
        /* Cancel timer for the given FCpacket */
        testOut("Cancel Timer for packet" + packet.getSeqNum());

        if (packet.getTimer() != null) {
            packet.getTimer().interrupt();
        }
    }

    /**
     * Implementation specific task performed at timeout
     * 1. Recalc the TimeoutValue
     * 2. Resend Package
     */
    public void timeoutTask(long seqNum) {

        FCpacket currentPkg;
        DatagramPacket currentDataGramm;

        synchronized (sendBuffer){
            currentPkg = sendBuffer.get(seqNum);
        }

        timeoutValue = 2 * timeoutValue;

        currentPkg.setTimestamp(System.nanoTime());
        // TODO ???- Zeitmessung, zeitstempel setzen wenn das Packet auf reisen geht und timer start
        startTimer(currentPkg);
        if (TEST_OUTPUT_MODE) {
            System.out.printf("\t FCC: TIME-OUT for SeqNum: %d - resend \n", seqNum);
            System.out.printf("\t FCC: TIME-OUT sendbuffer contains: %s \n", sendBuffer.keySet().stream().map(Object::toString).collect(Collectors.joining(",")));
        }
        try {
            currentDataGramm = new DatagramPacket(currentPkg.getSeqNumBytesAndData(),
                    currentPkg.getSeqNumBytesAndData().length,
                    InetAddress.getByName(servername),
                    SERVER_PORT);
            clientSocket.send(currentDataGramm);

            if (TEST_OUTPUT_MODE) {
                System.out.printf("\tFCC: timeoutTask(): Sende paket %d \n", seqNum);
            }

        } catch (UnknownHostException uhe) {
            System.err.printf("Pity - timeoutTask - fail to connect to given host %s\n%s\n", servername, uhe.getMessage());
        } catch (IOException ioe) {
            System.err.printf("Pity - timeoutTask - fail to send package %d \n%s\n", seqNum, ioe.getMessage());
        }

    }


    /**
     * Computes the current timeout value (in nanoseconds)
     */
    public void computeTimeoutValue(long sampleRTT) {

        double x = 0.25;
        double y = x / 2;

        double expRTT = (1 - y) * oldRTT + y * sampleRTT;

        double jitter = (1 - x) * oldJitter + x * Math.abs(sampleRTT - oldRTT);

        timeoutValue = oldRTT + 4 * oldJitter;

        oldRTT = (long) expRTT;
        oldJitter = (long) jitter;

    }


    /**
     * Return value: FCPacket with (0 destPath;windowSize;errorRate)
     */
    public FCpacket makeControlPacket() {
   /* Create first packet with seq num 0. Return value: FCPacket with
     (0 destPath ; windowSize ; errorRate) */
        String sendString = destPath + ";" + windowSize + ";" + serverErrorRate;
        byte[] sendData = null;
        try {
            sendData = sendString.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return new FCpacket(0L, sendData, sendData.length);
    }

    public void testOut(String out) {
        if (TEST_OUTPUT_MODE) {
            System.err.printf("%,d %s: %s\n", System.nanoTime(), Thread
                    .currentThread().getName(), out);
        }
    }


    /**
     * 1. Hostname oder IP-Adresse des Dateitransfer-Servers
     * 2. Portnummer des Dateitransfer-Servers
     * 3. Quellpfad inkl. Dateiname der zu sendenden Datei auf dem lokalen System
     * 4. Zielpfad inkl. Dateiname der zu empfangenden Datei auf dem Dateitransfer-Server
     * 5. Window-Größe N (> 0, ganzzahlig)
     * 6. Fehlerrate ERROR_RATE (>= 0, ganzzahlig) zur Übergabe an den Dateitransfer-Server
     *
     * @param argv
     * @throws Exception
     */
    public static void main(String argv[]) throws Exception {
        FileCopyClient myClient = new FileCopyClient(
                argv[0], // ServerArg
                argv[1], // sourcePath
                argv[2], // destPath
                argv[3], // windowSize
                argv[4]  // ErrorRate
        );


        myClient.runFileCopyClient();


    }

    private class SenderThread extends Thread {
        DatagramPacket dg;
        public SenderThread(DatagramPacket currentDataGramm, FileCopyClient fc) {
            dg = currentDataGramm;

        }
        @Override
        public void run() {
            try {
                clientSocket.send(dg);
                sleep(100);
            } catch (IOException | InterruptedException io) {
                System.err.printf("Pity - senderThread unable to send pkgt\n%s\n",io.getMessage());
            }
        }
    }
}
