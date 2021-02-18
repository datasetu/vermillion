Feature: VermillhFeature: Vermillion is able to handle secure files


	 Scenario: Consumer publishes secure file data with a valid token
                Given Vermillion is running
                When The consumer publishes secure file with a valid token
		        Then The response status should be 201
	
	Scenario: Consumer publishes secure file with file and timeseries data
                Given Vermillion is running
                When The consumer publishes secure file with a file and timeseries data
                Then The response status should be 400

 	Scenario: Consumer publishes secure file with invalid token
                Given Vermillion is running
                When The consumer publishes secure file with an invalid token
		        Then The response status should be 403

 	Scenario: Consumer publishes secure file with an empty token
                Given Vermillion is running
                When The consumer publishes secure file with an empty token
		        Then The response status should be 403

 	Scenario: Consumer publishes secure file with invalid resource id
                Given Vermillion is running
                When The consumer publishes secure file with an invalid resource id
		        Then The response status should be 403

 	Scenario: Consumer publishes secure file with empty resource id
                Given Vermillion is running
                When The consumer publishes secure file with an empty resource id
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

	Scenario: Download secure-file with only token
                Given Vermillion is running
                When The consumer downloads file by passing only token
                Then The response status should be 403

	Scenario: Download secure-file with multiple resource ids and a token
                Given Vermillion is running
                When The consumer downloads file by passing multiple resource ids and a token
                Then The response status should be 200
