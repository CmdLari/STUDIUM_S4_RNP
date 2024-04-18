import java.io.*;
import java.net.Socket;
import java.util.Scanner;


public class Client {

    private static final int MAX_LENGTH = 255;

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

        printInitialPrompt();

        try(Socket s = new Socket(hostname,port);
            //Communication Client-Server
            OutputStream outputToServer = s.getOutputStream();
            InputStream inputFromServer = s.getInputStream();

            //Streams made to be used for reading and writing
            Writer writer = new OutputStreamWriter(outputToServer);
            PrintWriter pwriter = new PrintWriter(writer);

            Reader reader = new InputStreamReader(inputFromServer);
            BufferedReader breader = new BufferedReader(reader)

        ) {
            //TODO
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
                else if(msg.equals("BYE")){
                    break;
                }

                /*  Send message to server */
                pwriter.printf("%s\n",msg);
                pwriter.flush();

                /*  Get message//response from Server  */

                while(actual_Length==0){
                    actual_Length = inputFromServer.available(); // Checks if stream from Server ist available and how many bytes are ready to be read

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
            }

        }catch(IOException iox){
            System.err.println(iox.getMessage());
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
    static void printPrompt(){
        System.out.print("Client>>");
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



