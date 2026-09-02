package com.tohidabdi.webquizengine;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/quiz")
public class QuizController {

    private static final Quiz QUIZ = new Quiz(
            "The Java Logo",
            "What is depicted on the Java logo?",
            List.of("Robot", "Tea leaf", "Cup of coffee", "Bug")
    );

    @GetMapping
    public Quiz getQuiz() {
        return QUIZ;
    }

    @PostMapping
    public AnswerResponse solveQuiz(@RequestParam int answer) {
        if (answer == 2) {
            return new AnswerResponse(true, "Congratulations, you're right!");
        }

        return new AnswerResponse(false, "Wrong answer! Please, try again.");
    }

    public record Quiz(String title, String text, List<String> options) {
    }

    public record AnswerResponse(boolean success, String feedback) {
    }
}
