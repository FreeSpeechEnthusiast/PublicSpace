namespace java com.twitter.auth.thrift.tamperproofing.paseto
namespace py gen.twitter.auth.thrift.tamperproofing.paseto
namespace rb AuthThriftTamperProofingPaseto
namespace go auth.tamperproofing.paseto
#@namespace scala com.twitter.auth.tamperproofing.paseto.thriftscala
#@namespace strato com.twitter.auth.tamperproofing.paseto

struct PasetoMetadata {

  // paseto token string
  1: string pasetoSignature

  // The serialization algorithm used to convert data to a string
  // before feeding into the paseto library to generate the token string
  2: string dataSerializationAlgorithm

  // The private key used to generate the paseto token
  3: i32 pasetoEncryptionKeyVersion

  // The hash algorithm used to condense the data before generating the token string
  4: optional string pasetoHashVersion
}
