package com.mcmagic.omnira.gametest;
import com.mcmagic.omnira.spell.*;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.block.entity.VoidCrystalBlockEntity;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;
import java.util.UUID;

@GameTestHolder("omnira_utility")
@PrefixGameTestTemplate(false)
public final class UtilityGameTests {
    private static FakePlayer player(GameTestHelper h) {
        var player=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"utility-test"));
        player.setPos(h.absoluteVec(new Vec3(3.5,2,3.5)));return player;
    }
    @GameTest(template="spell_arena")
    public static void shearAndDisarmDoNotDuplicateEquipment(GameTestHelper h) {
        var player=player(h);var sheep=h.spawn(EntityType.SHEEP,5,2,5);
        sheep.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_AXE));
        sheep.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.SHIELD));
        float health=sheep.getHealth();
        UtilitySpellEffects.applyLiving(SpellEffect.utility("dissociation",1,1,1),player,player,sheep,1);
        h.assertTrue(sheep.isSheared(),"Sheep did not use shearing state");
        h.assertTrue(sheep.getMainHandItem().isEmpty() && sheep.getOffhandItem().isEmpty(),"Infusion did not strip two pieces");
        h.assertTrue(sheep.getHealth()==health,"Delay did not suppress damage");
        var drops=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,sheep.getBoundingBox().inflate(2));
        long axes=drops.stream().filter(e->e.getItem().is(Items.IRON_AXE)).count();
        h.assertTrue(axes==1,"Equipped axe duplicated");
        long wool=drops.stream().filter(e->e.getItem().is(net.minecraft.tags.ItemTags.WOOL)).mapToInt(e->e.getItem().getCount()).sum();
        UtilitySpellEffects.applyLiving(SpellEffect.utility("dissociation",1,1,1),player,player,sheep,1);
        long after=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,sheep.getBoundingBox().inflate(2))
                .stream().filter(e->e.getItem().is(net.minecraft.tags.ItemTags.WOOL)).mapToInt(e->e.getItem().getCount()).sum();
        h.assertTrue(wool>0 && after==wool,"Sheared sheep produced wool again");h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void blockHarvestHonorsTier(GameTestHelper h) {
        var player=player(h);var pos=h.absolutePos(new BlockPos(6,2,6));
        h.getLevel().setBlockAndUpdate(pos,Blocks.OBSIDIAN.defaultBlockState());
        UtilitySpellEffects.applyBlock(SpellEffect.utility("dissociation",0,0,0),player,pos,Direction.UP,pos.above());
        h.assertTrue(h.getLevel().getBlockState(pos).is(Blocks.OBSIDIAN),"Iron tier broke obsidian");
        UtilitySpellEffects.applyBlock(SpellEffect.utility("dissociation",1,0,1),player,pos,Direction.UP,pos.above());
        h.assertTrue(h.getLevel().getBlockState(pos).isAir(),"Enhanced tier did not break obsidian");h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=80)
    public static void crystalExpiryAndPermanence(GameTestHelper h) {
        var player=player(h);var pos=h.absolutePos(new BlockPos(6,2,6));
        UtilitySpellEffects.applyBlock(SpellEffect.utility("construction",1,1,0),player,pos.below(),Direction.UP,pos);
        h.assertTrue(h.getLevel().getBlockState(pos).is(ModBlocks.REINFORCED_VOID_CRYSTAL.get()),"Construction failed");
        var crystal=(VoidCrystalBlockEntity)h.getLevel().getBlockEntity(pos);
        var saved=crystal.saveWithFullMetadata(h.getLevel().registryAccess());
        h.assertTrue(saved.getInt("Lifetime")==800,"Delay did not double crystal duration");
        crystal.configure(20,false);
        h.runAtTickTime(2,()->h.getLevel().getBlockTicks().clearArea(new net.minecraft.world.level.levelgen.structure.BoundingBox(pos)));
        h.runAtTickTime(12,()->h.assertTrue(h.getLevel().getBlockState(pos).getValue(com.mcmagic.omnira.block.VoidCrystalBlock.FADE)>0,"Lost scheduled tick stopped fade"));
        var permanent=pos.offset(2,0,0);
        UtilitySpellEffects.applyBlock(SpellEffect.utility("construction",0,0,1),player,permanent.below(),Direction.UP,permanent);
        h.runAtTickTime(35,()->{
            h.assertTrue(h.getLevel().getBlockState(pos).isAir(),"Expired crystal remained");
            h.assertTrue(h.getLevel().getBlockState(permanent).is(ModBlocks.PERMANENT_VOID_CRYSTAL.get()),"Permanent crystal expired");
            h.assertTrue(h.getLevel().getBlockEntity(permanent)==null,"Permanent crystal retained block entity");
            h.succeed();
        });
    }
    @GameTest(template="spell_arena",timeoutTicks=150)
    public static void lockExpiresAndAllowsContinuousMining(GameTestHelper h) {
        var player=player(h);
        var cow=h.spawn(EntityType.COW,3,2,5);
        var sheep=h.spawn(EntityType.SHEEP,8,2,8);
        sheep.setNoAi(true);
        ConstructionLock.apply(cow,SpellEffect.utility("construction",0,0,1));
        ConstructionLock.apply(sheep,SpellEffect.utility("construction",1,0,0));
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_PICKAXE));
        player.setOnGround(true);
        h.runAtTickTime(5,()->{
            player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            h.assertTrue(player.gameMode.destroyBlock(cow.blockPosition()),"Prison block could not be mined");
        });
        h.runAtTickTime(110,()->{
            h.assertTrue(!cow.isAlive(),"Continuous mining did not kill infused target");
            h.assertTrue(sheep.getHealth()<=6,"Repeated suffocation did not apply");
            h.assertTrue(!ConstructionLock.locked(sheep) && sheep.isNoAi(),"Lock failed to expire or changed original NoAI");
            h.succeed();
        });
    }
}
