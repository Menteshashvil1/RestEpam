Feature: Publishing trainer workload events to the workload microservice

  The two microservices integrate through an ActiveMQ queue. Every training change becomes a
  message carrying the agreed JSON contract, a service token the workload service can verify and
  the transaction id of the originating request.

  Background:
    Given a trainer "Nora" "Kim" and a trainee "Diego" "Alba" exist
    And the workload queue is empty

  Scenario: Adding a training publishes an ADD event in the agreed contract
    When a training "Morning cardio" of 60 minutes is added on "2026-07-21"
    Then an event is published to the workload queue
    And the event field "actionType" is "ADD"
    And the event field "trainerFirstName" is "Nora"
    And the event field "trainerLastName" is "Kim"
    And the event field "trainingDate" is "2026-07-21"
    And the event field "trainingDuration" is "60"
    And the event field "isActive" is "true"
    And the event names the registered trainer
    And the event body has exactly the agreed fields
    And the event carries the logical type id the workload service maps
    And the event carries a service token the workload service can verify
    And the event carries a transaction id

  Scenario: Cancelling a training publishes a DELETE event
    Given a training "Evening cardio" of 45 minutes was added on "2026-08-02"
    And the workload queue is empty
    When that training is cancelled
    Then an event is published to the workload queue
    And the event field "actionType" is "DELETE"
    And the event field "trainingDuration" is "45"

  Scenario: Deleting a trainee reports every one of their trainings as removed
    Given a training "Morning cardio" of 60 minutes was added on "2026-07-21"
    And a training "Evening cardio" of 30 minutes was added on "2026-08-02"
    And the workload queue is empty
    When the trainee profile is deleted
    Then 2 events are published to the workload queue
    And every published event has action type "DELETE"

  Scenario: A training rejected by validation publishes nothing
    When a training "Morning cardio" of 0 minutes is added on "2026-07-21"
    Then the response status is 400
    And no event is published to the workload queue

  Scenario: A training for an unknown trainee publishes nothing
    When a training is added for unknown trainee "Ghost.Trainee"
    Then the response status is 404
    And no event is published to the workload queue
