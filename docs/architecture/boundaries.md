# Architectural Boundaries

Type: **full boundary** (input and output ports, dependency inversion), single deployable,
enforced by ArchUnit (`CleanArchitectureBoundaryTest`). Rationale: [`src/ARCHITECTURE.md`](../../src/ARCHITECTURE.md).

```
   OUTER (volatile)                                      INNER (stable)

 HTTP / Spring MVC + Jackson ════════╗
                                     ║ Boundary 1: Web ↔ Application (input side)
 UserAccountController ══════════════╝   Crossed via: RegisterUserUseCase, GetUserAccountQuery
 UserAccountPresenter                    Data in:     RegisterUserCommand (record), String id
 UserExceptionHandler                    Data out:    UserAccountView (record) — never the entity
 GlobalExceptionHandler (shared)         Errors out:  DomainValidationException,
                                                      EmailAlreadyRegisteredException,
                                                      UserAccountNotFoundException

 UserAccountPersistenceAdapter ══════╗
                                     ║ Boundary 2: Application ↔ Database (output side, DIP)
 RegisterUserInteractor ═════════════╝   Crossed via: UserAccountRepository «interface»
 GetUserAccountInteractor                             (declared inside, implemented outside)
                                         Data:        UserAccount entity, UserId, EmailAddress
                                                      (allowed — the adapter depends inward)
                                         Errors in:   DB unique violation → EmailAlreadyRegisteredException
                                         Never crosses in: UserAccountJpaEntity, Spring/JPA exceptions

 user.config.UserConfig ═════════════╗
                                     ║ Main boundary
 everything else ════════════════════╝   The only code that references interactors by class;
                                         everyone else sees port interfaces.
```

## Boundary 3: order ↔ user (cross-feature)

```
 order.application.usecase ══════════╗
                                     ║ Crossed via: CustomerLookup «interface» (declared by order)
 order.adapter.out.customer ═════════╝ Data:        CustomerId → boolean
   UserCustomerLookupAdapter
        │ calls
        ▼
 user.application.port.in.GetUserAccountQuery   (user's input port — its public entry point)

 Never crosses: UserAccount, UserId, user persistence, user use cases. No DB foreign key between the features.
```

## What is forbidden across each boundary (and which rule checks it)

| Forbidden | ArchUnit rule |
|---|---|
| Any outward dependency inside a feature (Domain ← Application ← Adapters ← Main) | `dependency_rule` |
| Spring / JPA / Hibernate / Jackson / servlet / Lombok in any `*.domain` or `*.application` | `inner_layers_are_framework_free` |
| Web adapter using interactors, output ports or persistence | `web_adapter_talks_only_to_input_ports` |
| Web adapter touching entities | `web_adapter_does_not_touch_entities` |
| Input ports depending on entities | `input_ports_do_not_expose_entities` |
| Persistence adapter using the input side | `persistence_adapter_does_not_use_input_side` |
| Port packages containing anything but interfaces / records | `ports_are_interfaces_or_data` |
| Public adapter classes | `adapters_are_hidden` |
| A feature importing another feature (except an adapter calling its input ports) | `features_are_independent` |
| `shared` depending on any feature | `shared_kernel_does_not_know_features` |
| Interactors not implementing an input port | `interactors_implement_input_ports` |
| Package cycles | `no_package_cycles` |
