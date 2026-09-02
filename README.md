# Web Quiz Engine

Stages 1-5 of the Web Quiz Engine project: a Spring Boot JSON API for creating and solving quizzes.

## Run

```bash
mvn spring-boot:run
```

## Registration and authentication

Register a user with the public endpoint:

```http
POST /api/register
Content-Type: application/json

{"email":"test@mail.org","password":"strongpassword"}
```

The email must be valid and the password must contain at least five characters.
All quiz endpoints require HTTP Basic authentication using the registered email and
password. Passwords are stored as BCrypt hashes.

## API

### Get and solve the fixed Stage 1 quiz

```http
GET /api/quiz
POST /api/quiz?answer=2
```

These endpoints require authentication.

### Multiple quizzes

The multi-quiz API exposes:

- `POST /api/quizzes`
- `GET /api/quizzes/{id}`
- `GET /api/quizzes`
- `POST /api/quizzes/{id}/solve`
- `DELETE /api/quizzes/{id}`

Create requests must contain a non-blank `title`, a non-blank `text`, and at least
two `options`. The `answer` is an array of correct option indexes and may be empty
or absent when no option is correct. Solve requests send an `answer` array; the
submitted indexes are compared as a set.

The answer is never included when retrieving or listing quizzes. Only the user who
created a quiz can delete it.

Stage 4 persists quizzes in a disk-based H2 database named `quizdb`, so quizzes
survive service restarts. Stage 5 also persists registered users.

## Test

```bash
mvn test
```
