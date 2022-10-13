package com.yugabyte.simulation.services;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

@Service
public class CryptoService {
	public String generateIvAsString() {
		byte[] iv = new byte[16];
		new SecureRandom().nextBytes(iv);
		return Base64.getEncoder().encodeToString(iv);
	}

	public IvParameterSpec getIvParameterSpec(String ivAsString) {
		return new IvParameterSpec(Base64.getDecoder().decode(ivAsString));
	}

	public IvParameterSpec generateIv() {
		byte[] iv = new byte[16];
		new SecureRandom().nextBytes(iv);
		return new IvParameterSpec(iv);
	}

	public SecretKey getKeyFromPassword(String password, String salt)
		throws NoSuchAlgorithmException, InvalidKeySpecException {

		SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
		KeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), 65536, 256);
		SecretKey secret = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
		return secret;
	}

	public String encryptPasswordBased(String plainText, SecretKey key, IvParameterSpec iv)
			throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
				InvalidKeyException, BadPaddingException, IllegalBlockSizeException {

		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, key, iv);
		return Base64.getEncoder().encodeToString(cipher.doFinal(plainText.getBytes()));
	}

	public String decryptPasswordBased(String cipherText, SecretKey key, IvParameterSpec iv)
			throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
				InvalidKeyException, BadPaddingException, IllegalBlockSizeException {

		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
		cipher.init(Cipher.DECRYPT_MODE, key, iv);
		return new String(cipher.doFinal(Base64.getDecoder().decode(cipherText)));
	}

	public void givenPassword_whenEncrypt_thenSuccess()
			throws InvalidKeySpecException, NoSuchAlgorithmException,
				IllegalBlockSizeException, InvalidKeyException, BadPaddingException,
				InvalidAlgorithmParameterException, NoSuchPaddingException {

		String plainText = "www.baeldung.com";
		String password = "baeldung";
		String salt = "12345678";
		IvParameterSpec ivParameterSpec = generateIv();
		SecretKey key = getKeyFromPassword(password,salt);
		String cipherText = encryptPasswordBased(plainText, key, ivParameterSpec);
		String decryptedCipherText = decryptPasswordBased(
				cipherText, key, ivParameterSpec);
		System.out.printf("plain text = %s, cipherText = %s, decryptedCipherText = %s", plainText, cipherText, decryptedCipherText);
	}

}
