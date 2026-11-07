package com.mcmagic.omnira.client.hud;

import com.mcmagic.omnira.network.TransitViewPayload.Style;
import com.mcmagic.omnira.world.dimension.ModDimensions;
import java.util.function.BooleanSupplier;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterDimensionTransitionScreenEvent;

@EventBusSubscriber(modid="omnira", value=Dist.CLIENT, bus=EventBusSubscriber.Bus.MOD)
public final class PortalPassageScreen extends ReceivingLevelScreen {
    private final Style style;
    private final long started = Util.getMillis();

    private PortalPassageScreen(BooleanSupplier ready, Reason reason, Style style) {
        super(ready, reason);
        this.style = style;
    }

    @SubscribeEvent public static void register(RegisterDimensionTransitionScreenEvent event) {
        var dream = ModDimensions.DREAM_REALM;
        var spacetime = ModDimensions.SPACETIME_CORRIDOR;
        event.registerIncomingEffect(dream, PortalPassageScreen::dream);
        event.registerOutgoingEffect(dream, PortalPassageScreen::dream);
        event.registerIncomingEffect(spacetime, PortalPassageScreen::interstice);
        event.registerOutgoingEffect(spacetime, PortalPassageScreen::interstice);
    }

    private static PortalPassageScreen dream(BooleanSupplier ready, Reason reason) {
        return new PortalPassageScreen(ready, reason, Style.DREAM);
    }

    private static PortalPassageScreen interstice(BooleanSupplier ready, Reason reason) {
        return new PortalPassageScreen(ready, reason, Style.INTERSTICE);
    }

    @Override public void onClose() {
        TransitView.arrived(style);
        super.onClose();
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean dream = style == Style.DREAM;
        graphics.fillGradient(0, 0, width, height, dream ? 0xFF181126 : 0xFF0B1721, dream ? 0xFF392644 : 0xFF15323B);
        double scale = minecraft.options.screenEffectScale().get();
        double elapsed = (Util.getMillis() - started) * 0.001 * scale;
        double seam = 1;
        TransitView.draw(graphics, style, 1 + elapsed * 0.25, scale, seam);
    }
}
