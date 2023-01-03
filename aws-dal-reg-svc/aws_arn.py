from errors import AwsInvalidArn


class Arn:
  NAME_DELIM = '-'

  def __init__(self, arn: str):
    elements = arn.split(':', 5)
    if len(elements) != 6:
      raise AwsInvalidArn(arn)

    self.arn = arn
    self.partition = elements[1]
    self.service = elements[2]
    self.region = elements[3]
    self.account_id = elements[4]
    self.resource = elements[5]
    self.resource_type = None

    for char in ('/', ':'):
      if char in self.resource:
        self.resource_type, self.resource = self.resource.split(char, 1)
        break

  def __str__(self) -> str:
    return '{}(value={})'.format(
      self.__class__.__name__,
      self.arn)

  def dal_name(self) -> str:
    # eagleeye does not support dataset names that include
    # some special characters such as slashes which prevents
    # AWS ARNs from being used directly. A new name is constructed
    # from ARN elements to be used as a replacement.
    elements = [
      self.partition,
      self.account_id,
      self.region,
      self.service,
    ]
    if self.resource_type:
      elements.append(self.resource_type)
    elements.append(self.resource)

    return self.NAME_DELIM.join(elements).replace('/', self.NAME_DELIM)

  def kite_name(self) -> str:
    # CAT-2699 for context
    return self.account_id + '.' + self.NAME_DELIM.join([
        self.region,
        self.resource,
      ])


class S3Arn(Arn):
  """
  S3 ARN IDs use a global format which does not include account ID or region information which is needed
  for common Arn class functionality like generating DAL and Kite names.
  """

  def __init__(self, bucket_name: str, account_id: str, region: str):
    super().__init__('arn:aws:s3:::' + bucket_name)
    self.account_id = account_id
    self.region = region
