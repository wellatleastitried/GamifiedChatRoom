package com.walit.lifeServer;

import jakarta.mail.internet.*;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.Transport;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import java.util.Properties;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServerDriver {

    private final ThreadPoolExecutor threadPool;
    private final ArrayList<ClientHandler> connections;
    private final Set<String> namesUsed;
    private boolean KEEP_ALIVE;

    public static void main(String[] args) {
        Scanner s = new Scanner(System.in);
        ServerDriver sD = null;
        System.out.println("How many people do you expect to connect to this server? (Enter a number OR enter \"unknown\" if you are unsure):");
        String expectedConnections = s.nextLine();
        if (expectedConnections.equalsIgnoreCase("unknown")) {
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
            System.err.println("Server terminated due to an error.");
        }
        s.close();
    }

    public ServerDriver() {
        threadPool = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        connections = new ArrayList<>();
        namesUsed = new HashSet<>();
        KEEP_ALIVE = true;
    }

    public ServerDriver(int expectedConnections) {
        int SIZE;
        SIZE = expectedConnections < 1 ? 1 : Math.min(expectedConnections, 20);
        threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(SIZE);
        connections = new ArrayList<>();
        namesUsed = new HashSet<>();
        KEEP_ALIVE = true;
    }

    private int runServer() {
        try (ServerSocket server = new ServerSocket(4444)) {
            System.out.println(ServerMessage.ServerCreated.getMessage());
            System.out.println(ServerMessage.ServerWait.getMessage());
            while (KEEP_ALIVE) {
                Socket clientSocket = server.accept();
                System.out.println(ServerMessage.ServerClientConnect.getMessage());
                ClientHandler client = new ClientHandler(clientSocket);
                connections.add(client);
                threadPool.execute(client);
                checkConcurrentUsersIsOverLimit(connections.size(), true);
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

    protected boolean checkConcurrentUsersIsOverLimit(int userCount, boolean sendWarning) {
        System.out.println("Current users: " + userCount);
        if (userCount >= 1000) {
            if (sendWarning) {
                String email = System.getenv("GMAIL_ADDR");
                String token = System.getenv("conwaysGOLKey");
                String host = "smtp.gmail.com";
                Properties properties = System.getProperties();
                properties.put("mail.smtp.host", host);
                properties.put("mail.smtp.port", "587");
                properties.put("mail.smtp.auth", "true");
                properties.put("mail.smtp.starttls.enable", "true");
                Session session = Session.getInstance(properties, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(email, token);
                    }
                });
                try {
                    MimeMessage message = new MimeMessage(session);
                    message.setFrom(new InternetAddress(email));
                    message.addRecipient(Message.RecipientType.TO, new InternetAddress(email));
                    message.setSubject("High server load");
                    message.setText(String.format("The number of concurrent users has reached %d on your server.", userCount));
                    Transport.send(message);
                    System.out.println("Server load warning has been sent.");
                } catch (Exception mex) {
                    System.out.println(ServerMessage.EmailFailure.getMessage());
                }
            }
            return true;
        }
        return false;
    }

    protected void shutdown() {
        KEEP_ALIVE = false;
        threadPool.shutdown();
        for (ClientHandler c : connections) {
            c.shutdown();
        }
    }

    protected void messageAllClients(String message) {
        for (ClientHandler c : connections) {
            if (c != null) {
                c.messageClient(message);
            }
        }
    }

    protected String getUniqueSignature(String ipAddress, String name) {
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


        // Activities to choose from
        private boolean noActivityChosen;
        private boolean conwaysChosen;
        //private boolean otherGame;

        private final Socket connection;
        private PrintWriter outbound;
        private BufferedReader inbound;
        
        private String name;
        private int nameChangesRemaining = 2;

        private int SPEED = -1;
        protected int xSize = -1;
        protected int ySize = -1;
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
        private final Map<Integer, Integer> speedToFPS = new HashMap<>();
        private AtomicBoolean KEEP_SIM_ALIVE;

        private boolean signatureSet = false;
        private String connectionSignature;

        public ClientHandler(Socket connection) {
            this.connection = connection;
            conwaysChosen = false;
            noActivityChosen = true;
            speedToFPS.put(1, 1);
            speedToFPS.put(2, 3);
            speedToFPS.put(3, 5);
            speedToFPS.put(4, 7);
            speedToFPS.put(5, 10);
            KEEP_SIM_ALIVE = new AtomicBoolean(false);
        }

        protected boolean isValidSpeed(int passedSpeed) {
            return passedSpeed < 6 && passedSpeed > 0;
        }

        protected boolean isValidSize(int x, int y) {
            return x > 5 && x <= 75 && y < 75 && y > 5;
        }

        private void startSim() {

            int count = 0;

            messageClient(ServerMessage.SimulationStarted.getMessage());
            int[][] previousState;
            int[][] nextState;
            int tick = convertSpeedToTickRate(SPEED);
            outbound.println(String.format("%s:tick:%d:frame:%s", connectionSignature, tick, serializeFrame(startPosition)));
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException iE) {
                Thread.currentThread().interrupt();
                messageClient(ServerMessage.SimulationFailure.getMessage());
                return;
            }
            previousState = startPosition;
            nextState = generateNextFrame(previousState);
            while (KEEP_SIM_ALIVE.get()) {
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException iE) {
                    Thread.currentThread().interrupt();
                    messageClient(ServerMessage.SimulationFailure.getMessage());
                    return;
                }
                String frame = connectionSignature + serializeFrame(nextState);
                outbound.println(frame);
                previousState = nextState;
                nextState = generateNextFrame(previousState);
                count++;
            }
            System.out.println("Total frames rendered for " + name + ": " + count);
            messageClient(ServerMessage.SimulationFinished.getMessage());
        }

        protected int convertSpeedToTickRate(int speed) {
            return 1000 / speedToFPS.get(speed);
        }

        protected int countNeighbors(int[][] frame, int x, int y) {
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

        protected int[][] generateNextFrame(int[][] lastFrame) {
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
                    int neighbors = countNeighbors(lastFrame, i, j);
                    if (lastFrame[i][j] == 1 && neighbors > 3) {
                        result[i][j] = 0;
                    } else if ((lastFrame[i][j] == 1 && neighbors >= 2) || (lastFrame[i][j] == 0 && neighbors == 3)) {
                        result[i][j] = 1;
                    } else {
                        result[i][j] = 0;
                    }
                }
            }
            return result;
        }

        protected String serializeFrame(int[][] frame) {
            StringBuilder builder = new StringBuilder();
            for (int[] row : frame) {
                builder.append(Arrays.toString(row).replaceAll("[\\[\\]\\s]", "")).append(";");
            }
            return builder.toString();
        }

        protected int[][] getRandomlyGeneratedPosition() {
            int[][] position = new int[ySize][xSize];
            for (int i = 0; i < position.length; i++) {
                for (int j = 0; j < position[i].length; j++) {
                    position[i][j] = getRandomDigit();
                }
            }
            return position;
        }

        protected int getRandomDigit() {
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
                    if (namesUsed.contains(name)) {
                        name = null;
                        outbound.println("This name has already been taken!");
                    }
                }
                namesUsed.add(name);
                System.out.printf("%s registered the name %s.%n", ipAddress, name);
                messageAllClients(name + " has joined the server.\n");

                connectionSignature = getUniqueSignature(ipAddress, name);
                if (connectionSignature.equals("Failed")) {
                    messageClient("Error generating unique signature, try reconnecting if you want to use the simulation functionality.");
                }
                messageClient("Send the following command to validate your connection:\n&set server signature " + connectionSignature + "\n");

                // MAIN LOOP
                String chat;
                while ((chat = inbound.readLine()) != null) {
                    if (chat.toLowerCase().startsWith("&")) {
                        if (chat.toLowerCase().startsWith("&quit")) {
                            messageAllClients(name + " left the server.");
                            System.out.println("[*] " + name + ServerMessage.ServerClientDC.getMessage());
                            shutdown();
                            return;
                        } else if (chat.equalsIgnoreCase("&help")) {
                            messageClient(getHelpText());
                        } else if (chat.toLowerCase().startsWith("&name")) {
                            handleNameChange(chat);
                        } else if (chat.equals("&set server signature " + connectionSignature) && !signatureSet) {
                            signatureSet = true;
                            messageClient(ServerMessage.SignatureSet.getMessage());
                        } else if (noActivityChosen) {
                            handleNoActivityCommand(chat);
                        } else if (conwaysChosen) {
                            handleConwaysCommands(chat);
                        } else {
                            messageClient(ServerMessage.InvalidCommand.getMessage());
                        }
                    } else {
                        if (!chat.contains(connectionSignature)) {
                            messageAllClients(ServerMessage.getSyntax(name) + chat);
                        } else {
                            messageClient(ServerMessage.SignatureInMessageError.getMessage());
                        }
                    }
                }
            } catch (IOException iE) {
                System.out.println("[*] " + name + ServerMessage.ServerClientDC.getMessage());
                shutdown();
            } catch (NullPointerException nPE) {
                System.err.println(ServerMessage.NullException.getMessage());
                shutdown();
            }
        }

        private void handleNoActivityCommand(String chat) {
            if (chat.equalsIgnoreCase("&conways")) {
                conwaysChosen = true;
                noActivityChosen = false;
                messageClient("Joined Conway's Game of Life, use &help to view the commands.");
            } else {
                messageClient(ServerMessage.InvalidCommand.getMessage());
            }
        }

        private void handleConwaysCommands(String chat) {
            if (chat.equalsIgnoreCase("&leave activity")) {
                conwaysChosen = false;
                noActivityChosen = true;
                messageClient(ServerMessage.LeftActivity.getMessage());
            } else if (chat.startsWith(String.format("&%s", connectionSignature))) {
                receiveInitialState(chat);
            } else if (chat.toLowerCase().startsWith("&set speed")) {
                handleSettingSpeed(chat);
            } else if (chat.equals("&get speed")) {
                sendSpeedToClient();
            } else if (chat.toLowerCase().startsWith("&set size")) {
                handleSettingSize(chat);
            } else if (chat.equals("&get size")) {
                sendSizeToClient();
            } else if (chat.equalsIgnoreCase("&set init pos")) {
                handleSettingInitialPosition();
            } else if (chat.equals("&start sim")) { 
                handleSimStartCommand();
            } else if (chat.equals("&end sim")) {
                if (KEEP_SIM_ALIVE.get() == true) {
                    KEEP_SIM_ALIVE.set(false);
                } else {
                    messageClient("[!] This command can only be sent when the simulation is running.");
                }
            } else {
                messageClient(ServerMessage.InvalidCommand.getMessage());
            }
        }

        private void handleNameChange(String chat) {
            if (nameChangesRemaining == 0) {
                messageClient("You have no name changes remaining.");
                return;
            }
            String[] cmdAndName = chat.split(" ", 2);
            if (cmdAndName.length != 2) {
                messageClient("You did not provide a new name. Type \"&help\" to see the help menu.");
                return;
            }
            if (cmdAndName[1].equals(name)) {
                messageClient("This name is already your existing name.");
                return;
            }
            if (cmdAndName[1].startsWith("&")) {
                messageClient("Your name cannot start with the command symbol.");
                return;
            }
            if (cmdAndName[1].length() < 2) {
                messageClient("This name is not long enough.");
                return;
            }
            if (namesUsed.contains(chat)) {
                messageClient("This name is already in use by another member.");
                return;
            }
            namesUsed.remove(name);
            messageAllClients(String.format("%s has changed their name to %s", name, cmdAndName[1]));
            System.out.printf("%s changed their name to %s.%n", name, cmdAndName[1]);
            messageClient(String.format("%d name changes remaining.", --nameChangesRemaining));
            name = cmdAndName[1];
            namesUsed.add(name);
        }

        private void handleSimStartCommand() {
            messageClient("Checking configuration...");
            if (!signatureSet) {
                messageClient(ServerMessage.SignatureNotSet.getMessage());
                return;
            }
            if (isValidSpeed(SPEED) && isValidSize(xSize, ySize)) {
                if (startPosition == null) {
                    messageClient("You did not set a starting position. It will be randomly generated for you.");
                    startPosition = getRandomlyGeneratedPosition();
                }
                KEEP_SIM_ALIVE.set(true);
                CompletableFuture.runAsync(() -> startSim());
            } else {
                messageClient(ServerMessage.SimulationInvalid.getMessage());
            }
        }

        private void handleSettingInitialPosition() {
            if (!isValidSize(xSize, ySize)) {
                messageClient("There is not a valid grid size set. Use '&set size x y' to change this.");
                return;
            }
            String message = String.format("%s:SIZE:%d:%d", connectionSignature, xSize, ySize);
            messageClient(message);
        }

        private void receiveInitialState(String state) {
            startPosition = deserializeStartBoard(state.substring(connectionSignature.length() + 1));
            messageClient("Initial position of simulation has been set.");
        }

        private void handleSettingSize(String chat) {
            String[] sizeValues = chat.split(" ");
            if (sizeValues.length != 4) {
                messageClient(ServerMessage.InvalidCommand.getMessage());
                return;
            }
            try {
                int x = Integer.parseInt(sizeValues[2]);
                int y = Integer.parseInt(sizeValues[3]);
                if (!isValidSize(x, y)) {
                    messageClient(ServerMessage.InvalidCommand.getMessage());
                    return;
                }
                xSize = x;
                ySize = y;
                messageClient(String.format("Size of grid set to: %dx%d.", xSize, ySize));
            } catch (NumberFormatException nFE) {
                messageClient(ServerMessage.InvalidCommand.getMessage());
            }

        }

        private void sendSpeedToClient() {
            if (isValidSpeed(SPEED)) {
                messageClient("Current set speed is: " + SPEED);
            } else {
                messageClient(ServerMessage.UnsetValue.getMessage());
            }
        }

        private void sendSizeToClient() {
            if (isValidSpeed(SPEED)) {
                messageClient("Current set speed is: " + SPEED);
            } else {
                messageClient(ServerMessage.UnsetValue.getMessage());
            }
        }

        private void handleSettingSpeed(String chat) {
            String[] speedCall = chat.split(" ");
            if (speedCall.length != 3) {
                messageClient(ServerMessage.InvalidCommand.getMessage());
                return;
            }
            try {
                int tempSpeed = Integer.parseInt(speedCall[2]);
                if (!isValidSpeed(tempSpeed)) {
                    messageClient(ServerMessage.InvalidCommand.getMessage());
                    return;
                }
                SPEED = tempSpeed;
            } catch (NumberFormatException nFE) {
                messageClient(ServerMessage.InvalidCommand.getMessage());
            }
            messageClient("Tick speed has been set.");
        }


        protected int[][] deserializeStartBoard(String data) {
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
            StringBuilder sB = new StringBuilder();
            sB.append("#######################################################################################");
            sB.append("\nHow to use commands: \n\n");
            sB.append("&quit            ->    Enter to exit the chat.\n\n");
            sB.append("&name NEW_NAME   ->    Enter to change name (2 name changes allowed).\n\n");
            if (!signatureSet) {
                sB.append("Use this command to verify the signature with the server.\n");
                sB.append(String.format("&set server signature %s\n\n", connectionSignature));
            }
            if (noActivityChosen) {
                sB.append("To choose an activity, use one of the following commands:\n");
                sB.append("&conways         ->    Build and run a simulation for Conway's Game of Life.\n");
            } else if (conwaysChosen) {
                sB.append("&set size        ->    Set the size of the grid (Used as \"&set size x y\").\n");
                sB.append("                            Size for x and y must be between 5 and 75.\n\n");
                sB.append("&set init pos    ->    Set the initial configuration of the grid by selecting the cells to be considered \"alive\" (Size of grid must already be set).\n");
                sB.append("&set speed       ->    Set the speed (1-5) at which the game cycles (Used as \"&set speed x\").\n\n");
                sB.append("&get speed       ->    Get the current set speed for the tick rate of the simulation.\n");
                sB.append("&get size        ->    Get the current set size for the grid of the simulation.\n");
                sB.append("&start sim       ->    Start the simulation with the provided settings.\n");
                sB.append("&end sim         ->    Terminate the simulation.\n");
                sB.append("&leave activity  ->    Leave the current activity.\n");
            }
            sB.append("\n---------------------------------------------------------------------------------------\n\n");
            sB.append("Anytime \"&\" is used at the beginning of a chat it will not be displayed in the chat log.\n");
            sB.append("#######################################################################################");
            return sB.toString();
        }

        private void shutdown() {
            try {
                namesUsed.remove(name);
                outbound.close();
                inbound.close();
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (IOException iE) {
                System.err.println("Error closing resources.");
            }
        }

        private void messageClient(String message) {
            outbound.println(message);
        }
    }
}
