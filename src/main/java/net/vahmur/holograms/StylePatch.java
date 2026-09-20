package net.vahmur.holograms;

import java.util.Locale;

final class StylePatch {
    private Float scale;
    private Float scaleX;
    private Float scaleY;
    private Float scaleZ;
    private Float offsetX;
    private Float offsetY;
    private Float offsetZ;
    private Double gapBefore;
    private Double gapAfter;
    private Boolean textShadow;
    private Boolean seeThrough;

    private boolean clearScale;
    private boolean clearScaleX;
    private boolean clearScaleY;
    private boolean clearScaleZ;
    private boolean clearOffsetX;
    private boolean clearOffsetY;
    private boolean clearOffsetZ;
    private boolean clearGapBefore;
    private boolean clearGapAfter;
    private boolean clearTextShadow;
    private boolean clearSeeThrough;

    void apply(LineStyle style) {
        if (clearScale) style.setScale(null);
        if (scale != null) style.setScale(scale);

        if (clearScaleX) style.setScaleX(null);
        if (clearScaleY) style.setScaleY(null);
        if (clearScaleZ) style.setScaleZ(null);
        if (scaleX != null) style.setScaleX(scaleX);
        if (scaleY != null) style.setScaleY(scaleY);
        if (scaleZ != null) style.setScaleZ(scaleZ);

        if (clearOffsetX) style.setOffsetX(null);
        if (clearOffsetY) style.setOffsetY(null);
        if (clearOffsetZ) style.setOffsetZ(null);
        if (offsetX != null) style.setOffsetX(offsetX);
        if (offsetY != null) style.setOffsetY(offsetY);
        if (offsetZ != null) style.setOffsetZ(offsetZ);

        if (clearGapBefore) style.setGapBefore(null);
        if (clearGapAfter) style.setGapAfter(null);
        if (gapBefore != null) style.setGapBefore(gapBefore);
        if (gapAfter != null) style.setGapAfter(gapAfter);

        if (clearTextShadow) style.setTextShadow(null);
        if (clearSeeThrough) style.setSeeThrough(null);
        if (textShadow != null) style.setTextShadow(textShadow);
        if (seeThrough != null) style.setSeeThrough(seeThrough);
    }

    void set(String flag, String value) {
        String normalized = flag.toLowerCase(Locale.ROOT);
        boolean inherit = isInherit(value);

        switch (normalized) {
            case "--scale" -> {
                if (inherit) clearScale = true;
                else scale = parseFloat(flag, value);
            }
            case "--scale-x" -> {
                if (inherit) clearScaleX = true;
                else scaleX = parseFloat(flag, value);
            }
            case "--scale-y" -> {
                if (inherit) clearScaleY = true;
                else scaleY = parseFloat(flag, value);
            }
            case "--scale-z" -> {
                if (inherit) clearScaleZ = true;
                else scaleZ = parseFloat(flag, value);
            }
            case "--offset-x" -> {
                if (inherit) clearOffsetX = true;
                else offsetX = parseFloat(flag, value);
            }
            case "--offset-y" -> {
                if (inherit) clearOffsetY = true;
                else offsetY = parseFloat(flag, value);
            }
            case "--offset-z" -> {
                if (inherit) clearOffsetZ = true;
                else offsetZ = parseFloat(flag, value);
            }
            case "--gap-before" -> {
                if (inherit) clearGapBefore = true;
                else gapBefore = parseDouble(flag, value);
            }
            case "--gap-after" -> {
                if (inherit) clearGapAfter = true;
                else gapAfter = parseDouble(flag, value);
            }
            case "--text-shadow" -> {
                if (inherit) clearTextShadow = true;
                else textShadow = parseBoolean(flag, value);
            }
            case "--see-through" -> {
                if (inherit) clearSeeThrough = true;
                else seeThrough = parseBoolean(flag, value);
            }
            default -> throw new IllegalArgumentException("Opción desconocida: " + flag);
        }
    }

    private static boolean isInherit(String value) {
        return value.equalsIgnoreCase("inherit")
                || value.equalsIgnoreCase("default")
                || value.equalsIgnoreCase("reset");
    }

    private static float parseFloat(String flag, String value) {
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("El valor de " + flag + " debe ser un número o 'inherit'.");
        }
    }

    private static double parseDouble(String flag, String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("El valor de " + flag + " debe ser un número o 'inherit'.");
        }
    }

    private static boolean parseBoolean(String flag, String value) {
        if (value.equalsIgnoreCase("true")) return true;
        if (value.equalsIgnoreCase("false")) return false;
        throw new IllegalArgumentException("El valor de " + flag + " debe ser true, false o inherit.");
    }
}
