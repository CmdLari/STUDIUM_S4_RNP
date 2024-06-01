package Praktika1c2;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;



public class Client {

    private static final int MAX_LENGTH = 255;
    private static int packageSize = MAX_LENGTH;

    /**
     * Makes a new client and establishes a connection to the server
     * @param args - 1. hostname/IP-Address of the servers, 2. PortNr of the server
     */
    public static void main(String[] args) {
        String hostname = args[0];
        int port = Integer.parseInt(args[1]);

        String msg;
        int actual_Length; // Number of bytes send by server
        char[] cbuf;

        // Read and Set the packageSize to given value
        if(args.length==3){

            int size = Integer.parseInt(args[2]);

            if(size>0 && size<255){
                packageSize = size;
            }
        }

        printInitialPrompt();

        try(Socket s = new Socket(hostname,port);
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
            s.setSendBufferSize(packageSize);

            // Check actual buffer size
            int actualBufferSize = s.getSendBufferSize();
            if(actualBufferSize!=packageSize){
                System.out.printf("The Current SendBufferSize is: %3d \n",actualBufferSize);
            }

            while(true){
                // VARIABLES
                String response=null;
                actual_Length=0;


                printPrompt();

                msg = readPrompt();
                if (msg == null){
                    System.err.println("Message exceeds character limitation or is empty!");
                    continue;
                }


                int expectedPackageCount = msg.getBytes().length / actualBufferSize ;


                System.out.printf("Expected number of packages: %3d - %d\n",expectedPackageCount, expectedPackageCount+1);

                /*  Send message to server */
                pwriter.printf("%s\n",msg);
                pwriter.flush();


                // Versuch ByteWeise zu Schreiben , funktioniert nicht, erstes Byte wird geschrieben, aber der rest läuft paket.
//                for(byte b:msg.getBytes()){
//                    outputToServer.write(b);
//                   // outputToServer.flush();
//                }
//                outputToServer.write("\n".getBytes());
//                outputToServer.flush();

                /*  Get message//response from Praktika1c2.Server  */

                while(actual_Length==0){
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
                if(response.equals("OK BYE")){break;}
                if(response.equals("OK SHUTDOWN")){
                    break;
                }
            }

        }catch(IOException iox){
            System.err.println("Praktika1c2.Server is not accepting more Clients at this time.");
        }
    }


    /**
     * Prints a simple Greeting and explanation of usage to console.
     */
    private static void printInitialPrompt() {
        System.out.println("Welcome to the greatest Praktika1c2.Client!");
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
    static void printPrompt(){
        System.out.print("Praktika1c2.Client>>");
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
        return (msg.length()<=MAX_LENGTH) ? msg :  null;
    }

}



