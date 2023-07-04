import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable{

    private ServerSocket server;
    private boolean done;
    private ExecutorService pool;
    private ArrayList<ConnectionHandler> connections;

    public Server() {
        connections = new ArrayList<>();
        done = false;
    }

    @Override
    public void run() {
        try {
            server = new ServerSocket(9999);
            pool = Executors.newCachedThreadPool();
            while (!done) {
            Socket client = server.accept();
            ConnectionHandler handler = new ConnectionHandler(client);
            connections.add(handler);
            pool.execute(handler);
            }
        } catch (Exception e) {
            showdown();
        }
    }

    public void broadcast(String message) {
        for (ConnectionHandler ch : connections) {
            if (ch != null)
                ch.sendMessage(message);
        }
    }

    public void showdown() {
        try {
            done = true;
            if (!server.isClosed()) {
                server.close();
            }
            for (ConnectionHandler ch : connections) {
                ch.showdownUser();
            }
        } catch (IOException e) {
            // ignore
        }
    }

    class ConnectionHandler implements Runnable{

        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String nikeName;

        public ConnectionHandler(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            try{
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
                out.println("Please enter a nickname: ");
                nikeName = in.readLine();
                System.out.println(nikeName + " connected!");
                broadcast(nikeName + " joined the chat!");
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("/nick ")) {
                        String[] messageSplit = message.split(" ", 2);
                        if (messageSplit.length == 2) {
                            broadcast(nikeName + " renamed themselves to " + messageSplit[1]);
                            System.out.println(nikeName + " renamed themselves to " + messageSplit[1]);
                            nikeName = messageSplit[1];
                            out.println("Successfully changed nickname to " + nikeName);
                        } else {
                            out.println("No nickname provide!");
                        }
                    } else if (message.startsWith("/quit")) {
                        broadcast(nikeName + " left the chat!");
                        showdown();
                    } else {
                        broadcast(nikeName + ": " + message);
                    }
                }
            } catch (IOException e) {
                showdown();
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        public void showdownUser() {
            try {
                in.close();
                out.close();
                if (!client.isClosed()) {
                    client.close();
                }
            } catch (IOException e) {
                // ignore
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }
}
