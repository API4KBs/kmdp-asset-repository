package edu.mayo.kmdp.repository.asset;

import org.springframework.util.StringUtils;

import java.io.File;

public class Mea3Config {

    public static String getRepositoryUrl() {
        return System.getProperty("REPOSITORY_URL");
    }

    public static String getSwaggerhubApiKey() {
        return System.getProperty("SWAGGERHUB_API_KEY");
    }

    public static File getConfigDir() {
        File home;

        String mea3Home = System.getProperty("MEA3_HOME");

        if(StringUtils.isEmpty(mea3Home)) {
            home = new File(System.getProperty("user.home"), ".mea3");
            home.mkdir();
        } else {
            home = new File(mea3Home);
        }

        return home;
    }
}
