package dev.lhoopy.profile;

import ru.cristalix.core.database.document.IDocument;
import ru.cristalix.core.database.nosql.mongo.MongoDocument;

import java.util.UUID;

final class ProfileDocuments {
    private static final String ROOT = "slimes";
    private static final String COINS = "coins";
    private static final String FARM = "farm";
    private static final String STORAGE = "storage";
    private static final String VACPACK = "vacpack";
    private static final String VACPACK_CAPACITY = "vacpackCapacity";
    private static final String VACPACK_PLORT_CAPACITY = "vacpackPlortCapacity";
    private static final String VACPACK_SLIME_CAPACITY = "vacpackSlimeCapacity";
    private static final String VACPACK_FOOD_CAPACITY = "vacpackFoodCapacity";
    private static final String VACPACK_SEED_CAPACITY = "vacpackSeedCapacity";
    private static final String VACPACK_RESOURCE_CAPACITY = "vacpackResourceCapacity";
    private static final String VACPACK_OTHER_CAPACITY = "vacpackOtherCapacity";
    private static final String PROGRESSION = "progression";
    private static final String QUESTS = "quests";
    private static final String RESOURCE_NODES = "resourceNodes";

    private ProfileDocuments() {
    }

    static PlayerProfile read(UUID playerId, IDocument document) {
        IDocument root = document == null ? null : document.getDocument(ROOT);
        PlayerProfile profile = new PlayerProfile(playerId, root == null ? 0L : DocumentValues.readLong(root.get(COINS), 0L));
        if (root == null) {
            return profile;
        }

        IDocument farm = root.getDocument(FARM);
        if (farm != null) {
            FarmProfileCodec.readInto(profile, farm);
        }

        IDocument storage = root.getDocument(STORAGE);
        if (storage != null) {
            StorageProfileCodec.readInto(profile.getStorage(), storage);
        }

        readVacpack(profile, root);

        IDocument progression = root.getDocument(PROGRESSION);
        if (progression != null) {
            ProgressProfileCodec.readInto(profile, progression);
        }

        IDocument quests = root.getDocument(QUESTS);
        if (quests != null) {
            QuestProfileCodec.readInto(profile, quests);
        }

        IDocument resourceNodes = root.getDocument(RESOURCE_NODES);
        if (resourceNodes != null) {
            ResourceNodeProfileCodec.readInto(profile, resourceNodes);
        }
        return profile;
    }

    static IDocument writeInto(IDocument document, PlayerProfile profile) {
        IDocument target = document == null ? new MongoDocument() : document;
        IDocument root = new MongoDocument();

        root.put(COINS, profile.getCoins());
        root.put(FARM, FarmProfileCodec.write(profile));
        root.put(STORAGE, StorageProfileCodec.write(profile.getStorage()));
        writeVacpack(root, profile);
        root.put(PROGRESSION, ProgressProfileCodec.write(profile));
        root.put(QUESTS, QuestProfileCodec.write(profile));
        root.put(RESOURCE_NODES, ResourceNodeProfileCodec.write(profile));

        target.put(ROOT, root);
        return target;
    }

    private static void readVacpack(PlayerProfile profile, IDocument root) {
        int legacyCapacity = DocumentValues.readInt(root.get(VACPACK_CAPACITY), 32);
        profile.setVacpackPlortCapacity(DocumentValues.readInt(root.get(VACPACK_PLORT_CAPACITY), legacyCapacity));
        profile.setVacpackSlimeCapacity(DocumentValues.readInt(root.get(VACPACK_SLIME_CAPACITY), 4));
        profile.setVacpackFoodCapacity(DocumentValues.readInt(root.get(VACPACK_FOOD_CAPACITY), 64));
        profile.setVacpackSeedCapacity(DocumentValues.readInt(root.get(VACPACK_SEED_CAPACITY), 64));
        profile.setVacpackResourceCapacity(DocumentValues.readInt(root.get(VACPACK_RESOURCE_CAPACITY), 64));
        profile.setVacpackOtherCapacity(DocumentValues.readInt(root.get(VACPACK_OTHER_CAPACITY), 32));

        IDocument vacpack = root.getDocument(VACPACK);
        if (vacpack != null) {
            StorageProfileCodec.readInto(profile.getVacpackStorage(), vacpack);
        }
    }

    private static void writeVacpack(IDocument root, PlayerProfile profile) {
        root.put(VACPACK_CAPACITY, profile.getVacpackPlortCapacity());
        root.put(VACPACK_PLORT_CAPACITY, profile.getVacpackPlortCapacity());
        root.put(VACPACK_SLIME_CAPACITY, profile.getVacpackSlimeCapacity());
        root.put(VACPACK_FOOD_CAPACITY, profile.getVacpackFoodCapacity());
        root.put(VACPACK_SEED_CAPACITY, profile.getVacpackSeedCapacity());
        root.put(VACPACK_RESOURCE_CAPACITY, profile.getVacpackResourceCapacity());
        root.put(VACPACK_OTHER_CAPACITY, profile.getVacpackOtherCapacity());
        root.put(VACPACK, StorageProfileCodec.write(profile.getVacpackStorage()));
    }
}
