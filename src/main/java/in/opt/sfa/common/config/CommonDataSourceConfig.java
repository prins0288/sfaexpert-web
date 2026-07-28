package in.opt.sfa.common.config;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * COMMON database wiring.
 *
 * All repositories under in.opt.sfa.common.repository are bound to the
 * "commonEntityManagerFactory". These beans are exposed with the @Qualifier
 * "common" so they NEVER clash with the tenant beans (which are @Primary).
 *
 * -> To use the common DB anywhere, inject with @Qualifier("common").
 */
@Configuration
@EnableJpaRepositories(
        basePackages = { "in.opt.sfa.common.repository", "in.opt.sfa.theme.repository" },
        entityManagerFactoryRef = "commonEntityManagerFactory",
        transactionManagerRef = "commonTransactionManager"
)
public class CommonDataSourceConfig {

    @Bean(name = "commonDataSource")
    @Qualifier("common")
    @ConfigurationProperties("app.datasource.common")
    public DataSource commonDataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    @Bean(name = "commonEntityManagerFactory")
    @Qualifier("common")
    public LocalContainerEntityManagerFactoryBean commonEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("commonDataSource") DataSource dataSource) {

        Map<String, Object> props = new HashMap<>();
        props.put("hibernate.hbm2ddl.auto", "none");
        props.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");

        return builder
                .dataSource(dataSource)
                .packages("in.opt.sfa.common.entity", "in.opt.sfa.theme.entity")
                .persistenceUnit("common")
                .properties(props)
                .build();
    }

    @Bean(name = "commonTransactionManager")
    @Qualifier("common")
    public PlatformTransactionManager commonTransactionManager(
            @Qualifier("commonEntityManagerFactory") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }

    /**
     * JdbcTemplate on the COMMON datasource. Inject with @Qualifier("common").
     * e.g. public Svc(@Qualifier("common") JdbcTemplate jdbc) { ... }
     */
    @Bean(name = "commonJdbcTemplate")
    @Qualifier("common")
    public JdbcTemplate commonJdbcTemplate(@Qualifier("commonDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
