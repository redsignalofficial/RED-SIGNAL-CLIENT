/*
 * RED SIGNAL CLIENT
 * Copyright (C) 2026 Rin. All rights reserved.
 */
package de.florianmichael.vialoadingbase.platform.viaversion;

import com.viaversion.viaversion.api.platform.ViaInjector;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.libs.fastutil.objects.ObjectLinkedOpenHashSet;
import com.viaversion.viaversion.libs.gson.JsonObject;
import de.florianmichael.vialoadingbase.netty.VLBPipeline;

import java.util.SortedSet;

public class VLBViaInjector implements ViaInjector {

    @Override
    public void inject() {
    }

    @Override
    public void uninject() {
    }

    @Override
    public String getDecoderName() {
        return VLBPipeline.VIA_DECODER_HANDLER_NAME;
    }

    @Override
    public String getEncoderName() {
        return VLBPipeline.VIA_ENCODER_HANDLER_NAME;
    }

    @Override
    public SortedSet<ProtocolVersion> getServerProtocolVersions() {
        final SortedSet<ProtocolVersion> versions = new ObjectLinkedOpenHashSet<>();
        versions.addAll(ProtocolVersion.getProtocols());
        return versions;
    }

    @Override
    public ProtocolVersion getServerProtocolVersion() {
        return this.getServerProtocolVersions().first();
    }

    @Override
    public JsonObject getDump() {
        return new JsonObject();
    }
}
