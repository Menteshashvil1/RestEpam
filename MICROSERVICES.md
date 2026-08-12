# Gym CRM — Microservices

This repository (`RestEpam`) is the **main microservice**. The full solution is split across
three repositories that must be run together.

| Service | Repository | Port | Role |
|---|---|---|---|
| Main service (Gym CRM) | this repo — `RestEpam` | 8080 | Existing REST API; produces workload events |
| Eureka Discovery Server | https://github.com/Menteshashvil1/eureka-server | 8761 | Service registry |
| Trainer Workload Service | https://github.com/Menteshashvil1/trainer-workload-service | 8081 | Consumes workload events; keeps monthly summaries in-memory |

## Flow

```
Client ──► Main (RestEpam) ──POST   /api/v1/trainings        (ADD)   ─┐
        ├► Main (RestEpam) ──DELETE /api/v1/trainings/{id}   (DELETE)─┤
        └► Main (RestEpam) ──DELETE /api/v1/trainees/{username} ──────┤  (cascade: one DELETE per training)
                                                                      ▼
                              Feign (via Eureka) + service JWT + X-Transaction-Id
                                                 + Resilience4j circuit breaker
                                                                      ▼
                              Trainer Workload Service   POST /api/v1/workload
                                            └► in-memory monthly summary DB
                              GET /api/v1/workload/{username} ──► summary
```

Adding a training sends an `ADD` event; deleting a training or deleting a trainee (which
cascade-removes their trainings) sends `DELETE` events that subtract the hours. If the workload
service is unreachable, the circuit breaker falls back and the main operation still succeeds.

## Requirement → where

1. **Separate Spring Boot microservice** — `trainer-workload-service`.
2. **REST endpoint for workload** — `POST /api/v1/workload` (username, first/last name, isActive,
   training date, duration, action type ADD/DELETE) → `200 OK`.
3. **In-memory monthly summary** — `WorkloadService` + `TrainerWorkload` (per year → per month);
   read via `GET /api/v1/workload/{username}`.
4. **Main calls secondary on add/delete** — `TrainingService` / `TraineeService` → `WorkloadNotifier`
   → `TrainerWorkloadClient` (Feign). Trainee deletion reports every removed training.
5. **Eureka discovery** — `eureka-server`; both services register as clients.
6. **Circuit breaker** — Resilience4j behind Feign (`TrainerWorkloadClientFallback`).
7. **JWT bearer between services** — main mints a service token (`WorkloadFeignConfig`), workload
   service verifies it with the shared `security.jwt.secret` (`JwtAuthenticationFilter`).
8. **Two logging levels + transactionId** — `TransactionLoggingFilter` in both services; the id is
   generated/propagated via `X-Transaction-Id` and passed downstream by the Feign interceptor.

REST endpoints use Richardson maturity level 2. A training is deleted when a session is cancelled
or when its trainee is deleted.

## Build (each repo)

Java 21 + Maven:

```bash
mvn clean package
```

## Run (in order)

```bash
java -jar eureka-server/target/eureka-server-1.0-SNAPSHOT.jar
java -jar trainer-workload-service/target/trainer-workload-service-1.0-SNAPSHOT.jar
java -jar target/gym-crm-rest-1.0-SNAPSHOT.jar
```

Use the **same** secret for the main and workload services so signed tokens verify:

```bash
export SECURITY_JWT_SECRET=<same-256-bit-secret-for-both>
```

Swagger: main `http://localhost:8080/swagger-ui.html`,
workload `http://localhost:8081/swagger-ui.html`.
Eureka dashboard: `http://localhost:8761`.
