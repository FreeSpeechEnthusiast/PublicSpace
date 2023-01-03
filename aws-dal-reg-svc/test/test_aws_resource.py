from dataset_attribution import resource_owner


def test_resource_owner():
  # included
  expected = "CLOUDACCEL"
  actual = resource_owner(
      account_id="673964658973",
      name="dev-container-registrar-a",
      properties={})
  assert expected == actual

  # No match, default
  expected = "PSCP"
  actual = resource_owner(account_id="673964658973", name="random", properties={})
  assert expected == actual

  # excluded
  expected = "CLOUDACCEL"
  actual = resource_owner(account_id="673964658973", name="hydra-registrar", properties={})
  assert expected == actual

  expected = "TAG"
  actual = resource_owner(
    account_id="673964658973",
    name="test",
    properties={
      "Tags": [
        {
          "Key": "pdp_team",
          "Value": "tag",
        },
      ]
    })
  assert expected == actual
