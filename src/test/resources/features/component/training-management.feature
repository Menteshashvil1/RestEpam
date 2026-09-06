Feature: Recording and cancelling trainings

  A training is always recorded between an existing trainee and an existing trainer, and can be
  cancelled afterwards.

  Background:
    Given a trainer "Lena" "Ford" and a trainee "Marco" "Ricci" exist

  Scenario: A training is recorded for a trainee and a trainer
    When a training "Morning cardio" of 60 minutes is added on "2026-07-21"
    Then the response status is 200
    And the trainee has 1 training recorded

  Scenario: A training without a name is rejected
    When a training "" of 60 minutes is added on "2026-07-21"
    Then the response status is 400
    And the trainee has 0 trainings recorded

  Scenario: A training with a non-positive duration is rejected
    When a training "Morning cardio" of 0 minutes is added on "2026-07-21"
    Then the response status is 400

  Scenario: A training longer than the allowed maximum is rejected
    When a training "Marathon session" of 601 minutes is added on "2026-07-21"
    Then the response status is 400

  Scenario: A training for an unknown trainee is rejected
    When a training is added for unknown trainee "Ghost.Trainee"
    Then the response status is 404

  Scenario: A recorded training can be cancelled
    Given a training "Evening cardio" of 45 minutes was added on "2026-08-02"
    When that training is cancelled
    Then the response status is 200
    And the trainee has 0 trainings recorded

  Scenario: Cancelling a training that does not exist is rejected
    When training 999999 is cancelled
    Then the response status is 404
