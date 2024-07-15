package com.walit.lifeServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class ServerDriver {

    private final ThreadPoolExecutor threadPool;
    private final ArrayList<ClientHandler> connections;
    private boolean KEEP_ALIVE;

    public static void main(String[] args) {
        Scanner s = new Scanner(System.in);
        ServerDriver sD = null;
        System.out.println("How many people do you expect to connect to this server? (Enter a number OR enter \"no\" if you are unsure):");
        String expectedConnections = s.nextLine();
        if (expectedConnections.equalsIgnoreCase("no")) {
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
        assert(sD != null);
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
                checkConcurrentUsers(connections.size());
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

    private void checkConcurrentUsers(int userCount) {
        if (userCount >= 1000) {
            // TODO: Send me a text or email so that I know that I need to host this on a better server
        }
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

    private String getUniqueSignature(String ipAddress, String name) {
        try {
            long currentTimeMillis = Instant.now().toEpochMilli();
            String uniqueIdentifier = name + ":" + ipAddress + ":" + currentTimeMillis;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(uniqueIdentifier.getBytes());
            StringBuilder builder = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    builder.append("0");
                }
                builder.append(hex);
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException noAlg) {
            System.err.println(ServerMessage.FailedSignatureGeneration.getMessage());
        }
        return "Failed";
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
        private final int[][] neighbors = new int[][] {
                {0, 1},
                {0, -1},
                {1, -1},
                {1, 0},
                {1, 1},
                {-1, -1},
                {-1, 0},
                {-1, 1}
        };
        private boolean newFrameIsDifferent = true;
        private final Map<Integer, Integer> speedToFPS = new HashMap<>();

        private boolean signatureSet = false;
        private String connectionSignature;

        public ClientHandler(Socket connection) {
            this.connection = connection;
            speedToFPS.put(1, 1);
            speedToFPS.put(2, 3);
            speedToFPS.put(3, 5);
            speedToFPS.put(4, 7);
            speedToFPS.put(5, 10);
        }

        private boolean isValidSpeed(int passedSpeed) {
            return passedSpeed < 6 && passedSpeed > 0;
        }

        private boolean isValidSize(int x, int y) {
            return x > 5 && x <= 1000 && y < 750 && y > 3;
        }

        private int startSim() {
            messageClient(ServerMessage.SimulationStarted.getMessage());
            int[][] previousState;
            int[][] nextState;
            int tick = convertSpeedToTickRate(SPEED);
            outbound.println(connectionSignature + serializeFrame(startPosition));
            try {
                TimeUnit.SECONDS.sleep(3);
            } catch (InterruptedException iE) {
                Thread.currentThread().interrupt();
            }
            previousState = startPosition;
            nextState = generateNextFrame(previousState);
            while (newFrameIsDifferent) {
                try {
                    TimeUnit.MILLISECONDS.sleep(tick);
                } catch (InterruptedException iE) {
                    Thread.currentThread().interrupt();
                }
                outbound.println(connectionSignature + serializeFrame(nextState));
                previousState = nextState;
                nextState = generateNextFrame(previousState);
            }
            outbound.println(connectionSignature + " KILL");
            return 1;
        }

        private int convertSpeedToTickRate(int speed) {
            return 1000 / speedToFPS.get(speed);
        }

        private int countNeighbors(int[][] frame, int x, int y) {
            int count = 0;
            for (int[] neighbor : neighbors) {
                int row = x + neighbor[0];
                int col = y + neighbor[1];
                if (row < 0 || row >= frame.length) {
                    continue;
                }
                if (col < 0 || col >= frame[0].length) {
                    continue;
                }
                if (frame[row][col] == 1) {
                    count++;
                }
            }
            return count;
        }

        private int[][] generateNextFrame(int[][] lastFrame) {
            newFrameIsDifferent = true;
            int count = 0;
            /*
            Rules of Conway's Game of Life
            1. Any live cell with fewer than two live neighbors dies
            2. Any live cell with two or three live neighbors lives
            3. Any live cell with more than three live neighbors dies
            4. Any dead cell with exactly three live neighbors becomes alive
             */
            int[][] result = new int[lastFrame.length][lastFrame[0].length];
            for (int i = 0; i < lastFrame.length; i++) {
                for (int j = 0; j < lastFrame[i].length; j++) {
                    int neighbors = countNeighbors(lastFrame, j, i);
                    if (lastFrame[i][j] == 1 && neighbors > 3) {
                        result[i][j] = 0;
                    } else if ((lastFrame[i][j] == 1 && neighbors >= 2) || (lastFrame[i][j] == 0 && neighbors == 3)) {
                        result[i][j] = 1;
                    } else {
                        result[i][j] = 0;
                    }
                    if (result[i][j] == lastFrame[i][j]) {
                        count++;
                    }
                }
            }
            if (count == lastFrame.length * lastFrame[0].length) {
                newFrameIsDifferent = false;
            }
            return result;
        }

        private String serializeFrame(int[][] frame) {
            StringBuilder builder = new StringBuilder();
            for (int[] row : frame) {
                builder.append(Arrays.toString(row).replaceAll("[\\[\\]\\s]", "")).append(";");
            }
            return builder.toString();
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
                String ipAddress = connection.getRemoteSocketAddress().toString().split(":")[0].substring(1);
                System.out.println("    | " + ipAddress + " has connected.");
                while (name == null || name.length() > 20 || name.length() < 2 && !name.startsWith("&")) {
                    outbound.println("Enter your name: ");
                    name = inbound.readLine();
                }
                System.out.printf("%s registered the name %s.%n", ipAddress, name);
                messageAllClients(name + " has joined the server.\n");

                // Generate signature and send to client
                connectionSignature = getUniqueSignature(ipAddress, name);
                if (connectionSignature.equals("Failed")) {
                    messageClient("Error generating unique signature, try reconnecting if you want to use the simulation functionality.");
                }
                messageClient("Send the following command to validate your connection:\n&set server signature " + connectionSignature + "\n");
                String chat;
                while ((chat = inbound.readLine()) != null) {
                    if (chat.toLowerCase().startsWith("&")) {
                        if (chat.toLowerCase().startsWith("&quit")) {
                            messageAllClients(name + " left the server.");
                            System.out.println("[*] " + name + ServerMessage.ServerClientDC.getMessage());
                            shutdown();
                        } else if (chat.contains(connectionSignature)) {
                            signatureSet = true;
                            messageClient(ServerMessage.SignatureSet.getMessage());
                        } else if (chat.toLowerCase().startsWith("&help")) {
                            messageClient(getHelpText());
                        } else if (chat.toLowerCase().startsWith("&set speed")) {
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
                            messageClient("Tick speed has been set.");
                        } else if (chat.toLowerCase().startsWith("&get speed")) {
                            if (isValidSpeed(SPEED)) {
                                messageClient("Current set speed is: " + SPEED);
                            } else {
                                messageClient(ServerMessage.UnsetValue.getMessage());
                            }
                        } else if (chat.toLowerCase().startsWith("&set size")) {
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
                        } else if (chat.toLowerCase().startsWith("&get size")) {
                            if (isValidSize(xSize, ySize)) {
                                messageClient(String.format("Current size:\nx -> %d\ny -> %d\n", xSize, ySize));
                            } else {
                                messageClient(ServerMessage.UnsetValue.getMessage());
                            }
                        } else if (chat.toLowerCase().startsWith("&set init pos")) {
                            if (!isValidSize(xSize, ySize)) {
                                messageClient("There is not a valid grid size set. Use '&set size x y' to change this.");
                                continue;
                            }
                            String message = String.format("%s:SIZE:%d:%d", connectionSignature, xSize, ySize);
                            messageClient(message);
                            String initialRes = inbound.readLine();
                            startPosition = deserializeStartBoard(initialRes);
                            messageClient("Initial position of simulation has been set.");
                        } else if (chat.toLowerCase().startsWith("&start sim")) { // TODO
                            messageClient("Checking configuration...");
                            if (!signatureSet) {
                                messageClient(ServerMessage.SignatureNotSet.getMessage());
                                continue;
                            }
                            if (isValidSpeed(SPEED) && isValidSize(xSize, ySize)) {
                                if (startPosition == null) {
                                    messageClient("You did not set a starting position. It will be randomly generated for you.");
                                    startPosition = getRandomlyGeneratedPosition();
                                }
                                if (startSim() == 1) {
                                    messageClient(ServerMessage.SimulationFinished.getMessage());
                                } else {
                                    messageClient(ServerMessage.SimulationFailure.getMessage());
                                }
                            } else {
                                messageClient(ServerMessage.SimulationInvalid.getMessage());
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
                System.err.println(ServerMessage.NullException.getMessage());
                System.err.println(nPE.getMessage());
                shutdown();
            }
        }

        private int[][] deserializeStartBoard(String data) {
            int[][] board = new int[ySize][xSize];
            String[] rows = data.split(";");
            for (int i = 0; i < ySize; i++) {
                String[] elements = rows[i].split(",");
                for (int j = 0; j < xSize; j++) {
                    board[i][j] = Integer.parseInt(elements[j]);
                }
            }
            return board;
        }

        private String getHelpText() {
            StringBuilder helpTxt = new StringBuilder();
            helpTxt.append("#######################################################################################");
            helpTxt.append("\nHow to use commands: \n\n");
            helpTxt.append("&quit            ->    Enter to exit the chat.\n\n");
            helpTxt.append("&name NEW_NAME   ->    Enter to change name (2 name changes allowed).\n\n");
            if (!signatureSet) {
                helpTxt.append("Use this command to verify the signature with the server.\n");
                helpTxt.append(String.format("&set server signature %s\n\n", connectionSignature));
            }
            helpTxt.append("&set size        ->    Set the size of the grid (Used as \"&set size x y\").\n");
            helpTxt.append("&set init pos    ->    Set the initial configuration of the grid by selecting the cells to be considered \"alive\" (Size of grid must already be set).\n");
            helpTxt.append("&set speed       ->    Set the speed (1-5) at which the game cycles (Used as \"&set speed x\").\n\n");
            helpTxt.append("&get speed       ->    Get the current set speed for the tick rate of the simulation.\n");
            helpTxt.append("&get size        ->    Get the current set size for the grid of the simulation.\n");
            helpTxt.append("\n---------------------------------------------------------------------------------------\n\n");
            helpTxt.append("Anytime \"&\" is used at the beginning of a chat it will not be displayed in the chat log.\n");
            helpTxt.append("#######################################################################################");
            return helpTxt.toString();
        }

        private void shutdown() {
            try {
                outbound.close();
                inbound.close();
                if (!connection.isClosed()) {
                    connection.close();
                }
                System.err.println("Closed connection to client.");
            } catch (IOException iE) {
                System.err.println("Error closing resources.");
            }
        }

        private void messageClient(String message) {
            outbound.println(message);
        }
    }
}