Feature: Vermillion is able to handle timeseries queries

    	Scenario: Timeseries query for empty body
		Given Vermillion is running
		When Timeseries query body is empty
		Then The response status should be 400
	
	Scenario: Timeseries query for invalid body
		Given Vermillion is running
		When Timeseries query body is invalid
		Then The response status should be 400
	
	Scenario: Timeseries query for invalid start date
		Given Vermillion is running
		When Timeseries query start date is invalid
		Then The response status should be 400
	
	Scenario: Timeseries query for invalid end date
		Given Vermillion is running
		When Timeseries query end date is invalid
		Then The response status should be 400

	Scenario: Timeseries query for date not present
		Given Vermillion is running
		When Timeseries query date is not present
		Then The response status should be 400

	Scenario: Timeseries query for start and end date not present
		Given Vermillion is running
		When Timeseries query start and end date is not present
		Then The response status should be 400
	
	Scenario: Timeseries query for empty date
		Given Vermillion is running
		When Timeseries query date is empty
		Then The response status should be 400
	
	Scenario: Timeseries query for invalid resource id
		Given Vermillion is running
		When Timeseries query resource id is invalid
		Then The response status should be 400
	
	Scenario: Timeseries query for empty resource id
		Given Vermillion is running
		When Timeseries query resource id is empty
		Then The response status should be 400

	Scenario: Timeseries query for start and end date objects not string
		Given Vermillion is running
		When Timeseries query start and end date is not string
		Then The response status should be 400

	Scenario: Timeseries query with just resource id as payload
		Given Vermillion is running
		When Timeseries query has only resource id
		Then The response status should be 400



