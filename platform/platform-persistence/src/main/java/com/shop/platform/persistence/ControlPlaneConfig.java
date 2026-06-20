package com.shop.platform.persistence;

import javax.sql.DataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Wires the <strong>control plane</strong>: a single, non-sharded {@link DataSource} and the MyBatis
 * stack bound to it, used for global data that cannot be sharded by {@code shop_id} because it is how
 * a shop is resolved to a shard in the first place — the shop registry, the shop&rarr;shard map, and
 * the control outbox.
 *
 * <p>Mappers opt into this stack with {@link ControlMapper @ControlMapper}; {@link #controlMapperScanner()}
 * registers them against {@code controlSqlSessionFactory}. None of these beans are {@code @Primary} —
 * the tenant stack in {@link PersistenceConfig} owns that — so control-plane writes must use the named
 * {@code controlTransactionManager} (e.g. {@code @Transactional("controlTransactionManager")}).
 *
 * <p>Connection settings come from {@link ControlPlaneProperties}; when none are given (local/dev and
 * the single-node Testcontainers setup) the control plane shares shard 0's physical Postgres via the
 * ambient {@link JdbcConnectionDetails}.
 */
@Configuration
@EnableConfigurationProperties(ControlPlaneProperties.class)
public class ControlPlaneConfig {

	public static final String TRANSACTION_MANAGER = "controlTransactionManager";

	@Bean
	public DataSource controlDataSource(ControlPlaneProperties properties, JdbcConnectionDetails connection) {
		var builder = DataSourceBuilder.create();
		if (properties.hasExplicitConnection()) {
			builder.url(properties.getUrl())
					.username(properties.getUsername())
					.password(properties.getPassword())
					.driverClassName(properties.getDriverClassName() != null
							? properties.getDriverClassName() : connection.getDriverClassName());
		}
		else {
			builder.url(connection.getJdbcUrl())
					.username(connection.getUsername())
					.password(connection.getPassword())
					.driverClassName(connection.getDriverClassName());
		}
		return builder.build();
	}

	@Bean
	public SqlSessionFactory controlSqlSessionFactory(DataSource controlDataSource) throws Exception {
		return PersistenceConfig.MyBatisFactories.build(controlDataSource, PersistenceConfig.MAPPER_LOCATIONS);
	}

	@Bean
	public SqlSessionTemplate controlSqlSessionTemplate(SqlSessionFactory controlSqlSessionFactory) {
		return new SqlSessionTemplate(controlSqlSessionFactory);
	}

	@Bean(TRANSACTION_MANAGER)
	public PlatformTransactionManager controlTransactionManager(DataSource controlDataSource) {
		return new DataSourceTransactionManager(controlDataSource);
	}

	/**
	 * Registers every {@link ControlMapper @ControlMapper} interface against the control factory.
	 * {@code static} so it runs as a bean-factory post-processor before regular beans instantiate.
	 */
	@Bean
	public static MapperScannerConfigurer controlMapperScanner() {
		var scanner = new MapperScannerConfigurer();
		scanner.setBasePackage("com.shop");
		scanner.setAnnotationClass(ControlMapper.class);
		scanner.setSqlSessionFactoryBeanName("controlSqlSessionFactory");
		return scanner;
	}
}
