package com.twitter.auth.pasetoheaders.encryption;

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;

import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Test;

import static org.junit.Assert.assertNotEquals;

/**
 * This class is designed to simplify key pair generation
 */
public class KeyGenTest extends Stubs  {

  private String base16(byte[] bytes) {
    return new BigInteger(1, bytes).toString(16);
  }

  @Test
  public void testGenerateKeyPair() throws NoSuchAlgorithmException,
      NoSuchProviderException,
      IOException {

    Security.addProvider(new BouncyCastleProvider());
    KeyPairGenerator keyGen =
        KeyPairGenerator.getInstance("Ed25519", "BC");
    KeyPair pair = keyGen.generateKeyPair();
    PrivateKey priv = pair.getPrivate();
    PublicKey pub = pair.getPublic();

    String privateKeyData =
        PrivateKeyInfo
            .getInstance(
                ASN1Sequence.getInstance(priv.getEncoded()))
            .parsePrivateKey().toString().substring(1);

    String publicKeyData = base16(SubjectPublicKeyInfo
        .getInstance(pub.getEncoded()).getPublicKeyData().getBytes());

    System.out.println("Private key generated: " + privateKeyData);
    System.out.println("Public key generated: " + publicKeyData);

    assertNotEquals(privateKeyData, "");
    assertNotEquals(publicKeyData, "");

  }

}
