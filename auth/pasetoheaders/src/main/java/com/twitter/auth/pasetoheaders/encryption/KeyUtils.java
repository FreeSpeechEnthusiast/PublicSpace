package com.twitter.auth.pasetoheaders.encryption;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.edec.EdECObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public final class KeyUtils {

  private KeyUtils() {
  }

  /**
   * Generates a byte array key from base16 encoded (hex) key
   *
   * @param base16key
   * @return string key
   */
  public static byte[] decodeHex(String base16key) {
    byte[] byteArray = new BigInteger(base16key, 16)
        .toByteArray();
    if (byteArray[0] == 0) {
      byte[] output = new byte[byteArray.length - 1];
      System.arraycopy(
          byteArray, 1, output,
          0, output.length);
      return output;
    }
    return byteArray;
  }

  /**
   * Generate a java.security.PrivateKey from a hex encoded private
   * key. You should store it in a secure location. For the example
   * the keys are directly hardcoded in the code
   *
   * @return java.security.PrivateKey
   */
  public static PrivateKey getPrivateKey(String privateKey) {
    Security.addProvider(new BouncyCastleProvider());
    byte[] pvKey = KeyUtils.decodeHex(privateKey);
    if (pvKey.length != Ed25519PrivateKeyParameters.KEY_SIZE) {
      throw new Error("Private key size should be strictly "
          + Ed25519PrivateKeyParameters.KEY_SIZE + " bytes");
    }
    try {
      KeyFactory keyFact = KeyFactory.getInstance("Ed25519");
      PrivateKeyInfo privateKeyInfo = new PrivateKeyInfo(
          new AlgorithmIdentifier(EdECObjectIdentifiers.id_Ed25519),  new DEROctetString(
          pvKey));
      PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(privateKeyInfo.getEncoded());
      return keyFact.generatePrivate(pkcs8KeySpec);
    } catch (NoSuchAlgorithmException | InvalidKeySpecException | IOException e) {
      throw new Error(e);
    }
  }

  /**
   * Generate a java.security.PublicKey from a hex encoded private
   * key. You should store it in a secure location. For the example
   * the keys are directly hardcoded in the code
   *
   * @return java.security.PublicKey
   */
  public static PublicKey getPublicKey(String publicKey) {
    Security.addProvider(new BouncyCastleProvider());
    byte[] pbKey = KeyUtils.decodeHex(publicKey);
    if (pbKey.length != Ed25519PublicKeyParameters.KEY_SIZE) {
      throw new Error("Public key size should be strictly "
          + Ed25519PublicKeyParameters.KEY_SIZE + " bytes");
    }
    try {
      KeyFactory keyFact = KeyFactory.getInstance("Ed25519");
      SubjectPublicKeyInfo publicKeyInfo = new SubjectPublicKeyInfo(
          new AlgorithmIdentifier(EdECObjectIdentifiers.id_Ed25519), pbKey);
      X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(publicKeyInfo.getEncoded());
      return keyFact.generatePublic(x509EncodedKeySpec);
    } catch (NoSuchAlgorithmException | InvalidKeySpecException | IOException e) {
      throw new Error(e);
    }
  }
}
