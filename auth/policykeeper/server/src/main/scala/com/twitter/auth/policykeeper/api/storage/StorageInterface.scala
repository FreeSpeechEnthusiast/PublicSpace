package com.twitter.auth.policykeeper.api.storage

case class FeatureNotSupported() extends Exception

trait StorageInterface[M, T <: EndpointAssociationData[M, T]]
    extends PolicyStorageInterface
    with ReadOnlyPolicyStorageInterface
    with EndpointAssociationStorageInterface[M, T]
    with ReadOnlyEndpointAssociationStorageInterface[M, T]
