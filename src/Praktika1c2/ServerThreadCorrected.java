package Praktika1c2;


/*
Anforderungen an den ServerThread:

- Verarbeitet einen beliebig langen eingabe Strom
    => eingelesen vom socket in einen buffer mit begrenzter Kapazität
    => damit wird verhindert, dass der Server überfüllt wird.
    => die Auswertung liest aus dem Buffer
- Zerlegt den Eingabestrom bei \n
- Einzelteile werden als "Messages" interpretiert
- Enthält eine Message ein \r ist sie ungültig
    => Rückmeldung ERROR: Msg xyz contains \r
- eine Message darf nur 256 (255 mit 0 und \n) lang sein.
    - ist die message länger wird der rest bis zu nächsten \n verworfen? fehlermeldung?
    -

- der Eingabestrom soll utf-8 codiert sein und verarbeitet werden



 */


//import Praktika1c2.syslog.Syslog;
//import Praktika1c2.syslog.SyslogException;

import java.io.*;
import java.net.Socket;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;

public class ServerThreadCorrected extends Thread {

    public final int INPUT_CAPACITY = 1048;// maximum Input capacity 1048 Byte for InputBuffer, in order to protect the server against overflow
    public final int MAX_MSG_LENGTH = 256; // maximum length for a single Message
    public static final long SHUTDOWN_DELAY = 30000L; // 30000 ms = 30 sec
    private static final String PASSWORD = "666"; // Streng geheimes Passwort um den Server abzuschalten => TODO: move to server
    private final short id;
    protected Socket socket;
    //ByteBuffer msgBuffer; // Buffer for a single Message
    CharBuffer msgBuffer;

    private boolean running = true; /* Variables needed to handle Shutdown, set by BYE-Command */
    private boolean runtime = true; /* Variables needed to handle Shutdown, set by Shutdown-timeOut */
    private Timer shutdownTimer;
    private TimerTask shutdownTask;

    private PrintWriter out;


    public ServerThreadCorrected(Socket clientSocket, short id) {
        this.id = id;
        this.socket = clientSocket;
        //msgBuffer = ByteBuffer.allocate(MAX_MSG_LENGTH);
        msgBuffer = CharBuffer.allocate(MAX_MSG_LENGTH);
    }

    @Override
    public void run() {

        // create Connection to client
        try (InputStream inputStream = socket.getInputStream();
             BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8), INPUT_CAPACITY);
             OutputStream outputToClient = socket.getOutputStream();
             //Streams made to be used for reading and writing
             Writer writer = new OutputStreamWriter(outputToClient, StandardCharsets.UTF_8);
             PrintWriter out = new PrintWriter(writer)) {

            //make OutputWrite to client  available to all methods
            this.out = out;

            int currentCharCode; // the byte-representation of character is interpreted as integer, therefore int = char (in UNICODE)
            boolean msgContains_r = false;
            boolean msgTooLong = false;

            // as long as these server thread is connected to some client.
            while (!socket.isClosed() && running && runtime) {

                // Debugging für Arme...
                //System.out.printf("Bedingungen für dauerhafte schleife:  %b", bufferedReader.ready());


                // As long as there are chars to reade in the buffer,
                // read chars/byte to msgBuffer

                // Debugging für Arme...
                //System.out.printf("Buffer ready? %b", bufferedReader.ready());

                // If there is nothing to read, loop instantly
                if(!bufferedReader.ready()) continue;
                msgBuffer.position(0);
                while ( msgBuffer.position() < MAX_MSG_LENGTH) {

                    //Read current byte/Charakter
                    currentCharCode = bufferedReader.read();

                    if(msgBuffer.position()==MAX_MSG_LENGTH-1 && currentCharCode!=(int)'\n'){
                        msgTooLong=true;
                        break;
                    }

                    // Message has ended, leaf while-loop.
                    if ((char)currentCharCode == '\n'){//Character.codePointOf("\n")) {
                        //debugging für Arme
                        //System.out.printf("\tEnde einer Nachricht \\n gelesen\n");
                        break;
                    }

                    // is backslash r read ?
                    if ((char)currentCharCode == '\r'){//Character.codePointOf("\r")) {
                        msgContains_r = true;
                        msgBuffer.put('\\');
                        msgBuffer.put('r');
                        continue;
                        //debugging für Arme
                        //System.out.printf("\tHabe ein \\r gelesen\n");
                    }

                    //debugging für Arme
                    //System.out.printf("\tHabe ein \"%s\" gelesen\n",(char)currentCharCode);

                    //msgBuffer.put((byte)currentCharCode);
                    msgBuffer.put((char)currentCharCode);
                }

                // Convert msgBuffer to String
                msgBuffer.flip();
                String msg = msgBuffer.toString();

                // Proceed message
                if (msgContains_r) { // Error case - msg contains \r
                    sendErrorMsg(msg);
                } else if(msgTooLong){ // Errorcase - msg is to long
                    sendErrorMsgTooLong();
                }else {// Normal Case
                    running = processMsg(msg);
                }

                // Anyway - reset everything and keep going
                // Delete the old messageBuffer and create a new one. Buffer.Clear doesn't remove content.
                //msgBuffer = ByteBuffer.allocate(MAX_MSG_LENGTH);
                msgBuffer = CharBuffer.allocate(MAX_MSG_LENGTH);
                msgContains_r = false;
                msgTooLong = false;

                // is the Shutdowntimer already set?
                // if so, reset timer, after receiving a message
                if (!(shutdownTimer == null)) {
                    initializeShutdownTimer();
                }

            }

        } catch (IOException ioe) {
            System.out.println("Pity - error occurred during connecting or while reading the message ...");
            System.out.println(ioe.getMessage());
        }

        // unsubscribe this thread from boss-server
//        try {
//            Syslog.log(1, 7, "Praktika1c2.Client %d timed out. " + id);
//        } catch (SyslogException se) {
//            System.out.printf("Pity - Error occurred while logging the un-subscription from server \n %s\n", se.getMessage());
//        } finally {
//            Server.clientClosed();
//        }
        System.out.printf("fac:%d ,lvl:%d, %s\n",1,7,"Praktika1c2.Client " + id + " timed out. ");
        Server.clientClosed();
    }

    /**
     * Send client error message, that current message is too long, and exceeds the 255 byte limitiation.
     */
    private void sendErrorMsgTooLong() {
        out.printf("ERROR STRING TOO LONG\n");
        out.flush();
    }


    /**
     * Send an error Messesage to client.
     *
     * @param msg - inhalt der fehlerhaften nachricht
     */
    private void sendErrorMsg(String msg) {
        out.printf("ERROR \"%s\" contains \\r\n", msgBuffer);
        out.flush();
    }

    /**
     * Processes the msg, provided no \r is included.
     * <p>
     * Every Successful command is acknowledged with OK
     * If interpretation of the message fails, it is acknowledged with ERROR
     *
     * @param msg - Message containing at Least one command word and an optionale argument
     * @return false, falls die anwendung beendet werden soll, andernfalls true.
     *
     */
    private boolean processMsg(String msg){// throws SyslogException {

        // Handle BYE Command
        if (msg.trim().equalsIgnoreCase("BYE")) {
            //Syslog.log(1, 7, "Praktika1c2.Client sent BYE");
            System.out.printf("fac:%d ,lvl:%d, %s\n",1,7,"Praktika1c2.Client sent BYE");
            // System.out.println("Praktika1c2.Client sent BYE");
            out.printf("%s", "OK BYE\n");
            out.flush();
            return false;
        }



        //String[] tokens = msg.trim().split(" ");

        String[] tokens = new String[2];

        int spaceIndex = msg.indexOf(" ");
        if(spaceIndex<=0){
            out.printf("%s", "ERROR UNKNOWN COMMAND\n");
            out.flush();
            //Syslog.log(1, 7, "Praktika1c2.Client sent BYE");
            System.out.printf("fac:%d ,lvl:%d, %s\n",1,7,"ERROR UNKNOWN COMMAND");
            return true;
        }

        tokens[0] = msg.substring(0,spaceIndex);
        tokens[1] = msg.substring(spaceIndex+1);

        // Handle SHUTDOWN Command
        if (tokens[0].equals("SHUTDOWN")) {
//            //Check Password
//            if (tokens.length == 2) {
                if (tokens[1].equals(PASSWORD)) {
                    // Password Accepted
                    //Syslog.log(1, 7, "client %2d has send SHUTDOWN-command\n" + id);
                    System.out.printf("fac:%d ,lvl:%d, %s\n",1,7,"client "+id+" has send SHUTDOWN-command");
                    out.printf("%s", "OK SHUTDOWN\n");
                    out.flush();
                    Server.handleShutdownCommand();
                    return false;
                } else { // Wrong Password
                    out.printf("%s", "ERROR Password incorrect\n");
                    out.flush();
                    //Syslog.log(1, 7, "ERROR Password incorrect");
                    System.out.printf("fac:%d ,lvl:%d, %s\n",1,7,"ERROR Password incorrect");

                    return true;
//                }
//            } else { // No Password given, or to many arguments
//                out.printf("%s", " ERROR SHUTDOWN command incorrect [needs cmd + password]\n");
//                out.flush();
//                //Syslog.log(1, 7, "SHUTDOWN command incorrect [needs cmd + password]");
//                System.out.printf("fac:%d ,lvl:%d, %s\n",1, 7, "ERROR SHUTDOWN command incorrect [needs cmd + password]");
//                return true;
            }
        }

//        String argument = Arrays.stream(tokens).skip(1).collect(Collectors.joining(" ")); ;
//
//        // Handle all String-Based Commands.md
//        String response = switch (tokens[0]) {
//            case "LOWERCASE" -> "OK " + argument.toLowerCase();
//            case "UPPERCASE" -> "OK " + argument.toUpperCase();
//            case "REVERSE" -> "OK " + new StringBuilder(argument).reverse();
//            //default -> "Received: " + tokens[0] + " ERROR UNKNOWN COMMAND";
//            default -> "ERROR UNKNOWN COMMAND";
//        };


        // Handle all String-Based Commands.md
        String response = switch (tokens[0]) {
            case "LOWERCASE" -> "OK " + tokens[1].toLowerCase();
            case "UPPERCASE" -> "OK " + tokens[1].toUpperCase();
            case "REVERSE" -> "OK " + new StringBuilder(tokens[1]).reverse();
            //default -> "Received: " + tokens[0] + " ERROR UNKNOWN COMMAND";
            default -> "ERROR UNKNOWN COMMAND";
        };


        // Send response to Praktika1c2.Client
        out.printf("%s\n", response);
        out.flush();

        //Print clients Request
        //System.out.printf("Request from Praktika1c2.Client %d : ",id);
        //Syslog.log(1, 7, "Request from Praktika1c2.Client %d : " + id);
//        for (String s : tokens) {
//            //Syslog.log(1, 7, s);
//            System.out.printf("fac:%d ,lvl:%d, %s\n",1, 7, s);
//            // System.out.printf("%s ",s);
//        }
        System.out.println();
        System.out.printf("fac:%d ,lvl:%d, %s\n",1,7,"Request from Praktika1c2.Client " + id + tokens[0]);

        //Print Praktika1c2.Server Answer
        //Syslog.log(1, 7, "Response to Praktika1c2.Client" + id + " " + response + "\n");
        System.out.printf("fac:%d ,lvl:%d, %s\n",1, 7, "Response to Praktika1c2.Client " + id + " " + response);
        return true;
    }

    /**
     * Starts the 30-secs Timer after a shutdown command was received.
     */
    public void initializeShutdownTimer() {
        shutdownTask = new TimerTask() {
            public void run() {
                // When the timer has counted to SHUTDOWN_DELAY, execute the following statement.
                runtime = false;
            }
        };
        shutdownTimer = new Timer("ShutdownTimer");

        shutdownTimer.schedule(shutdownTask, SHUTDOWN_DELAY);

    }

}
