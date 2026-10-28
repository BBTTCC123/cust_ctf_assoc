package cn.edu.cust.assoc.service;

import cn.edu.cust.assoc.common.BizException;
import cn.edu.cust.assoc.domain.SensitiveWordEntity;
import cn.edu.cust.assoc.repository.SensitiveWordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * 敏感词检查。先规范化再比对：NFKC、去零宽/控制字符、去所有空白、转小写。
 * 这是第一道闸，不是保证；绕法还有很多，所以后面必须配人工审核。
 *
 * 词库没有缓存：后台新增/停用后立即生效，不需要等缓存过期。
 */
@Service
public class SensitiveWordService {

    public enum Result { PASS, BLOCK, REVIEW }

    private final SensitiveWordRepository repository;
    private final AuditService audit;

    public SensitiveWordService(SensitiveWordRepository repository, AuditService audit) {
        this.repository = repository;
        this.audit = audit;
    }

    public Result check(String text) {
        if (text == null || text.isBlank()) return Result.PASS;
        String normalized = normalize(text);
        List<SensitiveWordEntity> words = repository.findByEnabledTrue();
        boolean review = false;
        for (SensitiveWordEntity w : words) {
            if (normalized.contains(normalize(w.getWord()))) {
                if ("BLOCK".equals(w.getLevel())) {
                    return Result.BLOCK;
                }
                review = true;
            }
        }
        return review ? Result.REVIEW : Result.PASS;
    }

    /**
     * 规范化：全角转半角（NFKC），去掉零宽字符、软连字符和所有空白。
     * 用显式字符集而不是 "\s+"：Java 字符串里的 "\s" 只是普通空格，
     * 换行、制表符、全角空格都会漏掉。
     */
    static String normalize(String s) {
        String nfkc = Normalizer.normalize(s, Normalizer.Form.NFKC);
        StringBuilder sb = new StringBuilder(nfkc.length());
        nfkc.codePoints()
                .filter(cp -> !Character.isWhitespace(cp) && !Character.isSpaceChar(cp) && !isInvisible(cp))
                .forEach(sb::appendCodePoint);
        return sb.toString().toLowerCase(Locale.ROOT);
    }

    private static boolean isInvisible(int cp) {
        return (cp >= 0x200B && cp <= 0x200F)
                || cp == 0xFEFF
                || cp == 0x2060
                || cp == 0x00AD;
    }

    // ------------------------------------------------------------------
    // 后台词库管理
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<SensitiveWordEntity> all() {
        return repository.findAllByOrderByIdAsc();
    }

    @Transactional
    public SensitiveWordEntity add(String word, String level) {
        String text = validWord(word);
        String grade = validLevel(level);
        if (repository.existsByWordAndLevel(text, grade)) {
            throw new BizException("这个词已经在词库里了");
        }
        SensitiveWordEntity entity = new SensitiveWordEntity();
        entity.setWord(text);
        entity.setLevel(grade);
        entity.setEnabled(true);
        SensitiveWordEntity saved = repository.save(entity);
        audit.record("SENSITIVE_WORD_ADD", "SENSITIVE_WORD", saved.getId(), null, text + "/" + grade);
        return saved;
    }

    @Transactional
    public void update(Long id, String word, String level, boolean enabled) {
        SensitiveWordEntity entity = repository.findById(id)
                .orElseThrow(() -> new BizException("敏感词不存在"));
        String before = entity.getWord() + "/" + entity.getLevel() + "/" + entity.getEnabled();
        entity.setWord(validWord(word));
        entity.setLevel(validLevel(level));
        entity.setEnabled(enabled);
        repository.save(entity);
        audit.record("SENSITIVE_WORD_UPDATE", "SENSITIVE_WORD", id, before,
                entity.getWord() + "/" + entity.getLevel() + "/" + enabled);
    }

    @Transactional
    public void delete(Long id) {
        SensitiveWordEntity entity = repository.findById(id)
                .orElseThrow(() -> new BizException("敏感词不存在"));
        repository.delete(entity);
        audit.record("SENSITIVE_WORD_DELETE", "SENSITIVE_WORD", id, entity.getWord(), null);
    }

    /** 批量导入：每行「词,级别」，不写级别默认 REVIEW；坏行跳过并返回提示。 */
    @Transactional
    public String importLines(String text) {
        if (text == null || text.isBlank()) {
            throw new BizException("导入内容是空的");
        }
        int added = 0;
        int skipped = 0;
        for (String rawLine : text.split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] parts = line.split("[,，\\s]+");
            String word = parts[0].trim();
            String level = parts.length > 1 ? parts[1].trim().toUpperCase(Locale.ROOT) : "REVIEW";
            if (word.isEmpty() || word.length() > 64 || !("BLOCK".equals(level) || "REVIEW".equals(level))) {
                skipped++;
                continue;
            }
            if (repository.existsByWordAndLevel(word, level)) {
                skipped++;
                continue;
            }
            SensitiveWordEntity entity = new SensitiveWordEntity();
            entity.setWord(word);
            entity.setLevel(level);
            entity.setEnabled(true);
            repository.save(entity);
            added++;
        }
        audit.record("SENSITIVE_WORD_IMPORT", "SENSITIVE_WORD", null, null, "新增 " + added + "，跳过 " + skipped);
        return "新增 " + added + " 条，跳过 " + skipped + " 条";
    }

    private String validWord(String word) {
        String text = word == null ? "" : word.trim();
        if (text.isEmpty() || text.length() > 64) {
            throw new BizException("敏感词不能为空且不超过 64 个字符");
        }
        return text;
    }

    private String validLevel(String level) {
        String grade = level == null ? "" : level.trim().toUpperCase(Locale.ROOT);
        if (!"BLOCK".equals(grade) && !"REVIEW".equals(grade)) {
            throw new BizException("级别只能是 BLOCK 或 REVIEW");
        }
        return grade;
    }
}
