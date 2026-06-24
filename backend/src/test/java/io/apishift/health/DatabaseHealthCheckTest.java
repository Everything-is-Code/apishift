package io.apishift.health;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DatabaseHealthCheckTest {

    @Test
    void up_whenDatabaseReachable() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metadata);
        when(metadata.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(connection.isValid(5)).thenReturn(true);

        DatabaseHealthCheck check = new DatabaseHealthCheck();
        setField(check, "dataSource", dataSource);

        HealthCheckResponse response = check.call();
        assertEquals(HealthCheckResponse.Status.UP, response.getStatus());
        assertEquals("database", response.getName());
        assertEquals("PostgreSQL", response.getData().get().get("database"));
        assertEquals(true, response.getData().get().get("valid"));
    }

    @Test
    void down_whenDatabaseUnreachable() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection refused"));

        DatabaseHealthCheck check = new DatabaseHealthCheck();
        setField(check, "dataSource", dataSource);

        HealthCheckResponse response = check.call();
        assertEquals(HealthCheckResponse.Status.DOWN, response.getStatus());
        assertEquals("Connection refused", response.getData().get().get("error"));
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
