package com.mcmagic.omnira.client.hud;

import com.mcmagic.omnira.entity.WeaverSpirit;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;

@EventBusSubscriber(modid="omnira", value=Dist.CLIENT)
public final class WeaverBossBar {
    private static final ResourceLocation READY = texture("ready"), SPENT = texture("spent");
    private static final int[] LILAC = {0xffd5d1fc,0xff999cd6,0xff9594ce,0xff8986c0,0xff817ab2,0xff70679c};
    private static final int[] ROSE = {0xffffd8de,0xffea9daa,0xffd78b9f,0xffbe718a,0xffa85f7b,0xff914e6f};

    private static ResourceLocation texture(String state) {
        return ResourceLocation.fromNamespaceAndPath("omnira","textures/gui/weaver/"+state+".png");
    }

    @SubscribeEvent
    public static void render(CustomizeGuiOverlayEvent.BossEventProgress event) {
        var boss = event.getBossEvent();
        if (!(boss.getName().getContents() instanceof TranslatableContents title)
                || !WeaverSpirit.BOSS_TITLE.equals(title.getKey())) return;
        var client = Minecraft.getInstance();
        if (client.level == null) return;
        var gui = event.getGuiGraphics();
        int center = gui.guiWidth()/2, y = event.getY();
        boolean spent = boss.getColor() == BossEvent.BossBarColor.RED;
        int frame = (int)((client.level.getGameTime() / (spent ? 2 : 3)) % 18);
        float scale = Math.min(1f,(gui.guiWidth()-4)/240f);
        gui.pose().pushPose();
        gui.pose().translate(center,y,0);
        gui.pose().scale(scale,scale,1);
        gui.blit(spent ? SPENT : READY,-120,0,0,frame*52,240,52,240,936);
        int width = Mth.ceil(Mth.clamp(boss.getProgress(),0f,1f)*200);
        int[] gradient = spent ? ROSE : LILAC;
        for (int row=0;row<6;row++)
            gui.fill(-100,24+row,-100+width,25+row,gradient[row]);
        // The riveted midpoint is always visible, even on a completely empty track.
        gui.fill(-1,23,1,31,0xff33334c);
        gui.fill(-3,25,3,29,0xff33334c);
        gui.fill(-2,26,2,28,spent ? 0xffed8a9c : 0xffc4f6ff);
        if (!spent) gui.fill(-1,24,1,30,0xffe3ffff);
        gui.pose().popPose();
        event.setIncrement(Mth.ceil(52*scale)+11);
        event.setCanceled(true);
    }
}
