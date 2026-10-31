package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.fate.*;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_suitcase") @PrefixGameTestTemplate(false)
public final class SuitcaseGameTests {
    @GameTest(template="spell_arena") public static void musicBuffsAndInterruptedDrawBag(GameTestHelper h){
        var be=placed(h);var center=be.getBlockPos().getCenter();
        var inside=net.minecraft.world.entity.EntityType.COW.create(h.getLevel());inside.setPos(center.add(10,0,0));h.getLevel().addFreshEntity(inside);
        var outside=net.minecraft.world.entity.EntityType.COW.create(h.getLevel());outside.setPos(center.add(8,0,8));h.getLevel().addFreshEntity(outside);
        try{
            var seen=new java.util.HashSet<Integer>();
            for(int i=0;i<com.mcmagic.omnira.music.ViolinTracks.size();i++){
                be.insert(new ItemStack(ModItems.VOYAGER_VIOLIN.get()),false);be.tickPerformance();
                h.assertTrue(seen.add(be.track()),"Interrupted/restarted case repeated a spent recording");
                h.assertTrue(inside.getEffect(net.minecraft.world.effect.MobEffects.REGENERATION).getAmplifier()==1,"Missing musical regeneration II");
                h.assertTrue(inside.getEffect(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE).getAmplifier()==1,"Missing musical resistance II");
                h.assertTrue(!outside.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION),"Music aura leaked outside sphere");
                be.removeDisplay();inside.removeAllEffects();be.tickPerformance();
                h.assertTrue(!inside.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION)&&be.track()==-1,"Stopped music still granted effects");
            }
            be.insert(new ItemStack(ModItems.VOYAGER_VIOLIN.get()),false);be.tickPerformance();
            h.assertTrue(be.track()>=0,"Exhausted draw bag failed to refill");
        }finally{inside.discard();outside.discard();}h.succeed();
    }
    @GameTest(template="spell_arena") public static void musicBagSurvivesReloadAndCompletesTracks(GameTestHelper h){
        var be=placed(h);be.insert(new ItemStack(ModItems.VOYAGER_VIOLIN.get()),false);be.tickPerformance();int first=be.track();
        for(int i=0;i<com.mcmagic.omnira.music.ViolinTracks.ticks(first);i++)be.tickPerformance();
        h.assertTrue(be.track()!=first&&be.performance()==2,"Natural completion failed to draw next recording");
        var tag=be.saveWithFullMetadata(h.getLevel().registryAccess());
        var copy=new SuitcaseEntity(be.getBlockPos(),be.getBlockState());copy.setLevel(h.getLevel());copy.loadWithComponents(tag,h.getLevel().registryAccess());copy.tickPerformance();
        h.assertTrue(copy.track()!=first&&copy.track()!=be.track(),"Reload replayed a spent recording");h.succeed();
    }
    private static SuitcaseEntity placed(GameTestHelper h){
        var p=h.absolutePos(new BlockPos(4,2,4));h.getLevel().setBlockAndUpdate(p.below(),Blocks.STONE.defaultBlockState());
        h.getLevel().setBlockAndUpdate(p,ModBlocks.RAIN_SOAKED_SUITCASE.get().defaultBlockState());return (SuitcaseEntity)h.getLevel().getBlockEntity(p);
    }
    @GameTest(template="spell_arena") public static void sneakingPlacesWithoutAwakening(GameTestHelper h){
        var l=h.getLevel();var p=new net.neoforged.neoforge.common.util.FakePlayer(l,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"case"));
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.setShiftKeyDown(true);p.setPos(h.absoluteVec(new Vec3(6,2,6)));
        var floor=h.absolutePos(new BlockPos(4,1,4));l.setBlockAndUpdate(floor,Blocks.STONE.defaultBlockState());
        var stack=new ItemStack(ModItems.RAIN_SOAKED_SUITCASE.get());p.setItemInHand(InteractionHand.MAIN_HAND,stack);
        var hit=new BlockHitResult(floor.getCenter().add(0,.5,0),Direction.UP,floor,false);
        var result=stack.useOn(new net.minecraft.world.item.context.UseOnContext(p,InteractionHand.MAIN_HAND,hit));
        h.assertTrue(result.consumesAction()&&stack.isEmpty()&&l.getBlockEntity(floor.above()) instanceof SuitcaseEntity,"Sneak placement failed or did not consume one box");
        h.assertTrue(ManuscriptReward.equipped(p,"talent")==null,"Placement awakened talent");
        h.assertTrue(ModItems.RAIN_SOAKED_SUITCASE.get().use(l,p,InteractionHand.MAIN_HAND).getResult()==InteractionResult.PASS,"Sneak-air activated suitcase");h.succeed();
    }
    @GameTest(template="spell_arena") public static void oneItemExchangeAndPersistence(GameTestHelper h){
        var be=placed(h);var p=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);var source=new ItemStack(Items.DIAMOND,8);
        source.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("keepsake"));
        p.setItemInHand(InteractionHand.MAIN_HAND,source);var hit=new BlockHitResult(be.getBlockPos().getCenter(),Direction.UP,be.getBlockPos(),false);
        be.getBlockState().useItemOn(source,h.getLevel(),p,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(source.getCount()==7&&be.displayed().getCount()==1,"Single-item transfer lost/duplicated items");
        be.getBlockState().useItemOn(source,h.getLevel(),p,InteractionHand.MAIN_HAND,hit);h.assertTrue(source.getCount()==7,"Occupied case consumed a second item");
        var copy=new SuitcaseEntity(be.getBlockPos(),be.getBlockState());copy.loadWithComponents(be.saveWithFullMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(ItemStack.isSameItemSameComponents(copy.displayed(),be.displayed()),"Saved display lost components");
        p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);be.getBlockState().useWithoutItem(h.getLevel(),p,hit);
        h.assertTrue(be.displayed().isEmpty()&&p.getInventory().countItem(Items.DIAMOND)==1,"Empty hand did not retrieve display");h.succeed();
    }
    @GameTest(template="spell_arena") public static void violinAndBreakingReturnExactlyOnce(GameTestHelper h){
        var be=placed(h);var violin=new ItemStack(ModItems.VOYAGER_VIOLIN.get());h.assertTrue(be.insert(violin,false)&&violin.isEmpty()&&be.musical(),"Violin did not enable music");
        var area=new AABB(be.getBlockPos()).inflate(2);h.getLevel().destroyBlock(be.getBlockPos(),true);
        var drops=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,area);
        h.assertTrue(drops.stream().filter(e->e.getItem().is(ModItems.VOYAGER_VIOLIN.get())).mapToInt(e->e.getItem().getCount()).sum()==1,"Broken case lost/duplicated violin");
        h.assertTrue(drops.stream().filter(e->e.getItem().is(ModItems.RAIN_SOAKED_SUITCASE.get())).mapToInt(e->e.getItem().getCount()).sum()==1,"Broken case did not return one suitcase");
        h.assertTrue(be.displayed().isEmpty()&&!be.musical(),"Removed block retained musical content");h.succeed();
    }
    @GameTest(template="spell_arena") public static void supportAndFourDirections(GameTestHelper h){
        var be=placed(h);var l=h.getLevel();var pos=be.getBlockPos();
        for(var dir:Direction.Plane.HORIZONTAL){var state=be.getBlockState().setValue(SuitcaseBlock.FACING,dir);h.assertTrue(state.canSurvive(l,pos)&&!state.getShape(l,pos).isEmpty(),"Invalid horizontal placement");}
        be.insert(new ItemStack(Items.EMERALD),false);l.destroyBlock(pos.below(),false);
        h.assertTrue(l.isEmptyBlock(pos),"Unsupported suitcase stayed floating");
        var drops=l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(pos).inflate(2));
        h.assertTrue(drops.stream().filter(e->e.getItem().is(Items.EMERALD)).mapToInt(e->e.getItem().getCount()).sum()==1,"Support removal lost display");h.succeed();
    }
}
