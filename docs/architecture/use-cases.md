# Use Case Flows

Control flow goes outward → inward → outward; source-code dependencies always point inward.
Interfaces are marked `«interface»`.

## Use case: Register user — `POST /api/users`

```
HTTP POST /api/users  {"email": "...", "displayName": "..."}
    │                                         Spring MVC + Jackson   [framework]
    ▼
UserAccountController.register(RegisterUserHttpRequest)      [adapter.in.web]
    │  translates HTTP body → RegisterUserCommand (raw strings, no validation)
    ▼
RegisterUserUseCase «interface»                              [application.port.in]
    │  register(RegisterUserCommand) : UserAccountView
    ▼
RegisterUserInteractor                                       [application.usecase]
    │
    ├─creates──▶ EmailAddress                               [domain.user]
    │              └─ normalises + validates → DomainValidationException
    │
    ├─asks────▶ UserAccountRepository «interface»           [application.port.out]
    │              existsByEmail(EmailAddress)
    │                  │  implemented by
    │                  ▼
    │              UserAccountPersistenceAdapter             [adapter.out.persistence]
    │                  └─▶ SpringDataUserAccountRepository ─▶ PostgreSQL
    │              true ⇒ EmailAlreadyRegisteredException
    │
    ├─creates──▶ UserAccount.register(email, name, clock.instant())   [domain.user]
    │              └─ validates display name; generates UserId
    │
    ├─saves───▶ UserAccountRepository.save(UserAccount) «interface»
    │                  │
    │                  ▼
    │              UserAccountPersistenceAdapter: UserAccount → UserAccountJpaEntity
    │                  └─ saveAndFlush; uk_user_account_email violation
    │                     ⇒ EmailAlreadyRegisteredException (race guard)
    │
    └─returns─▶ UserAccountView (via UserAccountViews mapper)  [application.port.in]
                   │
                   ▼
UserAccountPresenter.created(UserAccountView)               [adapter.in.web]
    │  UserAccountView → UserAccountViewModel, 201 + Location
    ▼
HTTP 201 Created  Location: /api/users/{id}  {"id", "email", "displayName", "registeredAt"}

Errors: ApiExceptionHandler  DomainValidationException → 400
                             EmailAlreadyRegisteredException → 409
        Spring MVC           malformed JSON → 400 (use case never called)
```

## Use case: Get user account — `GET /api/users/{id}`

```
HTTP GET /api/users/{id}
    │
    ▼
UserAccountController.getById(String id)                     [adapter.in.web]
    │
    ▼
GetUserAccountQuery «interface»                              [application.port.in]
    │  getById(String) : UserAccountView
    ▼
GetUserAccountInteractor                                     [application.usecase]
    │
    ├─parses──▶ UserId.of(id)                               [domain.user]
    │              malformed ⇒ UserAccountNotFoundException
    │
    ├─reads───▶ UserAccountRepository.findById(UserId) «interface»   [application.port.out]
    │                  │
    │                  ▼
    │              UserAccountPersistenceAdapter: UserAccountJpaEntity → UserAccount.restore(...)
    │              empty ⇒ UserAccountNotFoundException
    │
    └─returns─▶ UserAccountView
                   │
                   ▼
UserAccountPresenter.found(UserAccountView)                  [adapter.in.web]
    │
    ▼
HTTP 200 OK {"id", "email", "displayName", "registeredAt"}

Errors: ApiExceptionHandler  UserAccountNotFoundException → 404
```

Sequence diagrams (Mermaid): see `4b` in [`diagrams.md`](diagrams.md).
