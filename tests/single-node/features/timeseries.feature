Feature: Vermillion is able to handle timeseries data


    	Scenario: Timeseries query for empty payload
		Given Vermillion is running
		When Timeseries payload is empty
		Then The response status should be "400"
	
	Scenario: Timeseries query for invalid payload
		Given Vermillion is running
		When Timeseries payload is invalid
		Then The response status should be "400"
	
	Scenario: Timeseries query for invalid start date
		Given Vermillion is running
		When Timeseries start date is invalid
		Then The response status should be "400"
	
	Scenario: Timeseries query for invalid end date
		Given Vermillion is running
		When Timeseries end date is invalid
		Then The response status should be "400"

	Scenario: Timeseries query for empty date
		Given Vermillion is running
		When Timeseries date is empty
		Then The response status should be "400"
	
	Scenario: Timeseries query for invalid payload id
		Given Vermillion is running
		When Timeseries id is invalid
		Then The response status should be "400"
	
	Scenario: Timeseries query for empty payload id
		Given Vermillion is running
		When Timeseries id is empty
		Then The response status should be "400"

	Scenario: Attribute term query
		Given Vermillion is running
		When An attribute term query is initiated
		Then All matching records are returned


