from decimal import Decimal

from dataset_metastore import should_set_init_field


def test_should_set_init_field():
  expected = True
  actual = should_set_init_field(
    'observed',
    {'ID': 'dynamodb/dev-chatdb-room-index'})
  assert expected == actual

  expected = True
  actual = should_set_init_field(
    'registered',
    {
      'ID': 'dynamodb/dev-chatdb-room-index',
      'observed': Decimal('1623402707')
    })
  assert expected == actual

  expected = False
  actual = should_set_init_field(
    'observed',
    {
      'ID': 'dynamodb/dev-chatdb-room-index',
      'registered': Decimal('1623402710'),
      'observed': Decimal('1623402707')
    })
  assert expected == actual

  expected = False
  actual = should_set_init_field(
    'observed',
    {
      'ID': 'dynamodb/dev-chatdb-room-index',
      'init_registered': Decimal('1623402710')
    })
  assert expected == actual

  expected = False
  actual = should_set_init_field(
    'registered',
    {
      'ID': 'dynamodb/dev-chatdb-room-index',
      'registered': Decimal('1623402710'),
      'observed': Decimal('1623402707')
    })
  assert expected == actual
