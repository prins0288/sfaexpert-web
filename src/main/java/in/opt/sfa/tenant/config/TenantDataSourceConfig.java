package in.opt.sfa.tenant.config;

import in.opt.sfa.tenant.datasource.TenantDataSourceManager;
import in.opt.sfa.tenant.datasource.TenantRoutingDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * TENANT database wiring.
 *
 * Repositories under in.opt.sfa.tenant.repository are bound to the (primary) tenant
 * EntityManagerFactory. Because these beans are @Primary you inject them WITHOUT
 * any @Qualifier -> they automatically route to whichever tenant is active.
 *
 * IMPORTANT: metadata access is disabled and the dialect is set explicitly so
 * Hibernate does NOT try to open a connection at application startup. The tenant
 * pool therefore stays closed until the first authenticated request/login.
 */
@Configuration
@EnableJpaRepositories(
        basePackages = { "in.opt.sfa.tenant.repository", "in.opt.sfa.menu.repository" },
        entityManagerFactoryRef = "tenantEntityManagerFactory",
        transactionManagerRef = "tenantTransactionManager"
)
public class TenantDataSourceConfig {

    @Bean(name = "tenantDataSource")
    @Primary
    public DataSource tenantDataSource(TenantDataSourceManager manager) {
        return new TenantRoutingDataSource(manager);
    }

    @Bean(name = "tenantEntityManagerFactory")
    @Primary
    public LocalContainerEntityManagerFactoryBean tenantEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("tenantDataSource") DataSource dataSource) {

        Map<String, Object> props = new HashMap<>();
        props.put("hibernate.hbm2ddl.auto", "none");
        props.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        // Prevent a startup connection just to read JDBC metadata:
        props.put("hibernate.boot.allow_jdbc_metadata_access", false);
        props.put("hibernate.temp.use_jdbc_metadata_defaults", false);

        return builder
                .dataSource(dataSource)
                .packages("in.opt.sfa.tenant.entity", "in.opt.sfa.menu.entity")
                .persistenceUnit("tenant")
                .properties(props)
                .build();
    }

    @Bean(name = "tenantTransactionManager")
    @Primary
    public PlatformTransactionManager tenantTransactionManager(
            @Qualifier("tenantEntityManagerFactory") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }

    /**
     * @Primary JdbcTemplate on the tenant routing datasource. Inject it WITHOUT
     * any qualifier and it auto-routes to whichever tenant is bound to the
     * current request (TenantContext). Only usable inside an authenticated request.
     */
    @Bean(name = "tenantJdbcTemplate")
    @Primary
    public JdbcTemplate tenantJdbcTemplate(@Qualifier("tenantDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
