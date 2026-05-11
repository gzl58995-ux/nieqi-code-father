package com.nieqi.nieqicodefather.utils;

public class CacheKeyUtils {

    private CacheKeyUtils() {
    }

    public static String generateKey(Object obj) {
        return obj == null ? "null" : Integer.toHexString(obj.hashCode());
    }
}

