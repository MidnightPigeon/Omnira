package com.mcmagic.omnira.client.screen;

import com.mcmagic.omnira.menu.ArcaneAssemblyTableMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class ArcaneAssemblyTableScreen extends AbstractContainerScreen<ArcaneAssemblyTableMenu> {
    private static final ResourceLocation BACKGROUND=CrystalWorkstationBackground.texture("workstation_assembly");
    private static final ResourceLocation HAMMER=ResourceLocation.fromNamespaceAndPath("omnira","textures/gui/assembly_hammer.png");
    private Button hammer;
    private int swing,lastProgress,cooldown;
    public ArcaneAssemblyTableScreen(ArcaneAssemblyTableMenu menu,Inventory inventory,Component title) {
        super(menu,inventory,title);imageWidth=176;imageHeight=240;inventoryLabelY=146;
    }
    @Override protected void init() {
        super.init();lastProgress=menu.progress();
        hammer=addRenderableWidget(new Button(leftPos+138,topPos+116,26,26,Component.translatable("button.omnira.assembly.forge"),
                button->{if(minecraft!=null && minecraft.gameMode!=null && cooldown==0 && menu.canStrike()) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0);swing=6;cooldown=com.mcmagic.omnira.block.entity.ArcaneAssemblyTableBlockEntity.STRIKE_COOLDOWN;button.active=false;
                }},supplier->supplier.get()) {
            @Override protected void renderWidget(GuiGraphics graphics,int x,int y,float partialTick) {
                graphics.pose().pushPose();
                graphics.pose().translate(getX()+13,getY()+13,0);
                graphics.pose().mulPose(Axis.ZP.rotationDegrees(swing>0?-35F*(float)Math.sin((6-swing+partialTick)*Math.PI/6):isHoveredOrFocused()?-8:0));
                graphics.pose().scale(1.5F,1.5F,1);
                RenderSystem.enableBlend();
                graphics.setColor(1,1,1,active?1:.4F);
                graphics.blit(HAMMER,-8,-8,0,0,16,16,16,16);
                graphics.setColor(1,1,1,1);
                graphics.pose().popPose();
                int remaining=Math.max(cooldown,menu.cooldown());
                if(remaining>0) {
                    graphics.fill(getX()+3,getY()+24,getX()+23,getY()+26,0xFF565D67);
                    graphics.fill(getX()+3,getY()+24,getX()+3+20*(10-Math.min(10,remaining))/10,getY()+26,0xFFBDE1ED);
                }
            }
        });
        hammer.setTooltip(Tooltip.create(Component.translatable("button.omnira.assembly.forge")));
        hammer.active=menu.canStrike();
    }
    @Override protected void containerTick() {
        super.containerTick();
        if(swing>0) swing--;
        if(cooldown>0) cooldown--;
        if(menu.progress()!=lastProgress) {lastProgress=menu.progress();swing=6;}
        hammer.active=cooldown==0 && menu.canStrike();
    }
    @Override protected void renderBg(GuiGraphics graphics,float partialTick,int mouseX,int mouseY) {
        CrystalWorkstationBackground.draw(graphics,BACKGROUND,leftPos,topPos);
    }
    @Override protected void renderLabels(GuiGraphics graphics,int mouseX,int mouseY) {
        CrystalWorkstationBackground.labels(graphics,font,title,playerInventoryTitle);
        WorkstationManaStatus.render(graphics,font,minecraft.player,menu.manaCost());
        graphics.fill(61,132,130,138,0xFF555A63);
        graphics.fill(62,133,62+(int)(67*menu.progress()/100F),137,0xFFDCF1F3);
        String text=menu.progress()+"%";
        graphics.pose().pushPose();graphics.pose().translate(96,125,0);graphics.pose().scale(.75F,.75F,1);
        graphics.drawString(font,text,-font.width(text)/2,0,0xFF404040,false);graphics.pose().popPose();
    }
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) {
        super.render(graphics,mouseX,mouseY,partialTick);renderTooltip(graphics,mouseX,mouseY);
    }
}
