class UnexpectedStatusCodeError(Exception):

    def __init__(self, expected, actual, response_object):
        self.expected = expected
        self.actual = actual
        self.message = "Expecting {0} from API call got {1} with response {2}".format(expected, actual, str(response_object))
        super().__init__(self.message)

class ResponseCountMismatchError(Exception):

    def __init__(self, expected, actual):
        self.expected = expected
        self.actual = actual
        self.message = "Expecting {0} responses from API call got {1}".format(expected, actual)
        super().__init__(self.message)
