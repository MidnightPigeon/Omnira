package com.mcmagic.omnira.client;

import com.mcmagic.omnira.client.renderer.CrystalGlassLayer;
import com.mcmagic.omnira.client.renderer.EnhancedAlchemyCoreRenderer;
import com.mcmagic.omnira.alchemy.AlchemyMaterials;
import com.mcmagic.omnira.registry.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mcmagic.omnira.alchemy.RoughAlchemyCauldronBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.extensions.common.*;

@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class AlchemyClient {
    private static final ModelResourceLocation MODEL=ModelResourceLocation.standalone(ResourceLocation.parse("omnira:block/rough_alchemy_cauldron"));
    private static final ModelResourceLocation CORE=ModelResourceLocation.standalone(ResourceLocation.parse("omnira:block/rough_alchemical_energy_core"));
    private static void draw(PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        draw(MODEL,pose,buffers,light,overlay);
    }
    private static void draw(ModelResourceLocation model,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var mc=Minecraft.getInstance();
        mc.getItemRenderer().renderModelLists(mc.getModelManager().getModel(model),ItemStack.EMPTY,light,overlay,pose,
                buffers.getBuffer(CrystalGlassLayer.ATLAS));
    }
    @SubscribeEvent public static void models(ModelEvent.RegisterAdditional event){event.register(MODEL);event.register(CORE);
        event.register(EnhancedAlchemyCoreRenderer.FRAME);event.register(EnhancedAlchemyCoreRenderer.HEART);
        event.register(EnhancedAlchemyCoreRenderer.TIME);event.register(EnhancedAlchemyCoreRenderer.SPACE);
    }
    @SubscribeEvent public static void blocks(EntityRenderersEvent.RegisterRenderers event){
        event.registerBlockEntityRenderer(AlchemyMaterials.CORE_ENTITY.get(),context->
                (block,partial,pose,buffers,light,overlay)->EnhancedAlchemyCoreRenderer.render(pose,buffers,light,overlay,block.getLevel().getGameTime()+partial));
        event.registerBlockEntityRenderer(ModBlockEntityTypes.ROUGH_ALCHEMY_CAULDRON.get(),context->
                (block,partial,pose,buffers,light,overlay)->vessel(block,partial,pose,buffers,light,overlay));
    }
    private static void vessel(RoughAlchemyCauldronBlockEntity block,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        double time=block.getLevel().getGameTime()+partial;
        if(block.reaction())heat(pose,buffers,time);
        pose.pushPose();
        if(block.reaction()){
            pose.translate(.5,.005*Math.sin(time*.09),.5);
            pose.mulPose(Axis.ZP.rotationDegrees((float)(.45*Math.sin(time*.045))));
            pose.translate(-.5,0,-.5);
        }
        liquid(block,pose,buffers,light,overlay);
        contents(block,pose,buffers,time,light,overlay);
        draw(pose,buffers,light,overlay);pose.popPose();
    }
    private static void contents(RoughAlchemyCauldronBlockEntity block,PoseStack pose,MultiBufferSource buffers,double time,int light,int overlay){
        MultiBufferSource sorted=CrystalGlassLayer.contents(buffers);
        boolean wet=!block.tank.isEmpty();
        double height=wet?(5+7.5*block.tank.getFluidAmount()/4000.0)/16+.11:.4125;
        for(int i=0;i<4;i++){
            var stack=block.items.getStackInSlot(i);if(stack.isEmpty())continue;
            double angle=i*Math.PI/2+(wet?time*.008:0);
            pose.pushPose();pose.translate(.5+.12*Math.cos(angle),height+(wet?Math.sin(time*.05+i)*.012:0),.5+.12*Math.sin(angle));
            pose.mulPose(Axis.YP.rotationDegrees((float)(i*90+(wet?time*.45:0))));
            pose.scale(.19F,.19F,.19F);
            Minecraft.getInstance().getItemRenderer().renderStatic(stack,ItemDisplayContext.NONE,light,overlay,pose,sorted,block.getLevel(),i);
            pose.popPose();
        }
    }
    private static void heat(PoseStack pose,MultiBufferSource buffers,double time){
        var mesh=buffers.getBuffer(CrystalGlassLayer.COLOR);
        var matrix=pose.last().pose();
        for(int ring=0;ring<2;ring++)for(int i=0;i<16;i++){
            double a=i*Math.PI/8+time*(ring==0?.012:-.018),b=a+Math.PI/10;
            double r=.26+ring*.105,thickness=.018;
            float alpha=(float)(.48+.22*Math.sin(time*.07+i*.4));
            for(var point:new double[][]{{r,a},{r+thickness,a},{r+thickness,b},{r,b}})
                mesh.addVertex(matrix,(float)(.5+point[0]*Math.cos(point[1])),.07F+ring*.025F,(float)(.5+point[0]*Math.sin(point[1])))
                        .setColor(.55F,.9F,1F,alpha);
        }
        for(int i=0;i<6;i++){
            double phase=(time*.008+i/6.0)%1,a=i*Math.PI/3+time*.015;
            float x=(float)(.5+.26*Math.cos(a)),z=(float)(.5+.26*Math.sin(a)),y=(float)(.06+phase*.18),s=.012F;
            float[][] v={{x-s,y-s,z-s},{x+s,y-s,z-s},{x+s,y+s,z-s},{x-s,y+s,z-s},
                    {x-s,y-s,z+s},{x+s,y-s,z+s},{x+s,y+s,z+s},{x-s,y+s,z+s}};
            for(var face:new int[][]{{0,3,2,1},{4,5,6,7},{0,4,7,3},{1,2,6,5},{3,7,6,2},{0,1,5,4}})
                for(int n:face)mesh.addVertex(matrix,v[n][0],v[n][1],v[n][2]).setColor(.7F,.96F,1F,(float)(.8*(1-phase)));
        }
    }
    private static void liquid(RoughAlchemyCauldronBlockEntity block,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var fluid=block.tank.getFluid();if(fluid.isEmpty())return;
        var ext=IClientFluidTypeExtensions.of(fluid.getFluid());
        var sprite=Minecraft.getInstance().getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture(fluid));
        int color=ext.getTintColor(fluid),alpha=color>>>24;if(alpha==0)alpha=255;
        var out=buffers.getBuffer(CrystalGlassLayer.ATLAS);
        int glow=fluid.getFluidType().getLightLevel(fluid),packed=LightTexture.pack(Math.max(glow,LightTexture.block(light)),LightTexture.sky(light));
        float top=(float)((5+7.5*fluid.getAmount()/4000.0)/16),bottom=5/16F;
        // Stepped octagonal bands follow the interior profile of the crystal bowl.
        for(float low=bottom;low<top;){
            float high=Math.min(top,low<7/16F?7/16F:13/16F),r=low<7/16F?3.45F/16:4.45F/16,k=r*.57F;
            float[][] p={{-k,-r},{k,-r},{r,-k},{r,k},{k,r},{-k,r},{-r,k},{-r,-k}};
            for(int i=0;i<8;i++){
                var a=p[i];var b=p[(i+1)%8];
                float nx=a[0]+b[0],nz=a[1]+b[1],length=(float)Math.sqrt(nx*nx+nz*nz);
                float[][] quad={{a[0],low,a[1]},{a[0],high,a[1]},{b[0],high,b[1]},{b[0],low,b[1]}};
                for(var q:quad)out.addVertex(pose.last(),.5F+q[0],q[1],.5F+q[2]).setColor(color>>16&255,color>>8&255,color&255,alpha)
                        .setUv(sprite.getU(q[0]+.5F),sprite.getV(1-q[1])).setOverlay(overlay).setLight(packed)
                        .setNormal(pose.last(),nx/length,0,nz/length);
            }
            if(low==bottom)for(int i=0;i<8;i++){
                var a=p[i];var b=p[(i+1)%8];
                for(var q:new float[][]{{0,low,0},{a[0],low,a[1]},{b[0],low,b[1]},{0,low,0}})
                    out.addVertex(pose.last(),.5F+q[0],q[1],.5F+q[2]).setColor(color>>16&255,color>>8&255,color&255,alpha)
                            .setUv(sprite.getU(q[0]+.5F),sprite.getV(q[2]+.5F)).setOverlay(overlay).setLight(packed).setNormal(pose.last(),0,-1,0);
            }
            if(low>bottom)for(int i=0;i<8;i++){
                var a=p[i];var b=p[(i+1)%8];float ratio=3.45F/4.45F;
                for(var q:new float[][]{{a[0]*ratio,low,a[1]*ratio},{b[0]*ratio,low,b[1]*ratio},{b[0],low,b[1]},{a[0],low,a[1]}})
                    out.addVertex(pose.last(),.5F+q[0],q[1],.5F+q[2]).setColor(color>>16&255,color>>8&255,color&255,alpha)
                            .setUv(sprite.getU(q[0]+.5F),sprite.getV(q[2]+.5F)).setOverlay(overlay).setLight(packed).setNormal(pose.last(),0,-1,0);
            }
            if(high==top)for(int i=0;i<8;i++){
                var a=p[i];var b=p[(i+1)%8];
                for(var q:new float[][]{{0,high,0},{b[0],high,b[1]},{a[0],high,a[1]},{0,high,0}})
                    out.addVertex(pose.last(),.5F+q[0],q[1],.5F+q[2]).setColor(color>>16&255,color>>8&255,color&255,alpha)
                            .setUv(sprite.getU(q[0]+.5F),sprite.getV(q[2]+.5F)).setOverlay(overlay).setLight(packed).setNormal(pose.last(),0,1,0);
            }
            low=high;
        }
    }
    @SubscribeEvent public static void items(RegisterClientExtensionsEvent event){
        event.registerItem(new IClientItemExtensions(){
            private BlockEntityWithoutLevelRenderer renderer;
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer(){
                if(renderer==null)renderer=new BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels()){
                    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
                        if(stack.is(AlchemyMaterials.ENHANCED_CORE_ITEM.get())){
                            EnhancedAlchemyCoreRenderer.render(pose,buffers,light,overlay,net.minecraft.Util.getMillis()/50.0);return;
                        }
                        if(stack.is(ModItems.ROUGH_ALCHEMICAL_ENERGY_CORE.get())){
                            draw(CORE,pose,buffers,light,overlay);return;
                        }
                        if(stack.is(ModItems.PRIMITIVE_REACTION_CAULDRON.get()))heat(pose,buffers,net.minecraft.Util.getMillis()/50.0);
                        draw(pose,buffers,light,overlay);
                    }
                };
                return renderer;
            }
        },ModItems.ROUGH_ALCHEMY_CAULDRON.get(),ModItems.PRIMITIVE_REACTION_CAULDRON.get(),ModItems.ROUGH_ALCHEMICAL_ENERGY_CORE.get(),AlchemyMaterials.ENHANCED_CORE_ITEM.get());
    }
    private AlchemyClient(){}
}
