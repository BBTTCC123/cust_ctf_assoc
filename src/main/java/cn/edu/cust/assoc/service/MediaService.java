package cn.edu.cust.assoc.service;

import cn.edu.cust.assoc.domain.MediaEntity;
import cn.edu.cust.assoc.repository.MediaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class MediaService {

    private static final Logger log = LoggerFactory.getLogger(MediaService.class);
    private static final long MAX_BYTES = 10L * 1024 * 1024;

    private final MediaRepository repository;
    private final LocalStorageService storage;

    public MediaService(MediaRepository repository, LocalStorageService storage) {
        this.repository = repository;
        this.storage = storage;
    }

    @Transactional
    public MediaEntity upload(Long ownerId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件是空的");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("文件超过 10MB");
        }
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = extOf(original);
        FileTypes.Detected type = FileTypes.allowed(ext);
        if (type == null) {
            throw new IllegalArgumentException("只允许 jpg / png / pdf");
        }

        byte[] data = file.getBytes();
        if (!FileTypes.matchesMagic(data, ext)) {
            // 扩展名和文件头对不上：要么是改名的，要么是伪装成图片的脚本
            throw new IllegalArgumentException("文件内容与扩展名不符，已拒绝");
        }

        byte[] stored = data;
        String thumbKey = null;
        if (type.mime().startsWith("image/")) {
            // 重编码时保持原格式：png 存 png，jpg 存 jpg。统一转 jpg 会让扩展名和内容对不上。
            String format = type.ext().equals("png") ? "png" : "jpg";
            // 重新编码：把 EXIF、附加在图片后面的数据一起丢掉
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
            if (image == null) {
                throw new IllegalArgumentException("图片解不开，已拒绝");
            }
            stored = encode(image, 1600, format);
            thumbKey = saveThumb(image, format);
        }

        String key = UUID.randomUUID() + "." + type.ext();
        storage.write("private", key, stored);

        MediaEntity media = new MediaEntity();
        media.setOwnerUserId(ownerId);
        media.setOriginalName(original);
        media.setStoredKey(key);
        media.setThumbKey(thumbKey);
        media.setMime(type.mime());
        media.setBytes((long) stored.length);
        media.setSha256(sha256(stored));
        media.setZone("PRIVATE");
        media.setVisibility("MEMBER");
        media.setReviewState("PENDING");
        MediaEntity saved = repository.save(media);
        log.info("媒体入库 id={} owner={} bytes={} zone=private", saved.getId(), ownerId, saved.getBytes());
        return saved;
    }

    @Transactional(readOnly = true)
    public MediaEntity get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("文件不存在：" + id));
    }

    /** 审核通过：文件从 private 搬到 public，状态同时改，避免"状态改了文件还在私有区"。 */
    @Transactional
    public void approve(Long id) {
        MediaEntity media = get(id);
        try {
            byte[] data = storage.read(media.getZone(), media.getStoredKey());
            storage.write("public", media.getStoredKey(), data);
            if (media.getThumbKey() != null) {
                byte[] thumb = storage.read(media.getZone(), media.getThumbKey());
                storage.write("public", media.getThumbKey(), thumb);
            }
            storage.delete(media.getZone(), media.getStoredKey());
            if (media.getThumbKey() != null) {
                storage.delete(media.getZone(), media.getThumbKey());
            }
        } catch (IOException e) {
            throw new IllegalStateException("文件搬移失败", e);
        }
        media.setZone("PUBLIC");
        media.setVisibility("PUBLIC");
        media.setReviewState("APPROVED");
        repository.save(media);
    }

    @Transactional
    public void reject(Long id) {
        MediaEntity media = get(id);
        media.setReviewState("REJECTED");
        repository.save(media);
    }

    public byte[] content(MediaEntity media) throws IOException {
        return storage.read(media.getZone(), media.getStoredKey());
    }

    private String saveThumb(BufferedImage image, String format) throws IOException {
        String key = "thumb-" + UUID.randomUUID() + "." + format;
        storage.write("private", key, encode(image, 320, format));
        return key;
    }

    private byte[] encode(BufferedImage src, int maxSide, String format) throws IOException {
        BufferedImage scaled = src;
        int w = src.getWidth(), h = src.getHeight();
        if (Math.max(w, h) > maxSide) {
            double ratio = (double) maxSide / Math.max(w, h);
            scaled = new BufferedImage((int) (w * ratio), (int) (h * ratio), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(src, 0, 0, scaled.getWidth(), scaled.getHeight(), null);
            g.dispose();
        } else if (src.getType() != BufferedImage.TYPE_INT_RGB) {
            scaled = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = scaled.createGraphics();
            g.drawImage(src, 0, 0, null);
            g.dispose();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (!ImageIO.write(scaled, format, out)) {
            throw new IllegalStateException("当前 JDK 不支持写出 " + format);
        }
        return out.toByteArray();
    }

    private String extOf(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1);
    }

    private String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}