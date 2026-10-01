package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.item.bottle.*;
import com.mcmagic.omnira.registry.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_bottle_clear")
@PrefixGameTestTemplate(false)
public final class BottleClearGameTests {
    @GameTest(template="spell_arena")
    public static void clearBothKindsAndProtectFlyingCapture(GameTestHelper h) {
        var level=h.getLevel();
        var player=new net.neoforged.neoforge.common.util.FakePlayer(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"bottle-clear"));
        player.setShiftKeyDown(true);
        var storage=BottleStorage.get(level);
        for(String kind:java.util.List.of("entity","structure")) {
            var data=new CompoundTag();data.putString("Kind",kind);
            var id=storage.put(data);
            var bottle=new ItemStack(ModItems.POCKET_MAGIC_BOTTLE.get());
            bottle.set(ModDataComponents.BOTTLE_CAPTURE,id);
            bottle.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("filled"));
            player.setItemInHand(InteractionHand.MAIN_HAND,bottle);
            player.getCooldowns().addCooldown(bottle.getItem(),10);
            bottle.getItem().use(level,player,InteractionHand.MAIN_HAND);
            h.assertTrue(!PocketBottleItem.filled(bottle) && bottle.getCount()==1,"Empty bottle not retained");
            h.assertTrue(storage.get(id)==null,"Capture data leaked");
            h.assertTrue(!bottle.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME),"Filled name retained");
            bottle.getItem().use(level,player,InteractionHand.MAIN_HAND);
            h.assertTrue(bottle.getCount()==1,"Empty bottle changed");
        }
        var id=storage.put(new CompoundTag());
        storage.claim(id,java.util.UUID.randomUUID());
        var duplicate=new ItemStack(ModItems.POCKET_MAGIC_BOTTLE.get());duplicate.set(ModDataComponents.BOTTLE_CAPTURE,id);
        player.setItemInHand(InteractionHand.MAIN_HAND,duplicate);
        duplicate.getItem().use(level,player,InteractionHand.MAIN_HAND);
        h.assertTrue(storage.get(id)!=null,"Flying capture deleted via duplicate");
        storage.remove(id);
        h.succeed();
    }
}
