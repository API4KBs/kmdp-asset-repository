package edu.mayo.kmdp.repository.asset;

import com.zaxxer.hikari.HikariDataSource;
import edu.mayo.kmdp.health.datatype.ApplicationComponent;
import edu.mayo.kmdp.health.datatype.MiscProperties;
import edu.mayo.kmdp.health.datatype.Status;
import edu.mayo.kmdp.health.utils.MonitorUtil;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;


@Configuration
@PropertySource(value = {"classpath:application.properties"})
public class MonitoringConfig {

  @Bean
  Supplier<ApplicationComponent> artifactDB(@Autowired DataSource dataSource) {
    MiscProperties details = new MiscProperties();
    details.put("type", dataSource.getClass().getSimpleName());
    if (dataSource instanceof HikariDataSource) {
      HikariDataSource hkds = (HikariDataSource) dataSource;
      details.put("user", hkds.getUsername());
      details.put("password", MonitorUtil.obfuscate(hkds.getPassword(), 4));
      details.put("jdbcUrl", hkds.getJdbcUrl());
      details.put("jdbcDriver", hkds.getDriverClassName());
      details.put("connectionTimeout", Long.toString(hkds.getConnectionTimeout()));
      details.put("poolSize", Long.toString(hkds.getMaximumPoolSize()));
    }

    return () -> {
      ApplicationComponent c = new ApplicationComponent();
      c.setName("JPA Artifact Repository");
      try (Connection conn = dataSource.getConnection()) {
        if (conn.isClosed()) {
          c.setStatusMessage("Connection is closed");
          c.setStatus(Status.DOWN);
        } else {
          c.status(Status.UP);
        }
        c.setDetails(details);
      } catch (SQLException e) {
        e.printStackTrace();
        c.setStatusMessage(e.getMessage());
        c.setStatus(Status.DOWN);
      }
      return c;
    };
  }

  @Bean
  public Predicate<String> featureFlags() {
    return "allowClearAll"::equals;
  }

}
