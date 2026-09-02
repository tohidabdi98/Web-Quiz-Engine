# Web Quiz Engine

Stages 1 and 2 of the Web Quiz Engine project: a Spring Boot JSON API for creating and solving quizzes.

## Run

```bash
mvn spring-boot:run
```

## API

### Get the quiz

```http
GET /api/quiz
```

### Submit an answer

```http
POST /api/quiz?answer=2
```

The answer index is zero-based. The correct answer for the fixed Stage 1 quiz is index `2`.

## Multiple quizzes

Stage 2 stores quizzes in memory and exposes:

- `POST /api/quizzes`
- `GET /api/quizzes/{id}`
- `GET /api/quizzes`
- `POST /api/quizzes/{id}/solve?answer={index}`

Create requests contain `title`, `text`, `options`, and `answer`. The answer is never included when retrieving or listing quizzes.

## Test

```bash
mvn test
```
