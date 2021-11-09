Feature: Vermillion is able to handle provider query


  Scenario: Consumer publishes public file data with valid token
    Given Vermillion is running
    When The consumer publishes public file data with valid token
    Then The response status should be 201

  Scenario: Consumer downloads public file by query
    Given Vermillion is running
    When The consumer downloads public file by query
    Then The response status should be 202

  Scenario: Consumer downloads public file by query without id
    Given Vermillion is running
    When The consumer downloads public file by query without id
    Then The response status should be 202

  Scenario: Consumer downloads public file by query with invalid id
    Given Vermillion is running
    When The consumer downloads public file by query with invalid id
    Then The response status should be 403


  Scenario: Consumer downloads public file by query without query
    Given Vermillion is running
    When The consumer downloads public file by query without query
    Then The response status should be 400

  Scenario: Consumer downloads public file by query with invalid query
    Given Vermillion is running
    When The consumer downloads public file by query with invalid query
    Then The response status should be 404

  Scenario: Consumer downloads secure file by query
    Given Vermillion is running
    When The consumer downloads secure file by query
    Then The response status should be 403

  Scenario: Consumer downloads public file by query with empty payload
    Given Vermillion is running
    When The consumer downloads public file by query with empty payload
    Then The response status should be 400

  Scenario: Consumer downloads public file by query without email payload
    Given Vermillion is running
    When The consumer downloads public file by query without email payload
    Then The response status should be 400

  Scenario: Consumer downloads public file by query with invalid email
    Given Vermillion is running
    When The consumer downloads public file by query with invalid email
    Then The response status should be 400