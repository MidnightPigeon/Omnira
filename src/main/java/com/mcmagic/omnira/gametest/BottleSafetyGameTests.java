package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.item.bottle.*;
import com.mcmagic.omnira.registry.ModItems;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_bottle_safety")
@PrefixGameTestTemplate(false)
public final class BottleSafetyGameTests {
    @GameTest(template="spell_arena")
    public static void rejectsHostilesAndAngerButAllowsCalmNeutral(GameTestHelper h) {
        var player=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"anger-test"));
        var wolf=EntityType.WOLF.create(h.getLevel());wolf.setRemainingPersistentAngerTime(100);
        var targeted=EntityType.IRON_GOLEM.create(h.getLevel());targeted.setTarget(player);
        var remembered=EntityType.BEE.create(h.getLevel());remembered.setPersistentAngerTarget(player.getUUID());
        for(var target:java.util.List.of(EntityType.ZOMBIE.create(h.getLevel()),EntityType.SLIME.create(h.getLevel()),wolf,targeted,remembered)) {
            var bottle=new ItemStack(ModItems.POCKET_MAGIC_BOTTLE.get());
            h.assertTrue(!PocketBottleItem.capture(player,target,bottle) && !target.isRemoved()
                    && !PocketBottleItem.filled(bottle),"Hostile/angry target accepted");
        }
        var pig=EntityType.PIG.create(h.getLevel());
        var zombie=EntityType.ZOMBIE.create(h.getLevel());zombie.startRiding(pig,true);
        h.assertTrue(!PocketBottleItem.capture(player,pig,new ItemStack(ModItems.POCKET_MAGIC_BOTTLE.get()))
                && !pig.isRemoved() && !zombie.isRemoved(),"Hostile passenger accepted");
        var calm=EntityType.WOLF.create(h.getLevel());
        var bottle=new ItemStack(ModItems.POCKET_MAGIC_BOTTLE.get());
        h.assertTrue(PocketBottleItem.capture(player,calm,bottle),"Calm neutral rejected");
        BottleStorage.get(h.getLevel()).remove(bottle.get(com.mcmagic.omnira.registry.ModDataComponents.BOTTLE_CAPTURE));
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void rejectsPartsBossesAndHelpers(GameTestHelper h) {
        var player=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"bottle-safety"));
        var dragon=EntityType.ENDER_DRAGON.create(h.getLevel());
        var targets=new java.util.ArrayList<Entity>(java.util.List.of(dragon,dragon.head,
                EntityType.WITHER.create(h.getLevel()),EntityType.ARMOR_STAND.create(h.getLevel()),
                EntityType.ITEM_DISPLAY.create(h.getLevel()),EntityType.INTERACTION.create(h.getLevel()),
                EntityType.AREA_EFFECT_CLOUD.create(h.getLevel()),EntityType.ARROW.create(h.getLevel()),
                EntityType.TNT.create(h.getLevel()),EntityType.BOAT.create(h.getLevel())));
        for(var target:targets) {
            var bottle=new ItemStack(ModItems.POCKET_MAGIC_BOTTLE.get());
            h.assertTrue(!PocketBottleItem.capture(player,target,bottle),"Unsupported capture: "+target.getType());
            h.assertTrue(!target.isRemoved() && !PocketBottleItem.filled(bottle) && bottle.getCount()==1,"Rejected capture mutated target or bottle");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void rejectsUnsupportedPassengerAtomically(GameTestHelper h) {
        var player=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"bottle-passenger"));
        var pig=EntityType.PIG.create(h.getLevel());
        var stand=EntityType.ARMOR_STAND.create(h.getLevel());stand.startRiding(pig,true);
        var bottle=new ItemStack(ModItems.POCKET_MAGIC_BOTTLE.get());
        h.assertTrue(!PocketBottleItem.capture(player,pig,bottle) && !pig.isRemoved() && !stand.isRemoved()
                && stand.getVehicle()==pig && !PocketBottleItem.filled(bottle),"Passenger guard failed");
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void ordinaryCreatureCopiesRemainSupported(GameTestHelper h) {
        BottleCopyGameTests.creativeEntityCopies(h);
    }
}
