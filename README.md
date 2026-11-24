<div align="center">

<img src="frontend/public/logo.svg" alt="Open Quality Hub" width="280" />

### Quality Engineering & Test Management Platform

One command center for test intelligence, release confidence, and cross-team QA governance.

[![CI](https://github.com/saadfikri/OpenQualityHub/actions/workflows/ci.yml/badge.svg)](https://github.com/saadfikri/OpenQualityHub/actions/workflows/ci.yml)
[![License: AGPL-3.0](https://img.shields.io/badge/License-AGPL%20v3-blue.svg)](LICENSE)
[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot 3.2](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React 19](https://img.shields.io/badge/React-19-61DAFB.svg)](https://react.dev/)
[![PostgreSQL 17](https://img.shields.io/badge/PostgreSQL-17-336791.svg)](https://www.postgresql.org/)

[Website](https://openqualityhub.org) · [Getting Started](#getting-started) · [Contributing](CONTRIBUTING.md)

</div>

---

## Why Open Quality Hub?

Most QA teams juggle fragmented tools — test results in CI logs, defects in Jira, metrics in spreadsheets, and no single source of truth.

Open Quality Hub brings the entire quality lifecycle into one platform:

- **Centralize** test results from any CI/CD pipeline (GitLab, Jenkins, GitHub Actions)
- **Detect** flaky tests with stable fingerprinting and trend analysis
- **Analyze** failures with AI-powered root cause categorization
- **Track** release readiness with quality gates and multi-role sign-off
- **Manage** cross-team QA requests with SLA monitoring and capacity planning
- **Generate** synthetic test data with 11 field types and 6 strategies

<div align="center">
<img src="docs/screenshots/dashboard.png" alt="Quality Command Center" width="800" />
<p><em>Quality Command Center — real-time metrics across all projects</em></p>
</div>

## Capabilities

| Area | What it does |
|------|-------------|
| **Quality Dashboard** | Pass rate trends, defect density, automation coverage, quality scorecards, CSV export |
| **Test Intelligence** | Flaky test detection, stable fingerprinting, AI failure analysis (9 categories, confidence scoring) |
| **Release Confidence** | Weighted readiness scoring, policy-based quality gates, sign-off chain (QA Lead → EM → PO) |
| **Cross-Team QA** | Service catalog, 8-state request lifecycle, SLA breach alerts, capacity planning |
| **CI/CD Integrations** | Webhook ingestion with HMAC validation, GitLab pipeline import, JUnit/TestNG/Cypress parsers |
| **Documentation** | Asset health scoring, review workflows, staleness detection |
| **Test Data** | Synthetic generation with Datafaker — random, boundary, edge case, localization strategies |
| **Security** | JWT auth (access + refresh), 5 roles, 50 permissions, audit trail, SSE notifications |

## Tech Stack

| Layer | Technologies |
|-------|-------------|
| **Frontend** | React 19 · TypeScript · Tailwind CSS v4 · Vite · React Query · Recharts |
| **Backend** | Java 21 · Spring Boot 3.2 · MapStruct · Lombok · Flyway |
| **Database** | PostgreSQL 17 · JSONB · Full-text search (GIN) · Partial unique indexes |
| **AI** | Anthropic Claude API · Async analysis · Human feedback loop |
| **Testing** | JUnit 5 · Mockito (535 tests) · Playwright (52 E2E tests) |
| **DevOps** | Docker · GitHub Actions · Multi-stage builds |

## Getting Started

### Docker (recommended)

```bash
git clone https://github.com/saadfikri/OpenQualityHub.git
cd OpenQualityHub
docker-compose up -d
```

### Manual

```bash
# 1. Create the database
createdb openqualityhub

# 2. Start the backend
cd backend
mvn spring-boot:run

# 3. Start the frontend
cd frontend
npm install && npm run dev
```

The app runs at **http://localhost:5173** — API at **http://localhost:8080**.

### Demo Account

| Email | Password | Role |
|-------|----------|------|
| `sarah.chen@demo.oqh` | `Demo123!` | Admin |

## Testing

```bash
# Backend (535 unit tests)
cd backend && mvn test

# E2E (52 Playwright tests)
cd frontend && npx playwright test
```

## Roadmap

- [ ] GitHub Actions integration (parser + webhook processor)
- [ ] Jenkins pipeline integration
- [ ] Email notifications (SMTP)
- [ ] LDAP/SSO authentication
- [ ] Test case management module
- [ ] Performance test results tracking
- [ ] Multi-language support (i18n)
- [ ] Grafana/Prometheus metrics export
- [ ] Public API with API key authentication

## Contributing

We welcome contributions — see our [Contributing Guide](CONTRIBUTING.md) for setup instructions, code style, and PR process.

## License

[GNU Affero General Public License v3.0](LICENSE)

---

<div align="center">

**Built for the people behind software quality.**

[Report a Bug](https://github.com/saadfikri/OpenQualityHub/issues) · [Request a Feature](https://github.com/saadfikri/OpenQualityHub/issues)

</div>
