package com.mcmagic.omnira.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public abstract class WorldGlassShader extends RenderType {
    private static ShaderInstance shader;
    private static final RenderType MAIN=layer("omnira_world_glass",false);
    private static final RenderType FABULOUS=layer("omnira_world_glass_fabulous",true);
    private static RenderType layer(String name,boolean separateTarget){return create(name,DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,65536,false,true,
            CompositeState.builder().setShaderState(new ShaderStateShard(()->shader))
                    .setTextureState(new TextureStateShard(InventoryMenu.BLOCK_ATLAS,false,false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setLightmapState(LIGHTMAP).setOverlayState(OVERLAY)
                    .setCullState(NO_CULL).setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(separateTarget?COLOR_DEPTH_WRITE:COLOR_WRITE)
                    .setOutputState(TRANSLUCENT_TARGET).createCompositeState(false));}
    // Fabulous needs a nearest depth in its separate target for the vanilla six-layer compositor.
    // Fast/Fancy must not put that depth into the main world buffer.
    static RenderType layer(){return Minecraft.useShaderTransparency()?FABULOUS:MAIN;}
    private WorldGlassShader(){super("omnira_world_glass",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,65536,false,true,()->{},()->{});}
    @SubscribeEvent public static void shaders(RegisterShadersEvent event) throws java.io.IOException {
        WorldGlassBuffers.reset();
        event.registerShader(new ShaderInstance(event.getResourceProvider(),ResourceLocation.parse("omnira:world_glass"),DefaultVertexFormat.NEW_ENTITY),loaded->shader=loaded);
    }
}
