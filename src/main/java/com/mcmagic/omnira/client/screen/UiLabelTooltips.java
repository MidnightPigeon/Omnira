package com.mcmagic.omnira.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/** Retain full labels without shrinking text below a readable size. Bounds are in screen pixels. */
@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class UiLabelTooltips {
    private record Label(float x,float y,float right,float bottom,Component text) {}
    private static final java.util.List<Label> LABELS=new java.util.ArrayList<>();
    private static boolean collecting;
    private UiLabelTooltips() {}
    static void record(GuiGraphics g,Component text,int x,int y,int width,int height){
        if(!collecting)return;
        var matrix=g.pose().last().pose();
        var a=matrix.transformPosition(new org.joml.Vector3f(x,y,0));
        var b=matrix.transformPosition(new org.joml.Vector3f(x+width,y+height,0));
        LABELS.add(new Label(a.x,a.y,b.x,b.y,text));
    }
    @SubscribeEvent public static void before(ScreenEvent.Render.Pre event){LABELS.clear();collecting=true;}
    @SubscribeEvent public static void after(ScreenEvent.Render.Post event){
        collecting=false;var g=event.getGuiGraphics();
        for(var label:LABELS)if(event.getMouseX()>=label.x&&event.getMouseX()<label.right&&event.getMouseY()>=label.y&&event.getMouseY()<label.bottom){
            g.pose().pushPose();g.pose().translate(0,0,600);
            g.renderTooltip(Minecraft.getInstance().font,label.text,event.getMouseX(),event.getMouseY());g.pose().popPose();break;
        }
        LABELS.clear();
    }
}
