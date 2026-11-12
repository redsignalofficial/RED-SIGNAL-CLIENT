package us.rin.helpers;

import us.rin.gui.clickgui.components.ScriptsComponent;
import us.rin.gui.clickgui.ClickGui;
import us.rin.gui.clickgui.utils.ColorTheme;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class ClientConfig implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public Map<String, Boolean> hudOptionsMap = new HashMap<>();
    public boolean isVisible;
    public ColorTheme colorTheme;
    public ClickGui.HudPlace hudPlace;
    public ClickGui.ClientStyle clientStyle;
    public ScriptsComponent.ScriptsState scriptState;
    public String lastOfflineNick;
    public int lastProtocolVersion;
    public boolean lastLoginWasOffline;
    public Map<String, Integer> moduleBinds = new HashMap<>();
}