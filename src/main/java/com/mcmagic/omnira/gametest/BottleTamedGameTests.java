package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.item.bottle.*;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_bottle_tamed")
@PrefixGameTestTemplate(false)
public final class BottleTamedGameTests {
    private static net.neoforged.neoforge.common.util.FakePlayer player(GameTestHelper h) {
        return new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"bottle-tamed"));
    }
    private static Vec3 landing(GameTestHelper h) { return Vec3.atCenterOf(h.absolutePos(new BlockPos(5,4,5))); }
    private static int empties(GameTestHelper h) {
        return h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(h.absolutePos(new BlockPos(5,4,5))).inflate(5))
                .stream().filter(e->e.getItem().is(ModItems.POCKET_MAGIC_BOTTLE.get())&&!PocketBottleItem.filled(e.getItem()))
                .mapToInt(e->e.getItem().getCount()).sum();
    }
    @GameTest(template="spell_arena") public static void anyOwnerAndSpeciesReturnOneBottle(GameTestHelper h) {
        var capturer=player(h);var owner=player(h);
        for(var type:java.util.List.of(EntityType.WOLF,EntityType.HORSE,ModEntityTypes.VELOCIRAPTOR.get(),ModEntityTypes.ARCHAEOPTERYX_SPIRIT.get())) {
            Entity entity=type.create(h.getLevel());entity.setPos(landing(h));
            if(entity instanceof TamableAnimal animal)animal.tame(owner);
            else ((net.minecraft.world.entity.animal.horse.AbstractHorse)entity).tameWithName(owner);
            var uuid=entity.getUUID();var bottle=new ItemStack(ModItems.POCKET_MAGIC_BOTTLE.get());int before=empties(h);
            h.assertTrue(PocketBottleItem.capture(capturer,entity,bottle),"Other owner's tame creature cannot be captured");
            h.assertTrue(PocketBottleItem.release(h.getLevel(),bottle,landing(h)),"Tamed release failed");
            h.assertTrue(empties(h)==before+1,"Release did not return exactly one empty bottle");
            var restored=h.getLevel().getEntity(uuid);
            h.assertTrue(restored instanceof OwnableEntity own && owner.getUUID().equals(own.getOwnerUUID()),"Owner lost on release");
            h.assertTrue(!PocketBottleItem.release(h.getLevel(),bottle,landing(h))&&empties(h)==before+1,"Repeat release duplicated empty bottle");
            restored.discard();
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void wildCreatureStillConsumesBottle(GameTestHelper h) {
        var creature=EntityType.WOLF.create(h.getLevel());creature.setPos(landing(h));
        var bottle=new ItemStack(ModItems.POCKET_MAGIC_BOTTLE.get());int before=empties(h);
        h.assertTrue(PocketBottleItem.capture(player(h),creature,bottle)&&PocketBottleItem.release(h.getLevel(),bottle,landing(h)),"Wild release failed");
        h.assertTrue(empties(h)==before,"Wild creature returned an empty bottle");h.succeed();
    }
    @GameTest(template="spell_arena") public static void creativeCopyDoesNotDuplicateEmptyBottle(GameTestHelper h) {
        var creature=EntityType.WOLF.create(h.getLevel());creature.tame(player(h));creature.setPos(landing(h));
        var bottle=new ItemStack(ModItems.POCKET_MAGIC_BOTTLE.get());
        h.assertTrue(PocketBottleItem.capture(player(h),creature,bottle),"Capture failed");
        var storage=BottleStorage.get(h.getLevel());var data=storage.get(bottle.get(ModDataComponents.BOTTLE_CAPTURE));
        data.putBoolean("CreativeCopy",true);var copy=bottle.copy();copy.set(ModDataComponents.BOTTLE_CAPTURE,storage.put(data));
        int before=empties(h);
        h.assertTrue(PocketBottleItem.release(h.getLevel(),copy,landing(h)),"Creative copy failed to release");
        h.assertTrue(empties(h)==before&&storage.get(bottle.get(ModDataComponents.BOTTLE_CAPTURE))!=null,"Creative copy duplicated a bottle or consumed original");h.succeed();
    }
}
