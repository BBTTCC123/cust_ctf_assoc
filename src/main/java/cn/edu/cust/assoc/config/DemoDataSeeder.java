package cn.edu.cust.assoc.config;

import cn.edu.cust.assoc.domain.HonorEntity;
import cn.edu.cust.assoc.repository.HonorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 演示数据只属于开发/测试环境。
 * 生产库不加载任何示例业务数据；需要时显式设置 APP_DEMO_DATA_ENABLED=true。
 */
@Configuration
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class DemoDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    @Bean
    public ApplicationRunner seedDemoHonors(HonorRepository honorRepository) {
        return args -> {
            if (honorRepository.count() > 0) {
                return;
            }
            add(honorRepository, "CISCN 全国大学生信息安全竞赛 华北赛区三等奖", 2025, "首次参赛", 10);
            add(honorRepository, "吉林省大学生网络安全竞赛 二等奖", 2025, "两人进入前十", 20);
            log.info("已加载开发环境演示荣誉数据");
        };
    }

    private void add(HonorRepository repository, String title, int year, String description, int sortOrder) {
        HonorEntity honor = new HonorEntity();
        honor.setTitle(title);
        honor.setYear((short) year);
        honor.setDescription(description);
        honor.setSortOrder(sortOrder);
        repository.save(honor);
    }
}
