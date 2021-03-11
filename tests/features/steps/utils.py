"""
This error is for cases when the expected status code

does not match the status code from the API

"""
class UnexpectedStatusCodeError(Exception):

    def __init__(self, expected, actual, response_object):
        self.expected = expected
        self.actual = actual
        self.message = "Expecting {0} from API call got {1} with response {2}".format(expected, actual,
                                                                                      str(response_object))
        super().__init__(self.message)

"""
This is for cases when the expected number of data points
 
do not match the received datapoints from the API

"""
class ResponseCountMismatchError(Exception):

    def __init__(self, expected, actual):
        self.expected = expected
        self.actual = actual
        self.message = "Expecting {0} responses from API call got {1}".format(expected, actual)
        super().__init__(self.message)


"""
This error is for all kinds of unexpected behaviour of vermillion
 
The exact error should be described in the message
 
"""
class UnexpectedBehaviourError(Exception):

    def __init__(self, message):
        self.message = message
        super().__init__(self.message)
