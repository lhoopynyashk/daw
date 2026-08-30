package dev.lhoopy.farm;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class FarmData {
    private static final int DEFAULT_PLOTS = 3;

    private final Map<String, FarmPlot> plots = new LinkedHashMap<>();

    public FarmData() {
        ensureDefaultPlots();
    }

    public Collection<FarmPlot> getPlots() {
        ensureDefaultPlots();
        return Collections.unmodifiableCollection(this.plots.values());
    }

    public FarmPlot getPlot(String id) {
        if (id == null) {
            return null;
        }
        ensureDefaultPlots();
        return this.plots.get(normalize(id));
    }

    public FarmPlot getOrCreatePlot(String id) {
        String normalized = normalize(id);
        FarmPlot plot = this.plots.get(normalized);
        if (plot == null) {
            plot = new FarmPlot(normalized);
            this.plots.put(normalized, plot);
        }
        return plot;
    }

    private void ensureDefaultPlots() {
        for (int index = 1; index <= DEFAULT_PLOTS; index++) {
            String id = "plot_" + index;
            this.plots.putIfAbsent(id, new FarmPlot(id));
        }
    }

    private static String normalize(String id) {
        return id.toLowerCase(Locale.ROOT).replace('-', '_');
    }
}
