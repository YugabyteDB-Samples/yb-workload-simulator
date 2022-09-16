package com.yugabyte.simulation.service;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class LoadGeneratorUtils {
	
	private static final String DIGITS = "1234567890";
	private static final String HEX_DIGITS = "1234567890ABCDEF";
	private static final String ALPHA_DIGITS = "1234567890QWERTYUIOPASDFGHJKLZXCVBNM";
	
	public static UUID getUUID() {
		return UUID.randomUUID();
	}
	
	public static String getName() {
		return NameGenerator.getName();
	}
	
	public static String getText(int minLength, int maxLength) {
		return TextGenerator.getText(minLength, maxLength);
	}
	
	public static int getInt(int min, int max) {
		if (min >= max) {
			return min;
		}
		Random random = ThreadLocalRandom.current();
		return min + random.nextInt(max - min);
	}

	public static double getDouble() {
		return ThreadLocalRandom.current().nextDouble();
	}
	
	public static double getDouble(double min, double max) {
		return ThreadLocalRandom.current().nextDouble(min, max);
	}
	
	public static long getLong(long min, long max) {
		if (min >= max) {
			return min;
		}
		Random random = ThreadLocalRandom.current();
		return min + (random.nextLong() % (max-min));
	}

	public static String getFixedLengthNumber(int length) {
		return getFixedLengthString(DIGITS, length);
	}
	
	public static String getHexString(int length) {
		return getFixedLengthString(HEX_DIGITS, length);
	}
	
	public static String getAlphaString(int length) {
		return getFixedLengthString(ALPHA_DIGITS, length);
	}
	
	private static String getFixedLengthString(String alphabet, int length) {
		StringBuffer buffer = new StringBuffer(length);
		Random random = ThreadLocalRandom.current();
		for (int i = 0;i < length; i++) {
			buffer.append(alphabet.charAt(random.nextInt(alphabet.length())));
		}
		return buffer.toString();
	}

	public static byte[] getBinaryDataOfFixedSize(int sizeInBytes){
		if(sizeInBytes <= 0){
			return null;
		}
		byte[] bytes = new byte[sizeInBytes];
		Random random = ThreadLocalRandom.current();
		random.nextBytes(bytes);
		return bytes;
	}
}
