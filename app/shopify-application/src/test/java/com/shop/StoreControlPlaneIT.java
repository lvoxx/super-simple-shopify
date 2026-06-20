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
 * Proves the Phase 1 control-plane spine end to end against real Postgres (Testcontainers): POST to the
 * non-tenant control surface -> the shop is inserted into the global registry on the control datasource
 * -> a shard is assigned -> {@code ShopCreated} is written to the control outbox in the same
 * transaction -> the job-engine drains it to PROCESSED. Tagged {@code integration} (CI/Docker only).
 */
@Tag("integration")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(ContainersConfig.class)
class StoreControlPlaneIT {

	@LocalServerPort
	int port;

	@Autowired
	JdbcConnectionDetails connection;

	private DataSource raw;

	@BeforeEach
	void applySchema() {
		// Schema applied here as the infra migration container would — control + per-shard locations.
		raw = DataSourceBuilder.create()
				.url(connection.getJdbcUrl())
				.username(connection.getUsername())
				.password(connection.getPassword())
				.driverClassName(connection.getDriverClassName())
				.build();
		Flyway.configure()
				.dataSource(raw)
				.locations("filesystem:../../db/migration/platform", "filesystem:../../db/migration/control")
				.load()
				.migrate();
	}

	@Test
	void createsShopAssignsShardAndEmitsShopCreated() throws Exception {
		var client = HttpClient.newHttpClient();
		String body = "{\"name\":\"Acme\",\"plan\":\"PRO\",\"locale\":\"en-US\",\"primaryDomain\":\"acme.example\"}";
		var create = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/control/shops"))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(body))
				.build();

		HttpResponse<String> response = client.send(create, HttpResponse.BodyHandlers.ofString());

		assertThat(response.statusCode()).isEqualTo(201);
		assertThat(response.body()).contains("\"primaryDomain\":\"acme.example\"");
		assertThat(response.body()).contains("\"shardIndex\":0");
		awaitControlOutboxProcessed();
	}

	private void awaitControlOutboxProcessed() throws Exception {
		for (int i = 0; i < 50; i++) {
			try (var connection = raw.getConnection();
					var statement = connection.createStatement();
					var rs = statement.executeQuery(
							"SELECT count(*) FROM control_outbox WHERE status = 'PROCESSED'")) {
				rs.next();
				if (rs.getInt(1) >= 1) {
					return;
				}
			}
			Thread.sleep(200);
		}
		throw new AssertionError("ShopCreated control-outbox event was never drained to PROCESSED");
	}
}
