package Praktika1c2;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Scanner;


public class Client {

    private static final int MAX_LENGTH = 255;
    private static int packageSize = MAX_LENGTH;
    private static final int DELAY = 100;

    /**
     * Makes a new client and establishes a connection to the server
     *
     * @param args - 1. hostname/IP-Address of the servers, 2. PortNr of the server
     */
    public static void main(String[] args) {
        String hostname = null;
        int port = 8080;

        String msg;
        int actual_Length; // Number of bytes send by server
        char[] cbuf;

        // Read and Set the packageSize to given value
        if (args.length < 2) {
            System.out.println("ENTER HOSTNAME AND PORT!");
            System.exit(-1);
        } else if (args.length < 3) {
            hostname = args[0];
            port = Integer.parseInt(args[1]);
            System.out.printf("PACKAGESIZE not set - using default: %d", MAX_LENGTH);
        } else if (args.length == 3) {

            int size = Integer.parseInt(args[2]);
            if (size > 0 && size < 255) {
                packageSize = size;
            }

        }

        printInitialPrompt();

        try (Socket s = new Socket(hostname, port);
             //Communication Praktika1c2.Client-Praktika1c2.Server
             OutputStream outputToServer = s.getOutputStream();
             InputStream inputFromServer = s.getInputStream();

             //Streams made to be used for reading and writing
             Writer writer = new OutputStreamWriter(outputToServer, StandardCharsets.UTF_8);
             PrintWriter pwriter = new PrintWriter(writer);

             Reader reader = new InputStreamReader(inputFromServer, StandardCharsets.UTF_8);
             BufferedReader breader = new BufferedReader(reader)

        ) {

            // Apply packageSize to Socket
            // Source: https://docs.oracle.com/javase/8/docs/api/java/net/Socket.html#setSendBufferSize-int-
            //s.setSendBufferSize(packageSize);

            // Check actual buffer size
//            int actualBufferSize = s.getSendBufferSize();
//            if (actualBufferSize != packageSize) {
//                System.out.printf("The Current SendBufferSize is: %3d \n", actualBufferSize);
//            }

            while (true) {
                // VARIABLES
                String response = null;
                actual_Length = 0;


                printPrompt();

                msg = readPrompt();
                if (msg == null) {
                    System.err.println("Message exceeds character limitation or is empty!");
                    continue;
                }


                //int expectedPackageCount = msg.getBytes().length / actualBufferSize;


                //System.out.printf("Expected number of packages: %3d - %d\n", expectedPackageCount, expectedPackageCount + 1);

                /*  Send message to server */
//                pwriter.printf("%s\n", msg);
//                pwriter.flush();


                byte[] msgCharAry = msg.getBytes();

                if (msgCharAry.length < packageSize) {
                    outputToServer.write(msgCharAry);
                    outputToServer.write('\n');
                } else {

                    /* Send packages of defined size */
                    int sendCounter = 0;
                    while (sendCounter < msgCharAry.length) {
                        byte[] currentChunk;
                        if ((msgCharAry.length - sendCounter) < packageSize) {
                            currentChunk = Arrays.copyOfRange(msgCharAry, sendCounter, msgCharAry.length);
                        } else {
                            currentChunk = Arrays.copyOfRange(msgCharAry, sendCounter, sendCounter + packageSize);
                        }

                        StringBuilder sb = new StringBuilder();
                        for (byte b : currentChunk) {
                            sb.append((char) b);
                        }

                        System.err.printf("CurrentChunck ist: %s\n", sb);

                        outputToServer.write(currentChunk);

                        if (DELAY > 0) {
                            try {
                                Thread.currentThread().sleep(1000);
                            } catch (Exception e) {

                            }
                        }
                        sendCounter += packageSize;
                    }

                    outputToServer.write('\n');

                    // REVERSE AAAAAAAAAAAABBBBBBBBBBBBBBBBCCCCCCCCCCCCCCCCCCCCC
                }


                /*  Get message//response from Praktika1c2.Server  */

                while (actual_Length == 0) {
                    actual_Length = inputFromServer.available(); // Checks if stream from Praktika1c2.Server ist available and how many bytes are ready to be read

                    if (actual_Length <= MAX_LENGTH) {
                        cbuf = new char[actual_Length];
                    } else {
                        System.err.println("Answer exceeds character limitation!");
                        continue; // Starts the next while-loop-iteration
                    }

                    breader.read(cbuf, 0, actual_Length);
                    response = new String(cbuf);
                }

                /* Print serverresponse to Console//User*/
                System.out.println(response);
                if (response.equals("OK BYE")) {
                    break;
                }
                if (response.equals("OK SHUTDOWN")) {
                    break;
                }
            }

        } catch (IOException iox) {
            System.err.println("Praktika1c2.Server is not accepting more Clients at this time.");
        }
    }


    /**
     * Prints a simple Greeting and explanation of usage to console.
     */
    private static void printInitialPrompt() {
        System.out.println("Welcome to the greatest Client!");
        System.out.println("Usage: ");
        System.out.println("\t LOWERCASE <string>  - Transform string into lower case");
        System.out.println("\t UPPERCASE <string>  - Capitalise string");
        System.out.println("\t REVERSE <string>    - Reverse string (cba -> abc");
        System.out.println("\t BYE                 - Quit session");
        System.out.println("\t SHUTDOWN <password> - End session and shut down server");

        System.out.println();
        System.out.println("Made by: LP , FG");
    }

    /**
     *
     */
    static void printPrompt() {
        System.out.print("Client >> ");
    }

    /**
     * Reads the users command and arguments from Console and returns as string.
     * Could be followed by some checking...
     *
     * @return user's Command and Arguments
     */
    private static String readPrompt() {
        Scanner scanner = new Scanner(System.in);
        String msg = scanner.nextLine();
        return (msg.length() <= MAX_LENGTH) ? msg : null;
    }

}



