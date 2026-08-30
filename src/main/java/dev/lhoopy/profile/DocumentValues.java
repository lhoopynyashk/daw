package dev.lhoopy.profile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

final class DocumentValues {
    private DocumentValues() {
    }

    static long readLong(Object value, long fallback) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    static int readInt(Object value, int fallback) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    static double readDouble(Object value, double fallback) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    static boolean readBoolean(Object value, boolean fallback) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return fallback;
    }

    static String readString(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }

    static List<String> readStringList(Object value) {
        if (!(value instanceof Collection<?>)) {
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<>();
        for (Object entry : (Collection<?>) value) {
            if (entry != null) {
                result.add(String.valueOf(entry));
            }
        }
        return result;
    }

    static Object firstPresent(Map<?, ?> map, String primary, String fallback) {
        Object value = map.get(primary);
        return value == null ? map.get(fallback) : value;
    }
}
