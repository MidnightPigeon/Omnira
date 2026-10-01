package com.mcmagic.omnira.mana;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class DreamText {
    public static final String MARKER="{dream}";
    private DreamText() {}
    public static MutableComponent colored(String text) {
        var result=Component.empty();int index=0;
        for(int point:text.codePoints().toArray()) {
            int color=(index++%2==0)?0x87A9F5:0xB68AE3;
            result.append(Component.literal(new String(Character.toChars(point))).withStyle(style->style.withColor(color)));
        }
        return result;
    }
}
