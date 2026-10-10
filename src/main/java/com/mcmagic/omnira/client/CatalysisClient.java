package com.mcmagic.omnira.client;

import com.mcmagic.omnira.alchemy.catalysis.*;
import com.mcmagic.omnira.client.renderer.CrystalGlassLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.extensions.common.*;

@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class CatalysisClient {
    private static final com.google.gson.JsonObject GEOMETRY=geometry();
    private static com.google.gson.JsonObject geometry(){
        try(var reader=new java.io.InputStreamReader(java.util.Objects.requireNonNull(CatalysisClient.class.getResourceAsStream("/assets/omnira/geometry/catalysis_chamber.json")),java.nio.charset.StandardCharsets.UTF_8)){
            return com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
        }catch(java.io.IOException e){throw new IllegalStateException("Missing catalysis geometry",e);}
    }
    @SubscribeEvent public static void screens(RegisterMenuScreensEvent e){e.register(CatalysisContent.MENU.get(),com.mcmagic.omnira.client.screen.CatalysisScreen::new);}
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(CatalysisContent.ENTITY.get(),context->new BlockEntityRenderer<CatalysisBlockEntity>(){
        public void render(CatalysisBlockEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
            if(be.getBlockState().getValue(CatalysisBlock.PART)!=0||be.getLevel()==null)return;
            pose.pushPose();pose.translate(.5,0,.5);pose.mulPose(Axis.YP.rotationDegrees(180-be.getBlockState().getValue(CatalysisBlock.FACING).toYRot()));pose.translate(-.5,0,-.5);
            draw(be,be.getLevel().getGameTime()+partial,pose,buffers,light,overlay);pose.popPose();
        }
        @Override public AABB getRenderBoundingBox(CatalysisBlockEntity be){return new AABB(be.getBlockPos()).expandTowards(0,1.6,0);}
    });}
    public static void draw(CatalysisBlockEntity be,double time,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var mc=Minecraft.getInstance();var atlas=mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        var glow=atlas.apply(ResourceLocation.parse("omnira:block/reaction_pool_glow"));
        for(var element:GEOMETRY.getAsJsonArray("parts")){
            var part=element.getAsJsonObject();var a=part.getAsJsonArray("from");var b=part.getAsJsonArray("to");String material=part.get("material").getAsString();
            cuboid(pose,buffers,atlas.apply(ResourceLocation.parse("omnira:block/reaction_pool_"+material)),a.get(0).getAsDouble(),a.get(1).getAsDouble(),a.get(2).getAsDouble(),
                    b.get(0).getAsDouble(),b.get(1).getAsDouble(),b.get(2).getAsDouble(),(int)Long.parseLong(part.get("color").getAsString(),16),material.equals("glow")?LightTexture.FULL_BRIGHT:light,overlay,part.get("faces").getAsInt());
        }
        for(int side:new int[]{0,1}){
            double x=.22+side*.56;
            if(be!=null)item(be.upgrades.getStackInSlot(side),x,.45,.02,.15F,0,pose,buffers,light,overlay);
        }
        var plan=be==null?null:be.plan();boolean working=be!=null&&be.catalyzing();
        baseCore(time,working,pose,buffers,light,overlay,glow);
        if(be!=null){
            var fluid=be.tank.getFluid();
            if(!fluid.isEmpty()){
                var ext=IClientFluidTypeExtensions.of(fluid.getFluid());int color=ext.getTintColor(fluid);if((color>>>24)==0)color|=0xFF000000;
                var sprite=atlas.apply(ext.getStillTexture(fluid));var liquid=GEOMETRY.getAsJsonObject("liquid");double radius=liquid.get("radius").getAsDouble();
                double minimum=liquid.get("min_width").getAsDouble(),maximum=liquid.get("max_width").getAsDouble(),width=minimum+(maximum-minimum)*fluid.getAmount()/4000.;
                double outer=radius+width/2,inner=radius-width/2,low=liquid.get("bottom").getAsDouble(),high=low+width;
                cuboid(pose,buffers,sprite,.5-outer,low,.5-outer,.5+outer,high,.5-inner,color,light,overlay,63);
                cuboid(pose,buffers,sprite,.5-outer,low,.5+inner,.5+outer,high,.5+outer,color,light,overlay,63);
                cuboid(pose,buffers,sprite,.5-outer,low,.5-inner,.5-inner,high,.5+inner,color,light,overlay,60);
                cuboid(pose,buffers,sprite,.5+inner,low,.5-inner,.5+outer,high,.5+inner,color,light,overlay,60);
                for(int n=0;n<8;n++){
                    double t=(time*.008+n/8.)%1*4,a=.5-radius,b=.5+radius,x,z;
                    if(t<1){x=a+(b-a)*t;z=a;}else if(t<2){x=b;z=a+(b-a)*(t-1);}else if(t<3){x=b-(b-a)*(t-2);z=b;}else{x=a;z=b-(b-a)*(t-3);}
                    double r=Math.min(width*.18,.012);box(pose,buffers,glow,x-r,high+.001,z-r,x+r,high+.006,z+r,0xBED8F7F5,LightTexture.FULL_BRIGHT,overlay);
                }
            }
            var soil=be.items.getStackInSlot(0);if(soil.getItem() instanceof BlockItem block){
                pose.pushPose();pose.translate(.28125,1.04,.28125);pose.scale(.4375F,.18F,.4375F);
                mc.getBlockRenderer().renderSingleBlock(block.getBlock().defaultBlockState(),pose,CrystalGlassLayer.contents(buffers),light,overlay);pose.popPose();
            }
            if(CatalysisGrowthVisuals.sealed(soil)){
                CatalysisGrowthVisuals.sealedGrowth(be,time,pose,buffers,light,overlay);
            }else if(plan!=null&&plan.yield()==CatalysisGrowth.Yield.TREE){
                CatalysisGrowthVisuals.tree(plan,be.visualStage(time),pose,buffers,light,overlay);
            }else if(plan!=null&&!plan.plant().isAir()){
                boolean fruit=plan.yield()==CatalysisGrowth.Yield.FRUIT;
                float growth=(be.visualStage(time)-1F)/Math.max(1,plan.stages()-1),scale=fruit?.38F:.28F+.33F*growth;
                var visual=fruit?plan.plant().setValue(net.minecraft.world.level.block.StemBlock.AGE,be.stemStage()-1):plan.visual(be.stage());
                pose.pushPose();pose.translate((fruit?.35:.5)-scale/2,1.22,.5-scale/2);pose.scale(scale,scale,scale);
                mc.getBlockRenderer().renderSingleBlock(visual,pose,CrystalGlassLayer.contents(buffers),light,overlay);pose.popPose();
                if(fruit&&be.stemReady()){
                    float size=.12F+.20F*growth;pose.pushPose();pose.translate(.60-size/2,1.22,.5-size/2);pose.scale(size,size,size);
                    mc.getBlockRenderer().renderSingleBlock((plan.plant().is(net.minecraft.world.level.block.Blocks.MELON_STEM)?net.minecraft.world.level.block.Blocks.MELON:net.minecraft.world.level.block.Blocks.PUMPKIN).defaultBlockState(),pose,CrystalGlassLayer.contents(buffers),light,overlay);pose.popPose();
                }
            }else item(be.items.getStackInSlot(1),.5,1.43,.5,.35F,time*.35,pose,buffers,light,overlay);
            item(be.items.getStackInSlot(2),.5,2.25+Math.sin(time*.05)*.025,.5,.28F,time*.5,pose,buffers,light,overlay);
        }
        if(working)for(int i=0;i<10;i++){
            double phase=(time*.006+i*.1)%1,angle=time*.018+i*2.4;
            double x=.5+Math.cos(angle)*.32,z=.5+Math.sin(angle)*.32,y=1.15+phase*1.3;
            box(pose,buffers,glow,x-.012,y-.012,z-.012,x+.012,y+.012,z+.012,i%2==0?0xD9DBFFFF:0xC2AEF0CE,LightTexture.FULL_BRIGHT,overlay);
        }
    }
    private static void baseCore(double time,boolean working,PoseStack pose,MultiBufferSource buffers,int light,int overlay,TextureAtlasSprite glow){
        pose.pushPose();
        pose.translate(.5,.5+(working?Math.sin(time*.045)*.025:0),.5);
        pose.mulPose(Axis.YP.rotationDegrees((float)(working?time*.9%360:45)));
        pose.scale(.4375F,.4375F,.4375F);pose.translate(-.5,-.5,-.5);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(com.mcmagic.omnira.registry.ModBlocks.ROUGH_ALCHEMICAL_ENERGY_CORE.get().defaultBlockState(),
                pose,CrystalGlassLayer.contents(buffers),working?LightTexture.FULL_BRIGHT:light,overlay);
        pose.popPose();
        if(!working)return;
        int[] colors={0xDDDCC18A,0xDD8DCFF7,0xDDF4A8AD,0xDDE2FFF1};
        for(int i=0;i<8;i++){
            double angle=time*.035+i*Math.PI/4;
            double x=.5+Math.cos(angle)*.32,z=.5+Math.sin(angle)*.32,y=.5+Math.sin(angle*2)*.12;
            double radius=.012+(.5+.5*Math.sin(time*.08+i))*.004;
            box(pose,buffers,glow,x-radius,y-radius,z-radius,x+radius,y+radius,z+radius,colors[i%4],LightTexture.FULL_BRIGHT,overlay);
        }
    }
    private static void item(ItemStack stack,double x,double y,double z,float scale,double angle,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(stack.isEmpty())return;pose.pushPose();pose.translate(x,y,z);pose.mulPose(Axis.YP.rotationDegrees((float)angle));pose.scale(scale,scale,scale);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack,ItemDisplayContext.NONE,light,overlay,pose,CrystalGlassLayer.contents(buffers),Minecraft.getInstance().level,0);pose.popPose();
    }
    private static final int[][] FACES={{0,3,2,1},{4,5,6,7},{0,4,7,3},{1,2,6,5},{3,7,6,2},{0,1,5,4}};
    private static void box(PoseStack pose,MultiBufferSource buffers,TextureAtlasSprite sprite,double x,double y,double z,double xx,double yy,double zz,int color,int light,int overlay){
        cuboid(pose,buffers,sprite,x,y,z,xx,yy,zz,color,light,overlay,63);
    }
    private static void cuboid(PoseStack pose,MultiBufferSource buffers,TextureAtlasSprite sprite,double x,double y,double z,double xx,double yy,double zz,int color,int light,int overlay,int mask){
        float[][] v={{(float)x,(float)y,(float)z},{(float)xx,(float)y,(float)z},{(float)xx,(float)yy,(float)z},{(float)x,(float)yy,(float)z},
                {(float)x,(float)y,(float)zz},{(float)xx,(float)y,(float)zz},{(float)xx,(float)yy,(float)zz},{(float)x,(float)yy,(float)zz}};
        for(int face=0;face<6;face++){if((mask&(1<<face))==0)continue;var f=FACES[face];var a=v[f[0]];var b=v[f[1]];var d=v[f[3]];var normal=new org.joml.Vector3f(b[0]-a[0],b[1]-a[1],b[2]-a[2]).cross(new org.joml.Vector3f(d[0]-a[0],d[1]-a[1],d[2]-a[2])).normalize();
            var out=buffers.getBuffer(CrystalGlassLayer.ATLAS);for(int i=0;i<4;i++)out.addVertex(pose.last(),v[f[i]][0],v[f[i]][1],v[f[i]][2]).setColor(color)
                    .setUv(sprite.getU(i>=2?1:0),sprite.getV(i==1||i==2?1:0)).setOverlay(overlay).setLight(light).setNormal(pose.last(),normal.x,normal.y,normal.z);}
    }
    @SubscribeEvent public static void items(RegisterClientExtensionsEvent e){e.registerItem(new IClientItemExtensions(){private BlockEntityWithoutLevelRenderer renderer;
        public BlockEntityWithoutLevelRenderer getCustomRenderer(){if(renderer==null)renderer=new BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels()){
            @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
                pose.pushPose();pose.translate(.5,.01,.5);pose.scale(.38F,.38F,.38F);pose.translate(-.5,0,-.5);draw(null,0,pose,buffers,light,overlay);pose.popPose();
            }};return renderer;}
    },CatalysisContent.ITEM.get());}
    private CatalysisClient(){}
}
