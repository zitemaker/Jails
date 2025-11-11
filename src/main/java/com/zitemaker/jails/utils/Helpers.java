package com.zitemaker.jails.utils;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class Helpers {

    public Helpers(){

    }

    public static <E extends Enum<E>, V> Map<E, V> suppliedMap(Class<E> clazz, Function<E, V> mapper) {
        Map<E, V> map = new EnumMap<>(clazz);
        for (E e : clazz.getEnumConstants()) {
            map.put(e, mapper.apply(e));
        }

        return map;
    }

    private static int levenshteinDistance(String s1, String s2) {
        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();

        int[] costs = new int[s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            int lastValue = i;
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    costs[j] = j;
                } else if (j > 0) {
                    int newValue = costs[j - 1];
                    if (s1.charAt(i - 1) != s2.charAt(j - 1)) {
                        newValue = Math.min(Math.min(newValue, lastValue), costs[j]) + 1;
                    }
                    costs[j - 1] = lastValue;
                    lastValue = newValue;
                }
            }
            if (i > 0) {
                costs[s2.length()] = lastValue;
            }
        }
        return costs[s2.length()];
    }

    public static String findClosestMatch(String input, Set<String> options, int threshold) {
        if (input == null || options == null || options.isEmpty()) {
            return null;
        }

        String closestMatch = null;
        int minDistance = Integer.MAX_VALUE;

        for (String option : options) {
            int distance = levenshteinDistance(input, option);
            if (distance < minDistance) {
                minDistance = distance;
                closestMatch = option;
            }
        }

        return minDistance <= threshold ? closestMatch : null;
    }
}
