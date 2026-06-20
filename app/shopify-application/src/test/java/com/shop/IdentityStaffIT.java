package com.shop;

import static org.assertj.core.api.Assertions.assertThat;

import com.shop.platform.test.ContainersConfig;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

/**
 * Proves the Phase 1 identity slice end to end against real Postgres (Testcontainers): POST to the
 * tenant-scoped staff surface with {@code X-Shop-Id} -> the tenant filter binds {@code TenantContext} ->
 * the staff user + role rows are written to the routed shard -> {@code StaffInvited} is written to the
 * tenant outbox in the same transaction -> the job-engine drains it to PROCESSED. A read-back confirms
 * the roles round-trip through the collection mapping. Tagged {@code integration} (CI/Docker only).
 */
@Tag("integration")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(ContainersConfig.class)
class IdentityStaffIT {

	@LocalServerPort
	int port;

	@Autowired
	JdbcConnectionDetails connection;

	private DataSource raw;

	@BeforeEach
	void applySchema() {
		// Schema applied here as the infra migration container would — per-shard tenant locations.
		raw = DataSourceBuilder.create()
				.url(connection.getJdbcUrl())
				.username(connection.getUsername())
				.password(connection.getPassword())
				.driverClassName(connection.getDriverClassName())
				.build();
		Flyway.configure()
				.dataSource(raw)
				.locations("filesystem:../../db/migration/platform", "filesystem:../../db/migration/identity")
				.load()
				.migrate();
	}

	@Test
	void invitesStaffRoutesToShardReadsBackAndEmitsStaffInvited() throws Exception {
		var client = HttpClient.newHttpClient();
		String body = "{\"subject\":\"kc-sub-1\",\"displayName\":\"Ada\","
				+ "\"email\":\"ada@acme.example\",\"roles\":[\"OWNER\",\"ADMIN\"]}";
		var invite = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/staff"))
				.header("Content-Type", "application/json")
				.header("X-Shop-Id", "1")
				.POST(HttpRequest.BodyPublishers.ofString(body))
				.build();

		HttpResponse<String> created = client.send(invite, HttpResponse.BodyHandlers.ofString());

		assertThat(created.statusCode()).isEqualTo(201);
		assertThat(created.body()).contains("\"subject\":\"kc-sub-1\"");
		assertThat(created.body()).contains("\"status\":\"INVITED\"");

		var read = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/staff/kc-sub-1"))
				.header("X-Shop-Id", "1")
				.GET()
				.build();
		HttpResponse<String> fetched = client.send(read, HttpResponse.BodyHandlers.ofString());

		assertThat(fetched.statusCode()).isEqualTo(200);
		assertThat(fetched.body()).contains("OWNER").contains("ADMIN");
		awaitOutboxProcessed();
	}

	private void awaitOutboxProcessed() throws Exception {
		for (int i = 0; i < 50; i++) {
			try (var connection = raw.getConnection();
					var statement = connection.createStatement();
					var rs = statement.executeQuery(
							"SELECT count(*) FROM platform_outbox WHERE status = 'PROCESSED'")) {
				rs.next();
				if (rs.getInt(1) >= 1) {
					return;
				}
			}
			Thread.sleep(200);
		}
		throw new AssertionError("StaffInvited outbox event was never drained to PROCESSED");
	}
}
