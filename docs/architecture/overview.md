# Architecture Overview

Package root: `org.esonov.clean_architecture` · Rationale and trade-offs: [`src/ARCHITECTURE.md`](../../src/ARCHITECTURE.md)

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
│ │   Presenters:  UserAccountPresenter, ApiExceptionHandler               │ │
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
│ │ │ │   Errors:    DomainValidationException                         │ │ │ │
│ │ │ │                                                                │ │ │ │
│ │ │ └────────────────────────────────────────────────────────────────┘ │ │ │
│ │ │                                                                    │ │ │
│ │ └────────────────────────────────────────────────────────────────────┘ │ │
│ │                                                                        │ │
│ └────────────────────────────────────────────────────────────────────────┘ │
│                                                                            │
└────────────────────────────────────────────────────────────────────────────┘
                 All source-code dependencies point INWARD.
       Enforced by CleanArchitectureBoundaryTest (ArchUnit, 10 rules).

MAIN (outside every circle, wires them together):
  CleanArchitectureApplication  — Spring Boot entry point
  config.UseCaseConfig          — builds interactors as input-port beans, provides Clock
```

| Ring | Package(s) |
|---|---|
| Enterprise business rules | `domain`, `domain.user` |
| Application business rules | `application.port.in`, `application.port.out`, `application.usecase`, `application.exception` |
| Interface adapters | `adapter.in.web`, `adapter.out.persistence` |
| Frameworks & drivers | no own code — Spring, Hibernate, Flyway, Postgres (+ `resources/db/migration`, `application.yaml`) |
| Main | root package, `config` |

## 2. Component dependency graph

Arrows = source-code dependency (`A ──► B` means A imports B).

```
 VOLATILE   I = 1.00   [config]    [adapter.in.web]    [adapter.out.persistence]
                           │               │                      │
                           ▼               │                      │
            I = 0.80   [usecase]           │                      │
                           │               │                      │
                           ▼               ▼                      ▼
            I = 0.25   [port.in] ◄─────────┘        [port.out] ◄──┘   I = 0.40
                           │                             │
                           ▼                             ▼
 STABLE     I = 0.00   [exception]              [domain.user] ──► [domain]   I = 0.25 / 0.00

 Every arrow points DOWN — toward stability. Simplified: see the edge list for every target.
```

Exact edge list:

| From | To |
|---|---|
| `config` | `usecase`, `port.in`, `port.out` |
| `adapter.in.web` | `port.in`, `exception`, `domain` |
| `adapter.out.persistence` | `port.out`, `exception`, `domain.user` |
| `usecase` | `port.in`, `port.out`, `exception`, `domain.user` |
| `port.in` | `exception` |
| `port.out` | `exception`, `domain.user` |
| `domain.user` | `domain` |
| `domain`, `exception` | — |

No cycles (ADP ✓). Every edge goes from higher to lower instability (SDP ✓).

## 3. Stability and abstractness

`I = Ce / (Ca + Ce)` (0 = stable, 1 = volatile) · `A = abstract types / all types` · `D = |A + I − 1|`

```
Instability (I)                              Abstractness (A)
  domain                   0.00 ░░░░░░░░░░     0.00 ░░░░░░░░░░
  application.exception    0.00 ░░░░░░░░░░     0.00 ░░░░░░░░░░
  domain.user              0.25 ██░░░░░░░░     0.00 ░░░░░░░░░░
  port.in                  0.25 ██░░░░░░░░     0.50 █████░░░░░
  port.out                 0.40 ████░░░░░░     1.00 ██████████
  usecase                  0.80 ████████░░     0.00 ░░░░░░░░░░
  adapter.out.persistence  1.00 ██████████     0.33 ███░░░░░░░
  adapter.in.web           1.00 ██████████     0.00 ░░░░░░░░░░
  config                   1.00 ██████████     0.00 ░░░░░░░░░░
```

| Component | Ca | Ce | I | A | D | Zone |
|---|---|---|---|---|---|---|
| `domain` | 2 | 0 | 0.00 | 0.00 | 1.00 | Pain — acceptable (non-volatile) |
| `domain.user` | 3 | 1 | 0.25 | 0.00 | 0.75 | Pain — acceptable (non-volatile) |
| `application.exception` | 5 | 0 | 0.00 | 0.00 | 1.00 | Pain — watch (shared hub) |
| `port.in` | 3 | 1 | 0.25 | 0.50 | 0.25 | Main sequence ✓ |
| `port.out` | 3 | 2 | 0.40 | 1.00 | 0.40 | ✓ |
| `usecase` | 1 | 4 | 0.80 | 0.00 | 0.20 | ✓ |
| `adapter.in.web` | 0 | 3 | 1.00 | 0.00 | 0.00 | ✓ |
| `adapter.out.persistence` | 0 | 3 | 1.00 | 0.33 | 0.33 | ✓ |
| `config` | 0 | 3 | 1.00 | 0.00 | 0.00 | ✓ |

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
0.0 |DX             U                        C         WM
    +----+----+----+----+----+----+----+----+----+----+-- I
    0.0       0.2       0.4       0.6       0.8       1.0

 . main sequence (A + I = 1)
 D domain   X exception   U domain.user   I port.in   O port.out
 C usecase  W web         P persistence   M config
```

Mermaid versions of every diagram: [`diagrams.md`](diagrams.md) (sources: [`mermaid/`](mermaid/)).
