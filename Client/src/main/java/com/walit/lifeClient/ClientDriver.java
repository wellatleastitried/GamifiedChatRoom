package com.walit.lifeClient;

import com.walit.lifeClient.Frame.InitialPosition;
import com.walit.lifeClient.Frame.SimulationRender;

import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/* ******************************************************************************************
*                       THE LINE TO CHANGE THE IP AND PORT NUMBER IS ON LINE 69             *
******************************************************************************************* */

public class ClientDriver implements Runnable {

    private Socket client;
    public boolean KEEP_ALIVE;
    private BufferedReader inbound;
    public PrintWriter outbound;

    private String SERVER_SIGNATURE;

    private final Map<Long, String> serializedFrames;

    private SimulationRender sim;
    private boolean simIsReady = false;
    private boolean simSpawned = false;
    private boolean waitForInitialPositionResponse = false;
    private volatile boolean simulationRunning = false;
    private volatile boolean go = false;
    private boolean firstFrameRendered = false;
    private long totalFramesReceived;
    private long totalFramesRendered;

    private int xSize = 0;
    private int ySize = 0;
    private int tick = 0;

    public ClientDriver() {
        KEEP_ALIVE = true;
        serializedFrames = new ConcurrentHashMap<>();
        totalFramesReceived = 0;
        totalFramesRendered = 0;
    }
    public int shutdown() {
        KEEP_ALIVE = false;
        try {
            inbound.close();
            outbound.close();
            if (!client.isClosed()) {
                client.close();
            }
        } catch (Exception e) {
            System.err.println("Unable to connect to server.");
            return 1;
        }
        return 0;
    }

    @Override
    public void run() {
        try {
            // client = new Socket("10.0.0.59", 4444); // Change to public IP
            client = new Socket("192.168.20.38", 4444);
            // client = new Socket("chatroom.ddns.net", 59284);
            inbound = new BufferedReader(new InputStreamReader(client.getInputStream()));
            outbound = new PrintWriter(client.getOutputStream(), true);
            Input input = new Input(this);
            Thread thread = new Thread(input);
            thread.start();
            String inputChat;
            while ((inputChat = inbound.readLine()) != null && KEEP_ALIVE) {
                if (SERVER_SIGNATURE != null && inputChat.startsWith(SERVER_SIGNATURE)) {
                    if (inputChat.substring(SERVER_SIGNATURE.length()).startsWith("0") || inputChat.substring(SERVER_SIGNATURE.length()).startsWith("1") && firstFrameRendered) {
                        totalFramesReceived++;
                        serializedFrames.put(totalFramesReceived, inputChat.substring(SERVER_SIGNATURE.length()));
                    } else if (inputChat.startsWith(SERVER_SIGNATURE + ":SIZE:")) {
                        handleSizeMessage(inputChat.substring(70));
                        waitForInitialPositionResponse = false;
                        continue;
                    }
                } else {
                    System.out.println(inputChat);
                }
                if (simIsReady) {
                    String finalInputChat = inputChat;
                    switch (inputChat) {
                        case "Checking configuration...",
                             "You did not set a starting position. It will be randomly generated for you." ->
                                System.out.println(inputChat);
                        case "[!] Simulation cannot start because there are invalid settings set.",
                             "[!] Simulation cannot start because you never set the server signature." ->
                                simIsReady = false;
                        default -> {
                            if (!simSpawned) {
                                SwingUtilities.invokeAndWait(() -> sim = new SimulationRender(xSize, ySize));
                                simSpawned = true;
                            }
                            if (inputChat.startsWith(SERVER_SIGNATURE)) {
                                // SERVER_SIGNATURE:tick:5:frame:
                                String[] tokens = inputChat.split(":");
                                try {
                                    tick = Integer.parseInt(tokens[2]);
                                    //tick = Integer.parseInt(inputChat.substring(SERVER_SIGNATURE.length(), inputChat.indexOf(":frame")));
                                } catch (NumberFormatException ex) {
                                    System.out.println("There has been a fatal error.");
                                    System.exit(1);
                                }
                                totalFramesRendered = 1;
                                SwingUtilities.invokeLater(() -> sim.renderNextScene(deserializeStateFromServer(tokens[4]), totalFramesRendered));
                                //SwingUtilities.invokeLater(() -> sim.renderNextScene(deserializeStateFromServer(finalInputChat.substring(finalInputChat.indexOf(":frame") + 6)), totalFramesRendered));
                                firstFrameRendered = true;
                            } else {
                                continue;
                            }
                            if (firstFrameRendered && tick != 0) {
                                simIsReady = false;
                                System.out.println("[*] Simulation is starting...");
                                CompletableFuture.runAsync(() -> {
                                    try {
                                        TimeUnit.SECONDS.sleep(3);
                                    } catch (InterruptedException e) {
                                        Thread.currentThread().interrupt();
                                    }
                                    simulationRunning = true;
                                });
                            }
                        }
                    }
                } else if (simulationRunning && !go) {
                    go = true;
                    /*
                    if (serializedFrames.containsKey(totalFramesRendered) && serializedFrames.size() > totalFramesRendered) {
                        int[][] state = deserializeStateFromServer(serializedFrames.get(totalFramesRendered));
                        totalFramesRendered++;
                        SwingUtilities.invokeLater(() -> sim.renderNextScene(state, totalFramesRendered));
                    } else {
                        System.out.println("[*] Simulation has finished.");
                        simulationRunning = false;
                        SwingUtilities.invokeLater(() -> sim.shutdown());
                        simSpawned = false;
                        firstFrameRendered = false;
                        serializedFrames.clear();
                        totalFramesReceived = 0;
                        totalFramesRendered = 0;
                    }
                    */
                    CompletableFuture.runAsync(() -> {
                        while (simulationRunning) {
                            try {
                                TimeUnit.MILLISECONDS.sleep(tick);
                            } catch (InterruptedException iE) {
                                System.out.println("[!] Error occured while running simulation.");
                                simulationRunning = false;
                                break;
                            }
                            if (serializedFrames.containsKey(totalFramesRendered) && serializedFrames.size() > totalFramesRendered) {
                                int[][] state = deserializeStateFromServer(serializedFrames.get(totalFramesRendered));
                                totalFramesRendered++;
                                SwingUtilities.invokeLater(() -> sim.renderNextScene(state, totalFramesRendered));
                            } else {
                                System.out.println("[*] Simulation has finished.");
                                simulationRunning = false;
                                SwingUtilities.invokeLater(() -> sim.shutdown());
                                simSpawned = false;
                                firstFrameRendered = false;
                                serializedFrames.clear();
                                totalFramesReceived = 0;
                                totalFramesRendered = 0;
                            }
                        }
                        go = false;
                    });
                }/* else if (!simIsReady && !simulationRunning && !go) {
                    System.out.println(inputChat);
                }*/
            }
        } catch (Exception e) {
            shutdown();
        }
    }

    private int[][] deserializeStateFromServer(String input) {
        int[][] state = new int[ySize][xSize];
        String[] rows = input.split(";");
        for (int i = 0; i < ySize; i++) {
            String[] elements = rows[i].split(",");
            for (int j = 0; j < xSize; j++) {
                state[i][j] = Integer.parseInt(elements[j]);
            }
        }
        return state;
    }

    public void sendAndHandleCommand(String cmd) {
        if (cmd.equalsIgnoreCase("&set init pos")) {
            assert(!simulationRunning);
            assert(!waitForInitialPositionResponse);
            waitForInitialPositionResponse = true;
        } else if (cmd.equalsIgnoreCase("&start sim")) {
            if (SERVER_SIGNATURE == null) {
                return;
            }
            if (simulationRunning) {
                System.out.println("Simulation is already running.");
            } else if (simIsReady) {
                System.out.println("Simulation is being setup.");
            }
            simIsReady = true;
            tick = 0;
        } else if (cmd.toLowerCase().startsWith("&end sim")) {
            if (!simulationRunning) {
               System.out.println("[*] This command can only be used when the simulation is running.");
               return;
            }
        } else if (cmd.toLowerCase().startsWith("&set size")) {
            String[] dataFromCmd = cmd.split(" ");
            if (dataFromCmd.length == 4) {
                try {
                    xSize = Integer.parseInt(dataFromCmd[2]);
                    ySize = Integer.parseInt(dataFromCmd[3]);
                } catch (NumberFormatException nFE) {
                    System.err.println("Incorrect values were given for the size leading to an error.");
                }
                assert(xSize > 0);
                assert(ySize > 0);
            }
        } else if (cmd.startsWith("&set server signature")) {
            String[] partsOfSigCmd = cmd.trim().split(" ");
            if (SERVER_SIGNATURE != null) {
                System.out.println("The signature has already been set.");
                return;
            }
            if (!(SERVER_SIGNATURE == null && partsOfSigCmd.length == 4 && partsOfSigCmd[3].length() == 64)) {
                System.out.println("You have not copied the command correctly, it must be the exact command that you see in the terminal.");
                return;
            }
            SERVER_SIGNATURE = partsOfSigCmd[3];
        }
        outbound.println(cmd);
    }

    private void handleSizeMessage(String input) {
        if (SERVER_SIGNATURE == null) {
            System.out.println("[!] The server signature has not been initialized, open the help menu to find the command to set it.");
            return;
        }
        String[] strSize = input.split(":");
        int x = 0;
        int y = 0;
        try {
            x = Integer.parseInt(strSize[0]);
            y = Integer.parseInt(strSize[1]);
        } catch (Exception e) {
            System.err.println("Exception in handleSizeMessage:\n" + e.getMessage());
            shutdown();
        }
        InitialPosition frame = new InitialPosition(x, y);
        CompletableFuture.runAsync(() -> {
            while (!frame.isSavePressed) {
                Thread.onSpinWait();
            }
            int[][] initFrame = frame.getFinalCustomPosition();
            outbound.println(String.format("&%s%s", SERVER_SIGNATURE, serializeIntArray(initFrame)));
        });
    }

    private String serializeIntArray(int[][] array) {
        StringBuilder sB = new StringBuilder();
        for (int[] row : array) {
            sB.append(Arrays.toString(row).replaceAll("[\\[\\]\\s]", "")).append(";");
        }
        return sB.toString();
    }

    public static void main(String[] args) {
        ClientDriver client = new ClientDriver();
        client.run();
    }
}
