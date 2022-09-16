package com.yugabyte.simulation.util;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.json.JSONObject;

public class GeneralUtility {
    private static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static SecureRandom rnd = new SecureRandom();
    private static final String JSON_TEMPLATE="{name:}";
    public static final String FIXED_JSON_STRING = getRandomJSONString();// Creating this so I don't have to spend compute on creating the random json.

    public static void main(String[] args) {
        System.out.println(getRandomJSONString());
    }



    public static String randomString(int len){
        StringBuilder sb = new StringBuilder(len);
        for(int i = 0; i < len; i++)
            sb.append(AB.charAt(rnd.nextInt(AB.length())));
        return sb.toString();
    }

    public static int randomIntegerVal(int min, int max){
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    public static int randomIntegerVal(){
        return randomIntegerVal(0,9999);
    }

    public static String getRandomJSONString(){
        JSONObject obj = new JSONObject();
        obj.put("name",randomString(15));
        obj.put("address",randomString(20));
        obj.put("taxinfo",randomString(30));
        obj.put("taxowed",randomIntegerVal(0,1000));
        for(int i = 0; i < 60; i++){
            obj.put("field_"+i, randomIntegerVal());
        }
        return obj.toString();
    }

    public static long randomLongVal(long min, long max){
        return ThreadLocalRandom.current().nextLong(min, max + 1);
    }

    public static List<Long> getRandomIds(int size){
        List<Long> list = new ArrayList<>();
        for(int i = 0; i < size; i++){
            list.add(randomLongVal(1,10000));
        }
        return list;
    }



}
