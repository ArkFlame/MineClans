package com.arkflame.mineclans.utils;

public class NameUtil {
    public static boolean isValidName(String name) {
        if (name == null) {
            return false;
        }
        // Check if the string matches the alphanumeric pattern
        if (!name.matches("^[A-Za-z0-9]+$")) {
            return false;
        }
        // Check if the length is less than 10
        return name.length() < 10;
    }
}
