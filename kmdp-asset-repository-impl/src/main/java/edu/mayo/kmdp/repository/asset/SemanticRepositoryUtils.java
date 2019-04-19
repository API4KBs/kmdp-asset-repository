package edu.mayo.kmdp.repository.asset;

import java.io.IOException;
import java.net.URI;
import java.net.URLConnection;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SemanticRepositoryUtils {

    private static final Logger logger = LoggerFactory.getLogger(SemanticRepositoryUtils.class);

    public static Optional<byte[]> resolve(URI uri) {
        try {
            System.err.println( "RESOLVING SEMREPO " + uri );
            URLConnection connection = adjustUrlConnection(uri.toURL().openConnection());

            byte[] bytes = IOUtils.toByteArray(connection.getInputStream());

            return Optional.ofNullable(bytes);
        } catch (IOException e) {
            logger.warn("Cannot resolve artifact", e);
            return Optional.empty();
        }
    }

    private static URLConnection adjustUrlConnection(URLConnection connection) {
        if(StringUtils.startsWith(connection.getURL().toString(), "https://api.swaggerhub.com/apis/")) {
            connection.setRequestProperty("Authorization", Mea3Config.getSwaggerhubApiKey());
        }

        return connection;
    }

}
