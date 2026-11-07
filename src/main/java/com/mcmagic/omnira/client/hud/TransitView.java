package com.mcmagic.omnira.client.hud;

import com.mcmagic.omnira.network.TransitViewPayload;
import com.mcmagic.omnira.network.TransitViewPayload.Style;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(modid="omnira", value=Dist.CLIENT)
public final class TransitView {
    private static final int DURATION = 32;
    private static Style style;
    private static int age;

    private static int charge;
    private static boolean preparing;
    private static float releaseStrength;
    private static double releasePosition;
    private static boolean cancelled;

    public static void start(Style arrival, int ticks) {
        if (ticks < 0) {
            if (style == arrival && preparing) {
                release((float)strength(progress(0)));
                cancelled = true;
            }
            return;
        }
        cancelled = false;
        if (ticks > 0) {
            style = arrival;
            charge = ticks;
            preparing = true;
            age = 0;
            return;
        }
        if (arrival == Style.INTERSTICE && Minecraft.getInstance().screen instanceof ReceivingLevelScreen) {
            style = arrival;
            charge = TransitViewPayload.PORTAL_TICKS;
            preparing = true;
            age = 0;
            return;
        }
        // A portal arrival releases the veil already on screen. Other travel starts a fresh pulse.
        if (style == arrival && preparing) {
            release(1);
            return;
        }
        releasePosition = 0;
        releaseStrength = 1;
        preparing = false;
        style = arrival;
        age = 0;
    }

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (style == null) return;
        var mc = Minecraft.getInstance();
        if (mc.player != null && !mc.player.isAlive()) { style=null; return; }
        if (mc.level == null || mc.isPaused() || mc.screen instanceof ReceivingLevelScreen) return;
        age++;
        if (preparing && ((style != Style.INTERSTICE && age > TransitViewPayload.SYNC_INTERVAL + 2)
                || (style == Style.INTERSTICE && charge < TransitViewPayload.PORTAL_TICKS && age > 60))) release((float)strength(progress(0)));
        else if (!preparing && age >= DURATION) style = null;
    }

    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { style = null; }

    static void arrived(Style arrival) {
        style = arrival;
        preparing = true;
        charge = TransitViewPayload.PORTAL_TICKS;
        age = 0;
        cancelled = false;
        release(1);
    }

    private static void release(float intensity) {
        releasePosition = progress(0);
        releaseStrength = intensity;
        preparing = false;
        age = 0;
    }

    private static double progress(double partial) {
        return Math.min(1, preparing ? (charge + age + partial) / TransitViewPayload.PORTAL_TICKS
                : (age + partial) / DURATION);
    }

    private static double strength(double t) {
        if (preparing) return 0.14 + 0.86 * Math.sin(t * Math.PI * 0.5);
        if (style == Style.TIME || style == Style.SPACE) {
            double fade = Math.clamp((t - 0.125) / 0.875, 0, 1);
            return 1 - fade * fade * (3 - 2 * fade);
        }
        double fade = 1 - Math.max(0, Math.min(1, t));
        return releaseStrength * fade * (0.8 + 0.2 * fade);
    }

    @SubscribeEvent public static void lens(ViewportEvent.ComputeFov event) {
        if (style == null || !event.usedConfiguredFov()) return;
        var options = Minecraft.getInstance().options;
        double shift = switch (style) { case DREAM -> -0.18; case INTERSTICE -> 0.22; case TIME -> -0.20; case SPACE -> 0.30; };
        double amount = strength(progress(event.getPartialTick())) * options.fovEffectScale().get() * options.screenEffectScale().get();
        event.setFOV(event.getFOV() * (1 + shift * amount));
    }

    public static void render(GuiGraphics g, DeltaTracker tracker) {
        if (style == null) return;
        double t = progress(tracker.getGameTimeDeltaPartialTick(false));
        double opacity = strength(t) * Minecraft.getInstance().options.screenEffectScale().get();
        if (opacity < 0.01) return;
        double seam = preparing ? t : releasePosition * (cancelled ? 1 : 1 - t);
        if (style == Style.TIME || style == Style.SPACE) {
            // Cover the position update, then uncover the destination without opening a loading screen.
            double uncover = Math.clamp((t - 0.125) / 0.625, 0, 1);
            double cover = 1 - uncover * uncover * (3 - 2 * uncover);
            int backdrop = style == Style.TIME ? 0x142B23 : 0x17192F;
            double scale = Minecraft.getInstance().options.screenEffectScale().get();
            g.fill(0, 0, g.guiWidth(), g.guiHeight(), color(backdrop, 255 * cover * scale));
        }
        draw(g, style, t, opacity, seam);
    }

    static void draw(GuiGraphics g, Style style, double t, double opacity, double seam) {
        int w = g.guiWidth(), h = g.guiHeight();
        int tint = switch (style) { case DREAM -> 0xC39AE3; case INTERSTICE -> 0x87DAD8; case TIME -> 0xB1E791; case SPACE -> 0xADA2FF; };
        int veil = switch (style) { case DREAM -> 0x794FAD; case INTERSTICE -> 0x316B80; case TIME -> 0x54883F; case SPACE -> 0x51469E; };
        g.fill(0, 0, w, h, color(veil, opacity * 149));
        int edge = color(tint, opacity * 168);
        g.fillGradient(0, 0, w, h / 3, edge, tint);
        g.fillGradient(0, h * 2 / 3, w, h, tint, edge);

        int ink = color(tint, opacity * 255);
        switch (style) {
            case DREAM -> {
                // A mirrored wave of pixel glass sweeps inward from the edges.
                for (int y = 0; y < h; y += 5) {
                    double wave = Math.sin(6.0 * y / h - 5 * t);
                    int span = (int)(w * opacity * (0.19 + 0.06 * wave));
                    int glass = color(0xB68DE2, opacity * (63 + 24 * wave));
                    g.fill(0, y, span, y + 5, glass);
                    g.fill(w - span, y, w, y + 5, glass);
                }
                // Mirrored motes drift upward on either side of the gateway.
                for (int i = 0; i < 12; i++) {
                    int x = (int)(w * (0.06 + (i * 37 % 100) / 320.0 + 0.015 * Math.sin(i + t * 4)));
                    double phase = i * 0.381966 - t * 0.22;
                    int y = (int)(h * (phase - Math.floor(phase)));
                    star(g, x, y, ink);
                    star(g, w - x, y, ink);
                }
            }
            case INTERSTICE -> {
                // Two stepped mirror edges part to reveal the arrival point.
                for (int y = 0; y < h; y += 6) {

                    double gap = (1 - seam) * (w * 0.5 + Math.abs(y - h / 2) * 0.22);
                    int x = (int)(w * 0.5 - gap);
                    g.fill(0, y, Math.max(0, x), y + 6, color(0x15323B, opacity * 255));
                    g.fill(Math.min(w, w - x), y, w, y + 6, color(0x15323B, opacity * 255));
                    g.fill(x - 9, y, x, y + 6, color(tint, opacity * 75));
                    g.fill(x, y, x + 3, y + 6, ink);
                    g.fill(w - x, y, w - x + 9, y + 6, color(tint, opacity * 75));
                    g.fill(w - x - 3, y, w - x, y + 6, ink);
                }
            }
            case TIME, SPACE -> {
                boolean clock = style == Style.TIME;
                int marks = clock ? 24 : 32;
                for (int i = 0; i < marks; i++) {
                    double angle = 2 * Math.PI * i / marks - (clock ? t * 0.55 : 0);
                    double radius = clock ? h * (0.46 - 0.10 * t) : h * (0.12 + (i * 17 % 31) / 48.0 + t * t);
                    float length = clock ? (i % 6 == 0 ? 18 : 8) : (float)(12 + 42 * t);
                    var pose = g.pose();
                    pose.pushPose();
                    pose.translate(w * 0.5 + Math.cos(angle) * radius, h * 0.5 + Math.sin(angle) * radius, 0);
                    pose.mulPose(com.mojang.math.Axis.ZP.rotation((float)angle));
                    g.fill(0, 0, (int)length, clock && i % 6 == 0 ? 4 : 2, ink);
                    pose.popPose();
                }
            }
        }
    }

    private static void star(GuiGraphics g, int x, int y, int color) {
        g.fill(x - 2, y, x + 3, y + 1, color);
        g.fill(x, y - 2, x + 1, y + 3, color);
    }

    private static int color(int rgb, double alpha) { return (int)alpha << 24 | rgb; }
}
