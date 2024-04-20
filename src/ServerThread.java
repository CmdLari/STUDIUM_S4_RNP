import java.io.*;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;


public class ServerThread extends Thread{
    private int id;
    protected Socket socket;

    private boolean runtime = true; // Needed to Hanlde Shutdown
    private Timer shutdownTimer ;
    private TimerTask shutdownTask;
    public static final long SHUTDOWN_DELAY = 30000L; // 30000 ms = 30 sec

    public ServerThread(Socket clientSocket, int id){
        this.id = id;
        this.socket = clientSocket;
    }

    public void run(){

        try(InputStream inputStream = socket.getInputStream();
            BufferedReader bufferedReader= new BufferedReader(new InputStreamReader(inputStream));
            OutputStream outputToClient= socket.getOutputStream();
            //Streams made to be used for reading and writing
            Writer writer = new OutputStreamWriter(outputToClient);
            PrintWriter out = new PrintWriter(writer);) {

            String line;
            while (true) {

                // After ShutdownCommand, were some commands received in last 30 sec? if not - disconnet this server/client.
                if(!runtime){break;}

                try {

                    line = bufferedReader.readLine();

                    // is the Shutdowntimer already set? if so, reset timer.
                    if(! (shutdownTimer==null) ) {resetTimer();}


                    if ((line == null) || line.equalsIgnoreCase("BYE")) {
                        System.out.printf("Client hat BYE gesendet\n");
                        Server.clientClosed();
                        // TO-DO

                        socket.close();
                        return;
                    } else {
                        out.printf("%s",line);
                        out.flush();
                        System.out.printf("Antwort an Client ist: %s\n",line);
                    }
                } catch (IOException ioException) {
                    System.err.println(ioException.getMessage());
                    return;
                }
            }

        } catch (IOException ioException) {
            System.err.println(ioException.getMessage());

        }

    }


    /**
     * Inform the "Boss" Server, that a SHUTDOWN command was received.
     * The Server has to notice all ServerThreads, that a SHUTDOWN command was received.
     */
    private static void serverShutDown(){
       Server.handleShutdownCommand();
    }

    /**
     * Starts the 30-secs Timer after a shutdown command was received.
     */
    public void initializeShutdownTimer(){
        shutdownTask = new TimerTask() {
            public void run() {
                // When the timer has counted to SHUTDOWN_DELAY, execute the following statement.
                runtime=false;
            }
        };
        shutdownTimer = new Timer("ShutdownTimer");

        shutdownTimer.schedule(shutdownTask, SHUTDOWN_DELAY);

    }

    /**
     * Resets the Shutdown-Timer after each command that was received.
     */
    private void resetTimer(){
        if(!(shutdownTimer==null)){
            shutdownTimer.schedule(shutdownTask, SHUTDOWN_DELAY);
        }
    }


    private static void checkMsg(){}

    private static void processMsg(){}




}