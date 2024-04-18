package com.walit.lifeServer;

public enum ServerMessage {

        ServerCreated("[*] Server has been created."),
        ServerError("[!] The server received an error:\n"),
        ServerShutdown("[*] The server is shutting down."),
        ServerWait("[*] Waiting for connections...\n"),
        ServerClientConnect("[*] Client connected."),
        ServerClientDC(" has disconnected from the server."),
        InvalidCommand("[!] This is not a valid command. Enter \"&help\" to see a list of valid commands."),
        SimulationSuccess("Simulation has successfully started."),
        SimulationFailure("Simulation has had an unexpected error."),
        WaitingClientError("[!] Null pointer exception from client in waiting room."),
        ServerCachedThreads("[*] Starting server with cached thread pool."),
        ServerFixedThreads("[*] Starting server with fixed thread pool of size: ");

        private final String message;

        ServerMessage(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
        public static String getSyntax(String name) {
            return "    | " + name + ": ";
        }
}
