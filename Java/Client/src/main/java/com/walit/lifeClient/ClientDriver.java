package com.walit.lifeClient;

import com.walit.lifeClient.Frame.InitialPosition;
import com.walit.lifeClient.Frame.SimulationRender;

import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ClientDriver implements Runnable {

    private Socket client;
    public boolean KEEP_ALIVE;
    private BufferedReader inbound;
    public PrintWriter outbound;

    private String SERVER_SIGNATURE;

    private final Queue<String> serializedFramesToRender = new PriorityQueue<>();

    private SimulationRender sim;
    private boolean simIsReady = false;
    private boolean simSpawned = false;
    private boolean waitForInitialPositionResponse = false;
    private volatile boolean simulationRunning = false;
    private boolean firstFrameRendered = false;

    private int xSize = 0;
    private int ySize = 0;

    public ClientDriver() {
        KEEP_ALIVE = true;
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
            client = new Socket("10.0.0.59", 4444); // Change to public IP
            inbound = new BufferedReader(new InputStreamReader(client.getInputStream()));
            outbound = new PrintWriter(client.getOutputStream(), true);
            Input input = new Input(this);
            Thread thread = new Thread(input);
            thread.start();
            String inputChat;
            while ((inputChat = inbound.readLine()) != null && KEEP_ALIVE) {
                if (SERVER_SIGNATURE != null && inputChat.startsWith(SERVER_SIGNATURE)) {
                    if (inputChat.substring(SERVER_SIGNATURE.length()).startsWith("0") || inputChat.substring(SERVER_SIGNATURE.length()).startsWith("1") && firstFrameRendered) {
                        serializedFramesToRender.add(inputChat.substring(SERVER_SIGNATURE.length()));
                    } else if (inputChat.equals(SERVER_SIGNATURE + " KILL")) {
                        simulationRunning = false;
                        SwingUtilities.invokeLater(() -> sim.shutdown());
                        simSpawned = false;
                        firstFrameRendered = false;
                    } else if (inputChat.startsWith(SERVER_SIGNATURE + ":SIZE:")) {
                        handleSizeMessage(inputChat.substring(70));
                        waitForInitialPositionResponse = false;
                    }
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
                                SwingUtilities.invokeLater(() -> sim.renderNextScene(deserializeStateFromServer(finalInputChat.substring(SERVER_SIGNATURE.length()))));
                                firstFrameRendered = true;
                            } else {
                                continue;
                            }
                            simIsReady = false;
                            System.out.println("Simulation is starting...");
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
                } else if (simulationRunning) {
                    if (!serializedFramesToRender.isEmpty()) {
                        int[][] state = deserializeStateFromServer(serializedFramesToRender.remove());
                        SwingUtilities.invokeLater(() -> sim.renderNextScene(state));
                    } else {
                        System.err.println("Queue has no more frames to render.");
                    }
                } else {
                    System.out.println(inputChat);
                }
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
        } else if (cmd.toLowerCase().startsWith("&set size")) {
            String[] dataFromCmd = cmd.split(" ");
            if (dataFromCmd.length == 4) {
                try {
                    xSize = Integer.parseInt(dataFromCmd[2]);
                    ySize = Integer.parseInt(dataFromCmd[3]);
                } catch (NumberFormatException nFE) {
                    System.err.println("NumberFormatException in sendAndHandleCommand()");
                }
            }
        } else if (cmd.startsWith("&set server signature")) {
            String[] partsOfSigCmd = cmd.trim().split(" ");
            if (SERVER_SIGNATURE != null) {
                System.out.println("The signature has already been set.");
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
        while (!frame.isSavePressed) {
            Thread.onSpinWait();
        }
        int[][] initFrame = frame.getFinalCustomPosition();
        outbound.println(serializeIntArray(initFrame));
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
