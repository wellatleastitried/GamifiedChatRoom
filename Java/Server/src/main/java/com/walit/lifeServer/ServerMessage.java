package com.walit.lifeServer;

public enum ServerMessage {

        ServerCreated("[*] Server has been created."),
        ServerError("[!] The server received an error:\n"),
        ServerShutdown("[*] The server is shutting down."),
        ServerWait("[*] Waiting for connections...\n"),
        ServerClientConnect("[*] Client connected."),
        ServerClientDC(" has disconnected from the server.");

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
