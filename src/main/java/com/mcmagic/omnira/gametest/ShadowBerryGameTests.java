package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.alchemy.catalysis.CatalysisGrowth;
import com.mcmagic.omnira.mana.ManaState;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_shadow_berries") @PrefixGameTestTemplate(false)
public final class ShadowBerryGameTests {
    @GameTest(template="spell_arena") public static void eatingRestoresFoodAndAttributeScaledMana(GameTestHelper h){
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        player.getAttribute(ModAttributes.MAX_MANA).setBaseValue(250);
        player.getFoodData().setFoodLevel(5);player.getFoodData().setSaturation(0);
        player.setData(ModAttachments.MANA,new ManaState(0,100,2));
        var berries=new ItemStack(DreamContent.SHADOW_BERRIES.get(),3);
        var food=berries.get(DataComponents.FOOD);
        h.assertTrue(food!=null&&food.nutrition()==3&&Math.abs(food.saturation()-4)<.0001&&!food.canAlwaysEat(),"Berry food values incorrect");
        var result=berries.finishUsingItem(h.getLevel(),player);
        h.assertTrue(result.getCount()==2&&player.getFoodData().getFoodLevel()==8&&Math.abs(player.getFoodData().getSaturationLevel()-4)<.0001,"Eating did not use native food/consumption");
        var mana=player.getData(ModAttachments.MANA);
        h.assertTrue(mana.current()==50&&mana.maximum()==250&&mana.affinity()==2,"Mana did not use current maximum or lost affinity");
        player.setData(ModAttachments.MANA,new ManaState(240,250,2));
        result.finishUsingItem(h.getLevel(),player);
        h.assertTrue(player.getData(ModAttachments.MANA).current()==250,"Berry overcharged mana");
        player.setData(ModAttachments.MANA,new ManaState(300,250,2));
        result.finishUsingItem(h.getLevel(),player);
        h.assertTrue(player.getData(ModAttachments.MANA).current()==300,"Berry changed existing pulse overcharge");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void edibleBerriesKeepPlantingAndCatalysis(GameTestHelper h){
        var seed=new ItemStack(DreamContent.SHADOW_BERRIES.get());
        h.assertTrue(Block.byItem(seed.getItem())==DreamContent.SHADOW_BERRIES.get(),"Edible berries lost planting block");
        for(var substrate:new Block[]{DreamContent.SHADOW_LOG.get(),DreamContent.SHADOW_PLANKS.get()}){
            var base=new ItemStack(substrate);
            var plan=CatalysisGrowth.resolve(h.getLevel(),base,seed,new FluidStack(Fluids.WATER,100));
            h.assertTrue(plan!=null&&plan.stages()==4,"Existing berry catalysis no longer resolves");
            var drops=plan.harvest(h.getLevel(),h.absolutePos(net.minecraft.core.BlockPos.ZERO),base,seed);
            h.assertTrue(drops.size()==1&&drops.getFirst().is(seed.getItem())&&drops.getFirst().getCount()==8,"Berry catalysis yield changed");
            h.assertTrue(CatalysisGrowth.resolve(h.getLevel(),base,seed,new FluidStack(Fluids.LAVA,100))==null,"Berry catalysis accepted wrong fluid");
        }
        h.succeed();
    }
    private ShadowBerryGameTests(){}
}
