package it.controll3r24.playersflag;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.UUID;

final class FlagExpansion extends PlaceholderExpansion {

    private final PlayersFlagPlugin plugin;
    private final String identifier;
    private final PlaceholderExpansion delegate;

    FlagExpansion(PlayersFlagPlugin plugin, String identifier, PlaceholderExpansion delegate) {
        this.plugin = plugin;
        this.identifier = identifier;
        this.delegate = delegate;
    }

    @Override
    public @NotNull String getIdentifier() {
        return identifier;
    }

    @Override
    public @NotNull String getAuthor() {
        return "Controll3r24";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        String key = params.toLowerCase(Locale.ROOT);
        boolean ours = key.equals("flag") || key.startsWith("flag_");
        if (!ours) {
            return delegate != null ? delegate.onRequest(player, params) : null;
        }
        if (player == null) return "";

        UUID id = player.getUniqueId();
        FlagManager fm = plugin.flags();
        String code = fm.codeOf(id);
        return switch (key) {
            case "flag" -> fm.flagFor(code);
            case "flag_code" -> code;
            case "flag_country" -> fm.countryName(code);
            case "flag_emoji" -> FlagManager.emoji(code);
            default -> null;
        };
    }
}
