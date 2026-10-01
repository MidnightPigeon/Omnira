package com.mcmagic.omnira.client.hud;

import com.mcmagic.omnira.mana.Affinity;
import com.mcmagic.omnira.mana.ManaState;
import com.mcmagic.omnira.registry.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class ManaBarTooltips {
    private record Area(float left,float top,float right,float bottom,Affinity affinity) {
        boolean contains(double x,double y){return x>=left && x<=right && y>=top && y<=bottom;}
    }
    private static final java.util.List<Area> AREAS=new java.util.ArrayList<>();
    private static boolean collecting;
    private ManaBarTooltips() {}
    public static void record(GuiGraphics g,ManaState mana,int x,int y,int width) {
        if(!collecting)return;
        var transform=g.pose().last().pose();
        var a=transform.transformPosition(new org.joml.Vector3f(x,y-1,0));
        var b=transform.transformPosition(new org.joml.Vector3f(x+width,y+14,0));
        AREAS.add(new Area(Math.min(a.x,b.x),Math.min(a.y,b.y),Math.max(a.x,b.x),Math.max(a.y,b.y),Affinity.byId(mana.affinity())));
    }
    @SubscribeEvent public static void before(ScreenEvent.Render.Pre event){AREAS.clear();collecting=true;}
    @SubscribeEvent public static void after(ScreenEvent.Render.Post event) {
        var mc=Minecraft.getInstance();var g=event.getGuiGraphics();
        if(mc.player!=null && event.getScreen() instanceof AbstractContainerScreen<?> && !mc.player.isSpectator()) {
            // Keep the inventory itself unchanged; redraw the HUD above the screen background.
            int x=Math.max(2,g.guiWidth()/2-202),y=g.guiHeight()-18;
            ManaBar.render(g,mc.font,mc.player.getData(ModAttachments.MANA),x,y,81);
        }
        collecting=false;
        for(int i=AREAS.size()-1;i>=0;i--) {
            var area=AREAS.get(i);
            if(!area.contains(event.getMouseX(),event.getMouseY()))continue;
            var text=new java.util.ArrayList<Component>();
            text.add(Component.translatable(area.affinity.key()).withStyle(style->style.withColor(area.affinity.color)));
            int lines=switch(area.affinity){case NONE -> 1;case DREAM -> 4;default -> 2;};
            for(int line=1;line<=lines;line++)text.add(Component.translatable("tooltip.omnira.affinity."+area.affinity.name().toLowerCase(java.util.Locale.ROOT)+"."+line));
            g.pose().pushPose();g.pose().translate(0,0,500);
            g.renderComponentTooltip(mc.font,text,event.getMouseX(),event.getMouseY());g.pose().popPose();break;
        }
        AREAS.clear();
    }
}
