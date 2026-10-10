package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.workstation.*;
import com.mcmagic.omnira.registry.ModAttachments;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_crystal_utility") @PrefixGameTestTemplate(false)
public final class CrystalUtilityGameTests {
    @GameTest(template="spell_arena") public static void anvilShiftClickAllEquipmentAndInventorySlots(GameTestHelper h){
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        Item[] equipment={Items.IRON_SWORD,Items.IRON_AXE,Items.IRON_PICKAXE,Items.IRON_SHOVEL,Items.IRON_HOE,
                Items.BOW,Items.CROSSBOW,Items.TRIDENT,Items.SHIELD,Items.ELYTRA,Items.IRON_HELMET,
                Items.IRON_CHESTPLATE,Items.IRON_LEGGINGS,Items.IRON_BOOTS,Items.FISHING_ROD,
                Items.SHEARS,Items.FLINT_AND_STEEL,Items.ENCHANTED_BOOK,Items.IRON_INGOT,Items.COAL};
        for(int index:new int[]{2,5}){
            var work=machine(h,index);var menu=new CrystalWorkstationMenu(1,player.getInventory(),work);
            for(int source=3;source<39;source++)for(var item:equipment){
                work.clearContent();player.getInventory().clearContent();menu.getSlot(source).set(new ItemStack(item));
                h.assertTrue(!menu.quickMoveStack(player,source).isEmpty()&&work.getItem(0).is(item)
                        &&work.getItem(1).isEmpty()&&!menu.getSlot(source).hasItem(),"Anvil did not prefer first input: machine="+index+", slot="+source+", item="+item);
                menu.getSlot(source).set(new ItemStack(Items.DIAMOND_SWORD));
                h.assertTrue(!menu.quickMoveStack(player,source).isEmpty()&&work.getItem(0).is(item)
                        &&work.getItem(1).is(Items.DIAMOND_SWORD)&&!menu.getSlot(source).hasItem(),"Anvil did not use second input after first was occupied");
                menu.getSlot(source).set(new ItemStack(Items.NETHERITE_SWORD));
                h.assertTrue(menu.quickMoveStack(player,source).isEmpty()&&menu.getSlot(source).getItem().is(Items.NETHERITE_SWORD)
                        &&work.getItem(0).is(item)&&work.getItem(1).is(Items.DIAMOND_SWORD),"Full anvil changed or lost items");
            }
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void grindstoneShiftClickEquipmentAndRejectsInvalidItems(GameTestHelper h){
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        Item[] equipment={Items.IRON_SWORD,Items.IRON_AXE,Items.IRON_PICKAXE,Items.IRON_SHOVEL,Items.IRON_HOE,
                Items.BOW,Items.CROSSBOW,Items.TRIDENT,Items.SHIELD,Items.ELYTRA,Items.IRON_HELMET,
                Items.IRON_CHESTPLATE,Items.IRON_LEGGINGS,Items.IRON_BOOTS,Items.FISHING_ROD,Items.SHEARS,Items.FLINT_AND_STEEL};
        var sharp=h.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS);
        for(int index:new int[]{1,4}){
            var work=machine(h,index);var menu=new CrystalWorkstationMenu(1,player.getInventory(),work);
            for(int source=3;source<39;source++){
                for(var item:equipment){
                    work.clearContent();player.getInventory().clearContent();menu.getSlot(source).set(new ItemStack(item));
                    h.assertTrue(!menu.quickMoveStack(player,source).isEmpty()&&work.getItem(0).is(item)
                            &&work.getItem(1).isEmpty()&&!menu.getSlot(source).hasItem(),"Grindstone did not prefer first input");
                    menu.getSlot(source).set(new ItemStack(Items.DIAMOND_SWORD));menu.quickMoveStack(player,source);
                    h.assertTrue(work.getItem(0).is(item)&&work.getItem(1).is(Items.DIAMOND_SWORD)&&!menu.getSlot(source).hasItem(),"Grindstone second input failed");
                    menu.getSlot(source).set(new ItemStack(Items.NETHERITE_SWORD));
                    h.assertTrue(menu.quickMoveStack(player,source).isEmpty()&&menu.getSlot(source).getItem().is(Items.NETHERITE_SWORD),"Full grinder lost equipment");
                }
                work.clearContent();player.getInventory().clearContent();
                var book=new ItemStack(Items.ENCHANTED_BOOK);book.enchant(sharp,1);var expected=book.copy();menu.getSlot(source).set(book);
                menu.quickMoveStack(player,source);
                h.assertTrue(ItemStack.matches(work.getItem(0),expected)&&work.getItem(1).isEmpty()&&!menu.getSlot(source).hasItem(),"Grindstone book routing: machine="+index+", source="+source+", first="+work.getItem(0)+", second="+work.getItem(1)+", remaining="+menu.getSlot(source).getItem()+", expected="+expected);
                work.clearContent();menu.getSlot(source).set(new ItemStack(Items.COAL,8));
                h.assertTrue(menu.quickMoveStack(player,source).isEmpty()&&work.getItem(0).isEmpty()&&work.getItem(1).isEmpty()
                        &&menu.getSlot(source).getItem().is(Items.COAL)&&menu.getSlot(source).getItem().getCount()==8,"Grindstone accepted or lost invalid items");
            }
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void utilityShiftClickUsesMachineKindNotInventorySlot(GameTestHelper h){
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        for(int index=0;index<6;index++){
            var work=machine(h,index);var menu=new CrystalWorkstationMenu(1,player.getInventory(),work);
            for(int source=3;source<39;source++){
                work.clearContent();player.getInventory().clearContent();
                var item=index%3==0?Items.RAW_IRON:Items.IRON_SWORD;
                menu.getSlot(source).set(new ItemStack(item));menu.quickMoveStack(player,source);
                h.assertTrue(work.getItem(0).is(item)&&work.getItem(1).isEmpty()&&!menu.getSlot(source).hasItem(),"Utility input routing depends on inventory slot");
                if(index%3==0){
                    menu.getSlot(source).set(new ItemStack(Items.COAL));menu.quickMoveStack(player,source);
                    h.assertTrue(work.getItem(1).is(Items.COAL)&&!menu.getSlot(source).hasItem(),"Furnace fuel did not enter lower input");
                }
            }
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void menusMatchVanillaSlotLayouts(GameTestHelper h){
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        var snapshots=new com.google.gson.JsonArray();
        for(int index=0;index<6;index++){
            var work=machine(h,index);var menu=new CrystalWorkstationMenu(1,player.getInventory(),work);
            net.minecraft.world.inventory.AbstractContainerMenu vanilla=switch(index%3){
                case 0->new net.minecraft.world.inventory.FurnaceMenu(2,player.getInventory());
                case 1->new net.minecraft.world.inventory.GrindstoneMenu(2,player.getInventory());
                default->new net.minecraft.world.inventory.AnvilMenu(2,player.getInventory());
            };
            h.assertTrue(menu.slots.size()==vanilla.slots.size(),"Vanilla inventory size changed");
            var positions=new com.google.gson.JsonArray();
            for(int i=0;i<menu.slots.size();i++){
                var a=menu.getSlot(i);var b=vanilla.getSlot(i);
                h.assertTrue(a.x==b.x&&a.y==b.y,"Utility slot differs from vanilla: machine="+index+", slot="+i);
                var xy=new com.google.gson.JsonArray();xy.add(a.x);xy.add(a.y);positions.add(xy);
            }
            snapshots.add(positions);
        }
        try{
            var out=java.nio.file.Path.of("../../build/previews/utility-layout.json");java.nio.file.Files.createDirectories(out.getParent());
            java.nio.file.Files.writeString(out,new com.google.gson.Gson().toJson(snapshots));
        }catch(java.io.IOException e){throw new IllegalStateException(e);}
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void infusedFuelSmeltsSixteenAndDoesNotResumeAfterPower(GameTestHelper h){
        for(int index:new int[]{0,3}){
            var work=machine(h,index);var crystal=new ItemStack(com.mcmagic.omnira.registry.ModItems.INFUSED_SPIRITUAL_CRYSTAL.get());
            h.assertTrue(work.canPlaceItem(1,crystal),"Furnace refuses infused fuel");
            work.setItem(0,new ItemStack(Items.RAW_IRON,17));work.setItem(1,crystal);
            for(int tick=0;tick<1700;tick++)work.tick(false,0);
            h.assertTrue(work.getItem(2).getCount()==16&&work.getItem(0).getCount()==1&&work.getItem(1).isEmpty()&&work.burn==0,"Single crystal must smelt exactly sixteen items");
            var player=h.makeMockPlayer(GameType.SURVIVAL);player.getInventory().setItem(0,new ItemStack(com.mcmagic.omnira.registry.ModItems.INFUSED_SPIRITUAL_CRYSTAL.get()));
            var menu=new CrystalWorkstationMenu(1,player.getInventory(),work);menu.quickMoveStack(player,30);
            h.assertTrue(work.getItem(1).is(com.mcmagic.omnira.registry.ModItems.INFUSED_SPIRITUAL_CRYSTAL.get())&&player.getInventory().getItem(0).isEmpty(),"Shift-click did not insert fuel into lower slot");
        }
        var work=machine(h,3);work.setItem(0,new ItemStack(Items.RAW_IRON,32));
        for(boolean mechanical:new boolean[]{true,false}){
            work.setItem(1,new ItemStack(com.mcmagic.omnira.registry.ModItems.INFUSED_SPIRITUAL_CRYSTAL.get()));
            work.tick(false,0);int full=work.burn;
            for(int tick=0;tick<10;tick++)work.tick(false,0);
            h.assertTrue(full==1600&&work.burn==1590,"Fuel timer does not descend");
            if(!mechanical)work.energy=256;
            work.tick(mechanical,mechanical?16:0);
            h.assertTrue(work.powered()&&work.burn==0&&work.burnTotal==0,"External power retained old fuel");
            work.energy=0;int progress=work.progress;work.tick(false,0);
            h.assertTrue(!work.powered()&&work.burn==0&&work.progress==progress,"Disconnect resumed old fuel");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void basicGrindstoneKeepsVanillaCurses(GameTestHelper h){
        var work=machine(h,1);var player=h.makeMockPlayer(GameType.SURVIVAL);
        var registry=h.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        var sharp=registry.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS);
        var curse=registry.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.VANISHING_CURSE);
        var sword=new ItemStack(Items.IRON_SWORD);sword.enchant(sharp,2);sword.enchant(curse,1);work.setItem(0,sword);
        var result=work.take(player,1);
        h.assertTrue(!result.isEmpty()&&result.getEnchantmentLevel(sharp)==0&&result.getEnchantmentLevel(curse)==1&&work.getItem(0).isEmpty(),"Basic grinder changed vanilla curse or input consumption");h.succeed();
    }
    @GameTest(template="spell_arena") public static void craftingAndAnvilItemState(GameTestHelper h){
        for(int i=0;i<6;i++){
            var id=net.minecraft.resources.ResourceLocation.parse("omnira:"+(i<3?"assembly/":"advanced_assembly_table/")+CrystalWorkstations.IDS[i]);
            var recipe=h.getLevel().getRecipeManager().byKey(id).orElseThrow().value();
            h.assertTrue(recipe.getResultItem(h.getLevel().registryAccess()).is(CrystalWorkstations.BLOCKS.get(i).get().asItem()),"Missing utility recipe output");
        }
        var work=machine(h,2);work.durability=3;work.repairMana=150;
        var drops=net.minecraft.world.level.block.Block.getDrops(work.getBlockState(),h.getLevel(),work.getBlockPos(),h.getLevel().getBlockEntity(work.getBlockPos()));
        h.assertTrue(drops.size()==1,"Anvil drop missing");
        var copy=new CrystalWorkstationEntity(work.getBlockPos(),work.getBlockState());copy.applyComponentsFromItemStack(drops.getFirst());
        h.assertTrue(copy.durability==3&&copy.repairMana==150,"Breaking and replacing reset anvil durability");h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=120) public static void actualEngineRequiresAlignedShaft(GameTestHelper h){
        var work=machine(h,3);var pos=work.getBlockPos().south();
        h.getLevel().setBlockAndUpdate(pos,com.mcmagic.omnira.registry.ModBlocks.MANA_ENGINE.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING,Direction.NORTH));
        var engine=((com.mcmagic.omnira.block.entity.ManaEngineAccess)h.getLevel().getBlockEntity(pos)).engineState();
        engine.redstoneControl=false;engine.inventory.setItem(0,new ItemStack(com.mcmagic.omnira.registry.ModItems.TEST_SPELL_CORE.get()));
        h.runAfterDelay(35,()->{
            h.assertTrue(work.mechanical&&work.rpm!=0,"Aligned engine did not drive workstation");
            h.getLevel().setBlockAndUpdate(pos,h.getLevel().getBlockState(pos).setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING,Direction.EAST));
            h.runAfterDelay(30,()->{h.assertTrue(!work.mechanical,"Misaligned engine still drives workstation");h.succeed();});
        });
    }
    private static CrystalWorkstationEntity machine(GameTestHelper h,int index){
        var pos=h.absolutePos(new BlockPos(4,2,4));h.getLevel().setBlockAndUpdate(pos,CrystalWorkstations.BLOCKS.get(index).get().defaultBlockState());
        return CrystalWorkstations.work(h.getLevel().getBlockEntity(pos));
    }
    @GameTest(template="spell_arena") public static void furnaceDoubleSpeedAndFixedElectricCost(GameTestHelper h){
        var work=machine(h,3);work.setItem(0,new ItemStack(Items.RAW_IRON));work.energy=2560;
        for(int i=0;i<99;i++)work.tick(false,0);
        h.assertTrue(work.getItem(2).isEmpty(),"Furnace completed too early");work.tick(false,0);
        h.assertTrue(work.getItem(2).is(Items.IRON_INGOT)&&work.energy==1280&&work.rpm==0&&work.electrical,"Electric smelting timing or fixed consumption failed");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void sharedAnvilEnergyLimit(GameTestHelper h){
        var work=machine(h,5);var facing=work.getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
        var front=work.energyPort(facing);var rear=work.energyPort(facing.getOpposite());
        int expected=front.receiveEnergy(Integer.MAX_VALUE,true);
        h.assertTrue(expected>=12&&expected<=13&&work.energy==0,"Unbounded or mutating FE simulation");
        h.assertTrue(front.receiveEnergy(1,false)+rear.receiveEnergy(Integer.MAX_VALUE,false)==expected,"Two faces did not share budget");
        h.assertTrue(front.receiveEnergy(100,false)==0&&rear.receiveEnergy(100,false)==0,"Two faces bypassed limit");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void fixedOccupancyAtHighRpm(GameTestHelper h){
        if(net.neoforged.fml.ModList.get().isLoaded("create"))CrystalUtilityPowerChecks.fixedOccupancy(h);
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void fuelAndOutputBlocking(GameTestHelper h){
        var work=machine(h,0);work.setItem(0,new ItemStack(Items.RAW_IRON,2));work.setItem(1,new ItemStack(Items.COAL));
        for(int i=0;i<100;i++)work.tickDirect();h.assertTrue(work.getItem(2).is(Items.IRON_INGOT),"Crystal furnace did not smelt in 100 ticks");
        work.setItem(2,new ItemStack(Items.GOLD_INGOT,64));for(int i=0;i<110;i++)work.tickDirect();
        h.assertTrue(work.getItem(0).getCount()==1&&work.getItem(2).getCount()==64,"Blocked output consumed input");h.succeed();
    }
    @GameTest(template="spell_arena") public static void anvilDurabilityAndExperience(GameTestHelper h){
        var work=machine(h,2);var player=h.makeMockPlayer(GameType.SURVIVAL);player.experienceLevel=30;
        var tool=new ItemStack(Items.IRON_PICKAXE);tool.setDamageValue(120);work.setItem(0,tool);work.setItem(1,new ItemStack(Items.IRON_INGOT));
        var result=work.recipe();int levels=result.levels();h.assertTrue(!result.stack().isEmpty(),"Vanilla anvil calculation missing");
        var output=work.take(player,1);h.assertTrue(output.getDamageValue()<120&&work.durability==15&&player.experienceLevel==30-levels,"Manual anvil costs incorrect");
        work.durability=0;work.setItem(0,tool.copy());work.setItem(1,new ItemStack(Items.IRON_INGOT));
        h.assertTrue(!work.canTake(player)&&work.take(player,1).isEmpty(),"Exhausted anvil still works");
        h.assertTrue(h.getLevel().getBlockState(work.getBlockPos()).is(CrystalWorkstations.BLOCKS.get(2).get()),"Exhausted anvil disappeared");h.succeed();
    }
    @GameTest(template="spell_arena") public static void automaticAnvilAndPause(GameTestHelper h){
        var work=machine(h,5);var tool=new ItemStack(Items.IRON_PICKAXE);tool.setDamageValue(40);work.setItem(0,tool);work.setItem(1,new ItemStack(Items.IRON_INGOT));
        int duration=work.recipe().levels()*400;for(int i=0;i<100;i++)work.tick(true,128);
        int progress=work.progress;for(int i=0;i<20;i++)work.tick(false,0);h.assertTrue(work.progress==progress,"Power loss did not pause");
        for(int i=progress;i<duration;i++)work.tick(true,16);
        h.assertTrue(work.getItem(2).is(Items.IRON_PICKAXE)&&work.getItem(2).getDamageValue()==0&&work.durability==16,"Automatic forge timing or output incorrect");h.succeed();
    }
    @GameTest(template="spell_arena") public static void selectiveDisenchantingAndRepair(GameTestHelper h){
        var work=machine(h,4);var player=h.makeMockPlayer(GameType.SURVIVAL);player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(work.getBlockPos()));
        player.setData(ModAttachments.MANA,new com.mcmagic.omnira.mana.ManaState(1000,1000));
        var registry=h.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        var sharp=registry.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS);
        var curse=registry.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.VANISHING_CURSE);
        var sword=new ItemStack(Items.IRON_SWORD);sword.setDamageValue(10);sword.enchant(sharp,2);sword.enchant(curse,1);work.setItem(0,sword);
        h.assertTrue(work.preview().isEmpty(),"Default keep-all must not offer a redundant disenchantment");
        var menu=new CrystalWorkstationMenu(1,player.getInventory(),work);
        h.assertTrue(menu.configure(player,curse.getRegisteredName(),false),"Curse selection rejected");
        h.assertTrue(work.recipe().mana()==500,"Curse cost incorrect");
        for(int i=0;i<100;i++)work.tickDirect();h.assertTrue(work.getItem(0).getDamageValue()==9&&work.recipe().mana()==500,"Repair lost selection or used wrong rate");
        var result=work.take(player,1);
        h.assertTrue(result.getEnchantmentLevel(sharp)==2&&result.getEnchantmentLevel(curse)==0&&player.getData(ModAttachments.MANA).current()==500,"Selective removal did not preserve other enchantment");h.succeed();
    }
    @GameTest(template="spell_arena") public static void faceContractsAndPersistence(GameTestHelper h){
        for(int index:new int[]{3,5}){
            var work=machine(h,index);var block=(CrystalWorkstationBlock)work.getBlockState().getBlock();
            for(Direction face:Direction.values()){
                boolean expected=face==Direction.SOUTH||index==5&&face==Direction.NORTH;
                h.assertTrue(block.shaft(work.getBlockState(),face)==expected,"Wrong shaft face");
                h.assertTrue((h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK,work.getBlockPos(),face)!=null)==expected,"Wrong FE face");
            }
            work.energy=1000;work.tick(true,32);h.assertTrue(work.energy==1000&&!work.electrical,"Mechanical drive also spent FE");
        }
        var work=machine(h,2);work.durability=7;work.repairMana=120;
        var copy=new CrystalWorkstationEntity(work.getBlockPos(),work.getBlockState());copy.loadWithComponents(work.getUpdateTag(h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(copy.durability==7&&copy.repairMana==120,"Durability repair state lost on reload");h.succeed();
    }
    @GameTest(template="spell_arena") public static void sharedManaRepairBudget(GameTestHelper h){
        var work=machine(h,2);work.durability=15;
        var a=net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"repairA"));
        var b=net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"repairB"));
        for(var p:java.util.List.of(a,b)){p.setPos(net.minecraft.world.phys.Vec3.atCenterOf(work.getBlockPos()));p.setData(ModAttachments.MANA,new com.mcmagic.omnira.mana.ManaState(500,500));h.getLevel().players().add(p);}
        try{
            for(int i=0;i<20;i++)work.tickDirect();
            h.assertTrue(a.getData(ModAttachments.MANA).current()+b.getData(ModAttachments.MANA).current()==990&&work.repairMana==10,"Players multiplied repair rate");
            for(int i=20;i<400;i++)work.tickDirect();
            h.assertTrue(work.durability==16&&work.repairMana==0&&a.getData(ModAttachments.MANA).current()+b.getData(ModAttachments.MANA).current()==800,"Repair did not use exactly 200 mana");
        }finally{h.getLevel().players().remove(a);h.getLevel().players().remove(b);}h.succeed();
    }
}
