package it.controll3r24.playersflag;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Cache dei paesi dei player + costruzione della stringa bandiera. Gli IP NON vengono salvati. */
final class FlagManager {

    private static final Set<String> ISO = new HashSet<>(Arrays.asList(Locale.getISOCountries()));

    private final PlayersFlagPlugin plugin;
    private final File file;
    private final Map<UUID, String> codes = new ConcurrentHashMap<>();
    private final Map<UUID, Long> times = new ConcurrentHashMap<>();
    private final Set<UUID> manual = ConcurrentHashMap.newKeySet();
    private volatile boolean welcomeShown;

    FlagManager(PlayersFlagPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
    }

    // ---------- persistenza ----------

    void load() {
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        welcomeShown = y.getBoolean("welcome-shown", false);
        ConfigurationSection ps = y.getConfigurationSection("players");
        if (ps == null) return;
        for (String key : ps.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                String code = ps.getString(key + ".code");
                if (code == null) continue;
                codes.put(id, code.toUpperCase(Locale.ROOT));
                times.put(id, ps.getLong(key + ".time", 0L));
                if (ps.getBoolean(key + ".manual", false)) manual.add(id);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    synchronized void save() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("welcome-shown", welcomeShown);
        for (Map.Entry<UUID, String> e : codes.entrySet()) {
            String base = "players." + e.getKey();
            y.set(base + ".code", e.getValue());
            y.set(base + ".time", times.getOrDefault(e.getKey(), 0L));
            if (manual.contains(e.getKey())) y.set(base + ".manual", true);
        }
        try {
            y.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("Impossibile salvare data.yml: " + ex.getMessage());
        }
    }

    boolean welcomeShown() {
        return welcomeShown;
    }

    void markWelcomeShown() {
        welcomeShown = true;
        save();
    }

    // ---------- dati player ----------

    String codeOf(UUID id) {
        String c = codes.get(id);
        if (c != null) return c;
        return plugin.getConfig().getString("geolocation.default-flag-code", "").toUpperCase(Locale.ROOT);
    }

    boolean isManual(UUID id) {
        return manual.contains(id);
    }

    void set(UUID id, String code) {
        codes.put(id, code.toUpperCase(Locale.ROOT));
        times.put(id, System.currentTimeMillis());
        manual.add(id);
        save();
    }

    void reset(UUID id) {
        codes.remove(id);
        times.remove(id);
        manual.remove(id);
        save();
    }

    /** Avvia (in async) la ricerca del paese per il player, se serve. */
    void refresh(Player p, boolean force) {
        FileConfiguration cfg = plugin.getConfig();
        if (!cfg.getBoolean("geolocation.enabled", true)) return;
        UUID id = p.getUniqueId();
        if (manual.contains(id) && !force) return;

        long maxAge = cfg.getLong("geolocation.cache-days", 30) * 86_400_000L;
        Long t = times.get(id);
        if (!force && t != null && System.currentTimeMillis() - t < maxAge) return;

        InetSocketAddress sa = p.getAddress();
        if (sa == null || sa.getAddress() == null) return;
        InetAddress ia = sa.getAddress();
        if (ia.isAnyLocalAddress() || ia.isLoopbackAddress() || ia.isSiteLocalAddress() || ia.isLinkLocalAddress()) {
            return; // IP locale: resta il codice di default
        }

        plugin.geo().lookup(ia.getHostAddress(), res -> res.ifPresent(code -> {
            codes.put(id, code);
            times.put(id, System.currentTimeMillis());
            manual.remove(id);
            save();
        }));
    }

    // ---------- bandiere ----------

    static boolean isValidCode(String code) {
        return code != null && ISO.contains(code.toUpperCase(Locale.ROOT));
    }

    List<String> allCodes() {
        return List.copyOf(new TreeSet<>(ISO));
    }

    boolean hasHead(String code) {
        String v = plugin.getConfig().getString("flags." + code.toUpperCase(Locale.ROOT));
        return v != null && !v.isBlank();
    }

    String countryName(String code) {
        if (code == null || code.isBlank()) return "Unknown";
        Locale lang = Locale.forLanguageTag(plugin.getConfig().getString("country-names-language", "en"));
        return Locale.of("", code.toUpperCase(Locale.ROOT)).getDisplayCountry(lang);
    }

    static String emoji(String code) {
        if (code == null || code.length() != 2) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : code.toUpperCase(Locale.ROOT).toCharArray()) {
            if (c < 'A' || c > 'Z') return "";
            sb.appendCodePoint(0x1F1E6 + (c - 'A'));
        }
        return sb.toString();
    }

    /** Stringa finale (MiniMessage) della bandiera: <head:...> oppure emoji oppure testo "sconosciuto". */
    String flagFor(String code) {
        FileConfiguration cfg = plugin.getConfig();
        String unknown = cfg.getString("fallback.unknown-text", "?");
        if (code == null || code.isBlank()) return unknown;
        String upper = code.toUpperCase(Locale.ROOT);
        String value = cfg.getString("flags." + upper);
        if (value != null && !value.isBlank()) {
            return cfg.getString("head.format", "<head:%value%>").replace("%value%", value);
        }
        if (cfg.getBoolean("fallback.emoji", true)) {
            String e = emoji(upper);
            if (!e.isEmpty()) return e;
        }
        return unknown;
    }
}
