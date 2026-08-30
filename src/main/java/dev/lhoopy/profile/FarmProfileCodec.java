package dev.lhoopy.profile;

import dev.lhoopy.farm.FarmPlot;
import dev.lhoopy.pen.PenSlime;
import dev.lhoopy.pen.PenFeedQuality;
import ru.cristalix.core.database.document.IDocument;
import ru.cristalix.core.database.nosql.mongo.MongoDocument;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

final class FarmProfileCodec {
    private static final String PEN_CAPACITY = "penCapacity";
    private static final String PEN_CASE_KEYS = "penCaseKeys";
    private static final String PEN_BLUEPRINTS = "penBlueprints";
    private static final String ACTIVE_PEN_STYLE = "activePenStyle";
    private static final String CAPTURED_SLIMES = "capturedSlimes";
    private static final String PEN_SLIMES = "penSlimes";
    private static final String SLIME_ID = "slimeId";
    private static final String LAST_FED = "lastFed";
    private static final String FED_UNTIL = "fedUntil";
    private static final String FEED_QUALITY = "feedQuality";
    private static final String PRODUCTION_REMAINDER = "productionRemainder";
    private static final String LAST_PLORT_PRODUCTION = "lastPlortProduction";
    private static final String PEN_PLORTS = "penPlorts";
    private static final String PLOTS = "plots";
    private static final String PLOT_TYPE_ID = "plotTypeId";
    private static final String PLANT_ID = "plantId";
    private static final String PLANTED_AT = "plantedAt";
    private static final String WATERED_UNTIL = "wateredUntil";
    private static final String GROWTH_PROGRESS = "growthProgress";
    private static final String LAST_GROWTH_UPDATE = "lastGrowthUpdate";

    private FarmProfileCodec() {
    }

    static void readInto(PlayerProfile profile, IDocument farm) {
        profile.setPenCapacity(DocumentValues.readInt(farm.get(PEN_CAPACITY), 6));
        profile.setPenCaseKeys(DocumentValues.readInt(farm.get(PEN_CASE_KEYS), 0));
        profile.setActivePenStyleId(DocumentValues.readString(farm.get(ACTIVE_PEN_STYLE), "pen_basic"));
        readBlueprints(profile, farm.getDocument(PEN_BLUEPRINTS));
        profile.setCapturedSlimeIds(DocumentValues.readStringList(farm.get(CAPTURED_SLIMES)));
        readPenSlimes(profile, farm.get(PEN_SLIMES));
        profile.setLastPlortProductionMillis(DocumentValues.readLong(farm.get(LAST_PLORT_PRODUCTION), 0L));

        IDocument penPlorts = farm.getDocument(PEN_PLORTS);
        if (penPlorts != null) {
            StorageProfileCodec.readInto(profile.getPenPlortStorage(), penPlorts);
        }

        IDocument plots = farm.getDocument(PLOTS);
        if (plots != null) {
            readPlots(profile, plots);
        }
    }

    static IDocument write(PlayerProfile profile) {
        IDocument farm = new MongoDocument();
        farm.put(PEN_CAPACITY, profile.getPenCapacity());
        farm.put(PEN_CASE_KEYS, profile.getPenCaseKeys());
        farm.put(ACTIVE_PEN_STYLE, profile.getActivePenStyleId());
        farm.put(PEN_BLUEPRINTS, writeBlueprints(profile));
        farm.put(CAPTURED_SLIMES, new ArrayList<>(profile.getCapturedSlimeIds()));
        farm.put(PEN_SLIMES, writePenSlimes(profile));
        farm.put(LAST_PLORT_PRODUCTION, profile.getLastPlortProductionMillis());
        farm.put(PEN_PLORTS, StorageProfileCodec.write(profile.getPenPlortStorage()));
        farm.put(PLOTS, writePlots(profile));
        return farm;
    }

    private static void readBlueprints(PlayerProfile profile, IDocument document) {
        Map<String, Integer> blueprints = new java.util.LinkedHashMap<>();
        if (document != null) {
            for (String styleId : document.keys()) {
                int amount = DocumentValues.readInt(document.get(styleId), 0);
                if (amount > 0) {
                    blueprints.put(styleId, amount);
                }
            }
        }
        profile.setPenBlueprints(blueprints);
    }

    private static IDocument writeBlueprints(PlayerProfile profile) {
        IDocument document = new MongoDocument();
        profile.getPenBlueprints().forEach(document::put);
        return document;
    }

    private static void readPlots(PlayerProfile profile, IDocument plots) {
        for (String plotId : plots.keys()) {
            IDocument plotDocument = plots.getDocument(plotId);
            if (plotDocument == null) {
                continue;
            }
            FarmPlot plot = profile.getFarmData().getOrCreatePlot(plotId);
            plot.setPlotTypeId(DocumentValues.readString(plotDocument.get(PLOT_TYPE_ID), "basic"));
            plot.setPlantId(DocumentValues.readString(plotDocument.get(PLANT_ID), null));
            plot.setPlantedAtMillis(DocumentValues.readLong(plotDocument.get(PLANTED_AT), 0L));
            plot.setWateredUntilMillis(DocumentValues.readLong(plotDocument.get(WATERED_UNTIL), 0L));
            plot.setGrowthProgressMillis(DocumentValues.readLong(plotDocument.get(GROWTH_PROGRESS), 0L));
            plot.setLastGrowthUpdateMillis(DocumentValues.readLong(plotDocument.get(LAST_GROWTH_UPDATE), plot.getPlantedAtMillis()));
        }
    }

    private static IDocument writePlots(PlayerProfile profile) {
        IDocument plots = new MongoDocument();
        for (FarmPlot plot : profile.getFarmData().getPlots()) {
            IDocument plotDocument = new MongoDocument();
            plotDocument.put(PLOT_TYPE_ID, plot.getPlotTypeId());
            if (!plot.isEmpty()) {
                plotDocument.put(PLANT_ID, plot.getPlantId());
            }
            plotDocument.put(PLANTED_AT, plot.getPlantedAtMillis());
            plotDocument.put(WATERED_UNTIL, plot.getWateredUntilMillis());
            plotDocument.put(GROWTH_PROGRESS, plot.getGrowthProgressMillis());
            plotDocument.put(LAST_GROWTH_UPDATE, plot.getLastGrowthUpdateMillis());
            plots.put(plot.getId(), plotDocument);
        }
        return plots;
    }

    private static void readPenSlimes(PlayerProfile profile, Object value) {
        if (!(value instanceof Collection<?>)) {
            profile.setPenSlimeIds(Collections.emptyList());
            return;
        }

        List<PenSlime> penSlimes = new ArrayList<>();
        List<String> legacySlimes = new ArrayList<>();
        for (Object entry : (Collection<?>) value) {
            if (entry instanceof IDocument) {
                readPenSlimeDocument(penSlimes, (IDocument) entry);
                continue;
            }
            if (entry instanceof Map<?, ?>) {
                readPenSlimeMap(penSlimes, (Map<?, ?>) entry);
                continue;
            }
            if (entry != null) {
                String slimeId = readLegacySlimeId(String.valueOf(entry));
                if (slimeId != null) {
                    legacySlimes.add(slimeId);
                }
            }
        }

        if (!penSlimes.isEmpty()) {
            profile.setPenSlimes(penSlimes);
        } else {
            profile.setPenSlimeIds(legacySlimes);
        }
    }

    private static void readPenSlimeDocument(List<PenSlime> penSlimes, IDocument slime) {
        String slimeId = DocumentValues.readString(slime.get(SLIME_ID), null);
        if (slimeId == null && slime.getDocument("data") != null) {
            slime = slime.getDocument("data");
            slimeId = DocumentValues.readString(slime.get(SLIME_ID), null);
        }
        if (slimeId != null) {
            long fedUntil = DocumentValues.readLong(slime.get(FED_UNTIL), 0L);
            penSlimes.add(new PenSlime(
                    slimeId,
                    DocumentValues.readLong(slime.get(LAST_FED), 0L),
                    fedUntil,
                    PenFeedQuality.read(DocumentValues.readString(slime.get(FEED_QUALITY), null),
                            fedUntil > 0L ? PenFeedQuality.FAVORITE : PenFeedQuality.HUNGRY),
                    DocumentValues.readDouble(slime.get(PRODUCTION_REMAINDER), 0.0D)
            ));
        }
    }

    private static void readPenSlimeMap(List<PenSlime> penSlimes, Map<?, ?> slime) {
        if (!slime.containsKey(SLIME_ID) && !slime.containsKey("slime-id") && slime.get("data") instanceof Map<?, ?>) {
            slime = (Map<?, ?>) slime.get("data");
        }
        String slimeId = DocumentValues.readString(DocumentValues.firstPresent(slime, SLIME_ID, "slime-id"), null);
        if (slimeId != null) {
            long fedUntil = DocumentValues.readLong(DocumentValues.firstPresent(slime, FED_UNTIL, "fed-until"), 0L);
            penSlimes.add(new PenSlime(
                    slimeId,
                    DocumentValues.readLong(DocumentValues.firstPresent(slime, LAST_FED, "last-fed"), 0L),
                    fedUntil,
                    PenFeedQuality.read(DocumentValues.readString(
                            DocumentValues.firstPresent(slime, FEED_QUALITY, "feed-quality"), null),
                            fedUntil > 0L ? PenFeedQuality.FAVORITE : PenFeedQuality.HUNGRY),
                    DocumentValues.readDouble(DocumentValues.firstPresent(
                            slime, PRODUCTION_REMAINDER, "production-remainder"), 0.0D)
            ));
        }
    }

    private static List<IDocument> writePenSlimes(PlayerProfile profile) {
        List<IDocument> slimes = new ArrayList<>();
        for (PenSlime slime : profile.getPenSlimes()) {
            IDocument document = new MongoDocument();
            document.put(SLIME_ID, slime.getSlimeId());
            document.put(LAST_FED, slime.getLastFedMillis());
            document.put(FED_UNTIL, slime.getFedUntilMillis());
            document.put(FEED_QUALITY, slime.getStoredFeedQuality().name());
            document.put(PRODUCTION_REMAINDER, slime.getProductionRemainder());
            slimes.add(document);
        }
        return slimes;
    }

    private static String readLegacySlimeId(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String trimmed = value.trim();
        String slimeId = readBetween(trimmed, "slimeId=", ",");
        if (slimeId == null) {
            slimeId = readBetween(trimmed, "slime-id=", ",");
        }
        if (slimeId != null) {
            return cleanLegacySlimeId(slimeId);
        }
        if (trimmed.contains("fedUntil=") || trimmed.contains("lastFed=") || trimmed.contains("data={")) {
            return null;
        }
        return cleanLegacySlimeId(trimmed);
    }

    private static String readBetween(String value, String prefix, String suffix) {
        int start = value.indexOf(prefix);
        if (start < 0) {
            return null;
        }
        start += prefix.length();
        int end = value.indexOf(suffix, start);
        if (end < 0) {
            end = value.indexOf('}', start);
        }
        if (end < 0) {
            end = value.length();
        }
        return value.substring(start, end);
    }

    private static String cleanLegacySlimeId(String slimeId) {
        String cleaned = slimeId == null ? "" : slimeId.trim();
        while (cleaned.endsWith("}") || cleaned.endsWith("]")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1).trim();
        }
        return cleaned.isEmpty() ? null : cleaned;
    }
}
