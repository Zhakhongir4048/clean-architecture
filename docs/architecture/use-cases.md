# Use Case Flows

Control flow goes outward → inward → outward; source-code dependencies always point inward.
Interfaces are marked `«interface»`.

## Use case: Register user — `POST /api/users`

```
HTTP POST /api/users  {"email": "...", "displayName": "..."}
    │                                         Spring MVC + Jackson   [framework]
    ▼
UserAccountController.register(RegisterUserHttpRequest)      [user.adapter.in.web]
    │  translates HTTP body → RegisterUserCommand (raw strings, no validation)
    ▼
RegisterUserUseCase «interface»                              [user.application.port.in]
    │  register(RegisterUserCommand) : UserAccountView
    ▼
RegisterUserInteractor                                       [user.application.usecase]
    │
    ├─creates──▶ EmailAddress                               [user.domain]
    │              └─ normalises + validates → DomainValidationException
    │
    ├─asks────▶ UserAccountRepository «interface»           [user.application.port.out]
    │              existsByEmail(EmailAddress)
    │                  │  implemented by
    │                  ▼
    │              UserAccountPersistenceAdapter             [user.adapter.out.persistence]
    │                  └─▶ SpringDataUserAccountRepository ─▶ PostgreSQL
    │              true ⇒ EmailAlreadyRegisteredException
    │
    ├─creates──▶ UserAccount.register(email, name, clock.instant())   [user.domain]
    │              └─ validates display name; generates UserId
    │
    ├─saves───▶ UserAccountRepository.save(UserAccount) «interface»
    │                  │
    │                  ▼
    │              UserAccountPersistenceAdapter: UserAccount → UserAccountJpaEntity
    │                  └─ saveAndFlush; uk_user_account_email violation
    │                     ⇒ EmailAlreadyRegisteredException (race guard)
    │
    └─returns─▶ UserAccountView (via UserAccountViews mapper)  [user.application.port.in]
                   │
                   ▼
UserAccountPresenter.created(UserAccountView)               [user.adapter.in.web]
    │  UserAccountView → UserAccountViewModel, 201 + Location
    ▼
HTTP 201 Created  Location: /api/users/{id}  {"id", "email", "displayName", "registeredAt"}

Errors: GlobalExceptionHandler (shared)  DomainValidationException → 400
        UserExceptionHandler             EmailAlreadyRegisteredException → 409
        Spring MVC                       malformed JSON → 400 (use case never called)
```

## Use case: Get user account — `GET /api/users/{id}`

```
HTTP GET /api/users/{id}
    │
    ▼
UserAccountController.getById(String id)                     [user.adapter.in.web]
    │
    ▼
GetUserAccountQuery «interface»                              [user.application.port.in]
    │  getById(String) : UserAccountView
    ▼
GetUserAccountInteractor                                     [user.application.usecase]
    │
    ├─parses──▶ UserId.of(id)                               [user.domain]
    │              malformed ⇒ UserAccountNotFoundException
    │
    ├─reads───▶ UserAccountRepository.findById(UserId) «interface»   [user.application.port.out]
    │                  │
    │                  ▼
    │              UserAccountPersistenceAdapter: UserAccountJpaEntity → UserAccount.restore(...)
    │              empty ⇒ UserAccountNotFoundException
    │
    └─returns─▶ UserAccountView
                   │
                   ▼
UserAccountPresenter.found(UserAccountView)                  [user.adapter.in.web]
    │
    ▼
HTTP 200 OK {"id", "email", "displayName", "registeredAt"}

Errors: UserExceptionHandler  UserAccountNotFoundException → 404
```

Sequence diagrams (Mermaid): see `4b` in [`diagrams.md`](diagrams.md).
