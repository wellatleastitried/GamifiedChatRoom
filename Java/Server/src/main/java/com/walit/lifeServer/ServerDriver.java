package com.walit.lifeServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class ServerDriver {
    // Create server socket that accepts connections
    // Create fixed thread pool that allocates new thread for each client
    private final ThreadPoolExecutor threadPool;
    private final HashMap<String, String> SERVERMESSAGE= new HashMap<>();
    public static void main(String[] args) {
        ServerDriver sD = new ServerDriver(1);
        if (sD.runServer() == 0) {
            System.out.println("Server has successfully terminated.");
        } else {
            System.err.println("Server had an error.");
        }
    }
    public ServerDriver() {
        threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
    }
    public ServerDriver(int expectedConnections) {
        int SIZE;
        SIZE = expectedConnections < 1 ? 1 : Math.min(expectedConnections, 50);
        threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(SIZE);
    }
    private void initMessages() {
        SERVERMESSAGE.put("create", "Server socket has been created.");
        SERVERMESSAGE.put("fail", "The server received an error.");
        SERVERMESSAGE.put("connect", "[*] Client connected.");
        SERVERMESSAGE.put("disconnect", "[*] Client disconnected.");
    }
    private int runServer() {
        try (ServerSocket server = new ServerSocket(4444)) {
            System.out.println(SERVERMESSAGE.get("create"));
            boolean KEEP_ALIVE = true;
            while (KEEP_ALIVE) {
                // Accept connections and allocate a thread from the thread pool to them




                long startTimeCheck = System.currentTimeMillis();
                long endTimeCheck;
                // If the server has no new connections after 15 seconds, shutdown
                while (threadPool.getMaximumPoolSize() - threadPool.getPoolSize() == threadPool.getMaximumPoolSize()) {
                    endTimeCheck = System.currentTimeMillis();
                    if (endTimeCheck - 15000 > startTimeCheck) {
                        KEEP_ALIVE = false;
                        break;
                    }
                }
            }
            threadPool.shutdown();
        } catch (IOException iE) {
            System.err.println("IOException with server socket:\n" + iE.getMessage());
            return 1;
        } catch (Exception e) {
            System.err.println("Get rid of this");
            return 1;
        }
        return 0;
    }
}