package cn.edu.cust.assoc.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;

/**
 * 本地存储。两个区：private 不在任何静态目录下，public 将来可以交给 Nginx 直出。
 * 现在统一走鉴权下载，所以两个区都只通过 MediaController 读。
 */
@Service
public class LocalStorageService {

    private final Path root;

    public LocalStorageService(@Value("${app.storage.root}") String rootDir) throws IOException {
        this.root = Paths.get(rootDir).toAbsolutePath().normalize();
        Files.createDirectories(this.root.resolve("private"));
        Files.createDirectories(this.root.resolve("public"));
    }

    public String write(String zone, String key, byte[] data) throws IOException {
        Path target = resolve(zone, key);
        Files.createDirectories(target.getParent());
        Files.write(target, data, StandardOpenOption.CREATE_NEW);
        return key;
    }

    /** 审核搬移用：允许覆盖已存在的同名文件，重复点击审核不会因为目标已存在而失败。 */
    public void writeReplace(String zone, String key, byte[] data) throws IOException {
        Path target = resolve(zone, key);
        Files.createDirectories(target.getParent());
        Files.write(target, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public byte[] read(String zone, String key) throws IOException {
        return Files.readAllBytes(resolve(zone, key));
    }

    public InputStream open(String zone, String key) throws IOException {
        return Files.newInputStream(resolve(zone, key));
    }

    public void delete(String zone, String key) throws IOException {
        Files.deleteIfExists(resolve(zone, key));
    }

    private Path resolve(String zone, String key) {
        Path p = root.resolve(zone).resolve(key).normalize();
        if (!p.startsWith(root)) {
            throw new IllegalArgumentException("非法路径");
        }
        return p;
    }
}
