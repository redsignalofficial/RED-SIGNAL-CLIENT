package net.minecraft.client.gui.components;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import us.whitedev.gui.clickgui.ClickGui;
import us.whitedev.gui.common.RenderUtils;

import java.awt.*;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractButton extends AbstractWidget {
   protected static final int TEXTURE_Y_OFFSET = 46;
   protected static final int TEXTURE_WIDTH = 200;
   protected static final int TEXTURE_HEIGHT = 20;
   protected static final int TEXTURE_BORDER_X = 20;
   protected static final int TEXTURE_BORDER_Y = 4;
   protected static final int TEXT_MARGIN = 2;

   public AbstractButton(int p_93365_, int p_93366_, int p_93367_, int p_93368_, Component p_93369_) {
      super(p_93365_, p_93366_, p_93367_, p_93368_, p_93369_);
   }

   public abstract void onPress();

   protected void renderWidget(GuiGraphics p_281670_, int p_282682_, int p_281714_, float p_282542_) {
      Minecraft minecraft = Minecraft.getInstance();
      if(ClickGui.CLIENT_STYLE == ClickGui.ClientStyle.MINECRAFT){
         p_281670_.setColor(1.0F, 1.0F, 1.0F, this.alpha);
         RenderSystem.enableBlend();
         RenderSystem.enableDepthTest();
         p_281670_.blitNineSliced(WIDGETS_LOCATION, this.getX(), this.getY(), this.getWidth(), this.getHeight(), 20, 4, 200, 20, 0, this.getTextureY());
         p_281670_.setColor(1.0F, 1.0F, 1.0F, 1.0F);
         int i = this.active ? 16777215 : 10526880;
         this.renderString(p_281670_, minecraft.font, i | Mth.ceil(this.alpha * 255.0F) << 24);
      }else {
         p_281670_.setColor(1.0F, 1.0F, 1.0F, this.alpha);
         RenderSystem.enableBlend();
         RenderSystem.enableDepthTest();

         float aMul = Mth.clamp(this.alpha, 0.0F, 1.0F);
         if (!this.active) {
            aMul *= 0.55F;
         }

         int x = this.getX();
         int y = this.getY();
         int w = this.getWidth();
         int h = this.getHeight();

         int radius = 10;

         if (this.isHoveredOrFocused() && this.active) {
            int glow = new Color(108, 124, 255, (int)(46 * aMul)).getRGB();
            RenderUtils.drawRoundedRect(p_281670_.pose(), x - 2, y - 2, w + 4, h + 4, radius + 2, glow);
         }

         p_281670_.fillRounded(x, y, w, h, radius, new Color(16, 19, 25, (int)(255 * aMul)).getRGB());
         RenderUtils.drawRoundedRectOutline(p_281670_.pose(), x, y, w, h, radius, 1, new Color(255, 255, 255, (int)(10 * aMul)).getRGB());

         String text = this.getMessage().getString();
         int fontSize = 13;
         boolean bold = false;
         float textWidth = RenderUtils.getCustomStringWidth(text, bold, fontSize);
         int textHeight = RenderUtils.getCustomFontHeight(fontSize, bold);

         float textX = x + (w - textWidth) / 2f;
         float baselineY = y + (h - textHeight) / 2f;
         int textColor = new Color(230, 234, 242, (int)(255 * aMul)).getRGB();
         RenderUtils.drawCustomString(p_281670_.pose(), text, textX, baselineY, textColor, bold, fontSize);

         p_281670_.setColor(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   public void renderString(GuiGraphics p_283366_, Font p_283054_, int p_281656_) {
      this.renderScrollingString(p_283366_, p_283054_, 2, p_281656_);
   }

   private int getTextureY() {
      int i = 1;
      if (!this.active) {
         i = 0;
      } else if (this.isHoveredOrFocused()) {
         i = 2;
      }

      return 46 + i * 20;
   }

   public void onClick(double p_93371_, double p_93372_) {
      this.onPress();
   }

   public boolean keyPressed(int p_93374_, int p_93375_, int p_93376_) {
      if (this.active && this.visible) {
         if (CommonInputs.selected(p_93374_)) {
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            this.onPress();
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }
}