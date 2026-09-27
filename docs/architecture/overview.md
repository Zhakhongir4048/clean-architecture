# Architecture Overview

Package root: `org.esonov.clean_architecture` · Rationale and trade-offs: [`src/ARCHITECTURE.md`](../../src/ARCHITECTURE.md)

> Diagrams and metrics below are drawn for the `user` feature and the `shared` kernel. The `order` feature has the
> same shape (see its flows in [`use-cases.md`](use-cases.md) and the cross-feature boundary in
> [`boundaries.md`](boundaries.md)).

## 1. The Clean Architecture circle

```
┌────────────────────────────────────────────────────────────────────────────┐
│ FRAMEWORKS & DRIVERS                                                       │
│   Spring MVC + Jackson (DispatcherServlet)                                 │
│   Spring Data JPA + Hibernate                                              │
│   Flyway: db/migration/V1__create_user_account.sql                         │
│   PostgreSQL                                                               │
│                                                                            │
│ ┌────────────────────────────────────────────────────────────────────────┐ │
│ │ INTERFACE ADAPTERS                                                     │ │
│ │   Controllers: UserAccountController                                   │ │
│ │   Presenters:  UserAccountPresenter, UserExceptionHandler              │ │
│ │                GlobalExceptionHandler (shared)                         │ │
│ │   Gateways:    UserAccountPersistenceAdapter                           │ │
│ │                (UserAccountJpaEntity, SpringDataUserAccountRepository) │ │
│ │                                                                        │ │
│ │ ┌────────────────────────────────────────────────────────────────────┐ │ │
│ │ │ APPLICATION BUSINESS RULES                                         │ │ │
│ │ │   Use cases:    RegisterUserInteractor                             │ │ │
│ │ │                 GetUserAccountInteractor                           │ │ │
│ │ │   Input ports:  RegisterUserUseCase, GetUserAccountQuery           │ │ │
│ │ │   Models:       RegisterUserCommand, UserAccountView               │ │ │
│ │ │   Output ports: UserAccountRepository                              │ │ │
│ │ │   Errors:       EmailAlreadyRegistered, UserAccountNotFound        │ │ │
│ │ │                                                                    │ │ │
│ │ │ ┌────────────────────────────────────────────────────────────────┐ │ │ │
│ │ │ │ ENTERPRISE BUSINESS RULES                                      │ │ │ │
│ │ │ │   Entities:  UserAccount                                       │ │ │ │
│ │ │ │   Value obj: UserId, EmailAddress                              │ │ │ │
│ │ │ │   Errors:    DomainValidationException (shared)                │ │ │ │
│ │ │ │                                                                │ │ │ │
│ │ │ └────────────────────────────────────────────────────────────────┘ │ │ │
│ │ │                                                                    │ │ │
│ │ └────────────────────────────────────────────────────────────────────┘ │ │
│ │                                                                        │ │
│ └────────────────────────────────────────────────────────────────────────┘ │
│                                                                            │
└────────────────────────────────────────────────────────────────────────────┘
                 All source-code dependencies point INWARD.
       Enforced by CleanArchitectureBoundaryTest (ArchUnit, 12 rules).

MAIN (outside every circle, wires them together):
  CleanArchitectureApplication  — Spring Boot entry point
  config.ClockConfig            — shared infrastructure (Clock)
  user.config.UserConfig        — builds the user interactors as input-port beans
```

Packages are feature-first (Screaming Architecture): the rings live **inside** each feature.

| Ring | Package(s) |
|---|---|
| Enterprise business rules | `user.domain` · shared kernel: `shared.domain` |
| Application business rules | `user.application.port.in`, `.port.out`, `.usecase`, `.exception` |
| Interface adapters | `user.adapter.in.web`, `user.adapter.out.persistence` · shared: `shared.adapter.in.web` |
| Frameworks & drivers | no own code — Spring, Hibernate, Flyway, Postgres (+ `resources/db/migration`, `application.yaml`) |
| Main | root package, `config`, `user.config` |

## 2. Component dependency graph

Arrows = source-code dependency (`A ──► B` means A imports B). Short names are inside `user`
unless prefixed with `shared.`.

```
 VOLATILE   I = 1.00   [user.config]   [adapter.in.web]   [adapter.out.persistence]   [shared.adapter.in.web]
                           │                  │                    │                         │
                           ▼                  │                    │                         │
            I = 0.83   [usecase]              │                    │                         │
                           │                  ▼                    ▼                         │
            I = 0.25   [port.in] ◄────────────┘      [port.out] ◄──┘   I = 0.40              │
                           │                             │                                   │
                           ▼                             ▼                                   ▼
 STABLE     I = 0.00   [exception]                [domain] ─────────────────────────► [shared.domain]
                                                  I = 0.25                             I = 0.00

 Every arrow points DOWN — toward stability. Simplified: see the edge list for every target.
```

Exact edge list:

| From | To |
|---|---|
| `user.config` | `usecase`, `port.in`, `port.out` |
| `user.adapter.in.web` | `port.in`, `exception` |
| `user.adapter.out.persistence` | `port.out`, `exception`, `user.domain` |
| `usecase` | `port.in`, `port.out`, `exception`, `user.domain`, `shared.domain` |
| `port.in` | `exception` |
| `port.out` | `exception`, `user.domain` |
| `user.domain` | `shared.domain` |
| `shared.adapter.in.web` | `shared.domain` |
| `shared.domain`, `exception`, `config` | — |

No cycles (ADP ✓). Every edge goes from higher to lower instability (SDP ✓).
`user` depends only on `shared` and root `config` — no feature depends on another.

## 3. Stability and abstractness

`I = Ce / (Ca + Ce)` (0 = stable, 1 = volatile) · `A = abstract types / all types` · `D = |A + I − 1|`

```
Instability (I)                                   Abstractness (A)
  shared.domain                 0.00 ░░░░░░░░░░     0.00 ░░░░░░░░░░
  user.application.exception    0.00 ░░░░░░░░░░     0.00 ░░░░░░░░░░
  user.domain                   0.25 ██░░░░░░░░     0.00 ░░░░░░░░░░
  user.application.port.in      0.25 ██░░░░░░░░     0.50 █████░░░░░
  user.application.port.out     0.40 ████░░░░░░     1.00 ██████████
  user.application.usecase      0.83 ████████░░     0.00 ░░░░░░░░░░
  user.adapter.out.persistence  1.00 ██████████     0.33 ███░░░░░░░
  user.adapter.in.web           1.00 ██████████     0.00 ░░░░░░░░░░
  user.config                   1.00 ██████████     0.00 ░░░░░░░░░░
  shared.adapter.in.web         1.00 ██████████     0.00 ░░░░░░░░░░
```

| Component | Ca | Ce | I | A | D | Zone |
|---|---|---|---|---|---|---|
| `shared.domain` | 3 | 0 | 0.00 | 0.00 | 1.00 | Pain — acceptable (non-volatile shared kernel) |
| `user.domain` | 3 | 1 | 0.25 | 0.00 | 0.75 | Pain — acceptable (non-volatile) |
| `user.application.exception` | 5 | 0 | 0.00 | 0.00 | 1.00 | Pain — acceptable now that it is feature-local |
| `user.application.port.in` | 3 | 1 | 0.25 | 0.50 | 0.25 | Main sequence ✓ |
| `user.application.port.out` | 3 | 2 | 0.40 | 1.00 | 0.40 | ✓ |
| `user.application.usecase` | 1 | 5 | 0.83 | 0.00 | 0.17 | ✓ |
| `user.adapter.in.web` | 0 | 2 | 1.00 | 0.00 | 0.00 | ✓ |
| `user.adapter.out.persistence` | 0 | 3 | 1.00 | 0.33 | 0.33 | ✓ |
| `user.config` | 0 | 3 | 1.00 | 0.00 | 0.00 | ✓ |
| `shared.adapter.in.web` | 0 | 1 | 1.00 | 0.00 | 0.00 | ✓ |
| `config` | 0 | 0 | — | 0.00 | — | isolated (reached only through Spring) |

Average D = 0.39.

```
A
1.0 |.                   O
0.9 |     .
0.8 |          .
0.7 |               .
0.6 |                    .
0.5 |               I         .
0.4 |                              .
0.3 |                                   .              P
0.2 |                                        .
0.1 |                                             .
0.0 |SX             U                        C         WMG
    +----+----+----+----+----+----+----+----+----+----+-- I
    0.0       0.2       0.4       0.6       0.8       1.0

 . main sequence (A + I = 1)
 S shared.domain   X exception   U user.domain   I port.in   O port.out   C usecase
 W web             P persistence M user.config   G shared.adapter.in.web
```

Mermaid versions of every diagram: [`diagrams.md`](diagrams.md) (sources: [`mermaid/`](mermaid/)).
