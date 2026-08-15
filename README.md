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

Currently implemented:

* Spring Boot application setup
* REST API controller
* Sample code-review endpoint
* Structured review findings
* Review category and severity enums
* Service layer for sample review logic
* Constructor-based dependency injection
* POST endpoint for submitting code-review requests
* JSON request deserialization
* Input validation for code-review requests
* Consistent JSON responses for validation errors
* Automated tests for API responses and request validation
* Support for multiple review findings in a single response

The current endpoint returns sample data. It is not connected to GitHub or an AI model yet.

## Technologies

* Java 21
* Spring Boot
* Maven

Additional technologies will be added as the project develops.

## Running the Application

On Windows:

```shell
mvnw.cmd spring-boot:run
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
  "code": "public void processReview() {}"
}
```

* All three request fields are required.
* Blank fields result in 400 Bad Request.
* Request fields have maximum sizes.

Example response:

```json
{
  "filePath": "src/main/java/ReviewService.java",
  "lineNumber": 12,
  "category": "BUG",
  "severity": "HIGH",
  "message": "This value could be null."
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

## Running the Tests

Run the automated test suite on Windows:

```shell
mvnw.cmd test
```

## Project Status

This project is actively being developed as part of my software engineering portfolio.
