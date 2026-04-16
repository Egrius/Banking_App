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

@Slf4j
public class Server {
    private final RequestDispatcher dispatcher;
    private final int port;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public Server(RequestDispatcher dispatcher, int port) {
        this.dispatcher = dispatcher;
        this.port = port;
    }

    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            log.info("Сервер запущен на порту " + port);
            System.out.println("--- СТАРТ ---");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                executor.submit(() -> handleClient(clientSocket));
            }
        }
    }

    private void handleClient(Socket clientSocket) {
        try (clientSocket;
             BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8))) {

            // 1. Читаем JSON из reader
            // 2. Парсим в Request
            // может быть брешь в том, что json не заканчивается на \n

            String jsonLine = reader.readLine();

            log.debug("Raw JSON received: '{}'", jsonLine);

            if (jsonLine == null || jsonLine.isBlank()) {
                log.warn("Пустой запрос от клиента");
                return;
            }

            Request request = JsonUtil.convert(jsonLine, Request.class);
            log.debug("Пришел запрос: {}", request);

            Response response = dispatcher.dispatch(request);
            log.debug("Получен ответ: {}", response);

            // 4. Сериализуем Response в JSON
            String responseJson = JsonUtil.toJson(response);

            // 5. Пишем в writer
            writer.write(responseJson);
            writer.newLine();
            writer.flush();

        } catch (IOException e) {
            log.error("Ошибка в ходе обработки клиента ", e);
        }
    }
}
