Feature: Vermillion is able to handle timeseries queries

    	Scenario: Timeseries query for empty body
		Given Vermillion is running
		When Timeseries query body is empty
		Then The response status should be 400
	
	Scenario: Timeseries query for invalid body
		Given Vermillion is running
		When Timeseries query body is invalid
		Then The response status should be 400

	Scenario: Timeseries query for invalid time json object
		Given Vermillion is running
		When Timeseries query time has invalid json object
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

	Scenario: Timeseries query for start and end date objects are not string
		Given Vermillion is running
		When Timeseries query start and end date is not string
		Then The response status should be 400

	Scenario: Timeseries query for start and end date is an integer
		Given Vermillion is running
		When Timeseries query start and end date is an integer
		Then The response status should be 400

	Scenario: Timeseries query for start date greater than end date
		Given Vermillion is running
		When Timeseries query start date greater than end date
		Then The response status should be 400

	Scenario: Timeseries query for year greater than 9999
		Given Vermillion is running
		When Timeseries query year greater than 9999
		Then The response status should be 400


	Scenario: Timeseries query for month greater than 12
		Given Vermillion is running
		When Timeseries query month greater than 12
		Then The response status should be 400

	Scenario: Timeseries query for day greater than 31
		Given Vermillion is running
		When Timeseries query day greater than 31
		Then The response status should be 400

	Scenario: Timeseries query for date in invalid format
		Given Vermillion is running
		When Timeseries query date in invalid format
		Then The response status should be 400

	Scenario: Timeseries query with just resource id as payload
		Given Vermillion is running
		When Timeseries query has only resource id
		Then The response status should be 400



