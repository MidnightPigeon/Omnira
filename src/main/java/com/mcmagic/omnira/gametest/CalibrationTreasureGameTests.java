package com.mcmagic.omnira.gametest;

import com.google.gson.JsonParser;
import com.mcmagic.omnira.block.entity.LiquidCrystalBallBlockEntity;
import com.mcmagic.omnira.compat.TreasurePotionFluid;
import com.mcmagic.omnira.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_calibration")
@PrefixGameTestTemplate(false)
public final class CalibrationTreasureGameTests {
    @GameTest(template="spell_arena")
    public static void potionBottleTransactions(GameTestHelper h){
        var tank=new net.neoforged.neoforge.fluids.capability.templates.FluidTank(1000);
        var bottle=new ItemStack(Items.GLASS_BOTTLE);
        for(var type:java.util.List.of(Items.POTION,Items.SPLASH_POTION,Items.LINGERING_POTION)){
            var input=net.minecraft.world.item.alchemy.PotionContents.createItemStack(type,net.minecraft.world.item.alchemy.Potions.STRONG_HEALING);
            tank.setFluid(FluidStack.EMPTY);
            var preview=com.mcmagic.omnira.compat.OrbPotionContainers.transfer(input,tank,true,false);
            h.assertTrue(preview.isSuccess()&&tank.isEmpty()&&input.getCount()==1,"Input simulation mutated inventory");
            h.assertTrue(com.mcmagic.omnira.compat.OrbPotionContainers.transfer(input,tank,true,true).isSuccess()&&tank.getFluidAmount()==250,"Potion could not be deposited");
            var result=com.mcmagic.omnira.compat.OrbPotionContainers.transfer(bottle,tank,false,false);
            h.assertTrue(result.isSuccess()&&result.getResult().is(type)&&tank.getFluidAmount()==250,"Bottle type or simulation changed");
            result=com.mcmagic.omnira.compat.OrbPotionContainers.transfer(bottle,tank,false,true);
            h.assertTrue(tank.isEmpty()&&input.get(DataComponents.POTION_CONTENTS).equals(result.getResult().get(DataComponents.POTION_CONTENTS)),"Bottle round trip lost potion data");
        }
        tank.setFluid(new FluidStack(net.minecraft.world.level.material.Fluids.WATER,249));
        h.assertTrue(!com.mcmagic.omnira.compat.OrbPotionContainers.transfer(bottle,tank,false,true).isSuccess()&&tank.getFluidAmount()==249,"Partial bottle consumed fluid");
        tank.setFluid(new FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000));
        var healing=net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.POTION,net.minecraft.world.item.alchemy.Potions.HEALING);
        h.assertTrue(!com.mcmagic.omnira.compat.OrbPotionContainers.transfer(healing,tank,true,true).isSuccess()&&tank.getFluidAmount()==1000,"Full or incompatible tank accepted potion");
        var water=com.mcmagic.omnira.compat.OrbPotionContainers.transfer(bottle,tank,false,true);
        h.assertTrue(water.isSuccess()&&tank.getFluidAmount()==750&&water.getResult().get(DataComponents.POTION_CONTENTS).is(net.minecraft.world.item.alchemy.Potions.WATER),"UI extraction incorrectly used infinite water");
        h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=140)
    public static void bottlePotionFromOrb(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(3,3,3));
        h.getLevel().setBlockAndUpdate(pos,ModBlocks.LIQUID_CRYSTAL_BALL.get().defaultBlockState());
        var ball=(LiquidCrystalBallBlockEntity)h.getLevel().getBlockEntity(pos);
        var fluid=BuiltInRegistries.FLUID.get(ResourceLocation.parse("create:potion"));
        var potion=TreasurePotionFluid.prepare(new FluidStack(fluid,24000),h.getLevel().random);
        h.assertTrue(potion.get(DataComponents.POTION_CONTENTS).hasEffects(),"Treasure potion must have an effect");
        ball.tank.setFluid(potion);
        h.assertTrue(ball.canPlaceItem(0,new ItemStack(Items.GLASS_BOTTLE)),"Bottle rejected by container slot");
        ball.setItem(0,new ItemStack(Items.GLASS_BOTTLE));
        h.runAfterDelay(65,()->{
            h.assertTrue(ball.getItem(0).is(Items.POTION),"Orb cannot bottle its potion treasure");
            h.assertTrue(potion.get(DataComponents.POTION_CONTENTS).equals(ball.getItem(0).get(DataComponents.POTION_CONTENTS)),"Potion effect changed during extraction");
            h.assertTrue(ball.tank.getFluidAmount()==23750,"Bottling must consume exactly 250 mB");
            h.runAfterDelay(50,()->{
                h.assertTrue(ball.tank.getFluidAmount()==23750&&ball.getItem(0).is(Items.POTION),"Filled bottle automatically emptied back into orb");
                h.succeed();
            });
        });
    }

    @GameTest(template="spell_arena")
    public static void templateAndLoot(GameTestHelper h)throws Exception {
        try(var in=CalibrationTreasureGameTests.class.getResourceAsStream("/data/omnira/structures/calibration_station.json")) {
            var cells=JsonParser.parseReader(new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonArray();
            int items=0,fluids=0;
            var seen=new java.util.HashSet<String>();
            for(var raw:cells){
                var c=raw.getAsJsonObject();var id=ResourceLocation.parse(c.get("name").getAsString());
                h.assertTrue(BuiltInRegistries.BLOCK.containsKey(id),"Unknown blueprint block: "+id);
                var block=BuiltInRegistries.BLOCK.get(id);
                if(c.has("properties"))for(var property:c.getAsJsonObject("properties").entrySet()){
                    var p=block.getStateDefinition().getProperty(property.getKey());
                    h.assertTrue(p!=null&&p.getValue(property.getValue().getAsString()).isPresent(),"Invalid blueprint state: "+c);
                }
                h.assertTrue(seen.add(c.get("x")+","+c.get("y")+","+c.get("z")),"Duplicate blueprint coordinate");
                if(id.getPath().equals("crystal_ball")){
                    items++;var tag=net.minecraft.nbt.TagParser.parseTag(c.get("nbt").getAsString());
                    var key=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,ResourceLocation.parse(tag.getString("LootTable")));
                    var table=h.getLevel().getServer().reloadableRegistries().getLootTable(key);
                    var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(h.getLevel()).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,h.absolutePos(BlockPos.ZERO).getCenter()).create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
                    h.assertTrue(!table.getRandomItems(params).isEmpty(),"Empty/unregistered calibration treasure");
                }
                if(id.getPath().equals("liquid_crystal_ball")){
                    fluids++;h.assertTrue(net.minecraft.nbt.TagParser.parseTag(c.get("nbt").getAsString()).getBoolean("FluidTreasure"),"Liquid ball is not unopened treasure");
                }
            }
            h.assertTrue(items==2&&fluids==1,"Expected two item treasures and one fluid treasure");
        }
        h.succeed();
    }
}
