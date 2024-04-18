import java.io.*;
import java.net.Socket;


public class ServerThread {
    protected Socket socket;

    public ServerThread(Socket clientSocket){
        this.socket = clientSocket;
    }

    public void run(){

        try(InputStream inputStream = socket.getInputStream();
            BufferedReader bufferedReader= new BufferedReader(new InputStreamReader(inputStream));
            DataOutputStream out= new DataOutputStream(socket.getOutputStream())) {

            String line;
            while (true) {
                try {
                    line = bufferedReader.readLine();
                    if ((line == null) || line.equalsIgnoreCase("BYE")) {
                        Server.clientClosed();
                        // TO-DO

                        socket.close();
                        return;
                    } else {
                        out.writeBytes(line + "\n\r");
                        out.flush();
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

    private static void checkMsg(){}

    private static void processMsg(){}

}