Feature: Vermillion is able to handle scroll search queries

  Scenario: Geo-spatial query along with scroll field
    Given Vermillion is running
    When A geo-spatial query is initiated with scroll field in body
    Then The response should contain the scroll id

  Scenario: Geo-spatial query along with extraneous parameters
    Given Vermillion is running
    When A geo-spatial query is initiated with extraneous parameters
    Then The response status should be 400

  Scenario: Geo-spatial query along with scroll field not a string
    Given Vermillion is running
    When A geo-spatial query is initiated with scroll field not a string
    Then The response status should be 400

  Scenario: Geo-spatial query along with response size as string
    Given Vermillion is running
    When A geo-spatial query is initiated with response size as string
    Then The response status should be 400

  Scenario: Geo-spatial query along with invalid scroll value
    Given Vermillion is running
    When A geo-spatial query is initiated with invalid scroll value
    Then The response status should be 400

  Scenario: Geo-spatial query along with scroll value equal to 0
    Given Vermillion is running
    When A geo-spatial query is initiated with scroll value equal to 0
    Then The response status should be 400

  Scenario: Geo-spatial query along with scroll value greater than 1hr
    Given Vermillion is running
    When A geo-spatial query is initiated with scroll value greater than 1hr
    Then The response status should be 400

  Scenario: Geo-spatial query along with invalid response size integer
    Given Vermillion is running
    When A geo-spatial query is initiated with invalid response size integer
    Then The response status should be 400

  Scenario: Geo-spatial query along with response size greater than 10000
    Given Vermillion is running
    When A geo-spatial query is initiated with response size greater than 10000
    Then The response status should be 400

  Scenario: Geo-spatial query along with response size less than 0
    Given Vermillion is running
    When A geo-spatial query is initiated with response size less than 0
    Then The response status should be 400

  Scenario: Scroll search query without body
    Given Vermillion is running
    When The scroll search query without body
    Then The response status should be 400

  Scenario: Scroll search query with invalid json body
    Given Vermillion is running
    When The scroll search query with invalid json body
    Then The response status should be 400

  Scenario: Scroll search query without scroll duration
    Given Vermillion is running
    When The scroll search query without scroll duration
    Then The response status should be 400

  Scenario: Scroll search query without scroll id
    Given Vermillion is running
    When The scroll search query without scroll id
    Then The response status should be 400

  Scenario: Scroll search query with token
    Given Vermillion is running
    When The scroll search query with token
    Then The response status should be 200

  Scenario: Scroll search query with invalid token
    Given Vermillion is running
    When The scroll search query with invalid token
    Then The response status should be 403

  Scenario: Scroll search query with extraneous parameters
    Given Vermillion is running
    When The scroll search query with extraneous parameters
    Then The response status should be 400

  Scenario: Scroll search query
    Given Vermillion is running
    When The scroll search query is initiated
    Then All matching records are returned