///*
 * RED SIGNAL CLIENT
 * Copyright (C) 2026 Rin. All rights reserved.
 */
//
//package de.florianmichael.viamcp.fixes;
//
//import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
//import de.florianmichael.vialoadingbase.ViaLoadingBase;
//import de.florianmichael.viamcp.ViaMCP;
//import net.minecraft.client.Minecraft;
//import net.minecraft.entity.Entity;
//import net.minecraft.entity.player.EntityPlayer;
//import net.minecraft.util.MovingObjectPosition;
//
//public class AttackOrder {
//    private final static Minecraft mc = Minecraft.getMinecraft();
//
//    public static void sendConditionalSwing(MovingObjectPosition mop) {
//        if (mop != null && mop.typeOfHit != MovingObjectPosition.MovingObjectType.ENTITY) mc.thePlayer.swingItem();
//    }
//
//    public static void sendFixedAttack(EntityPlayer entityIn, Entity target) {
//        if (ViaLoadingBase.getInstance().getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
//            mc.thePlayer.swingItem();
//            mc.playerController.attackEntity(entityIn, target);
//        } else {
//            mc.playerController.attackEntity(entityIn, target);
//            mc.thePlayer.swingItem();
//        }
//    }
//}
