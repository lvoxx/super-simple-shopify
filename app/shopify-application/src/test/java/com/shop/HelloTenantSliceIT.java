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
 * Proves the Phase 0 spine end to end against real Postgres + Redis (Testcontainers):
 * resolve tenant (header) -> bind {@code ScopedValue} -> route to shard -> read a tenant row ->
 * publish a domain event to the outbox -> the job-engine drains it to PROCESSED. Tagged
 * {@code integration} so it only runs where Docker is available (CI).
 */
@Tag("integration")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(ContainersConfig.class)
class HelloTenantSliceIT {

	@LocalServerPort
	int port;

	@Autowired
	JdbcConnectionDetails connection;

	private DataSource raw;

	@BeforeEach
	void applySchema() {
		// Schema is applied by Flyway here (as the infra migration container would), never by the
		// app. A plain datasource is used so migration does not depend on tenant routing.
		raw = DataSourceBuilder.create()
				.url(connection.getJdbcUrl())
				.username(connection.getUsername())
				.password(connection.getPassword())
				.driverClassName(connection.getDriverClassName())
				.build();
		Flyway.configure()
				.dataSource(raw)
				.locations("filesystem:../../db/migration/platform", "filesystem:../../db/migration/hello")
				.load()
				.migrate();
	}

	@Test
	void resolvesTenantReadsShardEmitsEventAndJobDrainsIt() throws Exception {
		var client = HttpClient.newHttpClient();
		var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/hello"))
				.header("X-Shop-Id", "1")
				.GET()
				.build();

		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("Hello from shop 1");
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
		throw new AssertionError("Outbox event was never drained to PROCESSED");
	}
}
