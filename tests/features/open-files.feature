Feature: Vermillion is able to handle static files


        
	Scenario: Consumer publishes with valid token
		      Given Vermillion is running
		      When The consumer publishes with a valid token
		      Then The response status should be 201



    Scenario: Consumer publishes without resource id
            Given Vermillion is running
            When The consumer publishes without resource id
            Then The response status should be 400

  Scenario: Consumer publishes without token
           Given Vermillion is running
            When The consumer publishes without token
            Then The response status should be 400

	Scenario: Consumer publishes with invalid resource id
                Given Vermillion is running
                When The consumer publishes with invalid resource id
                Then The response status should be 403

		
        Scenario: Consumer publishes with empty resource id
                Given Vermillion is running
                When The consumer publishes with empty resource id
                Then The response status should be 403
		
	
	Scenario: Consumer publishes with invalid token
                Given Vermillion is running
                When The consumer publishes with invalid token
		Then The response status should be 403

 	Scenario: Consumer publishes with empty token
                Given Vermillion is running
                When The consumer publishes with empty token
                Then The response status should be 403
		
	Scenario: Consumer publishes by removing file form parameter
                Given Vermillion is running
                When The consumer publishes by removing file form parameter 
                Then The response status should be 400

     Scenario: Consumer publishes with invalid json meta file
        Given Vermillion is running
         When The consumer publishes with invalid json meta file
          Then The response status should be 400

        Scenario: Consumer publishes by removing metadata form parameter
                Given Vermillion is running
                When The consumer publishes by removing metadata form parameter  
                Then The response status should be 201
	
	
	Scenario: Consumer publishes by using extraneous form parameter
                Given Vermillion is running
                When The consumer publishes by using extraneous form parameter
		        Then The response status should be 400
		        And The uploaded files are deleted
	

	 Scenario: Consumer publishes with empty form parameter
                Given Vermillion is running
                When The consumer publishes with empty form parameter  
                Then The response status should be 400

        Scenario: Consumer publishes with more than 2 form parameters
                Given Vermillion is running
                When The consumer publishes with more than 2 form parameters
                Then The response status should be 400
	
	Scenario: Consumer downloads the file
                Given Vermillion is running
                When The consumer downloads the file
                Then The response status should be 200
		        And The expected file is returned


  Scenario: Consumer publishes with more than 2 form parameters
    Given Vermillion is running
    When The consumer publishes with more than 2 form parameters-1
    Then The response status should be 400

  Scenario: Consumer publishes with form parameter other than file
    Given Vermillion is running
    When The consumer publishes with a valid and invalid form parameter
    Then The response status should be 201