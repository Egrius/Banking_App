package org.example.server;

import lombok.extern.slf4j.Slf4j;
import org.example.dto.Request;
import org.example.dto.Response;
import org.example.server.dispatcher.RequestDispatcher;
import org.example.util.JsonUtil;
import org.postgresql.util.ReaderInputStream;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public class Server {
    private final RequestDispatcher dispatcher;
    private final int port;
    private ServerSocket serverSocket;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public Server(RequestDispatcher dispatcher, int port) {
        this.dispatcher = dispatcher;
        this.port = port;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
            log.info("Сервер запущен на порту " + port);
            System.out.println("--- СТАРТ ---");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                executor.submit(() -> handleClient(clientSocket));
            }
    }

    public void stop() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        executor.shutdown();
        try {
            if(!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleClient(Socket clientSocket) {
        try (clientSocket;
             BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8))) {

            String jsonLine;

            while ((jsonLine = reader.readLine()) != null) {
                log.debug("Raw JSON received: '{}'", jsonLine);

                if (jsonLine.isBlank()) {
                    continue;
                }

                try {
                    Request request = JsonUtil.fromJson(jsonLine, Request.class);

                    Response response = dispatcher.dispatch(request);
                    String responseJson = JsonUtil.toJson(response);
                    writer.write(responseJson);
                    writer.newLine();
                    writer.flush();

                } catch (Exception e) {
                    log.error("Ошибка при обработке запроса", e);
                    // Отправляем клиенту ошибку
                    writer.write(JsonUtil.toJson(Response.error("Internal server error", 500)));
                    writer.newLine();
                    writer.flush();
                }
            }

        } catch (IOException e) {
            log.error("Ошибка в ходе обработки клиента ", e);
        }
    }
}
