package cn.edu.cust.assoc.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CsvLabelTest {

    @Test
    @DisplayName("中文级别能翻成代码")
    void levelLabels() {
        assertThat(CsvImportService.levelCode("省级")).isEqualTo("PROVINCE");
        assertThat(CsvImportService.levelCode("国赛")).isEqualTo("NATIONAL");
        assertThat(CsvImportService.levelCode("国际赛")).isEqualTo("INTERNATIONAL");
        assertThat(CsvImportService.levelCode("校级")).isEqualTo("CAMPUS");
        assertThat(CsvImportService.levelCode("市赛")).isNull();
    }

    @Test
    @DisplayName("类别中英文都能认")
    void categoryLabels() {
        assertThat(CsvImportService.categoryCode("Web")).isEqualTo("WEB");
        assertThat(CsvImportService.categoryCode("PWN")).isEqualTo("PWN");
        assertThat(CsvImportService.categoryCode("应急响应")).isEqualTo("INCIDENT");
        assertThat(CsvImportService.categoryCode("区块链")).isNull();
    }

    @Test
    @DisplayName("模板本身能被自己的解析器认出来")
    void templateIsWellFormed() {
        String[] lines = CsvImportService.TEMPLATE.split("\n");
        for (String line : lines) {
            assertThat(line.split(",", -1)).hasSize(7);
        }
    }
}
