Feature: Vermillion is able to handle attribute queries

        Scenario: Attribute value query with empty payload
                Given Vermillion is running
                When The attribute value query payload is empty
                Then The response status should be 400

        Scenario: Attribute value query with invalid payload
                Given Vermillion is running
                When The attribute value query payload is invalid
                Then The response status should be 400

        Scenario: Attribute value query with invalid payload id
                Given Vermillion is running
                When The attribute value query payload id is invalid
                Then The response status should be 400
	
	Scenario: Attribute value query with just id as payload
                Given Vermillion is running
                When The attribute value query payload has only id
                Then The response status should be 400


        Scenario: Attribute value query with empty payload id
                Given Vermillion is running
                When The attribute value query payload id is empty
                Then The response status should be 400

        Scenario: Attribute value query with invalid payload attribute
                Given Vermillion is running
                When The attribute value query payload attributes are invalid
                Then The response status should be 400

	Scenario: Attribute value query with empty payload attribute
                Given Vermillion is running
                When The attribute value query payload attributes are empty
                Then The response status should be 400

        Scenario: Attribute value query
                Given Vermillion is running
                When An attribute value query is initiated
                Then All matching records are returned

