import boto3
from botocore.stub import Stubber
import pytest
from server_config import GLOBAL_API_REGION


class MockAuthenticator:
  def __init__(self, service, region_name):
    self.client = boto3.client(service, region_name=region_name)
    self.stubber = Stubber(self.client)

  def new_client(self, service: str, region: str):
    return self.client

  def add_response(self, request: str, response: dict, params: dict):
    self.stubber.add_response(request, response, params)

  def activate(self):
    self.stubber.activate()


@pytest.fixture
def mock_org_auth():
  return MockAuthenticator("organizations", GLOBAL_API_REGION)
