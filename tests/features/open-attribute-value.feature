Feature: Vermillion is able to handle attribute queries

        Scenario: Attribute value query with empty body
                Given Vermillion is running
                When The attribute value query body is empty
                Then The response status should be 400

        Scenario: Attribute value query with invalid body
                Given Vermillion is running
                When The attribute value query body is invalid
                Then The response status should be 400

        Scenario: Attribute value query with invalid resource id
                Given Vermillion is running
                When The attribute value query resource id is invalid
                Then The response status should be 400
	
	Scenario: Attribute value query with just resource id as payload
                Given Vermillion is running
                When The attribute value query payload has only resource id
                Then The response status should be 400


        Scenario: Attribute value query with empty resource id
                Given Vermillion is running
                When The attribute value query resource id is empty
                Then The response status should be 400

        Scenario: Attribute value query with invalid attribute
                Given Vermillion is running
                When The attribute value query attributes are invalid
                Then The response status should be 400

	Scenario: Attribute value query with empty attribute
                Given Vermillion is running
                When The attribute value query attributes are empty
                Then The response status should be 400

        Scenario: Attribute value query
                Given Vermillion is running
                When An attribute value query is initiated
                Then All matching records are returned

