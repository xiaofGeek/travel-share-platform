# AGENTS.md

## Project scope

This repository contains three independent applications:

- `backend`: Spring Boot 3 / Java 21 REST API.
- `user-web`: Vue 3 customer-facing travel content site.
- `admin-web`: Vue 3 administration and moderation console.

Keep all work local-first. Do not introduce Docker, Redis, Elasticsearch, microservices, cloud databases, paid maps, or external image hotlinks.

## Required checks

- Backend: `backend\mvnw.cmd clean test` and `backend\mvnw.cmd clean package`.
- User site: run `npm.cmd run build` in `user-web`.
- Admin site: run `npm.cmd run build` in `admin-web`.
- Use `scripts\resolve-tools.ps1` or the wrapper scripts; do not change global PATH.

## Conventions

- Public APIs use `/api/public/**`, authenticated user APIs use `/api/user/**`, and administration APIs use `/api/admin/**`.
- Store image references as `/uploads/...` relative URLs.
- Never commit real credentials or generated runtime logs.
- Preserve independent `package.json` and `package-lock.json` files for the two Vue applications.

