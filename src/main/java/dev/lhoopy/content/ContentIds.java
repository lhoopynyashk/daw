package dev.lhoopy.content;

import java.util.Locale;

public final class ContentIds {
    public static final String FALLBACK_PLORT_ID = "plort_pink";

    private ContentIds() {
    }

    public static String plortForSlime(String slimeId) {
        return "plort_" + stripSlimePrefix(normalize(slimeId));
    }

    public static String resolvePlortForSlime(ContentRegistry contentRegistry, String slimeId) {
        String direct = plortForSlime(slimeId);
        if (contentRegistry.getPlort(direct) != null) {
            return direct;
        }
        return FALLBACK_PLORT_ID;
    }

    public static String plotRecipeCategory(String plotTypeId) {
        return "plot_" + normalize(plotTypeId);
    }

    private static String normalize(String id) {
        return id == null ? "" : id.toLowerCase(Locale.ROOT).replace('-', '_');
    }

    private static String stripSlimePrefix(String slimeId) {
        return slimeId.startsWith("slime_") ? slimeId.substring("slime_".length()) : slimeId;
    }
}
