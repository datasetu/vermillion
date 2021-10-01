Feature: Vermillion is able to handle download query


  Scenario: Consumer publishes secure file data with a valid token
    Given Vermillion is running
    When The consumer publishes secure file data with a valid token
    Then The response status should be 201

  Scenario: Consumer downloads by query with a valid token
    Given Vermillion is running
    When The consumer downloads by query with a valid token
    Then The response status should be 202

  Scenario: Consumer downloads by query without id
    Given Vermillion is running
    When The consumer downloads by query without id
    Then The response status should be 202

  Scenario: Consumer downloads by query with empty token
    Given Vermillion is running
    When The consumer downloads by query with empty token
    Then The response status should be 403

  Scenario: Consumer downloads by query without a token
    Given Vermillion is running
    When The consumer downloads by query without a token
    Then The response status should be 400


  Scenario: Consumer downloads by query with empty id
    Given Vermillion is running
    When The consumer downloads by query with empty id
    Then The response status should be 403

  Scenario: Consumer downloads by query without query parameters
    Given Vermillion is running
    When The consumer downloads by query without query parameters
    Then The response status should be 400

  Scenario: Consumer downloads by query with invalid token
    Given Vermillion is running
    When The consumer downloads by query with invalid token
    Then The response status should be 403

  Scenario: Consumer downloads by query with invalid id
    Given Vermillion is running
    When The consumer downloads by query with invalid id
    Then The response status should be 403

  Scenario: Consumer downloads by query with invalid query parameter
    Given Vermillion is running
    When The consumer downloads by query with invalid query parameter
    Then The response status should be 404

  Scenario: Consumer downloads by query with unauthorized id
    Given Vermillion is running
    When The consumer downloads by query with unauthorized id
    Then The response status should be 404

  Scenario: Consumer publishes secure file data with a valid token for 20secs
    Given Vermillion is running
    When The consumer publishes secure file data with a valid token for 20secs
    Then The response status should be 201

  Scenario: Consumer downloads by query with expired token
    Given Vermillion is running
    When The consumer downloads by query with expired token
    Then The response status should be 403

  Scenario: Consumer downloads by query with expired token via reroute link
    Given Vermillion is running
    When The consumer downloads by query with expired token via reroute link
    Then The response status should be 403