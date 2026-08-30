package dev.lhoopy.storage;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class PlayerStorage {
    public static final int MAX_AMOUNT_PER_ITEM = 1_000_000_000;

    private final Map<String, StoredItem> items = new LinkedHashMap<>();

    public Collection<StoredItem> getItems() {
        return Collections.unmodifiableCollection(this.items.values());
    }

    public StoredItem get(String itemId) {
        if (itemId == null) {
            return null;
        }
        return this.items.get(normalize(itemId));
    }

    public int getAmount(String itemId) {
        StoredItem item = get(itemId);
        return item == null ? 0 : item.getAmount();
    }

    public boolean has(String itemId, int amount) {
        return amount > 0 && getAmount(itemId) >= amount;
    }

    public void set(String itemId, int amount, boolean protectedItem) {
        String normalized = normalize(itemId);
        if (amount <= 0) {
            this.items.remove(normalized);
            return;
        }
        this.items.put(normalized, new StoredItem(normalized, Math.min(amount, MAX_AMOUNT_PER_ITEM), protectedItem));
    }

    public void add(String itemId, int amount) {
        if (amount <= 0) {
            return;
        }
        String normalized = normalize(itemId);
        StoredItem item = this.items.get(normalized);
        if (item == null) {
            this.items.put(normalized, new StoredItem(normalized, Math.min(amount, MAX_AMOUNT_PER_ITEM), false));
            return;
        }
        item.setAmount(Math.min(MAX_AMOUNT_PER_ITEM, item.getAmount() + amount));
    }

    public int addLimited(String itemId, int amount, int used, int capacity) {
        if (amount <= 0 || capacity <= 0) {
            return 0;
        }
        int free = Math.max(0, capacity - used);
        int itemFree = Math.max(0, MAX_AMOUNT_PER_ITEM - getAmount(itemId));
        int accepted = Math.min(amount, Math.min(free, itemFree));
        if (accepted > 0) {
            add(itemId, accepted);
        }
        return accepted;
    }

    public int getTotalAmount() {
        int total = 0;
        for (StoredItem item : this.items.values()) {
            total += Math.max(0, item.getAmount());
        }
        return total;
    }

    public boolean remove(String itemId, int amount) {
        String normalized = normalize(itemId);
        StoredItem item = this.items.get(normalized);
        if (item == null || !item.removeAmount(amount)) {
            return false;
        }
        if (item.getAmount() <= 0) {
            this.items.remove(normalized);
        }
        return true;
    }

    public void setProtected(String itemId, boolean protectedItem) {
        String normalized = normalize(itemId);
        StoredItem item = this.items.get(normalized);
        if (item == null) {
            this.items.put(normalized, new StoredItem(normalized, 0, protectedItem));
            return;
        }
        item.setProtectedItem(protectedItem);
    }

    public boolean isProtected(String itemId) {
        StoredItem item = get(itemId);
        return item != null && item.isProtectedItem();
    }

    private static String normalize(String id) {
        return id.toLowerCase(Locale.ROOT).replace('-', '_');
    }
}
