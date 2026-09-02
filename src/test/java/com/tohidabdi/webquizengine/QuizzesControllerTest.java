package com.tohidabdi.webquizengine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
class QuizzesControllerTest {

    private static final String OWNER_EMAIL = "owner@example.com";
    private static final String OWNER_PASSWORD = "ownerpass";
    private static final String OTHER_EMAIL = "other@example.com";
    private static final String OTHER_PASSWORD = "otherpass";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private QuizCompletionRepository completionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUpUsers() {
        completionRepository.deleteAll();
        completionRepository.flush();
        quizRepository.deleteAll();
        quizRepository.flush();
        userRepository.deleteAll();
        userRepository.save(new UserEntity(OWNER_EMAIL, passwordEncoder.encode(OWNER_PASSWORD)));
        userRepository.save(new UserEntity(OTHER_EMAIL, passwordEncoder.encode(OTHER_PASSWORD)));
    }

    @Test
    void newQuizCanBeCreatedAndRetrievedWithoutAnswer() throws Exception {
        String quiz = """
                {
                  "title": "The Java Logo",
                  "text": "What is depicted on the Java logo?",
                  "options": ["Robot", "Tea leaf", "Cup of coffee", "Bug"],
                  "answer": [2]
                }
                """;

        int id = createQuiz(quiz, OWNER_EMAIL, OWNER_PASSWORD);

        mockMvc.perform(get("/api/quizzes/{id}", id).header(HttpHeaders.AUTHORIZATION, auth(OWNER_EMAIL, OWNER_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("The Java Logo"))
                .andExpect(jsonPath("$.options[2]").value("Cup of coffee"))
                .andExpect(jsonPath("$.answer").doesNotExist());
    }

    @Test
    void allQuizzesAreReturnedInIdOrderWithoutAnswers() throws Exception {
        int firstId = createQuiz("""
                {"title":"First","text":"Question 1","options":["A","B"],"answer":[0]}
                """, OWNER_EMAIL, OWNER_PASSWORD);
        int secondId = createQuiz("""
                {"title":"Second","text":"Question 2","options":["C","D"],"answer":[1]}
                """, OWNER_EMAIL, OWNER_PASSWORD);

        mockMvc.perform(get("/api/quizzes?page=0").header(HttpHeaders.AUTHORIZATION, auth(OWNER_EMAIL, OWNER_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(firstId))
                .andExpect(jsonPath("$.content[0].title").value("First"))
                .andExpect(jsonPath("$.content[1].id").value(secondId))
                .andExpect(jsonPath("$.content[1].title").value("Second"))
                .andExpect(jsonPath("$.content[0].answer").doesNotExist())
                .andExpect(jsonPath("$.content[1].answer").doesNotExist());
    }

    @Test
    void emptyServiceReturnsAnEmptyArray() throws Exception {
        mockMvc.perform(get("/api/quizzes?page=0").header(HttpHeaders.AUTHORIZATION, auth(OWNER_EMAIL, OWNER_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.empty").value(true));
    }

    @Test
    void quizCanBeSolvedWithCorrectOrIncorrectAnswer() throws Exception {
        int id = createQuiz("""
                {"title":"Quiz","text":"Question","options":["A","B","C"],"answer":[0,2]}
                """, OWNER_EMAIL, OWNER_PASSWORD);

        mockMvc.perform(post("/api/quizzes/{id}/solve", id)
                        .header(HttpHeaders.AUTHORIZATION, auth(OTHER_EMAIL, OTHER_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answer":[2,0]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.feedback").value("Congratulations, you're right!"));

        mockMvc.perform(post("/api/quizzes/{id}/solve", id)
                        .header(HttpHeaders.AUTHORIZATION, auth(OTHER_EMAIL, OTHER_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answer":[0]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.feedback").value("Wrong answer! Please, try again."));
    }

    @Test
    void missingQuizReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/quizzes/15").header(HttpHeaders.AUTHORIZATION, auth(OWNER_EMAIL, OWNER_PASSWORD)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/quizzes/15/solve")
                        .header(HttpHeaders.AUTHORIZATION, auth(OWNER_EMAIL, OWNER_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answer":[1]}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void missingTitleIsRejected() throws Exception {
        mockMvc.perform(post("/api/quizzes")
                        .header(HttpHeaders.AUTHORIZATION, auth(OWNER_EMAIL, OWNER_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "text": "Question",
                                  "options": ["A", "B"],
                                  "answer": []
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void blankTitleIsRejected() throws Exception {
        mockMvc.perform(post("/api/quizzes")
                        .header(HttpHeaders.AUTHORIZATION, auth(OWNER_EMAIL, OWNER_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "",
                                  "text": "Question",
                                  "options": ["A", "B"],
                                  "answer": []
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void fewerThanTwoOptionsAreRejected() throws Exception {
        mockMvc.perform(post("/api/quizzes")
                        .header(HttpHeaders.AUTHORIZATION, auth(OWNER_EMAIL, OWNER_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Quiz",
                                  "text": "Question",
                                  "options": ["Only one"],
                                  "answer": []
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void quizzesWithNoCorrectOptionsCanBeSolvedWithAnEmptyAnswer() throws Exception {
        int id = createQuiz("""
                {"title":"Quiz","text":"Question","options":["A","B"],"answer":[]}
                """, OWNER_EMAIL, OWNER_PASSWORD);

        mockMvc.perform(post("/api/quizzes/{id}/solve", id)
                        .header(HttpHeaders.AUTHORIZATION, auth(OWNER_EMAIL, OWNER_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answer":[]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void quizzesAreReturnedInPagesOfTen() throws Exception {
        for (int index = 0; index < 11; index++) {
            createQuiz("""
                    {"title":"Quiz %d","text":"Question","options":["A","B"],"answer":[]}
                    """.formatted(index), OWNER_EMAIL, OWNER_PASSWORD);
        }

        mockMvc.perform(get("/api/quizzes?page=0")
                        .header(HttpHeaders.AUTHORIZATION, auth(OWNER_EMAIL, OWNER_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(11))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.numberOfElements").value(10))
                .andExpect(jsonPath("$.content.length()").value(10));

        mockMvc.perform(get("/api/quizzes?page=1")
                        .header(HttpHeaders.AUTHORIZATION, auth(OWNER_EMAIL, OWNER_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.numberOfElements").value(1))
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void successfulCompletionsAreStoredAndReturnedNewestFirst() throws Exception {
        int firstQuizId = createQuiz("""
                {"title":"First","text":"Question","options":["A","B"],"answer":[0]}
                """, OWNER_EMAIL, OWNER_PASSWORD);
        int secondQuizId = createQuiz("""
                {"title":"Second","text":"Question","options":["A","B"],"answer":[1]}
                """, OWNER_EMAIL, OWNER_PASSWORD);

        solveQuiz(firstQuizId, "[0]");
        Thread.sleep(20);
        solveQuiz(secondQuizId, "[1]");
        Thread.sleep(20);
        solveQuiz(firstQuizId, "[0]");

        mockMvc.perform(get("/api/quizzes/completed?page=0")
                        .header(HttpHeaders.AUTHORIZATION, auth(OWNER_EMAIL, OWNER_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.content[0].id").value(firstQuizId))
                .andExpect(jsonPath("$.content[1].id").value(secondQuizId))
                .andExpect(jsonPath("$.content[2].id").value(firstQuizId))
                .andExpect(jsonPath("$.content[0].completedAt").isString())
                .andExpect(jsonPath("$.content[1].completedAt").isString());
    }

    @Test
    void quizCanBeDeletedByItsAuthor() throws Exception {
        int id = createQuiz("""
                {"title":"Deletable","text":"Question","options":["A","B"],"answer":[0]}
                """, OWNER_EMAIL, OWNER_PASSWORD);

        solveQuiz(id, "[0]");

        mockMvc.perform(delete("/api/quizzes/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, auth(OWNER_EMAIL, OWNER_PASSWORD)))
                .andExpect(status().isNoContent())
                .andExpect(jsonPath("$").doesNotExist());

        mockMvc.perform(get("/api/quizzes/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, auth(OWNER_EMAIL, OWNER_PASSWORD)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/quizzes/completed?page=0")
                        .header(HttpHeaders.AUTHORIZATION, auth(OWNER_EMAIL, OWNER_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void quizCannotBeDeletedByAnotherUser() throws Exception {
        int id = createQuiz("""
                {"title":"Protected","text":"Question","options":["A","B"],"answer":[0]}
                """, OWNER_EMAIL, OWNER_PASSWORD);

        mockMvc.perform(delete("/api/quizzes/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, auth(OTHER_EMAIL, OTHER_PASSWORD)))
                .andExpect(status().isForbidden());
    }

    @Test
    void quizOperationsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/quizzes?page=0"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Quiz","text":"Question","options":["A","B"],"answer":[]}
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/quizzes/completed?page=0"))
                .andExpect(status().isUnauthorized());
    }

    private int createQuiz(String quiz, String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/quizzes")
                        .header(HttpHeaders.AUTHORIZATION, auth(email, password))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(quiz))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode responseJson = objectMapper.readTree(response);
        return responseJson.get("id").asInt();
    }

    private String auth(String email, String password) {
        String credentials = email + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    private void solveQuiz(int id, String answer) throws Exception {
        mockMvc.perform(post("/api/quizzes/{id}/solve", id)
                        .header(HttpHeaders.AUTHORIZATION, auth(OWNER_EMAIL, OWNER_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answer":%s}
                                """.formatted(answer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
