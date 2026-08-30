package dev.lhoopy.economy;

import dev.lhoopy.content.PlortDef;

public final class PriceService {
    public int getSellPrice(PlortDef plort) {
        return plort == null ? 0 : Math.max(0, plort.getBasePrice());
    }
}
