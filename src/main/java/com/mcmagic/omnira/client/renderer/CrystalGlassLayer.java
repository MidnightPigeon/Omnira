package com.mcmagic.omnira.client.renderer;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterRenderBuffersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Transparent crystal surfaces must not become depth occluders for the later terrain pass. */
@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public abstract class CrystalGlassLayer extends RenderType {
    private static final java.util.Map<RenderType,ResourceLocation> ENTITY_TEXTURES=new java.util.IdentityHashMap<>();
    private static final java.util.function.Function<net.minecraft.resources.ResourceLocation,RenderType> ENTITY=net.minecraft.Util.memoize(texture->create(
            "omnira_glass_entity",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,1536,false,true,
            CompositeState.builder().setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER).setTextureState(new TextureStateShard(texture,false,false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE).createCompositeState(false)));
    public static final RenderType HERON=ENTITY.apply(ResourceLocation.fromNamespaceAndPath("omnira","textures/entity/night_heron_feathers.png"));
    public static RenderType entity(net.minecraft.resources.ResourceLocation texture){return ENTITY.apply(texture);}
    public static final RenderType ARMOR_OUTER=ENTITY.apply(ResourceLocation.fromNamespaceAndPath("omnira","textures/models/armor/crystal_armor_layer_1.png"));
    public static final RenderType ARMOR_INNER=ENTITY.apply(ResourceLocation.fromNamespaceAndPath("omnira","textures/models/armor/crystal_armor_layer_2.png"));
    public static final RenderType AMBER=ENTITY.apply(ResourceLocation.fromNamespaceAndPath("omnira","textures/entity/solid_white.png"));
    public static final RenderType ARCHAEOPTERYX=ENTITY.apply(ResourceLocation.fromNamespaceAndPath("omnira","textures/entity/archaeopteryx_spirit.png"));
    public static final RenderType STAR_JELLYFISH=ENTITY.apply(ResourceLocation.parse("omnira:textures/entity/star_jellyfish.png"));
    public static final RenderType LIGHT_SPIRIT=ENTITY.apply(ResourceLocation.parse("omnira:textures/entity/light_spirit.png"));
    public static final RenderType SHADOW_GHOST=ENTITY.apply(ResourceLocation.parse("omnira:textures/entity/shadow_ghost.png"));
    public static final RenderType SHADOW_MIST=ENTITY.apply(ResourceLocation.parse("omnira:textures/entity/shadow_mist.png"));
    private static final java.util.List<RenderType> ENTITY_LAYERS=java.util.List.of(HERON,ARMOR_OUTER,ARMOR_INNER,AMBER,ARCHAEOPTERYX,STAR_JELLYFISH,LIGHT_SPIRIT,SHADOW_GHOST,SHADOW_MIST);
    static {
        String[] textures={"entity/night_heron_feathers","models/armor/crystal_armor_layer_1","models/armor/crystal_armor_layer_2",
                "entity/solid_white","entity/archaeopteryx_spirit","entity/star_jellyfish","entity/light_spirit","entity/shadow_ghost","entity/shadow_mist"};
        for(int i=0;i<textures.length;i++)ENTITY_TEXTURES.put(ENTITY_LAYERS.get(i),ResourceLocation.parse("omnira:"+textures[i]));
    }
    static ResourceLocation atlasTexture(RenderType type){return ENTITY_TEXTURES.get(type);}
    public static MultiBufferSource contents(MultiBufferSource source){
        return type->source.getBuffer(type==Sheets.translucentItemSheet() || type==Sheets.translucentCullBlockSheet()?ATLAS:type);
    }
    private CrystalGlassLayer(){super("omnira_glass",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,1536,false,true,()->{},()->{});}
    public static final RenderType ATLAS=create("omnira_glass",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,1536,false,true,
            CompositeState.builder().setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_CULL_SHADER)
                    .setTextureState(new TextureStateShard(InventoryMenu.BLOCK_ATLAS,false,false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setLightmapState(LIGHTMAP).setOverlayState(OVERLAY)
                    .setWriteMaskState(COLOR_WRITE).createCompositeState(false));
    public static final RenderType COLOR=create("omnira_glass_color",DefaultVertexFormat.POSITION_COLOR,VertexFormat.Mode.QUADS,1536,false,true,
            CompositeState.builder().setShaderState(POSITION_COLOR_SHADER).setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
    // Dense processing mist intentionally occludes later glass and translucent terrain.
    public static final RenderType FORGE_MIST=create("omnira_forge_mist",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,1536,false,true,
            CompositeState.builder().setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_CULL_SHADER)
                    .setTextureState(new TextureStateShard(InventoryMenu.BLOCK_ATLAS,false,false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setLightmapState(LIGHTMAP).setOverlayState(OVERLAY)
                    .setWriteMaskState(COLOR_DEPTH_WRITE).createCompositeState(false));
    @SubscribeEvent public static void registerBuffers(RegisterRenderBuffersEvent event){
        event.registerRenderBuffer(ATLAS);
        ENTITY_LAYERS.forEach(event::registerRenderBuffer);
        event.registerRenderBuffer(COLOR);
        event.registerRenderBuffer(FORGE_MIST);
    }
    @SubscribeEvent public static void finishWorldGlass(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES)return;
        CrystalInteriorMotes.flush();
        var buffers=Minecraft.getInstance().renderBuffers().bufferSource();
        // World glass is intercepted into WorldGlassBuffers; mist and local fallbacks stay here.
        buffers.endLastBatch();
        buffers.endBatch(FORGE_MIST);
        buffers.endBatch(Sheets.translucentCullBlockSheet());
        buffers.endBatch(COLOR);
        buffers.endBatch(ATLAS);
        ENTITY_LAYERS.forEach(buffers::endBatch);
    }
}
