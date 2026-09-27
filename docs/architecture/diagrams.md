# Architecture Diagrams (Mermaid)

Rendered natively by GitHub and IntelliJ (Mermaid plugin). Standalone sources, one diagram per file: [`mermaid/`](mermaid/).

## 4a. Layer / package dependency diagram (arrows = source-code dependency)

```mermaid
graph TD
    subgraph MAIN["Main"]
        app["CleanArchitectureApplication"]
        config["config<br/>UseCaseConfig"]
    end
    subgraph ADAPTERS["Interface Adapters"]
        web["adapter.in.web<br/>Controller · Presenter · ApiExceptionHandler"]
        persistence["adapter.out.persistence<br/>PersistenceAdapter · JpaEntity"]
    end
    subgraph APPLICATION["Application Business Rules"]
        usecase["application.usecase<br/>RegisterUserInteractor · GetUserAccountInteractor"]
        portin["application.port.in<br/>RegisterUserUseCase · GetUserAccountQuery"]
        portout["application.port.out<br/>UserAccountRepository"]
        exception["application.exception"]
    end
    subgraph DOMAIN["Enterprise Business Rules"]
        user["domain.user<br/>UserAccount · UserId · EmailAddress"]
        domain["domain<br/>DomainValidationException"]
    end

    config --> usecase
    config --> portin
    config --> portout
    web --> portin
    web --> exception
    web --> domain
    persistence --> portout
    persistence --> exception
    persistence --> user
    usecase --> portin
    usecase --> portout
    usecase --> exception
    usecase --> user
    portin --> exception
    portout --> exception
    portout --> user
    user --> domain

    style DOMAIN fill:#2d6a4f,color:#fff
    style APPLICATION fill:#40916c,color:#fff
    style ADAPTERS fill:#74c69d,color:#000
    style MAIN fill:#d8f3dc,color:#000
```

## 4b-1. Sequence: Register user

```mermaid
sequenceDiagram
    participant HTTP as HTTP client
    participant Ctrl as UserAccountController
    participant UC as RegisterUserInteractor
    participant Ent as UserAccount / EmailAddress
    participant Repo as UserAccountRepository<br/>(PersistenceAdapter)
    participant DB as PostgreSQL
    participant Pres as UserAccountPresenter

    HTTP->>Ctrl: POST /api/users {email, displayName}
    Ctrl->>UC: register(RegisterUserCommand)
    UC->>Ent: new EmailAddress(email)
    Ent-->>UC: EmailAddress (or DomainValidationException → 400)
    UC->>Repo: existsByEmail(email)
    Repo->>DB: SELECT
    Repo-->>UC: false (true → EmailAlreadyRegisteredException → 409)
    UC->>Ent: UserAccount.register(email, name, now)
    Ent-->>UC: UserAccount
    UC->>Repo: save(account)
    Repo->>DB: INSERT (unique violation → EmailAlreadyRegisteredException → 409)
    UC-->>Ctrl: UserAccountView
    Ctrl->>Pres: created(view)
    Pres-->>HTTP: 201 Created, Location, UserAccountViewModel
```

## 4b-2. Sequence: Get user account

```mermaid
sequenceDiagram
    participant HTTP as HTTP client
    participant Ctrl as UserAccountController
    participant UC as GetUserAccountInteractor
    participant Repo as UserAccountRepository<br/>(PersistenceAdapter)
    participant DB as PostgreSQL
    participant Pres as UserAccountPresenter

    HTTP->>Ctrl: GET /api/users/{id}
    Ctrl->>UC: getById(id)
    UC->>UC: UserId.of(id) (malformed → UserAccountNotFoundException → 404)
    UC->>Repo: findById(userId)
    Repo->>DB: SELECT
    Repo-->>UC: Optional<UserAccount> (empty → 404)
    UC-->>Ctrl: UserAccountView
    Ctrl->>Pres: found(view)
    Pres-->>HTTP: 200 OK, UserAccountViewModel
```

## 4c. Entity relationship diagram (Flyway schema V1)

```mermaid
erDiagram
    USER_ACCOUNT {
        uuid id PK
        varchar(254) email UK "uk_user_account_email, stored lower-case"
        varchar(100) display_name
        timestamptz registered_at
    }
```

## 4d. Component stability chart

```mermaid
graph LR
    subgraph IDEAL_ABSTRACT["Stable + abstract (on / near main sequence)"]
        portin["port.in<br/>I=0.25 A=0.50 D=0.25"]
        portout["port.out<br/>I=0.40 A=1.00 D=0.40"]
    end
    subgraph IDEAL_CONCRETE["Unstable + concrete (on / near main sequence)"]
        usecase["usecase<br/>I=0.80 A=0 D=0.20"]
        web["adapter.in.web<br/>I=1 A=0 D=0"]
        persistence["adapter.out.persistence<br/>I=1 A=0.33 D=0.33"]
        config["config<br/>I=1 A=0 D=0"]
    end
    subgraph PAIN["Stable + concrete (Zone of Pain)"]
        domain["domain<br/>I=0 A=0 D=1.00<br/>non-volatile: OK"]
        user["domain.user<br/>I=0.25 A=0 D=0.75<br/>non-volatile: OK"]
        exception["application.exception<br/>I=0 A=0 D=1.00<br/>watch: shared hub"]
    end

    style PAIN fill:#ffe8cc,color:#000
    style IDEAL_ABSTRACT fill:#d8f3dc,color:#000
    style IDEAL_CONCRETE fill:#d8f3dc,color:#000
```

## 4e. Ports and implementations (class diagram)

```mermaid
classDiagram
    direction LR
    class RegisterUserUseCase {
        <<interface>>
        +register(RegisterUserCommand) UserAccountView
    }
    class GetUserAccountQuery {
        <<interface>>
        +getById(String) UserAccountView
    }
    class UserAccountRepository {
        <<interface>>
        +findById(UserId) Optional~UserAccount~
        +existsByEmail(EmailAddress) boolean
        +save(UserAccount) void
    }
    class RegisterUserInteractor
    class GetUserAccountInteractor
    class UserAccountPersistenceAdapter
    class UserAccountController
    class UserAccountPresenter

    RegisterUserUseCase <|.. RegisterUserInteractor
    GetUserAccountQuery <|.. GetUserAccountInteractor
    UserAccountRepository <|.. UserAccountPersistenceAdapter
    RegisterUserInteractor --> UserAccountRepository
    GetUserAccountInteractor --> UserAccountRepository
    UserAccountController --> RegisterUserUseCase
    UserAccountController --> GetUserAccountQuery
    UserAccountController --> UserAccountPresenter
```
