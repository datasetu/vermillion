Feature: Vermillion is able to handle scroll search queries

  Scenario: Geo-spatial query along with scroll field
    Given Vermillion is running
    When A geo-spatial query is initiated with scroll field in body
    Then The response should contain the scroll id

  Scenario: Geo-spatial query along with scroll field and a token
    Given Vermillion is running
    When A geo-spatial query is initiated with scroll field and a token
    Then The response status should be 200

  Scenario: Geo-spatial query along with invalid coordinate value
    Given Vermillion is running
    When A geo-spatial query is initiated with invalid coordinate value
    Then The response status should be 400

  Scenario: Geo-spatial query for secured resource
    Given Vermillion is running
    When A geo-spatial query is initiated for secured resource
    Then The response status should be 200

  Scenario: Geo-spatial query for expired token
    Given Vermillion is running
    When A geo-spatial query is initiated for expired token
    Then The response status should be 403

  Scenario: Geo-spatial query along with scroll field value and distance is equal to 0m
    Given Vermillion is running
    When A geo-spatial query is initiated with scroll value and distance is equal to 0m
    Then The response status should be 400

  Scenario: Geo-spatial query along with extraneous parameters
    Given Vermillion is running
    When A geo-spatial query is initiated with extraneous parameters
    Then The response status should be 400

  Scenario: Geo-spatial query along with invalid id
    Given Vermillion is running
    When A geo-spatial query is initiated with invalid id
    Then The response status should be 400

  Scenario: Geo-spatial query along with scroll field not a string
    Given Vermillion is running
    When A geo-spatial query is initiated with scroll field not a string
    Then The response status should be 400

  Scenario: Geo-spatial query along with scroll duration equal to null
    Given Vermillion is running
    When A geo-spatial query is initiated with scroll duration equal to null
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

  Scenario: Geo-spatial query along with scroll value greater than 60m
    Given Vermillion is running
    When A geo-spatial query is initiated with scroll value greater than 60m
    Then The response status should be 400

  Scenario: Geo-spatial query along with scroll value greater than 3600s
    Given Vermillion is running
    When A geo-spatial query is initiated with scroll value greater than 3600s
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

  Scenario: Geo-spatial query along with invalid scroll unit
    Given Vermillion is running
    When A geo-spatial query is initiated with invalid scroll unit
    Then The response status should be 400

  Scenario: Scroll search query without body
    Given Vermillion is running
    When The scroll search query without body
    Then The response status should be 400

  Scenario: Scroll search query with invalid json body
    Given Vermillion is running
    When The scroll search query with invalid json body
    Then The response status should be 400

  Scenario: Scroll search query with scroll id equal to null
    Given Vermillion is running
    When The scroll search query with scroll id equal to null
    Then The response status should be 400
#
  Scenario: Scroll search query with scroll duration equal to null
    Given Vermillion is running
    When The scroll search query with scroll duration equal to null
    Then The response status should be 400

  Scenario: Scroll search query with scroll duration equal to 0m
    Given Vermillion is running
    When The scroll search query with scroll duration equal to 0m
    Then The response status should be 400

  Scenario: Scroll search query with scroll duration having invalid integer
    Given Vermillion is running
    When The scroll search query with scroll duration having invalid integer
    Then The response status should be 400

  Scenario: Scroll search query with scroll duration greater than 1hr
    Given Vermillion is running
    When The scroll search query with scroll duration greater than 1hr
    Then The response status should be 400

  Scenario: Scroll search query with scroll duration greater than 60m
    Given Vermillion is running
    When The scroll search query with scroll duration greater than 60m
    Then The response status should be 400

  Scenario: Scroll search query with scroll duration greater than 3600s
    Given Vermillion is running
    When The scroll search query with scroll duration greater than 3600s
    Then The response status should be 400

  Scenario: Scroll search query with scroll id as integer
    Given Vermillion is running
    When The scroll search query with scroll id as integer
    Then The response status should be 400

  Scenario: Scroll search query with scroll duration as integer
    Given Vermillion is running
    When The scroll search query with scroll duration as integer
    Then The response status should be 400

  Scenario: Scroll search query with invalid scroll id
    Given Vermillion is running
    When The scroll search query with invalid scroll id
    Then The response status should be 400

  Scenario: Scroll search query with invalid scroll unit
    Given Vermillion is running
    When The scroll search query with invalid scroll unit
    Then The response status should be 400

  Scenario: Scroll search query with invalid token
    Given Vermillion is running
    When The scroll search query with invalid token
    Then The response status should be 403

  Scenario: Scroll search query without scroll duration
    Given Vermillion is running
    When The scroll search query without scroll duration
    Then The response status should be 400

  Scenario: Scroll search query without scroll id
    Given Vermillion is running
    When The scroll search query without scroll id
    Then The response status should be 400

  Scenario: Scroll search query with extraneous parameters
    Given Vermillion is running
    When The scroll search query with extraneous parameters
    Then The response status should be 400


  Scenario: Scroll search query for secured resource
    Given Vermillion is running
    When The scroll search query for secured resource
    Then The response status should be 200

  Scenario: Scroll search query for secured resource and invalid id
    Given Vermillion is running
    When The scroll search query for secured resource and invalid id
    Then The response status should be 400

  Scenario: Scroll search query with token
    Given Vermillion is running
    When The scroll search query with token
    Then The response status should be 200

  Scenario: Scroll search query with empty body
    Given Vermillion is running
    When The scroll search query with empty body
    Then The response status should be 400

  Scenario: Scroll search query with expired token
    Given Vermillion is running
    When The scroll search query with expired token
    Then The response status should be 403

  Scenario: Scroll search query with scroll id not present in db
    Given Vermillion is running
    When The scroll search query with scroll id not present in db
    Then The response status should be 500

  Scenario: Scroll search query
    Given Vermillion is running
    When The scroll search query is initiated
    Then All matching records are returned

  Scenario: Scroll search query with expired scroll id
    Given Vermillion is running
    When The scroll search query with expired scroll id
    Then The response status should be 400