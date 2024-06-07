package Praktika1c2;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Scanner;


public class Client {

    private static final int MAX_LENGTH = 255;
    private static int packageSize = MAX_LENGTH;
    private static final int DELAY = 500; // Verzögerung zwischen dem absenden der einzelnen BytePackte -> damit man merk das wirklich einzelne Pakete durch die Luft fliegen.

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
            System.out.printf("PACKAGESIZE not set - using default: %d\n", MAX_LENGTH);
        } else if (args.length == 3) {
            int size = Integer.parseInt(args[2]);
            if (size > 0 && size < 255) {
                packageSize = size;
            }
        }
        hostname = args[0];
        port = Integer.parseInt(args[1]);

        printInitialPrompt();

        try (Socket s = new Socket(hostname, port);
             //Communication Praktika1c2.Client-Praktika1c2.Server
             OutputStream outputToServer = s.getOutputStream();
             InputStream inputFromServer = s.getInputStream();

             //Streams made to be used for reading and writing
             Writer writer = new OutputStreamWriter(outputToServer, StandardCharsets.UTF_8);

             Reader reader = new InputStreamReader(inputFromServer, StandardCharsets.UTF_8);
             BufferedReader breader = new BufferedReader(reader)

        ) {
            // Mainloop -> printPrompt - readMsg - sendMsg - getResponse - showResponse
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

                /*  Send message to server */
//                pwriter.printf("%s\n", msg);
//                pwriter.flush();


                //byte[] msgCharAry = msg.getBytes();
                char[] msgCharAry = msg.toCharArray();
                char[] currentChunk;

                // Message is shorter than packageSize => send as hole
                if (msgCharAry.length < packageSize) {
//                    outputToServer.write(msgCharAry);
//                    outputToServer.write('\n');
                    writer.write(msgCharAry);
                    writer.flush();
                } else {
                    /* Message is longer then packageSize  */
                    /*    Send packages of defined size    */
                    int sendCounter = 0;
                    // for the hole message, divide it in to chunks
                    while (sendCounter < msgCharAry.length) {
                        //byte[] currentChunk;

                        if ((msgCharAry.length - sendCounter) < packageSize) {
                            /* Last Package, last chunk of Message, maybe shorter then packageSize*/
                            currentChunk = Arrays.copyOfRange(msgCharAry, sendCounter, msgCharAry.length);
                        } else {
                            currentChunk = Arrays.copyOfRange(msgCharAry, sendCounter, sendCounter + packageSize);
                        }

                        // Debugging für Arme ...

                        StringBuilder sb = new StringBuilder();
                        //for (byte b : currentChunk) {
                        for (char b : currentChunk) {
                            sb.append((char) b);
                        }
                        System.err.printf("CurrentChunk ist: %s\n", sb);

                        // Send current chunk of message to server
                        //outputToServer.write(currentChunk);
                        writer.write(currentChunk);
                        writer.flush();

                        // Wait for some time, as the user can notice there are different packages.
                        if (DELAY > 0) {
                            try {
                                Thread.currentThread().sleep(DELAY);
                            } catch (Exception e) {
                                // Bad-Style, as it should be...
                            }
                        }
                        sendCounter += packageSize;
                    }

                    //outputToServer.write('\n'); // Final Part: send \n, therefore the server knows, the message is completed
                    writer.write('\n');
                    writer.flush();

                    // REVERSE AAAAAAAAAAAABBBBBBBBBBBBBBBBCCCCCCCCCCCCCCCCCCCCC
                    // REVERSE AAAAAAAAAAAABBBBBBBBBBBBBBBBCCCCCCCCCCCCCCCCCÄÄÄÄÖÖÖÖÜÜÜÜßßßßäääöööüüü
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

                /* Print server response to console//user */
                System.out.println(response);
                if (response.equals("OK BYE\n")) {
                    System.out.println("GoodBye and see u later");
                    break;
                }
                if (response.equals("OK SHUTDOWN")) {
                    System.out.println("GoodBye and see u tomorrow");
                    break;
                }
            }

        } catch (IOException iox) {
            System.err.println("Praktika1c2.Server is not accepting " +
                    " Clients at this time.");
        }

        System.exit(0);
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



