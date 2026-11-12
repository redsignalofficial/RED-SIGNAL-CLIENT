package us.whitedev.gui.hud.utils.stylesheet.segments;

import java.util.ArrayList;
import java.util.List;

public final class HudMetricList {
    public boolean visible;
    public float x;
    public float y;
    public int width;
    public int rowHeight;
    public String separator;
    public int separatorColor;
    public int labelColor;
    public boolean labelBold;
    public int labelSize;
    public boolean labelShadow;
    public String labelShadowColorToken;
    public float labelShadowOffsetX;
    public float labelShadowOffsetY;
    public String valueColorToken;
    public boolean valueBold;
    public int valueSize;
    public String valueAlign;
    public boolean valueShadow;
    public String valueShadowColorToken;
    public float valueShadowOffsetX;
    public float valueShadowOffsetY;
    public boolean respectHudOptions;
    public int zIndex;
    public final List<HudMetricItem> items = new ArrayList<>();
}