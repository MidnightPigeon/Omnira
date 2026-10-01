package com.mcmagic.omnira.client;

import net.minecraft.client.Minecraft;

public final class TravelerManuscriptClient {
    private TravelerManuscriptClient() {}
    public static void open() {Minecraft.getInstance().setScreen(new com.mcmagic.omnira.client.screen.TravelerManuscriptScreen());}
}
