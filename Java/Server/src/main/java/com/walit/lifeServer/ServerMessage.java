package com.walit.lifeServer;

public enum ServerMessage {

        ServerCreated("[*] Server has been created."),
        FailedSignatureGeneration("[!] The SHA-256 algorithm was not able to be loaded, resulting in an error while generating the signature."),
        SignatureSet("[*] You have properly initialized the server signature."),
        SignatureNotSet("[!] You never initialized the verification signature. Scroll up to find the command to send or reconnect."),
        SignatureInMessageError("[!] Do not send messages with your server signature attached."),
        ServerError("[!] The server received an error:\n"),
        ServerShutdown("[*] The server is shutting down."),
        ServerWait("[*] Waiting for connections...\n"),
        ServerClientConnect("[*] Client connected."),
        ServerClientDC(" has disconnected from the server."),
        InvalidCommand("[!] This is not a valid command. Enter \"&help\" to see a list of valid commands."),
        SimulationStarted("[*] Simulation has successfully started."),
        SimulationFinished("[*] Simulation has completed successfully."),
        SimulationInvalid("[!] Simulation cannot start because there are invalid settings set."),
        SimulationFailure("Simulation has had an unexpected error."),
        NullException("Null pointer exception encountered, closing client connection."),
        ServerCachedThreads("[*] Starting server with cached thread pool."),
        ServerFixedThreads("[*] Starting server with fixed thread pool of size: "),
        LeftActivity("[*] You have left your current activity. Open help to find others to join."),
        EmailFailure("[!] There has been an error while sending an email regarding the number of concurrent users."),
        UnsetValue("[*] This value has not been set. Use 'help' to find the proper command to set this value.");

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
