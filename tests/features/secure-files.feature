Feature: Vermillion is able to handle secure files


       Scenario: Consumer publishes secure file data with a valid token
         Given Vermillion is running
         When The consumer publishes secure file with a valid token
         Then The response status should be 201
         And The file gets uploaded in the provider secure directory

        Scenario: Consumer publishes with valid token
          Given Vermillion is running
          When The consumer publishes with a valid token(1)
          Then The response status should be 201

        Scenario: Consumer publishes with valid token
          Given Vermillion is running
          When The consumer publishes with a valid token(2)
          Then The response status should be 201

        Scenario: Consumer publishes with valid token
          Given Vermillion is running
          When The consumer publishes with a valid token(3)
          Then The response status should be 201

        Scenario: Consumer publishes secure file with file and timeseries data
          Given Vermillion is running
          When The consumer publishes secure file with a file and timeseries data
          Then The response status should be 400

        Scenario: Download secure-file with multiple resource ids and a token
          Given Vermillion is running
          When The consumer downloads file by passing multiple resource ids and a token
          Then The response status should be 200
          And The file gets uploaded in the consumer directory

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

          Scenario: Download secure-file with valid reroute link
                Given Vermillion is running
                 When The consumer downloads file by passing a valid reroute link
                Then The response status should be 200

          Scenario: Download secure-file with only token for single auth id
            Given Vermillion is running
            When The consumer downloads file by passing only token for single auth id
            Then The response status should be 200

          Scenario: Download secure-file with valid reroute link for single authorised id
                  Given Vermillion is running
                  When The consumer downloads file by passing a valid reroute link for single authorised id
                  Then The response status should be 200

          Scenario: Download secure-file with only token
                  Given Vermillion is running
                  When The consumer downloads file by passing only token
                  Then The response status should be 200

        Scenario: Download secure-file with invalid token
                Given Vermillion is running
                When The consumer downloads file by passing an invalid token
                Then The response status should be 403

        Scenario: Download secure-file with empty token
                Given Vermillion is running
                When The consumer downloads file by passing an empty token
                Then The response status should be 403

        Scenario: Download secure-file without passing token
                Given Vermillion is running
                When The consumer downloads file without passing token
                 Then The response status should be 400

        Scenario: Download secure-file with invalid resource id
                Given Vermillion is running
                When The consumer downloads file by passing an invalid resource id
                Then The response status should be 400

        Scenario: Download secure-file with empty resource id
                Given Vermillion is running
                When The consumer downloads file by passing an empty resource id
		        Then The response status should be 400

        Scenario: Download file with public id and token
          Given Vermillion is running
          When The consumer downloads file by passing public id and token
          Then The response status should be 400

        Scenario: Download secure-file with only token and requested id is not present
          Given Vermillion is running
          When The consumer downloads file by passing only token and requested id is not present
          Then The response status should be 403

        Scenario: Download secure-file with id,token and requested id is not present
          Given Vermillion is running
          When The consumer downloads file by passing id,token and requested id is not present
          Then The response status should be 404

#        Scenario: Consumer publishes with valid token and could not move files
#          Given Vermillion is running
#          When The consumer publishes with a valid token and could not move files
#          Then The response status should be 500
#          And  The file permission is reset

