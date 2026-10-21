package com.mcmagic.omnira.client;

import com.google.gson.JsonArray;
import com.mcmagic.omnira.client.renderer.CrystalGlassLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.BlockState;

/** Native block models only: no world placement, ticking machines or item display transforms. */
final class MiniatureWorldBlocks {
    private record Station(java.util.function.Supplier<BlockState> state,float x,float y,float z,float scale) {}
    private final List<Station> stations;

    MiniatureWorldBlocks(JsonArray entries){
        var list=new java.util.ArrayList<Station>();
        for(var entry:entries){
            var json=entry.getAsJsonObject();var p=json.getAsJsonArray("position");
            var block=ResourceLocation.parse(json.get("block").getAsString());
            var facing=Direction.valueOf(json.get("facing").getAsString().toUpperCase(java.util.Locale.ROOT));
            // Resolve after registries are ready, on first draw; keep only the immutable state.
            var state=com.google.common.base.Suppliers.memoize(()->{
                var value=BuiltInRegistries.BLOCK.get(block).defaultBlockState();
                return value.hasProperty(BlockStateProperties.HORIZONTAL_FACING)?value.setValue(BlockStateProperties.HORIZONTAL_FACING,facing):value;
            });
            list.add(new Station(state,p.get(0).getAsFloat(),p.get(1).getAsFloat(),p.get(2).getAsFloat(),json.get("scale").getAsFloat()));
        }
        stations=List.copyOf(list);
    }

    void draw(float amount,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(amount<=0)return;
        var mc=Minecraft.getInstance();var layers=CrystalGlassLayer.contents(buffers);
        for(var station:stations){
            var state=station.state.get();
            pose.pushPose();pose.translate(station.x,station.y,station.z);
            float size=station.scale*amount;pose.scale(size,size,size);pose.translate(-.5,0,-.5);
            // The cauldron's body belongs to its entity renderer, not the terrain pass.
            if(state.getBlock()==com.mcmagic.omnira.registry.ModBlocks.ROUGH_ALCHEMY_CAULDRON.get())AlchemyClient.renderEmptyCauldron(pose,layers,light,overlay);
            else mc.getBlockRenderer().renderSingleBlock(state,pose,layers,light,overlay);
            pose.popPose();
        }
    }
}
