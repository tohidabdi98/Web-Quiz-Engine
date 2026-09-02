package com.tohidabdi.webquizengine;

import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizRepository extends JpaRepository<QuizEntity, Integer> {
}
