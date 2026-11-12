/*
 * RED SIGNAL CLIENT
 * Copyright (C) 2026 Rin. All rights reserved.
 */
package de.florianmichael.viamcp;

import com.viaversion.viabackwards.protocol.v1_17to1_16_4.Protocol1_17To1_16_4;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.protocols.v1_16_1to1_16_2.packet.ClientboundPackets1_16_2;
import com.viaversion.viaversion.protocols.v1_16_1to1_16_2.packet.ServerboundPackets1_16_2;
import com.viaversion.viaversion.protocols.v1_16_4to1_17.packet.ClientboundPackets1_17;
import com.viaversion.viaversion.protocols.v1_16_4to1_17.packet.ServerboundPackets1_17;
import de.florianmichael.vialoadingbase.ViaLoadingBase;

import java.io.File;

public class ViaMCP {
    public final static int NATIVE_VERSION = 763;
    public static ViaMCP INSTANCE;

    public static void create() {
        INSTANCE = new ViaMCP();
    }

//    @Getter private AsyncVersionSlider asyncVersionSlider;

    public ViaMCP() {
        ViaLoadingBase.ViaLoadingBaseBuilder.create().runDirectory(new File("ViaMCP")).nativeVersion(NATIVE_VERSION).onProtocolReload(protocolVersion -> {
//            if (getAsyncVersionSlider() != null) {
//                getAsyncVersionSlider().setVersion(protocolVersion.getVersion());
//            }
            // TODO: SLider
        }).build();

        // Add this line if you implement the transaction fixes into the game code
        // fixTransactions();
    }

    private void fixTransactions() {
        // We handle the differences between those versions in the net code, so we can make the Via handlers pass through
        final Protocol1_17To1_16_4 protocol = Via.getManager().getProtocolManager().getProtocol(Protocol1_17To1_16_4.class);
        protocol.registerClientbound(ClientboundPackets1_17.PING, ClientboundPackets1_16_2.CONTAINER_ACK, wrapper -> {}, true);
        protocol.registerServerbound(ServerboundPackets1_16_2.CONTAINER_ACK, ServerboundPackets1_17.PONG, wrapper -> {}, true);
    }

    public void initAsyncSlider() {
        this.initAsyncSlider(5, 5, 110, 20);
    }

    public void initAsyncSlider(int x, int y, int width, int height) {
//        asyncVersionSlider = new AsyncVersionSlider(-1, x, y, Math.max(width, 110), height);
    }

}
