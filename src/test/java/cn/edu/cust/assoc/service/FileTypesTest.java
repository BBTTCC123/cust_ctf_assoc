package cn.edu.cust.assoc.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FileTypesTest {

    private byte[] head(int... bytes) {
        byte[] b = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) b[i] = (byte) bytes[i];
        return b;
    }

    @Test
    @DisplayName("JPEG 文件头被认出")
    void jpeg() {
        assertThat(FileTypes.matchesMagic(head(0xFF, 0xD8, 0xFF, 0xE0), "jpg")).isTrue();
        assertThat(FileTypes.matchesMagic(head(0xFF, 0xD8, 0xFF, 0xE0), "jpeg")).isTrue();
    }

    @Test
    @DisplayName("PNG 文件头被认出")
    void png() {
        assertThat(FileTypes.matchesMagic(head(0x89, 'P', 'N', 'G'), "png")).isTrue();
    }

    @Test
    @DisplayName("PDF 文件头被认出")
    void pdf() {
        assertThat(FileTypes.matchesMagic("%PDF".getBytes(), "pdf")).isTrue();
    }

    @Test
    @DisplayName("改名的脚本会被拒：内容是 <?php 却叫 .jpg")
    void renamedScript() {
        assertThat(FileTypes.matchesMagic("<?php".getBytes(), "jpg")).isFalse();
        assertThat(FileTypes.matchesMagic("%PDF".getBytes(), "jpg")).isFalse();
    }

    @Test
    @DisplayName("白名单外的一律不放行")
    void notAllowed() {
        assertThat(FileTypes.allowed("exe")).isNull();
        assertThat(FileTypes.allowed("JPG").ext()).isEqualTo("jpg");
    }
}
