package edu.mayo.kmdp.repository.asset;

import edu.mayo.kmdp.repository.asset.server.Swagger2SpringBoot;
import org.junit.runner.RunWith;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IntegrationTestConfig.class)
@ActiveProfiles(profiles = "integration")
public abstract class IntegrationTestBase {

    private ConfigurableApplicationContext ctx;

    @org.junit.Before
    public void setupServer() {
        SpringApplication app = new SpringApplication(Swagger2SpringBoot.class);
        this.ctx = app.run();
    }

    @org.junit.After
    public void stopServer() {
        ctx.close();

        // TODO: Don't know why this is necessary, but seems to be.
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
