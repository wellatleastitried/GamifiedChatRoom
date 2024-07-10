package com.walit.lifeClient;

import com.walit.lifeClient.SetPosition.*;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;

public class ClientDriver implements Runnable {

    private Socket client;
    public boolean KEEP_ALIVE;
    private BufferedReader inbound;
    public PrintWriter outbound;

    private boolean waitForCoordsResponse = false;

    public ClientDriver() {
        KEEP_ALIVE = true;
    }
    public void shutdown() {
        KEEP_ALIVE = false;
        try {
            inbound.close();
            outbound.close();
            if (!client.isClosed()) {
                client.close();
            }
        } catch (Exception e) {
            System.err.println("Try again later...");
        }
    }

    @Override
    public void run() {
        try {
            client = new Socket("10.0.0.56", 4444); // Change to public IP
            inbound = new BufferedReader(new InputStreamReader(client.getInputStream()));
            outbound = new PrintWriter(client.getOutputStream(), true);
            Input input = new Input(this);
            Thread thread = new Thread(input);
            thread.start();
            String inputChat;
            while ((inputChat = inbound.readLine()) != null && KEEP_ALIVE) {
                if (waitForCoordsResponse) {
                    handleSizeMessage(inputChat.substring(5));
                    waitForCoordsResponse = false;
                } else {
                    System.out.println(inputChat);
                }
            }
        } catch (Exception e) {
            System.out.println("Exception in run():\n" + e.getMessage());
            shutdown();
        }
    }
    public void sendCommand(String cmd) {
        outbound.println(cmd);
        if (cmd.equalsIgnoreCase("&set init pos")) {
            waitForCoordsResponse = true;
        }
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