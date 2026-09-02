package com.tohidabdi.webquizengine;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/quizzes")
public class QuizzesController {

    private final AtomicInteger nextId = new AtomicInteger(1);
    private final ConcurrentMap<Integer, StoredQuiz> quizzes = new ConcurrentHashMap<>();

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public QuizView createQuiz(@Valid @RequestBody QuizRequest request) {
        int id = nextId.getAndIncrement();
        StoredQuiz quiz = new StoredQuiz(
                id,
                request.title(),
                request.text(),
                request.options(),
                request.answer() == null ? null : new HashSet<>(request.answer())
        );
        quizzes.put(id, quiz);
        return quiz.view();
    }

    @GetMapping("/{id}")
    public QuizView getQuiz(@PathVariable int id) {
        return findQuiz(id).view();
    }

    @GetMapping
    public List<QuizView> getAllQuizzes() {
        return quizzes.values().stream()
                .sorted((first, second) -> Integer.compare(first.id(), second.id()))
                .map(StoredQuiz::view)
                .toList();
    }

    @PostMapping("/{id}/solve")
    public AnswerResponse solveQuiz(@PathVariable int id, @RequestBody SolveRequest request) {
        StoredQuiz quiz = findQuiz(id);
        Set<Integer> submittedAnswers = request.answer() == null
                ? Set.of()
                : new HashSet<>(request.answer());
        boolean correct = quiz.answer().equals(submittedAnswers);
        if (correct) {
            return new AnswerResponse(true, "Congratulations, you're right!");
        }

        return new AnswerResponse(false, "Wrong answer! Please, try again.");
    }

    private StoredQuiz findQuiz(int id) {
        StoredQuiz quiz = quizzes.get(id);
        if (quiz == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found");
        }
        return quiz;
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

    private record StoredQuiz(int id, String title, String text, List<String> options, Set<Integer> answer) {

        private StoredQuiz {
            options = List.copyOf(new ArrayList<>(options));
            answer = answer == null ? Set.of() : Set.copyOf(answer);
        }

        private QuizView view() {
            return new QuizView(id, title, text, options);
        }
    }
}
