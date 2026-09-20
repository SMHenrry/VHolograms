package net.vahmur.holograms;

import de.oliver.fancyholograms.api.FancyHologramsPlugin;
import de.oliver.fancyholograms.api.data.TextHologramData;
import de.oliver.fancyholograms.api.hologram.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.TabCompleteEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class FancyCommandBridge implements Listener {
    private static final Set<String> FANCY_ALIASES = Set.of("hologram", "holograms", "holo", "fholo");
    private static final List<String> FLAGS = List.of(
            "--scale", "--scale-x", "--scale-y", "--scale-z",
            "--offset-x", "--offset-y", "--offset-z",
            "--gap-before", "--gap-after",
            "--text-shadow", "--see-through"
    );

    private final VHologramsPlugin plugin;
    private final MetadataStore store;
    private final AdvancedHologramManager manager;
    private boolean redispatching;

    FancyCommandBridge(VHologramsPlugin plugin, MetadataStore store, AdvancedHologramManager manager) {
        this.plugin = plugin;
        this.store = store;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (redispatching) return;

        ParsedCommand parsed;
        try {
            parsed = parse(event.getMessage());
        } catch (IllegalArgumentException exception) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cVHolograms: " + exception.getMessage());
            return;
        }

        if (parsed == null || !shouldIntercept(parsed)) return;

        event.setCancelled(true);
        execute(event.getPlayer(), parsed);
    }

    private boolean shouldIntercept(ParsedCommand parsed) {
        return switch (parsed.action) {
            case "setline", "addline" -> parsed.patch != null;
            case "insertbefore", "insertafter" -> parsed.patch != null || store.isManaged(parsed.hologramName);
            case "removeline" -> store.isManaged(parsed.hologramName);
            default -> false;
        };
    }

    private void execute(Player player, ParsedCommand parsed) {
        Hologram hologram = FancyHologramsPlugin.get().getHologramManager().getHologram(parsed.hologramName).orElse(null);
        if (hologram == null) {
            player.sendMessage("§cVHolograms: no existe el holograma '" + parsed.hologramName + "'.");
            return;
        }
        if (!(hologram.getData() instanceof TextHologramData beforeData)) {
            player.sendMessage("§cVHolograms: las opciones por línea solo funcionan con hologramas TEXT.");
            return;
        }

        int oldSize = beforeData.getText().size();
        boolean success;
        redispatching = true;
        try {
            success = Bukkit.dispatchCommand(player, parsed.sanitizedCommand);
        } finally {
            redispatching = false;
        }

        if (!success) return;

        Hologram after = FancyHologramsPlugin.get().getHologramManager().getHologram(parsed.hologramName).orElse(hologram);
        int newSize = after.getData() instanceof TextHologramData textData ? textData.getText().size() : oldSize;

        switch (parsed.action) {
            case "setline" -> {
                if (parsed.patch != null) {
                    int line = parsed.lineNumber <= oldSize ? parsed.lineNumber : newSize;
                    if (line >= 1) store.applyPatch(parsed.hologramName, line, parsed.patch);
                }
            }
            case "addline" -> {
                if (parsed.patch != null && newSize >= 1) {
                    store.applyPatch(parsed.hologramName, newSize, parsed.patch);
                }
            }
            case "insertbefore" -> {
                int inserted = Math.min(Math.max(1, parsed.lineNumber), oldSize + 1);
                if (store.isManaged(parsed.hologramName)) store.shiftForInsert(parsed.hologramName, inserted);
                if (parsed.patch != null) store.applyPatch(parsed.hologramName, inserted, parsed.patch);
            }
            case "insertafter" -> {
                int inserted = Math.min(Math.max(1, parsed.lineNumber + 1), oldSize + 1);
                if (store.isManaged(parsed.hologramName)) store.shiftForInsert(parsed.hologramName, inserted);
                if (parsed.patch != null) store.applyPatch(parsed.hologramName, inserted, parsed.patch);
            }
            case "removeline" -> {
                if (parsed.lineNumber >= 1 && parsed.lineNumber <= oldSize) {
                    store.shiftForRemoval(parsed.hologramName, parsed.lineNumber);
                }
            }
            default -> { }
        }

        manager.scheduleRebuild(parsed.hologramName);
    }

    private ParsedCommand parse(String rawCommand) {
        String command = rawCommand.startsWith("/") ? rawCommand.substring(1) : rawCommand;
        List<String> tokens = tokenize(command);
        if (tokens.size() < 4) return null;
        if (!FANCY_ALIASES.contains(tokens.get(0).toLowerCase(Locale.ROOT))) return null;
        if (!tokens.get(1).equalsIgnoreCase("edit")) return null;

        String hologram = tokens.get(2);
        String action = tokens.get(3).toLowerCase(Locale.ROOT);
        if (!Set.of("setline", "addline", "insertbefore", "insertafter", "removeline").contains(action)) return null;

        int lineNumber = -1;
        int textStart;
        if (action.equals("addline")) {
            textStart = 4;
        } else {
            if (tokens.size() < 5) return null;
            try {
                lineNumber = Integer.parseInt(tokens.get(4));
            } catch (NumberFormatException ignored) {
                return null;
            }
            textStart = action.equals("removeline") ? tokens.size() : 5;
        }

        if (action.equals("removeline")) {
            return new ParsedCommand(hologram, action, lineNumber, null, String.join(" ", tokens));
        }

        int firstFlag = -1;
        for (int i = textStart; i < tokens.size(); i++) {
            if (isFlag(tokens.get(i))) {
                firstFlag = i;
                break;
            }
        }

        if (firstFlag < 0) {
            return new ParsedCommand(hologram, action, lineNumber, null, String.join(" ", tokens));
        }

        if (firstFlag == textStart) {
            throw new IllegalArgumentException("debes escribir el texto de la línea antes de las opciones de VHolograms.");
        }

        StylePatch patch = new StylePatch();
        int cursor = firstFlag;
        while (cursor < tokens.size()) {
            String flag = tokens.get(cursor);
            if (!isFlag(flag)) {
                throw new IllegalArgumentException("las opciones de VHolograms deben ir al final del comando.");
            }
            if (cursor + 1 >= tokens.size()) {
                throw new IllegalArgumentException("falta el valor de " + flag + ".");
            }
            String value = tokens.get(cursor + 1);
            patch.set(flag, value);
            cursor += 2;
        }

        String sanitized = String.join(" ", tokens.subList(0, firstFlag));
        return new ParsedCommand(hologram, action, lineNumber, patch, sanitized);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTabComplete(TabCompleteEvent event) {
        if (!(event.getSender() instanceof Player)) return;

        List<String> suggestions = completions(event.getBuffer());
        if (suggestions.isEmpty()) return;

        LinkedHashSet<String> merged = new LinkedHashSet<>(event.getCompletions());
        merged.addAll(suggestions);
        event.setCompletions(new ArrayList<>(merged));
    }

    private List<String> completions(String buffer) {
        String command = buffer.startsWith("/") ? buffer.substring(1) : buffer;
        boolean trailingSpace = command.endsWith(" ");
        List<String> tokens = tokenize(command);
        if (trailingSpace) tokens.add("");

        if (tokens.size() < 4) return List.of();
        if (!FANCY_ALIASES.contains(tokens.get(0).toLowerCase(Locale.ROOT))) return List.of();
        if (!tokens.get(1).equalsIgnoreCase("edit")) return List.of();

        String action = tokens.get(3).toLowerCase(Locale.ROOT);
        int textStart;
        if (action.equals("addline")) {
            textStart = 4;
        } else if (Set.of("setline", "insertbefore", "insertafter").contains(action)) {
            if (tokens.size() < 5) return List.of();
            textStart = 5;
        } else {
            return List.of();
        }

        if (tokens.size() <= textStart) return List.of();

        int currentIndex = tokens.size() - 1;
        String current = tokens.get(currentIndex);
        String previous = currentIndex > 0 ? tokens.get(currentIndex - 1) : "";

        if (isFlag(previous)) {
            return valueSuggestions(previous, current);
        }

        int firstFlag = -1;
        for (int i = textStart; i < tokens.size(); i++) {
            if (isFlag(tokens.get(i))) {
                firstFlag = i;
                break;
            }
        }

        boolean hasText = firstFlag < 0 ? currentIndex >= textStart : firstFlag > textStart;
        if (!hasText) return List.of();

        if (!current.isEmpty() && !current.startsWith("--")) return List.of();

        Set<String> used = new LinkedHashSet<>();
        for (String token : tokens) {
            if (isFlag(token)) used.add(token.toLowerCase(Locale.ROOT));
        }

        String prefix = current.toLowerCase(Locale.ROOT);
        return FLAGS.stream()
                .filter(flag -> !used.contains(flag) || flag.equals(prefix))
                .filter(flag -> flag.startsWith(prefix))
                .toList();
    }

    private List<String> valueSuggestions(String flag, String prefix) {
        List<String> values = switch (flag.toLowerCase(Locale.ROOT)) {
            case "--scale", "--scale-x", "--scale-y", "--scale-z" ->
                    List.of("0.5", "0.75", "1.0", "1.25", "1.5", "2.0", "inherit");
            case "--offset-x", "--offset-y", "--offset-z" ->
                    List.of("-0.25", "-0.15", "-0.10", "0", "0.10", "0.15", "0.25", "inherit");
            case "--gap-before", "--gap-after" ->
                    List.of("-0.10", "0", "0.05", "0.10", "0.15", "inherit");
            case "--text-shadow", "--see-through" -> List.of("true", "false", "inherit");
            default -> List.of();
        };

        String lower = prefix.toLowerCase(Locale.ROOT);
        return values.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lower)).toList();
    }

    private boolean isFlag(String value) {
        return FLAGS.contains(value.toLowerCase(Locale.ROOT));
    }

    private static List<String> tokenize(String command) {
        if (command == null || command.isBlank()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(command.trim().split("\\s+")));
    }

    private record ParsedCommand(
            String hologramName,
            String action,
            int lineNumber,
            StylePatch patch,
            String sanitizedCommand
    ) { }
}
