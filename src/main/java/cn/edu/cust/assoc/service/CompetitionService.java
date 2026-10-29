package cn.edu.cust.assoc.service;

import cn.edu.cust.assoc.domain.CompetitionEntity;
import cn.edu.cust.assoc.domain.Competitions;
import cn.edu.cust.assoc.dto.CompetitionForm;
import cn.edu.cust.assoc.repository.CompetitionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CompetitionService {

    private final CompetitionRepository repository;
    private final AuditService audit;

    public CompetitionService(CompetitionRepository repository, AuditService audit) {
        this.repository = repository;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<CompetitionEntity> listAll() {
        return repository.findAllByOrderByStartDateDesc();
    }

    @Transactional(readOnly = true)
    public List<CompetitionEntity> listBySeason(Short seasonYear) {
        return seasonYear == null ? listAll() : repository.findBySeasonYearOrderByStartDateDesc(seasonYear);
    }

    @Transactional(readOnly = true)
    public List<Short> seasons() {
        return repository.distinctSeasons();
    }

    @Transactional(readOnly = true)
    public CompetitionEntity get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("比赛不存在：" + id));
    }

    @Transactional
    public CompetitionEntity create(CompetitionForm form) {
        CompetitionEntity entity = new CompetitionEntity();
        apply(form, entity);
        CompetitionEntity saved = repository.save(entity);
        audit.record("COMPETITION_CREATE", "COMPETITION", saved.getId(), null, saved.getName());
        return saved;
    }

    @Transactional
    public CompetitionEntity update(Long id, CompetitionForm form) {
        CompetitionEntity entity = get(id);
        String before = entity.getName();
        apply(form, entity);
        CompetitionEntity saved = repository.save(entity);
        audit.record("COMPETITION_UPDATE", "COMPETITION", id, before, saved.getName());
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        CompetitionEntity entity = get(id);
        try {
            repository.deleteById(id);
            repository.flush();
            audit.record("COMPETITION_DELETE", "COMPETITION", id, entity.getName(), "DELETED");
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new cn.edu.cust.assoc.common.BizException(
                    "这场比赛已经有关联奖项或积分记录，不能删除；请先处理关联数据");
        }
    }

    @Transactional(readOnly = true)
    public CompetitionForm toForm(CompetitionEntity entity) {
        CompetitionForm form = new CompetitionForm();
        form.setName(entity.getName());
        form.setOrganizer(entity.getOrganizer());
        form.setLevel(entity.getLevel());
        form.setCategory(entity.getCategory());
        form.setStartDate(entity.getStartDate());
        form.setEndDate(entity.getEndDate());
        form.setSeasonYear(entity.getSeasonYear());
        return form;
    }

    /** 把表单落到实体，并做业务校验（这些是 Bean Validation 管不到的跨字段规则）。 */
    private void apply(CompetitionForm form, CompetitionEntity entity) {
        if (!Competitions.Level.isValid(form.getLevel())) {
            throw new IllegalArgumentException("未知的比赛级别：" + form.getLevel());
        }
        if (!Competitions.Category.isValid(form.getCategory())) {
            throw new IllegalArgumentException("未知的比赛类别：" + form.getCategory());
        }
        if (form.getEndDate().isBefore(form.getStartDate())) {
            throw new IllegalArgumentException("结束日期不能早于开始日期");
        }
        entity.setName(form.getName().trim());
        entity.setOrganizer(form.getOrganizer().trim());
        entity.setLevel(form.getLevel());
        entity.setCategory(form.getCategory());
        entity.setStartDate(form.getStartDate());
        entity.setEndDate(form.getEndDate());
        entity.setSeasonYear(form.getSeasonYear());
    }
}
