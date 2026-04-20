package org.example.integration.controller.config;

import org.example.integration.AbstractIntegrationTest;
import org.example.server.Server;
import org.example.server.ServerConfig;
import org.example.service.AuthenticationService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AbstractUserControllerIntegrationTest extends AbstractIntegrationTest {
    protected ServerConfig serverConfig;
    protected Server server;
    protected int port;

    protected AuthenticationService authenticationService;

    public AbstractUserControllerIntegrationTest() {
        this.authenticationService = new AuthenticationService();
    }

    @BeforeAll
    void initServer() {
        port = 8081;
        serverConfig = ServerConfig.customHibernateSessionFactoryServerConfig(port, sessionFactory);
        server = serverConfig.getServer();
    }
}
