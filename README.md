# AI-Powered Code Review Assistant

An in-progress portfolio project that will review GitHub pull requests and provide context-aware code review suggestions.

## Project Goal

The completed application will:

* Receive pull-request events from GitHub
* Analyze changed code using an AI model
* Identify potential bugs, security concerns, and maintainability problems
* Post review suggestions on the pull request
* Track review results and code-quality metrics

## Current Status

The project is in early development.

## Current Review Rules

| Rule | Category | Severity |
| --- | --- | --- |
| Use of `System.out.println` | `MAINTAINABILITY` | `LOW` |
| Potential hardcoded password, API key, secret, or token | `SECURITY` | `HIGH` |

The hardcoded-secret rule uses basic pattern matching and is not intended to replace a production security scanner.

Currently implemented:

* Spring Boot application setup
* REST API controller
* Code-review endpoint
* Structured review findings
* Review category and severity enums
* Service layer that coordinates code-review requests
* Replaceable `CodeAnalyzer` interface
* Dedicated rule-based code analyzer
* Constructor-based dependency injection
* POST endpoint for submitting code-review requests
* JSON request deserialization
* Input validation for code-review requests
* Consistent JSON responses for validation errors
* Support for multiple review findings in a single response
* Detection of `System.out.println` statements
* Accurate line numbers for review findings
* Support for reviews with no findings
* Automated tests for API responses, request validation, and review logic
* Basic detection of potential hardcoded secrets
* GitHub pull-request webhook endpoint
* GitHub webhook event and delivery header handling
* Pull-request webhook payload deserialization
* Filtering of pull-request webhook actions

The endpoint currently performs a small rule-based code review that detects `System.out.println` statements and potential hardcoded secrets. It is not connected to GitHub or an AI model yet.

## Technologies

* Java 21
* Spring Boot
* Maven
* JUnit 5
* Postman

Additional technologies will be added as the project develops.

## Running the Application

On Windows:

```shell
.\mvnw.cmd spring-boot:run
```

After the application starts, use Postman to send a `POST` request to:

```text
http://localhost:8080/api/reviews
```

Select **Body → raw → JSON** and enter:

```json
{
  "filePath": "src/main/java/ReviewService.java",
  "language": "Java",
  "code": "public class ReviewService {\n    public void processReview() {\n        System.out.println(\"Processing review\");\n    }\n}"
}
```

The application detects the `System.out.println` statement and returns a maintainability finding with its line number:

```json
{
  "findings": [
    {
      "filePath": "src/main/java/ReviewService.java",
      "lineNumber": 3,
      "category": "MAINTAINABILITY",
      "severity": "LOW",
      "message": "Avoid System.out.println in application code. Use a logger instead."
    }
  ]
}
```

If the submitted code does not contain any recognized problems, the endpoint returns an empty findings list:

```json
{
  "findings": []
}
```

## Request Validation

All code-review requests must include:

* `filePath`: Required, maximum 500 characters
* `language`: Required, maximum 50 characters
* `code`: Required, maximum 50,000 characters

Missing, empty, or blank fields return a `400 Bad Request` response.

## Validation Error Response

Validation errors identify each invalid field and explain why the request was rejected.

Example:

```json
{
  "timestamp": "2026-08-13T16:59:41.351801800Z",
  "status": 400,
  "error": "Bad Request",
  "fieldErrors": {
    "code": "Code cannot be blank"
  }
}
```

## GitHub Webhook Endpoint

The application includes an endpoint for receiving GitHub-style pull-request webhook payloads:

```text
POST http://localhost:8080/api/github/webhooks
```

## Running the Tests

Run the automated test suite on Windows:

```shell
.\mvnw.cmd test
```

The tests verify:

* Successful API requests and JSON responses
* Request validation and validation error responses
* Detection of `System.out.println` statements
* Correct review-finding line numbers
* Multiple findings in one response
* Empty responses when no problems are detected
* Detection of potential hardcoded secrets
* Exclusion of secrets loaded from environment variables
* GitHub webhook payload deserialization
* GitHub event and action filtering
* Required GitHub webhook headers

## Project Status

This project is actively being developed as part of my software engineering portfolio.
