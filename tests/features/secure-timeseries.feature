Feature: Vermillion is able to handle secure timeseries datasets

        Background: Permissions setup
	    Given All permissions are set up

        Scenario: Unauthorised ID - single
	    """
	    Consumer requests for a standalone unauthorised ID
	    """
            Given Vermillion is running
            When The consumer requests for an unauthorised ID
            Then The response status should be 403

        Scenario: Unauthorised ID - multiple
	    """
	    Consumer requests for multiple unauthorised IDs
	    """
            Given Vermillion is running
            When The consumer requests for multiple unauthorised IDs
            Then The response status should be 403

        Scenario: Unauthorised ID - mixed
	    """
	    Consumer requests for some authorised and some unauthorised IDs
	    """
            Given Vermillion is running
            When The consumer requests for unauthorised IDs among authorised IDs
            Then The response status should be 403

        Scenario: Authorised ID - single
	    """
	    Consumer requests for a standalone authorised ID
	    """
            Given Vermillion is running
            When The consumer requests for a standalone authorised ID
            Then The response status should be 200
	    And The response should contain the secure timeseries data

        Scenario: Authorised ID - multiple
	    """
	    Consumer requests for multiple authorised IDs
	    """
            Given Vermillion is running
            When The consumer requests for multiple authorised IDs
            Then The response status should be 200
	    And The response should contain the secure timeseries data

