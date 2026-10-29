package it.controll3r24.playersflag;

import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public final class PlayersFlagPlugin extends JavaPlugin {

    private Text text;
    private FlagManager flags;
    private GeoLocator geo;
    private Object papiHook; // PapiHook, caricato solo se PlaceholderAPI c'e'

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getDataFolder().mkdirs();

        text = new Text(this);
        flags = new FlagManager(this);
        flags.load();
        geo = new GeoLocator(this);

        PluginCommand cmd = getCommand("pf");
        if (cmd != null) {
            PfCommand pf = new PfCommand(this);
            cmd.setExecutor(pf);
            cmd.setTabCompleter(pf);
        }
        getServer().getPluginManager().registerEvents(new JoinListener(this), this);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            // Aspettiamo che PAPI abbia caricato le sue espansioni (serve per %player_flag%)
            getServer().getScheduler().runTaskLater(this, this::hookPapi, 100L);
        } else {
            getLogger().warning("PlaceholderAPI non trovato: i placeholder %player_flag% non saranno disponibili.");
        }

        for (Player p : getServer().getOnlinePlayers()) {
            flags.refresh(p, false);
        }
        getLogger().info("Players Flag " + getPluginMeta().getVersion() + " attivo.");
    }

    @Override
    public void onDisable() {
        if (papiHook instanceof PapiHook h) h.unregister();
        if (flags != null) flags.save();
        if (geo != null) geo.shutdown();
    }

    private void hookPapi() {
        PapiHook h = new PapiHook(this);
        h.register();
        papiHook = h;
    }

    public void reload() {
        reloadConfig();
        text.reload();
        if (papiHook instanceof PapiHook h) h.register(); // ri-tenta l'aggancio a %player_flag%
    }

    /** Messaggio di benvenuto: solo la prima volta, al primo amministratore che entra. */
    void maybeWelcome(Player p) {
        FileConfiguration cfg = getConfig();
        if (!cfg.getBoolean("welcome.enabled", true) || flags.welcomeShown()) return;
        boolean admin = p.hasPermission(cfg.getString("welcome.permission", "playersflag.admin"))
                || (cfg.getBoolean("welcome.also-ops", true) && p.isOp());
        if (!admin) return;
        flags.markWelcomeShown();
        long delay = Math.max(1, cfg.getLong("welcome.delay-ticks", 40));
        getServer().getScheduler().runTaskLater(this, () -> sendWelcome(p), delay);
    }

    void sendWelcome(Player p) {
        if (!p.isOnline()) return;
        Map<String, String> ph = Map.of(
                "player", p.getName(),
                "version", getPluginMeta().getVersion(),
                "author", "Controll3r24");
        for (String line : getConfig().getStringList("welcome.lines")) {
            p.sendMessage(text.parse(line, ph));
        }
    }

    Text text() { return text; }
    FlagManager flags() { return flags; }
    GeoLocator geo() { return geo; }
}
