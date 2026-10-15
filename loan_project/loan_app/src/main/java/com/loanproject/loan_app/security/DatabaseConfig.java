package com.loanproject.loan_app.security;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;

/**
 * Cloud Database Configuration (Railway / Render / Docker / Local).
 * Automatically parses cloud database URIs like postgresql://user:pass@host:port/db
 * into standard JDBC format jdbc:postgresql://host:port/db with separate credentials.
 * Supports DB_URL, DB_USERNAME, DB_PASSWORD, DATABASE_URL, and SPRING_DATASOURCE_URL.
 */
@Configuration
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @Value("${spring.datasource.url:${DB_URL:${SPRING_DATASOURCE_URL:${DATABASE_URL:jdbc:postgresql://localhost:5432/loan_db}}}}")
    private String rawUrl;

    @Value("${spring.datasource.username:${DB_USERNAME:${SPRING_DATASOURCE_USERNAME:postgres}}}")
    private String defaultUsername;

    @Value("${spring.datasource.password:${DB_PASSWORD:${SPRING_DATASOURCE_PASSWORD:admin}}}")
    private String defaultPassword;

    @Value("${spring.datasource.driver-class-name:org.postgresql.Driver}")
    private String driverClassName;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName(driverClassName);

        String url = rawUrl;
        String username = defaultUsername;
        String password = defaultPassword;

        // Automatically convert Render / Railway cloud database URLs (postgres:// or postgresql://) to JDBC format
        if (url != null && (url.startsWith("postgres://") || url.startsWith("postgresql://"))) {
            try {
                URI uri = new URI(url);
                String host = uri.getHost();
                int port = uri.getPort() == -1 ? 5432 : uri.getPort();
                String path = uri.getPath();

                if (uri.getUserInfo() != null) {
                    String[] userParts = uri.getUserInfo().split(":", 2);
                    username = userParts[0];
                    if (userParts.length > 1) {
                        password = userParts[1];
                    }
                }

                url = "jdbc:postgresql://" + host + ":" + port + path;
                log.info("Detected Railway/Render cloud database URL. Converted to JDBC format: jdbc:postgresql://{}:{}{}", host, port, path);
            } catch (Exception e) {
                log.warn("Failed to parse cloud database URI '{}', attempting prefix prepend fallback", url, e);
                if (!url.startsWith("jdbc:")) {
                    url = "jdbc:" + url;
                }
            }
        }

        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);

        return new HikariDataSource(config);
    }
}
