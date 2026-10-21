package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.aggregation.*;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_aggregation_process")
@PrefixGameTestTemplate(false)
public final class AggregationProcessGameTests {
    @GameTest(template="spell_arena") public static void reloadInfusionWorldButton(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(5,3,5));var player=BranchAdvancementGameTests.player(h);
        player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);h.getLevel().addFreshEntity(player);
        try{
            for(var facing:Direction.Plane.HORIZONTAL){
                player.setData(ModAttachments.MANA,new com.mcmagic.omnira.mana.ManaState(1200,1200));
                h.assertTrue(AggregationStructure.assemble(h.getLevel(),pos,facing),"Assembly failed");
                var machine=(AggregationRingBlockEntity)h.getLevel().getBlockEntity(pos);
                machine.setItem(0,new ItemStack(ModItems.AIR_MICROCORE.get()));
                machine.setItem(1,new ItemStack(ModItems.FIRE_MICROCORE.get()));
                machine.setItem(3,new ItemStack(com.mcmagic.omnira.myriad.WorldSeedContent.ACTIVE.get()));
                machine.setItem(5,new ItemStack(ModItems.SPIRITUAL_CRYSTAL.get()));
                machine.setItem(8,new ItemStack(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get(),2));
                machine.setItem(9,new ItemStack(ModItems.SPELL_INK.get()));
                h.assertTrue(machine.recipeError().equals("core")&&!machine.start(player)&&machine.progress()==0,
                        "TEST save's missing core was not diagnosed");
                h.assertTrue(machine.getItem(3).getCount()==1&&machine.getItem(8).getCount()==2
                        &&machine.getItem(9).getDamageValue()==0,"Failed start consumed materials");
                machine.setItem(10,com.mcmagic.omnira.item.SpellCoreItem.examples().get(facing.get2DDataValue()));
                h.assertTrue(machine.recipeReady(),"Reported combination rejected: "+machine.recipeError());
                var target=pos.getCenter().add(facing.getStepX()*AggregationLayout.BUTTON_Z,
                        AggregationLayout.BUTTON_Y+AggregationLayout.bob(h.getLevel().getGameTime(),true),
                        facing.getStepZ()*AggregationLayout.BUTTON_Z);
                net.minecraft.world.phys.BlockHitResult hit=null;
                for(double distance:new double[]{3,1.5,.8}){
                    player.setPos(target.add(facing.getStepX()*distance,0,facing.getStepZ()*distance));
                    var end=target.add(target.subtract(player.getEyePosition()).normalize());
                    hit=h.getLevel().clip(new net.minecraft.world.level.ClipContext(player.getEyePosition(),end,
                            net.minecraft.world.level.ClipContext.Block.OUTLINE,net.minecraft.world.level.ClipContext.Fluid.NONE,player));
                    h.assertTrue(AggregationLayout.button(AggregationLayout.local(hit.getLocation(),pos,facing),
                            AggregationLayout.bob(h.getLevel().getGameTime(),true)),"Visible button missed at distance "+distance+" facing "+facing+" hit "+hit.getLocation());
                }
                h.getLevel().getBlockState(hit.getBlockPos()).useWithoutItem(h.getLevel(),player,hit);
                h.assertTrue(machine.progress()==1,"Actual raycast missed start button facing "+facing+" hit "+hit.getLocation());
                for(int i=0;i<AggregationLayout.DURATION;i++)machine.tick();
                var result=machine.getItem(AggregationLayout.OUTPUT);
                h.assertTrue(result.is(AggregationContent.CRYSTAL.get()),"No high-tier crystal");
                var payload=result.get(ModDataComponents.SPELL_PAYLOAD);
                h.assertTrue(payload.keywords().contains("reload")&&payload.keywords().contains("infusion"),"Reload or infusion lost");
                h.assertTrue(payload.effects().size()==1&&payload.effects().getFirst().operation().equals("reload")
                        &&payload.effects().getFirst().infusion()==1&&payload.baseCost()==40,"Reload payload incorrect");
                h.assertTrue(machine.powered()&&machine.getItem(8).getCount()==1&&machine.getItem(9).getDamageValue()==1,
                        "Core retention or ingredient consumption incorrect");
                machine.clearContent();AggregationStructure.remove(h.getLevel(),pos,machine.getBlockState());
            }
        }finally{player.discard();}
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void startFailureReasons(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(5,3,5));AggregationStructure.assemble(h.getLevel(),pos,Direction.NORTH);
        var m=(AggregationRingBlockEntity)h.getLevel().getBlockEntity(pos);
        h.assertTrue(m.recipeError().equals("core"),"Missing core is silent");
        m.setItem(10,new ItemStack(ModItems.TEST_SPELL_CORE.get()));
        h.assertTrue(m.recipeError().equals("substrate"),"Missing substrate is silent");
        m.setItem(8,new ItemStack(ModItems.SPIRITUAL_CRYSTAL.get()));
        h.assertTrue(m.recipeError().equals("substrate"),"Wrong substrate is silent");
        m.setItem(8,new ItemStack(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get()));
        h.assertTrue(m.recipeError().equals("ink"),"Missing ink is silent");
        m.setItem(9,new ItemStack(ModItems.SPELL_INK.get()));
        h.assertTrue(m.recipeError().equals("pattern"),"Missing target is silent");
        m.setItem(0,new ItemStack(ModItems.AIR_MICROCORE.get()));
        h.assertTrue(m.recipeReady(),"Optional nodes became mandatory");
        m.setItem(2,new ItemStack(net.minecraft.world.item.Items.DIRT));
        h.assertTrue(m.recipeError().equals("nodes"),"Invalid element is silent");
        m.setItem(2,ItemStack.EMPTY);m.setItem(11,new ItemStack(AggregationContent.CRYSTAL.get()));
        h.assertTrue(m.recipeError().equals("output"),"Blocked output is silent");
        m.setItem(11,ItemStack.EMPTY);m.setItem(0,new ItemStack(ModItems.OMNI_MICROCORE.get()));
        h.assertTrue(m.recipeReady(),"World Rotation no longer starts");
        m.setItem(5,new ItemStack(ModItems.SPIRITUAL_CRYSTAL.get()));
        h.assertTrue(m.recipeError().equals("reserved"),"Reserved nodes are silent");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void completionAndPersistence(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(5,3,5));var player=BranchAdvancementGameTests.player(h);player.setPos(pos.getCenter());
        player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);h.getLevel().addFreshEntity(player);
        try{
            AggregationStructure.assemble(h.getLevel(),pos,Direction.NORTH);
            var machine=(AggregationRingBlockEntity)h.getLevel().getBlockEntity(pos);
            machine.setItem(0,new ItemStack(ModItems.EARTH_MICROCORE.get(),2));
            machine.setItem(8,new ItemStack(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get(),2));
            machine.setItem(9,new ItemStack(ModItems.SPELL_INK.get()));machine.setItem(10,new ItemStack(ModItems.TEST_SPELL_CORE.get()));
            h.assertTrue(machine.start(player),"Cannot start synthesis");
            for(int i=0;i<80;i++)machine.tick();
            var saved=machine.saveWithFullMetadata(h.getLevel().registryAccess());machine.loadWithComponents(saved,h.getLevel().registryAccess());
            h.assertTrue(machine.progress()==81,"Progress lost across save");
            for(int i=80;i<159;i++)machine.tick();
            h.assertTrue(machine.getItem(11).isEmpty(),"Synthesis completed before eight seconds");
            machine.tick();
            h.assertTrue(machine.getItem(11).is(AggregationContent.CRYSTAL.get()),"Output missing");
            h.assertTrue(BranchAdvancementGameTests.recorded(player,"eightfold_work"),"Successful aggregation did not record branch");
            h.assertTrue(machine.getItem(0).getCount()==1&&machine.getItem(8).getCount()==1,"Ingredient count incorrect");
            h.assertTrue(machine.powered()&&machine.getItem(9).getDamageValue()==1,"Core consumed or ink durability incorrect");
            h.assertTrue(machine.getItem(11).get(ModDataComponents.SPELL_PAYLOAD).baseCost()==10,"Crafting surcharge leaked into spell cost");
            h.assertTrue(machine.collect(player)&&!machine.collect(player),"Output duplicated");
        }finally{player.discard();}
        h.succeed();
    }
}
