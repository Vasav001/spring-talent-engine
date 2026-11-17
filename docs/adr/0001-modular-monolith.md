# ADR 0001: Modular monolith with Spring Modulith

**Status:** accepted

## Context
The app has clear business areas (tenants, jobs, billing, ingestion, screening) but one developer, one database and a free-tier budget. It has to be explainable end to end and cheap to run on a single small machine.

## Decision
One Spring Boot application split into Spring Modulith modules, one package each. Modules talk through public types in the module's base package (for example `JobRubrics`, `ResumeProcessing`, `Credits`) or through events (`TenantCreated`, `BatchSubmitted`). `ApplicationModules.verify()` runs as a unit test and fails the build on cycles or on access to another module's internal classes. Spring Modulith's event publication registry gives the batch pipeline a transactional outbox.

## Alternatives considered
- **Microservices:** separate deploys, network calls and distributed transactions for no real benefit at this size; would not fit on one `t4g.small`.
- **Plain layered monolith:** simplest, but nothing stops modules from reaching into each other, so boundaries erode.

## Consequences
- One deploy, one database, local method calls, simple transactions.
- Boundaries are enforced by a test, not by convention; the module diagram is generated from the code (`target/spring-modulith-docs`).
- A module could later become its own service along an existing boundary, but nothing forces that now.
- Two empty modules from the first plan (`usage`, `review`) were removed when their features fitted naturally into `screening`, because a separate module would have created a dependency cycle around the ranking query.
