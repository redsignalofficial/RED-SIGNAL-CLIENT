package us.rin.crashers;

import us.rin.commands.CommandManager;
import us.rin.gui.clickgui.ClickGui;
import us.rin.gui.hud.utils.NotificationManager;
import us.rin.helpers.MessageHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class CrashManager {
    private final List<Crasher> crashers = new ArrayList<>();
    private static CrashManager crashManager;
    private final MessageHelper msgHelper = new MessageHelper();

    public void addMethod(Crasher... crashers) {
        this.crashers.addAll(Arrays.asList(crashers));
    }

    public List<Crasher> getMethodsList(){
        return crashers;
    }

    public static CrashManager getManager(){
        if (crashManager == null) {
            crashManager = new CrashManager();
        }
        return crashManager;
    }

    public void handleMethod(String msg) {
        if (!msg.startsWith(CommandManager.COMMAND_PREFIX)) {
            return;
        }

        String[] args = msg.substring(1).split(" ");
        if(args.length >= 2) {
            Optional<Crasher> methodOptional = crashers.stream().filter((crasher) -> args[1].equalsIgnoreCase(crasher.getName())).findFirst();

            if (methodOptional.isPresent()) {
                Crasher crasher = methodOptional.get();
                if((crasher.getOptions().size() + 2) == args.length) {
                    try {
                        crasher.onMethod(args);
                        NotificationManager.push(NotificationManager.NotificationType.SUCCESS, "Crash Manager", "Crasher " + (ClickGui.IS_VISIBLE ? crasher.getName() : "******") + " executed successfully!", 5000);
                    }catch (Exception e){
                        e.printStackTrace();
                        NotificationManager.push(NotificationManager.NotificationType.ERROR, "Crash Manager", "Crasher " + (ClickGui.IS_VISIBLE ? crasher.getName() : "******") + " encountered an error while trying to call", 5000);
                        msgHelper.sendMessage("&cError &7while running: &f" + e.getClass(), true);
                    }
                    return;
                }else{
                    msgHelper.sendMessage("&7You have provided &cinvalid &7arguments!", true);
                    msgHelper.sendMessage("&7Correct usage: &f" + crasher.getArgsUsage(), true);
                    NotificationManager.push(NotificationManager.NotificationType.WARNING, "Crash Manager", "You have provided invalid arguments!", 5000);
                }
            } else {
                msgHelper.sendMessage("&7Available methods:", true);
                for (Crasher crasher : crashers) {
                    msgHelper.sendMessage("&7" + crasher.getName() + " &8- &f" + crasher.getDescription(), true);
                }
            }
            return;
        }
        msgHelper.sendMessage("&7Available methods:", true);
        for (Crasher crasher : crashers) {
            msgHelper.sendMessage("&7" + crasher.getName() + " &8- &f" + crasher.getDescription(), true);
        }
    }

    public Optional<Crasher> getCrashClassByName(String name){
        return crashers.stream().filter((crasher) -> name.equalsIgnoreCase(crasher.getName())).findFirst();
    }

    public String allMethods(){
        List<String> methodNames = new ArrayList<>();
        for (Crasher crasher : crashers) {
            methodNames.add(crasher.getName());
        }
        return String.join(", ", methodNames);
    }

}
