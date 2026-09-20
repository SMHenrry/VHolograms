package net.vahmur.holograms;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

final class MetadataStore {
    private final VHologramsPlugin plugin;
    private final File file;
    private final Map<String, HologramStyle> styles = new LinkedHashMap<>();
    private YamlConfiguration yaml;

    MetadataStore(VHologramsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
    }

    void load() {
        styles.clear();

        yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("holograms");
        if (root != null) {
            for (String hologramName : root.getKeys(false)) {
                ConfigurationSection section = root.getConfigurationSection(hologramName);
                if (section == null) continue;

                HologramStyle style = new HologramStyle(hologramName);
                if (section.contains("line_height")) style.setLineHeight(section.getDouble("line_height"));
                if (section.contains("start_y")) style.setStartY(section.getDouble("start_y"));

                ConfigurationSection lines = section.getConfigurationSection("lines");
                if (lines != null) {
                    for (String rawLine : lines.getKeys(false)) {
                        int line;
                        try {
                            line = Integer.parseInt(rawLine);
                        } catch (NumberFormatException ignored) {
                            continue;
                        }
                        if (line < 1) continue;

                        ConfigurationSection lineSection = lines.getConfigurationSection(rawLine);
                        if (lineSection == null) continue;

                        LineStyle lineStyle = LineStyle.read(lineSection);
                        if (!lineStyle.isEmpty()) {
                            style.lines().put(line, lineStyle);
                        }
                    }
                }

                if (style.isManaged()) {
                    styles.put(key(hologramName), style);
                }
            }
        }

        save();
    }

    boolean isManaged(String hologramName) {
        HologramStyle style = styles.get(key(hologramName));
        return style != null && style.isManaged();
    }

    HologramStyle get(String hologramName) {
        return styles.get(key(hologramName));
    }

    Collection<HologramStyle> all() {
        return List.copyOf(styles.values());
    }

    void applyPatch(String hologramName, int line, StylePatch patch) {
        if (line < 1) return;

        HologramStyle style = styles.computeIfAbsent(
                key(hologramName),
                ignored -> new HologramStyle(hologramName)
        );

        LineStyle lineStyle = style.lineOrCreate(line).copy();
        patch.apply(lineStyle);

        if (lineStyle.isEmpty()) {
            style.removeLineStyle(line);
        } else {
            style.lines().put(line, lineStyle);
        }

        cleanup(hologramName);
        save();
    }

    void shiftForInsert(String hologramName, int insertedLine) {
        HologramStyle style = styles.get(key(hologramName));
        if (style == null) return;

        style.shiftForInsert(insertedLine);
        save();
    }

    void shiftForRemoval(String hologramName, int removedLine) {
        HologramStyle style = styles.get(key(hologramName));
        if (style == null) return;

        style.shiftForRemoval(removedLine);
        cleanup(hologramName);
        save();
    }

    boolean clearLine(String hologramName, int line) {
        HologramStyle style = styles.get(key(hologramName));
        if (style == null || style.line(line) == null) return false;

        style.removeLineStyle(line);
        cleanup(hologramName);
        save();
        return true;
    }

    boolean clearHologram(String hologramName) {
        HologramStyle removed = styles.remove(key(hologramName));
        if (removed == null) return false;

        save();
        return true;
    }

    void removeHologram(String hologramName) {
        styles.remove(key(hologramName));
        save();
    }

    void prune(String hologramName, int maxLine) {
        HologramStyle style = styles.get(key(hologramName));
        if (style == null) return;

        int before = style.lines().size();
        style.pruneAfter(maxLine);
        cleanup(hologramName);

        if (style.lines().size() != before) {
            save();
        }
    }

    private void cleanup(String hologramName) {
        HologramStyle style = styles.get(key(hologramName));
        if (style != null && !style.isManaged()) {
            styles.remove(key(hologramName));
        }
    }

    void save() {
        if (yaml == null) {
            yaml = new YamlConfiguration();
        }

        yaml.set("holograms", null);

        for (HologramStyle style : styles.values()) {
            String base = "holograms." + style.hologramName();

            if (style.rawLineHeight() != null) {
                yaml.set(base + ".line_height", style.rawLineHeight());
            }
            if (style.rawStartY() != null) {
                yaml.set(base + ".start_y", style.rawStartY());
            }

            for (Map.Entry<Integer, LineStyle> entry : style.lines().entrySet()) {
                ConfigurationSection line = yaml.createSection(base + ".lines." + entry.getKey());
                entry.getValue().write(line);
            }
        }

        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("No se pudo crear la carpeta de VHolograms.");
            }
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "No se pudo guardar data.yml", exception);
        }
    }

    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
