package com.mcmagic.omnira.client.renderer;
import com.mcmagic.omnira.forging.*;
import com.mcmagic.omnira.item.SpellCoreItem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;

public final class AdvancedForgeRenderer implements BlockEntityRenderer<AdvancedForgeBlockEntity> {
    public static final String[] PARTS={"base","node","button","wand","tongs","needle","hammer","rune","table","panel","mist"};
    public AdvancedForgeRenderer(BlockEntityRendererProvider.Context context){}
    public static ModelResourceLocation model(String part){return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("omnira","block/advanced_assembly_table_"+part));}
    private static void draw(String part,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var mc=Minecraft.getInstance();mc.getItemRenderer().renderModelLists(mc.getModelManager().getModel(model(part)),ItemStack.EMPTY,light,overlay,pose,buffers.getBuffer(CrystalGlassLayer.ATLAS));
    }
    private static void item(ItemStack stack,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(stack.isEmpty())return;pose.pushPose();pose.scale(.25F,.25F,.25F);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack,ItemDisplayContext.FIXED,light,overlay,pose,buffers,Minecraft.getInstance().level,0);pose.popPose();
    }
    private static float smooth(float t){t=Math.clamp(t,0,1);return t*t*(3-2*t);}
    public static int color(ItemStack core,double time){
        return SpellCoreRenderer.effectColor(core,time);
    }
    public void render(AdvancedForgeBlockEntity forge,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        double time=forge.getLevel()==null?0:forge.getLevel().getGameTime()+partial;
        pose.pushPose();pose.translate(.5,0,.5);pose.mulPose(Axis.YP.rotationDegrees(-forge.getBlockState().getValue(AdvancedForgeBlock.FACING).toYRot()));
        renderLocal(forge,forge.progress(partial),time,pose,buffers,light,overlay);pose.popPose();
    }
    public static void renderLocal(AdvancedForgeBlockEntity forge,float progress,double time,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        ItemStack core=forge==null?ItemStack.EMPTY:forge.getItem(9);
        boolean powered=core.getItem() instanceof SpellCoreItem;
        int coreColor=color(core,time);
        MultiBufferSource glow=type->new Tint(buffers.getBuffer(CrystalGlassLayer.ATLAS),coreColor);
        pose.pushPose();pose.translate(-.5,0,-.5);draw("base",pose,buffers,light,overlay);pose.popPose();
        pose.pushPose();
        if(powered)pose.mulPose(Axis.YP.rotationDegrees((float)(time*.15%360)));
        draw("table",pose,buffers,light,overlay);pose.popPose();
        draw("panel",pose,buffers,light,overlay);
        MultiBufferSource buttonTint=type->new Tint(buffers.getBuffer(CrystalGlassLayer.ATLAS),powered?coreColor:0x559FD9,160);
        pose.pushPose();pose.translate(0,.625,forge!=null && forge.buttonTicks()>0?-.71875:-.75);
        draw("button",pose,buttonTint,LightTexture.FULL_BRIGHT,overlay);pose.popPose();
        float approach=progress<=.2F?smooth(progress/.2F):1-smooth((progress-.9F)/.1F);
        for(int n=0;n<3;n++){
            int[] pos=ForgeLayout.NODES[n];double distance=1.04-.68*approach;
            pose.pushPose();pose.translate(pos[0]*distance,1.3075,pos[1]*distance);
            pose.mulPose(Axis.YP.rotationDegrees((float)Math.toDegrees(Math.atan2(-pos[0],-pos[1]))));pose.mulPose(Axis.XP.rotationDegrees(45+45*approach));
            draw("node",pose,buffers,light,overlay);
            if(powered){
                // Four bounded pixel motes orbit beneath each plate, following its full transform.
                for(int j=0;j<4;j++){
                    double phase=time*.035+n*2+j*Math.PI/2;
                    pose.pushPose();pose.translate(Math.cos(phase)*.22,-.16-.025*Math.sin(phase*2),Math.sin(phase)*.22);
                    pose.mulPose(Axis.XP.rotationDegrees(90));pose.scale(.16F,.16F,.16F);
                    draw("button",pose,glow,LightTexture.FULL_BRIGHT,overlay);pose.popPose();
                }
            }
            if(forge!=null){pose.translate(0,.08,0);item(forge.getItem(6+n),pose,buffers,light,overlay);}pose.popPose();
        }
        for(int i=0;i<4;i++){
            float motion=Math.min(smooth((progress-.34F-i*.07F)/.08F),1-smooth((progress-.66F-i*.06F)/.06F));
            // Enclosed tools disappear inside the dense cloud, independent of translucent draw order.
            float visibility=1-smooth((motion-.35F)/.35F);
            if(visibility<=0)continue;
            MultiBufferSource toolBuffers=type->new Tint(buffers.getBuffer(CrystalGlassLayer.ATLAS),0xFFFFFF,(int)(255*visibility));
            double bob=powered?Math.sin(time*.055+i*Math.PI/2)*.045*(1-motion):0;
            int[] at=ForgeLayout.TOOLS[i];pose.pushPose();pose.translate(at[0]*(1-.85*motion),1.4+.2*motion+bob,at[1]*(1-.85*motion));
            pose.mulPose(Axis.ZP.rotationDegrees((i%2==0?1:-1)*20));draw(PARTS[3+i],pose,toolBuffers,light,overlay);
            if(i==2){for(int j=0;j<3;j++){pose.pushPose();pose.translate(Math.sin(time*.06+j*2)*.13,j*.12-.1,Math.cos(time*.06+j*2)*.13);pose.scale(.35F,.35F,.35F);draw("rune",pose,toolBuffers,LightTexture.FULL_BRIGHT,overlay);pose.popPose();}}
            pose.popPose();
        }
        if(forge!=null){
            if(powered){
                // Render the core itself, without the inventory model's additional display scaling.
                pose.pushPose();pose.translate(0,.56,0);pose.scale(.95F,.95F,.95F);pose.translate(-.5,-.5,-.5);
                SpellCoreRenderer.renderCore(core,pose,buffers,LightTexture.FULL_BRIGHT,overlay,time);pose.popPose();
            }
            for(int i=0;i<6;i++){double a=i*Math.PI/3;pose.pushPose();pose.translate(Math.cos(a)*.42,1.2125,Math.sin(a)*.42);item(forge.getItem(i),pose,buffers,light,overlay);pose.popPose();}
            pose.pushPose();pose.translate(0,1.2825,0);item(forge.output(),pose,buffers,light,overlay);pose.popPose();
        }
        if(progress>=.2 && progress<1){
            float growth=smooth((progress-.2F)/.12F),fade=1-smooth((progress-.92F)/.08F);
            float size=growth*fade;
            MultiBufferSource tinted=type->new Tint(buffers.getBuffer(CrystalGlassLayer.FORGE_MIST),coreColor,(int)(245*fade));
            pose.pushPose();pose.translate(0,1.125,0);pose.scale(size,size,size);
            pose.mulPose(Axis.YP.rotationDegrees((float)(time*.6%360)));
            draw("mist",pose,tinted,LightTexture.FULL_BRIGHT,overlay);pose.popPose();
        }
    }
    private record Tint(VertexConsumer parent,int color,int alpha) implements VertexConsumer {
        Tint(VertexConsumer parent,int color){this(parent,color,255);}
        public VertexConsumer addVertex(float x,float y,float z){parent.addVertex(x,y,z);return this;}
        public VertexConsumer setColor(int r,int g,int b,int a){parent.setColor((color>>16)&255,(color>>8)&255,color&255,a*alpha/255);return this;}
        public VertexConsumer setUv(float u,float v){parent.setUv(u,v);return this;}
        public VertexConsumer setUv1(int u,int v){parent.setUv1(u,v);return this;}
        public VertexConsumer setUv2(int u,int v){parent.setUv2(u,v);return this;}
        public VertexConsumer setNormal(float x,float y,float z){parent.setNormal(x,y,z);return this;}
    }
}
