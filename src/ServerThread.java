import java.io.*;
import java.net.Socket;


public class ServerThread extends Thread{
    protected Socket socket;

    public ServerThread(Socket clientSocket){
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
                try {
                    line = bufferedReader.readLine();
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

    private static void checkMsg(){}

    private static void processMsg(){}

}