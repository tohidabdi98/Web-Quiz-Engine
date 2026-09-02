package com.tohidabdi.webquizengine;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QuizController.class)
class QuizControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getQuizReturnsTheFixedQuiz() throws Exception {
        mockMvc.perform(get("/api/quiz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("The Java Logo"))
                .andExpect(jsonPath("$.text").value("What is depicted on the Java logo?"))
                .andExpect(jsonPath("$.options").isArray())
                .andExpect(jsonPath("$.options.length()").value(4))
                .andExpect(jsonPath("$.options[2]").value("Cup of coffee"));
    }

    @Test
    void correctAnswerReturnsSuccess() throws Exception {
        mockMvc.perform(post("/api/quiz").param("answer", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.feedback").value("Congratulations, you're right!"));
    }

    @Test
    void incorrectAnswerReturnsFailure() throws Exception {
        mockMvc.perform(post("/api/quiz").param("answer", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.feedback").value("Wrong answer! Please, try again."));
    }
}
