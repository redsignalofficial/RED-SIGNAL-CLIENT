package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.item.ItemStack;
import us.rin.RedSignalClient;

public class ServerboundSetCreativeModeSlotPacket implements Packet<ServerGamePacketListener> {
   private final int slotNum;
   private final ItemStack itemStack;

   public ServerboundSetCreativeModeSlotPacket(int p_134553_, ItemStack p_134554_) {
      this.slotNum = p_134553_;
      this.itemStack = p_134554_.copy();
   }

   public void handle(ServerGamePacketListener p_134560_) {
      p_134560_.handleSetCreativeModeSlot(this);
   }

   public ServerboundSetCreativeModeSlotPacket(FriendlyByteBuf p_179760_) {
      this.slotNum = p_179760_.readShort();
      this.itemStack = p_179760_.readItem();
   }

   public void write(FriendlyByteBuf p_134563_) {
      p_134563_.writeShort(this.slotNum);
      p_134563_.writeItem(this.itemStack);

      if(this.itemStack.getTag() != null && RedSignalClient.DEBUG_MODE) {
         this.itemStack.getTag().getAllKeys().forEach(e -> {
            System.out.println("[ItemStack NBT Debug] key: " + e);
            System.out.println("-> Value: " + this.itemStack.getTag().get(e));
         });
      }
   }

   public int getSlotNum() {
      return this.slotNum;
   }

   public ItemStack getItem() {
      return this.itemStack;
   }
}