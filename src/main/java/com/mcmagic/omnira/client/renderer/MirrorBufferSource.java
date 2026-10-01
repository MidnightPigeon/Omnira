package com.mcmagic.omnira.client.renderer;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.LinkedHashMap;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

/** Pass-local buffers: foil and base-material consumers must remain writable together. */
public final class MirrorBufferSource extends MultiBufferSource.BufferSource implements AutoCloseable {
    public MirrorBufferSource() {
        super(new ByteBufferBuilder(256), new LinkedHashMap<>());
    }

    @Override
    public VertexConsumer getBuffer(RenderType type) {
        // Include modded layers too; a whitelist of vanilla glint layers is insufficient.
        fixedBuffers.computeIfAbsent(type, ignored -> new ByteBufferBuilder(4096));
        return super.getBuffer(type);
    }

    @Override
    public void close() {
        // Do not submit incomplete geometry when unwinding a failed render pass.
        startedBuilders.clear();
        lastSharedType = null;
        fixedBuffers.values().forEach(ByteBufferBuilder::close);
        fixedBuffers.clear();
        sharedBuffer.close();
    }
}
