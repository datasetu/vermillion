Feature: Vermillion is able to handle static files


        
	Scenario: Consumer requests with valid token
		Given Vermillion is running
		When The consumer requests with a valid token
		Then The response status should be 201

	Scenario: Consumer requests with invalid payload id
                Given Vermillion is running
                When The consumer requests with invalid payload id
                Then The response status should be 403

		
        Scenario: Consumer requests with empty payload id
                Given Vermillion is running
                When The consumer requests with empty payload id
                Then The response status should be 500
		
	
	Scenario: Consumer requests with invalid token
                Given Vermillion is running
                When The consumer requests with invalid payload token
		Then The response status should be 403

 	Scenario: Consumer requests with empty token
                Given Vermillion is running
                When The consumer requests with empty payload token
                Then The response status should be 403
		
	Scenario: Consumer requests by removing file form parameter
                Given Vermillion is running
                When The consumer requests by removing file form parameter 
                Then The response status should be 400

        Scenario: Consumer requests by removing metadata form parameter
                Given Vermillion is running
                When The consumer requests by removing metadata form parameter  
                Then The response status should be 201
	
	
	Scenario: Consumer requests by using extraneous form parameter
                Given Vermillion is running
                When The consumer requests by using extraneous form parameter
                Then The response status should be 400
	

		
	 Scenario: Consumer requests with empty form parameter
                Given Vermillion is running
                When The consumer requests with empty form parameter  
                Then The response status should be 400

       
