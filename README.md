# Multi-Tenant Login System (Spring Boot 3, Java 17)

Ek common database (users + tenant connection details) aur har tenant ka apna
alag database. User login karta hai → uska tenant common DB se milta hai → us
tenant ka datasource **login ke waqt** banta hai → aage ke saare requests
automatically usi tenant ke DB pe route hote hain. Koi tenant 30 minute tak idle
rahe to uska datasource **auto-close** ho jata hai.

## Architecture

```
                 ┌─────────────────────────────┐
   login  ─────► │  AuthService (COMMON DB)     │
                 │  - app_user check (bcrypt)   │
                 │  - tenantId nikalo           │
                 │  - tenant_config nikalo      │
                 │  - tenant datasource OPEN     │──┐
                 └─────────────────────────────┘  │
                          │ JWT { sub, tenant }    │
                          ▼                        ▼
   request ─► TenantAuthFilter ─► TenantContext ─► TenantRoutingDataSource
             (JWT se tenant)      (ThreadLocal)     │
                                                    ▼
                                      TenantDataSourceManager
                                      - pool lazily banao
                                      - last-access track karo
                                      - 30 min idle → close (scheduler)
```

### Qualifier design (jaisa aapne maanga)
- **Common** beans `@Qualifier("common")` ke saath expose hote hain
  (`commonDataSource`, `commonEntityManagerFactory`, `commonTransactionManager`).
- **Tenant** beans `@Primary` hain — isliye tenant repos ko inject karte waqt
  **koi qualifier nahi** lagता.
- Repos konse EMF se bind honge, ye `@EnableJpaRepositories(basePackages=...)`
  decide karta hai:
  - `com.app.common.repository`  → common EMF
  - `com.app.tenant.repository`  → tenant (primary) EMF

  Isliye repository **interface** pe `@Qualifier` likhne ki zaroorat nahi —
  package hi routing kar deta hai. Qualifier sirf datasource/EMF layer pe hai.

### Tenant datasource sirf login pe kyu khulta hai?
`tenantEntityManagerFactory` me ye set hai:
```
hibernate.boot.allow_jdbc_metadata_access = false
hibernate.temp.use_jdbc_metadata_defaults = false
hibernate.dialect = ...MySQLDialect (explicit)
```
Isse Hibernate startup pe koi connection nahi kholta. Pehla connection tabhi
banta hai jab `TenantDataSourceManager.getDataSource(tenantId)` call hota hai —
yaani login pe (ya us tenant ki pehli request pe).

### 30-minute auto-close
`TenantDataSourceManager` har access pe `lastAccess` timestamp update karta hai.
`@Scheduled(fixedDelay = 60s)` job un pools ko band kar deta hai jo
`app.tenant.idle-timeout-minutes` (default 30) se zyada idle hain. Agli request
pe pool dobara ban jata hai.

## Setup

1. **MySQL me schema banao** (`src/main/resources/sql/`):
   ```
   mysql -u root -p < src/main/resources/sql/01_common_db.sql
   mysql -u root -p < src/main/resources/sql/02_tenant_dbs.sql
   ```
2. **application.yml** me common DB ka url/user/pass aur JWT secret set karo.
3. **Run**: `mvn spring-boot:run`

> Lombok use hua hai — apne IDE me *Enable annotation processing* on kar dena.
> PostgreSQL chahiye to `mysql-connector-j` ko `postgresql` se replace karo aur
> dialect/URLs update kar do.

## Try it (curl)

```bash
# alice -> tenant_a
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"password123"}' | jq -r .token)

# tenant_a ka data aayega
curl -s localhost:8080/api/products -H "Authorization: Bearer $TOKEN"

# bob -> tenant_b (dusra DB, dusra data)
TOKEN_B=$(curl -s -X POST localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"bob","password":"password123"}' | jq -r .token)

curl -s localhost:8080/api/products -H "Authorization: Bearer $TOKEN_B"
```

Log me dikhega:
```
Opened datasource for tenant 'tenant_a' (active tenants: 1)
Tenant 'tenant_a' datasource opened and verified on login
...
Auto-closed idle datasource for tenant 'tenant_a' (idle > 1800000 ms)
```

## Naye user ka password hash

BCrypt hash banane ke liye (strength 10):
```java
new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("yourPassword");
```
Ya kisi bhi online BCrypt tool se `$2a/$2b` hash le lo — Spring dono accept karta hai.

## Notes / production tips
- `tenant_config.db_password` ko encrypt karke store karo (e.g. Jasypt).
- Testing ke liye idle-timeout chhota rakh lo (e.g. 2 min) taaki auto-close jaldi dikhe.
- Ek hi request me common + tenant dono repos chahiye to sahi transaction manager
  choose karo: `@Transactional("commonTransactionManager")` vs
  `@Transactional("tenantTransactionManager")` (dono ek saath ek transaction me
  nahi aayenge — alag datasources hain).
