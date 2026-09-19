package net.vahmur.holograms;

import de.oliver.fancyholograms.api.FancyHologramsPlugin;
import de.oliver.fancyholograms.api.HologramManager;
import de.oliver.fancyholograms.api.data.TextHologramData;
import de.oliver.fancyholograms.api.hologram.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

final class AdvancedHologramManager {
    private final VahmurHologramsPlugin plugin;
    private final Logger logger;

    private final Map<String, GroupDefinition> definitionsById = new LinkedHashMap<>();
    private final Map<String, GroupDefinition> definitionsByAnchor = new HashMap<>();
    private final Map<String, ActiveGroup> activeByAnchor = new HashMap<>();

    private double defaultLineHeight = 0.24D;
    private boolean warnVisibleAnchor = true;

    AdvancedHologramManager(VahmurHologramsPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    void loadDefinitions() {
        definitionsById.clear();
        definitionsByAnchor.clear();

        defaultLineHeight = plugin.getConfig().getDouble("settings.default_line_height", 0.24D);
        warnVisibleAnchor = plugin.getConfig().getBoolean("settings.warn_visible_anchor", true);

        ConfigurationSection root = plugin.getConfig().getConfigurationSection("holograms");
        if (root == null) return;

        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) continue;

            GroupDefinition def = GroupDefinition.read(id, section, defaultLineHeight);
            definitionsById.put(id, def);
            if (def.enabled) {
                definitionsByAnchor.put(def.anchor, def);
            }
        }
    }

    void rebuildAll() {
        Set<String> oldAnchors = new HashSet<>(activeByAnchor.keySet());
        for (String anchor : oldAnchors) {
            destroyAnchor(anchor);
        }

        HologramManager fancy = fancyManager();
        if (!fancy.isLoaded()) {
            logger.warning("FancyHolograms todavía no terminó de cargar; se reintentará cuando dispare HologramsLoadedEvent.");
            return;
        }

        for (GroupDefinition def : definitionsById.values()) {
            if (def.enabled) rebuildDefinition(def);
        }
    }

    void rebuildAnchor(String anchorName) {
        GroupDefinition def = definitionsByAnchor.get(anchorName);
        destroyAnchor(anchorName);
        if (def != null && def.enabled) {
            rebuildDefinition(def);
        }
    }

    private void rebuildDefinition(GroupDefinition def) {
        Optional<Hologram> parentOpt = fancyManager().getHologram(def.anchor);
        if (parentOpt.isEmpty()) {
            logger.warning("No existe el anchor de FancyHolograms '" + def.anchor + "' para el grupo '" + def.id + "'.");
            return;
        }

        Hologram parent = parentOpt.get();
        if (!(parent.getData() instanceof TextHologramData parentData)) {
            logger.warning("El anchor '" + def.anchor + "' debe ser un holograma TEXT.");
            return;
        }

        if (def.lines.isEmpty()) {
            logger.warning("El grupo '" + def.id + "' no contiene líneas.");
            return;
        }

        if (warnVisibleAnchor && looksVisible(parentData.getText())) {
            logger.warning("El anchor '" + def.anchor + "' tiene texto visible. Usa text: ['<reset>'] para evitar duplicados.");
        }

        Set<UUID> currentViewers = new HashSet<>(parent.getViewers());
        ActiveGroup group = new ActiveGroup(def);

        Vector3f parentScale = parentData.getScale();
        Vector3f parentTranslation = parentData.getTranslation();

        double cursor = 0.0D;
        int index = 0;

        for (LineSpec line : def.lines) {
            cursor += line.gapBefore;

            Location lineLocation = parentData.getLocation().clone();
            float translateX = parentTranslation.x() + line.offsetX;
            float translateY = parentTranslation.y() + (float) (def.startY - cursor) + line.offsetY;
            float translateZ = parentTranslation.z() + line.offsetZ;

            TextHologramData childData = new TextHologramData(internalName(def.id, index), lineLocation);
            childData.setPersistent(false);
            childData.setVisibility(parentData.getVisibility());
            childData.setVisibilityDistance(parentData.getVisibilityDistance());

            childData.setBillboard(parentData.getBillboard());
            childData.setBrightness(parentData.getBrightness());
            childData.setShadowRadius(parentData.getShadowRadius());
            childData.setShadowStrength(parentData.getShadowStrength());
            childData.setInterpolationDuration(parentData.getInterpolationDuration());

            childData.setScale(new Vector3f(
                    parentScale.x() * line.scaleX,
                    parentScale.y() * line.scaleY,
                    parentScale.z() * line.scaleZ
            ));
            childData.setTranslation(new Vector3f(translateX, translateY, translateZ));

            childData.setText(List.of(line.text));
            childData.setTextAlignment(parentData.getTextAlignment());
            childData.setBackground(parentData.getBackground());
            childData.setTextShadow(line.textShadow != null ? line.textShadow : parentData.hasTextShadow());
            childData.setSeeThrough(line.seeThrough != null ? line.seeThrough : parentData.isSeeThrough());
            childData.setTextUpdateInterval(parentData.getTextUpdateInterval());

            Hologram child = fancyManager().create(childData);
            group.children.add(child);

            cursor += def.lineHeight * Math.max(0.05D, line.scaleY);
            cursor += line.gapAfter;
            index++;
        }

        activeByAnchor.put(def.anchor, group);

        for (UUID viewerId : currentViewers) {
            Player player = Bukkit.getPlayer(viewerId);
            if (player != null) group.show(player);
        }
    }

    private boolean looksVisible(List<String> lines) {
        if (lines == null || lines.isEmpty()) return false;
        for (String line : lines) {
            if (line == null) continue;
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            if (trimmed.equalsIgnoreCase("<reset>") || trimmed.equalsIgnoreCase("&r")) continue;
            return true;
        }
        return false;
    }

    private String internalName(String id, int index) {
        return "__vahmur_adv_" + id + "_" + index;
    }

    void showForAnchor(String anchor, Player player) {
        ActiveGroup group = activeByAnchor.get(anchor);
        if (group != null) group.show(player);
    }

    void hideForAnchor(String anchor, Player player) {
        ActiveGroup group = activeByAnchor.get(anchor);
        if (group != null) group.hide(player);
    }

    void destroyAnchor(String anchor) {
        ActiveGroup group = activeByAnchor.remove(anchor);
        if (group != null) group.destroy();
    }

    void destroyAll() {
        Collection<ActiveGroup> copy = new ArrayList<>(activeByAnchor.values());
        activeByAnchor.clear();
        for (ActiveGroup group : copy) group.destroy();
    }

    int configuredCount() {
        return definitionsById.size();
    }

    int enabledCount() {
        int n = 0;
        for (GroupDefinition def : definitionsById.values()) if (def.enabled) n++;
        return n;
    }

    int activeCount() {
        return activeByAnchor.size();
    }

    boolean isConfiguredAnchor(String anchor) {
        return definitionsByAnchor.containsKey(anchor);
    }

    String anchorForId(String id) {
        GroupDefinition def = definitionsById.get(id);
        return def == null ? null : def.anchor;
    }

    private HologramManager fancyManager() {
        return FancyHologramsPlugin.get().getHologramManager();
    }
}
