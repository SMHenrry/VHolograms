package net.vahmur.holograms;

import java.util.Map;
import java.util.TreeMap;

final class HologramStyle {
    private final String hologramName;
    private final TreeMap<Integer, LineStyle> lines = new TreeMap<>();
    private Double lineHeight;
    private Double startY;

    HologramStyle(String hologramName) {
        this.hologramName = hologramName;
    }

    String hologramName() {
        return hologramName;
    }

    Map<Integer, LineStyle> lines() {
        return lines;
    }

    LineStyle line(int index) {
        return lines.get(index);
    }

    LineStyle lineOrCreate(int index) {
        return lines.computeIfAbsent(index, ignored -> new LineStyle());
    }

    void removeLineStyle(int index) {
        lines.remove(index);
    }

    boolean isManaged() {
        return !lines.isEmpty();
    }

    double lineHeight(double fallback) {
        return lineHeight == null ? fallback : lineHeight;
    }

    double startY() {
        return startY == null ? 0.0D : startY;
    }

    Double rawLineHeight() { return lineHeight; }
    Double rawStartY() { return startY; }
    void setLineHeight(Double lineHeight) { this.lineHeight = lineHeight; }
    void setStartY(Double startY) { this.startY = startY; }

    void shiftForInsert(int insertedLine) {
        TreeMap<Integer, LineStyle> shifted = new TreeMap<>();
        for (Map.Entry<Integer, LineStyle> entry : lines.entrySet()) {
            int line = entry.getKey();
            shifted.put(line >= insertedLine ? line + 1 : line, entry.getValue());
        }
        lines.clear();
        lines.putAll(shifted);
    }

    void shiftForRemoval(int removedLine) {
        TreeMap<Integer, LineStyle> shifted = new TreeMap<>();
        for (Map.Entry<Integer, LineStyle> entry : lines.entrySet()) {
            int line = entry.getKey();
            if (line == removedLine) continue;
            shifted.put(line > removedLine ? line - 1 : line, entry.getValue());
        }
        lines.clear();
        lines.putAll(shifted);
    }

    void pruneAfter(int maxLine) {
        lines.keySet().removeIf(line -> line < 1 || line > maxLine);
    }
}
