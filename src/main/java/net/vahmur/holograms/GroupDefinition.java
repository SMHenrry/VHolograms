package net.vahmur.holograms;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class GroupDefinition {
    final String id;
    final boolean enabled;
    final String anchor;
    final double startY;
    final double lineHeight;
    final List<LineSpec> lines;

    private GroupDefinition(String id, boolean enabled, String anchor, double startY, double lineHeight, List<LineSpec> lines) {
        this.id = id;
        this.enabled = enabled;
        this.anchor = anchor;
        this.startY = startY;
        this.lineHeight = lineHeight;
        this.lines = lines;
    }

    static GroupDefinition read(String id, ConfigurationSection section, double defaultLineHeight) {
        boolean enabled = section.getBoolean("enabled", true);
        String anchor = section.getString("anchor", id);
        double startY = section.getDouble("start_y", 0.0D);
        double lineHeight = section.getDouble("line_height", defaultLineHeight);

        List<LineSpec> lines = new ArrayList<>();
        for (Map<?, ?> map : section.getMapList("lines")) {
            lines.add(LineSpec.from(map));
        }

        return new GroupDefinition(id, enabled, anchor, startY, lineHeight, List.copyOf(lines));
    }
}
