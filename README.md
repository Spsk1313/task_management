# Task Management API

A backend-focused task management service built with **Java 21, Spring Boot 4.1, PostgreSQL, Flyway, JPA/Hibernate, Testcontainers, and OpenAPI**.

This project was built as a production-oriented learning exercise: not a basic todo app, but a structured REST API with domain rules, relational modeling, validation, filtering, pagination, exception handling, database migrations, automated tests, and interactive API documentation.

## Highlights

- Users own projects.
- Projects own tasks.
- Tasks support priority, status, due dates, completion timestamps, and reusable tags.
- Task status changes are controlled by explicit domain transition rules.
- Tasks can be filtered by status and priority, searched by title, paginated, and sorted.
- Tags are normalized and reused globally rather than duplicated per task.
- Database schema changes are versioned with Flyway.
- PostgreSQL runs locally through Docker Compose.
- Integration tests run against real PostgreSQL instances with Testcontainers.
- OpenAPI documentation is generated with springdoc and exposed through Swagger UI.

## Tech Stack

| Area | Technology |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 4.1 |
| Web | Spring Web MVC |
| Persistence | Spring Data JPA / Hibernate |
| Database | PostgreSQL 17 |
| Migrations | Flyway |
| Validation | Jakarta Bean Validation |
| API documentation | springdoc-openapi / Swagger UI |
| Unit testing | JUnit 5, Mockito |
| HTTP/controller testing | MockMvc |
| Integration testing | Testcontainers + PostgreSQL |
| Local infrastructure | Docker Compose |
| Build | Maven Wrapper |

## Domain Model

```text
User
 └── Project
      └── Task
           └── Tags (many-to-many)
```

### User

Represents the owner of one or more projects.

### Project

A project belongs to one user. Project names are unique per owner, and deleting a user cascades to that user's projects.

### Task

Each task belongs to exactly one project and contains:

- title
- description
- priority
- status
- due date
- created / updated timestamps
- completion timestamp
- tags

Supported priorities:

```text
HIGH
MEDIUM
LOW
```

Supported statuses:

```text
TODO
IN_PROGRESS
BLOCKED
DONE
```

New tasks always begin in `TODO`.

Status transitions are enforced inside the domain entity rather than scattered through controllers or repositories. For example, invalid transitions such as `BLOCKED -> DONE` are rejected with a conflict response, while moving into `DONE` automatically sets `completedAt` and reopening a completed task clears it.

### Tags

Tags are globally reusable. Input is normalized by trimming whitespace and converting names to lowercase.

For example:

```text
" JAVA "
"Java"
"java"
```

all resolve to the same canonical `java` tag.

The `task_tags` junction table uses `(task_id, tag_id)` as its composite primary key, preventing duplicate associations at the database level.

## API Overview

### Users

| Method | Endpoint | Purpose |
| --- | --- | --- |
| GET | `/api/users/{id}` | Get a user |
| POST | `/api/users` | Create a user |
| PUT | `/api/users/{id}` | Update a user |

### Projects

| Method | Endpoint | Purpose |
| --- | --- | --- |
| GET | `/api/projects` | List all projects |
| GET | `/api/projects?ownerId={ownerId}` | List projects owned by a user |
| GET | `/api/projects/{id}` | Get a project |
| POST | `/api/projects` | Create a project |
| PUT | `/api/projects/{id}?ownerId={ownerId}` | Update a project as its owner |
| DELETE | `/api/projects/{id}?ownerId={ownerId}` | Delete a project as its owner |

### Tasks

| Method | Endpoint | Purpose |
| --- | --- | --- |
| POST | `/api/projects/{projectId}/tasks` | Create a task inside a project |
| GET | `/api/projects/{projectId}/tasks` | Query project tasks |
| PATCH | `/api/tasks/{taskId}` | Partially update a task |
| DELETE | `/api/tasks/{taskId}` | Delete a task |

The task query endpoint supports optional filters and Spring Data pagination/sorting parameters:

```http
GET /api/projects/1/tasks?status=TODO&priority=HIGH&search=spring&page=0&size=20&sort=dueDate,asc
```

Supported query behavior includes:

- filter by status
- filter by priority
- case-insensitive title search
- pagination
- one or multiple sort fields

### Tags

| Method | Endpoint | Purpose |
| --- | --- | --- |
| POST | `/api/tasks/{taskId}/tags` | Attach a reusable tag to a task |

Example:

```json
{
  "name": "java"
}
```

Attempting to attach the same normalized tag twice to one task returns `409 Conflict`.

## Example Task Request

```http
POST /api/projects/1/tasks
Content-Type: application/json
```

```json
{
  "title": "Implement task filtering",
  "description": "Add dynamic JPA specifications",
  "priority": "HIGH",
  "dueDate": "2026-08-20"
}
```

New tasks are created with `TODO` status automatically.

## Dynamic Querying

Task filtering is implemented with `JpaSpecificationExecutor<Task>` and composable JPA Specifications instead of creating repository methods for every possible filter combination.

Conceptually:

```text
belongsToProject(projectId)
AND hasStatus(status)
AND hasPriority(priority)
AND searchByTitle(search)
```

The resulting `Specification<Task>` is combined with `Pageable`, allowing filtering, searching, pagination, and sorting through one query path.

This avoids a combinatorial explosion of methods such as `findByProjectAndStatusAndPriorityAndTitle...`.

## Database & Migrations

The schema is managed entirely through Flyway migrations:

```text
V1__create_users_table.sql
V2__create_projects_table.sql
V3__create_tasks_table.sql
V4__create_tags_table.sql
```

Hibernate is configured with:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

Hibernate therefore validates entity mappings against the migrated schema instead of creating or silently modifying production tables.

PostgreSQL-specific enum types are used for task priority and status and mapped through Hibernate.

## Local Setup

### Requirements

Install:

- Java 21
- Docker with Docker Compose support

Maven does not need to be installed globally because the repository includes the Maven Wrapper.

### 1. Start PostgreSQL

```bash
docker compose up -d
```

The included Compose configuration starts:

```text
PostgreSQL 17
Database: task_management
User: admin
Password: admin
Host port: 5434
```

The application is configured to connect to:

```text
jdbc:postgresql://localhost:5434/task_management
```

### 2. Start the API

```bash
./mvnw spring-boot:run
```

Flyway automatically applies pending migrations during startup.

### 3. Stop PostgreSQL

```bash
docker compose down
```

To also remove the persistent database volume:

```bash
docker compose down -v
```

## OpenAPI / Swagger

Once the application is running, springdoc generates the OpenAPI contract directly from the Spring controllers and DTOs.

Swagger UI is available at the standard springdoc endpoint:

```text
http://localhost:8080/swagger-ui/index.html
```

The generated OpenAPI JSON is available at:

```text
http://localhost:8080/v3/api-docs
```

The API documentation groups endpoints into Users, Projects, Tasks, and Tags and documents non-obvious behavior such as task querying, task status transitions, and reusable tag associations.

## Testing

The project currently has **130 automated tests** spanning multiple testing levels.

Run the complete suite with:

```bash
./mvnw test
```

Or run the full Maven verification lifecycle with:

```bash
./mvnw clean verify
```

### Unit tests

Service and domain tests use JUnit 5 and Mockito to verify business behavior in isolation, including:

- successful and failed entity creation
- missing resources
- partial task updates
- task status transitions
- completion timestamp behavior
- tag normalization and reuse
- duplicate tag prevention
- service/repository interactions

### Controller tests

MockMvc controller tests verify the HTTP boundary, including:

- request JSON -> DTO binding
- Bean Validation
- enum conversion
- path and query parameter binding
- HTTP status codes
- pagination and sorting parameter binding
- centralized exception translation
- response serialization

### Integration tests

Integration tests use **Testcontainers PostgreSQL**, not mocked repositories.

They exercise the complete path:

```text
Controller
  -> Service
  -> Repository
  -> Hibernate
  -> PostgreSQL
```

Integration coverage includes real persistence behavior, Flyway-managed schemas, filtering, search, pagination, sorting, enum persistence, task state changes, tag relationships, tag reuse, and database constraints.

## Error Handling

The application uses centralized exception handling through `@RestControllerAdvice`.

Examples include:

| Situation | HTTP status |
| --- | --- |
| Bean Validation failure | `400 Bad Request` |
| Missing user/project/task | `404 Not Found` |
| Duplicate email/project/tag association | `409 Conflict` |
| Invalid task state transition | `409 Conflict` |
| Forbidden project modification | `403 Forbidden` |

Validation errors return structured field-level error information rather than raw framework exceptions.

## Engineering Decisions

A few deliberate design choices in this project:

### Flyway owns the schema

Schema evolution is explicit, versioned, and reviewable. Hibernate validates rather than generates tables.

### Entities protect domain invariants

Behavior such as task status transitions and `completedAt` synchronization lives in the domain entity instead of being duplicated across controllers and services.

### DTOs isolate the API from persistence entities

Controllers expose dedicated request/response records rather than directly serializing JPA entities.

### Lazy relationships by default

JPA relationships such as task tags use lazy loading. Transaction boundaries and persistence-context behavior are treated deliberately rather than switching relationships to `EAGER` simply to avoid lazy-loading errors.

### Database integrity is not delegated entirely to Java

Foreign keys, unique constraints, composite keys, `NOT NULL` constraints, PostgreSQL enums, and cascade behavior provide another layer of correctness beneath application validation.

### Query composition over repository-method explosion

JPA Specifications provide reusable predicates for dynamic task querying and work cleanly with `Pageable`.

## What I Learned Building This

This project was built to develop practical backend engineering skills rather than maximize feature count. Key learnings included:

- structuring a Spring Boot application into controller, service, repository, entity, DTO, exception, and configuration layers
- deciding which rules belong in the API boundary, service layer, domain entity, or database
- modeling one-to-many and many-to-many relationships in PostgreSQL and JPA
- designing Flyway migrations incrementally as the domain evolves
- mapping PostgreSQL enum types to Java enums
- understanding managed vs detached entities, dirty checking, transaction boundaries, persistence contexts, and lazy loading
- implementing domain state machines rather than treating entities as mutable data bags
- building composable dynamic queries with JPA Specifications
- using `Pageable` for pagination and multi-column sorting
- designing PATCH-style partial updates
- normalizing and reusing shared entities such as tags
- distinguishing unit, controller, and integration testing responsibilities
- testing against real PostgreSQL with Testcontainers
- using OpenAPI as an executable API contract rather than maintaining separate handwritten endpoint documentation
- using feature branches, pull requests, and squash merges to keep Git history clean

## Project Scope

This repository intentionally stops at the point where the core learning objectives were achieved. Features such as authentication, messaging, caching, observability, application containerization, and distributed-system concerns are intentionally left for later projects where they can be introduced as primary learning objectives rather than added as scope creep here.

---

Built as a focused backend engineering project for learning production-oriented Spring Boot development.