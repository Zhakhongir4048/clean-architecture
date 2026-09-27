# Architecture Diagrams (Mermaid)

Rendered natively by GitHub and IntelliJ (Mermaid plugin). Standalone sources, one diagram per file: [`mermaid/`](mermaid/).

## 4a. Package dependency diagram, feature-first (arrows = source-code dependency)

```mermaid
graph TD
    app["CleanArchitectureApplication"]
    config["config<br/>ClockConfig"]

    subgraph USER["feature: user"]
        subgraph U_MAIN["Main"]
            ucfg["user.config<br/>UserConfig"]
        end
        subgraph U_ADAPTERS["Interface Adapters"]
            web["user.adapter.in.web<br/>Controller · Presenter · UserExceptionHandler"]
            persistence["user.adapter.out.persistence<br/>PersistenceAdapter · JpaEntity"]
        end
        subgraph U_APPLICATION["Application Business Rules"]
            usecase["user.application.usecase<br/>RegisterUserInteractor · GetUserAccountInteractor"]
            portin["user.application.port.in<br/>RegisterUserUseCase · GetUserAccountQuery"]
            portout["user.application.port.out<br/>UserAccountRepository"]
            exception["user.application.exception"]
        end
        subgraph U_DOMAIN["Enterprise Business Rules"]
            udomain["user.domain<br/>UserAccount · UserId · EmailAddress"]
        end
    end

    subgraph SHARED["shared kernel"]
        sweb["shared.adapter.in.web<br/>GlobalExceptionHandler"]
        sdomain["shared.domain<br/>DomainValidationException"]
    end

    ucfg --> usecase
    ucfg --> portin
    ucfg --> portout
    web --> portin
    web --> exception
    persistence --> portout
    persistence --> exception
    persistence --> udomain
    usecase --> portin
    usecase --> portout
    usecase --> exception
    usecase --> udomain
    usecase --> sdomain
    portin --> exception
    portout --> exception
    portout --> udomain
    udomain --> sdomain
    sweb --> sdomain

    style U_DOMAIN fill:#2d6a4f,color:#fff
    style U_APPLICATION fill:#40916c,color:#fff
    style U_ADAPTERS fill:#74c69d,color:#000
    style U_MAIN fill:#d8f3dc,color:#000
    style SHARED fill:#e9ecef,color:#000
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
        portin["user.application.port.in<br/>I=0.25 A=0.50 D=0.25"]
        portout["user.application.port.out<br/>I=0.40 A=1.00 D=0.40"]
    end
    subgraph IDEAL_CONCRETE["Unstable + concrete (on / near main sequence)"]
        usecase["user.application.usecase<br/>I=0.83 A=0 D=0.17"]
        web["user.adapter.in.web<br/>I=1 A=0 D=0"]
        persistence["user.adapter.out.persistence<br/>I=1 A=0.33 D=0.33"]
        ucfg["user.config<br/>I=1 A=0 D=0"]
        sweb["shared.adapter.in.web<br/>I=1 A=0 D=0"]
    end
    subgraph PAIN["Stable + concrete (Zone of Pain)"]
        sdomain["shared.domain<br/>I=0 A=0 D=1.00<br/>non-volatile shared kernel: OK"]
        udomain["user.domain<br/>I=0.25 A=0 D=0.75<br/>non-volatile: OK"]
        exception["user.application.exception<br/>I=0 A=0 D=1.00<br/>feature-local: OK"]
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
