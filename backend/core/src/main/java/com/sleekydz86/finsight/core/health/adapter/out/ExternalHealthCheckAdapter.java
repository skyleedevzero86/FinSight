package com.sleekydz86.finsight.core.health.adapter.out;

import com.sleekydz86.finsight.core.health.domain.vo.HealthStatus;
import com.sleekydz86.finsight.core.health.domain.port.out.ExternalHealthCheckPort;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import java.sql.Connection;
import java.net.HttpURLConnection;
import java.net.URL;

@Component
public class ExternalHealthCheckAdapter implements ExternalHealthCheckPort {

    private final DataSource dataSource;

    public ExternalHealthCheckAdapter(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public HealthStatus checkDatabaseHealth() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(5)) {
                return new HealthStatus("UP", "Database is healthy");
            } else {
                return new HealthStatus("DOWN", "Database connection validation failed");
            }
        } catch (Exception e) {
            return new HealthStatus("DOWN", "Database health check failed: " + e.getMessage());
        }
    }

    @Override
    public HealthStatus checkRedisHealth() {
        return new HealthStatus("UNKNOWN", "Redis health check not implemented");
    }

    @Override
    public HealthStatus checkExternalApiHealth(String apiName, String endpoint) {
        try {
            URL url = new URL("https://api.example.com" + endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                return new HealthStatus("UP", apiName + " API is healthy");
            } else {
                return new HealthStatus("DOWN", apiName + " API returned status: " + responseCode);
            }
        } catch (Exception e) {
            return new HealthStatus("DOWN", apiName + " API health check failed: " + e.getMessage());
        }
    }
}