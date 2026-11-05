package com.vasav.springtalentengine.shared;

import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

class TenantAwareDataSource extends DelegatingDataSource {

    private static final String APPLY_CONTEXT =
            "SELECT set_config('app.tenant_id', ?, false), set_config('app.user_email', ?, false)";

    TenantAwareDataSource(DataSource target) {
        super(target);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return applyContext(super.getConnection());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return applyContext(super.getConnection(username, password));
    }

    private Connection applyContext(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(APPLY_CONTEXT)) {
            statement.setString(1, TenantContext.current().map(UUID::toString).orElse(""));
            statement.setString(2, CurrentUser.email().orElse(""));
            statement.execute();
            return connection;
        } catch (SQLException e) {
            connection.close();
            throw e;
        }
    }
}
