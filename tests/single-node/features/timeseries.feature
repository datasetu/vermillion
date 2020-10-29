Feature: Vermillion is able to handle timeseries queries

    	Scenario: Timeseries query for empty payload
		Given Vermillion is running
		When Timeseries payload is empty
		Then The response status should be 400
	
	Scenario: Timeseries query for invalid payload
		Given Vermillion is running
		When Timeseries payload is invalid
		Then The response status should be 400
	
	Scenario: Timeseries query for invalid start date
		Given Vermillion is running
		When Timeseries payload start date is invalid
		Then The response status should be 400
	
	Scenario: Timeseries query for invalid end date
		Given Vermillion is running
		When Timeseries payload end date is invalid
		Then The response status should be 400

	Scenario: Timeseries query for date not present
		Given Vermillion is running
		When Timeseries payload date is not present
		Then The response status should be 400
	
	Scenario: Timeseries query for empty date
		Given Vermillion is running
		When Timeseries payload date is empty
		Then The response status should be 400
	
	Scenario: Timeseries query for invalid payload id
		Given Vermillion is running
		When Timeseries payload id is invalid
		Then The response status should be 400
	
	Scenario: Timeseries query for empty payload id
		Given Vermillion is running
		When Timeseries payload id is empty
		Then The response status should be 400

	Scenario: Timeseries query with just id as payload 
		Given Vermillion is running
		When Timeseries payload has only id
		Then The response status should be 400


	Scenario: Attribute term query
		Given Vermillion is running
		When An attribute term query is initiated
		Then All matching records are returned


