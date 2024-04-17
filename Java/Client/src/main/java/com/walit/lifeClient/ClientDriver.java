// package com.walit.lifeClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientDriver implements Runnable {

    private Socket client;
    private boolean KEEP_ALIVE;
    private BufferedReader inbound;
    private PrintWriter outbound;

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
            client = new Socket("192.168.56.1", 4444);
            inbound = new BufferedReader(new InputStreamReader(client.getInputStream()));
            outbound = new PrintWriter(client.getOutputStream(), true);
            Input input = new Input();
            Thread thread = new Thread(input);
            thread.start();
            String inputChat;
            while ((inputChat = inbound.readLine()) != null) {
                System.out.println(inputChat);
            }
        } catch (Exception e) {
            shutdown();
        }
    }
    class Input implements Runnable {
        @Override
        public void run() {
            try {
                BufferedReader readIn = new BufferedReader(new InputStreamReader(System.in));
                while (KEEP_ALIVE) {
                    String chat = readIn.readLine();
                    if (chat.equalsIgnoreCase("&quit")) {
                        outbound.println(chat);
                        readIn.close();
                        shutdown();
                    } else {
                        outbound.println(chat);
                    }
                }
            } catch (Exception e) {
                shutdown();
            }
        }
    }
    public static void main(String[] args) {
        ClientDriver client = new ClientDriver();
        client.run();
    }
}