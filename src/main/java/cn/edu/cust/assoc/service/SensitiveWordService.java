package cn.edu.cust.assoc.service;

import cn.edu.cust.assoc.domain.SensitiveWordEntity;
import cn.edu.cust.assoc.repository.SensitiveWordRepository;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;

/**
 * 敏感词检查。先规范化再比对：去零宽字符、全角转半角、NFKC。
 * 这是第一道闸，不是保证；绕法还有很多，所以后面必须配人工审核。
 */
@Service
public class SensitiveWordService {

    public enum Result { PASS, BLOCK, REVIEW }

    private final SensitiveWordRepository repository;

    public SensitiveWordService(SensitiveWordRepository repository) {
        this.repository = repository;
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

    static String normalize(String s) {
        // 去掉零宽字符：这些字符人看不见，但会让「精确匹配」全部失效
        String noZeroWidth = s.replaceAll("[\u200B-\u200F\uFEFF]", "");
        String nfkc = Normalizer.normalize(noZeroWidth, Normalizer.Form.NFKC);
        return nfkc.toLowerCase().replaceAll("\s+", "");
    }
}
