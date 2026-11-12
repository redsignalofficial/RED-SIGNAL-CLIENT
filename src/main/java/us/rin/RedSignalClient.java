package us.rin;

import com.mojang.blaze3d.platform.NativeImage;
import de.florianmichael.viamcp.ViaMCP;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.Main;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import us.rin.commands.CommandManager;
import us.rin.commands.impl.*;
import us.rin.crashers.CrashManager;
import us.rin.crashers.impl.*;
import us.rin.exploits.ExploitManager;
import us.rin.exploits.impl.*;
import us.rin.gui.clickgui.utils.FavoritesManager;
import us.rin.helpers.ClientConfigHelper;
import us.rin.helpers.AccountHelper;
import us.rin.helpers.ConfigHelper;
import us.rin.helpers.FxFixer;
import us.rin.utils.DiscordRP;

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

    public static void log(String msg) {
        System.out.println("[RED SIGNAL CLIENT] " + msg);
    }

    public void start() {
        try {
            ViaMCP.create();
        } catch (Throwable e) {
            log("ViaMCP notice: " + e.getMessage());
        }
        log("RedSignalClient Initialization...");
        initializeClient();
    }

    private void initializeClient() {
        log("Registration Commands...");
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
        log("Registration Crash Methods...");
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
        log("Registration Crash Exploits...");
        try {
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
        } catch (Throwable t) {
            log("Exploits registration notice: " + t);
        }

        try {
            registerCustomImages();
        } catch (Throwable t) {
            log("Custom image registration deferred: " + t.getMessage());
        }

        try {
            new DiscordRP().runDiscordRP();
        } catch (Throwable t) {
            log("DiscordRP disabled: " + t.getMessage());
        }

        log("Loading visuals...");
        try {
            clientConfigHelper.readClientConfig();
        } catch (Throwable t) {
            log("Client config load notice: " + t.getMessage());
        }

        log("Loading accounts...");
        try {
            accountHelper.loadAccounts();
        } catch (Throwable t) {
            log("Accounts load notice: " + t.getMessage());
        }

        if (ClientConfigHelper.LAST_LOGIN_WAS_OFFLINE && ClientConfigHelper.LAST_OFFLINE_NICK != null && !ClientConfigHelper.LAST_OFFLINE_NICK.isBlank()) {
            log("Auto offline login: " + ClientConfigHelper.LAST_OFFLINE_NICK);
            try {
                accountHelper.offlineLogin(ClientConfigHelper.LAST_OFFLINE_NICK);
            } catch (Throwable t) {
                log("Offline login deferred: " + t.getMessage());
            }
        }

        log("Loading configs...");
        try {
            configHelper.readBypassList();
        } catch (Throwable t) {
            log("Config bypass notice: " + t.getMessage());
        }

        log("Loading favorites...");
        try {
            FavoritesManager.load();
        } catch (Throwable t) {
            log("Favorites load notice: " + t.getMessage());
        }

        log("Running JavaFx Fixer...");
        try {
            FxFixer.runFxFixer();
        } catch (Throwable t) {
            log("JavaFX Fixer skipped: " + t.getMessage());
        }
    }

    private void registerCustomImages() {
        log("Registration Custom Images...");

        try {
            Map<String, String> imagesToRegister = Map.of(
                    "/client/images/background.png", "textures/gui/custom_background.png",
                    "/client/images/cape.png", "textures/client/cape.png"
            );

            imagesToRegister.forEach((image, mcImageLoc) -> {
                try {
                    InputStream inputStream = Main.class.getResourceAsStream(image);
                    if (inputStream != null) {
                        NativeImage nativeImage = NativeImage.read(inputStream);
                        ResourceLocation customBackground = new ResourceLocation("redsignal", mcImageLoc);
                        if (Minecraft.getInstance() != null && Minecraft.getInstance().getTextureManager() != null) {
                            Minecraft.getInstance().getTextureManager().register(customBackground, new DynamicTexture(nativeImage));
                        }
                        inputStream.close();
                    }
                } catch (Throwable e) {
                    log("Image load notice: " + e.getMessage());
                }
            });
        } catch (Throwable t) {
            log("Image subsystem notice: " + t.getMessage());
        }
    }

    public String getTitle() {
        return "RedSignalClient [1.20.1] - " + VERSION;
    }

}
