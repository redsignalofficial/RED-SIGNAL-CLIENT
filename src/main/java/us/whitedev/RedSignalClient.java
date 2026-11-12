package us.whitedev;

import com.mojang.blaze3d.platform.NativeImage;
import de.florianmichael.viamcp.ViaMCP;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.Main;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import us.whitedev.commands.CommandManager;
import us.whitedev.commands.impl.*;
import us.whitedev.crashers.CrashManager;
import us.whitedev.crashers.impl.*;
import us.whitedev.exploits.ExploitManager;
import us.whitedev.exploits.impl.*;
import us.whitedev.gui.clickgui.utils.FavoritesManager;
import us.whitedev.helpers.ClientConfigHelper;
import us.whitedev.helpers.AccountHelper;
import us.whitedev.helpers.ConfigHelper;
import us.whitedev.helpers.FxFixer;
import us.whitedev.utils.DiscordRP;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class RedSignalClient {
    private final AccountHelper accountHelper = new AccountHelper();
    private final ConfigHelper configHelper = new ConfigHelper();
    private final ClientConfigHelper clientConfigHelper = new ClientConfigHelper();
    public static RedSignalClient redSignalClient;
    public static boolean DEBUG_MODE = false;
    public static boolean SONAR_ANTIBOT_FIX = false;
    public static final String VERSION = "6.4";

    public static RedSignalClient getInstance() {
        if (redSignalClient == null) redSignalClient = new RedSignalClient();
        return redSignalClient;
    }

    public void start() {
        try {
            ViaMCP.create();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Minecraft.LOGGER.info("RedSignalClient Initialization...");
        initializeClient();
    }

    private void initializeClient() {
        Minecraft.LOGGER.info("Registration Commands...");
        CommandManager.getManager().addCommands(
                new StopCommand(),
                new AuthorsCommand(),
                new HelpCommand(),
                new CrashCommand(),
                new ExploitCommand(),
                new BypassListCommand(),
                new FakeGmCommand(),
                new DetectCommand(),
                new ProxyCommand(),
                new ConfigCommand(),
                new PlayerListCommand(),
                new PluginListCommand()
        );
        Minecraft.LOGGER.info("Registration Crash Methods...");
        CrashManager.getManager().addMethod(
                new Strong1(),
                new Rogue1(),
                new Rogue2(),
                new Shift1(),
                new Shift2(),
                new Nova1(),
                new Nova2(),
                new Creative1(),
                new Vortex1(),
                new Vortex2(),
                new Vortex3(),
                new Vortex4(),
                new Onyx1(),
                new Onyx2(),
                new Echo1(),
                new Echo2(),
                new Cipher1(),
                new Cipher2(),
                new Cipher3(),
                new Smite1(),
                new Spectre1(),
                new Spectre2(),
                new Spectre3(),
                new Flare1(),
                new Shock1(),
                new Void1(),
                new Pulse1(),
                new Ignite1(),
                new Lux1(),
                new Storm1(),
                new Nexus1(),
                new Banshee1(),
                new Pearl1(),
                new Typhoon1(),
                new Banshee2()
        );
        Minecraft.LOGGER.info("Registration Crash Exploits...");
        ExploitManager.getManager().addExploit(
                new BungeeExploit(),
                new ConsoleSpammer(),
                new CommandsExploit(),
                new EntityExploit(),
                new EssentialsExploit(),
                new FaweExploit(),
                new ForceOpExploit(),
                new Log4JExploit(),
                new LuckPermsExploit(),
                new BundleExploit(),
                new MVCExploit(),
                new PexExploit(),
                new SignExploit(),
                new PluginExploit(),
                new LogsExploit(),
                new LPXExploit(),
                new WorldEditExploit(),
                new PVExploit(),
                new PurpurExploit(),
                new ModernSignExploit()
        );
        registerCustomImages();

        new DiscordRP().runDiscordRP();

        Minecraft.LOGGER.info("Loading visuals...");
        clientConfigHelper.readClientConfig();
        Minecraft.LOGGER.info("Loading accounts...");
        accountHelper.loadAccounts();

        if (ClientConfigHelper.LAST_LOGIN_WAS_OFFLINE && ClientConfigHelper.LAST_OFFLINE_NICK != null && !ClientConfigHelper.LAST_OFFLINE_NICK.isBlank()) {
            Minecraft.LOGGER.info("Auto offline login: " + ClientConfigHelper.LAST_OFFLINE_NICK);
            accountHelper.offlineLogin(ClientConfigHelper.LAST_OFFLINE_NICK);
        }

        Minecraft.LOGGER.info("Loading configs...");
        configHelper.readBypassList();
        Minecraft.LOGGER.info("Loading favorites...");
        FavoritesManager.load();
        Minecraft.LOGGER.info("Running JavaFx Fixer...");
        FxFixer.runFxFixer();
    }

    private void registerCustomImages() {
        Minecraft.LOGGER.info("Registration Custom Images...");

        Map<String, String> imagesToRegister = Map.of(
                "/client/images/background.png", "textures/gui/custom_background.png",
                "/client/images/cape.png", "textures/client/cape.png"
        );

        imagesToRegister.forEach((image, mcImageLoc) -> {
            InputStream inputStream = Main.class.getResourceAsStream(image);
            try {
                if (inputStream != null) {
                    NativeImage nativeImage = NativeImage.read(inputStream);
                    ResourceLocation customBackground = new ResourceLocation("redsignal", mcImageLoc);
                    Minecraft.getInstance().getTextureManager().register(customBackground, new DynamicTexture(nativeImage));
                    inputStream.close();
                }
            } catch (IOException e) {
                Minecraft.LOGGER.info("Error while reading image!");
                e.printStackTrace();
            }
        });
    }

    public String getTitle() {
        return "RedSignalClient [1.20.1] - " + VERSION;
    }

}
