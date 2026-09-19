package net.vahmur.holograms;

import de.oliver.fancyholograms.api.hologram.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class ActiveGroup {
    final GroupDefinition definition;
    final String anchorName;
    final List<Hologram> children = new ArrayList<>();

    ActiveGroup(GroupDefinition definition) {
        this.definition = definition;
        this.anchorName = definition.anchor;
    }

    void show(Player player) {
        for (Hologram child : children) {
            child.forceShowHologram(player);
        }
    }

    void hide(Player player) {
        for (Hologram child : children) {
            child.forceHideHologram(player);
        }
    }

    void destroy() {
        for (Hologram child : children) {
            for (UUID viewerId : child.getViewers()) {
                Player player = Bukkit.getPlayer(viewerId);
                if (player != null) {
                    child.forceHideHologram(player);
                }
            }
            child.deleteHologram();
        }
        children.clear();
    }
}
