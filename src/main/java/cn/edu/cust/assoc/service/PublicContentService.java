package cn.edu.cust.assoc.service;

import cn.edu.cust.assoc.common.BizException;
import cn.edu.cust.assoc.domain.AnnouncementEntity;
import cn.edu.cust.assoc.domain.HonorEntity;
import cn.edu.cust.assoc.domain.RecruitApplicationEntity;
import cn.edu.cust.assoc.repository.AnnouncementRepository;
import cn.edu.cust.assoc.repository.HonorRepository;
import cn.edu.cust.assoc.repository.RecruitApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PublicContentService {

    /** 招新报名允许的状态流转。终态不能再动。 */
    private static final Map<String, Set<String>> RECRUIT_FLOW = Map.of(
            "NEW", Set.of("CONTACTED", "REJECTED"),
            "CONTACTED", Set.of("ACCEPTED", "REJECTED"),
            "ACCEPTED", Set.of(),
            "REJECTED", Set.of());

    private final AnnouncementRepository announcements;
    private final HonorRepository honors;
    private final RecruitApplicationRepository recruits;
    private final MarkdownRenderer renderer;
    private final AuditService audit;

    public PublicContentService(AnnouncementRepository announcements, HonorRepository honors,
                                RecruitApplicationRepository recruits, MarkdownRenderer renderer,
                                AuditService audit) {
        this.announcements = announcements;
        this.honors = honors;
        this.recruits = recruits;
        this.renderer = renderer;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<AnnouncementEntity> announcements() {
        return announcements.findByDeletedAtIsNullOrderByPinnedDescPublishedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<AnnouncementEntity> allAnnouncements() {
        return announcements.findAllByOrderByPinnedDescPublishedAtDesc();
    }

    @Transactional(readOnly = true)
    public AnnouncementEntity announcement(Long id) {
        return announcements.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BizException("公告不存在或已下架"));
    }

    @Transactional(readOnly = true)
    public List<HonorEntity> honors() {
        return honors.findByDeletedAtIsNullOrderByYearDescSortOrderAsc();
    }

    @Transactional(readOnly = true)
    public List<HonorEntity> allHonors() {
        return honors.findAllByOrderByYearDescSortOrderAsc();
    }

    @Transactional
    public AnnouncementEntity publishAnnouncement(Long authorId, String title, String contentMd, boolean pinned) {
        if (title == null || title.isBlank()) throw new BizException("标题不能为空");
        if (contentMd == null || contentMd.isBlank()) throw new BizException("正文不能为空");
        if (title.length() > 200) throw new BizException("标题不能超过 200 字");
        AnnouncementEntity announcement = new AnnouncementEntity();
        announcement.setTitle(title.trim());
        announcement.setContentMd(contentMd);
        announcement.setContentHtml(renderer.render(contentMd));
        announcement.setAuthorId(authorId);
        announcement.setPinned(pinned);
        AnnouncementEntity saved = announcements.save(announcement);
        audit.record("ANNOUNCEMENT_PUBLISH", "ANNOUNCEMENT", saved.getId(), null, saved.getTitle());
        return saved;
    }

    /** 公告下架：软删除，内容保留可追溯。 */
    @Transactional
    public void takedownAnnouncement(Long id) {
        AnnouncementEntity announcement = announcements.findById(id)
                .orElseThrow(() -> new BizException("公告不存在"));
        if (announcement.getDeletedAt() != null) {
            throw new BizException("公告已经下架了");
        }
        announcement.setDeletedAt(LocalDateTime.now());
        announcements.save(announcement);
        audit.record("ANNOUNCEMENT_TAKEDOWN", "ANNOUNCEMENT", id, announcement.getTitle(), "OFFLINE");
    }

    @Transactional
    public HonorEntity addHonor(String title, Short year, String description, Integer sortOrder) {
        if (title == null || title.isBlank()) throw new BizException("荣誉名称不能为空");
        if (title.length() > 200) throw new BizException("荣誉名称不能超过 200 字");
        if (year == null || year < 2000 || year > 2100) throw new BizException("年份不太对");
        HonorEntity honor = new HonorEntity();
        honor.setTitle(title.trim());
        honor.setYear(year);
        honor.setDescription(description);
        honor.setSortOrder(sortOrder == null ? 0 : sortOrder);
        HonorEntity saved = honors.save(honor);
        audit.record("HONOR_ADD", "HONOR", saved.getId(), null, saved.getTitle());
        return saved;
    }

    @Transactional
    public void takedownHonor(Long id) {
        HonorEntity honor = honors.findById(id)
                .orElseThrow(() -> new BizException("荣誉不存在"));
        if (honor.getDeletedAt() != null) {
            throw new BizException("荣誉已经下架了");
        }
        honor.setDeletedAt(LocalDateTime.now());
        honors.save(honor);
        audit.record("HONOR_TAKEDOWN", "HONOR", id, honor.getTitle(), "OFFLINE");
    }

    /** 招新报名：游客可填，同一个学号同一赛季只能报一次。 */
    @Transactional
    public RecruitApplicationEntity recruit(String name, String studentNo, String college, String major,
                                            String contact, String message, Short seasonYear) {
        if (name == null || name.isBlank()) throw new BizException("姓名不能为空");
        if (name.trim().length() > 64) throw new BizException("姓名太长");
        if (studentNo == null || !studentNo.matches("\\d{6,20}")) throw new BizException("学号格式不对");
        if (college == null || college.isBlank()) throw new BizException("学院不能为空");
        if (major == null || major.isBlank()) throw new BizException("专业不能为空");
        if (contact == null || contact.isBlank()) throw new BizException("联系方式不能为空");
        if (college.trim().length() > 128 || major.trim().length() > 128 || contact.trim().length() > 128) {
            throw new BizException("填写内容过长");
        }
        if (message != null && message.length() > 1000) throw new BizException("留言不能超过 1000 字");
        if (recruits.existsByStudentNoAndSeasonYear(studentNo, seasonYear)) {
            throw new BizException("这个学号本赛季已经报过名了");
        }
        RecruitApplicationEntity application = new RecruitApplicationEntity();
        application.setName(name.trim());
        application.setStudentNo(studentNo.trim());
        application.setCollege(college.trim());
        application.setMajor(major.trim());
        application.setContact(contact.trim());
        application.setMessage(message);
        application.setSeasonYear(seasonYear);
        application.setState("NEW");
        return recruits.save(application);
    }

    @Transactional(readOnly = true)
    public List<RecruitApplicationEntity> recruitList() {
        return recruits.findAllByOrderByCreatedAtDesc();
    }

    /** 招新状态流转：只允许 NEW→CONTACTED/REJECTED、CONTACTED→ACCEPTED/REJECTED。 */
    @Transactional
    public void transitionRecruit(Long id, String targetState, String note, Long operatorId) {
        RecruitApplicationEntity application = recruits.findByIdForUpdate(id)
                .orElseThrow(() -> new BizException("报名记录不存在"));
        String before = application.getState();
        Set<String> allowed = RECRUIT_FLOW.getOrDefault(before, Set.of());
        if (!allowed.contains(targetState)) {
            throw new BizException("不能从 " + before + " 直接变成 " + targetState);
        }
        application.setState(targetState);
        application.setNote(note == null || note.isBlank() ? null : note.trim());
        application.setHandledBy(operatorId);
        application.setHandledAt(LocalDateTime.now());
        recruits.save(application);
        audit.record("RECRUIT_STATE_CHANGE", "RECRUIT", id, before, targetState + " " + (note == null ? "" : note));
    }
}
