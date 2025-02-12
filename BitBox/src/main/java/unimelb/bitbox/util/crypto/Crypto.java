package unimelb.bitbox.util.crypto;

import functional.algebraic.Result;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.jetbrains.annotations.NotNull;
import unimelb.bitbox.util.concurrency.LazyInitialiser;
import unimelb.bitbox.util.network.JSONDocument;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.*;
import java.util.Base64;

/**
 * Contains utility methods for working with the cryptography library.
 *
 * @author Eleanor McMurtry
 */
// We need to be specific about the types of keys we accept
@SuppressWarnings("TypeMayBeWeakened")
public class Crypto {
    private static final int AES_KEY_BITS = 128;
    private static final int AES_KEY_BYTES = AES_KEY_BITS / 8;
    private static final int RSA_KEY_BYTES = 256;

    private Crypto() {}

    public static SecretKey parseKey(String solution) {
        return new SecretKeySpec(Base64.getDecoder().decode(solution), "AES");
    }

    public static Result<PrivateKey, IOException> getRSAPrivateKey(String filename) {
        return Result.of(() -> {
            Security.addProvider(new BouncyCastleProvider());
            PEMParser pemParser = new PEMParser(new FileReader(new File(filename)));
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
            KeyPair kp = converter.getKeyPair((PEMKeyPair) pemParser.readObject());
            return kp.getPrivate();
        });
    }

    /**
     * Generates a secret AES key.
     */
    public static SecretKey generateSecretKey() {
        try {
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(AES_KEY_BITS);
            return generator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static final LazyInitialiser<SecureRandom> rand = new LazyInitialiser<>(SecureRandom::new);

    /**
     * Encrypts the provided secret key with the provided public key, and returns a base-64 encoding of the ciphertext.
     */
    public static Result<String, CryptoException> encryptSecretKey(SecretKey secretKey, PublicKey publicKey) {
        try {
            Cipher encipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            encipher.init(Cipher.PUBLIC_KEY, publicKey);
            byte[] encrypted = encipher.doFinal(secretKey.getEncoded());
            return Result.value(new String(Base64.getEncoder().encode(encrypted)));
        } catch (NoSuchAlgorithmException e) {
            return Result.error(new CryptoException(e));
        } catch (InvalidKeyException e) {
            return Result.error(new CryptoException(e));
        } catch (NoSuchPaddingException e) {
            return Result.error(new CryptoException(e));
        } catch (BadPaddingException e) {
            return Result.error(new CryptoException(e));
        } catch (IllegalBlockSizeException e) {
            return Result.error(new CryptoException(e));
        }
    }

    /**
     * Decrypts the received secret key using the provided private key.
     * See {@link Crypto#encryptSecretKey} for details on the quirks involved.
     *
     * @param privateKey the private key to use for decryption
     * @return the decrypted secret key
     */
    public static Result<SecretKey, CryptoException> decryptSecretKey(@NotNull byte[] key, @NotNull PrivateKey privateKey) {
        try {
            Cipher decipher = Cipher.getInstance("RSA/ECB/NoPadding");
            decipher.init(Cipher.PRIVATE_KEY, privateKey);
            byte[] decrypted = decipher.doFinal(key);
            // We have a leading 0 byte, so we ignore the first index
            return Result.value(new SecretKeySpec(decrypted,Crypto.RSA_KEY_BYTES - Crypto.AES_KEY_BYTES, Crypto.AES_KEY_BYTES, "AES"));
        } catch (NoSuchAlgorithmException e) {
            return Result.error(new CryptoException(e));
        } catch (InvalidKeyException e) {
            return Result.error(new CryptoException(e));
        } catch (NoSuchPaddingException e) {
            return Result.error(new CryptoException(e));
        } catch (BadPaddingException e) {
            return Result.error(new CryptoException(e));
        } catch (IllegalBlockSizeException e) {
            return Result.error(new CryptoException(e));
        }
    }

    public static Result<SecretKey, CryptoException> decryptSecretKey(@NotNull String key, @NotNull PrivateKey privateKey) {
        return decryptSecretKey(Base64.getDecoder().decode(key), privateKey);
    }

    /**
     * Decrypts a received message of the form {"payload":"CIPHERTEXT"}.
     * Returns the decrypted ciphertext.
     */
    public static Result<JSONDocument, CryptoException> decryptMessage(SecretKey secretKey, JSONDocument message){
        return message.getString("payload")
               .matchThen(payload -> {
                              try {
                                  Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
                                  cipher.init(Cipher.DECRYPT_MODE, secretKey);
                                  String result = new String(cipher.doFinal(Base64.getDecoder().decode(payload)));
                                  // Padding comes after a newline character
                                  result = result.split("\n")[0];
                                  return JSONDocument.parse(result).mapError(CryptoException::new);
                              } catch (NoSuchAlgorithmException e) {
                                  return Result.error(new CryptoException(e));
                              } catch (InvalidKeyException e) {
                                  return Result.error(new CryptoException(e));
                              } catch (NoSuchPaddingException e) {
                                  return Result.error(new CryptoException(e));
                              } catch (BadPaddingException e) {
                                  return Result.error(new CryptoException(e));
                              } catch (IllegalBlockSizeException e) {
                                  return Result.error(new CryptoException(e));
                              }
                          }, err -> Result.error(new CryptoException(err)));
    }

    /**
     * Returns a cryptographically secure random number between min and max (inclusive)
     * @param min the minimum value to return
     * @param max the maximum value to return
     * @return the generated number
     */
    public static int cryptoRandRange(int min, int max) {
        // + 1 because nextInt(bound) is exclusive
        return min + rand.get().nextInt(max - min + 1);
    }

    /**
     * Encrypts a prepared message that has been networkEncoded in JSON.
     * Returns a JSON message ready to be sent of the form {"payload":"CIPHERTEXT"}.
     */
    public static Result<JSONDocument, CryptoException> encryptMessage(SecretKey secretKey, JSONDocument message) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            // Pad the message (if necessary)
            StringBuilder paddedMessage = new StringBuilder(message + "\n");
            // The number of bytes we need is 16 minus the remainder, so that we end up with a multiple of 16 total
            int requiredBytes = AES_KEY_BYTES - paddedMessage.length() % AES_KEY_BYTES;
            if (requiredBytes < AES_KEY_BYTES) {
                for (int i = 0; i < requiredBytes; ++i) {
                    // Printable character range is 32-126. Trust me on this.
                    @SuppressWarnings("MagicNumber")
                    char next = (char) cryptoRandRange(32, 126);

                    // Crap, we can't use a quote or a backslash for JSON.
                    // Just try again if this happens
                    if (next == '"' || next == '\\') {
                        ++requiredBytes;
                    } else {
                        paddedMessage.append(next);
                    }
                }
            }
            byte[] encryptedBytes = cipher.doFinal(paddedMessage.toString().getBytes());

            JSONDocument encrypted = new JSONDocument();
            encrypted.append("payload", Base64.getEncoder().encodeToString(encryptedBytes));
            return Result.value(encrypted);
        } catch (NoSuchAlgorithmException e) {
            return Result.error(new CryptoException(e));
        } catch (InvalidKeyException e) {
            return Result.error(new CryptoException(e));
        } catch (NoSuchPaddingException e) {
            return Result.error(new CryptoException(e));
        } catch (BadPaddingException e) {
            return Result.error(new CryptoException(e));
        } catch (IllegalBlockSizeException e) {
            return Result.error(new CryptoException(e));
        }
    }
}
