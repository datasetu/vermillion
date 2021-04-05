Feature: Vermillion is able to handle latest API



  Scenario: Latest API query
    Given Vermillion is running
    When A latest API query is initiated
    Then The response status should be 200


  Scenario: Latest API query for public resource id
    Given Vermillion is running
    When A latest API query is initiated for public resource id
    Then The response status should be 200

  Scenario: Latest API query with invalid resource id
    Given Vermillion is running
    When A latest API query with invalid resource id
    Then The response status should be 400

  Scenario: Latest API query with empty resource id
    Given Vermillion is running
    When A latest API query is with empty resource id
    Then The response status should be 400

  Scenario: Latest API query without resource id
    Given Vermillion is running
    When A latest API query is without resource id
    Then The response status should be 400

  Scenario: Latest API query without token
    Given Vermillion is running
    When A latest API query is without token
    Then The response status should be 400

  Scenario: Latest API query with empty token
    Given Vermillion is running
    When A latest API query is with empty token
    Then The response status should be 400

  Scenario: Latest API query with invalid token
    Given Vermillion is running
    When A latest API query is with invalid token
    Then The response status should be 400