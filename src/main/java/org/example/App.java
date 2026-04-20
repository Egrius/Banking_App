package org.example;

import org.example.server.ServerApplication;
import org.example.server.ServerConfig;
import org.example.service.AuditService;
import org.example.service.IdempotencyService;
import org.example.util.ValidatorUtil;

import java.io.IOException;


// TODO: Написать тесты для контроллеров в связке со всей цепочкой
// TODO: Покрыть транзакционный сервис многопоточными тестами на блокировки

/*

{"command":"user.register","payload":{"type":"user.register", "firstName":"John","lastName":"Doe","rawPassword":"12345","email":"john@test.com"}}

 */
public class App 
{
    public static void main( String[] args ) throws IOException
    {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ValidatorUtil.close();
            IdempotencyService.close();
            AuditService.shutdown();
        }));

        ServerApplication serverApplication = new ServerApplication();
        ServerConfig config = ServerConfig.defaultServerConfig(8080);

        serverApplication.start(config);
    }
}