# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2025-11-28

### Added

- **Maestro Mobile Testing** — Device compatibility matrix, flow analytics, top failing flows, platform summary, dual-format JSON parser (CLI + Cloud), webhook integration, frontend dashboard with import modal and paginated flow results
- **AI Failure Analysis** — Claude-powered root cause categorization with 9 failure categories, confidence scoring, human feedback loop, and graceful degradation
- **WebdriverIO Import** — Full WebdriverIO JSON parser with suite/test/hook support
- **Streaming CSV Exports** — Paginated streaming for test runs and defects via StreamingResponseBody
- **Release Quality Gates** — Policy-based quality gates with configurable conditions and 40/30/30 weighted readiness scoring
- **Test Data Generation** — Synthetic data engine with Datafaker — 11 field types, 6 strategies (random, boundary, edge case, localization)

### Changed

- **Performance** — Caffeine caching (dashboard 5min, 500 max entries), lazy fetching with `default_batch_fetch_size: 20`, aggregated GROUP BY queries, 10 performance indexes, HikariCP tuning
- **Security** — Hardened JWT auth, expanded to 39+ permissions, strengthened RBAC enforcement, rate limiting with Caffeine
- **Database** — 42 entities, 6 Flyway migrations (V1-V6), cross-layer enum CHECK constraints, CASCADE deletes on Maestro FK references
- **Testing** — 563 unit tests (up from 507), 52 E2E Playwright tests (up from 10)
- **Frontend** — Maestro pages (dashboard + flows), error state handling, React Fragment key fixes

### Fixed

- 401 redirect loop in frontend API interceptor
- 4 failing E2E tests (headings + active class)
- Critical security findings in enterprise auth, RBAC, and AI analysis pipeline
- N+1 query prevention with `@EntityGraph` and batch fetch size
- Database CHECK constraints blocking non-JUnit/Playwright imports

## [0.1.0] - 2025-11-12

### Added

#### Quality Dashboard & Test Intelligence
- JUnit XML, Playwright JSON, Cypress JSON (mochawesome), and TestNG XML test result import
- Pass rate trends, failure analysis, and duration tracking
- Flaky test detection with stable fingerprinting and history analysis
- Quality scores per project with trend visualization
- Defect tracking with severity, priority, and lifecycle management
- Trend analytics (pass rate, defects, test volume, flaky rate over time)

#### Cross-Team QA Service Management
- QA service catalog with SLA definitions
- 8-state request lifecycle (SUBMITTED through CLOSED)
- Priority-based routing with ITIL impact/urgency matrix
- Capacity planning with team utilization tracking
- SLA breach monitoring and escalation

#### Documentation Intelligence
- Documentation asset tracking across projects
- Automated health checks (staleness detection, review cycles)
- Review workflow with approval chain
- Health status dashboard

#### Release Confidence
- Readiness reports with weighted scoring (pass rate, build health, defects)
- Quality gate evaluation with configurable conditions
- Multi-stakeholder sign-off chain (QA, Dev Lead, Product Owner)
- Release dashboard with confidence gauges

#### External Integrations
- Webhook-based integration with GitHub, GitLab, Jenkins, Jira
- HMAC-SHA256 signature validation (GitHub/Jenkins) and token validation (GitLab)
- GitLab CI test report auto-fetch via GitLab API
- Strategy-pattern event processors (7 specialized processors)
- Real-time event log and integration dashboard

#### Operations & Observability
- User management with role assignment, activate/deactivate
- Real-time notifications via Server-Sent Events (SSE)
- AOP-based audit trail with filterable log
- Full-text global search with PostgreSQL GIN indexes
- CSV export for test runs and defects
- Notification bell with unread badge and 30s polling

#### Platform
- JWT authentication with access/refresh token rotation
- Role-Based Access Control (5 roles, 39+ permissions)
- Rate limiting on authentication endpoints
- Security response headers
- PostgreSQL 17 with Flyway migrations
- Docker Compose deployment with multi-stage builds
- GitHub Actions CI/CD pipeline with JaCoCo coverage
- Externalized configuration via environment variables
- Swagger UI / OpenAPI documentation
- React 19 frontend with TypeScript, Tailwind CSS v4, Recharts

[1.0.0]: https://github.com/saadfikri/OpenQualityHub/compare/v0.1.0...v1.0.0
[0.1.0]: https://github.com/saadfikri/OpenQualityHub/releases/tag/v0.1.0
