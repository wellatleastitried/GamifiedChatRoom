package com.walit.lifeServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class ServerDriver {
    // Create server socket that accepts connections
    // Create fixed thread pool that allocates new thread for each client
    private final ThreadPoolExecutor threadPool;
    private final ArrayList<ClientHandler> connections;
    private boolean KEEP_ALIVE;


    public static void main(String[] args) {
        Scanner s = new Scanner(System.in);
        ServerDriver sD = null;
        System.out.println("How many people do you expect to connect to this server? (Enter a number OR enter \"no\" if you are unsure):");
        String expectedConnections = s.nextLine();
        if (expectedConnections.toLowerCase().equals("no")) {
            System.out.println(ServerMessage.ServerCachedThreads.getMessage());
            sD = new ServerDriver();
        } else {
            try {
                int conns = Integer.parseInt(expectedConnections);
                sD = new ServerDriver(conns);
                System.out.println(ServerMessage.ServerFixedThreads.getMessage() + conns);
            } catch (Exception e) {
                System.err.println("Not a valid input. Shutting down.");
            }
        }
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
            messageAllClients(ServerMessage.ServerShutdown.getMessage());
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

    private void messageAllClients(String message) {
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
        private int nameChangesRemaining = 2;

        private int SPEED = -1;
        private int xSize = -1;
        private int ySize = -1;
        private int[][] startPosition;

        public ClientHandler(Socket connection) {
            this.connection = connection;
        }

        private boolean isValidSpeed(int passedSpeed) {
            return passedSpeed < 6 && passedSpeed > 0;
        }

        private boolean isValidSize(int x, int y) {
            return x > 5 && x < 1000 && y < 750 && y > 3;
        }

        private boolean startSim() {
            messageClient(ServerMessage.SimulationSuccess.getMessage());
            return true;
        }

        private int[][] getRandomlyGeneratedPosition() {
            int[][] position = new int[ySize][xSize];
            for (int i = 0; i < position.length; i++) {
                for (int j = 0; j < position[i].length; j++) {
                    position[i][j] = getRandomDigit();
                }
            }
            return position;
        }

        private int getRandomDigit() {
            return new SecureRandom().nextInt(2);
        }

        @Override
        public void run() {
            try {
                outbound = new PrintWriter(connection.getOutputStream(), true);
                inbound = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String tempName = connection.getRemoteSocketAddress().toString().split(":")[0].substring(1);
                System.out.println("    | " + tempName + " has connected.");
                while (name == null || name.length() > 20 || name.length() < 2 && !name.startsWith("&")) {
                    outbound.println("Enter your name: ");
                    name = inbound.readLine();
                }
                System.out.printf("%s registered the name %s.%n", tempName, name);
                messageAllClients(name + " has joined the server.");
                String chat;
                while ((chat = inbound.readLine()) != null) {
                    if (chat.toLowerCase().startsWith("&")) {
                        if (chat.toLowerCase().startsWith("&quit")) {
                            messageAllClients(name + " left the server.");
                            System.out.println("[*] " + name + ServerMessage.ServerClientDC.getMessage());
                            shutdown();
                        } else if (chat.toLowerCase().startsWith("&help")) {
                            messageClient(getHelpText());
                        } else if (chat.toLowerCase().startsWith("&set speed")) { // TODO: Make get speed
                            messageClient("set speed called.");
                            String[] speedCall = chat.split(" ");
                            if (speedCall.length != 3) {
                                messageClient(ServerMessage.InvalidCommand.getMessage());
                                continue;
                            }
                            try {
                                int tempSpeed = Integer.parseInt(speedCall[2]);
                                if (!isValidSpeed(tempSpeed)) {
                                    messageClient(ServerMessage.InvalidCommand.getMessage());
                                    continue;
                                }
                                SPEED = tempSpeed;
                            } catch (NumberFormatException nFE) {
                                messageClient(ServerMessage.InvalidCommand.getMessage());
                            }
                        } else if (chat.toLowerCase().startsWith("&set size")) { // TODO: Make get size
                            String[] sizeValues = chat.split(" ");
                            if (sizeValues.length != 4) {
                                messageClient(ServerMessage.InvalidCommand.getMessage());
                                continue;
                            }
                            try {
                                int x = Integer.parseInt(sizeValues[2]);
                                int y = Integer.parseInt(sizeValues[3]);
                                if (!isValidSize(x, y)) {
                                    messageClient(ServerMessage.InvalidCommand.getMessage());
                                    continue;
                                }
                                xSize = x;
                                ySize = y;
                                messageClient(String.format("Size of grid set to: %dx%d.", xSize, ySize));
                            } catch (NumberFormatException nFE) {
                                messageClient(ServerMessage.InvalidCommand.getMessage());
                            }
                        } else if (chat.toLowerCase().startsWith("&set init pos")) {
                            messageClient("set init pos called.");
                            // Bring up GUI for client (size must already be given) where they can click on cells they want to be "alive"
                        } else if (chat.toLowerCase().startsWith("&start sim")) {
                            messageClient("Checking configuration...");
                            if (isValidSpeed(SPEED) && isValidSize(xSize, ySize)) {
                                if (startPosition == null) {
                                    startPosition = getRandomlyGeneratedPosition();
                                }
                                if (!startSim()) {
                                    messageClient(ServerMessage.SimulationFailure.getMessage());
                                }
                            }

                        } else if (chat.toLowerCase().startsWith("&name")) {
                            if (nameChangesRemaining == 0) {
                                messageClient("You have no name changes remaining.");
                                continue;
                            }
                            String[] cmdAndName = chat.split(" ", 2);
                            if (cmdAndName.length != 2) {
                                messageClient("You did not provide a new name. Type \"&help\" to see the help menu.");
                                continue;
                            }
                            if (cmdAndName[1].equals(name)) {
                                messageClient("This name is already in use.");
                                continue;
                            }
                            if (cmdAndName[1].startsWith("&")) {
                                messageClient("Your name cannot start with the command symbol.");
                                continue;
                            }
                            if (cmdAndName[1].length() < 2) {
                                messageClient("This name is not long enough.");
                                continue;
                            }
                            messageAllClients(String.format("%s has changed their name to %s", name, cmdAndName[1]));
                            System.out.printf("%s changed their name to %s.%n", name, cmdAndName[1]);
                            messageClient(String.format("%d name changes remaining.", --nameChangesRemaining));
                            name = cmdAndName[1];
                        } else {
                            messageClient(ServerMessage.InvalidCommand.getMessage());
                        }
                    } else {
                        messageAllClients(ServerMessage.getSyntax(name) + chat);
                    }
                }
            } catch (IOException iE) {
                System.err.println(name + ServerMessage.ServerClientDC.getMessage());
                shutdown();
            } catch (NullPointerException nPE) {
                System.err.println(ServerMessage.WaitingClientError.getMessage());
            }
        }

        public String getHelpText() {
            StringBuilder helpTxt = new StringBuilder();
            helpTxt.append("#######################################################################################");
            helpTxt.append("\nHow to use commands: \n");
            helpTxt.append("&quit            ->    Enter to exit the chat.\n");
            helpTxt.append("&name NEW_NAME   ->    Enter to change name (2 name changes allowed).\n");

            helpTxt.append("&set size        ->    Set the size of the grid (Used as \"&set size x y\").\n");
            helpTxt.append("&set init pos    ->    Set the initial configuration of the grid by selecting the cells to be considered \"alive\" (Size of grid must already be set).\n");
            helpTxt.append("&set speed       ->    Set the speed (1-5) at which the game cycles (Used as \"&set speed x\").\n");

            helpTxt.append("\n---------------------------------------------------------------------------------------\n\n");
            helpTxt.append("Anytime \"&\" is used at the beginning of a chat it will not be displayed in the chat log.\n");
            helpTxt.append("#######################################################################################");
            return helpTxt.toString();
        }

        public void shutdown() {
            try {
                outbound.close();
                inbound.close();
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (IOException iE) {
                System.err.println("Error closing resources.");
            }
        }

        public void messageClient(String message) {
            outbound.println(message);
        }
    }
}