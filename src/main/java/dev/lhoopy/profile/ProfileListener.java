package dev.lhoopy.profile;

import dev.lhoopy.profile.ProfileService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class ProfileListener implements Listener {
    private final ProfileService profileService;

    public ProfileListener(ProfileService profileService) {
        this.profileService = profileService;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        this.profileService.load(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        this.profileService.unload(event.getPlayer()).join();
    }
}
