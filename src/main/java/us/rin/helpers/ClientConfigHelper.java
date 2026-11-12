package us.rin.helpers;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.vialoadingbase.ViaLoadingBase;
import net.minecraft.client.Minecraft;
import us.rin.crashers.CrashManager;
import us.rin.exploits.ExploitManager;
import us.rin.gui.clickgui.ClickGui;
import us.rin.gui.hud.utils.HudOptions;

import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class ClientConfigHelper {
    public static String LAST_OFFLINE_NICK = "iBuyPower_Xx";
    public static int LAST_PROTOCOL_VERSION = -1;
    public static boolean LAST_LOGIN_WAS_OFFLINE = false;

    public static final Map<String, Integer> MODULE_BINDS = new HashMap<>();

    private final File configFile = new File(Minecraft.getInstance().gameDirectory, "VisualConfig.ser");
    private final File customHexFile = new File(Minecraft.getInstance().gameDirectory, "VisualConfig_Custom.txt");

    public void saveClientConfig() {
        ClientConfig config = new ClientConfig();
        config.hudOptionsMap.putAll(HudOptions.getMap());
        config.isVisible = ClickGui.IS_VISIBLE;
        config.colorTheme = ClickGui.COLOR_THEME;
        config.hudPlace = ClickGui.HUD_PLACE;
        config.clientStyle = ClickGui.CLIENT_STYLE;
        config.scriptState = ClickGui.LAST_SCRIPTS_STATE;
        config.lastOfflineNick = LAST_OFFLINE_NICK;
        config.lastProtocolVersion = LAST_PROTOCOL_VERSION;
        config.lastLoginWasOffline = LAST_LOGIN_WAS_OFFLINE;
        config.moduleBinds.clear();
        config.moduleBinds.putAll(MODULE_BINDS);

        try {
            if (!configFile.getParentFile().exists()) {
                configFile.getParentFile().mkdirs();
            }
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(configFile))) {
                oos.writeObject(config);
            }
            Files.writeString(customHexFile.toPath(), ClickGui.CUSTOM_HEX);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public void readClientConfig() {
        if (customHexFile.exists()) {
            try {
                String hex = Files.readString(customHexFile.toPath()).trim();
                if (!hex.isEmpty()) ClickGui.CUSTOM_HEX = hex;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (configFile.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(configFile))) {
                ClientConfig config = (ClientConfig) ois.readObject();

                HudOptions.getMap().clear();
                HudOptions.setMap(config.hudOptionsMap);

                ClickGui.IS_VISIBLE = config.isVisible;
                ClickGui.HUD_PLACE = config.hudPlace;
                ClickGui.CLIENT_STYLE = config.clientStyle;
                ClickGui.LAST_SCRIPTS_STATE = config.scriptState;

                MODULE_BINDS.clear();
                if (config.moduleBinds != null) {
                    MODULE_BINDS.putAll(config.moduleBinds);
                }

                if (config.lastOfflineNick != null && !config.lastOfflineNick.isBlank()) {
                    LAST_OFFLINE_NICK = config.lastOfflineNick;
                } else {
                    LAST_OFFLINE_NICK = "iBuyPower_Xx";
                }

                LAST_LOGIN_WAS_OFFLINE = config.lastLoginWasOffline;

                if (config.lastProtocolVersion > 0) {
                    LAST_PROTOCOL_VERSION = config.lastProtocolVersion;
                    ProtocolVersion pv = ProtocolVersion.getProtocol(config.lastProtocolVersion);
                    if (pv != null && ViaLoadingBase.getInstance() != null) {
                        ViaLoadingBase.getInstance().reload(pv);
                    }
                } else {
                    LAST_PROTOCOL_VERSION = -1;
                }

                if (config.colorTheme != null) {
                    ClickGui.COLOR_THEME = config.colorTheme;
                    ClickGui.setThemeColor(config.colorTheme);
                }

            } catch (IOException | ClassNotFoundException | ClassCastException e) {
                e.printStackTrace();
                HudOptions.initMap();
                LAST_OFFLINE_NICK = "iBuyPower_Xx";
                LAST_PROTOCOL_VERSION = -1;
                LAST_LOGIN_WAS_OFFLINE = false;
                MODULE_BINDS.clear();
            }
        } else {
            HudOptions.initMap();
            LAST_OFFLINE_NICK = "iBuyPower_Xx";
            LAST_PROTOCOL_VERSION = -1;
            LAST_LOGIN_WAS_OFFLINE = false;
            MODULE_BINDS.clear();
        }
    }

    public static boolean tryExecuteBind(int keyCode) {
        if (keyCode <= 0) return false;

        String entryKey = null;
        for (Map.Entry<String, Integer> e : MODULE_BINDS.entrySet()) {
            if (e.getValue() != null && e.getValue() == keyCode) {
                entryKey = e.getKey();
                break;
            }
        }

        if (entryKey == null || entryKey.isBlank()) return false;

        String mode;
        String moduleName;
        int sep = entryKey.indexOf(':');
        if (sep > 0 && sep < entryKey.length() - 1) {
            mode = entryKey.substring(0, sep);
            moduleName = entryKey.substring(sep + 1);
        } else {
            mode = "";
            moduleName = entryKey;
        }

        String[] args = ConfigHelper.loadedConfigs.get(moduleName);
        if (args == null) return false;

        StringBuilder builder = new StringBuilder();
        for (String val : args) {
            builder.append(val).append(' ');
        }

        if ("CRASH".equalsIgnoreCase(mode)) {
            CrashManager.getManager().handleMethod(String.format("!crash %s %s", moduleName, builder));
            return true;
        }

        if ("EXPLOIT".equalsIgnoreCase(mode)) {
            ExploitManager.getManager().handleExploit(String.format("!exploit %s %s", moduleName, builder));
            return true;
        }

        if (CrashManager.getManager().getCrashClassByName(moduleName).isPresent()) {
            CrashManager.getManager().handleMethod(String.format("!crash %s %s", moduleName, builder));
            return true;
        }
        if (ExploitManager.getManager().getExploitClassByName(moduleName).isPresent()) {
            ExploitManager.getManager().handleExploit(String.format("!exploit %s %s", moduleName, builder));
            return true;
        }
        return false;
    }
}