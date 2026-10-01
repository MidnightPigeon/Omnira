package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.spell.*;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.block.entity.VoidCrystalBlockEntity;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_construction_prison")
@PrefixGameTestTemplate(false)
public final class ConstructionPrisonGameTests {
    private static net.neoforged.neoforge.common.util.FakePlayer player(GameTestHelper h) {
        var p=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"prison-test"));
        p.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(2,2,2)));p.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_PICKAXE));return p;
    }
    @GameTest(template="spell_arena",timeoutTicks=140)
    public static void sizeStrengthExpiryAndBosses(GameTestHelper h) {
        var cow=h.spawn(EntityType.COW,5,2,5);cow.setNoAi(true);float health=cow.getHealth();
        ConstructionLock.apply(cow,SpellEffect.utility("construction",0,0,1));
        var p=cow.blockPosition();h.assertTrue(h.getLevel().getBlockEntity(p) instanceof VoidCrystalBlockEntity,"No prison");
        var first=(VoidCrystalBlockEntity)h.getLevel().getBlockEntity(p);float hardness=first.hardness();
        var saved=first.saveWithFullMetadata(h.getLevel().registryAccess());
        var restored=new VoidCrystalBlockEntity(p,first.getBlockState());restored.loadWithComponents(saved,h.getLevel().registryAccess());
        h.assertTrue(cow.getUUID().equals(restored.prisoner())&&first.seal().equals(restored.seal())&&restored.hardness()==hardness,"Prison persistence lost");
        var golem=h.spawn(EntityType.IRON_GOLEM,11,2,11);golem.setNoAi(true);ConstructionLock.apply(golem,SpellEffect.utility("construction",0,0,1));
        h.assertTrue(((VoidCrystalBlockEntity)h.getLevel().getBlockEntity(golem.blockPosition())).hardness()>hardness,"Health does not affect hardness");
        var box=golem.getBoundingBox();
        for(var pos:BlockPos.betweenClosed(BlockPos.containing(box.minX,box.minY,box.minZ),BlockPos.containing(box.maxX-.001,box.maxY-.001,box.maxZ-.001)))
            h.assertTrue(h.getLevel().getBlockEntity(pos) instanceof VoidCrystalBlockEntity be&&golem.getUUID().equals(be.prisoner()),"Large model not fully enclosed");
        var wither=EntityType.WITHER.create(h.getLevel());wither.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(15,4,15)));
        ConstructionLock.apply(wither,SpellEffect.utility("construction",0,0,1));h.assertTrue(!ConstructionLock.locked(wither),"Boss was locked");
        h.runAtTickTime(50,()->{h.assertTrue(cow.getHealth()==health,"Prison suffocates target");h.assertTrue(h.getLevel().getBlockState(p).getValue(com.mcmagic.omnira.block.VoidCrystalBlock.FADE)>0,"Crystal does not fade");});
        h.runAtTickTime(115,()->{h.assertTrue(!ConstructionLock.locked(cow)&&cow.isNoAi()&&cow.isAlive(),"Expiry changed original AI or killed target");h.assertTrue(h.getLevel().getBlockState(p).isAir(),"Prison outlived lock");h.succeed();});
    }
    @GameTest(template="spell_arena")
    public static void protectionRefreshAndManualRelease(GameTestHelper h) {
        var miner=player(h);var cow=h.spawn(EntityType.COW,5,2,5);ConstructionLock.apply(cow,SpellEffect.utility("construction",0,0,1));
        var p=cow.blockPosition();var be=(VoidCrystalBlockEntity)h.getLevel().getBlockEntity(p);var seal=be.seal();
        miner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        h.assertTrue(!miner.gameMode.destroyBlock(p)&&cow.isAlive(),"Bare hands broke prison");
        ConstructionLock.apply(cow,SpellEffect.utility("construction",0,1,1));
        h.assertTrue(seal.equals(((VoidCrystalBlockEntity)h.getLevel().getBlockEntity(p)).seal()),"Refresh made a second prison");
        ConstructionLock.release(cow);h.assertTrue(h.getLevel().getBlockState(p).isAir()&&cow.isAlive(),"Manual release killed target or left blocks");
        var blocked=h.spawn(EntityType.COW,10,2,10);var obstruction=blocked.blockPosition();
        h.getLevel().setBlockAndUpdate(obstruction,net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState());
        ConstructionLock.apply(blocked,SpellEffect.utility("construction",0,0,1));
        h.assertTrue(h.getLevel().getBlockState(obstruction).is(net.minecraft.world.level.block.Blocks.CHEST),"Prison overwrote container");
        ConstructionLock.release(blocked);h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void miningFortuneAndSingleDeath(GameTestHelper h) {
        var miner=player(h);var cow=h.spawn(EntityType.COW,5,2,5);ConstructionLock.apply(cow,SpellEffect.utility("construction",0,0,1));
        var p=cow.blockPosition();var other=p.above();
        miner.getMainHandItem().enchant(h.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE),3);
        final int[] deaths={0},looting={-1};
        java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.LivingDropsEvent> listener=e->{if(e.getEntity()==cow){deaths[0]++;looting[0]=net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(h.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING),miner.getMainHandItem());}};
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(listener);
        try {h.assertTrue(miner.gameMode.destroyBlock(p),"Mining rejected");}finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(listener);}
        h.assertTrue(!cow.isAlive()&&deaths[0]==1&&looting[0]==3,"Kill/fortune conversion failed: "+deaths[0]+","+looting[0]);
        h.assertTrue(h.getLevel().getBlockState(other).isAir(),"Remaining prison not cleared");
        h.assertTrue(net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(h.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING),miner.getMainHandItem())==0,"Converted looting leaked onto tool");
        h.assertTrue(ModBlocks.CRYSTAL_BALL.get().defaultBlockState().getLightEmission()==12,"Ball light differs");h.succeed();
    }
}
