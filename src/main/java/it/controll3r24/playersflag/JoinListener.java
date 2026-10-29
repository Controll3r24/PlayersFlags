package it.controll3r24.playersflag;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

final class JoinListener implements Listener {

    private final PlayersFlagPlugin plugin;

    JoinListener(PlayersFlagPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        plugin.flags().refresh(e.getPlayer(), false);
        plugin.maybeWelcome(e.getPlayer());
    }
}
