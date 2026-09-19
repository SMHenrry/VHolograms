package net.vahmur.holograms;

import java.util.Map;

final class LineSpec {
    final String text;
    final float scaleX;
    final float scaleY;
    final float scaleZ;
    final float offsetX;
    final float offsetY;
    final float offsetZ;
    final double gapBefore;
    final double gapAfter;
    final Boolean textShadow;
    final Boolean seeThrough;

    private LineSpec(
            String text,
            float scaleX,
            float scaleY,
            float scaleZ,
            float offsetX,
            float offsetY,
            float offsetZ,
            double gapBefore,
            double gapAfter,
            Boolean textShadow,
            Boolean seeThrough
    ) {
        this.text = text;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.scaleZ = scaleZ;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.gapBefore = gapBefore;
        this.gapAfter = gapAfter;
        this.textShadow = textShadow;
        this.seeThrough = seeThrough;
    }

    static LineSpec from(Map<?, ?> map) {
        String text = string(map.get("text"), "<reset>");

        float scalar = number(map.get("scale"), 1.0f);
        float sx = number(map.get("scale_x"), scalar);
        float sy = number(map.get("scale_y"), scalar);
        float sz = number(map.get("scale_z"), scalar);

        float ox = number(map.get("offset_x"), 0.0f);
        float oy = number(map.get("offset_y"), 0.0f);
        float oz = number(map.get("offset_z"), 0.0f);

        double gapBefore = numberD(map.get("gap_before"), 0.0D);
        double gapAfter = numberD(map.get("gap_after"), 0.0D);

        Boolean textShadow = nullableBoolean(map.get("text_shadow"));
        Boolean seeThrough = nullableBoolean(map.get("see_through"));

        return new LineSpec(text, sx, sy, sz, ox, oy, oz, gapBefore, gapAfter, textShadow, seeThrough);
    }

    private static String string(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }

    private static float number(Object value, float fallback) {
        if (value instanceof Number n) return n.floatValue();
        if (value != null) {
            try { return Float.parseFloat(String.valueOf(value)); }
            catch (NumberFormatException ignored) { }
        }
        return fallback;
    }

    private static double numberD(Object value, double fallback) {
        if (value instanceof Number n) return n.doubleValue();
        if (value != null) {
            try { return Double.parseDouble(String.valueOf(value)); }
            catch (NumberFormatException ignored) { }
        }
        return fallback;
    }

    private static Boolean nullableBoolean(Object value) {
        if (value instanceof Boolean b) return b;
        if (value == null) return null;
        String s = String.valueOf(value);
        if (s.equalsIgnoreCase("true")) return Boolean.TRUE;
        if (s.equalsIgnoreCase("false")) return Boolean.FALSE;
        return null;
    }
}
