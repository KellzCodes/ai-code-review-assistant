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

After the application starts, open:

```text
http://localhost:8080/api/reviews/test
```

The endpoint returns a sample review finding:

```json
{
  "filepath": "src/main/java/Review.java",
  "lineNumber": 12,
  "category": "BUG",
  "severity": "HIGH",
  "message": "This value could be null."
}
```

## Project Status

This project is actively being developed as part of my software engineering portfolio.
