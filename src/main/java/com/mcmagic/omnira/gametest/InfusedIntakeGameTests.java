package com.mcmagic.omnira.gametest;
import com.mcmagic.omnira.storage.InfusedIntake;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.block.entity.OrbUpgrades;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.items.ItemStackHandler;
@GameTestHolder("omnira_infused_intake") @PrefixGameTestTemplate(false)
public final class InfusedIntakeGameTests {
    @GameTest(template="spell_arena") public static void cooldownFiltersAndCrops(GameTestHelper h){
        var level=h.getLevel();var origin=h.absolutePos(new BlockPos(4,3,4));var center=origin.getCenter();
        var storage=new ItemStackHandler(12);var upgrades=new ItemStackHandler(1);var plate=new ItemStack(ModItems.INFUSED_INTAKE_UPGRADE.get());upgrades.setStackInSlot(0,plate);
        var drop=new net.minecraft.world.entity.item.ItemEntity(level,center.x,center.y,center.z,new ItemStack(Items.DIAMOND,3));level.addFreshEntity(drop);
        var clock=new InfusedIntake();for(int i=0;i<199;i++)clock.tick(level,center,upgrades,storage);
        h.assertTrue(drop.isAlive(),"Collected before ten seconds");clock.tick(level,center,upgrades,storage);
        h.assertTrue(drop.isRemoved()&&storage.getStackInSlot(0).getCount()==3,"Pickup failed");
        var crop=origin.east();level.setBlock(crop.below(),Blocks.FARMLAND.defaultBlockState(),2);level.setBlock(crop,Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE,7),2);
        var rock=origin.west();level.setBlock(rock,Blocks.STONE.defaultBlockState(),2);
        OrbUpgrades.setFilter(plate,0,new ItemStack(Items.WHEAT),level.registryAccess());
        InfusedIntake.collect(level,center,plate,storage);
        h.assertTrue(level.getBlockState(crop).getValue(CropBlock.AGE)==7,"Blacklisted harvest changed crop");
        OrbUpgrades.setFilter(plate,0,ItemStack.EMPTY,level.registryAccess());InfusedIntake.collect(level,center,plate,storage);
        h.assertTrue(level.getBlockState(crop).getValue(CropBlock.AGE)==0&&level.getBlockState(rock).is(Blocks.STONE),"Harvest did not replant or broke an ordinary block");h.succeed();
    }
    @GameTest(template="spell_arena") public static void compatibilityAndFullStorage(GameTestHelper h){
        var kind=com.mcmagic.omnira.item.OrbUpgradeItem.Kind.INFUSED_INTAKE;
        var expected=java.util.Set.of(com.mcmagic.omnira.item.UpgradeCompatibility.Host.CRYSTAL_BALL,com.mcmagic.omnira.item.UpgradeCompatibility.Host.RESONANT_CRYSTAL_BALL,com.mcmagic.omnira.item.UpgradeCompatibility.Host.CRUISE_ORB);
        h.assertTrue(java.util.Set.copyOf(com.mcmagic.omnira.item.UpgradeCompatibility.hosts(kind)).equals(expected),"Upgrade accepts a machine or fluid container");
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(4,3,4));var storage=new ItemStackHandler(1);storage.setStackInSlot(0,new ItemStack(Items.STONE,64));var plate=new ItemStack(ModItems.INFUSED_INTAKE_UPGRADE.get());
        level.setBlock(pos.below(),Blocks.FARMLAND.defaultBlockState(),2);level.setBlock(pos,Blocks.CARROTS.defaultBlockState().setValue(CropBlock.AGE,7),2);
        var sheep=net.minecraft.world.entity.EntityType.SHEEP.create(level);sheep.setPos(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5);level.addFreshEntity(sheep);float health=sheep.getHealth();
        InfusedIntake.collect(level,pos.getCenter(),plate,storage);
        h.assertTrue(!sheep.isSheared()&&level.getBlockState(pos).getValue(CropBlock.AGE)==7,"Full storage consumed harvest");
        storage.setStackInSlot(0,ItemStack.EMPTY);InfusedIntake.collect(level,sheep.position(),plate,new ItemStackHandler(12));
        h.assertTrue(sheep.isSheared()&&sheep.getHealth()==health,"Shearing failed or damaged animal");h.succeed();
    }
    @GameTest(template="spell_arena") public static void configurationMenu(GameTestHelper h){
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);var plate=new ItemStack(ModItems.INFUSED_INTAKE_UPGRADE.get());player.getInventory().setItem(0,plate);
        var menu=new com.mcmagic.omnira.menu.OrbUpgradeMenu(1,player.getInventory(),0,true);
        h.assertTrue(menu.stillValid(player)&&menu.areaIntake,"New upgrade cannot open filter menu");
        h.assertTrue(menu.clickMenuButton(player,0)&&OrbUpgrades.settings(plate).getBoolean("Whitelist"),"Whitelist toggle failed");
        h.assertTrue(!menu.clickMenuButton(player,1),"Range upgrade exposes adjacency directions");h.succeed();
    }
    @GameTest(template="spell_arena") public static void animalsAndWhitelist(GameTestHelper h){
        var level=h.getLevel();var center=h.absolutePos(new BlockPos(4,3,4)).getCenter();
        var cow=net.minecraft.world.entity.EntityType.COW.create(level);cow.setPos(center);level.addFreshEntity(cow);
        var chicken=net.minecraft.world.entity.EntityType.CHICKEN.create(level);chicken.setPos(center);level.addFreshEntity(chicken);
        float health=cow.getHealth();var storage=new ItemStackHandler(12);var plate=new ItemStack(ModItems.INFUSED_INTAKE_UPGRADE.get());
        var tag=OrbUpgrades.settings(plate);tag.putBoolean("Whitelist",true);plate.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));
        OrbUpgrades.setFilter(plate,0,new ItemStack(ModItems.MILK_SUSPENSION.get()),level.registryAccess());
        InfusedIntake.collect(level,center,plate,storage);
        h.assertTrue(storage.getStackInSlot(0).is(ModItems.MILK_SUSPENSION.get())&&storage.getStackInSlot(0).getCount()==1&&cow.getHealth()==health,"Cow harvest failed or caused damage");
        for(int slot=0;slot<storage.getSlots();slot++)h.assertTrue(!storage.getStackInSlot(slot).is(Items.EGG),"Forced egg production");h.succeed();
    }
    @GameTest(template="spell_arena") public static void movingCenterRetainsSphereBoundary(GameTestHelper h){
        var level=h.getLevel();var origin=h.absolutePos(new BlockPos(4,3,4));var center=new net.minecraft.world.phys.Vec3(origin.getX()+.99,origin.getY()+.5,origin.getZ()+.5);
        var inside=origin.east(8);var outside=origin.west(8);
        for(var pos:java.util.List.of(inside,outside)){level.setBlock(pos.below(),Blocks.SOUL_SAND.defaultBlockState(),2);level.setBlock(pos,Blocks.NETHER_WART.defaultBlockState().setValue(NetherWartBlock.AGE,3),2);}
        InfusedIntake.collect(level,center,new ItemStack(ModItems.INFUSED_INTAKE_UPGRADE.get()),new ItemStackHandler(12));
        h.assertTrue(level.getBlockState(inside).getValue(NetherWartBlock.AGE)==0&&level.getBlockState(outside).getValue(NetherWartBlock.AGE)==3,"Moving sphere boundary changed");h.succeed();
    }
    @GameTest(template="spell_arena") public static void capacityTrialRefreshesAcrossHarvests(GameTestHelper h){
        var level=h.getLevel();var origin=h.absolutePos(new BlockPos(4,3,4));
        for(int i=0;i<3;i++){var pos=origin.east(i);level.setBlock(pos.below(),Blocks.SOUL_SAND.defaultBlockState(),2);level.setBlock(pos,Blocks.NETHER_WART.defaultBlockState().setValue(NetherWartBlock.AGE,3),2);}
        var storage=new ItemStackHandler(1);storage.setStackInSlot(0,new ItemStack(Items.NETHER_WART,63));
        InfusedIntake.collect(level,origin.getCenter(),new ItemStack(ModItems.INFUSED_INTAKE_UPGRADE.get()),storage);
        h.assertTrue(storage.getStackInSlot(0).getCount()<=64,"Capacity simulation overfilled inventory");
        if(storage.getStackInSlot(0).getCount()==64)h.assertTrue(level.getBlockState(origin.east(2)).getValue(NetherWartBlock.AGE)==3,"Full storage consumed later crop");h.succeed();
    }
    @GameTest(template="spell_arena") public static void assemblyRecipe(GameTestHelper h){
        var id=net.minecraft.resources.ResourceLocation.parse("omnira:assembly/infused_intake_upgrade");
        var holder=h.getLevel().getRecipeManager().byKey(id).orElseThrow();
        var recipe=(com.mcmagic.omnira.recipe.AssemblyRecipe)holder.value();var input=new net.minecraft.world.SimpleContainer(7);
        String[] ids={"infused_spiritual_crystal","resonance_core","dissociation_thread","construction_matrix"};
        for(int i=0;i<4;i++)input.setItem(i,new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("omnira:"+ids[i]))));
        input.setItem(6,new ItemStack(ModItems.INTAKE_UPGRADE.get()));
        var data=new com.mcmagic.omnira.recipe.AssemblyRecipe.Input(input);
        h.assertTrue(recipe.matches(data,h.getLevel())&&recipe.assemble(data,h.getLevel().registryAccess()).is(ModItems.INFUSED_INTAKE_UPGRADE.get()),"Shapeless outer recipe fails");h.succeed();
    }
}
