# Bank Transfer Service (Spring Boot + H2)

This is a simple Java Spring Boot application simulating money transfers between bank accounts with support for:
- Base currency validation
- FX conversion (rates stored in database)
- Concurrent-safe transfers using pessimistic locking
- In-memory H2 database
- REST API and unit tests

---

### Assumptions
- Money can be transferred to an account only in its base currency
- Money can only be transferred from an account in its base currency
- The FX conversion rate is 0.5 USD to 1 AUD
- A transaction fee of 1% applies to all transfers and is charged to transaction initiator

---

### Enhancements proposed
- A controller to add, update or delete FX conversion rate
- A transaction table to keep each transaction details for daily reconciliation

---

## Tech Stack

- Java 17+
- Spring Boot 3+
- Spring Data JPA
- H2 Database (in-memory)
- Gradle (build tool)
- JUnit 5 & Mockito (testing)

---

## Getting Started

### Prerequisites

- Java 17+
- IntelliJ IDEA (recommended)
- Gradle 8+

---
### Swagger UI

http://localhost:8080/swagger-ui/index.html#/transfer-controller/transfer
___

## Running the App

```bash
./gradlew bootRun
```

---

## Running Test

```bash
./gradlew test
```