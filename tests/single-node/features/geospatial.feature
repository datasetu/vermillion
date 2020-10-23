Feature: Vermillion is able to handle timeseries data


        Scenario: Geo-spatial query with empty payload
                Given Vermillion is running
                When The payload is empty
                Then The response status should be "400"

        Scenario: Geo-spatial query with invalid id
                Given Vermillion is running
                When The payload id is invalid
                Then The response status should be "400"

        Scenario: Geo-spatial query with empty id
                Given Vermillion is running
                When The payload id is empty
                Then The response status should be "400"

        Scenario: Geo-spatial query for invalid payload
                Given Vermillion is running
                When The payload id is invalid
                Then The response status should be "400"
	
	Scenario: Geo-spatial query with just id as payload
                Given Vermillion is running
                When The payload has only id
                Then The response status should be "400"


        Scenario: Geo-spatial query for invalid coordinates
                Given Vermillion is running
                When The coordinates are invalid
                Then The response status should be "400"

        Scenario: Geo-spatial query for empty coordinates
                Given Vermillion is running
                When The coordinates are empty
                Then The response status should be "400"
	
	Scenario: Geo-spatial query for coordinates not present
                Given Vermillion is running
                When The coordinates are not present
                Then The response status should be "400"

	Scenario: Geo-spatial query for distance not present
                Given Vermillion is running
                When The distance is not present
                Then The response status should be "400"


	Scenario: Geo-spatial query for invalid distance
                Given Vermillion is running
                When The distance is invalid
                Then The response status should be "400"

        Scenario: Geo-spatial query for empty distance
                Given Vermillion is running
                When The distance is empty
                Then The response status should be "400"

        Scenario: Geo-spatial query
                Given Vermillion is running
                When A geo-spatial query is initiated
                Then All matching records are returned















