Feature: Vermillion is able to handle timeseries data


    	Scenario: Geo-spactial query with no payload
		Given Vermillion is running
		When The payload is empty
		Then The response will have "error"

	Scenario: Geo-spatial query for random payload
		Given Vermillion is running
		When The payload is random
		Then The response will have "error"
		
	Scenario: Geo-spatial query for change in coordinates
		Given Vermillion is running
		When The coordinates are changed
		Then The response will have "error"
	Scenario: Geo-spatial query for empty coordinates
		Given Vermillion is running
		When The coordinates are empty
		Then The response will have "error"
	Scenario: Geo-spatial query for change in distance
		Given Vermillion is running
		When The distance is changed
		Then The response will have "error"

	Scenario: Geo-spatial query for empty distance
		Given Vermillion is running
		When The distance is empty
		Then The response will have "error"




	Scenario: Geo-spatial query
		Given Vermillion is running
		When A geo-spatial query is initiated
		Then All matching records are returned

	Scenario: Timeseries query
		Given Vermillion is running
		When A timeseries query is initiated
		Then All matching records are returned

	Scenario: Attribute term query
		Given Vermillion is running
		When An attribute term query is initiated
		Then All matching records are returned

	Scenario: Attribute value query
		Given Vermillion is running
		When An attribute value query is initiated
		Then All matching records are returned

	Scenario: Complex query
		Given Vermillion is running
		When A complex query is initiated
		Then All matching records are returned
