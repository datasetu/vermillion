Feature: VermillhFeature: Vermillion is able to handle secure files


	 Scenario: Consumer publishes along with a valid token
                Given Vermillion is running
                When The consumer publishes along with a valid token
		Then The response status should be 201
	
	Scenario: Consumer publishes along with file and timeseries data
                Given Vermillion is running
                When The consumer publishes along with a file and timeseries data
                Then The response status should be 400

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
		Then The response status should be 400

 	Scenario: Consumer publishes along with empty resource id
                Given Vermillion is running
                When The consumer publishes along with an empty resource id
		Then The response status should be 400

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
