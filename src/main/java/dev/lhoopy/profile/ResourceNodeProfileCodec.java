package dev.lhoopy.profile;

import ru.cristalix.core.database.document.IDocument;
import ru.cristalix.core.database.nosql.mongo.MongoDocument;

import java.util.Map;

final class ResourceNodeProfileCodec {
    private ResourceNodeProfileCodec() {
    }

    static void readInto(PlayerProfile profile, IDocument document) {
        long now = System.currentTimeMillis();
        for (String nodeKey : document.keys()) {
            long respawnAt = DocumentValues.readLong(document.get(nodeKey), 0L);
            if (respawnAt > now) {
                profile.setResourceNodeRespawn(nodeKey, respawnAt);
            }
        }
    }

    static IDocument write(PlayerProfile profile) {
        long now = System.currentTimeMillis();
        profile.removeExpiredResourceNodeRespawns(now);
        IDocument document = new MongoDocument();
        for (Map.Entry<String, Long> entry : profile.getResourceNodeRespawns().entrySet()) {
            if (entry.getValue() > now) {
                document.put(entry.getKey(), entry.getValue());
            }
        }
        return document;
    }
}
