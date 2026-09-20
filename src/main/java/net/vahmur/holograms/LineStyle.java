package net.vahmur.holograms;

import org.bukkit.configuration.ConfigurationSection;

final class LineStyle {
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

    LineStyle copy() {
        LineStyle copy = new LineStyle();
        copy.scaleX = scaleX;
        copy.scaleY = scaleY;
        copy.scaleZ = scaleZ;
        copy.offsetX = offsetX;
        copy.offsetY = offsetY;
        copy.offsetZ = offsetZ;
        copy.gapBefore = gapBefore;
        copy.gapAfter = gapAfter;
        copy.textShadow = textShadow;
        copy.seeThrough = seeThrough;
        return copy;
    }

    static LineStyle read(ConfigurationSection section) {
        LineStyle style = new LineStyle();

        if (section.contains("scale")) {
            float scale = (float) section.getDouble("scale", 1.0D);
            style.scaleX = scale;
            style.scaleY = scale;
            style.scaleZ = scale;
        }

        if (section.contains("scale_x")) style.scaleX = (float) section.getDouble("scale_x");
        if (section.contains("scale_y")) style.scaleY = (float) section.getDouble("scale_y");
        if (section.contains("scale_z")) style.scaleZ = (float) section.getDouble("scale_z");
        if (section.contains("offset_x")) style.offsetX = (float) section.getDouble("offset_x");
        if (section.contains("offset_y")) style.offsetY = (float) section.getDouble("offset_y");
        if (section.contains("offset_z")) style.offsetZ = (float) section.getDouble("offset_z");
        if (section.contains("gap_before")) style.gapBefore = section.getDouble("gap_before");
        if (section.contains("gap_after")) style.gapAfter = section.getDouble("gap_after");
        if (section.contains("text_shadow")) style.textShadow = section.getBoolean("text_shadow");
        if (section.contains("see_through")) style.seeThrough = section.getBoolean("see_through");

        return style;
    }

    void write(ConfigurationSection section) {
        if (scaleX != null && scaleY != null && scaleZ != null
                && Float.compare(scaleX, scaleY) == 0
                && Float.compare(scaleY, scaleZ) == 0) {
            section.set("scale", scaleX);
        } else {
            if (scaleX != null) section.set("scale_x", scaleX);
            if (scaleY != null) section.set("scale_y", scaleY);
            if (scaleZ != null) section.set("scale_z", scaleZ);
        }

        if (offsetX != null) section.set("offset_x", offsetX);
        if (offsetY != null) section.set("offset_y", offsetY);
        if (offsetZ != null) section.set("offset_z", offsetZ);
        if (gapBefore != null) section.set("gap_before", gapBefore);
        if (gapAfter != null) section.set("gap_after", gapAfter);
        if (textShadow != null) section.set("text_shadow", textShadow);
        if (seeThrough != null) section.set("see_through", seeThrough);
    }

    boolean isEmpty() {
        return scaleX == null && scaleY == null && scaleZ == null
                && offsetX == null && offsetY == null && offsetZ == null
                && gapBefore == null && gapAfter == null
                && textShadow == null && seeThrough == null;
    }

    float scaleX() { return scaleX == null ? 1.0F : scaleX; }
    float scaleY() { return scaleY == null ? 1.0F : scaleY; }
    float scaleZ() { return scaleZ == null ? 1.0F : scaleZ; }
    float offsetX() { return offsetX == null ? 0.0F : offsetX; }
    float offsetY() { return offsetY == null ? 0.0F : offsetY; }
    float offsetZ() { return offsetZ == null ? 0.0F : offsetZ; }
    double gapBefore() { return gapBefore == null ? 0.0D : gapBefore; }
    double gapAfter() { return gapAfter == null ? 0.0D : gapAfter; }
    Boolean textShadow() { return textShadow; }
    Boolean seeThrough() { return seeThrough; }

    void setScale(Float value) {
        scaleX = value;
        scaleY = value;
        scaleZ = value;
    }

    void setScaleX(Float value) { scaleX = value; }
    void setScaleY(Float value) { scaleY = value; }
    void setScaleZ(Float value) { scaleZ = value; }
    void setOffsetX(Float value) { offsetX = value; }
    void setOffsetY(Float value) { offsetY = value; }
    void setOffsetZ(Float value) { offsetZ = value; }
    void setGapBefore(Double value) { gapBefore = value; }
    void setGapAfter(Double value) { gapAfter = value; }
    void setTextShadow(Boolean value) { textShadow = value; }
    void setSeeThrough(Boolean value) { seeThrough = value; }
}
