package cn.edu.cust.assoc.migration;

import cn.edu.cust.assoc.support.TestDatabase;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.testcontainers.containers.MySQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * V1-V10 必须在全新空库上一次性执行成功。
 *
 * 这里不复用 Spring 上下文：单独建一个临时库（容器或外部实例），
 * 从零跑 Flyway，再把关键表、唯一约束和列型逐项查出来。
 * 这样既能证明「空库可迁移」，也不会污染应用集成测试所在的库。
 */
@EnabledIf("cn.edu.cust.assoc.support.TestDatabase#isAvailable")
class FlywayMigrationIT {

    private static final String PROBE_DB =
            "assoc_migration_probe_" + Long.toString(System.currentTimeMillis(), 36);

    /** V1-V8 新增加的 29 张业务表（不含 flyway_schema_history）。 */
    private static final Set<String> EXPECTED_TABLES = Set.of(
            "user", "role", "user_role", "member_profile", "team", "team_member",
            "registration_invite", "password_reset_token",
            "competition",
            "award", "team_award", "award_share",
            "media",
            "points_rule", "vulnerability", "points_request", "points_ledger", "points_appeal",
            "article", "sensitive_word",
            "announcement", "honor", "recruit_application",
            "audit_log", "media_link", "article_tag", "comment", "favorite",
            "team_award_dedupe_log", "team_member_dedupe_log");

    private static MySQLContainer<?> container;
    private static String jdbcUrl;
    private static String username;
    private static String password;

    @BeforeAll
    static void prepareEmptyDatabase() throws Exception {
        if (TestDatabase.isExternal()) {
            username = TestDatabase.externalUsername();
            password = TestDatabase.externalPassword();
            try (Connection connection = DriverManager.getConnection(TestDatabase.externalUrl(), username, password);
                 Statement statement = connection.createStatement()) {
                statement.execute("DROP DATABASE IF EXISTS " + PROBE_DB);
                statement.execute("CREATE DATABASE " + PROBE_DB
                        + " CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
            }
            jdbcUrl = TestDatabase.urlWithDatabase(TestDatabase.externalUrl(), PROBE_DB);
        } else {
            container = new MySQLContainer<>(TestDatabase.MYSQL_IMAGE)
                    .withDatabaseName(PROBE_DB)
                    .withUsername("assoc_migration")
                    .withPassword("assoc_migration_pw");
            container.start();
            jdbcUrl = container.getJdbcUrl();
            username = container.getUsername();
            password = container.getPassword();
        }
    }

    @AfterAll
    static void dropProbeDatabase() throws Exception {
        if (TestDatabase.isExternal()) {
            try (Connection connection = DriverManager.getConnection(TestDatabase.externalUrl(), username, password);
                 Statement statement = connection.createStatement()) {
                statement.execute("DROP DATABASE IF EXISTS " + PROBE_DB);
            }
        } else if (container != null) {
            container.stop();
        }
    }

    @Test
    void migrationsRunOnEmptyDatabase() throws Exception {
        assertEmptyDatabase();

        Flyway flyway = Flyway.configure()
                .dataSource(jdbcUrl, username, password)
                .locations("classpath:db/migration")
                .load();
        assertThat(flyway.info().current()).isNull();

        MigrateResult result = flyway.migrate();

        // V1-V8 必须全部执行成功；后续新增迁移继续顺延执行
        assertThat(result.migrationsExecuted).isGreaterThanOrEqualTo(8);
        assertThat(result.success).isTrue();

        MigrationInfo[] applied = flyway.info().applied();
        assertThat(applied).hasSizeGreaterThanOrEqualTo(8);
        List<String> versions = new ArrayList<>();
        for (MigrationInfo info : applied) {
            assertThat(info.getState().isApplied()).isTrue();
            versions.add(info.getVersion().getVersion());
        }
        assertThat(versions).containsSubsequence(
                "1", "2", "3", "4", "5", "6", "7", "8", "9", "10");
        assertThat(flyway.info().pending()).isEmpty();

        // validate 会比对校验和，防止有人改了已执行过的迁移
        flyway.validate();

        assertBusinessTablesExist();
        assertCriticalConstraintsExist();
    }

    private void assertEmptyDatabase() throws Exception {
        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = ?")) {
            statement.setString(1, PROBE_DB);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                assertThat(rs.getInt(1)).as("临时库在迁移前必须是空的").isZero();
            }
        }
    }

    private void assertBusinessTablesExist() throws Exception {
        Set<String> actual = new HashSet<>();
        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT table_name FROM information_schema.tables WHERE table_schema = ?")) {
            statement.setString(1, PROBE_DB);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    actual.add(rs.getString(1));
                }
            }
        }
        assertThat(actual).containsAll(EXPECTED_TABLES);
        assertThat(actual).contains("flyway_schema_history");
    }

    private void assertCriticalConstraintsExist() throws Exception {
        // 积分流水两条红线：幂等键唯一、同一笔只能冲抵一次
        assertThat(uniqueIndexesOf("points_ledger"))
                .contains("uk_ledger_idem", "uk_ledger_reversal");
        // 同一平台漏洞编号唯一；团队奖按比赛+名次唯一
        assertThat(uniqueIndexesOf("vulnerability")).contains("uk_vulnerability");
        assertThat(uniqueIndexesOf("team_award")).contains("uk_team_award_comp_rank");
        assertThat(uniqueIndexesOf("team_member")).contains("uk_team_member_active_user");
        assertThat(uniqueIndexesOf("points_appeal")).contains("uk_points_appeal_open_ledger");
        assertThat(uniqueIndexesOf("redeem_request")).contains("uk_redeem_pending_user_item");
        // V8 增加的强制改密列必须是 NOT NULL
        assertThat(columnType("user", "must_change_password"))
                .as("V8 必须补上 must_change_password 列")
                .isNotNull()
                .containsIgnoringCase("tinyint");
    }

    private Set<String> uniqueIndexesOf(String table) throws Exception {
        Set<String> indexes = new HashSet<>();
        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT DISTINCT index_name FROM information_schema.statistics "
                             + "WHERE table_schema = ? AND table_name = ? AND non_unique = 0")) {
            statement.setString(1, PROBE_DB);
            statement.setString(2, table);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    indexes.add(rs.getString(1));
                }
            }
        }
        return indexes;
    }

    private String columnType(String table, String column) throws Exception {
        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT column_type FROM information_schema.columns "
                             + "WHERE table_schema = ? AND table_name = ? AND column_name = ?")) {
            statement.setString(1, PROBE_DB);
            statement.setString(2, table);
            statement.setString(3, column);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }
}
