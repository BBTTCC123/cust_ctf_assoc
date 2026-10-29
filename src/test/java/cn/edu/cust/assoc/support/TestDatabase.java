package cn.edu.cust.assoc.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MySQLContainer;

/**
 * 集成测试数据库。
 *
 * 默认起一个 Testcontainers MySQL（CI、装了 Docker 的本机）；只要显式传入
 * {@code -Dit.datasource.url=...}（以及可选的 username/password），
 * 就改为连接外部 MySQL，方便没有 Docker 的开发机用本机实例跑同一套用例。
 * 外部库必须是专门给测试用的空库（名字建议以 _it 结尾），用例会直接建表并写数据。
 */
public final class TestDatabase {

    /** 和 V1-V8 的验证环境保持一致，避免 MySQL 8.0.x 小版本差异干扰。 */
    public static final String MYSQL_IMAGE = "mysql:8.0.43";

    private static final String DB_NAME = "assoc_it";
    private static final String DB_USER = "assoc_it";
    private static final String DB_PASSWORD = "assoc_it_pw";

    private static final MySQLContainer<?> CONTAINER = new MySQLContainer<>(MYSQL_IMAGE)
            .withDatabaseName(DB_NAME)
            .withUsername(DB_USER)
            .withPassword(DB_PASSWORD);

    private static Boolean dockerAvailable;

    private TestDatabase() {
    }

    /** 当前是否走外部数据库（无 Docker 的本机）。 */
    public static boolean isExternal() {
        return !externalUrl().isBlank();
    }

    /**
     * 是否具备跑集成测试的条件：外部库已配置，或者 Docker 可用。
     * 结果缓存，避免每个测试类都去探测一次 Docker。
     */
    public static boolean isAvailable() {
        if (isExternal()) {
            return true;
        }
        synchronized (TestDatabase.class) {
            if (dockerAvailable == null) {
                try {
                    dockerAvailable = DockerClientFactory.instance().isDockerAvailable();
                } catch (Throwable e) {
                    // 没装 Docker、Docker Desktop 没启动、socket 权限不足都算不可用
                    dockerAvailable = false;
                }
            }
            return dockerAvailable;
        }
    }

    /** Spring 上下文共用的容器；由 spring-boot-testcontainers 负责启动和停止。 */
    public static MySQLContainer<?> container() {
        return CONTAINER;
    }

    public static void registerExternalDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", TestDatabase::externalUrl);
        registry.add("spring.datasource.username", TestDatabase::externalUsername);
        registry.add("spring.datasource.password", TestDatabase::externalPassword);
    }

    public static String externalUrl() {
        return property("it.datasource.url", "IT_DATASOURCE_URL");
    }

    public static String externalUsername() {
        String value = property("it.datasource.username", "IT_DATASOURCE_USERNAME");
        return value.isBlank() ? DB_USER : value;
    }

    public static String externalPassword() {
        String value = property("it.datasource.password", "IT_DATASOURCE_PASSWORD");
        return value.isBlank() ? DB_PASSWORD : value;
    }

    /** 把 JDBC URL 里的库名换成另一个库（迁移测试要在独立的空库上执行 V1-V8）。 */
    public static String urlWithDatabase(String jdbcUrl, String database) {
        return jdbcUrl.replaceFirst("(jdbc:mysql://[^/]+)/[^?]+", "$1/" + database);
    }

    private static String property(String systemKey, String envKey) {
        String value = System.getProperty(systemKey);
        if (value == null || value.isBlank()) {
            value = System.getenv(envKey);
        }
        return value == null ? "" : value.trim();
    }
}
