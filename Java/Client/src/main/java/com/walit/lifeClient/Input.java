package com.walit.lifeClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Input implements Runnable {
    private final ClientDriver clientDriver;
    public Input(ClientDriver clientDriver) {
        this.clientDriver = clientDriver;
    }
    @Override
    public void run() {
        try {
            BufferedReader readIn = new BufferedReader(new InputStreamReader(System.in));
            while (clientDriver.KEEP_ALIVE) {
                String chat = readIn.readLine();
                if (chat.equalsIgnoreCase("&quit")) {
                    clientDriver.outbound.println(chat);
                    readIn.close();
                    clientDriver.shutdown();
                } else if (chat.equalsIgnoreCase("&set init pos")) {
                    clientDriver.sendAndHandleCommand(chat);
                } else if (chat.toLowerCase().startsWith("&set size")) {
                    clientDriver.sendAndHandleCommand(chat);
                } else if (chat.equalsIgnoreCase("&start sim")) {
                    clientDriver.sendAndHandleCommand(chat);
                } else if (chat.startsWith("&set server signature")) {
                    clientDriver.sendAndHandleCommand(chat);
                } else {
                    clientDriver.outbound.println(chat);
                }
            }
        } catch (Exception e) {
            clientDriver.shutdown();
        }
    }
}
