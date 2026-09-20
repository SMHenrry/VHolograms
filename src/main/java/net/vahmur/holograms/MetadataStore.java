package net.vahmur.holograms;

import de.oliver.fancyholograms.api.FancyHologramsPlugin;
import de.oliver.fancyholograms.api.data.TextHologramData;
import de.oliver.fancyholograms.api.hologram.Hologram;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

final class MetadataStore {
    private final VHologramsPlugin plugin;
    private final File file;
    private final Map<String, HologramStyle> styles = new LinkedHashMap<>();
    private final Map<String, List<String>> pendingLegacyText = new HashMap<>();
    private final Map<String, List<String>> legacyConfigIdsByHologram = new HashMap<>();
    private YamlConfiguration yaml;

    MetadataStore(VHologramsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
    }

    void load() {
        styles.clear();
        pendingLegacyText.clear();
        legacyConfigIdsByHologram.clear();

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
                        if (!lineStyle.isEmpty()) style.lines().put(line, lineStyle);
                    }
                }

                if (style.isManaged()) {
                    styles.put(key(hologramName), style);
                }
            }
        }

        importLegacyConfig();
        save();
    }

    private void importLegacyConfig() {
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("holograms");
        if (root == null) return;

        for (String legacyId : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(legacyId);
            if (section == null || !section.isList("lines")) continue;

            String hologramName = section.getString("anchor", legacyId);
            HologramStyle style = styles.computeIfAbsent(key(hologramName), ignored -> new HologramStyle(hologramName));

            if (section.contains("line_height")) style.setLineHeight(section.getDouble("line_height"));
            if (section.contains("start_y")) style.setStartY(section.getDouble("start_y"));

            List<String> texts = new ArrayList<>();
            List<Map<?, ?>> legacyLines = section.getMapList("lines");
            for (int i = 0; i < legacyLines.size(); i++) {
                Map<?, ?> map = legacyLines.get(i);
                Object text = map.get("text");
                texts.add(text == null ? "<reset>" : String.valueOf(text));

                LineStyle lineStyle = LineStyle.fromLegacyMap(map);
                if (!lineStyle.isEmpty()) style.lines().put(i + 1, lineStyle);
            }

            if (!texts.isEmpty()) {
                pendingLegacyText.put(key(hologramName), List.copyOf(texts));
                legacyConfigIdsByHologram.computeIfAbsent(key(hologramName), ignored -> new ArrayList<>()).add(legacyId);
            }
        }
    }

    boolean restoreLegacyTextIfNeeded(Hologram hologram) {
        List<String> legacyText = pendingLegacyText.get(key(hologram.getName()));
        if (legacyText == null || !(hologram.getData() instanceof TextHologramData textData)) {
            return false;
        }

        if (isEffectivelyEmpty(textData.getText())) {
            textData.setText(legacyText);
            hologram.queueUpdate();
            FancyHologramsPlugin.get().getHologramStorage().save(hologram);
            plugin.getLogger().info("Migrado el texto legacy de '" + hologram.getName() + "' a FancyHolograms.");
        } else {
            plugin.getLogger().info("'" + hologram.getName() + "' ya tiene texto en FancyHolograms; se conservó ese texto durante la migración.");
        }

        pendingLegacyText.remove(key(hologram.getName()));
        List<String> ids = legacyConfigIdsByHologram.remove(key(hologram.getName()));
        if (ids != null) {
            for (String id : ids) {
                plugin.getConfig().set("holograms." + id, null);
            }
            ConfigurationSection remaining = plugin.getConfig().getConfigurationSection("holograms");
            if (remaining != null && remaining.getKeys(false).isEmpty()) {
                plugin.getConfig().set("holograms", null);
            }
            plugin.saveConfig();
        }

        save();
        return true;
    }

    private boolean isEffectivelyEmpty(List<String> lines) {
        if (lines == null || lines.isEmpty()) return true;
        for (String line : lines) {
            if (line == null) continue;
            String value = line.trim();
            if (value.isEmpty() || value.equalsIgnoreCase("<reset>") || value.equalsIgnoreCase("&r")) continue;
            return false;
        }
        return true;
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
        HologramStyle style = styles.computeIfAbsent(key(hologramName), ignored -> new HologramStyle(hologramName));
        LineStyle lineStyle = style.lineOrCreate(line).copy();
        patch.apply(lineStyle);

        if (lineStyle.isEmpty()) style.removeLineStyle(line);
        else style.lines().put(line, lineStyle);

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
        pendingLegacyText.remove(key(hologramName));
        legacyConfigIdsByHologram.remove(key(hologramName));
        save();
    }

    void prune(String hologramName, int maxLine) {
        HologramStyle style = styles.get(key(hologramName));
        if (style == null) return;
        int before = style.lines().size();
        style.pruneAfter(maxLine);
        cleanup(hologramName);
        if (style.lines().size() != before) save();
    }

    private void cleanup(String hologramName) {
        HologramStyle style = styles.get(key(hologramName));
        if (style != null && !style.isManaged()) {
            styles.remove(key(hologramName));
        }
    }

    void save() {
        if (yaml == null) yaml = new YamlConfiguration();
        yaml.set("holograms", null);

        for (HologramStyle style : styles.values()) {
            String base = "holograms." + style.hologramName();
            if (style.rawLineHeight() != null) yaml.set(base + ".line_height", style.rawLineHeight());
            if (style.rawStartY() != null) yaml.set(base + ".start_y", style.rawStartY());

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
