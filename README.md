# Web Quiz Engine

Stage 1 of the Web Quiz Engine project: a simple Spring Boot JSON API.

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

## Test

```bash
mvn test
```
