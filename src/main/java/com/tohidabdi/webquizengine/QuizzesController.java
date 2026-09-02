package com.tohidabdi.webquizengine;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
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
    public QuizView createQuiz(@RequestBody(required = false) QuizRequest request) {
        QuizRequest quizRequest = request == null ? new QuizRequest(null, null, null, null) : request;
        int id = nextId.getAndIncrement();
        StoredQuiz quiz = new StoredQuiz(
                id,
                quizRequest.title(),
                quizRequest.text(),
                quizRequest.options(),
                quizRequest.answer()
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
    public AnswerResponse solveQuiz(@PathVariable int id, @RequestParam int answer) {
        StoredQuiz quiz = findQuiz(id);
        boolean correct = quiz.answer() != null && quiz.answer() == answer;
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

    public record QuizRequest(String title, String text, List<String> options, Integer answer) {
    }

    public record QuizView(int id, String title, String text, List<String> options) {
    }

    public record AnswerResponse(boolean success, String feedback) {
    }

    private record StoredQuiz(int id, String title, String text, List<String> options, Integer answer) {

        private StoredQuiz {
            options = options == null ? null : List.copyOf(new ArrayList<>(options));
        }

        private QuizView view() {
            return new QuizView(id, title, text, options);
        }
    }
}
