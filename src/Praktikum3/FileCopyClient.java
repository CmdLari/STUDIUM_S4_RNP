package Praktikum3;

/* FileCopyClient.java
 Version 0.1 - Muss erg�nzt werden!!
 Praktikum 3 Rechnernetze BAI4 HAW Hamburg
 Autoren:
 */

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
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

    private Long sendBase = 0L;

    DatagramSocket clientSocket;
    public final int CLIENT_PORT = 23400;

    long oldRTT = timeoutValue;
    long oldJitter = 0;
    Map<Long, FCpacket> allPackages; // Synchronisierte Liste aller Pakete
    long totalPackageCount = 0;

    SendBuffer sb;

    private final AtomicBoolean allSend = new AtomicBoolean(false);

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

        allPackages = readFile(sourcePath);

        sb = new SendBuffer(this);

        totalPackageCount = allPackages.size();

        System.out.printf("\t FCC: totalPackageCount ist : %d\n",totalPackageCount);

        clientSocket = new DatagramSocket(CLIENT_PORT);


        // Wait for ACK
        // Erzeuge Empfänger Thread für ACKs
        ListenerThread ackReceiver = new ListenerThread();
        ackReceiver.start();

        /*Create Initital controllPackage */
        FCpacket controlPkg = makeControlPacket();
        sb.addPaket(controlPkg);
        sendPackage(sb.getPkg(0));

        // sendBase
        // nextSeqNum
        // windowSize

        long nextSeqNum=1;
//        for (nextSeqNum = 1; nextSeqNum <= totalPackageCount; nextSeqNum++) {
//            sb.addPaket(allPackages.get(nextSeqNum));
//            sendPackage(sb.getPkg(nextSeqNum));

        System.out.printf("Schleife startet\n");
        while (sendBase < totalPackageCount ) {

            System.out.printf("Sende einen chunk von: %d Paketen \n",(sendBase+windowSize)-nextSeqNum);

            if(nextSeqNum <= sendBase + windowSize && nextSeqNum < totalPackageCount+1){
                sb.addPaket(allPackages.get(nextSeqNum));
                sendPackage(sb.getPkg(nextSeqNum));
                nextSeqNum++;
            }

            sendBase = sb.getLowestUnsend();
            System.out.printf("\t FCC: SendBase ist: %d \n",sendBase);

        }

        allSend.set(true);

        System.out.printf("Schleife Durchlaufen\n");

        if(sb.missingACK()){
            System.out.printf("Es gibt fehlende ACKs...\n");
        }else{
            System.out.printf("alles wurde ACKed...\n");

        }




        ackReceiver.interrupt();



        //ackReceiver.join();
        //ackReceiver.interrupt();

        System.out.printf("\t ACK-Receiver hat abgeschaltet...\n");

        clientSocket.close();


        System.out.printf("\t\t ERFOLG \t Alle Pakete überträgen und bestätigt. Beende Programm ablauf. \n");

    }


    private void sendPackage(FCpacket fcp) {
        try {
            DatagramPacket currentDataGramm = new DatagramPacket(fcp.getSeqNumBytesAndData(),
                    fcp.getSeqNumBytesAndData().length,
                    InetAddress.getByName(servername),
                    SERVER_PORT);

            clientSocket.send(currentDataGramm);


        } catch (IOException uhe) {
            System.err.printf("Pity - fail to connect to given host %s\n%s\n", servername, uhe.getMessage());
        }
    }


    /**
     *
     */
    private class ListenerThread extends Thread {

        @Override
        public void run() {

                boolean ackMissing = true;
                while (!allSend.get() && ackMissing) {
                    byte[] udpBuffer = new byte[8];
                    DatagramPacket rcvDatagramm = new DatagramPacket(udpBuffer, udpBuffer.length);
                    try {
                        clientSocket.receive(rcvDatagramm);
                        FCpacket rcvPkg = new FCpacket(rcvDatagramm.getData(), rcvDatagramm.getLength());
                        FCpacket ackPkg = sb.getPkg(rcvPkg.getSeqNum());
                        cancelTimer(ackPkg);
                        computeTimeoutValue(System.nanoTime() - ackPkg.getTimestamp());
                        ackPkg.setValidACK(true);
                    } catch (IOException io) {
                        throw new RuntimeException();
                    }
                    ackMissing = sb.missingACK();
                }
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
    private Map<Long, FCpacket> readFile(String sourcePath) {
        TreeMap<Long, FCpacket> packages = new TreeMap<Long, FCpacket>();

        try (InputStream inputStream = new FileInputStream(sourcePath);) {

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

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return packages;
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

        FCpacket currentPkg = sb.getPkg(seqNum);
        timeoutValue = 2 * timeoutValue;

        currentPkg.setTimestamp(System.nanoTime());
        // TODO ???- Zeitmessung, zeitstempel setzen wenn das Packet auf reisen geht und timer start
        startTimer(currentPkg);
        if (TEST_OUTPUT_MODE) {
            System.out.printf("\t FCC: TIME-OUT for SeqNum: %d - resend \n", seqNum);
            System.out.printf("\t FCC: TIME-OUT sendbuffer contains:  \n"
                    //sendBuffer.keySet().stream().map(Object::toString).collect(Collectors.joining(","))
            );
        }
        try {
            DatagramPacket currentDataGramm = new DatagramPacket(currentPkg.getSeqNumBytesAndData(),
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

        //TODO: +8 für die SeqNum????
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


}
