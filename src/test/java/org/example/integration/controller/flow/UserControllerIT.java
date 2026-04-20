package org.example.integration.controller.flow;

import jakarta.persistence.EntityManager;
import org.example.dto.Response;
import org.example.dto.user.UserLoginReadDto;
import org.example.dto.user.UserReadDto;
import org.example.entity.Role;
import org.example.entity.User;
import org.example.integration.controller.config.AbstractUserControllerIntegrationTest;
import org.example.security.AuthContext;
import org.example.service.AuditService;
import org.example.service.IdempotencyService;
import org.example.util.JsonUtil;
import org.example.util.ValidatorUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UserControllerIT extends AbstractUserControllerIntegrationTest {

    @BeforeAll
    void initServer() throws InterruptedException {

        runSql("/role_service/init_role_table.sql");

        Thread serverThread = new Thread(() -> {
            try {
                server.start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        serverThread.start();
        Thread.sleep(500);
    }

    @AfterAll
    void close() {
        server.stop();
        ValidatorUtil.close();
        IdempotencyService.close();
        AuditService.shutdown();
    }

    @Test
    void registrationAndLoginSuccessFlow() {
        try(Socket socket = new Socket("localhost", 8081);) {

            // Проверка регистрации

            String registerJson = """
                    {
                    "command":"user.register",
                    "payload":{
                        "type":"user.register", 
                        "firstName":"John",
                        "lastName":"Doe",
                        "rawPassword":"12345",
                        "email":"john@test.com"
                        }
                    }
                    """.replace("\n", "").concat("\n");

            BufferedOutputStream writer = new BufferedOutputStream(socket.getOutputStream());

            writer.write(registerJson.getBytes(StandardCharsets.UTF_8));

            writer.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String responseJson =  reader.readLine();

            Response response = JsonUtil.fromJson(responseJson, Response.class);

            UserReadDto responseDto = JsonUtil.convert(response.getData(), UserReadDto.class);

            assertEquals(200, response.getStatusCode());

            assertEquals("John", responseDto.firstName());
            assertEquals("Doe", responseDto.lastName());
            assertEquals("john@test.com", responseDto.email());
            assertEquals(1, responseDto.roles().size());
            assertEquals("USER", responseDto.roles().getFirst().roleName());

            EntityManager em = sessionFactory.createEntityManager();
            User createdUser = em.createQuery("SELECT u FROM User u WHERE u.email = 'john@test.com'", User.class)
                    .getSingleResult();

            assertEquals(responseDto.firstName(), createdUser.getFirstName());
            assertEquals(responseDto.lastName(), createdUser.getLastName());
            assertEquals(responseDto.email(), createdUser.getEmail());

            Iterator<Role> iter = createdUser.getRoles().iterator();

            assertEquals(responseDto.roles().getFirst().roleName(), iter.next().getName());
            assertFalse(iter.hasNext());

            // Проверка логина

            String loginJson = """
                    {
                    "command":"user.login",
                    "payload":{
                        "type":"user.login",
                        "email":"john@test.com",
                        "rawPassword":"12345"
                        }
                    }
                    """.replace("\n", "").concat("\n");

            writer.write(loginJson.getBytes(StandardCharsets.UTF_8));
            writer.flush();

            String responseLoginJson = reader.readLine();

            Response responseLogin = JsonUtil.fromJson(responseLoginJson, Response.class);

            UserLoginReadDto responseLoginDto = JsonUtil.convert(responseLogin.getData(), UserLoginReadDto.class);

            String jwtToken = responseLoginDto.jwtToken();
            assertFalse(jwtToken.isBlank());
            assertEquals(responseLoginDto.userReadDto().firstName(), createdUser.getFirstName());
            assertEquals(responseLoginDto.userReadDto().lastName(), createdUser.getLastName());
            assertEquals(responseLoginDto.userReadDto().email(), createdUser.getEmail());


            AuthContext authFromToken = authenticationService.getAuthContextFromToken(jwtToken);
            assertEquals(createdUser.getId(), authFromToken.getUserId());
            assertEquals(createdUser.getEmail(), authFromToken.getEmail());
            assertTrue(authFromToken.getRoles().contains("USER"));

            // Проверка корректной обработки JWT токена и прохождение фильтров

            String findByIdJson = """
                {
                "command":"user.findById.%s",
                "headers":{
                    "Authorization":"Bearer %s"
                    }
                }
            """.formatted(createdUser.getId(), jwtToken)
                    .replace("\n", "")
                    .concat("\n");

            writer.write(findByIdJson.getBytes(StandardCharsets.UTF_8));
            writer.flush();

            String findByIdResponse = reader.readLine();
            Response findByIdResp = JsonUtil.fromJson(findByIdResponse, Response.class);
            UserReadDto foundUser = JsonUtil.convert(findByIdResp.getData(), UserReadDto.class);

            assertEquals(createdUser.getId(), foundUser.id());
            assertEquals(createdUser.getEmail(), foundUser.email());


            writer.close();
            reader.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }
}
