package Praktikum3;

/* FileCopyClient.java
 Version 0.1 - Muss erg�nzt werden!!
 Praktikum 3 Rechnernetze BAI4 HAW Hamburg
 Autoren:
 */

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

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

    SendBuffer sendBuffer;

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

        sendBuffer = new SendBuffer(this);

        totalPackageCount = allPackages.size();

        System.out.printf("\t FCC: totalPackageCount ist : %d\n",totalPackageCount);

        clientSocket = new DatagramSocket(CLIENT_PORT);


        // Wait for ACK
        // Erzeuge Empfänger Thread für ACKs
        ListenerThread ackReceiver = new ListenerThread();
        ackReceiver.start();

        /*Create Initital controllPackage */
        FCpacket controlPkg = makeControlPacket();
        sendBuffer.addPaket(controlPkg);
        sendPackage(sendBuffer.getPkg(0));

        while (!controlPkg.isValidACK()){
            System.out.printf("Und wir warten...\n");
            Thread.sleep(20);
        }

        long nextSeqNum=1;

        System.out.printf("Schleife startet\n");

        while (sendBase < totalPackageCount ) {

            if(nextSeqNum <= sendBase + windowSize && nextSeqNum < totalPackageCount+1){
                sendBuffer.addPaket(allPackages.get(nextSeqNum));
                sendPackage(sendBuffer.getPkg(nextSeqNum));
                nextSeqNum++;
            }

            sendBase = sendBuffer.getLowestUnsend();

        }

        allSend.set(true);

        System.out.printf("Schleife Durchlaufen\n");

        ackReceiver.join();


        if(sendBuffer.missingACK()){
            System.out.printf("Es gibt fehlende ACKs...\n");
        }else{
            System.out.printf("alles wurde ACKed...\n");

        }

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
                while (!allSend.get() || ackMissing) {
                    byte[] udpBuffer = new byte[8];
                    DatagramPacket rcvDatagramm = new DatagramPacket(udpBuffer, udpBuffer.length);
                    try {
                        clientSocket.receive(rcvDatagramm);
                        FCpacket rcvPkg = new FCpacket(rcvDatagramm.getData(), rcvDatagramm.getLength());
                        FCpacket ackPkg = sendBuffer.getPkg(rcvPkg.getSeqNum());
                        cancelTimer(ackPkg);
                        computeTimeoutValue(System.nanoTime() - ackPkg.getTimestamp());
                        ackPkg.setValidACK(true);
                    } catch (IOException io) {
                        throw new RuntimeException();
                    }
                    ackMissing = sendBuffer.missingACK();
                }
            }

    }

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

        FCpacket currentPkg = sendBuffer.getPkg(seqNum);
        cancelTimer(currentPkg);

        Long currentTimestammp = currentPkg.getTimestamp();
        long currentTime = System.nanoTime();

        long delay = currentTime-currentTimestammp;

        FC_Timer newtimer = new FC_Timer(2*delay, this, currentPkg.getSeqNum());

        currentPkg.setTimestamp(System.nanoTime());
        currentPkg.setTimer(newtimer);

        startTimer(currentPkg);

        try {
            DatagramPacket currentDataGramm = new DatagramPacket(currentPkg.getSeqNumBytesAndData(),
                    currentPkg.getSeqNumBytesAndData().length,
                    InetAddress.getByName(servername),
                    SERVER_PORT);
            clientSocket.send(currentDataGramm);

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
