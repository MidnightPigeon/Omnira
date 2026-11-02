package com.mcmagic.omnira.client.renderer;

import com.mcmagic.omnira.block.entity.CrystalBallBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.*;
import com.mojang.math.Axis;
import com.mcmagic.omnira.config.OmniraConfig;

public final class CrystalBallRenderer implements BlockEntityRenderer<CrystalBallBlockEntity> {
    public static final String[] PARTS={"base","shell","mote","cracks_1","cracks_2","void_base","star_base","guidance_base","lootr_open_base","pulse_base"};
    private static final java.util.Map<String,ModelResourceLocation> MODELS=java.util.Arrays.stream(PARTS)
            .collect(java.util.stream.Collectors.toUnmodifiableMap(part->part,part->ModelResourceLocation.standalone(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("omnira","item/crystal_ball_"+part))));
    private static final float[][] UPGRADE_RIM=upgradeRim();
    public CrystalBallRenderer(BlockEntityRendererProvider.Context context) {}
    public static ModelResourceLocation model(String part) {
        var id=MODELS.get(part);return id!=null?id:CrystalGridRenderer.part("crystal_ball",part);
    }
    private static void draw(String part,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var mc=Minecraft.getInstance();
        StaticModelQuads.draw(mc.getModelManager().getModel(model(part)),pose,buffers.getBuffer(CrystalGlassLayer.ATLAS),light,overlay);
    }
    private static void base(CrystalBallBlockEntity ball,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var mc=Minecraft.getInstance();
        var model=ball.resonant()?com.mcmagic.omnira.client.StorageVisuals.base():model(com.mcmagic.omnira.compat.LootrOrbs.appearance(ball)?(ball.lootrOpened||ball.lootrPersonalView?"lootr_open_base":"guidance_base"):ball instanceof com.mcmagic.omnira.block.entity.VoidCrystalBallBlockEntity?"void_base":ball instanceof com.mcmagic.omnira.space.sea.StarClusterOrbEntity?"star_base":"base");
        int tint=(com.mcmagic.omnira.compat.LootrOrbs.appearance(ball))?(ball.lootrOpened||ball.lootrPersonalView?0xFFFFFF:0xEACB79):ball.resonant()?com.mcmagic.omnira.client.StorageVisuals.color(ball.networkStatus):ball instanceof com.mcmagic.omnira.space.sea.StarClusterOrbEntity?0x284E91:0xFFFFFF;
        var baked=mc.getModelManager().getModel(model);var out=buffers.getBuffer(CrystalGlassLayer.ATLAS);
        StaticModelQuads.drawTinted(baked,pose,out,light,overlay,tint,com.mcmagic.omnira.compat.LootrOrbs.appearance(ball));
    }
    private static void globe(PoseStack pose,MultiBufferSource buffers,double time,int light,int overlay,CrystalBallBlockEntity ball) {
        // Shared sorted glass batch; never let the shell write an opaque depth silhouette.
        MultiBufferSource contents=CrystalGlassLayer.contents(buffers);
        pose.pushPose();pose.translate(0,.125+Math.sin(time*.035)*.018,0);
        boolean liquid=ball instanceof com.mcmagic.omnira.block.entity.LiquidCrystalBallBlockEntity;
        if(liquid)LiquidOrbVisuals.render((com.mcmagic.omnira.block.entity.LiquidCrystalBallBlockEntity)ball,pose,buffers,time,light,overlay);
        if(ball instanceof com.mcmagic.omnira.block.entity.VoidCrystalBallBlockEntity)vortex(pose,buffers,time);
        boolean stellar=ball instanceof com.mcmagic.omnira.space.sea.StarClusterOrbEntity;
        if(stellar)StellarVisuals.cluster(pose,buffers,time);
        if(ball!=null && ball.hasPendingLoot() && !(com.mcmagic.omnira.compat.LootrOrbs.appearance(ball)&&ball.lootrOpened))fog(pose,buffers,time,com.mcmagic.omnira.compat.LootrOrbs.appearance(ball));
        // Independent, bounded orbits remain inside the hollow shell at every phase.
        var displayed=ball!=null&&!liquid?ball.displayItems():java.util.List.<ItemStack>of();
        for(int i=0;i<displayed.size();i++) {
            var item=displayed.get(i);
            if(item.isEmpty())continue;
            if(stellar){StellarVisuals.centeredItem(item,pose,buffers,overlay);continue;}
            double phase=time*.025+i*1.93;
            pose.pushPose();pose.translate(Math.sin(phase)*.13,Math.sin(phase*1.31+i)*.11,Math.cos(phase*.87+i)*.12);
            if(OmniraConfig.CRYSTAL_BALL_DISPLAY_MODE.get()==1)draw("mote",pose,buffers,LightTexture.FULL_BRIGHT,overlay);
            else {
                var mc=Minecraft.getInstance();
                var renderer=mc.getItemRenderer();
                var baked=renderer.getModel(item,ball.getLevel(),null,i);
                pose.translate(.5,.5,.5);
                pose.mulPose(Axis.YP.rotationDegrees((float)(time*.65+i*137.5)));
                boolean thin=!baked.isGui3d() || !baked.usesBlockLight();
                if(thin)pose.mulPose(Axis.XP.rotationDegrees(20+(float)Math.sin(phase)*15));
                boolean miniature=thin && CrystalBallItemShape.apply(baked,pose);
                if(!miniature)pose.scale(.2F,.2F,.2F);
                UnstableAggregateRenderer.inStorage(()->renderer.render(item,miniature?ItemDisplayContext.NONE:ItemDisplayContext.GROUND,false,
                        pose,contents,LightTexture.FULL_BRIGHT,overlay,baked));
            }
            pose.popPose();
        }
        String shell=ball!=null && ball.crackStage()>0?"cracks_"+ball.crackStage():"shell";
        if(com.mcmagic.omnira.compat.LootrOrbs.appearance(ball))StaticModelQuads.drawTinted(Minecraft.getInstance().getModelManager().getModel(model(shell)),pose,
                buffers.getBuffer(CrystalGlassLayer.ATLAS),light,overlay,0x9DDED5,true);
        else draw(shell,pose,buffers,light,overlay);
        pose.popPose();
    }
    private static void fog(PoseStack pose,MultiBufferSource buffers,double time,boolean lootr) {
        var mesh=buffers.getBuffer(CrystalGlassLayer.COLOR);
        for(int i=0;i<9;i++) {
            double phase=time*.012+i*2.4;
            float x=.5F+(float)Math.sin(phase)*.08F;
            float y=.5F+(float)Math.sin(phase*.83+i)*.07F;
            float z=.5F+(float)Math.cos(phase*1.13)*.08F;
            float r=.075F+.012F*(float)Math.sin(phase*.7+i);
            StellarVisuals.cube(pose,mesh,x,y,z,r,lootr?0xE8D9A3:0xABBADB,.18F);
        }
    }
    @Override public boolean shouldRender(CrystalBallBlockEntity ball,net.minecraft.world.phys.Vec3 camera){
        return BlockEntityRenderer.super.shouldRender(ball,camera)&&com.mcmagic.omnira.client.LabyrinthDisplayVisibility.visible(ball,camera,new net.minecraft.world.phys.AABB(ball.getBlockPos()).inflate(.3));
    }
    @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(CrystalBallBlockEntity ball){
        return new net.minecraft.world.phys.AABB(ball.getBlockPos()).inflate(ball.resonant()?.3:0);
    }
    public static void vortex(PoseStack pose,MultiBufferSource buffers,double time){
        vortex(pose,buffers,time,false);
    }
    public static void vortex(PoseStack pose,MultiBufferSource buffers,double time,boolean paleBlue){
        var mesh=buffers.getBuffer(CrystalGlassLayer.COLOR);var matrix=pose.last().pose();
        for(int arm=0;arm<3;arm++)for(int i=0;i<18;i++){
            double radius=.025+i*.011,angle=time*.065+i*.3+arm*Math.PI*2/3;
            float x=.5F+(float)(Math.cos(angle)*radius),z=.5F+(float)(Math.sin(angle)*radius);
            float y=.5F+(float)Math.sin(angle*2)*.025F,r=.018F+i*.0004F;
            float shade=.12F+i*.018F;
            int red=(int)((paleBlue?.36F+shade*.6F:shade*.8F)*255);
            int green=(int)((paleBlue?.62F+shade*.5F:shade*.55F)*255);
            int blue=(int)((paleBlue?.8F+shade*.3F:shade+.12F)*255);
            StellarVisuals.cube(pose,mesh,x,y,z,r,red<<16|green<<8|blue,.94F);
        }
    }
    @Override public void render(CrystalBallBlockEntity ball,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        double time=ball.getLevel()==null?0:ball.getLevel().getGameTime()+partial;
        pose.pushPose();pose.translate(.5,.5,.5);
        pose.mulPose(com.mcmagic.omnira.block.OrbMount.rotation(ball.getBlockState().getValue(com.mcmagic.omnira.block.CrystalBallBlock.MOUNT)));
        pose.translate(0,-.5,0);
        pose.mulPose(Axis.YP.rotationDegrees(180-ball.getBlockState().getValue(com.mcmagic.omnira.block.CrystalBallBlock.FACING).toYRot()));
        pose.translate(-.5,0,-.5);
        base(ball,pose,buffers,light,overlay);
        if(!com.mcmagic.omnira.compat.LootrOrbs.appearance(ball))for(int i=0;i<3;i++)upgrade(ball.upgrades.getStackInSlot(i),i,pose,buffers,light,overlay);
        globe(pose,buffers,time+(ball.getBlockPos().asLong()&255),light,overlay,ball);
        if(ball.resonant())ResonanceSatellites.render(ball.frequency,pose,buffers,time,overlay);
        if(ball instanceof com.mcmagic.omnira.space.sea.StarClusterOrbEntity)StellarVisuals.baseStars(pose,buffers,time);
        pose.popPose();
    }
    private static void upgrade(ItemStack item,int slot,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        if(item.isEmpty())return;
        var kind=com.mcmagic.omnira.block.entity.OrbUpgrades.kind(item);
        if(kind==null)return;
        int color=kind.displayColor;
        pose.pushPose();pose.translate((5+slot*3)/16.0,2.76/16.0,(slot==1?4:5)/16.0);
        var mesh=buffers.getBuffer(CrystalGlassLayer.COLOR);
        float r=(color>>16&255)/255F,g=(color>>8&255)/255F,b=(color&255)/255F;
        // Narrow luminous rim on the occupied recess; empty slots remain unlit.
        for(var v:UPGRADE_RIM)mesh.addVertex(pose.last().pose(),v[0],0,v[1]).setColor(r,g,b,.8F);
        pose.translate(0,.039,0);
        pose.mulPose(Axis.XP.rotationDegrees(45));pose.scale(.14F,.14F,.14F);
        Minecraft.getInstance().getItemRenderer().renderStatic(item,ItemDisplayContext.NONE,LightTexture.FULL_BRIGHT,overlay,pose,buffers,Minecraft.getInstance().level,slot);
        pose.popPose();
    }
    private static float[][] upgradeRim(){
        float outer=.077F,ratio=.061F/outer;
        float[][] corners={{-outer,-outer},{outer,-outer},{outer,outer},{-outer,outer}},rim=new float[16][2];
        for(int i=0;i<4;i++){
            var a=corners[i];var c=corners[(i+1)%4];
            rim[i*4]=new float[]{a[0],a[1]};rim[i*4+1]=new float[]{a[0]*ratio,a[1]*ratio};
            rim[i*4+2]=new float[]{c[0]*ratio,c[1]*ratio};rim[i*4+3]=new float[]{c[0],c[1]};
        }
        return rim;
    }
    public static final class ItemRenderer extends BlockEntityWithoutLevelRenderer {
        public ItemRenderer() {super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
        @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
            var mc=Minecraft.getInstance();
            double time=mc.level==null?0:mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false);
            if(stack.is(com.mcmagic.omnira.storage.CrystalStorageContent.ITEM.get())||stack.is(com.mcmagic.omnira.storage.CrystalStorageContent.FLUID.get()))
                mc.getItemRenderer().renderModelLists(mc.getModelManager().getModel(com.mcmagic.omnira.client.StorageVisuals.base()),stack,light,overlay,pose,buffers.getBuffer(CrystalGlassLayer.ATLAS));
            else if(stack.is(com.mcmagic.omnira.space.sea.StellarCraftContent.ORB_ITEM.get())){
                mc.getItemRenderer().renderModelLists(mc.getModelManager().getModel(model("star_base")),stack,light,overlay,pose,buffers.getBuffer(CrystalGlassLayer.ATLAS));
                StellarVisuals.baseStars(pose,buffers,time);
            }else draw(stack.is(com.mcmagic.omnira.storage.CrystalStorageContent.VOID_ITEM.get())?"void_base":"base",pose,buffers,light,overlay);
            globe(pose,buffers,time,light,overlay,null);
            if(stack.is(com.mcmagic.omnira.space.sea.StellarCraftContent.ORB_ITEM.get())){
                pose.pushPose();pose.translate(0,.125+Math.sin(time*.035)*.018,0);StellarVisuals.cluster(pose,buffers,time);
                if(mc.level!=null){
                    var data=stack.getOrDefault(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
                    var items=data.getList("Items",10);
                    for(var raw:items){var entry=(net.minecraft.nbt.CompoundTag)raw;if(entry.getInt("Slot")==0)StellarVisuals.centeredItem(ItemStack.parseOptional(mc.level.registryAccess(),entry),pose,buffers,overlay);}
                }
                pose.popPose();
            }
            if(stack.is(com.mcmagic.omnira.storage.CrystalStorageContent.VOID_ITEM.get())){
                pose.pushPose();pose.translate(0,.125+Math.sin(time*.035)*.018,0);vortex(pose,buffers,time);pose.popPose();
            }
            if(mc.level!=null) {
                var data=stack.getOrDefault(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
                var list=data.getCompound("Upgrades").getList("Items",10);
                int multiplier=1;
                for(int i=0;i<list.size();i++) {
                    var entry=list.getCompound(i);int slot=entry.getInt("Slot");
                    if(slot>=0 && slot<3) {
                        var plate=ItemStack.parseOptional(mc.level.registryAccess(),entry);upgrade(plate,slot,pose,buffers,light,overlay);
                        var kind=com.mcmagic.omnira.block.entity.OrbUpgrades.kind(plate);
                        if(kind!=null)multiplier=Math.max(multiplier,kind.capacityMultiplier);
                    }
                }
                if(stack.is(com.mcmagic.omnira.registry.ModItems.LIQUID_CRYSTAL_BALL.get())||stack.is(com.mcmagic.omnira.storage.CrystalStorageContent.FLUID.get())) {
                    var fluid=net.neoforged.neoforge.fluids.FluidStack.parseOptional(mc.level.registryAccess(),data.getCompound("Fluid").getCompound("Fluid"));
                    pose.pushPose();pose.translate(0,.125+Math.sin(time*.035)*.018,0);
                    LiquidOrbVisuals.render(fluid,24000*multiplier,pose,buffers,time,light,overlay);pose.popPose();
                }
            }
        }
    }
}
