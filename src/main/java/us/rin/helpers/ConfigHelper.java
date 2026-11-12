package us.rin.helpers;

import us.rin.utils.BypassListUtil;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ConfigHelper {
    private final String filePath = (System.getenv("APPDATA") != null ? System.getenv("APPDATA") : System.getProperty("user.dir")) + "/.minecraft/ConfigRed SignalList.ser";

    public static final Map<String, String[]> loadedConfigs = new HashMap<>();
    private static final List<BypassListUtil> customBypassList = new ArrayList<>();
    private static final List<BypassListUtil> bypassList = new ArrayList<>(List.of(
            new BypassListUtil("Crash Banshee2", "Paper 26.1", "Paper261","packets[40], size[4000000], sleep[5000], loop[5]"),
            new BypassListUtil("Crash Banshee2", "LPX 3.8.1", "LPX381","packets[1000], size[40000], sleep[1], loop[1]"),
            new BypassListUtil("Crash Typhoon1", "PaperAndVia", "PaperAndViaCrash","packets[300], size[2], count[1000000]"),
            new BypassListUtil("Crash Banshee1", "LPX Bypass", "LPX380","packets[1000], subs[98000], sleep[1000], loop[20]"),
            new BypassListUtil("Crash Banshee1", "PaperLatest", "PaperLatestCrash","packets[1000], subs[1000000], sleep[1], loop[1]"),
            new BypassListUtil("Crash Nexus1", "LPX Bypass", "LPXBypass1","packets[1000], size[2000], scales[11], sleep[1], loop[1]"),
            new BypassListUtil("Crash Nexus1", "Via Bypass", "ViaBypass1","packets[1000], size[256], scales[256], sleep[1], loop[1]"),
            new BypassListUtil("Crash Pearl1", "Basic Netty Crash", "BasicNetty1","packets[300], size[10000], map-size[127]"),
            new BypassListUtil("Crash Pulse1", "ViaBackwards Big", "ViaBackwardsBig","packets[200], size[1500], map-size[127]"),
            new BypassListUtil("Crash Pulse1", "ViaBackwards Medium", "ViaBackwardsMedium","packets[200], size[900], map-size[127]"),
            new BypassListUtil("Crash Pulse1", "ViaBackwards Small", "ViaBackwardsSmall","packets[200], size[500], map-size[127]"),
            new BypassListUtil("Crash Ignite1", "TimeOut", "TimeOut","packets[1000], size[1000], size2[1000], thread-sleep[2000], loop-amount[15]"),
            new BypassListUtil("Crash Void1", "PaperSpigot - Big NBT", "PaperSpigotNBT1","packets[100], size[2096000], type[Normal]"),
            new BypassListUtil("Crash Void1", "PaperSpigot - Medium NBT", "PaperSpigotNBT2","packets[1000], size[35000], type[Normal]"),
            new BypassListUtil("Crash Shock1", "UniverseSpigot", "USpigotBypass","packets[500], size[256], scales[15], map-size[0], sleep[1500], loop[15]"),
            new BypassListUtil("Crash Shock1", "LPXBypass", "LPXBypassShock","packets[410], size[10], scales[120], map-size[0], sleep[500], loop[85]"),
            new BypassListUtil("Crash Shock1", "Full Paper Crash", "AllNettyFull","packets[200], size[256], scales[60], map-size[5], sleep[1], loop[1]"),
            new BypassListUtil("Crash Shock1", "Full Paper Crash", "AllNettyFull2","packets[10], size[256], scales[60], map-size[15], sleep[1], loop[1]"),
            new BypassListUtil("Crash Smite1", "Paper", "PaperHash","packets[1600], size1[256], size2[256], count[128], thread-sleep[3750], loop-amount[10]"),
            new BypassListUtil("Crash Smite1", "LPX Latest", "LPX364Hash","packets[1000], size1[256], size2[256], count[60], thread-sleep[4000], loop-amount[10]"),
            new BypassListUtil("Crash Cipher1", "Paper", "Paper8MiB","packets[10], size[1], count[2097146], illegal[false]"),
            new BypassListUtil("Crash Cipher1", "Paper", "PaperBomb", "packets[500], count1[32000], count2[10], illegal[false]"),
            new BypassListUtil("Crash Cipher1", "LPX 3.5.13", "LPX3513", "packets[1000], count1[2048], count2[11], illegal[false]"),
            new BypassListUtil("Crash Cipher1", "LPX 3.6.12", "LPX3612", "packets[1000], count1[120], count2[120], illegal[false]"),
            new BypassListUtil("Crash Cipher1", "LPX Latest", "LPX377", "packets[1000], count1[24], count2[10], illegal[true]"),
            new BypassListUtil("Crash Cipher1", "ViaVersion", "ViaComponentBomb1", "packets[1000], count1[5416], count2[1], illegal[false]"),
            new BypassListUtil("Crash Cipher1", "ViaBackwards", "ViaComponentBomb2", "packets[1000], count1[8000], count2[11], illegal[false]"),
            new BypassListUtil("Crash Cipher2", "LPX Latest", "LPX362", "packets[1000], size[910], count[100], thread-sleep[2000], loop-amount[15]"),
            new BypassListUtil("Crash Cipher2", "Paper/ViaVersion", "ComponentBomb1", "packets[100], size[500000], count[1], thread-sleep[1], loop-amount[1]"),
            new BypassListUtil("Crash Cipher3", "LPX Latest", "LPX364", "packets[1000], length[42], amount[0], size[2048], thread-sleep[2000], loop-amount[10]"),
            new BypassListUtil("Crash Cipher3", "ViaVersion", "ViaComponentBomb3", "packets[1000], length[100], amount[100], size[85], thread-sleep[1], loop-amount[1]"),
            new BypassListUtil("Crash Cipher3", "Paper", "ComponentBomb2", "packets[500], length[2048], amount[0], size[1024], thread-sleep[1], loop-amount[1]"),
            new BypassListUtil("Crash Echo1", "Paper Latest", "PaperFull","power[500], size[2096000], depth[1], type[Main2]"),
            new BypassListUtil("Crash Echo1", "Paper Latest", "PaperLarge","power[500], size[5000], depth[1000], type[Main1]"),
            new BypassListUtil("Crash Echo1", "Paper Latest", "PaperMedium","power[500], size[11000], depth[1], type[Netty1]"),
            new BypassListUtil("Crash Echo1", "Paper Latest", "PaperSmall","power[500], size[2500], depth[1], type[Netty2]"),
            new BypassListUtil("Crash Echo1", "ExploitFixer 2.7.x", "EF27x","power[2500], size[35000], depth[1], type[Main2]"),
            new BypassListUtil("Crash Echo1", "LPX 3.5.8", "LPX358","power[500], size[35000], depth[1], type[Main2]"),
            new BypassListUtil("Crash Spectre1", "ViaVersion OnePacket", "ViaOnePacket1","packets[1], mode[pot], count[20000], type[Legit], state[Illegal], map-size[128]"),
            new BypassListUtil("Crash Spectre1", "Old ViaVer OnePacket", "ViaOnePacket2","packets[1], mode[pot], count[20000], type[Illegal], state[Legit], map-size[128]"),
            new BypassListUtil("Crash Spectre3", "Old ViaVersion OnePacket", "ViaOnePacketOld","packets[1], pages[6000], size[1], type[Convert]"),
            new BypassListUtil("Crash Spectre3", "LPX 3.5.x", "LPX35x","packets[20], pages[500], size[1], type[Convert]"),
            new BypassListUtil("Crash Onyx1", "ViaRewind", "ViaRewJson","Packets[100], Mode[Invalid], Size[226]"),
            new BypassListUtil("Crash Onyx1", "Console Freezer", "Freezer","Packets[100], Mode[Comp], Size[1000]"),
            new BypassListUtil("Crash Onyx1", "ViaVersion (OLD)", "ViaVerNBT","Packets[1], Mode[Display], Size[1800]"),
            new BypassListUtil("Crash Onyx1", "LPX 3.5.4", "1LPX354","Packets[1], Mode[Map], Size[16000]"),
            new BypassListUtil("Crash Onyx1", "LPX 3.5.4", "2LPX354","Packets[1500], Mode[Comp], Size[2]"),
            new BypassListUtil("Crash Onyx1", "LPX 3.5.4 (1.20.5+)", "3LPX354","Packets[300], Mode[Comp], Size[1420]"),
            new BypassListUtil("Crash Onyx1", "LPX 3.5.4 (1.20.5+)", "4LPX354","Packets[500], Mode[Display], Size[50]"),
            new BypassListUtil("Crash Onyx2", "Paper/ViaVersion", "ViaPaper","Packets[700], Size[35000], Length[1]"),
            new BypassListUtil("Crash Vortex1", "ExploitFixer", "EF","packets[1], power[700], type[EF-Bypass]"),
            new BypassListUtil("Crash Vortex1", "ViaVersion 5.1.0", "ViaVer510","packets[1], power[700], type[Default]"),
            new BypassListUtil("Crash Vortex1", "Many Servers","ManyServer", "packets[500], power[1], type[Unreachable]"),
            new BypassListUtil("Crash Vortex2", "LPX 3.4.11", "LPX3411","packets[300], power[400], type[Boost]"),
            new BypassListUtil("Crash Vortex2", "ALL", "ALL","packets[100], power[700], type[Boost]"),
            new BypassListUtil("Crash Vortex2", "ViaVersion Latest", "ViaVerAll","packets[100], power[1000], type[Default]"),
            new BypassListUtil("Crash Vortex2", "LPX 3.5.3 (1.12)", "1LPX350","packets[400], power[480], type[Default]"),
            new BypassListUtil("Crash Vortex3", "LPX 3.5.3 (Newest)", "2LPX350","packets[500], size[20], length[300], type[Full]"),
            new BypassListUtil("Crash Vortex3", "LPX 3.5.3 (Newest)", "3LPX350","packets[500], size[38000], length[1], type[Empty]"),
            new BypassListUtil("Crash Vortex3", "LPX 3.5.2", "4LPX350","packets[260], size[3000], length[1], type[Empty]"),
            new BypassListUtil("Crash Vortex3", "All Netty 1.20", "AllNetty120","packets[800], size[35000], length[1], type[Empty]"),
            new BypassListUtil("Crash Vortex3", "All Netty 1.21+", "AllNetty121","packets[800], size[1950500], length[1], type[Empty]"),
            new BypassListUtil("Crash Shift1", "LPX 3.5.3", "5LPX350","packets[500], size[920], type[Default]"),
            new BypassListUtil("Crash Storm1", "PaperSpigot - NBT Full Crash", "PSNbt1","packets[800], size[35000], depth[1], type[Normal], boost[true]"),
            new BypassListUtil("Crash Storm1", "PaperSpigot - NBT Full/Medium Crash", "PSNbt2","packets[800], size[35000], depth[1], type[Strong], boost[false]"),
            new BypassListUtil("Crash Storm1", "PaperSpigot - DataComponent Full Crash", "PSComp1","packets[100], size[1100000], depth[1], type[Normal], boost[true]"),
            new BypassListUtil("Crash Storm1", "PaperSpigot - DataComponent Full/Medium Crash", "PSComp2","packets[35000], size[35000], depth[1], type[Normal], boost[false]"),
            new BypassListUtil("Crash Shift2", "ExploitFixer 3.1.0", "EF310","packets[1000], size[1], mode[deco]"),
            new BypassListUtil("Crash Strong1", "Paper/Purpur 1.21+", "Paper121", "packets[100], chars[100], pages[15], empty-stack[false], generator[Modern1], item[Saved], map-size[46], thread-sleep[1500], loop-amount[15]"),
            new BypassListUtil("Crash Strong1", "ExploitFixer 3.x (1.21+)", "EF3", "packets[100], chars[300], pages[25], empty-stack[true], generator[Modern1], item[Saved], map-size[46], thread-sleep[1500], loop-amount[15]"),
            new BypassListUtil("Crash Strong1", "LPX 3.3.5", "LPX335","packets[100], chars[1500], pages[15], empty-stack[false], generator[Default], item[Saved], map-size[46], thread-sleep[1500], loop-amount[10]"),
            new BypassListUtil("Crash Strong1", "Hypixel", "Hypixel","packets[300], chars[175], pages[1], empty-stack[false], generator[Boost1], item[Saved],  map-size[46], thread-sleep[3000], loop-amount[10]"),
            new BypassListUtil("Crash Nova1", "ViaVersion 4.9.3", "ViaVer493","packets[500], pages[300000], size[1], mode[Legacy]"),
            new BypassListUtil("Crash Nova2", "LPX 3.5.0+ (Newest)", "6LPX350","packets[1500], size[17000], amount[30], scales[1], type[Saved], sleep[1], loops[1]"),
            new BypassListUtil("Crash Rogue1", "PacketsEvent-Old", "PEOld","packets[5000], invalidId[true], handException[false], clickException[false]"),
            new BypassListUtil("Crash Rogue1", "PacketsEvent-New", "PENew","packets[8000], invalidId[false], handException[false], clickException[true]"),
            new BypassListUtil("Crash Rogue2", "PaperSpigot 1.19/1.20", "PS1920","packets[500], tabException[false], buttonException[true], illegalPos[false], spigotException[false], slotException[false], recipeException[false]"),
            new BypassListUtil("Crash Rogue2", "PaperSpigot 1.13/1.20", "PS1320","packets[2], tabException[true], buttonException[false], illegalPos[false], spigotException[false], slotException[false], recipeException[false]"),
            new BypassListUtil("Crash Rogue2", "PaperSpigot 1.21.3", "PS1213","packets[2], tabException[true], buttonException[false], illegalPos[false], spigotException[false], slotException[false], recipeException[false]"),
            new BypassListUtil("Exploit PurpurExploit", "Purpur 1.19/1.20", "Purpur","power[1000], mode[Default]"),
            new BypassListUtil("Exploit WorldEditExploit", "WorldEdit Plugin", "WorldEdit","power[1000], mode[New]"),
            new BypassListUtil("Exploit SignExploit", "Spigot 1.8.9", "Spigot","mode[Crash]"),
            new BypassListUtil("Exploit EntityExploit", "NPC Plugin", "Entity","power[51000]")
    ));

    public List<BypassListUtil> getBypassList() {
        return bypassList;
    }

    public String antiDupeName(String baseName) {
        Set<String> existingNames = bypassList.stream()
                .map(BypassListUtil::bypassName)
                .collect(Collectors.toSet());
        existingNames.addAll(customBypassList.stream()
                .map(BypassListUtil::bypassName)
                .collect(Collectors.toSet()));

        if (!existingNames.contains(baseName))
            return baseName;

        int counter = 2;
        String newName;
        do {
            newName = baseName + counter;
            counter++;
        } while (existingNames.contains(newName));

        return newName;
    }


    public void updateBypassList(BypassListUtil bypassListUtil) {
        customBypassList.add(bypassListUtil);
        bypassList.add(bypassListUtil);
    }

    public void removeBypassList(String bypassName) {
        BypassListUtil bypassListUtil = getConfig(bypassName);
        customBypassList.remove(bypassListUtil);
        bypassList.remove(bypassListUtil);
    }

    public boolean loadConfig(String bypassName) {
        BypassListUtil bypassListUtil = getConfig(bypassName);
        if(bypassListUtil != null) {
            loadedConfigs.put(
                    bypassListUtil.moduleName().split(" ")[1],
                    extractValues(bypassListUtil.configString())
            );
            return true;
        }
        return false;
    }

    private BypassListUtil getConfig(String bypassName){
        return bypassList.stream()
                .filter(bypass -> bypass.bypassName().equalsIgnoreCase(bypassName))
                .findFirst()
                .orElse(null);
    }

    private String[] extractValues(String input) {
        Pattern pattern = Pattern.compile("\\[(.*?)]");
        Matcher matcher = pattern.matcher(input);
        ArrayList<String> values = new ArrayList<>();

        while (matcher.find()) {
            values.add(matcher.group(1));
        }

        return values.toArray(new String[0]);
    }

    public void saveBypassList() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(customBypassList);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public void readBypassList() {
        File file = new File(filePath);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
                List<BypassListUtil> loadedList = (List<BypassListUtil>) ois.readObject();
                bypassList.addAll(loadedList);
                customBypassList.addAll(loadedList);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

}
