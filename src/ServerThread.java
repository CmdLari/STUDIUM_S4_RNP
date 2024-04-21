import java.io.*;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;


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
        int actual_Length = 0;
        char[] cbuf = null;
        String line;

        try (InputStream inputStream = socket.getInputStream();
             BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
             OutputStream outputToClient = socket.getOutputStream();
             //Streams made to be used for reading and writing
             Writer writer = new OutputStreamWriter(outputToClient);
             PrintWriter out = new PrintWriter(writer)) {


            while (true) {
                // After ShutdownCommand, were some commands received in last 30 sec? if not - disconnect this server/client.

                if (!runtime) {
                    break;
                }
                try {

                    actual_Length = inputStream.available(); // Checks if stream from Server ist available and how many bytes are ready to be read

                    if (actual_Length > 0) {
                        if (actual_Length <= Server.MAX_LENGTH) {
                            cbuf = new char[actual_Length];

                        } else {
                            System.err.println("Answer exceeds character limitation!");
                            continue; // Starts the next while-loop-iteration
                        }
                        bufferedReader.read(cbuf, 0, actual_Length);
                        line = new String(cbuf);
                        processMsg(line, out);


                        // is the Shutdowntimer already set? if so, reset timer.
                        if (!(shutdownTimer == null)) {
                            initializeShutdownTimer();
                        }
                        actual_Length=0;
                    }




//                    /*  Get message from Client  */
//                    while (actual_Length == 0) {
//                        // After ShutdownCommand, were some commands received in last 30 sec? if not - disconnect this server/client.
//                        if (!runtime) {
//                            break;
//                        }
//                    }
                    //line = bufferedReader.readLine();




                } catch (IOException ioException) {
                    System.err.println(ioException.getMessage());
                    return;
                }
            }

        } catch (IOException ioException) {
            System.err.println(ioException.getMessage());
        }

        System.out.printf("Serverthread %d meldet sich ab\n",id);

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

    /**
     * Resets the Shutdown-Timer after each command that was received.
     */
    private void resetTimer() {
        if (!(shutdownTimer == null)) {
            shutdownTimer.cancel();
            shutdownTimer.schedule(shutdownTask, SHUTDOWN_DELAY);
        }
    }


    private void processMsg(String line, PrintWriter out) throws IOException {

        String response = "ERROR UNKNOWN COMMAND";

        // Handle BYE Command
        if ((line == null) || line.equalsIgnoreCase("BYE")) {
            System.out.println("Client hat BYE gesendet");
            out.printf("%s", "OK BYE");
            out.flush();
            Server.clientClosed();
            //socket.close();
            return;
        }

        String[] tokens = line.trim().split(" ");

        // Handle SHUTDOWN Command
        if (tokens[0].equals("SHUTDOWN")) {
            //Check Password
            if(tokens[1].equals(PASSWORD)){
                //Password Accepted
                System.out.printf("client %2d has send SHUTDOWN-command\n",id);
                out.printf("%s", "OK BYE");
                out.flush();
                Server.clientClosed();
                serverShutDown();
                //socket.close();
                return;
            }else{
                out.printf("%s","ERROR Password incorrect");
                out.flush();
            }
        }

        // Handle all String-Based Commands
        switch (tokens[0]) {
            case "LOWERCASE": response = "OK "+ tokens[1].toLowerCase(); break;
            case "UPPERCASE": response = "OK "+ tokens[1].toUpperCase();break;
            case "REVERSE": response = "OK "+ new StringBuilder(tokens[1]).reverse(); break;
            default:
                response = "ERROR UNKNOWN COMMAND";
        }


        out.printf("%s", response);
        out.flush();
        System.out.printf("Antwort an Client ist: %s\n", response);
    }

    private String checkPermissionShutdown(String input) {

        if(input.equals(PASSWORD)){
            serverShutDown();
            return "OK BYE";
        }else{
            return "ERROR Password incorrect";
        }
    }


}