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
    public static final String[] PARTS={"base","shell","mote","cracks_1","cracks_2"};
    public CrystalBallRenderer(BlockEntityRendererProvider.Context context) {}
    public static ModelResourceLocation model(String part) {return CrystalGridRenderer.part("crystal_ball",part);}
    private static void draw(String part,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var mc=Minecraft.getInstance();
        mc.getItemRenderer().renderModelLists(mc.getModelManager().getModel(model(part)),ItemStack.EMPTY,light,overlay,
                pose,buffers.getBuffer(CrystalGlassLayer.ATLAS));
    }
    private static void globe(PoseStack pose,MultiBufferSource buffers,double time,int light,int overlay,CrystalBallBlockEntity ball) {
        // Shared sorted glass batch; never let the shell write an opaque depth silhouette.
        MultiBufferSource contents=type->buffers.getBuffer(type==Sheets.translucentItemSheet()
                ?CrystalGlassLayer.ATLAS:type);
        pose.pushPose();pose.translate(0,.125+Math.sin(time*.035)*.018,0);
        boolean liquid=ball instanceof com.mcmagic.omnira.block.entity.LiquidCrystalBallBlockEntity;
        if(liquid)LiquidOrbVisuals.render((com.mcmagic.omnira.block.entity.LiquidCrystalBallBlockEntity)ball,pose,buffers,time,light,overlay);
        if(ball!=null && ball.hasPendingLoot())fog(pose,buffers,time);
        // Independent, bounded orbits remain inside the hollow shell at every phase.
        for(int i=0;ball!=null && !liquid && i<CrystalBallBlockEntity.SIZE;i++) {
            var item=ball.displayItem(i);
            if(item.isEmpty())continue;
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
        draw(ball!=null && ball.crackStage()>0?"cracks_"+ball.crackStage():"shell",pose,buffers,light,overlay);
        pose.popPose();
    }
    private static void fog(PoseStack pose,MultiBufferSource buffers,double time) {
        var mesh=buffers.getBuffer(CrystalGlassLayer.COLOR);
        var matrix=pose.last().pose();
        for(int i=0;i<9;i++) {
            double phase=time*.012+i*2.4;
            float x=.5F+(float)Math.sin(phase)*.08F;
            float y=.5F+(float)Math.sin(phase*.83+i)*.07F;
            float z=.5F+(float)Math.cos(phase*1.13)*.08F;
            float r=.075F+.012F*(float)Math.sin(phase*.7+i);
            float[][] v={{x-r,y-r,z-r},{x+r,y-r,z-r},{x+r,y+r,z-r},{x-r,y+r,z-r},
                    {x-r,y-r,z+r},{x+r,y-r,z+r},{x+r,y+r,z+r},{x-r,y+r,z+r}};
            int[][] faces={{0,3,2,1},{4,5,6,7},{0,4,7,3},{1,2,6,5},{3,7,6,2},{0,1,5,4}};
            for(var face:faces)for(int n:face)mesh.addVertex(matrix,v[n][0],v[n][1],v[n][2])
                    .setColor(.67F,.73F,.86F,.18F);
        }
    }
    @Override public void render(CrystalBallBlockEntity ball,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        double time=ball.getLevel()==null?0:ball.getLevel().getGameTime()+partial;
        pose.pushPose();pose.translate(.5,0,.5);
        pose.mulPose(Axis.YP.rotationDegrees(180-ball.getBlockState().getValue(com.mcmagic.omnira.block.CrystalBallBlock.FACING).toYRot()));
        pose.translate(-.5,0,-.5);
        for(int i=0;i<3;i++)upgrade(ball.upgrades.getStackInSlot(i),i,pose,buffers,light,overlay);
        pose.popPose();
        globe(pose,buffers,time+(ball.getBlockPos().asLong()&255),light,overlay,ball);
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
        float outer=.077F,inner=.061F;
        float[][] corners={{-outer,-outer},{outer,-outer},{outer,outer},{-outer,outer}};
        for(int i=0;i<4;i++) {
            var a=corners[i];var c=corners[(i+1)%4];float ratio=inner/outer;
            float[][] quad={{a[0],a[1]},{a[0]*ratio,a[1]*ratio},{c[0]*ratio,c[1]*ratio},{c[0],c[1]}};
            for(var v:quad)mesh.addVertex(pose.last().pose(),v[0],0,v[1]).setColor(r,g,b,.8F);
        }
        pose.translate(0,.039,0);
        pose.mulPose(Axis.XP.rotationDegrees(45));pose.scale(.14F,.14F,.14F);
        Minecraft.getInstance().getItemRenderer().renderStatic(item,ItemDisplayContext.NONE,LightTexture.FULL_BRIGHT,overlay,pose,buffers,Minecraft.getInstance().level,slot);
        pose.popPose();
    }
    public static final class ItemRenderer extends BlockEntityWithoutLevelRenderer {
        public ItemRenderer() {super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
        @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
            var mc=Minecraft.getInstance();
            double time=mc.level==null?0:mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false);
            draw("base",pose,buffers,light,overlay);globe(pose,buffers,time,light,overlay,null);
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
                if(stack.is(com.mcmagic.omnira.registry.ModItems.LIQUID_CRYSTAL_BALL.get())) {
                    var fluid=net.neoforged.neoforge.fluids.FluidStack.parseOptional(mc.level.registryAccess(),data.getCompound("Fluid").getCompound("Fluid"));
                    pose.pushPose();pose.translate(0,.125+Math.sin(time*.035)*.018,0);
                    LiquidOrbVisuals.render(fluid,24000*multiplier,pose,buffers,time,light,overlay);pose.popPose();
                }
            }
        }
    }
}
