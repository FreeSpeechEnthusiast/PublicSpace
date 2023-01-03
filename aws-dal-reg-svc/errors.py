class ExceptionWithValue(Exception):
  def __init__(self, value):
    self.value = value


class AwsEnvCredRequired(ExceptionWithValue):
  pass


class AwsInvalidArn(ExceptionWithValue):
  pass


class AwsInvalidIamEntityType(ExceptionWithValue):
  pass


class AwsRegionRequired(ExceptionWithValue):
  pass


class MisconfiguredResourceScanner(Exception):
  def __init__(self, attribute: str, class_name: str):
    self.attribute = attribute
    self.class_name = class_name


class RegistrationError(ExceptionWithValue):
  pass


class AwsMissingTTL(Exception):
  pass


class UnknownResourceStatus(Exception):
  def __init__(self, resource_type: str, status: str):
    self.resource_type = resource_type
    self.status = status
