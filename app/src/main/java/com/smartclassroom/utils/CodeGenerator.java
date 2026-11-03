package com.smartclassroom.utils;

import java.security.SecureRandom;

public final class CodeGenerator {
    private static final String CHARSET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    private CodeGenerator() {}

    public static String generateClassroomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            int index = RANDOM.nextInt(CHARSET.length());
            sb.append(CHARSET.charAt(index));
        }
        return sb.toString();
    }
}
