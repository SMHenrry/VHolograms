package net.vahmur.holograms;

import de.oliver.fancyholograms.api.HologramManager;
import de.oliver.fancyholograms.api.hologram.Hologram;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

final class ActiveGroup {
    final String hologramName;
    final List<Hologram> children = new ArrayList<>();

    ActiveGroup(String hologramName) {
        this.hologramName = hologramName;
    }

    void show(Player player) {
        for (Hologram child : children) {
            if (!child.isViewer(player)) {
                child.forceShowHologram(player);
            }
        }
    }

    void resync(Player player) {
        for (Hologram child : children) {
            if (child.isViewer(player)) {
                child.forceHideHologram(player);
            }

            child.forceShowHologram(player);
        }
    }

    void hide(Player player) {
        for (Hologram child : children) {
            if (child.isViewer(player)) {
                child.forceHideHologram(player);
            }
        }
    }

    void destroy(HologramManager manager) {
        for (Hologram child : List.copyOf(children)) {
            manager.removeHologram(child);
        }
        children.clear();
    }
}
