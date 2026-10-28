package cn.edu.cust.assoc.service;

import cn.edu.cust.assoc.common.BizException;
import cn.edu.cust.assoc.domain.MediaEntity;
import cn.edu.cust.assoc.repository.MediaLinkRepository;
import cn.edu.cust.assoc.repository.MediaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class MediaService {

    private static final Logger log = LoggerFactory.getLogger(MediaService.class);
    public static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final Set<String> VISIBILITIES = Set.of("PUBLIC", "MEMBER", "OWNER");

    private final MediaRepository repository;
    private final MediaLinkRepository linkRepository;
    private final LocalStorageService storage;
    private final AuditService audit;

    public MediaService(MediaRepository repository, MediaLinkRepository linkRepository,
                        LocalStorageService storage, AuditService audit) {
        this.repository = repository;
        this.linkRepository = linkRepository;
        this.storage = storage;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<MediaEntity> pending() {
        return repository.findByReviewStateOrderByCreatedAtAsc("PENDING");
    }

    @Transactional(readOnly = true)
    public List<MediaEntity> all() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public MediaEntity upload(Long ownerId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BizException("文件是空的");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BizException("文件超过 10MB");
        }
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = extOf(original);
        FileTypes.Detected type = FileTypes.allowed(ext);
        if (type == null) {
            throw new BizException("只允许 jpg / png / pdf");
        }

        byte[] data = file.getBytes();
        if (!FileTypes.matchesMagic(data, ext)) {
            // 扩展名和文件头对不上：要么是改名的，要么是伪装成图片的脚本
            throw new BizException("文件内容与扩展名不符，已拒绝");
        }

        byte[] stored = data;
        String thumbKey = null;
        if (type.mime().startsWith("image/")) {
            // 重编码时保持原格式：png 存 png，jpg 存 jpg。统一转 jpg 会让扩展名和内容对不上。
            String format = type.ext().equals("png") ? "png" : "jpg";
            // 重新编码：把 EXIF、附加在图片后面的数据一起丢掉
            BufferedImage image;
            try {
                image = ImageIO.read(new ByteArrayInputStream(data));
            } catch (IOException e) {
                // ImageIO 的原始异常含技术细节，不往页面带
                throw new BizException("图片解不开，已拒绝");
            }
            if (image == null) {
                throw new BizException("图片解不开，已拒绝");
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
                .orElseThrow(() -> new BizException("文件不存在：" + id));
    }

    /**
     * 审核通过：先把文件复制到公共区，数据库提交成功后再删私有区副本。
     * 顺序反了（先删后提交）会丢文件；先提交后复制会出现「状态已通过但文件还在私有区」。
     * 数据库失败时私有区文件仍在，重试审核是幂等的。
     *
     * 审核只改变审核状态和可见范围，不再替上传人把可见范围强制改成 PUBLIC。
     */
    @Transactional
    public void approve(Long id, String visibility) {
        if (visibility == null || !VISIBILITIES.contains(visibility)) {
            throw new BizException("未知的可见范围：" + visibility);
        }
        MediaEntity media = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new BizException("文件不存在：" + id));

        if ("APPROVED".equals(media.getReviewState())) {
            // 重复点击：只允许调整可见范围，不再搬文件
            if (!visibility.equals(media.getVisibility())) {
                media.setVisibility(visibility);
                repository.save(media);
            }
            return;
        }
        if (!"PENDING".equals(media.getReviewState())) {
            throw new BizException("只有待审文件才能通过审核");
        }

        String originZone = media.getZone();
        try {
            byte[] data = storage.read(originZone, media.getStoredKey());
            storage.writeReplace("public", media.getStoredKey(), data);
            if (media.getThumbKey() != null) {
                byte[] thumb = storage.read(originZone, media.getThumbKey());
                storage.writeReplace("public", media.getThumbKey(), thumb);
            }
        } catch (IOException e) {
            // 私有区文件原样保留，事务回滚后状态仍是 PENDING，可以重试
            throw new IllegalStateException("文件搬移失败", e);
        }

        media.setZone("PUBLIC");
        media.setReviewState("APPROVED");
        media.setVisibility(visibility);
        repository.save(media);

        if ("PRIVATE".equals(originZone)) {
            schedulePrivateCleanup(originZone, media.getStoredKey(), media.getThumbKey());
        }
        audit.record("MEDIA_APPROVE", "MEDIA", id, "PENDING", "APPROVED " + visibility);
        log.info("媒体审核通过 id={} visibility={} zone=public", id, visibility);
    }

    @Transactional
    public void reject(Long id) {
        MediaEntity media = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new BizException("文件不存在：" + id));
        if ("REJECTED".equals(media.getReviewState())) {
            return;
        }
        if (!"PENDING".equals(media.getReviewState())) {
            throw new BizException("只有待审文件才能驳回");
        }
        media.setReviewState("REJECTED");
        repository.save(media);
        audit.record("MEDIA_REJECT", "MEDIA", id, "PENDING", "REJECTED");
    }

    /** 已通过的内容下架：引用检查通过后只改状态，文件不再对外提供。 */
    @Transactional
    public void revoke(Long id) {
        MediaEntity media = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new BizException("文件不存在：" + id));
        if (!"APPROVED".equals(media.getReviewState())) {
            throw new BizException("只有已通过的文件才能下架");
        }
        long references = linkRepository.countByMediaId(id);
        if (references > 0) {
            throw new BizException("还有 " + references + " 处内容在引用这个文件，请先解除引用再下架");
        }
        media.setReviewState("REVOKED");
        repository.save(media);
        audit.record("MEDIA_REVOKE", "MEDIA", id, "APPROVED", "REVOKED");
        log.info("媒体已下架 id={}", id);
    }

    /** 删除未通过的文件：数据库删成功后，提交后再清理磁盘。已被引用的文件不允许删除。 */
    @Transactional
    public void delete(Long id) {
        MediaEntity media = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new BizException("文件不存在：" + id));
        if ("APPROVED".equals(media.getReviewState())) {
            throw new BizException("已通过的文件请先下架，不能直接删除");
        }
        if (linkRepository.countByMediaId(id) > 0) {
            throw new BizException("还有内容引用这个文件，不能删除");
        }
        repository.delete(media);
        schedulePrivateCleanup(media.getZone(), media.getStoredKey(), media.getThumbKey());
        audit.record("MEDIA_DELETE", "MEDIA", id, media.getReviewState(), "DELETED");
    }

    public byte[] content(MediaEntity media) throws IOException {
        return storage.read(media.getZone(), media.getStoredKey());
    }

    public byte[] thumbnail(MediaEntity media) throws IOException {
        if (media.getThumbKey() == null) {
            return content(media);
        }
        return storage.read(media.getZone(), media.getThumbKey());
    }

    private void schedulePrivateCleanup(String zone, String key, String thumbKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteQuietly(zone, key, thumbKey);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteQuietly(zone, key, thumbKey);
            }
        });
    }

    private void deleteQuietly(String zone, String key, String thumbKey) {
        try {
            storage.delete(zone, key);
            if (thumbKey != null) {
                storage.delete(zone, thumbKey);
            }
        } catch (IOException e) {
            // 删不掉只是留下孤儿文件，不影响一致性；记日志下次手动清理
            log.warn("清理旧区文件失败 zone={} key={}", zone, key, e);
        }
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
