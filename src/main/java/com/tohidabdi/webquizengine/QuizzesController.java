package com.tohidabdi.webquizengine;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/quizzes")
public class QuizzesController {

    private static final int PAGE_SIZE = 10;

    private final QuizRepository quizRepository;
    private final UserRepository userRepository;
    private final QuizCompletionRepository completionRepository;

    public QuizzesController(
            QuizRepository quizRepository,
            UserRepository userRepository,
            QuizCompletionRepository completionRepository
    ) {
        this.quizRepository = quizRepository;
        this.userRepository = userRepository;
        this.completionRepository = completionRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public QuizView createQuiz(@Valid @RequestBody QuizRequest request, Authentication authentication) {
        QuizEntity quiz = new QuizEntity(
                request.title(),
                request.text(),
                request.options(),
                request.answer() == null ? null : new HashSet<>(request.answer()),
                findUser(authentication)
        );
        return toView(quizRepository.save(quiz));
    }

    @GetMapping("/{id}")
    public QuizView getQuiz(@PathVariable int id) {
        return toView(findQuiz(id));
    }

    @GetMapping
    public Page<QuizView> getAllQuizzes(@RequestParam(defaultValue = "0") int page) {
        return quizRepository.findAll(PageRequest.of(page, PAGE_SIZE, Sort.by(Sort.Direction.ASC, "id")))
                .map(this::toView);
    }

    @GetMapping("/completed")
    public Page<CompletionView> getCompletedQuizzes(
            @RequestParam(defaultValue = "0") int page,
            Authentication authentication
    ) {
        return completionRepository
                .findByUserEmailOrderByCompletedAtDescIdDesc(
                        authentication.getName(),
                        PageRequest.of(page, PAGE_SIZE)
                )
                .map(completion -> new CompletionView(
                        completion.getQuiz().getId(),
                        completion.getCompletedAt()
                ));
    }

    @PostMapping("/{id}/solve")
    public AnswerResponse solveQuiz(
            @PathVariable int id,
            @RequestBody SolveRequest request,
            Authentication authentication
    ) {
        QuizEntity quiz = findQuiz(id);
        Set<Integer> submittedAnswers = request.answer() == null
                ? Set.of()
                : new HashSet<>(request.answer());
        boolean correct = quiz.getAnswer().equals(submittedAnswers);
        if (correct) {
            UserEntity user = findUser(authentication);
            OffsetDateTime completedAt = OffsetDateTime.now(ZoneOffset.UTC);
            completedAt = completedAt.withNano((completedAt.getNano() / 1_000_000) * 1_000_000);
            completionRepository.save(new QuizCompletionEntity(quiz, user, completedAt));
            return new AnswerResponse(true, "Congratulations, you're right!");
        }

        return new AnswerResponse(false, "Wrong answer! Please, try again.");
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void deleteQuiz(@PathVariable int id, Authentication authentication) {
        QuizEntity quiz = findQuiz(id);
        if (quiz.getAuthor() == null || !quiz.getAuthor().getEmail().equals(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this quiz");
        }

        completionRepository.deleteAllByQuiz(quiz);
        quizRepository.delete(quiz);
    }

    private QuizEntity findQuiz(int id) {
        return quizRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found"));
    }

    private UserEntity findUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
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

    public record CompletionView(
            int id,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
            OffsetDateTime completedAt
    ) {
    }
}
