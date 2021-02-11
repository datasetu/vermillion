Feature: Vermillion is able to handle secure files


	 Scenario: Consumer publishes along with a valid token
                Given Vermillion is running
                When The consumer publishes along with a valid token
		Then The response status should be 201

 	Scenario: Consumer publishes along with invalid token
                Given Vermillion is running
                When The consumer publishes along with an invalid token
		Then The response status should be 403

 	Scenario: Consumer publishes along with an empty token
                Given Vermillion is running
                When The consumer publishes along with an empty token
		Then The response status should be 403

 	Scenario: Consumer publishes along with invalid resource id
                Given Vermillion is running
                When The consumer publishes along with an invalid resource id
		Then The response status should be 403

	# TODO: Look into the cause
	# TODO: Check download with only token
	# TODO: Check download with many ids and token
	# TODO: Check download with one id and token
	# TODO: Test publish API with both file and ts data
 	Scenario: Consumer publishes along with empty resource id
                Given Vermillion is running
                When The consumer publishes along with an empty resource id
		Then The response status should be 403

          Scenario: Download secure-file with valid token
                Given Vermillion is running
                When The consumer downloads file by passing a valid token
                Then The response status should be 200

        Scenario: Download secure-file with invalid token
                Given Vermillion is running
                When The consumer downloads file by passing an invalid token
                Then The response status should be 403

        Scenario: Download secure-file with empty token
                Given Vermillion is running
                When The consumer downloads file by passing an empty token
                Then The response status should be 403

        Scenario: Download secure-file with invalid resource id
                Given Vermillion is running
                When The consumer downloads file by passing an invalid resource id
                Then The response status should be 403

        Scenario: Download secure-file with empty resource id
                Given Vermillion is running
                When The consumer downloads file by passing an empty resource id
                Then The response status should be 403	
