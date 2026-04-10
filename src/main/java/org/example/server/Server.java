package org.example.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    // Зависимости необходимые

    // Добавить executor для выполнения клиентских сокетов

    public void start() {
        int port = 0;
        try {
            ServerSocket serverSocket = new ServerSocket(port);

            while (true) {
                Socket clientSocket = serverSocket.accept();

                // executor.submit( handleClient() )
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
