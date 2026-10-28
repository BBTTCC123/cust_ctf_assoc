package cn.edu.cust.assoc.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveWordServiceTest {

    @Test
    @DisplayName("零宽字符插在词中间也能被抓到")
    void zeroWidth() {
        String text = "这里有一段测​试违禁词内容";
        assertThat(SensitiveWordService.normalize(text)).contains("测试违禁词");
    }

    @Test
    @DisplayName("全角字符会被规范化")
    void fullWidth() {
        assertThat(SensitiveWordService.normalize("ＴＥＳＴ")).isEqualTo("test");
    }

    @Test
    @DisplayName("换行、制表符、全角空格拆词都能还原")
    void whitespaceBetweenChars() {
        assertThat(SensitiveWordService.normalize("测\n试\t违 禁　词")).isEqualTo("测试违禁词");
        assertThat(SensitiveWordService.normalize("测 试 违 禁词")).isEqualTo("测试违禁词");
    }

    @Test
    @DisplayName("敏感词自身带空白也能匹配")
    void wordWithWhitespace() {
        String normalized = SensitiveWordService.normalize("测 试 违 禁 词");
        assertThat(normalized).contains(SensitiveWordService.normalize("测试违禁词"));
    }
}
