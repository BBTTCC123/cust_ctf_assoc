package cn.edu.cust.assoc.support;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MySQLContainer;

/**
 * 默认（Docker/CI）模式：把共享的 MySQL 容器注册成 Spring 数据源。
 *
 * 只要显式提供了 {@code it.datasource.url}（无 Docker 的本机连外部库），
 * 这个配置就整体不生效，改由 TestDatabase 注入外部数据源 —— 避免外部库和容器
 * 同时配置时连错库。
 */
@TestConfiguration(proxyBeanMethods = false)
@ConditionalOnExpression("'${it.datasource.url:}'.isBlank()")
public class TestContainerConfig {

    @Bean
    @ServiceConnection
    public MySQLContainer<?> mysqlContainer() {
        return TestDatabase.container();
    }
}
