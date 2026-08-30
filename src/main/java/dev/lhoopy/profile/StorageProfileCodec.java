package dev.lhoopy.profile;

import dev.lhoopy.storage.PlayerStorage;
import dev.lhoopy.storage.StoredItem;
import ru.cristalix.core.database.document.IDocument;
import ru.cristalix.core.database.nosql.mongo.MongoDocument;

final class StorageProfileCodec {
    private static final String AMOUNT = "amount";
    private static final String PROTECTED = "protected";

    private StorageProfileCodec() {
    }

    static void readInto(PlayerStorage target, IDocument storage) {
        for (String itemId : storage.keys()) {
            IDocument item = storage.getDocument(itemId);
            if (item == null) {
                continue;
            }
            target.set(
                    itemId,
                    DocumentValues.readInt(item.get(AMOUNT), 0),
                    DocumentValues.readBoolean(item.get(PROTECTED), false)
            );
        }
    }

    static IDocument write(PlayerStorage source) {
        IDocument storage = new MongoDocument();
        for (StoredItem storedItem : source.getItems()) {
            if (storedItem.getAmount() <= 0 && !storedItem.isProtectedItem()) {
                continue;
            }
            IDocument item = new MongoDocument();
            item.put(AMOUNT, storedItem.getAmount());
            item.put(PROTECTED, storedItem.isProtectedItem());
            storage.put(storedItem.getItemId(), item);
        }
        return storage;
    }
}
