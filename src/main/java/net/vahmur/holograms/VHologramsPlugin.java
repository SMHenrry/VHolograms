package net.vahmur.holograms;

import de.oliver.fancyholograms.api.FancyHologramsPlugin;
import de.oliver.fancyholograms.api.events.HologramCreateEvent;
import de.oliver.fancyholograms.api.events.HologramDeleteEvent;
import de.oliver.fancyholograms.api.events.HologramHideEvent;
import de.oliver.fancyholograms.api.events.HologramShowEvent;
import de.oliver.fancyholograms.api.events.HologramUpdateEvent;
import de.oliver.fancyholograms.api.events.HologramsLoadedEvent;
import de.oliver.fancyholograms.api.events.HologramsUnloadedEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public final class VHologramsPlugin extends JavaPlugin implements Listener {
    private AdvancedHologramManager manager;

    @Override
    public void onEnable() {
        migrateLegacyDataFolder();
        saveDefaultConfig();

        manager = new AdvancedHologramManager(this);
        manager.loadDefinitions();

        Bukkit.getPluginManager().registerEvents(this, this);

        if (!FancyHologramsPlugin.isEnabled()) {
            getLogger().severe("FancyHolograms no está habilitado. VHolograms se desactivará.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        if (FancyHologramsPlugin.get().getHologramManager().isLoaded()) {
            Bukkit.getScheduler().runTask(this, manager::rebuildAll);
        }

        getLogger().info("VHolograms 0.1.0 habilitado.");
    }

    private void migrateLegacyDataFolder() {
        File newFolder = getDataFolder();
        File pluginsFolder = newFolder.getParentFile();

        if (newFolder.exists() || pluginsFolder == null) {
            return;
        }

        File legacyFolder = new File(pluginsFolder, "VahmurHolograms");
        if (!legacyFolder.isDirectory()) {
            return;
        }

        try {
            Files.move(legacyFolder.toPath(), newFolder.toPath());
            getLogger().info("Configuración migrada de VahmurHolograms a VHolograms.");
        } catch (IOException exception) {
            getLogger().warning("No se pudo migrar automáticamente la carpeta VahmurHolograms: " + exception.getMessage());
        }
    }

    @Override
    public void onDisable() {
        if (manager != null) manager.destroyAll();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onHologramsLoaded(HologramsLoadedEvent event) {
        Bukkit.getScheduler().runTask(this, manager::rebuildAll);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onHologramsUnloaded(HologramsUnloadedEvent event) {
        manager.destroyAll();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShow(HologramShowEvent event) {
        String anchor = event.getHologram().getName();
        if (manager.isConfiguredAnchor(anchor)) {
            manager.showForAnchor(anchor, event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHide(HologramHideEvent event) {
        String anchor = event.getHologram().getName();
        if (manager.isConfiguredAnchor(anchor)) {
            manager.hideForAnchor(anchor, event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onUpdate(HologramUpdateEvent event) {
        String anchor = event.getHologram().getName();
        if (manager.isConfiguredAnchor(anchor)) {
            Bukkit.getScheduler().runTask(this, () -> manager.rebuildAnchor(anchor));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCreate(HologramCreateEvent event) {
        String anchor = event.getHologram().getName();
        if (manager.isConfiguredAnchor(anchor)) {
            Bukkit.getScheduler().runTask(this, () -> manager.rebuildAnchor(anchor));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDelete(HologramDeleteEvent event) {
        String anchor = event.getHologram().getName();
        if (manager.isConfiguredAnchor(anchor)) {
            manager.destroyAnchor(anchor);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("vholograms.admin")) {
            sender.sendMessage("§cNo tienes permiso para usar este comando.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§6VHolograms §7- §f/vholo <reload|status|rebuild [id]>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                manager.destroyAll();
                reloadConfig();
                manager.loadDefinitions();
                manager.rebuildAll();
                sender.sendMessage("§aVHolograms recargado.");
                return true;
            }
            case "status" -> {
                sender.sendMessage("§6VHolograms §7- §fEstado");
                sender.sendMessage("§7Configurados: §f" + manager.configuredCount());
                sender.sendMessage("§7Habilitados: §f" + manager.enabledCount());
                sender.sendMessage("§7Activos: §f" + manager.activeCount());
                return true;
            }
            case "rebuild" -> {
                if (args.length == 1) {
                    manager.rebuildAll();
                    sender.sendMessage("§aTodos los hologramas avanzados fueron reconstruidos.");
                    return true;
                }

                String anchor = manager.anchorForId(args[1]);
                if (anchor == null) {
                    sender.sendMessage("§cNo existe un grupo con id '" + args[1] + "'.");
                    return true;
                }

                manager.rebuildAnchor(anchor);
                sender.sendMessage("§aGrupo '" + args[1] + "' reconstruido.");
                return true;
            }
            default -> {
                sender.sendMessage("§cSubcomando desconocido. Usa /vholo <reload|status|rebuild [id]>.");
                return true;
            }
        }
    }
}
