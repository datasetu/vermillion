Feature: Vermillion is able to handle secure files


	 Scenario: Consumer requests having a valid token
                Given Vermillion is running
                When The consumer requests having a valid token
		Then The response status should be 201

 	Scenario: Consumer requests having invalid token
                Given Vermillion is running
                When The consumer requests having an invalid token
		Then The response status should be 403

 	Scenario: Consumer requests having empty token
                Given Vermillion is running
                When The consumer requests having an empty token
		Then The response status should be 403

 	Scenario: Consumer requests having invalid id
                Given Vermillion is running
                When The consumer requests having an invalid id
		Then The response status should be 403

 	Scenario: Consumer requests having empty id
                Given Vermillion is running
                When The consumer requests having an empty id
		Then The response status should be 500

 	
