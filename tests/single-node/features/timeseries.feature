Feature: Vermillion is able to handle timeseries data


    	Scenario: Geo-spactial query with no payload
		Given Vermillion is running
		When The payload is empty
		Then The response will have "error"
	
	Scenario: Geo-spactial query with wrong id
		Given Vermillion is running
		When The payload id is wrong
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
	
	Scenario: Timeseries query for empty payload
		Given Vermillion is running
		When Timeseries payload is empty
		Then The response will have "error"
	
	Scenario: Timeseries query for random payload
		Given Vermillion is running
		When Timeseries payload is random
		Then The response will have "error"
	
	Scenario: Timeseries query for random start date
		Given Vermillion is running
		When Timeseries start date is random
		Then The response will have "error"
	
	Scenario: Timeseries query for random end date
		Given Vermillion is running
		When Timeseries end date is random
		Then The response will have "error"

	Scenario: Timeseries query for empty date
		Given Vermillion is running
		When Timeseries date is empty
		Then The response will have "error"
	
	Scenario: Timeseries query for wrong id
		Given Vermillion is running
		When Timeseries id is wrong
		Then The response will have "error"





	
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
