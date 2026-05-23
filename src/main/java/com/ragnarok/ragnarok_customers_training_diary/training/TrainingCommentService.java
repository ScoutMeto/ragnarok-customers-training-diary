package com.ragnarok.ragnarok_customers_training_diary.training;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRole;
import com.ragnarok.ragnarok_customers_training_diary.common.ForbiddenException;
import com.ragnarok.ragnarok_customers_training_diary.common.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Komentáře k tréninkům. Komentovat smí:
 *  - majitel tréninku (poznámka sobě)
 *  - libovolný ADMIN (trenérská zpětná vazba)
 *
 * Mazat smí jen autor komentáře nebo admin.
 */
@Service
@Transactional
public class TrainingCommentService {

    private final TrainingCommentRepository commentRepository;
    private final TrainingRepository trainingRepository;

    public TrainingCommentService(TrainingCommentRepository commentRepository,
                                  TrainingRepository trainingRepository) {
        this.commentRepository = commentRepository;
        this.trainingRepository = trainingRepository;
    }

    @Transactional(readOnly = true)
    public List<TrainingCommentEntity> listForTraining(Long trainingId) {
        return commentRepository.findByTraining_IdOrderByCreatedAtAsc(trainingId);
    }

    public TrainingCommentEntity addComment(AccountEntity author, Long trainingId, String text) {
        TrainingEntity training = trainingRepository.findById(trainingId)
                .orElseThrow(() -> new NotFoundException("Trénink (id=" + trainingId + ") nenalezen."));

        // Smí komentovat:
        //  - PRIVATE: jen majitel NEBO admin
        //  - GROUP: kdokoliv přihlášený (klient i admin)
        boolean isGroup = training.getVisibility() == com.ragnarok.ragnarok_customers_training_diary.training.TrainingVisibility.GROUP;
        boolean isOwner = !isGroup && training.getOwner() != null
                && training.getOwner().getId().equals(author.getId());
        boolean isAdmin = author.getRole() == AccountRole.ADMIN;
        if (!isGroup && !isOwner && !isAdmin) {
            throw new ForbiddenException("Komentovat trénink může jen jeho majitel nebo admin.");
        }

        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Text komentáře nesmí být prázdný.");
        }

        TrainingCommentEntity comment = new TrainingCommentEntity();
        comment.setTraining(training);
        comment.setAuthor(author);
        comment.setText(text.trim());
        return commentRepository.save(comment);
    }

    public void deleteComment(AccountEntity user, Long commentId) {
        TrainingCommentEntity comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Komentář (id=" + commentId + ") nenalezen."));

        boolean isAuthor = comment.getAuthor().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == AccountRole.ADMIN;
        if (!isAuthor && !isAdmin) {
            throw new ForbiddenException("Komentář může smazat jen jeho autor nebo admin.");
        }

        commentRepository.delete(comment);
    }
}
