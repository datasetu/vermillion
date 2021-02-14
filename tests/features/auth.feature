Feature: Vermillion is able to use a dev version of the auth server

        Scenario: Provider sets rules in the auth server
                Given Vermillion is running
                When The provider sets rules in the auth server
                Then The response status should be 200

        Scenario: Consumer requests for tokens
                Given Vermillion is running
                When The consumer requests for a token
                Then The response status should be 200
		        And The response should contain an auth token

      Scenario: Consumer introspects token
                Given Vermillion is running
                When The consumer provides a valid token
                Then The response status should be 200
                And Introspect should succeed
