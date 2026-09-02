package com.tohidabdi.webquizengine;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "quiz_completions")
public class QuizCompletionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private QuizEntity quiz;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false)
    private OffsetDateTime completedAt;

    protected QuizCompletionEntity() {
    }

    public QuizCompletionEntity(QuizEntity quiz, UserEntity user, OffsetDateTime completedAt) {
        this.quiz = quiz;
        this.user = user;
        this.completedAt = completedAt;
    }

    public QuizEntity getQuiz() {
        return quiz;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }
}
