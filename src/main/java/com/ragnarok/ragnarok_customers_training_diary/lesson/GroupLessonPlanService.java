package com.ragnarok.ragnarok_customers_training_diary.lesson;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRepository;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRole;
import com.ragnarok.ragnarok_customers_training_diary.common.NotFoundException;
import com.ragnarok.ragnarok_customers_training_diary.lesson.dto.GroupLessonPlanInput;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Správa skupinových lekcí. Tvorbu/úpravy/mazání smí jen admin (vynuceno
 * v controllerech přes role-based authorize). Klient jen čte.
 */
@Service
@Transactional
public class GroupLessonPlanService {

    private final GroupLessonPlanRepository repository;
    private final AccountRepository accountRepository;

    public GroupLessonPlanService(GroupLessonPlanRepository repository, AccountRepository accountRepository) {
        this.repository = repository;
        this.accountRepository = accountRepository;
    }

    @Transactional(readOnly = true)
    public List<GroupLessonPlanEntity> listAll() {
        return repository.findAllByOrderByLessonDateDescStartTimeDesc();
    }

    /**
     * Klientský pohled — týden zpět + týden dopředu od dneška.
     */
    @Transactional(readOnly = true)
    public List<GroupLessonPlanEntity> listForClient(LocalDate today) {
        return repository.findByLessonDateBetweenOrderByLessonDateAscStartTimeAsc(
                today.minusDays(7), today.plusDays(7));
    }

    @Transactional(readOnly = true)
    public GroupLessonPlanEntity getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Skupinová lekce (id=" + id + ") nenalezena."));
    }

    public GroupLessonPlanEntity create(GroupLessonPlanInput input) {
        AccountEntity coach = loadCoach(input.getCoachId());

        GroupLessonPlanEntity entity = new GroupLessonPlanEntity();
        applyFields(entity, input, coach);
        return repository.save(entity);
    }

    public GroupLessonPlanEntity update(Long id, GroupLessonPlanInput input) {
        GroupLessonPlanEntity entity = getById(id);
        AccountEntity coach = loadCoach(input.getCoachId());
        applyFields(entity, input, coach);
        return repository.save(entity);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Skupinová lekce (id=" + id + ") nenalezena.");
        }
        repository.deleteById(id);
    }

    private void applyFields(GroupLessonPlanEntity entity, GroupLessonPlanInput input, AccountEntity coach) {
        entity.setLessonDate(input.getLessonDate());
        entity.setStartTime(input.getStartTime());
        entity.setEndTime(input.getEndTime());
        entity.setLessonName(input.getLessonName());
        entity.setCoach(coach);
        entity.setDescription(input.getDescription());
    }

    private AccountEntity loadCoach(Long coachId) {
        AccountEntity coach = accountRepository.findById(coachId)
                .orElseThrow(() -> new NotFoundException("Trenér (id=" + coachId + ") nenalezen."));
        if (coach.getRole() != AccountRole.ADMIN) {
            throw new IllegalArgumentException("Vybraný účet není trenér (admin).");
        }
        return coach;
    }
}
