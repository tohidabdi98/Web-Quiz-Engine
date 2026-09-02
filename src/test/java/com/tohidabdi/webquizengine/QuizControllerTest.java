package com.tohidabdi.webquizengine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:quiztest;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class QuizControllerTest {

    private static final String EMAIL = "stage5@example.com";
    private static final String PASSWORD = "secret";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private QuizCompletionRepository completionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUpUser() {
        completionRepository.deleteAll();
        completionRepository.flush();
        quizRepository.deleteAll();
        quizRepository.flush();
        userRepository.deleteAll();
        userRepository.save(new UserEntity(EMAIL, passwordEncoder.encode(PASSWORD)));
    }

    @Test
    void getQuizReturnsTheFixedQuiz() throws Exception {
        mockMvc.perform(get("/api/quiz").header(HttpHeaders.AUTHORIZATION, basicAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("The Java Logo"))
                .andExpect(jsonPath("$.text").value("What is depicted on the Java logo?"))
                .andExpect(jsonPath("$.options").isArray())
                .andExpect(jsonPath("$.options.length()").value(4))
                .andExpect(jsonPath("$.options[2]").value("Cup of coffee"));
    }

    @Test
    void correctAnswerReturnsSuccess() throws Exception {
        mockMvc.perform(post("/api/quiz?answer=2").header(HttpHeaders.AUTHORIZATION, basicAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.feedback").value("Congratulations, you're right!"));
    }

    @Test
    void incorrectAnswerReturnsFailure() throws Exception {
        mockMvc.perform(post("/api/quiz?answer=1").header(HttpHeaders.AUTHORIZATION, basicAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.feedback").value("Wrong answer! Please, try again."));
    }

    @Test
    void fixedQuizRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/quiz"))
                .andExpect(status().isUnauthorized());
    }

    private String basicAuth() {
        String credentials = EMAIL + ":" + PASSWORD;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
}
