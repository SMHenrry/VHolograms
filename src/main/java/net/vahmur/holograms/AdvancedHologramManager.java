package net.vahmur.holograms;

import de.oliver.fancyholograms.api.FancyHologramsPlugin;
import de.oliver.fancyholograms.api.HologramManager;
import de.oliver.fancyholograms.api.data.TextHologramData;
import de.oliver.fancyholograms.api.data.property.Visibility;
import de.oliver.fancyholograms.api.events.HologramCreateEvent;
import de.oliver.fancyholograms.api.events.HologramDeleteEvent;
import de.oliver.fancyholograms.api.events.HologramHideEvent;
import de.oliver.fancyholograms.api.events.HologramShowEvent;
import de.oliver.fancyholograms.api.events.HologramUpdateEvent;
import de.oliver.fancyholograms.api.events.HologramsLoadedEvent;
import de.oliver.fancyholograms.api.events.HologramsUnloadedEvent;
import de.oliver.fancyholograms.api.hologram.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class AdvancedHologramManager implements Listener {
    private static final long JOIN_SYNC_DELAY_TICKS = 10L;

    private final VHologramsPlugin plugin;
    private final MetadataStore store;
    private final Map<String, ActiveGroup> active = new ConcurrentHashMap<>();
    private final Set<String> pendingRebuilds = ConcurrentHashMap.newKeySet();
    private final Set<String> restoringParents = ConcurrentHashMap.newKeySet();

    AdvancedHologramManager(VHologramsPlugin plugin, MetadataStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    void rebuildAll() {
        for (String name : Set.copyOf(active.keySet())) {
            destroyGroup(name);
        }

        if (!fancyManager().isLoaded()) return;

        for (HologramStyle style : store.all()) {
            rebuild(style.hologramName());
        }
    }

    void scheduleRebuild(String hologramName) {
        String key = key(hologramName);
        if (!pendingRebuilds.add(key)) return;

        Bukkit.getScheduler().runTask(plugin, () -> {
            pendingRebuilds.remove(key);
            rebuild(hologramName);
        });
    }

    void rebuild(String hologramName) {
        HologramStyle style = store.get(hologramName);
        if (style == null || !style.isManaged()) {
            unmanageAndRestore(hologramName);
            return;
        }

        Hologram parent = fancyManager().getHologram(hologramName).orElse(null);
        if (parent == null) {
            destroyGroup(hologramName);
            return;
        }

        if (!(parent.getData() instanceof TextHologramData parentData)) {
            plugin.getLogger().warning("VHolograms solo puede aplicar estilos por línea a hologramas TEXT: " + hologramName);
            unmanageAndRestore(hologramName);
            return;
        }

        List<String> lines = parentData.getText();
        store.prune(hologramName, lines.size());
        style = store.get(hologramName);
        if (style == null || !style.isManaged()) {
            unmanageAndRestore(hologramName);
            return;
        }

        String key = key(hologramName);
        ActiveGroup previous = active.remove(key);
        Set<UUID> viewers = new HashSet<>(parent.getViewers());
        boolean firstAdoption = previous == null;

        if (previous != null) {
            previous.destroy(fancyManager());
        }

        if (firstAdoption) {
            // The parent is currently visible. Despawn it once, then keep its viewer
            // bookkeeping so FancyHolograms can continue driving visibility normally.
            for (UUID uuid : viewers) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null) continue;
                parent.forceHideHologram(player);
                parent.showHologram(player);
            }
        }

        ActiveGroup group = buildGroup(parent, parentData, style, lines);
        active.put(key, group);

        for (Player player : Bukkit.getOnlinePlayers()) {
            reconcile(parent, group, player, false);
        }
    }

    private ActiveGroup buildGroup(Hologram parent, TextHologramData parentData, HologramStyle style, List<String> lines) {
        ActiveGroup group = new ActiveGroup(parent.getName());
        Vector3f parentScale = parentData.getScale();
        Vector3f parentTranslation = parentData.getTranslation();
        double lineHeight = style.lineHeight(plugin.getConfig().getDouble("settings.default_line_height", 0.24D));
        double cursor = 0.0D;

        for (int index = 0; index < lines.size(); index++) {
            int lineNumber = index + 1;
            LineStyle lineStyle = style.line(lineNumber);
            if (lineStyle == null) lineStyle = new LineStyle();

            cursor += lineStyle.gapBefore();

            Location location = parentData.getLocation().clone();
            TextHologramData childData = new TextHologramData(internalName(parent.getName(), lineNumber), location);
            childData.setPersistent(false);
            childData.setVisibility(Visibility.MANUAL);
            childData.setVisibilityDistance(parentData.getVisibilityDistance());

            childData.setBillboard(parentData.getBillboard());
            childData.setBrightness(parentData.getBrightness());
            childData.setShadowRadius(parentData.getShadowRadius());
            childData.setShadowStrength(parentData.getShadowStrength());
            childData.setInterpolationDuration(parentData.getInterpolationDuration());

            childData.setScale(new Vector3f(
                    parentScale.x() * lineStyle.scaleX(),
                    parentScale.y() * lineStyle.scaleY(),
                    parentScale.z() * lineStyle.scaleZ()
            ));

            childData.setTranslation(new Vector3f(
                    parentTranslation.x() + lineStyle.offsetX(),
                    parentTranslation.y() + (float) (style.startY() - cursor) + lineStyle.offsetY(),
                    parentTranslation.z() + lineStyle.offsetZ()
            ));

            childData.setText(List.of(lines.get(index)));
            childData.setTextAlignment(parentData.getTextAlignment());
            childData.setBackground(parentData.getBackground());
            childData.setTextShadow(lineStyle.textShadow() != null ? lineStyle.textShadow() : parentData.hasTextShadow());
            childData.setSeeThrough(lineStyle.seeThrough() != null ? lineStyle.seeThrough() : parentData.isSeeThrough());
            childData.setTextUpdateInterval(parentData.getTextUpdateInterval());

            Hologram child = fancyManager().create(childData);
            fancyManager().addHologram(child);
            group.children.add(child);

            cursor += lineHeight * Math.max(0.05D, lineStyle.scaleY());
            cursor += lineStyle.gapAfter();
        }

        return group;
    }

    private void syncPlayer(Player player, boolean forceResend) {
        if (!player.isOnline()) return;

        for (ActiveGroup group : List.copyOf(active.values())) {
            Hologram parent = fancyManager().getHologram(group.hologramName).orElse(null);
            if (parent == null) continue;

            reconcile(parent, group, player, forceResend);
        }
    }

    private void reconcile(Hologram parent, ActiveGroup group, Player player, boolean forceResend) {
        boolean shouldSee = parent.meetsVisibilityConditions(player)
                && parent.isWithinVisibilityDistance(player);

        if (!shouldSee) {
            group.hide(player);

            if (forceResend && parent.isViewer(player)) {
                parent.forceHideHologram(player);
            }
            return;
        }

        if (forceResend) {
            // A login can happen while the client is still receiving its initial
            // world state. Re-send the displays once after a short delay so a
            // packet sent too early cannot leave Fancy and the client out of sync.
            if (parent.isViewer(player)) {
                parent.forceHideHologram(player);
            }

            parent.showHologram(player);
            group.resync(player);
            return;
        }

        if (!parent.isViewer(player)) {
            parent.showHologram(player);
        }

        group.show(player);
    }

    void unmanageAndRestore(String hologramName) {
        String key = key(hologramName);
        ActiveGroup group = active.remove(key);
        if (group == null) return;

        Hologram parent = fancyManager().getHologram(hologramName).orElse(null);
        Set<UUID> viewers = parent == null ? Set.of() : new HashSet<>(parent.getViewers());
        group.destroy(fancyManager());

        if (parent == null || viewers.isEmpty()) return;

        restoringParents.add(key);
        try {
            for (UUID uuid : viewers) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) parent.forceShowHologram(player);
            }
        } finally {
            restoringParents.remove(key);
        }
    }

    void destroyGroup(String hologramName) {
        ActiveGroup group = active.remove(key(hologramName));
        if (group != null) group.destroy(fancyManager());
    }

    void destroyAll(boolean restoreParents) {
        for (String name : Set.copyOf(active.keySet())) {
            if (restoreParents) unmanageAndRestore(name);
            else destroyGroup(name);
        }
    }

    int activeCount() {
        return active.size();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onShow(HologramShowEvent event) {
        String name = event.getHologram().getName();
        String key = key(name);
        if (restoringParents.contains(key)) return;

        ActiveGroup group = active.get(key);
        if (group == null || !store.isManaged(name)) return;

        // Suppress only the original multi-line TextDisplay. The text remains intact
        // inside FancyHolograms and is still the source of truth.
        event.setCancelled(true);
        event.getHologram().showHologram(event.getPlayer());
        group.show(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHide(HologramHideEvent event) {
        ActiveGroup group = active.get(key(event.getHologram().getName()));
        if (group != null) group.hide(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onUpdate(HologramUpdateEvent event) {
        String name = event.getHologram().getName();
        if (store.isManaged(name)) scheduleRebuild(name);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCreate(HologramCreateEvent event) {
        String name = event.getHologram().getName();
        if (store.isManaged(name)) scheduleRebuild(name);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDelete(HologramDeleteEvent event) {
        String name = event.getHologram().getName();
        if (active.containsKey(key(name)) || store.isManaged(name)) {
            destroyGroup(name);
            store.removeHologram(name);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLoaded(HologramsLoadedEvent event) {
        Bukkit.getScheduler().runTask(plugin, this::rebuildAll);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onUnloaded(HologramsUnloadedEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> destroyAll(false));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();

        // One-shot reconciliation only. There is no repeating VHolograms task.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                syncPlayer(player, true);
            }
        }, JOIN_SYNC_DELAY_TICKS);
    }

    private String internalName(String hologramName, int line) {
        String safe = hologramName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "_");
        if (safe.length() > 32) safe = safe.substring(0, 32);
        return "__vholo_" + safe + "_" + Integer.toHexString(hologramName.toLowerCase(Locale.ROOT).hashCode()) + "_" + line;
    }

    private HologramManager fancyManager() {
        return FancyHologramsPlugin.get().getHologramManager();
    }

    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
