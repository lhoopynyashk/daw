package dev.lhoopy.pen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PenData {
    private final List<String> slimeIds = new ArrayList<>();
    private final int capacity;

    public PenData(int capacity) {
        this.capacity = capacity;
    }

    public int getCapacity() {
        return this.capacity;
    }

    public List<String> getSlimeIds() {
        return Collections.unmodifiableList(this.slimeIds);
    }

    public boolean isFull() {
        return this.slimeIds.size() >= this.capacity;
    }

    public boolean add(String slimeId) {
        if (isFull()) {
            return false;
        }
        this.slimeIds.add(slimeId);
        return true;
    }
}
