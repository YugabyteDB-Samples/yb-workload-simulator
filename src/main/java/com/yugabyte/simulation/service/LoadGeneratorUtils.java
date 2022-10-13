package com.yugabyte.simulation.service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

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
	
	/**
	 * Return a pseudo English sentence whose length is between minLength and
	 * max length, both inclusive.
	 * @param minLength - the shortest string to return (inclusive)
	 * @param maxLength - the longest string to retun (inclusive)
	 * @return
	 */
	public static String getText(int minLength, int maxLength) {
		return TextGenerator.getText(minLength, maxLength);
	}
	
	/** 
	 * Get a number in the range min(inclusive) to max (exclusive)
	 * @param min
	 * @param max
	 * @return
	 */
	public static int getInt(int min, int max) {
		if (min >= max) {
			return min;
		}
		Random random = ThreadLocalRandom.current();
		return min + random.nextInt(max - min);
	}

	public static boolean getBoolean() {
		return ThreadLocalRandom.current().nextBoolean();
	}
	
	public static double getDouble() {
		return ThreadLocalRandom.current().nextDouble();
	}
	
	/**
	 * Returns a pseudorandom double value between the specified origin (inclusive) and bound (exclusive).
	 * @param min - the least value returned
	 * @param max - the upper limit on the value returned (exclusive)
	 * @return a pseudorandom double value between the origin (inclusive) and the bound (exclusive)
	 */
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
	
	public static Date getDateInRange(Date start, Date end) {
		if (start == null || end == null) {
			throw new IllegalArgumentException("Both Start and End dates must be provided");
		}
		if (start.after(end)) {
			Date temp = start;
			start = end;
			end = temp;
		}
		long time = getLong(start.getTime(), end.getTime());
		return new Date(time);
	}
	
	public static Date add(Date original, int duration, TimeUnit timeUnit) {
		return new Date(original.getTime() + TimeUnit.MILLISECONDS.convert(duration, timeUnit));
	}
	
	public static Date add(Date original, Duration duration) {
		return new Date(original.getTime() + duration.toMillis());
	}
	
	/**
	 * Return a date which will is the birth date of someone between minAge
	 * and maxAge years old. Note that someone is considered of a certain age
	 * from the date of their birthdate to the day prior to their next birthday. 
	 * Hence specifying (10, 10) as the parameters for instance still yields
	 * 365 possible days (366 on a leap year). 
	 * <p/>
	 * Also, someone born on February 29th is considered to celebrate their 
	 * birthdays for each calendar year elapsed, not just leap years.
	 * @param minAge - the minimum number of years a person has obtained (inclusive)
	 * @param maxAge - the maximum number of years a person has obtained (inclusive)
	 * @return
	 */
	public static Date getBirthDate(int minAge, int maxAge) {
		LocalDate now = LocalDate.now();
		long min = now.minusYears(minAge).toEpochDay();
		long max = now.minusYears(maxAge+1).toEpochDay();
		long selectedDay = ThreadLocalRandom.current().nextLong(max, min);
		LocalDate selectedDate = LocalDate.ofEpochDay(selectedDay);
		return java.sql.Date.valueOf(selectedDate);
	}
	
	/**
	 * Return a random item out of the provided list
	 * @param <T>
	 * @param values
	 * @return
	 */
	public static <T> T oneOf(T[] values) {
		if (values == null || values.length == 0) {
			return null;
		}
		return values[ThreadLocalRandom.current().nextInt(values.length)];
	}
	
	/**
	 * Return a random item out of the provided list
	 * @param <T>
	 * @param values
	 * @return
	 */
	public static <T> T oneOf(List<T> values) {
		if (values == null || values.isEmpty()) {
			return null;
		}
		return values.get(ThreadLocalRandom.current().nextInt(values.size()));
	}
}
