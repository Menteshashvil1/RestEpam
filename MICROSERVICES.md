# Gym CRM — Microservices

This repository (`RestEpam`) is the **main microservice**. The full solution is split across
three repositories plus an ActiveMQ broker, and they must be run together.

| Component | Repository / image | Port | Role |
|---|---|---|---|
| Main service (Gym CRM) | this repo — `RestEpam` | 8080 | REST API; **publishes** workload events |
| Trainer Workload Service | https://github.com/Menteshashvil1/trainer-workload-service | 8081 | **Consumes** workload events; stores monthly summaries in MongoDB |
| Eureka Discovery Server | https://github.com/Menteshashvil1/eureka-server | 8761 | Service registry |
| ActiveMQ broker | `apache/activemq-classic` | 61616 / 8161 | Transports the events |
| MongoDB | `mongo` | 27017 | Stores the trainers monthly summaries |

## Communication between the services

Inter-service communication is **asynchronous**. The main service hands each workload event to
ActiveMQ and returns immediately; it never calls the workload service directly, and it does not
need that service to be running.

```
Client ──► Main (RestEpam) ──POST   /api/v1/trainings         (ADD)   ─┐
        ├► Main (RestEpam) ──DELETE /api/v1/trainings/{id}    (DELETE)─┤
        └► Main (RestEpam) ──DELETE /api/v1/trainees/{username} ───────┤ (one DELETE per training)
                                                                       ▼
                                              ActiveMQ  trainer.workload.queue
                                        (JSON text message + JWT + transactionId)
                                                                       ▼
                                     Trainer Workload Service  @JmsListener
                                        ├─ valid   ──► MongoDB monthly summary
                                        └─ invalid ──► trainer.workload.dlq
                                                                       ▼
                              GET /api/v1/workload/{username} ──► summary (REST, read side)
```

The workload service's former `POST /api/v1/workload` endpoint is gone — that channel is the
queue now. Its `GET /api/v1/workload/{username}` read endpoint remains, since a requester still
asks for a trainer's hours over REST.

### Message contract

Body — JSON text message:

```json
{
  "trainerUsername": "Mary.Smith",
  "trainerFirstName": "Mary",
  "trainerLastName": "Smith",
  "isActive": true,
  "trainingDate": "2026-07-21",
  "trainingDuration": 60,
  "actionType": "ADD"
}
```

Message properties (JMS property names must be valid Java identifiers, so the old hyphenated
HTTP headers became camelCase):

| Property | Meaning |
|---|---|
| `_type` | Logical Jackson type id (`trainerWorkload`); each service maps it to its own class, so neither needs the other's package names |
| `authToken` | Bearer JWT minted by the main service, replacing the `Authorization` header |
| `transactionId` | The transaction id of the originating HTTP request, replacing `X-Transaction-Id` |

Both sides pin this contract with Cucumber. The main service's
`workload-event-publishing.feature` asserts what it puts on the queue, and the workload service's
`workload-event-consumption.feature` feeds exactly that JSON through a real broker. A one-sided
change breaks a build.

### Dead letter queue

Two tiers, by failure kind:

- **Permanently invalid** — required information missing, or a missing/invalid/expired token.
  Retrying cannot help, so the listener parks the message on `trainer.workload.dlq` immediately
  with a `deadLetterReason` property and the original body intact, then acknowledges it.
- **Transient or unparseable** — an unexpected error or a body Jackson cannot read. The listener
  lets it propagate, ActiveMQ redelivers, and once redeliveries are exhausted the broker moves it
  to its own `ActiveMQ.DLQ`.

### Where the summaries are stored

The workload service keeps each trainer's summary as one MongoDB document in the
`trainer_workloads` collection, shaped as the trainer's profile plus a list of years, each holding
a list of months:

```json
{
  "trainerUsername": "Mary.Smith",
  "trainerFirstName": "Mary",
  "trainerLastName": "Smith",
  "trainerStatus": true,
  "years": [
    { "year": 2026, "months": [ { "month": 7, "trainingSummaryDuration": 150 } ] }
  ]
}
```

On each event the service loads the document by username, finds (or creates) the year and month
taken from the training date, and adds the training duration to the stored
`trainingSummaryDuration` — subtracting it instead when the event is a `DELETE`, floored at zero.
Indexes: a unique one on `trainerUsername`, and a compound one on
`trainerFirstName` + `trainerLastName` backing `GET /api/v1/workload?firstName=..&lastName=..`.
Concurrent events for the same trainer are made safe by an optimistic-locking version field; a
clash makes the broker redeliver the message.

### Why there is no circuit breaker any more

The previous module wrapped the synchronous Feign call in a Resilience4j circuit breaker. With
messaging the broker itself is the decoupling point, so that breaker is gone along with the REST
client. Failure handling is now: a bounded send timeout (`gymcrm.messaging.send-timeout-ms`) so an
unreachable broker fails fast instead of hanging a training request, an error-level log carrying
the full event if publishing fails (the training still succeeds), broker redelivery on the consumer
side, and the dead letter queues above.

## Requirement → where

| Requirement | Where |
|---|---|
| Replace REST between microservices with async ActiveMQ | `WorkloadNotifier` (publisher) → `WorkloadMessageListener` (consumer); `POST /api/v1/workload` removed |
| Dead letter queue for invalid messages | `DeadLetterPublisher` + the reject path in `WorkloadMessageListener` |
| Monthly summary persisted in MongoDB | `TrainerWorkloadDocument` + `TrainerWorkloadRepository` + `WorkloadService` in the workload service |
| Eureka discovery | `eureka-server`; both services register as clients |
| JWT between services | Minted in `WorkloadNotifier`, verified in `WorkloadMessageListener` via the shared `security.jwt.secret` |
| Two levels of logging + transactionId | `TransactionLoggingFilter` for HTTP; `TRANSACTION` and `MESSAGING` loggers in the listener for the queue, correlated by the propagated `transactionId` |
| Component and integration tests in Cucumber | `features/component` and `features/integration` in both services (see Testing below) |

## Testing

Both services carry three layers, and every Cucumber feature covers the positive path and the
ways it can go wrong.

| Layer | Where | What it covers |
|---|---|---|
| Unit | JUnit + Mockito | Services, the publisher, the listener's accept/reject rules, the Mongo mapping and its indexes |
| Component (Cucumber) | `features/component` | Each service on its own through its real entry points, with the other service absent |
| Integration (Cucumber) | `features/integration` | The seam between the services: the queue, the message contract, the service token and the dead letter queue |

Component scenarios drive the main service through its REST API (registration, login, recording
and cancelling trainings) and the workload service through its queue and its read endpoints.

The integration scenarios test the seam from both ends against a real embedded broker: the main
service's feature asserts the exact JSON and message properties it publishes, and the workload
service's feature feeds precisely those messages in and checks what is stored or dead-lettered.
Running both services together additionally needs Docker (broker, database), so that end-to-end
pass is a manual step; the Cucumber suites need nothing but Maven.

Run one layer on its own with:

```bash
mvn test -Dtest=CucumberComponentTest
mvn test -Dtest=CucumberIntegrationTest
```

## Build

Java 21 + Maven, in each repository:

```bash
mvn clean package
```

## Run

Start the broker and the database first:

```bash
docker run -d --name activemq -p 61616:61616 -p 8161:8161 apache/activemq-classic:6.1.4
docker run -d --name mongo -p 27017:27017 mongo:7
```

Then, in order:

```bash
java -jar eureka-server/target/eureka-server-1.0-SNAPSHOT.jar
java -jar trainer-workload-service/target/trainer-workload-service-1.0-SNAPSHOT.jar
java -jar target/gym-crm-rest-1.0-SNAPSHOT.jar
```

Both services must share one JWT secret, so the token the main service signs verifies downstream:

```bash
export SECURITY_JWT_SECRET=<same-256-bit-secret-for-both>
```

Broker address, database URI and queue names are overridable via `ACTIVEMQ_BROKER_URL`, `ACTIVEMQ_USER`,
`ACTIVEMQ_PASSWORD`, `MONGODB_URI`, `WORKLOAD_QUEUE` and `WORKLOAD_DLQ`. The tests need none of
this — they run against an embedded in-VM broker with the database layer stubbed.

- ActiveMQ console — http://localhost:8161 (`admin`/`admin`): watch `trainer.workload.queue` and
  `trainer.workload.dlq`
- Swagger — main http://localhost:8080/swagger-ui.html, workload http://localhost:8081/swagger-ui.html
- Eureka dashboard — http://localhost:8761

## Trying it out

1. `POST /api/v1/trainings` on the main service (port 8080) to add a training.
2. `GET /api/v1/workload/{trainerUsername}` on the workload service (port 8081) — the hours are
   there, having arrived over the queue.
3. `DELETE /api/v1/trainings/{id}` (or delete the trainee) — the hours go back down.
4. Stop the workload service and repeat step 1: the training still succeeds and the message waits
   on the queue. Start the service again and it catches up.
