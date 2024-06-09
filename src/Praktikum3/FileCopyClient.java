package Praktikum3;

/* FileCopyClient.java
 Version 0.1 - Muss erg�nzt werden!!
 Praktikum 3 Rechnernetze BAI4 HAW Hamburg
 Autoren:
 */

// TODO: Sendbuffer (und allPackages?) durch Treemap ersetzen -> Synchronizieren mit https://docs.oracle.com/javase/8/docs/api/java/util/TreeMap.html

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.stream.Collectors;

public class FileCopyClient extends Thread {

    // -------- Constants
    public final static boolean TEST_OUTPUT_MODE = false;

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
    private long timeoutValue = 100000000L;

    // ... ToDo

    private long sendBase = 1;
    private long nextSeqNum = 1;

    //private Set<FCpacket> sendBuffer = new HashSet<>();
    private TreeMap<Long,FCpacket> sendBuffer = new TreeMap<>();
    DatagramSocket socket;

    Map<Long, FCpacket> allPackages; // Synchronisierte Liste aller Pakete


    // Constructor
    public FileCopyClient(String serverArg, String sourcePathArg,
                          String destPathArg, String windowSizeArg, String errorRateArg) {
        servername = serverArg;
        sourcePath = sourcePathArg;
        destPath = destPathArg;
        windowSize = Integer.parseInt(windowSizeArg);
        serverErrorRate = Long.parseLong(errorRateArg);

    }

    public void runFileCopyClient() throws SocketException {

        // DONE:
        // 1. Datei einlesen
        //    1.1 check file exists
        // 2. Make Packages from File
        // Make List Of All Packages
        readFile(sourcePath);


        socket = new DatagramSocket();


        SendAllPackages();


        // Wait for ACK
        // Erzeuge Empfänger Thread für ACKs
        new ListenerThread(this).start();


        // ToDo!!
        // 3. Fill buffer



        fillSendBuffer();



        // Verbindung herstellen
        // Create UDP Connection and simultaniously sending(+wait) and listening nad let timers run
        //


        // Erzeuge Sender Thread => Send all packages...


        //----> sendAllPackages()

    }

    /**
     * Behandle den Empfang eines ACKs
     * Prüfe ob das bestättigte Packet im sendbuffer ist, prüfe ob es die/das bisherige Sendbase war,
     * Bereinige den SendBuffer = verschiebe das "Sender window"
     * Entferne die bestätigten packte
     * und lasse den sendbuffer neu befüllen
     *
     * Synchronisierte Methode => Immer nur einer darf diese Methode auf einmal ausführe!
     *
     * @param ackPaket
     */
    private synchronized void handleACK(FCpacket ackPaket) {

        long ackSeqNum = ackPaket.getSeqNum();
        FCpacket currentPack;

        // Für das bestätigte Paket prüfe, ob es im window = sendBuffer ist
        //if (ackSeqNum >= sendBase && ackSeqNum < sendBase + windowSize) {
        if (sendBuffer.containsKey(ackSeqNum)) {

            // Hole, das Bestätige packet aus dem sendBuffer
            //currentPack = sendBuffer.stream().filter(fCpacket -> fCpacket.getSeqNum() == ackSeqNum).findFirst().orElseThrow(NoSuchElementException::new);
            currentPack = sendBuffer.get(ackSeqNum);

            // Halte den Timer an,
            // TODO - ??? Zeitmessung, Zeitspanne vom Start bis zur ankunft hier messen => Für RRT berechnung ...
            //currentPack.getTimer().interrupt();
            cancelTimer(currentPack);

            // Bestätige den Erfolgreichen Empfang
            currentPack.setValidACK(true);

            // Ist das bestätigte Packet gleich der sendBase?
            if (ackSeqNum == sendBase) {

                /*
                Entferne alle Packte aus dem sendBuffer, welche
                1. bestätigt wurden,
                2. deren SeqNum kleiner sind als die gerade bestätigte SeqNum
                */

                /*Eigener Set-orientier Code ==> Schlecht, FG,2024-06-09 !*/
//                sendBuffer.removeAll(
//                        sendBuffer.stream().filter(FCpacket::isValidACK)
//                                .filter(p -> p.getSeqNum() <= ackSeqNum)
//                                .collect(Collectors.toSet())
//                );


                /* EIgener TreeMap-orientierter Code,==> Besser, FG,2024-06-09   */
//               while (iter.hasNext()){
//                    Map.Entry<Long,FCpacket> bufferItem = iter.next();
//                    if(bufferItem.getValue().isValidACK() && bufferItem.getKey()<ackSeqNum){
//                        iter.remove();
//                    }
//                }

                /* IntelliSense generierter TreeMap-orientierter Code,==> Besser, FG,2024-06-09   */
                sendBuffer.entrySet().removeIf(bufferItem -> bufferItem.getValue().isValidACK() && bufferItem.getKey() <= ackSeqNum);


                // Hole aus dem sendBuffer die Kleinste SeqNum, die noch nicht bestätigt wurde
                long lowestSeqNum=nextSeqNum; // Für den Fall das der Sendbuffer gerade komplett leer ist, weil alle pakete gesendet und bestätigt wurden.
                if(!sendBuffer.isEmpty()){
                    // Fall => Des Sendbuffer enthält noch pakete
                    lowestSeqNum =sendBuffer.firstKey();
                    // Muss False sein ! => da packet darf noch nicht bestätigt sein.
                    if(sendBuffer.get(lowestSeqNum).isValidACK()){
                        throw new RuntimeException("Logik-Fehler - die Kleinste SeqNum im Sendbuffer ist schon bestättigt");
                    }
                }

                sendBase=lowestSeqNum;

                /*Eigener Set-orientier Code ==> Schlecht, FG,2024-06-09 !*/
//                Optional<FCpacket> fund = sendBuffer.stream().min(Comparator.comparing(FCpacket::getSeqNum));//.orElse(sendBase+windowSize);
//                fund.ifPresent(fCpacket -> sendBase = fCpacket.getSeqNum());




                // Fülle den sendBuffer wieder auf.
                fillSendBuffer();
            }

        } else {
            // Wirf ACK-Paket einfach weg
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

        while (sendBuffer.size() < windowSize) {

            currentPkg = allPackages.get(nextSeqNum);
            try {
                currentDataGramm = new DatagramPacket(currentPkg.getSeqNumBytesAndData(),
                        currentPkg.getLen(),
                        InetAddress.getByName(servername),
                        SERVER_PORT
                );
                socket.send(currentDataGramm);
                //sendBuffer.add(currentPkg);
                sendBuffer.put(nextSeqNum,currentPkg);

                // TODO ???- Zeitmessung, zeitstempel setzen wenn das Packet auf reisen geht und timer start
                startTimer(currentPkg);

                nextSeqNum++;
            } catch (UnknownHostException uhe) {
                System.err.printf("Pity - fail to connect to given host %s\n%s\n", servername, uhe.getMessage());
            } catch (IOException ioe) {
                System.err.printf("Pity - fail to send to given host %s\n%s\n", servername, ioe.getMessage());
            }

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
        DatagramSocket listenerSockert;
        FileCopyClient client;

        byte[] rcvByteBuffer = new byte[UDP_PACKET_SIZE];

        public ListenerThread(FileCopyClient fc) {
            this.client = fc;
        }

        @Override
        public void run() {

            // 1. Socket anlegen
            // 2. Verbindung abwarten
            //    weiterer Thread:
            //    2.1 ACK für package x ein lesen
            //    2.2 in der Liste Aller Packages x als validACK setzen....

            rcvDatagramm = new DatagramPacket(rcvByteBuffer, UDP_PACKET_SIZE);
            FCpacket rcvFCpacket;
            try {
                listenerSockert = new DatagramSocket(SERVER_PORT);
            } catch (SocketException se) {
                throw new RuntimeException(String.format("Pity - ListenerSocket can not created\n%s\n", se.getMessage()));
            } finally {
                listenerSockert.close();
            }

            while (!isInterrupted()) {
                try {
                    listenerSockert.receive(rcvDatagramm);
                    rcvFCpacket = new FCpacket(rcvByteBuffer, UDP_PACKET_SIZE);
                    client.handleACK(rcvFCpacket);
                } catch (IOException ioe) {
                    throw new RuntimeException(String.format("Pity - ListenerSocket can not receive response\n%s\n", ioe.getMessage()));
                } finally {
                    listenerSockert.close();
                }
            }
            listenerSockert.close();


        }

    }


    public void SendAllPackages() throws SocketException {

        //THREADS HERE

        // Stack unsent packages
        Map<Long, FCpacket> unsent = allPackages;

        // Stack sent packages
        Map<Integer, FCpacket> sent = new HashMap<Integer, FCpacket>(allPackages.size());//HashMap.newHashMap(packagesSync.size());

        int sequenceCounter = 0;

        FCpacket sentPack;

        System.out.println(unsent.get(0).toString());

        //      // Socket einmal erstellen, ohne Port und IP -> Das machen die Pakete
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();
        } catch (SocketException se) {
            System.out.printf("Schade - beim erzeugen des Sockets ist was schiefgegangen\n\t%s", se.getMessage());
        }


        /// send to server + wait for ack
        while (!allPackages.isEmpty()) {
            // Sending

            // package to sent
            sentPack = allPackages.get(sequenceCounter);

            // DEBUGGING FÜR ARME
            System.out.printf("PackageNr: %d, PackageLen: %d\n", sequenceCounter, sentPack.getLen());
            // Socket


            try {
                // Port and Host

                DatagramPacket toSend = new DatagramPacket(sentPack.getSeqNumBytesAndData(),
                        sentPack.getSeqNumBytesAndData().length,
                        InetAddress.getByName(servername),
                        SERVER_PORT);

                socket.send(toSend);

                // Timer des Pakets Starten ....


            } catch (SocketException e) {
                System.out.printf("Schade - beim senden ist was schiefgegangen...\n %s\n", e.getMessage());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }


            // update stacks and counter
            sent.put(sequenceCounter, sentPack);
            allPackages.remove(sequenceCounter);
            sequenceCounter++;
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
     * @param sourcePath
     */
    private void readFile(String sourcePath) {

        TreeMap<Long, FCpacket> packages = new TreeMap<Long, FCpacket>();

        byte[] byteArray = new byte[UDP_PACKET_SIZE - 8];

        try {
            InputStream inputStream = new FileInputStream(sourcePath);

            byte[] currentBuffer = new byte[UDP_PACKET_SIZE - 8];

            //int pgkCounter = 1;
            long pgkCounter = 1;

            int offset = 0;
            long cursor = 0;
            int actualLength;

            File file = new File(sourcePath);
            long fileLength = file.length();

            packages.put(0L, makeControlPacket());

//      Debugging für Arme
//      System.out.println(sourcePath);
//      System.out.printf("File length: %d\n", fileLength);


            while (cursor < fileLength) {

                actualLength = inputStream.read(currentBuffer, offset, UDP_PACKET_SIZE - 8);

//      Debugging für Arme
//      System.out.printf("Cursor: %d - Package: %d - Offset: %d - Actual Length: %d\n", cursor, pgkCounter,offset,actualLength);

                cursor += actualLength;
                FCpacket currentPkg = new FCpacket(pgkCounter, currentBuffer, actualLength);
                packages.put( pgkCounter, currentPkg);
                pgkCounter++;

            }

            allPackages = Collections.synchronizedMap(packages);

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
     */
    public void timeoutTask(long seqNum) {
        // ToDo
    }


    /**
     * Computes the current timeout value (in nanoseconds)
     */
    public void computeTimeoutValue(long sampleRTT) {

        // ToDo
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
        return new FCpacket(0, sendData, sendData.length);
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


//    System.out.println(myClient.packagesSync);


    }

}
