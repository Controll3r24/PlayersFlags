package it.controll3r24.playersflag;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.List;
import java.util.Map;

/** Gestisce MiniMessage + il tag personalizzato <gg> (oro sfumato). */
final class Text {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final PlayersFlagPlugin plugin;
    private String open = "<gradient:#FFE259:#FFC400:#B8860B>";

    Text(PlayersFlagPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    void reload() {
        List<String> cols = plugin.getConfig().getStringList("style.gradient");
        if (cols.size() < 2) {
            cols = List.of("#FFE259", "#FFC400", "#B8860B");
        }
        open = "<gradient:" + String.join(":", cols) + ">";
    }

    String expand(String in) {
        return in.replace("<gg>", open).replace("</gg>", "</gradient>");
    }

    Component parse(String in, Map<String, String> placeholders) {
        String s = in;
        if (placeholders != null) {
            for (Map.Entry<String, String> e : placeholders.entrySet()) {
                s = s.replace("{" + e.getKey() + "}", e.getValue());
            }
        }
        return MM.deserialize(expand(s));
    }

    Component parse(String in) {
        return parse(in, null);
    }

    /** Messaggio da config "messages.<key>" con prefisso. */
    Component msg(String key, Map<String, String> placeholders) {
        String raw = plugin.getConfig().getString("messages." + key, "<red>Messaggio mancante: " + key + "</red>");
        String prefix = plugin.getConfig().getString("style.prefix", "");
        return parse(prefix + raw, placeholders);
    }
}
