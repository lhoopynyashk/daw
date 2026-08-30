package dev.lhoopy.core.bootstrap;

import dev.lhoopy.access.WhitelistService;
import dev.lhoopy.content.ContentLoader;
import dev.lhoopy.content.ContentRegistry;
import dev.lhoopy.core.SlimesPlugin;
import dev.lhoopy.crafting.CraftingService;
import dev.lhoopy.crafting.FarmerTableService;
import dev.lhoopy.crafting.FarmerTableMenuService;
import dev.lhoopy.crafting.FarmerTableNpcService;
import dev.lhoopy.crafting.SlimeLabService;
import dev.lhoopy.economy.EconomyService;
import dev.lhoopy.economy.SellTerminalNpcService;
import dev.lhoopy.farm.FarmService;
import dev.lhoopy.hunt.EnginexHuntBridge;
import dev.lhoopy.hunt.HuntService;
import dev.lhoopy.hunt.HuntZoneService;
import dev.lhoopy.location.LocationRealmService;
import dev.lhoopy.pen.PenService;
import dev.lhoopy.pen.PenCaseService;
import dev.lhoopy.pen.PenStyleCatalog;
import dev.lhoopy.profile.CorePlayerDataBootstrapService;
import dev.lhoopy.profile.CoreProfileRepository;
import dev.lhoopy.profile.FallbackProfileRepository;
import dev.lhoopy.profile.ProfileService;
import dev.lhoopy.profile.YamlProfileRepository;
import dev.lhoopy.progression.ProgressionService;
import dev.lhoopy.quest.QuestService;
import dev.lhoopy.realm.RealmMetadataService;
import dev.lhoopy.slime.SlimeService;
import dev.lhoopy.sound.service.SoundtrackService;
import dev.lhoopy.storage.StorageService;
import dev.lhoopy.voice.RanchVoiceService;
import dev.lhoopy.world.RanchWorldService;

public final class ServiceRegistry {
    private final SlimesPlugin plugin;

    private ContentRegistry contentRegistry;
    private CorePlayerDataBootstrapService playerDataBootstrapService;
    private ProfileService profileService;
    private StorageService storageService;
    private HuntService huntService;
    private EnginexHuntBridge enginexHuntBridge;
    private SlimeService slimeService;
    private RanchWorldService ranchWorldService;
    private RanchVoiceService ranchVoiceService;
    private FarmService farmService;
    private HuntZoneService huntZoneService;
    private LocationRealmService locationRealmService;
    private PenService penService;
    private PenCaseService penCaseService;
    private PenStyleCatalog penStyleCatalog;
    private EconomyService economyService;
    private SellTerminalNpcService sellTerminalNpcService;
    private CraftingService craftingService;
    private FarmerTableService farmerTableService;
    private FarmerTableMenuService farmerTableMenuService;
    private FarmerTableNpcService farmerTableNpcService;
    private SlimeLabService slimeLabService;
    private ProgressionService progressionService;
    private QuestService questService;
    private SoundtrackService soundtrackService;
    private WhitelistService whitelistService;
    private RealmMetadataService realmMetadataService;

    public ServiceRegistry(SlimesPlugin plugin) {
        this.plugin = plugin;
    }

    public void createServices() {
        this.contentRegistry = new ContentRegistry(new ContentLoader(this.plugin));
        this.contentRegistry.load();
        this.plugin.getLogger().info("Loaded " + this.contentRegistry.slimes().size() + " slime definitions.");

        this.playerDataBootstrapService = new CorePlayerDataBootstrapService(this.plugin);
        this.profileService = new ProfileService(new FallbackProfileRepository(
                this.plugin,
                new CoreProfileRepository(this.plugin),
                new YamlProfileRepository(this.plugin)
        ));
        this.storageService = new StorageService(this.profileService);
        this.huntService = new HuntService(this.plugin);
        this.enginexHuntBridge = new EnginexHuntBridge(this.plugin);
        this.slimeService = new SlimeService(this.plugin, this.contentRegistry, this.enginexHuntBridge, this.profileService);
        this.ranchWorldService = new RanchWorldService(this.plugin);
        this.ranchVoiceService = new RanchVoiceService(this.plugin, this.ranchWorldService);
        this.locationRealmService = new LocationRealmService(
                this.plugin,
                this.contentRegistry,
                this.profileService,
                this.enginexHuntBridge,
                this.slimeService
        );
        this.farmService = new FarmService(this.plugin, this.ranchWorldService, this.enginexHuntBridge, this.contentRegistry, this.profileService, this.locationRealmService);
        this.huntZoneService = new HuntZoneService(this.plugin, this.profileService);
        this.penStyleCatalog = new PenStyleCatalog(this.plugin);
        this.penCaseService = new PenCaseService(this.plugin, this.profileService, this.enginexHuntBridge, this.penStyleCatalog);
        this.penService = new PenService(this.plugin, this.enginexHuntBridge, this.contentRegistry, this.slimeService,
                this.profileService, this.penStyleCatalog);
        this.penCaseService.setPenService(this.penService);
        this.economyService = new EconomyService(this.plugin, this.contentRegistry, this.profileService,
                this.penStyleCatalog, this.enginexHuntBridge);
        this.sellTerminalNpcService = new SellTerminalNpcService(
                this.plugin,
                this.ranchWorldService,
                this.economyService
        );
        this.slimeService.setEconomyService(this.economyService);
        this.craftingService = new CraftingService(this.contentRegistry);
        this.farmerTableService = new FarmerTableService(this.contentRegistry, this.craftingService);
        this.farmerTableMenuService = new FarmerTableMenuService(
                this.plugin,
                this.contentRegistry,
                this.profileService,
                this.farmerTableService,
                this.enginexHuntBridge
        );
        this.farmerTableNpcService = new FarmerTableNpcService(
                this.plugin,
                this.ranchWorldService,
                this.farmerTableMenuService
        );
        this.slimeLabService = new SlimeLabService(this.craftingService);
        this.progressionService = new ProgressionService(this.plugin);
        this.questService = new QuestService(this.plugin, this.profileService);
        this.soundtrackService = new SoundtrackService(this.plugin);
        this.whitelistService = new WhitelistService(this.plugin);
        this.realmMetadataService = new RealmMetadataService(this.plugin, this.locationRealmService);
    }

    public void enableServices() {
        this.whitelistService.enable();
        this.enginexHuntBridge.setSlimeService(this.slimeService);
        this.playerDataBootstrapService.enable();
        this.profileService.enable();
        this.enginexHuntBridge.enable();
        this.slimeService.enable();
        this.ranchWorldService.enable();
        this.ranchVoiceService.enable();
        this.locationRealmService.enable();
        this.farmService.enable();
        this.huntZoneService.enable();
        this.penService.enable();
        this.penCaseService.enable();
        this.economyService.enable();
        this.sellTerminalNpcService.enable();
        this.farmerTableMenuService.enable();
        this.farmerTableNpcService.enable();
        this.progressionService.enable();
        this.questService.enable();
        this.soundtrackService.enable();
        this.realmMetadataService.enable();
    }

    public void disableServices() {
        if (this.sellTerminalNpcService != null) {
            this.sellTerminalNpcService.shutdown();
        }
        if (this.farmerTableNpcService != null) {
            this.farmerTableNpcService.shutdown();
        }
        if (this.penService != null) {
            this.penService.shutdown();
        }
        if (this.penCaseService != null) {
            this.penCaseService.shutdown();
        }
        if (this.farmService != null) {
            this.farmService.shutdown();
        }
        if (this.ranchVoiceService != null) {
            this.ranchVoiceService.shutdown();
        }
        if (this.ranchWorldService != null) {
            this.ranchWorldService.shutdown();
        }
        if (this.huntZoneService != null) {
            this.huntZoneService.shutdown();
        }
        if (this.locationRealmService != null) {
            this.locationRealmService.shutdown();
        }
        if (this.soundtrackService != null) {
            this.soundtrackService.shutdown();
        }
        if (this.economyService != null) {
            this.economyService.shutdown();
        }
        if (this.farmerTableMenuService != null) {
            this.farmerTableMenuService.shutdown();
        }
        if (this.questService != null) {
            this.questService.shutdown();
        }
        if (this.progressionService != null) {
            this.progressionService.shutdown();
        }
        if (this.whitelistService != null) {
            this.whitelistService.shutdown();
        }
        if (this.realmMetadataService != null) {
            this.realmMetadataService.shutdown();
        }
        if (this.profileService != null) {
            this.profileService.shutdown();
        }
        if (this.playerDataBootstrapService != null) {
            this.playerDataBootstrapService.shutdown();
        }
        if (this.slimeService != null) {
            this.slimeService.shutdown();
        }
        if (this.huntService != null) {
            this.huntService.shutdown();
        }
        if (this.enginexHuntBridge != null) {
            this.enginexHuntBridge.shutdown();
        }
    }

    public ContentRegistry contentRegistry() {
        return this.contentRegistry;
    }

    public ProfileService profileService() {
        return this.profileService;
    }

    public StorageService storageService() {
        return this.storageService;
    }

    public HuntService huntService() {
        return this.huntService;
    }

    public EnginexHuntBridge enginexHuntBridge() {
        return this.enginexHuntBridge;
    }

    public SlimeService slimeService() {
        return this.slimeService;
    }

    public FarmService farmService() {
        return this.farmService;
    }

    public HuntZoneService huntZoneService() {
        return this.huntZoneService;
    }

    public LocationRealmService locationRealmService() {
        return this.locationRealmService;
    }

    public PenService penService() {
        return this.penService;
    }

    public EconomyService economyService() {
        return this.economyService;
    }

    public CraftingService craftingService() {
        return this.craftingService;
    }

    public PenCaseService penCaseService() {
        return this.penCaseService;
    }

    public FarmerTableMenuService farmerTableMenuService() {
        return this.farmerTableMenuService;
    }

    public QuestService questService() {
        return this.questService;
    }

    public SoundtrackService soundtrackService() {
        return this.soundtrackService;
    }

    public WhitelistService whitelistService() {
        return this.whitelistService;
    }
}
