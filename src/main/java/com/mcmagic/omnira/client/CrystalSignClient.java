package com.mcmagic.omnira.client;

import com.google.gson.*;
import com.mcmagic.omnira.client.renderer.CrystalGlassLayer;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.sign.*;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class CrystalSignClient {
    private record Part(float[] from,float[] to,boolean rim){}
    private static final java.util.List<Part> PARTS=parts();
    private static java.util.List<Part> parts(){
        try(var reader=new java.io.InputStreamReader(java.util.Objects.requireNonNull(CrystalSignClient.class.getResourceAsStream(
                "/assets/omnira/geometry/crystal_sign.json")),java.nio.charset.StandardCharsets.UTF_8)){
            var result=new java.util.ArrayList<Part>();
            for(var entry:JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("parts")){
                var part=entry.getAsJsonObject();float[] from=new float[3],to=new float[3];
                for(int i=0;i<3;i++){from[i]=part.getAsJsonArray("from").get(i).getAsFloat()/16;to[i]=part.getAsJsonArray("to").get(i).getAsFloat()/16;}
                result.add(new Part(from,to,part.get("material").getAsString().equals("rim")));
            }return java.util.List.copyOf(result);
        }catch(java.io.IOException ex){throw new IllegalStateException("Cannot read crystal sign geometry",ex);}
    }
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){
        event.registerBlockEntityRenderer(ModBlockEntityTypes.CRYSTAL_SIGN.get(),context->new BlockEntityRenderer<CrystalSignEntity>(){
            public void render(CrystalSignEntity sign,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
                var state=sign.getBlockState();boolean hanging=CrystalSigns.hanging(state.getBlock()),wall=state.getBlock() instanceof WallSignBlock;
                pose.pushPose();pose.translate(.5,wall?.55:hanging?.35:.8,.5);
                pose.mulPose(Axis.YP.rotationDegrees(-((SignBlock)state.getBlock()).getYRotationDegrees(state)));
                if(wall)pose.translate(0,0,-.4375);
                draw(pose,buffers,!wall&&!hanging,hanging,sign.wisdom(),sign.getLevel().getGameTime()+partial,light,overlay);
                text(sign.getFrontText(),pose,buffers,light,true,sign.getMaxTextLineWidth());
                if(sign.wisdom())text(sign.getBackText(),pose,buffers,light,false,sign.getMaxTextLineWidth());
                pose.popPose();
            }
            @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(CrystalSignEntity sign){
                return new net.minecraft.world.phys.AABB(sign.getBlockPos()).inflate(.125);
            }
        });
    }
    private static void text(SignText text,PoseStack pose,MultiBufferSource buffers,int light,boolean front,int maxWidth){
        var font=Minecraft.getInstance().font;pose.pushPose();if(!front)pose.mulPose(Axis.YP.rotationDegrees(180));
        pose.translate(0,0,.0335);pose.scale(1/120F,-1/120F,1/120F);
        var lines=text.getRenderMessages(Minecraft.getInstance().isTextFilteringEnabled(),component->{
            var split=font.split(component,maxWidth);return split.isEmpty()?net.minecraft.util.FormattedCharSequence.EMPTY:split.getFirst();
        });
        int dark=SignRenderer.getDarkColor(text),color=text.hasGlowingText()?text.getColor().getTextColor():dark;
        for(int i=0;i<4;i++){
            float x=-font.width(lines[i])/2F,y=i*10-20;
            if(text.hasGlowingText())font.drawInBatch8xOutline(lines[i],x,y,color,dark,pose.last().pose(),buffers,LightTexture.FULL_BRIGHT);
            else font.drawInBatch(lines[i],x,y,color,false,pose.last().pose(),buffers,Font.DisplayMode.POLYGON_OFFSET,0,light);
        }
        pose.popPose();
    }
    private static void draw(PoseStack pose,MultiBufferSource buffers,boolean floor,boolean hanging,boolean wisdom,double time,int light,int overlay){
        for(var part:PARTS)box(pose,buffers,part.from,part.to,part.rim,wisdom&&part.rim,light,overlay);
        if(floor)box(pose,buffers,new float[]{-.045F,-.8F,-.025F},new float[]{.045F,-.25F,.025F},true,false,light,overlay);
        if(hanging)for(float x:new float[]{-.32F,.32F})box(pose,buffers,new float[]{x-.015F,.25F,-.015F},new float[]{x+.015F,.65F,.015F},true,false,light,overlay);
        if(wisdom){
            var out=buffers.getBuffer(CrystalGlassLayer.COLOR);
            for(int i=0;i<5;i++){
                double a=time*.035+i*Math.PI*.4;float x=(float)(Math.cos(a)*.55),y=(float)(Math.sin(a)*.31),z=(float)(Math.sin(a*.7+i)*.08);
                var vertices=points(new float[]{x-.009F,y-.009F,z-.009F},new float[]{x+.009F,y+.009F,z+.009F});
                for(var face:FACES)for(int n:face)out.addVertex(pose.last().pose(),vertices[n][0],vertices[n][1],vertices[n][2]).setColor(0xDAFFFFFF);
            }
        }
    }
    private static final int[][] FACES={{0,3,2,1},{4,5,6,7},{0,4,7,3},{1,2,6,5},{3,7,6,2},{0,1,5,4}};
    private static final int[][] NORMALS={{0,0,-1},{0,0,1},{-1,0,0},{1,0,0},{0,1,0},{0,-1,0}};
    private static float[][] points(float[] a,float[] b){return new float[][]{{a[0],a[1],a[2]},{b[0],a[1],a[2]},{b[0],b[1],a[2]},{a[0],b[1],a[2]},
            {a[0],a[1],b[2]},{b[0],a[1],b[2]},{b[0],b[1],b[2]},{a[0],b[1],b[2]}};}
    private static void box(PoseStack pose,MultiBufferSource buffers,float[] a,float[] b,boolean rim,boolean whiteRim,int light,int overlay){
        // World color vertices now share the panel/pool quad sort without changing their lighting.
        if(whiteRim){
            var out=buffers.getBuffer(CrystalGlassLayer.COLOR);var vertices=points(a,b);
            for(int f=0;f<6;f++)if(!hidden(a,b,f))for(int n:FACES[f])
                out.addVertex(pose.last().pose(),vertices[n][0],vertices[n][1],vertices[n][2]).setColor(0xEDF5FAFF);
            return;
        }
        var sprite=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ResourceLocation.fromNamespaceAndPath("omnira",
                rim?"block/workstation_crystal_ring":"block/spiritual_crystal_clear"));
        var out=buffers.getBuffer(CrystalGlassLayer.ATLAS);var vertices=points(a,b);int color=rim?0xDDE0F5FF:0xBADEEEFF;
        for(int f=0;f<6;f++){
            // Remove the shared internal faces of the five adjoining panel solids.
            if(hidden(a,b,f))continue;
            var face=FACES[f];var start=vertices[face[0]];var across=vertices[face[3]];var down=vertices[face[1]];
            float width=distance(start,across),height=distance(start,down);
            for(int j=0;j<4;j++){
                var v=vertices[face[j]];float u=j>=2?width:0,w=j==1||j==2?height:0;
                out.addVertex(pose.last(),v[0],v[1],v[2]).setColor(color).setUv(sprite.getU(u),sprite.getV(w))
                        .setOverlay(overlay).setLight(light).setNormal(pose.last(),NORMALS[f][0],NORMALS[f][1],NORMALS[f][2]);
            }
        }
    }
    private static boolean hidden(float[] a,float[] b,int face){
        int axis=face<2?2:face<4?0:1;boolean positive=face==1||face==3||face==4;float plane=positive?b[axis]:a[axis];
        for(var part:PARTS){
            if(a==part.from)continue;
            if(Math.abs(plane-(positive?part.from[axis]:part.to[axis]))>.00001)continue;
            boolean covers=true;for(int i=0;i<3;i++)if(i!=axis&&(part.from[i]>a[i]+.00001||part.to[i]<b[i]-.00001))covers=false;
            if(covers)return true;
        }return false;
    }
    private static float distance(float[] a,float[] b){float total=0;for(int i=0;i<3;i++)total+=(b[i]-a[i])*(b[i]-a[i]);return (float)Math.sqrt(total);}
    @SubscribeEvent public static void items(RegisterClientExtensionsEvent event){
        event.registerItem(new IClientItemExtensions(){
            private BlockEntityWithoutLevelRenderer renderer;
            public BlockEntityWithoutLevelRenderer getCustomRenderer(){
                if(renderer==null)renderer=new BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels()){
                    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
                        double time=Minecraft.getInstance().level==null?0:Minecraft.getInstance().level.getGameTime()+Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
                        pose.pushPose();pose.translate(.5,.8,.5);draw(pose,buffers,true,false,stack.is(ModItems.WISDOM_SIGN.get()),time,light,overlay);pose.popPose();
                    }
                };return renderer;
            }
        },ModItems.CRYSTAL_SIGN.get(),ModItems.WISDOM_SIGN.get());
    }
    private CrystalSignClient(){}
}
