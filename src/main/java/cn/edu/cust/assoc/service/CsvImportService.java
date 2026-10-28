package cn.edu.cust.assoc.service;

import cn.edu.cust.assoc.domain.CompetitionEntity;
import cn.edu.cust.assoc.domain.Competitions;
import cn.edu.cust.assoc.repository.CompetitionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 历史数据导入。三个原则：
 * 1) 逐行校验，坏行只报错、不中断整批；
 * 2) 重复（同名同赛季）直接跳过，不报错；
 * 3) 每一行要么整行进、要么不进，不留半条。
 */
@Service
public class CsvImportService {

    private final CompetitionRepository competitionRepository;

    public CsvImportService(CompetitionRepository competitionRepository) {
        this.competitionRepository = competitionRepository;
    }

    public record Result(int created, int skipped, List<String> errors) {}

    /** CSV 表头：名称,主办方,级别,类别,开始日期,结束日期,赛季 */
    public static final String TEMPLATE = "名称,主办方,级别,类别,开始日期,结束日期,赛季\n"
            + "2025 吉林省大学生网络安全竞赛,吉林省教育厅,省级,Web,2025-05-10,2025-05-12,2025\n"
            + "CISCN 2025 全国大学生信息安全竞赛,教育部,国赛,Pwn,2025-08-01,2025-08-03,2025\n";

    private static final Map<String, String> LEVEL_LABELS = Map.ofEntries(
            Map.entry("省级", "PROVINCE"), Map.entry("国赛", "NATIONAL"),
            Map.entry("国际赛", "INTERNATIONAL"), Map.entry("校级", "CAMPUS"),
            Map.entry("PROVINCE", "PROVINCE"), Map.entry("NATIONAL", "NATIONAL"),
            Map.entry("INTERNATIONAL", "INTERNATIONAL"), Map.entry("CAMPUS", "CAMPUS"));

    // Map.of 最多 10 组键值，这里超过 10 组，必须用 ofEntries
    private static final Map<String, String> CATEGORY_LABELS = Map.ofEntries(
            Map.entry("Web", "WEB"), Map.entry("Pwn", "PWN"), Map.entry("Reverse", "REVERSE"),
            Map.entry("Crypto", "CRYPTO"), Map.entry("Misc", "MISC"), Map.entry("应急响应", "INCIDENT"),
            Map.entry("WEB", "WEB"), Map.entry("PWN", "PWN"), Map.entry("REVERSE", "REVERSE"),
            Map.entry("CRYPTO", "CRYPTO"), Map.entry("MISC", "MISC"), Map.entry("INCIDENT", "INCIDENT"));

    public static String levelCode(String raw) {
        return LEVEL_LABELS.get(raw == null ? "" : raw.trim());
    }

    public static String categoryCode(String raw) {
        return CATEGORY_LABELS.get(raw == null ? "" : raw.trim());
    }

    @Transactional
    public Result importCompetitions(String csv) {
        List<String> errors = new ArrayList<>();
        int created = 0, skipped = 0;
        if (csv == null || csv.isBlank()) {
            return new Result(0, 0, List.of("文件是空的"));
        }
        String[] lines = csv.replace("\r\n", "\n").replace("\r", "\n").split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            if (i == 0 && line.startsWith("名称")) continue;      // 表头
            String[] f = line.split(",", -1);
            int rowNo = i + 1;
            if (f.length < 7) {
                errors.add("第 " + rowNo + " 行：列数不足（需要 7 列，实际 " + f.length + " 列）");
                continue;
            }
            String name = f[0].trim();
            String organizer = f[1].trim();
            String level = levelCode(f[2]);
            String category = categoryCode(f[3]);
            if (name.isEmpty()) { errors.add("第 " + rowNo + " 行：名称不能为空"); continue; }
            if (level == null) { errors.add("第 " + rowNo + " 行：级别不认识 → " + f[2].trim()); continue; }
            if (category == null) { errors.add("第 " + rowNo + " 行：类别不认识 → " + f[3].trim()); continue; }

            LocalDate start, end;
            short season;
            try {
                start = LocalDate.parse(f[4].trim());
                end = LocalDate.parse(f[5].trim());
                season = Short.parseShort(f[6].trim());
            } catch (DateTimeParseException | NumberFormatException e) {
                errors.add("第 " + rowNo + " 行：日期或赛季格式不对（日期要 2025-05-10 这种）");
                continue;
            }
            if (end.isBefore(start)) { errors.add("第 " + rowNo + " 行：结束日期早于开始日期"); continue; }
            if (competitionRepository.existsByNameAndSeasonYear(name, season)) {
                skipped++;
                continue;
            }
            CompetitionEntity c = new CompetitionEntity();
            c.setName(name);
            c.setOrganizer(organizer.isEmpty() ? "未知" : organizer);
            c.setLevel(level);
            c.setCategory(category);
            c.setStartDate(start);
            c.setEndDate(end);
            c.setSeasonYear(season);
            competitionRepository.save(c);
            created++;
        }
        return new Result(created, skipped, errors);
    }
}