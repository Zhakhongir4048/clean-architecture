# Humble Object Pattern

Each boundary to a hard-to-test detail is split into a **testable** side holding the logic and a
**humble** side that is too thin to need its own tests.

## Web output — UserAccountPresenter

```
TESTABLE SIDE                           HUMBLE SIDE
(UserAccountPresenter)                  (Spring MVC + Jackson)
──────────────────────────              ──────────────────────────
+ created(UserAccountView)              DispatcherServlet
    → 201 + Location /api/users/{id}      → invokes controller method
    → builds UserAccountViewModel         → serialises ViewModel to JSON
+ found(UserAccountView)                  → writes status + headers
    → 200 + UserAccountViewModel
  formats registeredAt as ISO-8601

✓ Plain object; ResponseEntity is a     ✓ Framework code — not ours to test
  value type, no container needed       ✓ Contains no project logic
✓ Covered by UserAccountControllerTest
  (@WebMvcTest)
```

## Web errors — UserExceptionHandler + GlobalExceptionHandler

```
TESTABLE SIDE                           HUMBLE SIDE
(exception handlers)                    (Spring MVC exception resolution)
──────────────────────────              ──────────────────────────
GlobalExceptionHandler (shared)         finds the @ExceptionHandler,
  DomainValidationException  → 400      serialises ProblemDetail
UserExceptionHandler (user)
  EmailAlreadyRegistered…    → 409
  UserAccountNotFound…       → 404
✓ Covered by UserAccountControllerTest  ✓ Framework code
```

## Database — UserAccountPersistenceAdapter

```
TESTABLE SIDE                           HUMBLE SIDE
(interactors + adapter mapping)         (SpringDataUserAccountRepository + Hibernate)
──────────────────────────              ──────────────────────────
RegisterUserInteractor /                interface only — Spring Data generates
GetUserAccountInteractor                the implementation; SQL by Hibernate
  → all business decisions
UserAccountPersistenceAdapter
  → UserAccount ⇄ UserAccountJpaEntity
  → unique violation → EmailAlreadyRegistered

✓ Interactors: unit tests with          ✓ No hand-written logic
  InMemoryUserAccountRepository         ✓ Verified end-to-end by
✓ Adapter: UserAccountPersistenceAdapterIT  UserAccountPersistenceAdapterIT
  (Testcontainers Postgres)               and UserAccountApiIT
```
