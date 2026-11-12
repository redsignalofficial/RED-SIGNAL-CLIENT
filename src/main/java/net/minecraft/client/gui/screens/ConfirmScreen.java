package net.minecraft.client.gui.screens;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ConfirmScreen extends Screen {
   private static final int MARGIN = 20;
   private final Component message;
   private MultiLineLabel multilineMessage = MultiLineLabel.EMPTY;
   protected Component yesButton;
   protected Component noButton;
   protected Component maybeButton;
   private int delayTicker;
   protected final BooleanConsumer callback;
   protected final TriConsumer<Boolean, Boolean, Boolean> triCallback;
   private final List<Button> exitButtons = Lists.newArrayList();
   private final boolean useTriCallback;

   public ConfirmScreen(BooleanConsumer callback, Component title, Component message) {
      this(callback, title, message, CommonComponents.GUI_YES, CommonComponents.GUI_NO);
   }

   public ConfirmScreen(BooleanConsumer callback, Component title, Component message, Component yes, Component no) {
      super(title);
      this.callback = callback;
      this.triCallback = null;
      this.message = message;
      this.yesButton = yes;
      this.noButton = no;
      this.useTriCallback = false;
   }

   public ConfirmScreen(TriConsumer<Boolean, Boolean, Boolean> triCallback, Component title, Component message, Component yes, Component no, Component maybe) {
      super(title);
      this.callback = null;
      this.triCallback = triCallback;
      this.message = message;
      this.yesButton = yes;
      this.noButton = no;
      this.maybeButton = maybe;
      this.useTriCallback = true;
   }

   public Component getNarrationMessage() {
      return CommonComponents.joinForNarration(super.getNarrationMessage(), this.message);
   }

   protected void init() {
      super.init();
      this.multilineMessage = MultiLineLabel.create(this.font, this.message, this.width - 50);
      int buttonY = Mth.clamp(this.messageTop() + this.messageHeight() + 20, this.height / 6 + 96, this.height - 24);
      this.exitButtons.clear();
      this.addButtons(buttonY);
   }

   protected void addButtons(int y) {
      if (useTriCallback) {
         this.addExitButton(Button.builder(this.yesButton, (button) -> {
            this.triCallback.accept(true, false, false);
         }).bounds(this.width / 2 - 155, y, 100, 20).build());

         this.addExitButton(Button.builder(this.noButton, (button) -> {
            this.triCallback.accept(false, true, false);
         }).bounds(this.width / 2 - 50, y, 100, 20).build());

         this.addExitButton(Button.builder(this.maybeButton, (button) -> {
            this.triCallback.accept(false, false, true);
         }).bounds(this.width / 2 + 55, y, 100, 20).build());
      } else {
         this.addExitButton(Button.builder(this.yesButton, (button) -> {
            this.callback.accept(true);
         }).bounds(this.width / 2 - 155, y, 150, 20).build());

         this.addExitButton(Button.builder(this.noButton, (button) -> {
            this.callback.accept(false);
         }).bounds(this.width / 2 - 155 + 160, y, 150, 20).build());
      }
   }

   protected void addExitButton(Button button) {
      this.exitButtons.add(this.addRenderableWidget(button));
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      this.renderBackground(graphics);
      graphics.drawCenteredString(this.font, this.title, this.width / 2, this.titleTop(), 16777215);
      this.multilineMessage.renderCentered(graphics, this.width / 2, this.messageTop());
      super.render(graphics, mouseX, mouseY, partialTicks);
   }

   private int titleTop() {
      int i = (this.height - this.messageHeight()) / 2;
      return Mth.clamp(i - 20 - 9, 10, 80);
   }

   private int messageTop() {
      return this.titleTop() + 20;
   }

   private int messageHeight() {
      return this.multilineMessage.getLineCount() * 9;
   }

   public void setDelay(int delay) {
      this.delayTicker = delay;
      for (Button button : this.exitButtons) {
         button.active = false;
      }
   }

   public void tick() {
      super.tick();
      if (--this.delayTicker == 0) {
         for (Button button : this.exitButtons) {
            button.active = true;
         }
      }
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode == 256) {
         if (useTriCallback) {
            this.triCallback.accept(false, false, false);
         } else {
            this.callback.accept(false);
         }
         return true;
      } else {
         return super.keyPressed(keyCode, scanCode, modifiers);
      }
   }

   @FunctionalInterface
   public interface TriConsumer<A, B, C> {
      void accept(A a, B b, C c);
   }
}
