package us.whitedev.commands;

import us.whitedev.gui.hud.utils.NotificationManager;
import us.whitedev.helpers.MessageHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CommandManager {

    public static final String COMMAND_PREFIX = "!";
    private final List<Command> commands = new ArrayList<>();
    private static CommandManager commandManager;
    private final MessageHelper msgHelper = new MessageHelper();

    public void addCommands(Command... commands) {
        this.commands.addAll(Arrays.asList(commands));
    }

    public static CommandManager getManager() {
        if (commandManager == null) {
            commandManager = new CommandManager();
        }
        return commandManager;
    }

    public void handleCommand(String msg) {
        if (!msg.startsWith(COMMAND_PREFIX)) {
            return;
        }

        String[] args = msg.substring(1).split(" ");
        Optional<Command> commandOptional = commands.stream().filter((command) -> args[0].replace(COMMAND_PREFIX, "").equalsIgnoreCase(command.getName())).findFirst();

        if (commandOptional.isPresent()) {
            try {
                Command command = commandOptional.get();
                command.onCommand(args);
            } catch (Exception e) {
                e.printStackTrace();
                NotificationManager.push(NotificationManager.NotificationType.ERROR, "Command Manager", "Command encountered an error while trying to call", 5000);
                msgHelper.sendMessage("&cError &7while running: &f" + e.getClass(), true);
            }
        } else {
            msgHelper.sendMessage("&7Command not found!", true);
        }
    }

    public List<String> getRegisteredCommandNames() {
        return this.commands.stream()
                .map(Command::getName)
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }
}
