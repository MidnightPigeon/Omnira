package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.guidance.*;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.item.staff.StaffAssembly;
import com.mcmagic.omnira.menu.CrystalProcessingTableMenu;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("omnira_understanding")
@PrefixGameTestTemplate(false)
public final class UnderstandingGameTests {
    private static int count(List<ItemStack> stacks,Item item){return stacks.stream().filter(s->s.is(item)).mapToInt(ItemStack::getCount).sum();}
    private static net.neoforged.neoforge.common.util.FakePlayer player(GameTestHelper h){
        return new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"unravel"));
    }
    @GameTest(template="spell_arena") public static void allSixStagesSurviveDropsSerializationAndActualReplacement(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(4,3,4));var p=player(h);
        level.setBlockAndUpdate(pos.below(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        p.setPos(pos.getCenter().add(0,0,3));
        for(int stage=0;stage<=5;stage++)for(var front:Direction.Plane.HORIZONTAL){
            var block=stage==5?GuidanceContent.UNDERSTANDING.get():GuidanceContent.BLOCK.get();
            level.setBlockAndUpdate(pos,block.defaultBlockState().setValue(GuidanceOrbBlock.STAGE,stage).setValue(GuidanceOrbBlock.FACING,front));
            var drops=Block.getDrops(level.getBlockState(pos),level,pos,level.getBlockEntity(pos));
            h.assertTrue(drops.size()==1,"Stage did not drop exactly one orb: "+stage);
            var item=ItemStack.parseOptional(level.registryAccess(),(net.minecraft.nbt.CompoundTag)drops.getFirst().save(level.registryAccess()));
            level.removeBlock(pos,false);p.setItemInHand(InteractionHand.MAIN_HAND,item);
            var hit=new BlockHitResult(pos.below().getCenter().add(0,.5,0),Direction.UP,pos.below(),false);
            h.assertTrue(((BlockItem)item.getItem()).useOn(new net.minecraft.world.item.context.UseOnContext(p,InteractionHand.MAIN_HAND,hit)).consumesAction(),"Re-placement failed");
            h.assertTrue(level.getBlockState(pos).is(block)&&((GuidanceOrbEntity)level.getBlockEntity(pos)).stage()==stage,"Re-placement reset stage "+stage);
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void mainHandDismantlesAlongsideRefillButOffhandIsUntouched(GameTestHelper h){
        var p=player(h);var staff=StaffAssembly.basic().create();var offhand=new ItemStack(ModItems.WORLD_ROTATION_CRYSTAL.get());
        p.setItemInHand(InteractionHand.MAIN_HAND,staff);p.setItemInHand(InteractionHand.OFF_HAND,offhand);
        p.getAttribute(ModAttributes.MAX_MANA).setBaseValue(200);
        p.setData(ModAttachments.MANA,new com.mcmagic.omnira.mana.ManaState(0,100));
        UniversalUnderstanding.use(p);
        h.assertTrue(staff.isEmpty()&&p.getData(ModAttachments.MANA).current()==200,"Staff did not dismantle and refill to the actual maximum");
        h.assertTrue(offhand.getCount()==1,"Offhand spell was dismantled");
        p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setData(ModAttachments.MANA,new com.mcmagic.omnira.mana.ManaState(0,200));
        UniversalUnderstanding.use(p);
        h.assertTrue(offhand.getCount()==1&&p.getData(ModAttachments.MANA).current()==200,"Empty-main-hand use changed the offhand or did not refill");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void codexTransformsOnlyFinalStageWithoutConsumptionAndDropsItsNewForm(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(4,3,4));
        var codex=new ItemStack(com.mcmagic.omnira.codex.MyriadCodexContent.ITEM.get());
        for(var facing:Direction.Plane.HORIZONTAL){
            level.setBlockAndUpdate(pos,GuidanceContent.BLOCK.get().defaultBlockState().setValue(GuidanceOrbBlock.STAGE,4).setValue(GuidanceOrbBlock.FACING,facing));
            ((GuidanceOrbEntity)level.getBlockEntity(pos)).interact(codex,facing);
            h.assertTrue(level.getBlockState(pos).is(GuidanceContent.BLOCK.get()),"Codex advanced without entering requirements");
            ((GuidanceOrbEntity)level.getBlockEntity(pos)).interact(codex,Direction.UP);
            h.assertTrue(level.getBlockState(pos).is(GuidanceContent.BLOCK.get()),"Opening requirements also advanced");
            ((GuidanceOrbEntity)level.getBlockEntity(pos)).interact(codex,facing);
            var state=level.getBlockState(pos);var orb=(GuidanceOrbEntity)level.getBlockEntity(pos);
            h.assertTrue(state.is(GuidanceContent.UNDERSTANDING.get())&&state.getValue(GuidanceOrbBlock.FACING)==facing,"Transformation lost block identity or facing");
            h.assertTrue(codex.getCount()==1&&orb.stage()==5&&!orb.buttonsVisible()&&orb.projections().length==0,"Mastered orb retained navigation or consumed codex");
            h.assertTrue(state.getLightEmission(level,pos)==15,"Mastered orb is not luminous");
            var drops=Block.getDrops(state,level,pos,orb);
            h.assertTrue(drops.size()==1&&drops.getFirst().is(GuidanceContent.UNDERSTANDING_ITEM.get()),"Mastered form lost when mined");
            h.assertTrue(drops.getFirst().getRarity()==Rarity.EPIC,"Understanding must be purple rarity");
        }
        for(int stage=0;stage<4;stage++)h.assertTrue(!GuidanceOrbEntity.advances(stage,codex),"Codex skipped progression");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void realBlockUseRefillsAndDismantlesInsteadOfCasting(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(4,3,4));
        level.setBlockAndUpdate(pos,GuidanceContent.UNDERSTANDING.get().defaultBlockState());
        var p=player(h);p.setPos(pos.getCenter());p.setData(ModAttachments.MANA,new com.mcmagic.omnira.mana.ManaState(0,100));
        var hit=new BlockHitResult(pos.getCenter(),Direction.NORTH,pos,false);
        p.gameMode.useItemOn(p,level,ItemStack.EMPTY,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(p.getData(ModAttachments.MANA).current()==100,"Empty hand did not refill mana");
        p.setData(ModAttachments.MANA,new com.mcmagic.omnira.mana.ManaState(170,100));
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));UniversalUnderstanding.use(p);
        h.assertTrue(p.getData(ModAttachments.MANA).current()==170,"Refill removed overflow mana");
        p.setData(ModAttachments.MANA,new com.mcmagic.omnira.mana.ManaState(0,100));
        var crystal=new ItemStack(ModItems.WORLD_ROTATION_CRYSTAL.get());p.setItemInHand(InteractionHand.MAIN_HAND,crystal);
        p.gameMode.useItemOn(p,level,crystal,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(crystal.isEmpty()&&p.getInventory().countItem(ModItems.OMNI_MICROCORE.get())==1
                &&p.getInventory().countItem(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get())==1,"World Rotation cast instead of dismantling or returned wrong substrate");
        h.assertTrue(p.getInventory().countItem(ModItems.SPELL_INK.get())==0&&p.getData(ModAttachments.MANA).current()==100,"Dismantling did not also refill mana or restored ink");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void inverseWritingDeduplicatesElementsButRetainsModifierCounts(GameTestHelper h){
        var input=new SimpleContainer(9);
        input.setItem(0,new ItemStack(ModItems.EARTH_MICROCORE.get()));input.setItem(1,new ItemStack(ModItems.AIR_MICROCORE.get()));
        input.setItem(2,new ItemStack(ModItems.AMPLIFICATION_STAR.get()));input.setItem(3,new ItemStack(ModItems.AMPLIFICATION_STAR.get()));
        input.setItem(4,new ItemStack(ModItems.SHARP_BREATH.get()));input.setItem(5,new ItemStack(ModItems.SHARP_BREATH.get()));
        var crystal=CrystalProcessingTableMenu.previewCrystal(input);var before=crystal.copy();
        var out=UniversalUnderstanding.materials(crystal);
        h.assertTrue(count(out,ModItems.AMPLIFICATION_STAR.get())==2&&count(out,ModItems.SHARP_BREATH.get())==1,"Merged modifiers or refunded redundant element");
        h.assertTrue(count(out,ModItems.SPIRITUAL_CRYSTAL.get())==1&&out.stream().mapToInt(ItemStack::getCount).sum()==6,"Wrong substrate or microcores");
        h.assertTrue(ItemStack.matches(before,crystal)&&crystal.getComponents().equals(before.getComponents()),"Dismantle preview changed crystal data");
        var high=new SimpleContainer(12);
        high.setItem(0,input.getItem(0));high.setItem(1,input.getItem(1));
        high.setItem(2,new ItemStack(ModItems.SHARP_BREATH.get()));high.setItem(3,new ItemStack(ModItems.HEALING_DEW.get()));high.setItem(4,high.getItem(2).copy());
        high.setItem(5,new ItemStack(ModItems.LINGERING_THREAD.get()));high.setItem(6,new ItemStack(ModItems.LINGERING_THREAD.get()));high.setItem(7,new ItemStack(ModItems.INFUSION_BUD.get()));
        out=UniversalUnderstanding.materials(CrystalProcessingTableMenu.previewCrystal(high,5,8,2,5,com.mcmagic.omnira.aggregation.AggregationContent.CRYSTAL.get()));
        h.assertTrue(count(out,ModItems.LINGERING_THREAD.get())==2&&count(out,ModItems.INFUSION_BUD.get())==1&&count(out,ModItems.INFUSED_SPIRITUAL_CRYSTAL.get())==1
                &&count(out,ModItems.SHARP_BREATH.get())==1&&count(out,ModItems.HEALING_DEW.get())==1,"High-tier inverse duplicated shared modifiers or returned wrong substrate");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void staffReturnsItsActualPartsAndRuneComponents(GameTestHelper h){
        var base=StaffAssembly.basic();var rune=new ItemStack(ModItems.SWIFTNESS_RUNE.get());
        rune.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("kept rune"));
        var assembly=new StaffAssembly(base.shaft(),base.reinforcement(),new ItemStack(ModItems.INFUSED_CRYSTAL_TIP.get()),List.of(rune,new ItemStack(ModItems.WIDE_AREA_RUNE.get())));
        h.assertTrue(assembly.valid(),"Test staff invalid");var staff=assembly.create();var parts=UniversalUnderstanding.materials(staff);
        h.assertTrue(parts.size()==5&&parts.stream().anyMatch(s->ItemStack.matches(s,rune)),"Lost staff component data or rune");
        parts.getFirst().shrink(1);h.assertTrue(!StaffAssembly.of(staff).shaft().isEmpty(),"Returned parts alias staff components");h.succeed();
    }
    @GameTest(template="spell_arena") public static void fullInventoryDropsMaterialsAndConsumesOnlyOneCrystal(GameTestHelper h){
        var p=player(h);p.setPos(h.absolutePos(new BlockPos(6,3,6)).getCenter());
        for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));
        var held=new ItemStack(ModItems.LOW_TIER_MAGIC_CRYSTAL.get(),2);p.setItemInHand(InteractionHand.MAIN_HAND,held);
        UniversalUnderstanding.use(p);
        h.assertTrue(held.getCount()==1,"Did not consume exactly one crystal");
        var drops=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(p.blockPosition()).inflate(3),e->e.getItem().is(ModItems.SPIRITUAL_CRYSTAL.get()));
        h.assertTrue(drops.stream().mapToInt(e->e.getItem().getCount()).sum()==1,"Full inventory lost or duplicated materials");h.succeed();
    }
    @GameTest(template="spell_arena") public static void whiteCoreAndMotesStayInsideTheGlobe(GameTestHelper h){
        h.assertTrue(!UnderstandingVisual.CORE.isEmpty()&&UnderstandingVisual.CORE.size()%4==0,"Core has incomplete faces");
        for(var v:UnderstandingVisual.CORE)h.assertTrue(Math.abs(v.x()-.5)<=.126&&Math.abs(v.y()-.5)<=.126&&Math.abs(v.z()-.5)<=.126,"Core is not a small stable pixel sphere");
        for(int i=0;i<UnderstandingVisual.CORE.size();i+=4){
            var a=UnderstandingVisual.CORE.get(i);var b=UnderstandingVisual.CORE.get(i+1);var c=UnderstandingVisual.CORE.get(i+2);
            double ax=b.x()-a.x(),ay=b.y()-a.y(),az=b.z()-a.z(),bx=c.x()-b.x(),by=c.y()-b.y(),bz=c.z()-b.z();
            h.assertTrue((ay*bz-az*by)*(a.x()-.5)+(az*bx-ax*bz)*(a.y()-.5)+(ax*by-ay*bx)*(a.z()-.5)>0,"Core has an inward or degenerate face");
        }
        for(int t=0;t<1000;t++)for(int i=0;i<UnderstandingVisual.MOTES;i++){
            var p=UnderstandingVisual.mote(i,t*.37);double dx=p.x()-.5,dy=p.y()-.5,dz=p.z()-.5;
            double distance=Math.sqrt(dx*dx+dy*dy+dz*dz);
            h.assertTrue(distance>=.139&&distance+Math.sqrt(3)*p.size()<.24&&p.alpha()>=0F&&p.alpha()<=.8F,"Mote escaped the globe or crossed the core");
        }
        h.succeed();
    }
}
