package vn.edu.ut.its;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import vn.edu.ut.its.config.DataSeeder;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "its.demo.seed=false")
class DemoSeedDisabledTest {

    @Autowired
    private ApplicationContext application;

    @Test
    void leavesExistingDataAloneWhenDemoSeedingIsDisabled() {
        assertTrue(application.getBeansOfType(DataSeeder.class).isEmpty());
    }
}
