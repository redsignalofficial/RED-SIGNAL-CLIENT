package us.whitedev.gui.hud.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HudOptions {
    private static Map<String, Boolean> hudOptionsMap = new HashMap<>();

    public static void initMap(){
        hudOptionsMap.clear();
        hudOptionsMap.put("IP", true);
        hudOptionsMap.put("Engine", true);
        hudOptionsMap.put("Last Packet", true);
        hudOptionsMap.put("XYZ", false);
        hudOptionsMap.put("FPS", false);
        hudOptionsMap.put("PPS", false);
    }

    public static Map<String, Boolean> getMap(){
        return hudOptionsMap;
    }

    public static void setMap(Map<String, Boolean> hudOptionsMap){
        HudOptions.hudOptionsMap = hudOptionsMap;
    }

    public static List<String> getMapKeys(){
        return hudOptionsMap.keySet().stream().toList();
    }

    public static void setOptionValue(String name, boolean value){
        hudOptionsMap.replace(name, value);
    }

    public static boolean getOption(String name){
        return hudOptionsMap.get(name);
    }
}
