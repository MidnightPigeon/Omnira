package com.mcmagic.omnira.client.renderer;

import com.mcmagic.omnira.block.entity.SpellCoreBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.world.item.ItemStack;

public final class PlacedSpellCoreRenderer implements BlockEntityRenderer<SpellCoreBlockEntity> {
    public PlacedSpellCoreRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public boolean shouldRender(SpellCoreBlockEntity core,net.minecraft.world.phys.Vec3 camera){
        return BlockEntityRenderer.super.shouldRender(core,camera)&&com.mcmagic.omnira.client.LabyrinthDisplayVisibility.visible(core,camera,new net.minecraft.world.phys.AABB(core.getBlockPos()).inflate(.3));
    }
    @Override public void render(SpellCoreBlockEntity core,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        double time=core.getLevel()==null?0:core.getLevel().getGameTime()+partial;
        SpellCoreRenderer.renderCore(new ItemStack(core.getBlockState().getBlock()),pose,buffers,light,overlay,time);
    }
}
