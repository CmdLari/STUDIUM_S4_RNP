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

- der eingabestrom soll utf codiert sein und verarbeitet werden

 */


import Praktika1c2.syslog.Syslog;
import Praktika1c2.syslog.SyslogException;

import java.io.*;
import java.net.Socket;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;

public class ServerThreadCorrected extends Thread {

    public final int INPUT_CAPACITY = 1048;// Maximale Eingabe Kapazität 1048 Byte um den Server vor überfrachtung zu Schützen
    public final int MAX_MSG_LENGHT = 256; // Maximale länge einer nachricht beträgt 256 Byte
    public static final long SHUTDOWN_DELAY = 30000L; // 30000 ms = 30 sec
    private static final String PASSWORD = "666"; // Streng geheimes Passwort um den Server abzuschalten => TODO: move to server
    private final short id;
    protected Socket socket;
    ByteBuffer msgBuffer;

    private boolean running = true; /* Variables needed to handle Shutdown, set by BYE-Command */
    private boolean runtime = true; /* Variables needed to handle Shutdown, set by Shutdown-timeOut */
    private Timer shutdownTimer;
    private TimerTask shutdownTask;

    private PrintWriter out;


    public ServerThreadCorrected(Socket clientSocket, short id) {
        this.id = id;
        this.socket = clientSocket;
    }

    @Override
    public void run() {

        // Verbindbung aufnehemen
        try (InputStream inputStream = socket.getInputStream();
             BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8), INPUT_CAPACITY);
             OutputStream outputToClient = socket.getOutputStream();
             //Streams made to be used for reading and writing
             Writer writer = new OutputStreamWriter(outputToClient, StandardCharsets.UTF_8);
             PrintWriter out = new PrintWriter(writer)) {

            // Outputwrite zum Clienten verfügbar machen...
            this.out = out;

            int currentCharCode;
            boolean msgContains_r = false ;

            // Wenn/solange verbindung besteht ...
            while (!socket.isClosed() && running && runtime) {

                // Wenn es etwas zu lesen gibt...
                // Erstelle eine (neue) Nachricht/Msg
                while (bufferedReader.ready() && msgBuffer.position() < MAX_MSG_LENGHT) {

                    //Lese das Aktuelle Zeichen ...
                    currentCharCode = bufferedReader.read();

                    // Naricht zuende, verlasse die schleife...
                    if (currentCharCode != Character.codePointOf("\n")) {
                        break;
                    }

                    // Wird ein \r gelesen?
                    if (currentCharCode != Character.codePointOf("\r")) {
                        msgContains_r = true;
                    }

                    msgBuffer.put((byte) currentCharCode);
                }

                // Verarbeite die Nachricht
                if (msgContains_r) {
                    // Fehlerfall, msg enthält ein \r
                    sendErrorMsg(msgBuffer);
                } else {

                    String msg = StandardCharsets.UTF_8.decode(msgBuffer).toString();
                    try {
                        running = processMsg(msg);
                    } catch (SyslogException se) {
                        System.out.printf("Schade es hat ein Problem mit Syslog gegeben...\n%s\n", se.getMessage());
                    }
                }

                // Lösche den Alten MsgBuffer und erzeuge einen neuen.
                // buffer.clear() entfernt die alten daten NICHT!
                msgBuffer = ByteBuffer.allocate(MAX_MSG_LENGHT);
                msgContains_r = false;

                // is the Shutdowntimer already set?
                // if so, reset timer, after receiving a message
                if (!(shutdownTimer == null)) {
                    initializeShutdownTimer();
                }

            }

        } catch (IOException ioe) {
            System.out.println("Schade - beim erstellen der Verbindung oder beim lesen der Msg ist etwas schiefgegangen...");
            System.out.println(ioe.getMessage());
        }

        // Melde diesen Thread beim server ab...
        try {
            Syslog.log(1, 7, "Praktika1c2.Client %d timed out. " + id);
        } catch (SyslogException se) {
            System.out.printf("Schade - Beim Loggen des Beenden ist was scheifgelaufen \n %s\n",se.getMessage());
        }finally {
            Server.clientClosed();
        }

    }


    /**
     * Sende Fehlermeldung an Clienten zurrück
     *
     * @param msgBuffer - inhalt der fehlerhaften nachricht
     */
    private void sendErrorMsg(ByteBuffer msgBuffer) {
        out.printf("ERROR \"%s\" contains \\r\n", msgBuffer);
        out.flush();
    }

    /**
     * Verarbeitet die Nachricht sofern sie kein \r enthält....
     *
     * @param msg - die Nachricht ....
     * @return false, falls die anwendung beendet werden soll, andernfalls true.
     * @throws SyslogException
     */
    private boolean processMsg(String msg) throws SyslogException {

        String response = "ERROR UNKNOWN COMMAND";

        // Handle BYE Command
        if (msg.trim().equalsIgnoreCase("BYE")) {
            Syslog.log(1, 7, "Praktika1c2.Client sent BYE");
//            System.out.println("Praktika1c2.Client sent BYE");
            out.printf("%s", "OK BYE");
            out.flush();
            return false;
        }


        String[] tokens = msg.trim().split(" ");

        // Handle SHUTDOWN Command
        if (tokens[0].equals("SHUTDOWN")) {
            //Check Password

            if (tokens.length == 2) {
                if (tokens[1].equals(PASSWORD)) {
                    // Password Accepted
                    // System.out.printf("client %2d has send SHUTDOWN-command\n", id);
                    Syslog.log(1, 7, "client %2d has send SHUTDOWN-command\n" + id);
                    out.printf("%s", "OK SHUTDOWN");
                    out.flush();
                    Server.handleShutdownCommand();

                    return false;
                } else if (tokens[1] == null) {
                    out.printf("%s", "ERROR No password entered");
                    out.flush();
                    Syslog.log(1, 7, "ERROR No password entered");
                    return true;

                } else {
                    out.printf("%s", "ERROR Password incorrect");
                    out.flush();
                    Syslog.log(1, 7, "ERROR Password incorrect");
                    return true;
                }
            } else {
                out.printf("%s", "SHUTDOWN command incorrect [needs cmd + password]");
                out.flush();
                Syslog.log(1, 7, "SHUTDOWN command incorrect [needs cmd + password]");
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


        // Send response to Praktika1c2.Client
        out.printf("%s\n", response);
        out.flush();

        //Print clients Request
        //System.out.printf("Request from Praktika1c2.Client %d : ",id);
        Syslog.log(1, 7, "Request from Praktika1c2.Client %d : " + id);
        for (String s : tokens) {
            Syslog.log(1, 7, s);
            // System.out.printf("%s ",s);
        }
        System.out.println();

        //Print Praktika1c2.Server Answer
        System.out.printf("Response to Praktika1c2.Client %d: %s\n", id, response);
        Syslog.log(1, 7, "Response to Praktika1c2.Client" + id + " " + response + "\n");

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
