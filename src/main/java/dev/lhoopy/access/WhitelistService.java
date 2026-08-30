package dev.lhoopy.access;

import dev.lhoopy.core.lifecycle.PluginService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class WhitelistService implements PluginService, Listener {
    private static final boolean ENABLED = true;
    private static final String KICK_MESSAGE = ChatColor.RED + "Доступ запрещён";

    private static final Set<String> BASE_ALLOWED_NAMES = normalizedSet(
            "LhoopyNyashka148"
    );

    private final Plugin plugin;
    private final Set<String> runtimeAllowedNames = new LinkedHashSet<>(BASE_ALLOWED_NAMES);
    private final Set<String> storedAllowedNames = new LinkedHashSet<>();
    private final File storageFile;

    public WhitelistService(Plugin plugin) {
        this.plugin = plugin;
        this.storageFile = new File(plugin.getDataFolder(), "whitelist.yml");
    }

    @Override
    public void enable() {
        loadStoredNames();
        Bukkit.getPluginManager().registerEvents(this, this.plugin);
        enforceOnlinePlayers();
        this.plugin.getLogger().info("Hardcoded whitelist enabled with " + this.runtimeAllowedNames.size() + " allowed names.");
    }

    @Override
    public void shutdown() {
        HandlerList.unregisterAll(this);
        this.runtimeAllowedNames.clear();
    }

    @EventHandler
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        if (isAllowed(event.getName())) {
            return;
        }
        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, KICK_MESSAGE);
    }

    public void handleCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("slimes.admin") && !sender.hasPermission("slimes.whitelist")) {
            sender.sendMessage(ChatColor.RED + "Нет доступа.");
            return;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        if (action.equals("list")) {
            sender.sendMessage(ChatColor.GREEN + "Допущены: " + ChatColor.WHITE + String.join(", ", this.runtimeAllowedNames));
            return;
        }

        if (action.equals("enforce")) {
            int kicked = enforceOnlinePlayers();
            sender.sendMessage(ChatColor.GREEN + "Проверка готова. Кикнуто: " + ChatColor.WHITE + kicked);
            return;
        }

        if (args.length < 2) {
            sendUsage(sender);
            return;
        }

        String playerName = normalize(args[1]);
        if (action.equals("add")) {
            this.runtimeAllowedNames.add(playerName);
            this.storedAllowedNames.add(playerName);
            saveStoredNames();
            sender.sendMessage(ChatColor.GREEN + "Добавлен навсегда: " + ChatColor.WHITE + playerName);
            return;
        }

        if (action.equals("remove")) {
            if (BASE_ALLOWED_NAMES.contains(playerName)) {
                sender.sendMessage(ChatColor.RED + "Этот ник зашит в код и не удаляется командой.");
                return;
            }
            this.runtimeAllowedNames.remove(playerName);
            this.storedAllowedNames.remove(playerName);
            saveStoredNames();
            sender.sendMessage(ChatColor.YELLOW + "Удален навсегда: " + ChatColor.WHITE + playerName);
            Player player = findOnlinePlayer(playerName);
            if (player != null) {
                player.kickPlayer(KICK_MESSAGE);
            }
            return;
        }

        sendUsage(sender);
    }

    public boolean isAllowed(Player player) {
        return isAllowed(player.getName());
    }

    public boolean isAllowed(String playerName) {
        return !ENABLED || this.runtimeAllowedNames.contains(normalize(playerName));
    }

    private int enforceOnlinePlayers() {
        int kicked = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (kickIfDenied(player)) {
                kicked++;
            }
        }
        return kicked;
    }

    private boolean kickIfDenied(Player player) {
        if (isAllowed(player)) {
            return false;
        }
        player.kickPlayer(KICK_MESSAGE);
        return true;
    }

    private Player findOnlinePlayer(String normalizedName) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (normalize(player.getName()).equals(normalizedName)) {
                return player;
            }
        }
        return null;
    }

    private static void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "/slimesadmin whitelist add <nick>");
        sender.sendMessage(ChatColor.YELLOW + "/slimesadmin whitelist remove <nick>");
        sender.sendMessage(ChatColor.YELLOW + "/slimesadmin whitelist list");
        sender.sendMessage(ChatColor.YELLOW + "/slimesadmin whitelist enforce");
    }

    private void loadStoredNames() {
        this.runtimeAllowedNames.clear();
        this.runtimeAllowedNames.addAll(BASE_ALLOWED_NAMES);
        this.storedAllowedNames.clear();

        if (!this.storageFile.isFile()) {
            saveStoredNames();
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(this.storageFile);
        for (String name : config.getStringList("allowed")) {
            String normalized = normalize(name);
            this.storedAllowedNames.add(normalized);
            this.runtimeAllowedNames.add(normalized);
        }
    }

    private void saveStoredNames() {
        if (!this.plugin.getDataFolder().isDirectory() && !this.plugin.getDataFolder().mkdirs()) {
            this.plugin.getLogger().warning("Could not create plugin data folder for whitelist.yml");
            return;
        }

        FileConfiguration config = new YamlConfiguration();
        List<String> allowed = new ArrayList<>(this.storedAllowedNames);
        Collections.sort(allowed);
        config.set("allowed", allowed);
        try {
            config.save(this.storageFile);
        } catch (IOException exception) {
            this.plugin.getLogger().warning("Could not save whitelist.yml: " + exception.getMessage());
        }
    }

    private static Set<String> normalizedSet(String... names) {
        Set<String> result = new LinkedHashSet<>();
        Arrays.stream(names).map(WhitelistService::normalize).forEach(result::add);
        return Collections.unmodifiableSet(result);
    }

    private static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
