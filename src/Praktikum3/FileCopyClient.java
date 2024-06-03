package Praktikum3;

/* FileCopyClient.java
 Version 0.1 - Muss erg�nzt werden!!
 Praktikum 3 Rechnernetze BAI4 HAW Hamburg
 Autoren:
 */

import java.io.*;
import java.util.*;

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

  public List<FCpacket> allPackages ;

  // -------- Variables
  // current default timeout in nanoseconds
  private long timeoutValue = 100000000L;

  // ... ToDo

  Map<Integer, FCpacket> packagesSync;


  // Constructor
  public FileCopyClient(String serverArg, String sourcePathArg,
    String destPathArg, String windowSizeArg, String errorRateArg) {
    servername = serverArg;
    sourcePath = sourcePathArg;
    destPath = destPathArg;
    windowSize = Integer.parseInt(windowSizeArg);
    serverErrorRate = Long.parseLong(errorRateArg);

  }

  public void runFileCopyClient() {

    readFile(sourcePath);

      // ToDo!!
      // 1. Datei einlesen
        // 1.1 check file exists


      // 2. Make Packages from File
      // Make List Of All Packages

      // 3. Fill buffer

      //----> sendAllPackages()

  }


  public void SendAllPackages(){

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

  }


  private void readFile(String sourcePath) {

    TreeMap<Integer, FCpacket> packages = new TreeMap<Integer, FCpacket>();

    byte[] byteArray = new byte[UDP_PACKET_SIZE - 8];

    try {
      InputStream inputStream = new FileInputStream(sourcePath);

      byte[] currentBuffer = new byte[UDP_PACKET_SIZE - 8];

      int pgkCounter = 1;

      int offset = 0;
      long cursor = 0;
      int actualLength;

      File file = new File(sourcePath);
      long fileLength = file.length();

      packages.put(0, makeControlPacket());

//      Debugging für Arme
//      System.out.println(sourcePath);
//      System.out.printf("File length: %d\n", fileLength);


      while (cursor < fileLength) {

        actualLength = inputStream.read(currentBuffer, offset, UDP_PACKET_SIZE - 8);

//      Debugging für Arme
//      System.out.printf("Cursor: %d - Package: %d - Offset: %d - Actual Length: %d\n", cursor, pgkCounter,offset,actualLength);

        cursor += actualLength;
        FCpacket currentPkg = new FCpacket(pgkCounter, currentBuffer, actualLength);
        packages.put(pgkCounter, currentPkg);
        pgkCounter++;

      }

      packagesSync = Collections.synchronizedMap(packages);

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  /**
  *
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
   *
   * Computes the current timeout value (in nanoseconds)
   */
  public void computeTimeoutValue(long sampleRTT) {

  // ToDo
  }


  /**
   *
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
   * @param argv
   * @throws Exception
   */
  public static void main(String argv[]) throws Exception {
    FileCopyClient myClient = new FileCopyClient(argv[0], argv[1], argv[2],
        argv[3], argv[4]);
    myClient.runFileCopyClient();


//    System.out.println(myClient.packagesSync);



  }

}
