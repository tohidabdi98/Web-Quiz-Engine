package com.tohidabdi.webquizengine;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/quizzes")
public class QuizzesController {

    private final QuizRepository quizRepository;

    public QuizzesController(QuizRepository quizRepository) {
        this.quizRepository = quizRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public QuizView createQuiz(@Valid @RequestBody QuizRequest request) {
        QuizEntity quiz = new QuizEntity(
                request.title(),
                request.text(),
                request.options(),
                request.answer() == null ? null : new HashSet<>(request.answer())
        );
        return toView(quizRepository.save(quiz));
    }

    @GetMapping("/{id}")
    public QuizView getQuiz(@PathVariable int id) {
        return toView(findQuiz(id));
    }

    @GetMapping
    public List<QuizView> getAllQuizzes() {
        return quizRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(this::toView)
                .toList();
    }

    @PostMapping("/{id}/solve")
    public AnswerResponse solveQuiz(@PathVariable int id, @RequestBody SolveRequest request) {
        QuizEntity quiz = findQuiz(id);
        Set<Integer> submittedAnswers = request.answer() == null
                ? Set.of()
                : new HashSet<>(request.answer());
        boolean correct = quiz.getAnswer().equals(submittedAnswers);
        if (correct) {
            return new AnswerResponse(true, "Congratulations, you're right!");
        }

        return new AnswerResponse(false, "Wrong answer! Please, try again.");
    }

    private QuizEntity findQuiz(int id) {
        return quizRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found"));
    }

    private QuizView toView(QuizEntity quiz) {
        return new QuizView(quiz.getId(), quiz.getTitle(), quiz.getText(), quiz.getOptions());
    }

    public record QuizRequest(
            @NotBlank String title,
            @NotBlank String text,
            @NotNull @Size(min = 2) List<String> options,
            List<Integer> answer
    ) {
    }

    public record SolveRequest(List<Integer> answer) {
    }

    public record QuizView(int id, String title, String text, List<String> options) {
    }

    public record AnswerResponse(boolean success, String feedback) {
    }
}
