package it.controll3r24.playersflag;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

final class PfCommand implements TabExecutor {

    private static final List<String> SUBS = List.of("help", "list", "info", "reload", "set", "reset", "refresh", "welcome");

    private final PlayersFlagPlugin plugin;

    PfCommand(PlayersFlagPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean admin(CommandSender s) {
        if (s.hasPermission("playersflag.admin")) return true;
        s.sendMessage(plugin.text().msg("no-permission", null));
        return false;
    }

    private boolean use(CommandSender s) {
        if (s.hasPermission("playersflag.use") || s.hasPermission("playersflag.admin")) return true;
        s.sendMessage(plugin.text().msg("no-permission", null));
        return false;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c, @NotNull String label, String[] a) {
        String sub = a.length == 0 ? "help" : a[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> {
                if (!admin(s)) return true;
                plugin.reload();
                s.sendMessage(plugin.text().msg("reloaded", null));
            }
            case "list" -> { if (use(s)) list(s, a); }
            case "info" -> { if (use(s)) info(s, a); }
            case "set" -> { if (admin(s)) set(s, a); }
            case "reset" -> { if (admin(s)) reset(s, a); }
            case "refresh" -> { if (admin(s)) refresh(s, a); }
            case "welcome" -> {
                if (!admin(s)) return true;
                if (s instanceof Player p) plugin.sendWelcome(p);
                else s.sendMessage(plugin.text().msg("console-needs-player", null));
            }
            default -> { if (use(s)) help(s); }
        }
        return true;
    }

    private void help(CommandSender s) {
        for (String line : plugin.getConfig().getStringList("messages.help")) {
            s.sendMessage(plugin.text().parse(line));
        }
    }

    private void list(CommandSender s, String[] a) {
        FlagManager fm = plugin.flags();
        List<String> all = fm.allCodes();
        int per = Math.max(1, plugin.getConfig().getInt("list.per-page", 15));
        int pages = (all.size() + per - 1) / per;
        int page = 1;
        if (a.length > 1) {
            try { page = Integer.parseInt(a[1]); } catch (NumberFormatException ignored) { }
        }
        page = Math.max(1, Math.min(pages, page));

        s.sendMessage(plugin.text().parse(plugin.getConfig().getString("messages.list-header", ""),
                Map.of("page", String.valueOf(page), "pages", String.valueOf(pages))));

        String entry = plugin.getConfig().getString("list.entry", "{flag} {code} - {country}{mark}");
        String markOn = plugin.getConfig().getString("list.mark-configured", "");
        int from = (page - 1) * per;
        for (int i = from; i < Math.min(all.size(), from + per); i++) {
            String code = all.get(i);
            Map<String, String> ph = new HashMap<>();
            ph.put("flag", fm.flagFor(code));
            ph.put("code", code);
            ph.put("country", fm.countryName(code));
            ph.put("mark", fm.hasHead(code) ? markOn : "");
            s.sendMessage(plugin.text().parse(entry, ph));
        }
        s.sendMessage(plugin.text().parse(plugin.getConfig().getString("messages.list-footer", "")));
    }

    private void info(CommandSender s, String[] a) {
        UUID id;
        String name;
        if (a.length > 1) {
            OfflinePlayer op = Bukkit.getOfflinePlayerIfCached(a[1]);
            if (op == null) {
                s.sendMessage(plugin.text().msg("player-not-found", Map.of("name", a[1])));
                return;
            }
            id = op.getUniqueId();
            name = op.getName() != null ? op.getName() : a[1];
        } else if (s instanceof Player p) {
            id = p.getUniqueId();
            name = p.getName();
        } else {
            s.sendMessage(plugin.text().msg("console-needs-player", null));
            return;
        }
        FlagManager fm = plugin.flags();
        String code = fm.codeOf(id);
        Map<String, String> ph = new HashMap<>();
        ph.put("player", name);
        ph.put("flag", fm.flagFor(code));
        ph.put("code", code.isBlank() ? "??" : code);
        ph.put("country", fm.countryName(code));
        ph.put("manual", fm.isManual(id) ? plugin.getConfig().getString("messages.info-manual", "") : "");
        s.sendMessage(plugin.text().msg("info", ph));
    }

    private Player target(CommandSender s, String[] a, int idx) {
        if (a.length <= idx) {
            if (s instanceof Player p) return p;
            s.sendMessage(plugin.text().msg("console-needs-player", null));
            return null;
        }
        Player p = Bukkit.getPlayerExact(a[idx]);
        if (p == null) s.sendMessage(plugin.text().msg("player-not-found", Map.of("name", a[idx])));
        return p;
    }

    private void set(CommandSender s, String[] a) {
        if (a.length < 3) { help(s); return; }
        Player p = target(s, a, 1);
        if (p == null) return;
        String code = a[2].toUpperCase(Locale.ROOT);
        if (!FlagManager.isValidCode(code)) {
            s.sendMessage(plugin.text().msg("invalid-code", Map.of("code", a[2])));
            return;
        }
        plugin.flags().set(p.getUniqueId(), code);
        s.sendMessage(plugin.text().msg("set-ok", Map.of(
                "player", p.getName(), "flag", plugin.flags().flagFor(code), "code", code)));
    }

    private void reset(CommandSender s, String[] a) {
        Player p = target(s, a, 1);
        if (p == null) return;
        plugin.flags().reset(p.getUniqueId());
        plugin.flags().refresh(p, true);
        s.sendMessage(plugin.text().msg("reset-ok", Map.of("player", p.getName())));
    }

    private void refresh(CommandSender s, String[] a) {
        Player p = target(s, a, 1);
        if (p == null) return;
        plugin.flags().refresh(p, true);
        s.sendMessage(plugin.text().msg("refresh-ok", Map.of("player", p.getName())));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender s, @NotNull Command c, @NotNull String label, String[] a) {
        List<String> out = new ArrayList<>();
        if (a.length == 1) {
            for (String sub : SUBS) if (sub.startsWith(a[0].toLowerCase(Locale.ROOT))) out.add(sub);
        } else if (a.length == 2 && List.of("set", "reset", "refresh", "info").contains(a[0].toLowerCase(Locale.ROOT))) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(a[1].toLowerCase(Locale.ROOT))) out.add(p.getName());
            }
        } else if (a.length == 3 && a[0].equalsIgnoreCase("set")) {
            for (String code : plugin.flags().allCodes()) {
                if (code.startsWith(a[2].toUpperCase(Locale.ROOT))) out.add(code);
            }
        }
        return out;
    }
}
