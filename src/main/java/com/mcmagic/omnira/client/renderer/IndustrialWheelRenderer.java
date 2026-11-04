package com.mcmagic.omnira.client.renderer;
import com.mcmagic.omnira.registry.ModDataComponents;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
public final class IndustrialWheelRenderer extends BlockEntityWithoutLevelRenderer {
    public IndustrialWheelRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    public static ModelResourceLocation model(String part){return ModelResourceLocation.standalone(ResourceLocation.parse("omnira:item/industrial_wheel/"+part));}
    public static ModelResourceLocation gear(boolean large){return ModelResourceLocation.standalone(ResourceLocation.parse("create:block/"+(large?"large_":"")+"cogwheel_shaftless"));}
    private static void draw(ModelResourceLocation model,ItemStack stack,PoseStack p,MultiBufferSource b,int light,int overlay,boolean glass){
        var mc=Minecraft.getInstance();mc.getItemRenderer().renderModelLists(mc.getModelManager().getModel(model),stack,light,overlay,p,b.getBuffer(glass?CrystalGlassLayer.ATLAS:RenderType.solid()));
    }
    @Override public void renderByItem(ItemStack s,ItemDisplayContext c,PoseStack p,MultiBufferSource b,int light,int overlay){
        if(!com.mcmagic.omnira.item.IndustrialWheelItem.available())return;
        var mc=Minecraft.getInstance();double time=mc.level==null?0:mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false);
        double angle=com.mcmagic.omnira.item.IndustrialWheelItem.rotation(s,time);
        p.pushPose();
        if(c==ItemDisplayContext.GUI)p.translate(0,-11.78/16,0);
        StaffCastMotion.apply(s,c,p,time);
        // The shaft is authored upward from Y=0, unlike the modular staff's signed coordinates.
        // Move its middle grip into the hand after applying motion about the hand's fixed pivot.
        switch(c){
            case FIRST_PERSON_LEFT_HAND,FIRST_PERSON_RIGHT_HAND,THIRD_PERSON_LEFT_HAND,THIRD_PERSON_RIGHT_HAND -> p.translate(0,-8.0/16,0);
            default -> {}
        }
        draw(model("body"),s,p,b,light,overlay,false);draw(model("glass"),s,p,b,light,overlay,true);
        p.pushPose();p.translate(.5,27./16,.5);p.scale(.8F,.8F,.8F);p.translate(-.5,-27./16,-.5);draw(model("support"),s,p,b,light,overlay,false);p.popPose();
        for(int i=0;i<5;i++){
            double x=i==0?8:8+((i==1||i==2)?-4.48:4.48),z=i==0?8:8+((i==1||i==3)?-4.48:4.48);
            float a=(float)(i==0?angle:-2*angle+22.5);
            p.pushPose();p.translate(x/16,29.4/16,z/16);p.mulPose(Axis.YP.rotationDegrees(a));p.scale(.28F,.28F,.28F);p.translate(-.5,-.5,-.5);draw(gear(i==0),s,p,b,light,overlay,false);p.popPose();
            if(i>0){p.pushPose();p.translate(x/16,29.4/16,z/16);p.mulPose(Axis.YP.rotationDegrees(a));p.scale(.8F,.8F,.8F);p.translate(-.5,-.5,-.5);draw(model("vane"),s,p,b,light,overlay,false);p.popPose();}
        }
        p.pushPose();p.translate(.5,30.2/16,.5);p.mulPose(Axis.YP.rotationDegrees((float)angle));p.scale(.72F,.72F,.72F);p.mulPose(Axis.XP.rotationDegrees(-90));p.translate(-.5,-.5,-12./16);draw(model("hand"),s,p,b,light,overlay,true);p.popPose();
        var rune=com.mcmagic.omnira.item.IndustrialWheelItem.rune(s);
        var part=com.mcmagic.omnira.item.staff.StaffPart.of(rune);
        if(part!=null){
            p.pushPose();
            p.translate(.5,36.0/16,.5);
            p.mulPose(Axis.YP.rotationDegrees((float)(time*.6%360)));
            p.translate(6.5/16,0,0);
            p.mulPose(Axis.YP.rotationDegrees(90));
            p.scale(.28F,.28F,.28F);
            p.translate(-.5,-.5,-.5);
            var glyph=mc.getModelManager().getModel(ModularStaffRenderer.part(part.model()));
            for(var pass:glyph.getRenderPasses(rune,true))for(var layer:pass.getRenderTypes(rune,true))
                mc.getItemRenderer().renderModelLists(pass,rune,light,overlay,p,b.getBuffer(layer));
            p.popPose();
        }
        p.popPose();
    }
}
