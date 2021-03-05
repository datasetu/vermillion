Feature: Vermillion is able to handle latest API



  Scenario: Latest API query
    Given Vermillion is running
    When A latest API query is initiated
    Then All matching records are returned

  Scenario: Latest API query with invalid resource id
    Given Vermillion is running
    When A latest API query with invalid resource id
    Then The response status should be 400

  Scenario: Latest API query with empty resource id
    Given Vermillion is running
    When A latest API query is with empty resource id
    Then The response status should be 400

