package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.compat.SableBottleCompat;
import com.mcmagic.omnira.item.bottle.*;
import com.mcmagic.omnira.registry.*;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import java.nio.file.Path;
import java.util.*;

@GameTestHolder("omnira_bottle_copy")
@PrefixGameTestTemplate(false)
public final class BottleCopyGameTests {
    @GameTest(template="spell_arena")
    public static void creativeEntityCopies(GameTestHelper h) {
        var level=h.getLevel();
        var player=new net.neoforged.neoforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"copy-test"));
        player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);player.setPos(h.absoluteVec(new Vec3(2,4,2)));
        var mob=net.minecraft.world.entity.EntityType.PIG.create(level);mob.setPos(h.absoluteVec(new Vec3(4,4,4)));level.addFreshEntity(mob);
        var bottle=new net.minecraft.world.item.ItemStack(ModItems.POCKET_MAGIC_BOTTLE.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,bottle);
        h.assertTrue(PocketBottleItem.capture(player,mob,bottle),"Entity capture failed");
        var id=bottle.get(ModDataComponents.BOTTLE_CAPTURE);
        for(int i=0;i<2;i++) {
            player.getCooldowns().removeCooldown(ModItems.POCKET_MAGIC_BOTTLE.get());
            player.gameMode.useItem(player,level,bottle,net.minecraft.world.InteractionHand.MAIN_HAND);
            h.assertTrue(player.getMainHandItem().get(ModDataComponents.BOTTLE_CAPTURE).equals(id),"Creative template consumed");
            var thrown=level.getEntitiesOfClass(ThrownPocketBottle.class,player.getBoundingBox().inflate(3)).stream().filter(e->e.getOwner()==player).findFirst().orElseThrow();
            h.assertTrue(PocketBottleItem.release(level,thrown.getItem(),h.absoluteVec(new Vec3(8+i*5,4,8)),thrown.getUUID()),"Entity copy release failed");
            thrown.discard();
        }
        h.assertTrue(BottleStorage.get(level).owns(id,null),"Template was locked or deleted");
        var pigs=level.getEntitiesOfClass(net.minecraft.world.entity.animal.Pig.class,new net.minecraft.world.phys.AABB(h.absoluteVec(new Vec3(6,2,6)),h.absoluteVec(new Vec3(16,8,12))));
        h.assertTrue(pigs.size()==2 && !pigs.get(0).getUUID().equals(pigs.get(1).getUUID()),"Copies are not independent");
        pigs.forEach(net.minecraft.world.entity.Entity::discard);BottleStorage.get(level).remove(id);h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=200)
    public static void dreamStructureCopies(GameTestHelper h) throws Exception {
        if(!net.neoforged.fml.ModList.get().isLoaded("sable")){h.succeed();return;}
        SableChecks.run(h);
    }
    // Keep optional types out of the class scanned by GameTest discovery.
    private static final class SableChecks {
    private static void run(GameTestHelper h) throws Exception {
        String fixture=System.getenv("OMNIRA_BOTTLE_FIXTURE");
        h.assertTrue(fixture!=null,"Set OMNIRA_BOTTLE_FIXTURE to a read-only bottle save");
        var root=NbtIo.readCompressed(Path.of(fixture),NbtAccounter.unlimitedHeap());
        var data=root.getCompound("data").getList("Captures",Tag.TAG_COMPOUND).stream()
                .map(value->((CompoundTag)value).getCompound("Data"))
                .filter(value->!value.getList("Ships",Tag.TAG_COMPOUND).isEmpty())
                .findFirst().orElseThrow(()->new IllegalArgumentException("Fixture contains no captured structure"));
        var original=data.copy();var container=SubLevelContainer.getContainer(h.getLevel());
        var before=new HashSet<>(container.getAllSubLevels());
        h.runAfterDelay(5,()->{
            try {
                for(int i=0;i<2;i++) h.assertTrue(SableBottleCompat.release(h.getLevel(),data,h.absoluteVec(new Vec3(5+i*10,10,10))),"Structure copy "+i+" failed");
                var added=container.getAllSubLevels().stream().filter(s->!before.contains(s)).toList();
                h.assertTrue(added.size()==2,"Expected two independent structures");
                h.assertTrue(!added.get(0).getUniqueId().equals(added.get(1).getUniqueId()),"Structure UUID reused");
                h.assertTrue(!added.get(0).getPlot().plotPos.equals(added.get(1).getPlot().plotPos),"Structure plot reused");
                for(var ship:added) h.assertTrue(ship.getPlot().getBoundingBox().volume()>0,"Restored structure is empty");
                h.assertTrue(data.equals(original),"Creative template changed");h.succeed();
            } finally {
                for(var ship:new ArrayList<>(container.getAllSubLevels())) if(!before.contains(ship)) container.removeSubLevel(ship,SubLevelRemovalReason.REMOVED);
            }
        });
    }
    }
}
