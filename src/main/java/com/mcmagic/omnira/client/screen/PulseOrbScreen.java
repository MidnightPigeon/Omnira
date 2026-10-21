package com.mcmagic.omnira.client.screen;

import com.mcmagic.omnira.pulse.PulseOrbMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;
import com.mojang.math.Axis;

public final class PulseOrbScreen extends ResponsiveContainerScreen<PulseOrbMenu> {
    private static final ResourceLocation SHELL=ResourceLocation.parse("omnira:textures/gui/container/liquid_crystal_ball_storage.png");
    private final long openedAt=net.minecraft.Util.getMillis();
    public PulseOrbScreen(PulseOrbMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=176;imageHeight=166;}
    @Override protected int rightExtension(){return menu.orb.resonant()?31:0;}
    @Override protected boolean hasClickedOutside(double x,double y,int left,int top,int button){
        if(menu.orb.resonant()&&x>=left+178&&x<left+207&&y>=top+10&&y<top+68)return false;
        return super.hasClickedOutside(x,y,left,top,button);
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
        CrystalWorkstationBackground.inventory(g,leftPos,topPos,menu,9);
        double time=(net.minecraft.Util.getMillis()-openedAt)/50.0;
        g.blit(SHELL,leftPos+62,topPos+16,52,52,(float)PulseMeterArt.SHELL_X,(float)PulseMeterArt.SHELL_Y,PulseMeterArt.SHELL_SIZE,PulseMeterArt.SHELL_SIZE,176,244);
        for(int i=0;i<2;i++){
            g.pose().pushPose();g.pose().translate(leftPos+88,topPos+42,0);
            g.pose().mulPose(Axis.ZP.rotation((float)(time*.022*(i==0?1:-1))));
            PulseMeterArt.ring(g::fill,i==0?22:17,i==0?menu.orb.potential:menu.orb.occupied,menu.orb.capacity(),i==0?PulseMeterArt.PRESENT:PulseMeterArt.RESERVED);
            g.pose().popPose();
        }
        slot(g,80,34);if(menu.orb.resonant()){
            g.fill(leftPos+178,topPos+10,leftPos+207,topPos+68,0xE03D3659);
            for(int i=0;i<2;i++){int y=topPos+15+i*27;g.fill(leftPos+183,y,leftPos+201,y+18,0xFFB9A5DA);g.fill(leftPos+184,y+1,leftPos+200,y+17,0xFF403B53);}
        }
    }
    private void slot(GuiGraphics g,int x,int y){g.fill(leftPos+x-1,topPos+y-1,leftPos+x+17,topPos+y+17,0xFFCBE9F4);g.fill(leftPos+x,topPos+y,leftPos+x+16,topPos+y+16,0xFF294657);}
    @Override protected void renderLabels(GuiGraphics g,int mx,int my){CrystalUi.title(g,font,title,88,5,164,0xFFE4F1EC);}
    @Override protected void renderContent(GuiGraphics g,int mx,int my,float partial){
        super.renderContent(g,mx,my,partial);renderTooltip(g,mx,my);
        if(mx>=leftPos+62&&mx<leftPos+114&&my>=topPos+16&&my<topPos+69&&!(mx>=leftPos+80&&mx<leftPos+96&&my>=topPos+34&&my<topPos+50))
            g.renderTooltip(font,java.util.List.of(Component.translatable("gui.omnira.pulse.present",menu.orb.potential,menu.orb.capacity()).withColor(0x81D4FA),Component.translatable("gui.omnira.pulse.reserved",menu.orb.occupied).withColor(0xF1C76B)),java.util.Optional.empty(),mx,my);
    }
    @Override protected boolean mouseReleasedContent(double x,double y,int button){
        if(menu.orb.resonant()&&!menu.getCarried().isEmpty()&&x>=leftPos+183&&x<leftPos+201)
            for(int i=0;i<2;i++)if(y>=topPos+15+i*27&&y<topPos+33+i*27){slotClicked(menu.getSlot(1+i),1+i,button,net.minecraft.world.inventory.ClickType.PICKUP);return true;}
        return super.mouseReleasedContent(x,y,button);
    }
}
