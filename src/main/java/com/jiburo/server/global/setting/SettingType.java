package com.jiburo.server.global.setting;

import java.util.Arrays;
import java.util.List;

/**
 * 운영 설정값의 타입. DB에는 문자열로 저장하고, 읽을 때/저장 전에 이 타입으로 검증한다.
 */
public enum SettingType {
    INT, LONG, DOUBLE, BOOLEAN, STRING, STRING_LIST;

    /**
     * 문자열 값을 이 타입으로 해석할 수 있는지, 숫자라면 min~max 범위 안인지 검증한다.
     * @return 정규화된 문자열 (공백 제거 등)
     * @throws IllegalArgumentException 해석 불가/범위 밖
     */
    public String normalize(String raw, Double min, Double max) {
        if (raw == null) {
            throw new IllegalArgumentException("value is null");
        }
        String value = raw.trim();
        switch (this) {
            case INT -> checkRange(Integer.parseInt(value), min, max);
            case LONG -> checkRange(Long.parseLong(value), min, max);
            case DOUBLE -> {
                double d = Double.parseDouble(value);
                if (Double.isNaN(d) || Double.isInfinite(d)) throw new IllegalArgumentException("not a finite number");
                checkRange(d, min, max);
            }
            case BOOLEAN -> {
                if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                    throw new IllegalArgumentException("boolean must be true or false");
                }
                value = value.toLowerCase();
            }
            case STRING -> {
                if (value.isEmpty()) throw new IllegalArgumentException("empty string");
            }
            case STRING_LIST -> {
                List<String> items = splitList(value);
                if (items.isEmpty()) throw new IllegalArgumentException("empty list");
                value = String.join(",", items);
            }
        }
        return value;
    }

    public static List<String> splitList(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
    }

    private static void checkRange(double v, Double min, Double max) {
        if (min != null && v < min) throw new IllegalArgumentException("must be >= " + min);
        if (max != null && v > max) throw new IllegalArgumentException("must be <= " + max);
    }
}
