package net.vahmur.holograms;

import de.oliver.fancyholograms.api.FancyHologramsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class VHologramsPlugin extends JavaPlugin {
    private MetadataStore store;
    private AdvancedHologramManager manager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        if (!FancyHologramsPlugin.isEnabled()) {
            getLogger().severe("FancyHolograms no está habilitado. VHolograms se desactivará.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        store = new MetadataStore(this);
        store.load();

        manager = new AdvancedHologramManager(this, store);
        FancyCommandBridge commandBridge = new FancyCommandBridge(this, store, manager);

        Bukkit.getPluginManager().registerEvents(manager, this);
        Bukkit.getPluginManager().registerEvents(commandBridge, this);

        if (FancyHologramsPlugin.get().getHologramManager().isLoaded()) {
            Bukkit.getScheduler().runTask(this, manager::rebuildAll);
        }

        getLogger().info("VHolograms " + getPluginMeta().getVersion() + " habilitado.");
    }

    @Override
    public void onDisable() {
        if (manager != null && FancyHologramsPlugin.isEnabled()) {
            manager.destroyAll(true);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("vholograms.admin")) {
            sender.sendMessage("§cNo tienes permiso para usar este comando.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§6VHolograms §7- §f/vholo <reload|status|rebuild|clear>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                manager.destroyAll(true);
                reloadConfig();
                store.load();
                manager.rebuildAll();
                sender.sendMessage("§aVHolograms recargado.");
                return true;
            }
            case "status" -> {
                sender.sendMessage("§6VHolograms §7- §fEstado");
                sender.sendMessage("§7Hologramas con estilos: §f" + store.all().size());
                sender.sendMessage("§7Grupos activos: §f" + manager.activeCount());
                return true;
            }
            case "rebuild" -> {
                if (args.length == 1) {
                    manager.rebuildAll();
                    sender.sendMessage("§aTodos los hologramas de VHolograms fueron reconstruidos.");
                } else {
                    manager.rebuild(args[1]);
                    sender.sendMessage("§aHolograma '" + args[1] + "' reconstruido.");
                }
                return true;
            }
            case "clear" -> {
                if (args.length < 2) {
                    sender.sendMessage("§cUso: /vholo clear <holograma> [línea]");
                    return true;
                }

                String hologram = args[1];
                boolean changed;
                if (args.length >= 3) {
                    int line;
                    try {
                        line = Integer.parseInt(args[2]);
                    } catch (NumberFormatException exception) {
                        sender.sendMessage("§cLa línea debe ser un número.");
                        return true;
                    }
                    changed = store.clearLine(hologram, line);
                } else {
                    changed = store.clearHologram(hologram);
                }

                if (!changed) {
                    sender.sendMessage("§eNo había estilos de VHolograms para limpiar.");
                    return true;
                }

                manager.scheduleRebuild(hologram);
                sender.sendMessage("§aEstilos de VHolograms eliminados. FancyHolograms vuelve a mostrar el texto original.");
                return true;
            }
            default -> {
                sender.sendMessage("§cSubcomando desconocido. Usa /vholo <reload|status|rebuild|clear>.");
                return true;
            }
        }
    }
}
