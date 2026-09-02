package com.tohidabdi.webquizengine;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.annotation.DirtiesContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QuizzesController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class QuizzesControllerTest {

    @Autowired
    private MockMvc mockMvc;

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

        mockMvc.perform(post("/api/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(quiz))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("The Java Logo"))
                .andExpect(jsonPath("$.options[2]").value("Cup of coffee"))
                .andExpect(jsonPath("$.answer").doesNotExist());

        mockMvc.perform(get("/api/quizzes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("The Java Logo"))
                .andExpect(jsonPath("$.answer").doesNotExist());
    }

    @Test
    void allQuizzesAreReturnedInIdOrderWithoutAnswers() throws Exception {
        createQuiz("""
                {"title":"First","text":"Question 1","options":["A","B"],"answer":[0]}
                """);
        createQuiz("""
                {"title":"Second","text":"Question 2","options":["C","D"],"answer":[1]}
                """);

        mockMvc.perform(get("/api/quizzes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("First"))
                .andExpect(jsonPath("$[1].id").value(2))
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
        createQuiz("""
                {"title":"Quiz","text":"Question","options":["A","B","C"],"answer":[0,2]}
                """);

        mockMvc.perform(post("/api/quizzes/1/solve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answer":[2,0]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.feedback").value("Congratulations, you're right!"));

        mockMvc.perform(post("/api/quizzes/1/solve")
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
        createQuiz("""
                {"title":"Quiz","text":"Question","options":["A","B"],"answer":[]}
                """);

        mockMvc.perform(post("/api/quizzes/1/solve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answer":[]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    private void createQuiz(String quiz) throws Exception {
        mockMvc.perform(post("/api/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(quiz))
                .andExpect(status().isOk());
    }
}
