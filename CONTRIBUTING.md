# Contributing to Open Quality Hub

Thank you for your interest in contributing to Open Quality Hub. This guide will help you get started.

## Getting Started

1. Fork the repository
2. Clone your fork locally
3. Create a feature branch: `git checkout -b feature/your-feature-name`
4. Make your changes
5. Run tests to make sure everything passes
6. Commit your changes with a clear message
7. Push to your fork and open a Pull Request

## Development Setup

### Prerequisites

- Java 21+
- Node.js 20+
- PostgreSQL 16+
- Maven 3.8+

### Backend

```bash
cd backend
mvn spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

### Running Tests

```bash
# Backend tests
cd backend
mvn test

# Frontend type checking
cd frontend
npx tsc --noEmit
```

## Code Style

### Java

- Follow standard Java naming conventions
- Use Lombok for boilerplate reduction
- Use DTOs for API request/response (never expose entities directly)
- Write service interfaces with implementation classes
- Add validation annotations to request DTOs

### TypeScript/React

- Use TypeScript strict mode
- Use functional components with hooks
- Keep components focused and small
- Use the `@/` path alias for imports

## Pull Request Guidelines

- Keep PRs focused on a single concern
- Write clear commit messages explaining the "why"
- Ensure all tests pass before submitting
- Update documentation if you change public APIs
- Add tests for new features

## Reporting Issues

- Use GitHub Issues to report bugs or suggest features
- Include steps to reproduce for bugs
- Include expected vs actual behavior
- Mention your environment (OS, Java version, Node version)

## License

By contributing, you agree that your contributions will be licensed under the [AGPL-3.0 License](LICENSE).
