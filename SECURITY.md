# Security Policy

## Supported Versions

| Version | Supported          |
|---------|--------------------|
| 0.1.x   | Yes                |

## Reporting a Vulnerability

We take security seriously. If you discover a security vulnerability in Open Quality Hub, please report it responsibly.

**Do NOT open a public issue for security vulnerabilities.**

### How to Report

1. Email **security@openqualityhub.org** with a detailed description of the vulnerability
2. Include steps to reproduce the issue
3. Provide any relevant logs, screenshots, or proof-of-concept code

### What to Expect

- **Acknowledgment** within 48 hours of your report
- **Assessment** within 5 business days with an initial severity classification
- **Fix timeline** communicated based on severity:
  - Critical: patch within 72 hours
  - High: patch within 7 days
  - Medium/Low: included in next scheduled release
- **Credit** in the release notes (unless you prefer to remain anonymous)

### Scope

The following are in scope for security reports:

- Authentication and authorization bypasses
- SQL injection, XSS, CSRF vulnerabilities
- Sensitive data exposure
- HMAC/webhook signature validation bypasses
- JWT token manipulation or replay attacks
- Rate limiting bypasses
- Server-side request forgery (SSRF)

### Out of Scope

- Vulnerabilities in dependencies (report these upstream; we monitor via Dependabot)
- Denial of service attacks requiring excessive resources
- Social engineering attacks
- Issues in the demo/seed data

## Security Architecture

Open Quality Hub implements defense-in-depth:

- **Authentication**: JWT with access/refresh token rotation
- **Authorization**: Role-Based Access Control with 5 roles and 37 granular permissions
- **Password storage**: BCrypt hashing
- **API security**: Rate limiting on auth endpoints, security response headers
- **Webhook validation**: HMAC-SHA256 signature verification with constant-time comparison
- **Database**: Parameterized queries via JPA (no raw SQL), Flyway-managed migrations
- **Containers**: Non-root users, minimal base images, no secrets in images

## Known Limitations

### Token Storage (v0.1)

JWT tokens are currently stored in `localStorage` on the frontend. This is a deliberate trade-off for v0.1:

- **Risk**: If an XSS vulnerability exists, an attacker could extract tokens from localStorage.
- **Mitigation**: React's built-in XSS prevention (no `dangerouslySetInnerHTML` usage), strict Content Security Policy via nginx headers, and no user-generated HTML rendering.
- **Planned fix (v0.2)**: Migrate to `httpOnly` cookie-based token storage with CSRF protection, eliminating client-side token exposure entirely.

For production deployments handling sensitive data, we recommend deploying behind a reverse proxy that sets `httpOnly` cookies.
