package com.shop.platform.persistence;

import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Wires the <strong>tenant</strong> persistence stack: the shard-routing {@link DataSource} plus the
 * MyBatis {@code SqlSessionFactory}/{@code SqlSessionTemplate}/transaction manager bound to it, and a
 * mapper scanner that registers every {@code @Mapper} interface against that factory.
 *
 * <p>Because the control plane ({@link ControlPlaneConfig}) introduces a <em>second</em>
 * {@code DataSource}, Spring Boot's single-candidate MyBatis/DataSource/transaction autoconfiguration
 * backs off — so the tenant stack that was autoconfigured in Phase 0 is now declared explicitly here.
 * The {@code @Primary} beans are the tenant ones: an unqualified {@code @Transactional} or injected
 * {@code SqlSessionTemplate} resolves to the shard-routed datasource. {@code @ControlMapper} mappers
 * are excluded here (the scanner filters on {@code @Mapper}) and wired by {@link ControlPlaneConfig}.
 *
 * <p>Phase 0 ({@code shard-count=1}) backs every shard index with one physical datasource; Phase 8
 * supplies a distinct datasource per shard from config.
 */
@Configuration
@EnableConfigurationProperties(ShardingProperties.class)
public class PersistenceConfig {

	static final String MAPPER_LOCATIONS = "classpath*:com/shop/**/*.xml";

	@Bean
	public ShardResolver shardResolver(ShardingProperties properties) {
		return new ShardResolver(properties.getShardCount());
	}

	@Bean
	@Primary
	public DataSource shardRoutingDataSource(ShardResolver shardResolver, JdbcConnectionDetails connection) {
		DataSource shardZero = DataSourceBuilder.create()
				.url(connection.getJdbcUrl())
				.username(connection.getUsername())
				.password(connection.getPassword())
				.driverClassName(connection.getDriverClassName())
				.build();
		Map<Object, Object> targets = new HashMap<>();
		for (int shard = 0; shard < shardResolver.shardCount(); shard++) {
			targets.put(shard, shardZero);
		}
		var routing = new TenantRoutingDataSource(shardResolver);
		routing.setTargetDataSources(targets);
		routing.setDefaultTargetDataSource(shardZero);
		routing.afterPropertiesSet();
		return routing;
	}

	@Bean
	@Primary
	public SqlSessionFactory tenantSqlSessionFactory(DataSource shardRoutingDataSource) throws Exception {
		return MyBatisFactories.build(shardRoutingDataSource, MAPPER_LOCATIONS);
	}

	@Bean
	@Primary
	public SqlSessionTemplate tenantSqlSessionTemplate(SqlSessionFactory tenantSqlSessionFactory) {
		return new SqlSessionTemplate(tenantSqlSessionFactory);
	}

	@Bean
	@Primary
	public PlatformTransactionManager tenantTransactionManager(DataSource shardRoutingDataSource) {
		return new DataSourceTransactionManager(shardRoutingDataSource);
	}

	/**
	 * Registers every {@code @Mapper} interface against the tenant factory. Declared {@code static} so
	 * the bean-factory post-processor runs before regular beans are instantiated.
	 */
	@Bean
	public static MapperScannerConfigurer tenantMapperScanner() {
		var scanner = new MapperScannerConfigurer();
		scanner.setBasePackage("com.shop");
		scanner.setAnnotationClass(Mapper.class);
		scanner.setSqlSessionFactoryBeanName("tenantSqlSessionFactory");
		return scanner;
	}

	/** Shared MyBatis {@code SqlSessionFactory} assembly so the tenant and control stacks stay identical. */
	static final class MyBatisFactories {

		private MyBatisFactories() {
		}

		static SqlSessionFactory build(DataSource dataSource, String mapperLocations) throws Exception {
			var factory = new SqlSessionFactoryBean();
			factory.setDataSource(dataSource);
			factory.setMapperLocations(new PathMatchingResourcePatternResolver().getResources(mapperLocations));
			var configuration = new org.apache.ibatis.session.Configuration();
			configuration.setMapUnderscoreToCamelCase(true);
			factory.setConfiguration(configuration);
			return factory.getObject();
		}
	}
}
