package com.tohidabdi.webquizengine;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizCompletionRepository extends JpaRepository<QuizCompletionEntity, Integer> {

    Page<QuizCompletionEntity> findByUserEmailOrderByCompletedAtDescIdDesc(String email, Pageable pageable);

    void deleteAllByQuiz(QuizEntity quiz);
}
