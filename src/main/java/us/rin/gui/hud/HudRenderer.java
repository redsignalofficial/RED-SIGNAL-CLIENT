package us.rin.gui.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import us.rin.gui.clickgui.ClickGui;
import us.rin.gui.hud.utils.HudStyleManager;
import us.rin.gui.hud.utils.NotificationManager;

public class HudRenderer {
    private static final Minecraft mc = Minecraft.getInstance();

    public static void renderHud(GuiGraphics graphics) {
        if (mc.player == null ||
                !mc.player.connection.getConnection().isConnected() ||
                ClickGui.HUD_PLACE == ClickGui.HudPlace.HIDE
        ) return;

        HudStyleManager.render(graphics);
        NotificationManager.render(graphics);
    }
}
