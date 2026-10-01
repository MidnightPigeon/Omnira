package com.mcmagic.omnira.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.*;

public final class SpellCoreRenderer extends BlockEntityWithoutLevelRenderer {
    public static int effectColor(ItemStack stack,double time){
        if(!(stack.getItem() instanceof com.mcmagic.omnira.item.SpellCoreItem core))return 0xBBD8EC;
        double phase=.5+.5*Math.sin(time/17);
        return switch(core.kind()){
            case DREAM -> ((int)(120+55*phase)<<16)|((int)(175-55*phase)<<8)|255;
            case LIGHT_DARK -> {int shade=(int)(96+159*phase);yield shade<<16|shade<<8|shade;}
            case SPACETIME -> {int green=(int)(190+48*phase),blue=(int)(225+30*(1-phase));yield 0x85<<16|green<<8|blue;}
            default -> 0xBBD8EC;
        };
    }
    public SpellCoreRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());
    }
    public static ModelResourceLocation model(String part) {
        return CrystalGridRenderer.part("primordial_spell_core",part);
    }
    public static ModelResourceLocation dreamModel(String part) {return CrystalGridRenderer.part("dream_spell_core",part);}
    public static ModelResourceLocation lightDarkModel(String part) {return CrystalGridRenderer.part("light_dark_spell_core",part);}
    public static ModelResourceLocation spacetimeModel(String part) {return CrystalGridRenderer.part("spacetime_spell_core",part);}
    private static void draw(String part,ItemStack stack,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var mc=Minecraft.getInstance();
        boolean dream=stack.is(com.mcmagic.omnira.registry.ModItems.DREAM_SPELL_CORE.get());
        boolean lightDark=stack.getItem() instanceof com.mcmagic.omnira.item.SpellCoreItem core
                && core.kind()==com.mcmagic.omnira.item.SpellCoreItem.Kind.LIGHT_DARK;
        boolean spacetime=stack.is(com.mcmagic.omnira.registry.ModItems.SPACETIME_SPELL_CORE.get());
        mc.getItemRenderer().renderModelLists(mc.getModelManager().getModel(spacetime?spacetimeModel(part):lightDark?lightDarkModel(part):dream?dreamModel(part):model(part)),stack,light,overlay,
                pose,buffers.getBuffer(CrystalGlassLayer.ATLAS));
    }
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,
                                      MultiBufferSource buffers,int light,int overlay) {
        var mc=Minecraft.getInstance();
        double time=mc.level==null?0:mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false);
        renderCore(stack,pose,buffers,light,overlay,time);
    }
    public static void renderCore(ItemStack stack,PoseStack pose,MultiBufferSource buffers,int light,int overlay,double time) {
        boolean dream=stack.is(com.mcmagic.omnira.registry.ModItems.DREAM_SPELL_CORE.get());
        boolean spacetime=stack.is(com.mcmagic.omnira.registry.ModItems.SPACETIME_SPELL_CORE.get());
        pose.pushPose();
        if(dream) {
            float pulse=1+(float)Math.sin(time*.065)*.035F;
            pose.translate(.5,.5,.5);pose.scale(pulse,2-pulse,pulse);pose.translate(-.5,-.5,-.5);
        }
        if(spacetime) for(int i=0;i<9;i++) {
            double angle=i*2.3999632297+time*(.006+i*.0005),radius=.11+(i%3)*.055;
            double x=Math.cos(angle)*radius,y=((i*7)%9-4)*.045+Math.sin(time*.017+i)*.025,z=Math.sin(angle)*radius;
            pose.pushPose();pose.translate(.5+x-.03125,.5+y-.03125,.5+z-.03125);
            draw("star",stack,pose,buffers,LightTexture.FULL_BRIGHT,overlay);pose.popPose();
        }
        draw("body",stack,pose,buffers,light,overlay);
        pose.popPose();
        for(int ring=0;ring<2;ring++) {
            pose.pushPose();
            pose.translate(.5,.5,.5);
            pose.mulPose(Axis.ZP.rotationDegrees(ring==0?35:-55));
            pose.mulPose(Axis.XP.rotationDegrees(ring==0?25:75));
            for(int i=0;i<3;i++) {
                double a=time*.025*(ring==0?1:-1)+i*Math.PI*2/3;
                pose.pushPose();
                pose.translate(Math.cos(a)*.41-.03125,Math.sin(a)*.41-.03125,-.03125);
                draw(spacetime?((i+ring+(int)(time/24))&1)==0?"mote_blue":"mote_green":"mote",stack,pose,buffers,LightTexture.FULL_BRIGHT,overlay);
                pose.popPose();
            }
            pose.popPose();
        }
    }
}
