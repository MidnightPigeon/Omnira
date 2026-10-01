package com.mcmagic.omnira.client.screen;

import com.mcmagic.omnira.block.entity.ManaEngineAccess;
import com.mcmagic.omnira.block.entity.ArcaneAssemblyTableBlockEntity;
import com.mcmagic.omnira.client.renderer.AssemblyRenderer;
import com.mcmagic.omnira.energy.EnergyIntegration;
import com.mcmagic.omnira.registry.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** A client-only model stage. It never places blocks or runs a kinetic network. */
public final class EngineGuideScreen extends Screen {
    private final Screen parent;
    private int stage,ticks;
    private boolean electrical,paused;
    private BlockEntity preview;
    private ArcaneAssemblyTableBlockEntity assembly;
    public EngineGuideScreen(Screen parent) {super(Component.translatable("gui.omnira.engine.guide"));this.parent=parent;}
    @Override protected void init() {
        super.init();
        preview=ModBlockEntityTypes.MANA_ENGINE.get().create(BlockPos.ZERO,ModBlocks.MANA_ENGINE.get().defaultBlockState());
        if(preview!=null) preview.setLevel(minecraft.level);
        assembly=new ArcaneAssemblyTableBlockEntity(BlockPos.ZERO,ModBlocks.ARCANE_ASSEMBLY_TABLE.get().defaultBlockState());
        assembly.setLevel(minecraft.level);
        refreshPreview();
        int center=width/2;
        addRenderableWidget(Button.builder(Component.translatable("gui.omnira.engine.mechanical"),b->electrical=false)
                .bounds(center-(EnergyIntegration.available()?94:45),30,90,20).build());
        if(EnergyIntegration.available()) addRenderableWidget(Button.builder(Component.translatable("gui.omnira.engine.electric"),b->electrical=true)
                .bounds(center+4,30,90,20).build());
        addRenderableWidget(Button.builder(Component.literal("<"),b->change(-1)).bounds(center-50,height-27,22,20).build());
        addRenderableWidget(Button.builder(Component.literal("||"),b->{paused=!paused;b.setMessage(Component.literal(paused?">":"||"));})
                .bounds(center-24,height-27,22,20).build());
        addRenderableWidget(Button.builder(Component.literal(">"),b->change(1)).bounds(center+2,height-27,22,20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"),b->onClose()).bounds(width-68,height-27,58,20).build());
    }
    private void change(int delta) {stage=Math.floorMod(stage+delta,EngineGuideTimeline.STAGES);ticks=0;refreshPreview();}
    private void refreshPreview() {
        if(!(preview instanceof ManaEngineAccess access)) return;
        var state=access.engineState();
        state.enabled=true;state.redstoneControl=false;
        state.inventory.setItem(0,new ItemStack(ModItems.TEST_SPELL_CORE.get()));
        for(int i=1;i<4;i++) state.inventory.setItem(i,i<=EngineGuideTimeline.grids(stage,ticks)?new ItemStack(ModItems.ELEMENTAL_CRYSTAL_GRID.get()):ItemStack.EMPTY);
        boolean complete=ticks>=EngineGuideTimeline.COMPLETE;
        for(int i=0;i<6;i++)assembly.setItem(i,complete?ItemStack.EMPTY:new ItemStack(net.minecraft.world.item.Items.DIAMOND));
        assembly.setItem(6,complete?new ItemStack(ModItems.WIDE_AREA_RUNE.get()):new ItemStack(Blocks.AMETHYST_BLOCK));
    }
    @Override public void tick() {
        if(!paused && ++ticks>=EngineGuideTimeline.duration(stage)) change(1);
        refreshPreview();
        if(preview instanceof ManaEngineAccess access) {
            var state=access.engineState();state.previousRotorAngle=state.rotorAngle;
            if(!paused && EngineGuideTimeline.powered(stage,ticks)) state.rotorAngle+=4.8F;
        }
    }
    private void block(BlockState state,int x,int y,int z,PoseStack pose,net.minecraft.client.renderer.MultiBufferSource.BufferSource buffers) {
        pose.pushPose();pose.translate(x,y,z);
        minecraft.getBlockRenderer().renderSingleBlock(state,pose,buffers,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
    // This screen paints its own background; Screen.render must not blur the finished content.
    @Override public void renderBackground(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) {}

    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) {
        graphics.fill(0,0,width,height,0xF01C2228);
        graphics.drawCenteredString(font,title,width/2,12,0xFFF2F3EF);
        int sceneBottom=height-112;
        float size=Math.max(12,Math.min(42,(sceneBottom-55)/2.6F));
        double time=ticks+(paused?0:partialTick);
        graphics.flush();RenderSystem.enableDepthTest();
        var pose=graphics.pose();pose.pushPose();
        pose.translate(width/2.0,(55+sceneBottom)/2.0+size*.35,150);
        pose.scale(size,-size,size);pose.mulPose(Axis.XP.rotationDegrees(25));pose.mulPose(Axis.YP.rotationDegrees(EngineGuideTimeline.yaw(stage,time)));
        pose.translate(-.5,0,.5);
        var buffers=minecraft.renderBuffers().bufferSource();
        for(int x=-1;x<=1;x++) for(int z=-2;z<=0;z++) block(Blocks.SMOOTH_STONE.defaultBlockState(),x,-1,z,pose,buffers);
        pose.pushPose();pose.translate(0,0,EngineGuideTimeline.engineZ(stage,time));
        block(ModBlocks.MANA_ENGINE.get().defaultBlockState(),0,0,0,pose,buffers);
        if(preview!=null) minecraft.getBlockEntityRenderDispatcher().renderItem(preview,pose,buffers,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
        pose.popPose();
        if(stage==4) {
            pose.pushPose();pose.translate(0,0,EngineGuideTimeline.tableZ(time));
            block(assembly.getBlockState(),0,0,0,pose,buffers);
            var renderer=minecraft.getBlockEntityRenderDispatcher().getRenderer((BlockEntity)assembly);
            if(renderer instanceof AssemblyRenderer model)model.renderPreview(assembly,pose,buffers,
                    LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,time,
                    preview instanceof ManaEngineAccess access?access.engineState().rotorAngle:0,EngineGuideTimeline.strikeAge(time));
            pose.popPose();
        }
        if(stage==2) block(Blocks.LEVER.defaultBlockState().setValue(BlockStateProperties.POWERED,EngineGuideTimeline.powered(stage,time)),-1,0,0,pose,buffers);
        buffers.endBatch();pose.popPose();RenderSystem.disableDepthTest();
        String mode=electrical?"electric":"mechanical";
        int capacity=(int)(512*Math.pow(1.5,EngineGuideTimeline.grids(stage,ticks)));
        graphics.drawCenteredString(font,Component.literal(capacity+(electrical?" FE/s":" su | 16 rpm")),width/2,sceneBottom+5,0xFFE4F5F5);
        graphics.drawCenteredString(font,Component.translatable("gui.omnira.engine.guide.stage",stage+1),width/2,sceneBottom+18,0xFFADCBD2);
        var description=Component.translatable("gui.omnira.engine.guide."+mode+"."+stage);
        int y=sceneBottom+33;
        int column=Math.min(width-28,390);
        float textScale=1;
        var lines=font.split(description,column);
        while(lines.size()*10*textScale>height-34-y && textScale>.55F) {
            textScale-=.05F;lines=font.split(description,(int)(column/textScale));
        }
        pose.pushPose();pose.translate((width-column)/2F,y,0);pose.scale(textScale,textScale,1);
        for(int i=0;i<lines.size();i++) graphics.drawString(font,lines.get(i),0,i*10,0xFFE0E1DC,false);
        pose.popPose();
        super.render(graphics,mouseX,mouseY,partialTick);
    }
    @Override public boolean isPauseScreen() {return false;}
    @Override public void onClose() {minecraft.setScreen(parent);}
}
