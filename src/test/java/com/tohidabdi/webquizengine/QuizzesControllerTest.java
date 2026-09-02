package com.tohidabdi.webquizengine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

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

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private QuizRepository quizRepository;

    @BeforeEach
    void clearDatabase() {
        quizRepository.deleteAll();
        quizRepository.flush();
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

        int id = createQuiz(quiz);

        mockMvc.perform(get("/api/quizzes/{id}", id))
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
                """);
        int secondId = createQuiz("""
                {"title":"Second","text":"Question 2","options":["C","D"],"answer":[1]}
                """);

        mockMvc.perform(get("/api/quizzes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(firstId))
                .andExpect(jsonPath("$[0].title").value("First"))
                .andExpect(jsonPath("$[1].id").value(secondId))
                .andExpect(jsonPath("$[1].title").value("Second"))
                .andExpect(jsonPath("$[0].answer").doesNotExist())
                .andExpect(jsonPath("$[1].answer").doesNotExist());
    }

    @Test
    void emptyServiceReturnsAnEmptyArray() throws Exception {
        mockMvc.perform(get("/api/quizzes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void quizCanBeSolvedWithCorrectOrIncorrectAnswer() throws Exception {
        int id = createQuiz("""
                {"title":"Quiz","text":"Question","options":["A","B","C"],"answer":[0,2]}
                """);

        mockMvc.perform(post("/api/quizzes/{id}/solve", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answer":[2,0]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.feedback").value("Congratulations, you're right!"));

        mockMvc.perform(post("/api/quizzes/{id}/solve", id)
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
        mockMvc.perform(get("/api/quizzes/15"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/quizzes/15/solve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answer":[1]}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void missingTitleIsRejected() throws Exception {
        mockMvc.perform(post("/api/quizzes")
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
                """);

        mockMvc.perform(post("/api/quizzes/{id}/solve", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answer":[]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void quizSurvivesRepositoryReload() throws Exception {
        int id = createQuiz("""
                {"title":"Persistent","text":"Question","options":["A","B"],"answer":[1]}
                """);

        quizRepository.flush();

        mockMvc.perform(get("/api/quizzes/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Persistent"))
                .andExpect(jsonPath("$.options[1]").value("B"));
    }

    private int createQuiz(String quiz) throws Exception {
        String response = mockMvc.perform(post("/api/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(quiz))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode responseJson = objectMapper.readTree(response);
        return responseJson.get("id").asInt();
    }
}
