# Architecture

Package root: `org.esonov.clean_architecture`

```
            ┌──────────────────────── config/ + CleanArchitectureApplication (Main) ───────────────────────┐
            │  wires interactors as input-port beans; the only code that sees both sides of every boundary │
            └──────────────────────────────────────────────────────────────────────────────────────────────┘
                        │                                                          │
   adapter/in/web  ─────┤  HTTP ⇄ RegisterUserCommand / UserAccountView           │  adapter/out/persistence
   (Spring MVC)         ▼                                                         ▼  (JPA, Flyway, Postgres)
           ┌─────────── application/port/in ──────┐         ┌──── application/port/out ────┐
           │ RegisterUserUseCase                   │        │ UserAccountRepository        │◄── implemented by
           │ GetUserAccountQuery                   │        └──────────────────────────────┘    UserAccountPersistenceAdapter
           │ RegisterUserCommand, UserAccountView  │                       ▲
           └──────────────────▲────────────────────┘                       │ uses
                              │ implements                                 │
                    application/usecase: RegisterUserInteractor, GetUserAccountInteractor
                              │ uses
                    domain: UserAccount, UserId, EmailAddress, DomainValidationException
```

Source-code dependencies point **inward only**: `config → adapter → application → domain`.

---

## Boundary: Use Cases ↔ Web and Database

**Separates**: `domain` + `application` (stable policy) from `adapter.in.web` and
`adapter.out.persistence` (volatile detail).

**Type**: Full boundary, with DIP on both sides. It is deployed as a single unit, and the
compiler does not enforce it (single Maven module). ArchUnit enforces it at test time.

**Interfaces defined in inner layer**:
- `application/port/in/RegisterUserUseCase`, `GetUserAccountQuery`: called by outer → inner.
  They carry plain records only (`RegisterUserCommand` in, `UserAccountView` out).
- `application/port/out/UserAccountRepository`: implemented by outer, called by inner.

**Data crossing the boundary**:
- Input side: records only. The `UserAccount` entity never reaches the web adapter.
- Output side: the repository port takes and returns the `UserAccount` entity. This is allowed,
  because the persistence adapter is the outer layer depending *inward* on the domain. The
  persistence adapter maps it to `UserAccountJpaEntity`, which never crosses back in.
- Errors: inner layers throw their own exceptions (`DomainValidationException`,
  `EmailAlreadyRegisteredException`, `UserAccountNotFoundException`). Adapters translate
  framework errors inward (e.g. unique-constraint violation → `EmailAlreadyRegisteredException`),
  and translate these exceptions outward to HTTP (`ApiExceptionHandler`).

**Reason for this boundary**:
- The HTTP API shape, JPA mappings and DB schema change far more often than the rules for
  registering a user.
- The use cases can be tested with an in-memory fake (`UserAccountUseCasesTest`), with no Spring and no DB.
- Postgres/JPA or Spring MVC can be swapped without editing `domain` or `application`.

**Enforced by** `src/test/java/.../architecture/CleanArchitectureBoundaryTest.java`:
- Layered dependency rule (Domain ← Application ← Adapters ← Main).
- `domain` and `application` must not depend on Spring, JPA, Hibernate, Jackson, or servlet types.
- The web adapter may use only input ports: no interactors, no output ports, no entities.
- The persistence adapter must not use the input side.
- Ports are interfaces or records only.
- Adapters are package-private.
- No package cycles.

**Known trade-offs**:
- Interactors *return* a response model rather than pushing it to a presenter output port.
  This is simpler, and it is enough while there is only one delivery mechanism. Introduce a
  `*OutputBoundary`/presenter if several UIs need different presentations of the same result.
- There is no transaction spanning a whole use case. Duplicate emails are guarded by `existsByEmail`,
  and the DB constraint `uk_user_account_email` backs it up. When a use case needs multi-write
  atomicity, add a transactional decorator around the input-port beans in `config/`. Do not put
  `@Transactional` on interactors.

**When to upgrade**: split into Maven modules (`domain`, `application`, `adapters`, `bootstrap`)
once more than one team works on the codebase, or once a second delivery mechanism (CLI, messaging)
reuses the use cases. The compiler then enforces the rule, not just a test.
