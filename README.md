# Web Quiz Engine

Stages 1-4 of the Web Quiz Engine project: a Spring Boot JSON API for creating and solving quizzes.

## Run

```bash
mvn spring-boot:run
```

## API

### Get the fixed Stage 1 quiz

```http
GET /api/quiz
```

### Submit the fixed Stage 1 answer

```http
POST /api/quiz?answer=2
```

### Multiple quizzes

The multi-quiz API exposes:

- `POST /api/quizzes`
- `GET /api/quizzes/{id}`
- `GET /api/quizzes`
- `POST /api/quizzes/{id}/solve`

Create requests must contain a non-blank `title`, a non-blank `text`, and at least two `options`.
The `answer` is an array of correct option indexes and may be empty or absent when no option is correct.
Solve requests send an `answer` array. The submitted indexes are compared as a set.
The answer is never included when retrieving or listing quizzes.

Stage 4 persists quizzes in a disk-based H2 database named `quizdb`, so quizzes survive service restarts.

## Test

```bash
mvn test
```
