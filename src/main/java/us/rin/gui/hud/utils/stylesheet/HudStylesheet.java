package us.rin.gui.hud.utils.stylesheet;

import us.rin.gui.hud.utils.stylesheet.segments.HudBox;
import us.rin.gui.hud.utils.stylesheet.segments.HudImage;
import us.rin.gui.hud.utils.stylesheet.segments.HudLine;
import us.rin.gui.hud.utils.stylesheet.segments.HudMetricList;
import us.rin.gui.hud.utils.stylesheet.segments.HudRoot;

import java.util.ArrayList;
import java.util.List;

public final class HudStylesheet {
    public final HudRoot root = new HudRoot();
    public final List<HudBox> boxes = new ArrayList<>();
    public final List<HudLine> lines = new ArrayList<>();
    public final List<HudMetricList> metricLists = new ArrayList<>();
    public final List<HudImage> images = new ArrayList<>();
}
