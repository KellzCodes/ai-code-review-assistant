# AI Powered Code Review Assistant

An in-progress portfolio project that will review GitHub pull requests and provide context-aware code review suggestions.

## Project Goal

The completed application will:

- Receive pull request events from GitHub
- Retrieve changed files from pull requests
- Analyze changed code using AI and rule-based review logic
- Identify bugs, security issues, and maintainability problems
- Post review suggestions back to pull requests
- Track review activity and code quality metrics over time

## Current Status

The project is in early development.

The application currently has two working areas:

1. A REST API for submitting code review requests directly
2. A GitHub webhook pipeline that validates pull request events, retrieves changed pull request files from GitHub, extracts reviewable added code, analyzes it with the existing review rules, and keeps a pull request summary comment in sync on GitHub

The application does not yet:

- run AI-powered review analysis on GitHub pull request files automatically
- post inline review comments back to GitHub
- use a fine-tuned AI model
- store review history or metrics
- support version control systems beyond GitHub

## Current Review Rules

| Rule | Category | Severity |
| --- | --- | --- |
| Use of `System.out.println` | `MAINTAINABILITY` | `LOW` |
| Potential hardcoded password, API key, secret, or token | `SECURITY` | `HIGH` |

The hardcoded-secret rule uses basic pattern matching and is not intended to replace a production security scanner.

## Implemented So Far

- Spring Boot application setup
- REST API controller for code review requests
- Request validation for code review input
- Validation error responses
- Support for multiple review findings
- Rule-based code analysis
- Detection of `System.out.println`
- Detection of potential hardcoded secrets
- Accurate line number reporting
- Replaceable `CodeAnalyzer` abstraction
- GitHub pull request webhook endpoint
- GitHub webhook event and delivery header handling
- HMAC-SHA256 webhook signature verification
- Graceful handling of malformed webhook payloads
- Pull request webhook payload validation
- Retrieval of changed pull request files from GitHub for supported webhook events
- Preparation of reviewable pull request files by filtering skipped files and resolving languages
- Extraction of added reviewable code from GitHub pull request patches
- Automatic rule-based analysis of extracted pull request code during supported webhook events
- Mapping GitHub pull request review findings back to real file line numbers
- Posting and updating pull request review summary comments on GitHub
- Automated tests for API, webhook, validation, and analysis behavior

## Technologies

- Java 21
- Spring Boot
- Maven
- JUnit 5
- Mockito
- AssertJ

## Configuration

The application uses environment variables for secrets and GitHub API access.

### Required Environment Variables

- `GITHUB_WEBHOOK_SECRET`
  - Used to verify the `X-Hub-Signature-256` webhook signature from GitHub
- `GITHUB_API_TOKEN`
  - Used to authenticate GitHub API requests when retrieving changed pull request files and managing pull request comments

### Optional Environment Variables

- `GITHUB_API_BASE_URL`
  - Defaults to `https://api.github.com`
  - Useful for testing against GitHub Enterprise or a mock server

### Example PowerShell Setup

```powershell
$env:GITHUB_WEBHOOK_SECRET = "your-webhook-secret"
$env:GITHUB_API_TOKEN = "your-github-token"
$env:GITHUB_API_BASE_URL = "https://api.github.com"
```

## Running the Application

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

The application starts on:

```text
http://localhost:8080
```

## Code Review API

Submit code for manual review with:

```text
POST http://localhost:8080/api/reviews
```

Example request body:

```json
{
  "filePath": "src/main/java/ReviewService.java",
  "language": "Java",
  "code": "public class ReviewService {\n    public void processReview() {\n        System.out.println(\"Processing review\");\n    }\n}"
}
```

Example response:

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

If the submitted code contains no recognized issues, the API returns:

```json
{
  "findings": []
}
```

## Request Validation

All code review requests must include:

- `filePath`: required, maximum 500 characters
- `language`: required, maximum 50 characters
- `code`: required, maximum 50,000 characters

Missing, blank, or oversized values return `400 Bad Request`.

Example validation error response:

```json
{
  "timestamp": "2026-08-25T16:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "fieldErrors": {
    "code": "Code cannot be blank"
  }
}
```

## GitHub Webhook API

Receive GitHub pull request webhooks at:

```text
POST http://localhost:8080/api/github/webhooks
```

### Supported Webhook Flow

For supported GitHub webhook events, the application currently does the following:

1. Verifies the `X-Hub-Signature-256` header
2. Parses and validates the webhook payload
3. Accepts supported `pull_request` actions:
   - `opened`
   - `reopened`
   - `synchronize`
4. Calls the GitHub API to retrieve the changed files in that pull request
5. Prepares reviewable files by:
   - keeping files that include patch content
   - skipping removed files
   - skipping files with missing or blank patch data
   - inferring the programming language from the file extension when possible
6. Extracts only added reviewable code from each prepared patch
7. Runs the existing rule-based analyzer against the extracted code
8. Maps review findings from extracted snippet line numbers back to real pull request file lines
9. Creates a new pull request review summary comment on GitHub, or updates the existing assistant summary comment if one is already present
10. Returns an accepted response describing how many files were prepared, analyzed, how many review findings were generated, and which file lines were flagged

### Pull Request File Preparation

The webhook pipeline transforms GitHub file responses into an internal review-ready shape before analysis runs.

Each prepared file currently includes:

- file path
- inferred language
- GitHub change status
- patch text
- additions count
- deletions count

This stage now performs rule-based analysis on extracted pull request code before the webhook flow formats and synchronizes a summary comment on GitHub.

### Pull Request Patch Extraction And Review

After file preparation, the application extracts only added lines from each GitHub patch and ignores diff metadata, removed lines, and unchanged context lines.

While extracting added code, the application also keeps track of the real pull request file line number for each extracted line. That allows later review findings to point back to the actual changed file line instead of the temporary extracted snippet line.

The extracted code is then sent through the existing rule-based `CodeAnalyzer`, which currently checks for:

- `System.out.println` usage
- possible hardcoded secrets

The webhook response now returns a pull request review summary with analyzed file counts, total findings, mapped findings that use real file line numbers, and information about the synchronized GitHub summary comment.

### Example Accepted Response

```json
{
  "status": "ACCEPTED",
  "deliveryId": "delivery-123",
  "eventType": "pull_request",
  "action": "opened",
  "repository": "kellidavis/ai-code-review-assistant",
  "pullRequestNumber": 42,
  "totalChangedFiles": 3,
  "preparedFiles": 2,
  "skippedFiles": 1,
  "reviewedFiles": 2,
  "totalFindings": 2,
  "summaryCommentPosted": true,
  "summaryCommentUrl": "https://github.com/kellidavis/ai-code-review-assistant/pull/42#issuecomment-1",
  "findings": [
    {
      "filePath": "src/main/java/PaymentService.java",
      "lineNumber": 2,
      "category": "MAINTAINABILITY",
      "severity": "LOW",
      "message": "Avoid System.out.println in application code. Use a logger instead."
    },
    {
      "filePath": "src/main/java/OrderService.java",
      "lineNumber": 2,
      "category": "SECURITY",
      "severity": "HIGH",
      "message": "Possible hardcoded secret detected. Store sensitive values in environment variables or a secret manager."
    }
  ],
  "message": "Pull request event accepted and 2 file(s) were prepared from 3 changed file(s). 1 file(s) were skipped. 2 file(s) were analyzed and 2 review finding(s) were generated. A summary comment was posted on the pull request."
}
```

### Example Ignored Response

Unsupported event types or pull request actions are ignored safely.

```json
{
  "status": "IGNORED",
  "deliveryId": "delivery-456",
  "eventType": "push",
  "action": "opened",
  "repository": "kellidavis/ai-code-review-assistant",
  "pullRequestNumber": 42,
  "totalChangedFiles": 0,
  "preparedFiles": 0,
  "skippedFiles": 0,
  "reviewedFiles": 0,
  "totalFindings": 0,
  "summaryCommentPosted": false,
  "summaryCommentUrl": null,
  "findings": [],
  "message": "Webhook event type is not supported."
}
```

## Webhook Error Handling

### Invalid or Missing Signature

Returns `401 Unauthorized`.

### Malformed JSON Payload

Returns `400 Bad Request`.

### GitHub API Failure

If the webhook is valid but the application cannot retrieve pull request files from GitHub, it returns `502 Bad Gateway` with a structured JSON error response.

## Running the Tests

Run the full test suite on Windows:

```powershell
.\mvnw.cmd clean test
```

## Project Status

This project is actively being developed as part of a software engineering portfolio. The current focus is building the GitHub integration before adding full AI-powered pull request review behavior.
Smoke test PR for comment support commit
