package cn.edu.cust.assoc.service;

import cn.edu.cust.assoc.domain.AnnouncementEntity;
import cn.edu.cust.assoc.domain.HonorEntity;
import cn.edu.cust.assoc.domain.RecruitApplicationEntity;
import cn.edu.cust.assoc.repository.AnnouncementRepository;
import cn.edu.cust.assoc.repository.HonorRepository;
import cn.edu.cust.assoc.repository.RecruitApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PublicContentService {

    private final AnnouncementRepository announcements;
    private final HonorRepository honors;
    private final RecruitApplicationRepository recruits;
    private final MarkdownRenderer renderer;

    public PublicContentService(AnnouncementRepository announcements, HonorRepository honors,
                                RecruitApplicationRepository recruits, MarkdownRenderer renderer) {
        this.announcements = announcements;
        this.honors = honors;
        this.recruits = recruits;
        this.renderer = renderer;
    }

    @Transactional(readOnly = true)
    public List<AnnouncementEntity> announcements() {
        return announcements.findAllByOrderByPinnedDescPublishedAtDesc();
    }

    @Transactional(readOnly = true)
    public AnnouncementEntity announcement(Long id) {
        return announcements.findById(id).orElseThrow(() -> new IllegalArgumentException("公告不存在"));
    }

    @Transactional(readOnly = true)
    public List<HonorEntity> honors() {
        return honors.findAllByOrderByYearDescSortOrderAsc();
    }

    @Transactional
    public AnnouncementEntity publishAnnouncement(Long authorId, String title, String contentMd, boolean pinned) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("标题不能为空");
        if (contentMd == null || contentMd.isBlank()) throw new IllegalArgumentException("正文不能为空");
        AnnouncementEntity a = new AnnouncementEntity();
        a.setTitle(title.trim());
        a.setContentMd(contentMd);
        a.setContentHtml(renderer.render(contentMd));
        a.setAuthorId(authorId);
        a.setPinned(pinned);
        return announcements.save(a);
    }

    @Transactional
    public HonorEntity addHonor(String title, Short year, String description, Integer sortOrder) {
        HonorEntity h = new HonorEntity();
        h.setTitle(title);
        h.setYear(year);
        h.setDescription(description);
        h.setSortOrder(sortOrder == null ? 0 : sortOrder);
        return honors.save(h);
    }

    /** 招新报名：游客可填，同一个学号同一赛季只能报一次。 */
    @Transactional
    public RecruitApplicationEntity recruit(String name, String studentNo, String college, String major,
                                            String contact, String message, Short seasonYear) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("姓名不能为空");
        if (studentNo == null || !studentNo.matches("\\d{6,20}")) throw new IllegalArgumentException("学号格式不对");
        if (contact == null || contact.isBlank()) throw new IllegalArgumentException("联系方式不能为空");
        if (recruits.existsByStudentNoAndSeasonYear(studentNo, seasonYear)) {
            throw new IllegalArgumentException("这个学号本赛季已经报过名了");
        }
        RecruitApplicationEntity r = new RecruitApplicationEntity();
        r.setName(name.trim());
        r.setStudentNo(studentNo.trim());
        r.setCollege(college);
        r.setMajor(major);
        r.setContact(contact.trim());
        r.setMessage(message);
        r.setSeasonYear(seasonYear);
        r.setState("NEW");
        return recruits.save(r);
    }

    @Transactional(readOnly = true)
    public List<RecruitApplicationEntity> recruitList() {
        return recruits.findAllByOrderByCreatedAtDesc();
    }
}
