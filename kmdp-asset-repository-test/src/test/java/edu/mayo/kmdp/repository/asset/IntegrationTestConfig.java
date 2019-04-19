package edu.mayo.kmdp.repository.asset;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@ComponentScan
@Profile("integration")
class IntegrationTestConfig {


}
