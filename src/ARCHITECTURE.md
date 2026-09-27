# Architecture

Package root: `org.esonov.clean_architecture`. Packages are **feature-first** (Screaming Architecture):
the top level names what the system does; the Clean Architecture layers live inside each feature.

```
org.esonov.clean_architecture
├── CleanArchitectureApplication        Main: Spring Boot entry point
├── config/                             Main: infrastructure shared by all features (Clock)
├── shared/                             shared kernel — depends on no feature
│   ├── domain/                         DomainValidationException
│   └── adapter/in/web/                 GlobalExceptionHandler (DomainValidationException → 400)
├── user/                               feature: user accounts
│   ├── domain/                         UserAccount, UserId, EmailAddress
│   ├── application/
│   │   ├── port/in/                    RegisterUserUseCase, GetUserAccountQuery, RegisterUserCommand, UserAccountView
│   │   ├── port/out/                   UserAccountRepository
│   │   ├── usecase/                    RegisterUserInteractor, GetUserAccountInteractor
│   │   └── exception/                  EmailAlreadyRegisteredException, UserAccountNotFoundException
│   ├── adapter/
│   │   ├── in/web/                     UserAccountController, UserAccountPresenter, UserExceptionHandler
│   │   └── out/persistence/            UserAccountPersistenceAdapter, UserAccountJpaEntity
│   └── config/                         UserConfig — Main of the feature: wires interactors as input-port beans
└── order/                              feature: orders (create, get, delete)
    ├── domain/                         Order, OrderLine, Money, OrderId, CustomerId
    ├── application/
    │   ├── port/in/                    CreateOrderUseCase, GetOrderQuery, DeleteOrderUseCase, CreateOrderCommand, OrderView
    │   ├── port/out/                   OrderRepository, CustomerLookup (order's own view of customers)
    │   ├── usecase/                    CreateOrderInteractor, GetOrderInteractor, DeleteOrderInteractor
    │   └── exception/                  OrderNotFoundException, CustomerNotFoundException
    ├── adapter/
    │   ├── in/web/                     OrderController, OrderPresenter, OrderExceptionHandler
    │   ├── out/persistence/            OrderPersistenceAdapter, OrderJpaEntity (+ order_line)
    │   └── out/customer/               UserCustomerLookupAdapter — the bridge to the user feature
    └── config/                         OrderConfig
```

Inside every feature, source-code dependencies point **inward only**:
`config → adapter → application → domain`. Features may use `shared` and root `config`. A feature may reach another
feature only from one of its adapters, and only through that feature's input ports (see "Feature: order" below).

---

## Boundary: Use Cases ↔ Web and Database (feature `user`)

**Separates**: `user.domain` + `user.application` (stable policy) from `user.adapter.in.web` and
`user.adapter.out.persistence` (volatile detail).

**Type**: Full boundary, with DIP on both sides. It is deployed as a single unit, and the
compiler does not enforce it (single Maven module). ArchUnit enforces it at test time.

**Interfaces defined in inner layer**:
- `user/application/port/in/RegisterUserUseCase`, `GetUserAccountQuery`: called by outer → inner.
  They carry plain records only (`RegisterUserCommand` in, `UserAccountView` out).
- `user/application/port/out/UserAccountRepository`: implemented by outer, called by inner.

**Data crossing the boundary**:
- Input side: records only. The `UserAccount` entity never reaches the web adapter.
- Output side: the repository port takes and returns the `UserAccount` entity. This is allowed,
  because the persistence adapter is the outer layer depending *inward* on the domain. The
  persistence adapter maps it to `UserAccountJpaEntity`, which never crosses back in.
- Errors: inner layers throw their own exceptions (`DomainValidationException`,
  `EmailAlreadyRegisteredException`, `UserAccountNotFoundException`). Adapters translate
  framework errors inward (e.g. unique-constraint violation → `EmailAlreadyRegisteredException`),
  and translate these exceptions outward to HTTP (`UserExceptionHandler`, and
  `GlobalExceptionHandler` for the shared `DomainValidationException`).

**Reason for this boundary**:
- The HTTP API shape, JPA mappings and DB schema change far more often than the rules for
  registering a user.
- The use cases can be tested with an in-memory fake (`UserAccountUseCasesTest`), with no Spring and no DB.
  The fake is proven to behave like Postgres by `UserAccountRepositoryContract`.
- Postgres/JPA or Spring MVC can be swapped without editing `domain` or `application`.

**Enforced by** `src/test/java/.../architecture/CleanArchitectureBoundaryTest.java` (12 rules, written with
package wildcards so every new feature is covered automatically):
- Layered dependency rule (Domain ← Application ← Adapters ← Main) in every feature.
- `*.domain` and `*.application` must not depend on Spring, JPA, Hibernate, Jackson, servlet or Lombok types.
- The web adapter may use only input ports: no interactors, no output ports, no entities.
- Input ports must not depend on entities.
- The persistence adapter must not use the input side.
- Ports are interfaces or records only; interactors implement input ports.
- Adapters are package-private.
- Features do not depend on each other; `shared` depends on no feature.
- No package cycles.

**Known trade-offs**:
- Interactors *return* a response model rather than pushing it to a presenter output port.
  This is simpler, and it is enough while there is only one delivery mechanism. Introduce a
  `*OutputBoundary`/presenter if several UIs need different presentations of the same result.
- There is no transaction spanning a whole use case. Duplicate emails are guarded by `existsByEmail`,
  and the DB constraint `uk_user_account_email` backs it up. When a use case needs multi-write
  atomicity, add a transactional decorator around the input-port beans in the feature's `config`.
  Do not put `@Transactional` on interactors.

**When to upgrade**: split into Maven modules once more than one team works on the codebase, or once a
second delivery mechanism (CLI, messaging) reuses the use cases. With feature-first packages the natural
split is one module per feature (optionally `<feature>-domain` / `<feature>-application` /
`<feature>-adapters`) plus `shared` and `bootstrap`. The compiler then enforces the rules, not just a test.

---

## Feature: order — and the first cross-feature boundary

**Use cases**: create (`POST /api/orders`, 201), get (`GET /api/orders/{id}`, 200), delete
(`DELETE /api/orders/{id}`, 204). Unknown or malformed id → 404; unknown customer → 422; invalid data → 400.

**Business rules** (in the entity and value objects): at least 1 and at most 100 lines; quantity 1..10 000;
price non-negative and at the currency's precision (USD 12.50, JPY 1200); every line in the order's currency;
total = Σ quantity × unit price. Orders are immutable once placed; delete is a hard delete.

**Order needs customers, but must not know the user feature**:

```
 order.application.usecase.CreateOrderInteractor
        │ uses
        ▼
 order.application.port.out.CustomerLookup «interface»      ← order's own vocabulary: exists(CustomerId)
        ▲ implemented by
        │
 order.adapter.out.customer.UserCustomerLookupAdapter       ← the ONLY class in order that knows user
        │ calls
        ▼
 user.application.port.in.GetUserAccountQuery «interface»   ← user's public entry point
```

- The order domain has its own `CustomerId`; it never imports `UserId` or `UserAccount`.
- `orders.customer_id` has **no foreign key** to `user_account`: the order feature does not own that table.
  Existence is checked by the use case at creation time. Consequence: if user deletion is ever added,
  it must decide what happens to that user's orders (reject, cascade through the order input port, or keep).
- ArchUnit `features_are_independent` allows exactly this crossing: an adapter of one feature using another
  feature's input ports (and the exceptions those ports declare). Anything else fails the build.
- If the user feature were extracted into a separate service, only `UserCustomerLookupAdapter` would change
  (to an HTTP client); the order domain and use cases would not.

---

## Adding a new feature

1. Create `org.esonov.clean_architecture.<feature>/` with `domain`, `application/{port/in, port/out, usecase,
   exception}`, `adapter/{in/web, out/persistence}` and `config` — the same shape as `user`.
2. Keep feature-specific exceptions inside the feature. Only truly cross-feature concepts go to `shared`.
3. If the feature needs data from another feature, **do not import its classes**. Define an output port in
   your feature (as `order.application.port.out.CustomerLookup` does) and implement it with an adapter that
   calls the other feature's **input port**. `features_are_independent` fails the build otherwise.
4. The ArchUnit rules need no changes — they match `<root>.*.domain..`, `<root>.*.application..` and so on.
