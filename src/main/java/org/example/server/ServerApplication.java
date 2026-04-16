package org.example.server;

import java.io.IOException;

public class ServerApplication {

    public void start(ServerConfig config) throws IOException {
        config.getServer().start();
    }
}
