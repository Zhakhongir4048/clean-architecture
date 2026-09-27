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

## Use case: Create order — `POST /api/orders`

```
HTTP POST /api/orders  {"customerId", "currency", "lines": [{"productName", "quantity", "unitPrice"}]}
    │
    ▼
OrderController.create(CreateOrderHttpRequest)               [order.adapter.in.web]
    │  HTTP body → CreateOrderCommand (raw values)
    ▼
CreateOrderUseCase «interface»                               [order.application.port.in]
    ▼
CreateOrderInteractor                                        [order.application.usecase]
    │
    ├─parses──▶ CustomerId.of, Money.parseCurrency           [order.domain]  invalid ⇒ 400
    ├─creates─▶ Order.place(customer, currency, lines, now)  [order.domain]
    │              └─ ≥1 line, quantity 1..10000, price precision, same currency ⇒ else 400
    │
    ├─asks────▶ CustomerLookup.exists(CustomerId) «interface»        [order.application.port.out]
    │                  │  implemented by
    │                  ▼
    │              UserCustomerLookupAdapter                  [order.adapter.out.customer]
    │                  │  calls
    │                  ▼
    │              GetUserAccountQuery.getById «interface»    [user.application.port.in]  ← other feature
    │              false ⇒ CustomerNotFoundException ⇒ 422
    │
    ├─saves───▶ OrderRepository.save(Order) «interface»      [order.application.port.out]
    │                  ▼
    │              OrderPersistenceAdapter ─▶ orders + order_line
    │
    └─returns─▶ OrderView (via OrderViews)
                   ▼
OrderPresenter.created ─▶ HTTP 201 Created, Location: /api/orders/{id}, money as strings ("11.75")
```

## Use case: Get order — `GET /api/orders/{id}`

```
OrderController.getById ─▶ GetOrderQuery ─▶ GetOrderInteractor
    ├─ OrderId.of(id)                  malformed ⇒ OrderNotFoundException ⇒ 404
    ├─ OrderRepository.findById        empty ⇒ 404
    └─ OrderView ─▶ OrderPresenter.found ─▶ HTTP 200
```

## Use case: Delete order — `DELETE /api/orders/{id}`

```
OrderController.delete ─▶ DeleteOrderUseCase ─▶ DeleteOrderInteractor
    ├─ OrderId.of(id)                  malformed ⇒ 404
    ├─ OrderRepository.deleteById      false (no such order) ⇒ OrderNotFoundException ⇒ 404
    │     └─ order_line rows removed by ON DELETE CASCADE
    └─ OrderPresenter.deleted ─▶ HTTP 204 No Content
```

Sequence diagrams (Mermaid, user feature): see `4b` in [`diagrams.md`](diagrams.md).
