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

    public CompetitionService(CompetitionRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<CompetitionEntity> listAll() {
        return repository.findAllByOrderByStartDateDesc();
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
        return repository.save(entity);
    }

    @Transactional
    public CompetitionEntity update(Long id, CompetitionForm form) {
        CompetitionEntity entity = get(id);
        apply(form, entity);
        return repository.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
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
