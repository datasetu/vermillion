Feature: Vermillion is able to handle complex queries

	Scenario: Complex query with empty body
                Given Vermillion is running
                When The complex query body is empty
                Then The response status should be 400

        Scenario: Complex query with invalid body
                Given Vermillion is running
                When The complex query body is invalid
                Then The response status should be 400

        Scenario: Complex query with invalid resource id
                Given Vermillion is running
                When The complex query resource id is invalid
                Then The response status should be 400
	
	Scenario: Complex query with just resource id as payload
                Given Vermillion is running
                When The complex query has only resource id
                Then The response status should be 400


        Scenario: Complex query with empty resource id
                Given Vermillion is running
                When The complex query resource id is empty
                Then The response status should be 400

        Scenario: Complex query with empty attributes
                Given Vermillion is running
                When The complex query attributes are empty
                Then The response status should be 400


        Scenario: Complex query with invalid attributes
                Given Vermillion is running
                When The complex query attributes are invalid
                Then The response status should be 400
	
	Scenario: Complex query with empty time
                Given Vermillion is running
                When The complex query time is empty
                Then The response status should be 400

        Scenario: Complex query with invalid time
                Given Vermillion is running
                When The complex query time is invalid
                Then The response status should be 400

        Scenario: Complex query with empty coordinates
                Given Vermillion is running
                When The complex query coordinates are empty
                Then The response status should be 400

        Scenario: Complex query with invalid coordinates
                Given Vermillion is running
                When The complex query coordinates are invalid
                Then The response status should be 400

        Scenario: Complex query with string coordinates
                Given Vermillion is running
                When The complex query coordinates are strings
                Then The response status should be 400

        Scenario: Complex query with empty distance
                Given Vermillion is running
                When The complex query distance is empty
		Then The response status should be 400
	
	Scenario: Complex query with invalid distance
                Given Vermillion is running
                When The complex query distance is invalid
                Then The response status should be 400

        Scenario: Complex query with time not present
                Given Vermillion is running
                When The complex query time is not present
                Then The response status should be 400

        Scenario: Complex query with distance not present
                Given Vermillion is running
                When The complex query distance is not present
                Then The response status should be 400
	
	Scenario: Complex query
                Given Vermillion is running
                When A complex query is initiated
                Then All matching records are returned
                                                            

