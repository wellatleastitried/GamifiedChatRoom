package com.walit.lifeServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class ServerDriver {
    // Create server socket that accepts connections
    // Create fixed thread pool that allocates new thread for each client
    private final ThreadPoolExecutor threadPool;
    private final ArrayList<ClientHandler> connections;
    private boolean KEEP_ALIVE;


    public static void main(String[] args) {
        ServerDriver sD = new ServerDriver(1);
        if (sD.runServer() == 0) {
            System.out.println("Server has successfully terminated.");
        } else {
            System.err.println("Server had an error.");
        }
    }
    public ServerDriver() {
        threadPool = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        connections = new ArrayList<>();
        KEEP_ALIVE = true;
    }
    public ServerDriver(int expectedConnections) {
        int SIZE;
        SIZE = expectedConnections < 1 ? 1 : Math.min(expectedConnections, 20);
        threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(SIZE);
        connections = new ArrayList<>();
        KEEP_ALIVE = true;
    }
    private int runServer() {
        try (ServerSocket server = new ServerSocket(4444)) {
            System.out.println(ServerMessage.ServerCreated.getMessage());
            System.out.println(ServerMessage.ServerWait.getMessage());
            while (KEEP_ALIVE) {
                // Accept connections and allocate a thread from the thread pool to them
                Socket clientSocket = server.accept();
                System.out.println(ServerMessage.ServerClientConnect.getMessage());
                ClientHandler client = new ClientHandler(clientSocket);
                connections.add(client);
                threadPool.execute(client);
            }
            messageClients(ServerMessage.ServerShutdown.getMessage());
            System.out.println(ServerMessage.ServerShutdown.getMessage());
            shutdown();
        } catch (IOException iE) {
            System.err.println("1" + ServerMessage.ServerError.getMessage() + iE.getMessage());
            KEEP_ALIVE = false;
            return 1;
        }
        return 0;
    }
    private void shutdown() {
        KEEP_ALIVE = false;
        threadPool.shutdown();
        for (ClientHandler c : connections) {
            c.shutdown();
        }
    }
    private void messageClients(String message) {
        for (ClientHandler c : connections) {
            if (c != null) {
                c.messageClient(message);
            }
        }
    }
    class ClientHandler implements Runnable {

        private final Socket connection;
        private PrintWriter outbound;
        private BufferedReader inbound;
        private String name;

        public ClientHandler(Socket connection) {
            this.connection = connection;
        }

        @Override
        public void run() {
            try {
                outbound = new PrintWriter(connection.getOutputStream(),true);
                inbound = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                System.out.println("    | " + connection.getRemoteSocketAddress().toString() + " has connected.");
                while (name == null) {
                    outbound.print("Enter your name: ");
                    name = inbound.readLine();
                }
                System.out.println();
                messageClients(name + " has joined the server.");
                String chat;
                while ((chat = inbound.readLine()) != null) {
                    if (!chat.isEmpty()) {
                        if (chat.toLowerCase().startsWith("&")) {
                            if (chat.toLowerCase().startsWith("&quit")) {
                                messageClients(name + " left the server.");
                                System.out.println("[*] " + name + ServerMessage.ServerClientDC.getMessage());
                                shutdown();
                            }
                        } else {
                            messageClients(ServerMessage.getSyntax(name) + chat);
                        }
                    }
                }
            } catch (IOException iE) {
                System.err.println("2" + ServerMessage.ServerError.getMessage() + iE.getMessage());
                shutdown();
            }
        }
        public void shutdown() {
            try {
                outbound.close();
                inbound.close();
            } catch (IOException iE) {
                System.err.println("Error closing resources.");
            }
        }
        public void messageClient(String message) {
            outbound.println(message);
        }
        private void handleName() {
            try {
                while (name == null) {
                    outbound.print("Enter your name: ");
                    String temp = inbound.readLine();
                    if (!(temp.trim() == null || temp.trim().isEmpty())) {
                        name = temp;
                        break;
                    }
                }
            } catch (IOException iE) {
                System.err.println(ServerMessage.ServerError.getMessage() + iE.getMessage());
            }
        }
    }
}