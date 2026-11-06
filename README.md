# BankLedger API

A small but complete **Spring Boot REST API** for bank accounts and transactions: open accounts, deposit, withdraw, transfer money between accounts, and read a per-account transaction history (ledger). Built as a learning/portfolio project — the domain (banking) is real-world, and every operation enforces a real business rule: **you can never withdraw or transfer more than the balance**.

## Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.3 (Spring Web, Spring Data JPA, Bean Validation) |
| Build | Maven (`pom.xml`) |
| Database (default) | H2 in-memory — zero setup, data resets on restart |
| Database (optional) | MySQL via the `mysql` Spring profile (see `application-mysql.properties`) |
| Tests | JUnit 5 + MockMvc (`spring-boot-starter-test`) |

## Project layout

```
src/main/java/com/murrel/bankledger/
├── BankLedgerApplication.java     # entry point
├── controller/AccountController   # HTTP layer only
├── service/AccountService         # business logic + transactions
├── repository/                    # Spring Data JPA interfaces
├── model/                         # JPA entities: Account, Transaction
├── dto/                           # request/response records (never expose entities)
└── exception/                     # custom exceptions + global handler
```

## Endpoints

Base path: `/api/accounts`

| Method | Endpoint | Body | Success | Errors |
|---|---|---|---|---|
| POST | `/api/accounts` | `{"accountHolderName": "Asha Rao", "openingBalance": 1000.00}` | 201 + account | 400 invalid body |
| GET | `/api/accounts` | — | 200 + list | — |
| GET | `/api/accounts/{id}` | — | 200 + account | 404 unknown id |
| POST | `/api/accounts/{id}/deposit` | `{"amount": 250.00}` | 200 + account | 400 invalid amount, 404 |
| POST | `/api/accounts/{id}/withdraw` | `{"amount": 75.00}` | 200 + account | 404, **422 insufficient funds** |
| POST | `/api/accounts/transfer` | `{"fromAccountId": 1, "toAccountId": 2, "amount": 120.00}` | 204 No Content | 400 same account / invalid, 404, 422 insufficient funds |
| GET | `/api/accounts/{id}/transactions` | — | 200 + ledger, newest first | 404 |

Every error returns the same JSON shape, e.g.:

```json
{ "status": 422, "error": "Unprocessable Entity",
  "message": "Insufficient funds on account 1: balance is 50.00 but 500.00 was requested",
  "fieldErrors": null, "timestamp": "2026-11-05T12:00:00" }
```

### Try it with curl

```bash
curl -X POST localhost:8080/api/accounts -H "Content-Type: application/json" \
  -d '{"accountHolderName":"Asha Rao","openingBalance":1000}'
curl -X POST localhost:8080/api/accounts/1/deposit -H "Content-Type: application/json" -d '{"amount":250}'
curl localhost:8080/api/accounts/1/transactions
```

## How to run

Prerequisite: **Java 17+** and **Maven 3.8+** (`java -version`, `mvn -v`).

```bash
cd bankledger-java
mvn spring-boot:run
```

The API is then on `http://localhost:8080`. No database setup is needed — the default profile uses in-memory H2.

**H2 console** (inspect the tables in a browser): http://localhost:8080/h2-console
— JDBC URL `jdbc:h2:mem:bankledger`, user `sa`, empty password.

**MySQL instead of H2:** create a database named `bankledger`, then run with the profile:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

Credentials come from `application-mysql.properties` (or the `DB_USERNAME` / `DB_PASSWORD` environment variables).

## How to run the tests

```bash
mvn test
```

11 tests in `BankLedgerApiTests` drive the real HTTP layer with MockMvc: account creation (201 + validation 400), get/list, 404 handling, deposit/withdraw balance changes, the insufficient-funds rule (422, balance unchanged), a full transfer between two accounts, self-transfer rejection (400), and ledger-history contents/ordering.

## Java / Spring concepts this project teaches

**Dependency Injection (DI).** Classes never `new` their collaborators. `AccountController` receives `AccountService` through its constructor, and the service receives the two repositories the same way. Spring creates the objects, wires them together, and manages their lifecycle. Constructor injection (used here) beats field injection because dependencies are explicit, `final`, and easy to replace with mocks in tests.

**JPA entities.** `Account` and `Transaction` are plain Java classes annotated with `@Entity`; fields map to columns, `@Id` + `@GeneratedValue` makes the database generate primary keys, and `@ManyToOne` models "many transactions belong to one account". Hibernate turns object operations into SQL — you almost never write SQL for CRUD.

**DTO vs entity.** The entities are the database's view of the world; DTOs (`CreateAccountRequest`, `AccountResponse`, …) are the API's view. Returning entities directly leaks internal fields, breaks when lazy-loaded relations are serialised, and couples your public API to your schema. Here the controller only ever accepts and returns DTOs (Java `record`s), and validation annotations live on the request DTOs. Mapping happens in one obvious place (`AccountResponse.from(...)`).

**Exception handling.** Business code throws meaningful exceptions — `AccountNotFoundException`, `InsufficientFundsException` — instead of building error responses everywhere. One `@RestControllerAdvice` class (`GlobalExceptionHandler`) converts each exception type into the right HTTP status (404 / 422 / 400) with a consistent JSON body, including per-field messages when `@Valid` validation fails.

**Transactions — and why transfers are atomic.** `@Transactional` on a service method means: all database work in that call commits together, or not at all. `transfer()` withdraws from one account, deposits into another, and writes two ledger rows. If anything failed halfway — say the destination update threw — Spring rolls the whole transaction back, so the withdrawal never happened either. Money cannot vanish mid-transfer. A nice side effect: because the entities are loaded inside the transaction, mutating them (`account.deposit(...)`) is enough — JPA's *dirty checking* detects the change and writes it on commit, with no explicit `save()` call. (That is also why `InsufficientFundsException` extends `RuntimeException`: by default Spring only rolls back on unchecked exceptions.)

## Resume bullets (use only after you have run and understood this project)

- "Built a Spring Boot 3 / Java 17 REST API for bank accounts and transactions (Spring Web, Spring Data JPA, H2/MySQL), with DTO-based request/response models, bean validation, and a global exception handler returning consistent error responses (404/422/400)."
- "Implemented atomic money transfers with Spring `@Transactional` service methods and a double-entry style ledger, plus an 11-test JUnit 5 + MockMvc suite covering transfers, insufficient-funds rejection, and validation."

## Interview questions about this project (with short model answers)

**1. Why is your transfer method `@Transactional`, and what would go wrong without it?**
Without a transaction, the withdrawal and the deposit are separate commits. If the app crashed — or the second account didn't exist — after the withdrawal committed, the money would be gone from the source and never arrive. With `@Transactional`, all four writes (two balance updates, two ledger rows) commit together or roll back together.

**2. Why do you return DTOs instead of the JPA entities from your controller?**
Three reasons: I control exactly which fields are exposed; I avoid lazy-loading serialisation errors (a `Transaction` holds a lazy `@ManyToOne` back to `Account`); and my API contract stays stable if I refactor the database schema. Validation also belongs on input DTOs, not on entities.

**3. How does Spring Data JPA implement your repositories if you only wrote interfaces?**
Spring Data inspects the interface at startup and generates the implementation. Extending `JpaRepository` gives CRUD for free, and derived query methods are built by parsing the method name — `findByAccountIdOrderByIdDesc` becomes a `WHERE account_id = ? ORDER BY id DESC` query.

**4. Where is the insufficient-funds rule enforced, and why there?**
In the service layer (`AccountService.requireSufficientFunds`), because it's a business rule, not an HTTP or database concern. The controller shouldn't know balances exist; a database CHECK constraint could back it up, but the service is where the rule reads naturally and where it can throw a domain exception that the global handler maps to 422.

**5. In `deposit()` you never call `accountRepository.save()` after changing the balance. Why does it still persist?**
The account was loaded inside a `@Transactional` method, so it is a *managed* entity attached to the persistence context. JPA dirty-checks managed entities at commit time and issues UPDATEs for anything that changed. `save()` is only needed for new (transient) entities — like the `Transaction` rows, which is why those do get explicit saves.
