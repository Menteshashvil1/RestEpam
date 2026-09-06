Feature: Registering and authenticating gym users

  The main microservice generates the username and password on registration and hands out a JWT
  that every other endpoint requires.

  Scenario: A new trainee receives generated credentials
    When a trainee registers with first name "John" and last name "Doe"
    Then the response status is 201
    And the returned username starts with "John.Doe"
    And a password and a token are returned

  Scenario: A new trainer receives generated credentials
    When a trainer registers with first name "Mary" and last name "Smith"
    Then the response status is 201
    And the returned username starts with "Mary.Smith"

  Scenario: A registered user can log in with the generated password
    Given a trainer is registered with first name "Peter" and last name "Novak"
    When that user logs in with the generated password
    Then the response status is 200
    And a token is returned

  Scenario: The same person cannot hold both a trainer and a trainee profile
    Given a trainer is registered with first name "Anna" and last name "Kovacs"
    When a trainee registers with first name "Anna" and last name "Kovacs"
    Then the response status is 409

  Scenario: Logging in with a wrong password is refused
    Given a trainer is registered with first name "Igor" and last name "Petrov"
    When that user logs in with password "not-the-password"
    Then the response status is 401

  Scenario: An anonymous caller cannot read a profile
    Given a trainer is registered with first name "Sara" and last name "Klein"
    When the trainer profile is requested without a token
    Then the response status is 401

  Scenario: A caller with a valid token can read the profile
    Given a trainer is registered with first name "Omar" and last name "Haddad"
    When the trainer profile is requested with the returned token
    Then the response status is 200
    And the returned first name is "Omar"
