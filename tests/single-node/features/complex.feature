Feature: Vermillion is able to handle complex queries

	 Scenario: Complex query with empty payload
                Given Vermillion is running
                When The complex query payload is empty
                Then The response status should be 400

        Scenario: Complex query with invalid payload
                Given Vermillion is running
                When The complex query payload is invalid
                Then The response status should be 400

        Scenario: Complex query with invalid payload id
                Given Vermillion is running
                When The complex query payload id is invalid
                Then The response status should be 400
	
	Scenario: Complex query with just id as payload
                Given Vermillion is running
                When The complex query payload has only id
                Then The response status should be 400


        Scenario: Complex query with empty payload id
                Given Vermillion is running
                When The complex query payload id is empty
                Then The response status should be 400

        Scenario: Complex query with empty payload attributes
                Given Vermillion is running
                When The complex query payload attributes are empty
                Then The response status should be 400


        Scenario: Complex query with invalid payload attributes
                Given Vermillion is running
                When The complex query payload attributes are invalid
                Then The response status should be 400
	
	Scenario: Complex query with empty payload time
                Given Vermillion is running
                When The complex query payload time is empty
                Then The response status should be 400

        Scenario: Complex query with invalid payload time
                Given Vermillion is running
                When The complex query payload time is invalid
                Then The response status should be 400

        Scenario: Complex query with empty payload coordinates
                Given Vermillion is running
                When The complex query payload coordinates are empty
                Then The response status should be 400

        Scenario: Complex query with invalid payload coordinates
                Given Vermillion is running
                When The complex query payload coordinates are invalid
                Then The response status should be 400

        Scenario: Complex query with string payload coordinates
                Given Vermillion is running
                When The complex query payload coordinates are strings
                Then The response status should be 400

        Scenario: Complex query with empty payload distance
                Given Vermillion is running
                When The complex query payload distance is empty
		Then The response status should be 400
	
	Scenario: Complex query with invalid payload distance
                Given Vermillion is running
                When The complex query payload distance is invalid
                Then The response status should be 400

        Scenario: Complex query with payload time not present
                Given Vermillion is running
                When The complex query payload time is not present
                Then The response status should be 400

        Scenario: Complex query with payload distance not present
                Given Vermillion is running
                When The complex query payload distance is not present
                Then The response status should be 400
	
	Scenario: Complex query
                Given Vermillion is running
                When A complex query is initiated
                Then All matching records are returned
                                                            

