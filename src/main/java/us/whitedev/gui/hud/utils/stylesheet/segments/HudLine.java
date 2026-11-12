package us.whitedev.gui.hud.utils.stylesheet.segments;

import java.util.ArrayList;
import java.util.List;

public final class HudLine {
    public boolean visible;
    public float x;
    public float y;
    public String align;
    public boolean shadow;
    public String shadowColorToken;
    public float shadowOffsetX;
    public float shadowOffsetY;
    public int zIndex;
    public final List<HudSegment> segments = new ArrayList<>();
}