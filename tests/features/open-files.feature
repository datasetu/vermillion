Feature: Vermillion is able to handle static files


        
 	Scenario: Consumer requests for tokens
                Given Vermillion is running
                When The consumer requests token 
		Then The response status should be 201 
                                            
	Scenario: Consumer requests for tokens
                Given Vermillion is running
                When The consumer requests with invalid payload id
                Then The response status should be 500

		
        Scenario: Consumer requests for tokens
                Given Vermillion is running
                When The consumer requests with empty payload id
                Then The response status should be 500
		
	
	Scenario: Consumer requests for tokens
                Given Vermillion is running
                When The consumer requests with invalid payload token
		Then The response status should be 403

 	Scenario: Consumer requests for tokens
                Given Vermillion is running
                When The consumer requests with empty payload token
                Then The response status should be 403
		
	 Scenario: Consumer requests for tokens
                Given Vermillion is running
                When The consumer requests with invalid payload
                Then The response status should be 500

	 Scenario: Consumer requests for tokens
                Given Vermillion is running
                When The consumer requests with empty payload
                Then The response status should be 400

	Scenario: Consumer requests for tokens
                Given Vermillion is running
                When The consumer requests by removing file form parameter 
                Then The response status should be 400

        Scenario: Consumer requests for tokens
                Given Vermillion is running
                When The consumer requests by removing metadata form parameter  
                Then The response status should be 201

	 Scenario: Consumer requests for tokens
                Given Vermillion is running
                When The consumer requests with empty form parameter  
                Then The response status should be 400

		#TODO: Extraneous form paramters.
		#Check if files are getting deleted
