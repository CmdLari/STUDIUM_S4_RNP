import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;
import org.apache.commons.text.StringEscapeUtils;
import syslog.Syslog;
import syslog.SyslogException;


public class ServerThread extends Thread {
    private final int id;
    protected Socket socket;

    /* Variables needed to handle Shutdown */
    private boolean runtime = true;
    private Timer shutdownTimer;
    private TimerTask shutdownTask;
    public static final long SHUTDOWN_DELAY = 30000L; // 30000 ms = 30 sec

    private static final String PASSWORD = "666";


    /**
     * Constructor for a new ServerThread.
     *
     * @param clientSocket establish the connection to client
     * @param id           ID Number for this ServerThread
     */
    public ServerThread(Socket clientSocket, int id) {
        this.id = id;
        this.socket = clientSocket;
    }

    /**
     * ServerThread's Main Methode
     * Opens connection to the Client.
     * Waits for messages and proceeds them.
     * Checks if the Shutdown command ends the loop.
     */
    public void run() {

        // Variables to handles client's messages
        boolean running = true;
        int actual_Length = 0;
        char[] cbuf = null;
        String line;
        try {
            Syslog.open("localhost", "Legion", 0x20);
        } catch (SyslogException e) {
            throw new RuntimeException(e);
        }


        try (InputStream inputStream = socket.getInputStream();
             BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
             OutputStream outputToClient = socket.getOutputStream();
             //Streams made to be used for reading and writing
             Writer writer = new OutputStreamWriter(outputToClient, StandardCharsets.UTF_8);
             PrintWriter out = new PrintWriter(writer)) {


            while (running) {
                // After ShutdownCommand, were some commands received in last 30 sec? if not - disconnect this server/client.

                if (!runtime) {
                    break;
                }
                try {

                    actual_Length = inputStream.available(); // Checks if stream from Server ist available and how many bytes are ready to be read

                    if (actual_Length > 0) {
                        if (actual_Length <= Server.MAX_LENGTH) {
                            cbuf = new char[actual_Length];

                        }
                        else {
                            Syslog.log(1, 7, "Answer exceeds character limitation");
//                           System.err.println("Answer exceeds character limitation!");
                            actual_Length=0;
                            continue; // Starts the next while-loop-iteration
                        }


                        // Ausfiltern von Sonderzeichen und ANSI Command Sequenzen...
                        Charset charset = StandardCharsets.UTF_8;
                        byte[] bytes=new byte[actual_Length];
                        inputStream.read(bytes,0,actual_Length);
                        line = charset.decode(ByteBuffer.wrap(bytes)).toString();

                        running = processMsg(line, out);


                        // is the Shutdowntimer already set? if so, reset timer.
                        if (!(shutdownTimer == null)) {
                            initializeShutdownTimer();
                        }
                    }
                    //inputStream.skip(actual_Length);
                    actual_Length=0;



                } catch (IOException ioException) {
                    System.err.println(ioException.getMessage());
                    return;
                } catch (SyslogException e) {
                    throw new RuntimeException(e);
                }
            }

        } catch (IOException ioException) {
            System.err.println(ioException.getMessage());
        }

        System.out.printf("Client %d timed out.\n",id);


        Server.clientClosed();

    }


    /**
     * Inform the "Boss" Server, that a SHUTDOWN command was received.
     * The Server has to notice all ServerThreads, that a SHUTDOWN command was received.
     */
    private static void serverShutDown() {
        Server.handleShutdownCommand();
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


    private boolean processMsg(String line, PrintWriter out) throws IOException {

        String response = "ERROR UNKNOWN COMMAND";

        // Handle BYE Command
        if (line.trim().equalsIgnoreCase("BYE")) {
            System.out.println("Client sent BYE");
            out.printf("%s", "OK BYE");
            out.flush();
            return false;
        }


        String[] tokens = line.trim().split(" ");


//         UNESCAPINGJAVA
//        for(String s : tokens){
//            String snew = StringEscapeUtils.unescapeJava(s);
//           // System.out.printf("\t %s\n",snew);
//        }

        // NOT UNESCAPING JAVA
//        for(String s : tokens){
//            System.out.printf("\t %s\n",s);
//        }

        // Handle SHUTDOWN Command
        if (tokens[0].equals("SHUTDOWN")) {
            //Check Password

            if (tokens.length == 2) {
                if (tokens[1].equals(PASSWORD)) {
                    //Password Accepted
                    System.out.printf("client %2d has send SHUTDOWN-command\n", id);
                    out.printf("%s", "OK SHUTDOWN");
                    out.flush();
                    serverShutDown();

                    return false;
                } else if (tokens[1] == null) {
                    out.printf("%s", "ERROR No password entered");
                    out.flush();
                    return true;

                } else {
                    out.printf("%s", "ERROR Password incorrect");
                    out.flush();
                    return true;
                }
            } else {
                out.printf("%s", "SHUTDOWN command incorrect [needs cmd + password]");
                out.flush();
                return true;

            }
        }

        // Handle all String-Based Commands
        response = switch (tokens[0]) {
            case "LOWERCASE" -> "OK >> " + tokens[1].toLowerCase();
            case "UPPERCASE" -> "OK >> " + tokens[1].toUpperCase();
            case "REVERSE" -> "OK >> " + new StringBuilder(tokens[1]).reverse();
            default -> "Received: " + tokens[0] + " ERROR UNKNOWN COMMAND";
        };


        // Send response to Client
        out.printf("%s\n", response);
        out.flush();

        //Print clients Request
        System.out.printf("Request from Client %d : ",id);
        for(String s : tokens){
            System.out.printf("%s ",s);
        }
        System.out.println();

        //Print Server Answer
        System.out.printf("Response to Client %d: %s\n",id, response);

        return true;
    }


}