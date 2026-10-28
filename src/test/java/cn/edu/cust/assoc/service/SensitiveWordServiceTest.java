package cn.edu.cust.assoc.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveWordServiceTest {

    @Test
    @DisplayName("零宽字符插在词中间也能被抓到")
    void zeroWidth() {
        String text = "这里有一段测\u200B试违禁词内容";
        assertThat(SensitiveWordService.normalize(text)).contains("测试违禁词");
    }

    @Test
    @DisplayName("全角字符会被规范化")
    void fullWidth() {
        assertThat(SensitiveWordService.normalize("ＴＥＳＴ")).isEqualTo("test");
    }
}
