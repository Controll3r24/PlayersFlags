package it.controll3r24.playersflag;

import me.clip.placeholderapi.PlaceholderAPIPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import java.util.Optional;

/** Registra %playersflag_...% e (se possibile) anche %player_flag%. */
final class PapiHook {

    private final PlayersFlagPlugin plugin;
    private FlagExpansion own;
    private FlagExpansion playerAlias;

    PapiHook(PlayersFlagPlugin plugin) {
        this.plugin = plugin;
    }

    void register() {
        if (own == null) {
            own = new FlagExpansion(plugin, "playersflag", null);
            own.register();
        }
        if (playerAlias == null && plugin.getConfig().getBoolean("placeholder.override-player", true)) {
            tryPlayerAlias();
        }
    }

    void unregister() {
        if (own != null) own.unregister();
        if (playerAlias != null) playerAlias.unregister();
        own = null;
        playerAlias = null;
    }

    private void tryPlayerAlias() {
        PlaceholderExpansion existing = null;
        try {
            Object mgr = PlaceholderAPIPlugin.getInstance().getLocalExpansionManager();
            Object res = mgr.getClass().getMethod("getExpansion", String.class).invoke(mgr, "player");
            if (res instanceof Optional<?> o) res = o.orElse(null);
            if (res instanceof PlaceholderExpansion pe) existing = pe;
        } catch (Throwable t) {
            plugin.getLogger().warning("Impossibile cercare l'espansione 'player': " + t.getMessage());
        }

        if (existing != null) {
            existing.unregister();
        }
        FlagExpansion alias = new FlagExpansion(plugin, "player", existing);
        if (alias.register()) {
            playerAlias = alias;
            plugin.getLogger().info("%player_flag% attivo" + (existing != null
                    ? " (gli altri %player_*% sono inoltrati all'espansione Player originale)."
                    : ". ATTENZIONE: l'espansione Player non e' installata, %player_name% ecc. non funzioneranno: /papi ecloud download Player"));
        } else {
            plugin.getLogger().warning("Non sono riuscito a registrare %player_flag%. Usa %playersflag_flag%.");
            if (existing != null) existing.register(); // ripristina l'originale
        }
    }
}
