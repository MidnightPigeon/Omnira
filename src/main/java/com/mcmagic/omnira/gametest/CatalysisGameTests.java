package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.alchemy.catalysis.*;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_catalysis")
@PrefixGameTestTemplate(false)
public final class CatalysisGameTests {
    @GameTest(template="spell_arena") public static void debitMatchesDisplayAndHarvest(GameTestHelper h){
        var be=chamber(h);
        for(var seed:new Item[]{Items.WHEAT_SEEDS,Items.BEETROOT_SEEDS,Items.CARROT,Items.POTATO,Items.PITCHER_POD,Items.TORCHFLOWER_SEEDS,Items.COCOA_BEANS}){
            be.items.setStackInSlot(2,ItemStack.EMPTY);fill(be,seed==Items.COCOA_BEANS?Items.JUNGLE_LOG:Items.DIRT,seed,Fluids.WATER,400);
            h.assertTrue(be.plan()!=null&&be.plan().stages()==4,"Crop display is not four stages");
            for(int step=1;step<=3;step++){
                ticks(be,200);h.assertTrue(be.stage()==step+1&&be.tank.getFluidAmount()==400-100*step&&be.items.getStackInSlot(2).isEmpty(),"Debit/display mismatch for "+seed);
                var visual=be.plan().visual(be.stage());var age=CatalysisGrowth.age(visual);int expected;
                if(seed==Items.PITCHER_POD)expected=step;
                else if(seed==Items.TORCHFLOWER_SEEDS)expected=step/2;
                else if(seed==Items.COCOA_BEANS)expected=new int[]{0,0,1,2}[step];
                else expected=seed==Items.BEETROOT_SEEDS?step:new int[]{0,2,5,7}[step];
                h.assertTrue(visual.getValue(age)==expected,"Wrong native crop display age: "+seed);
                be.loadWithComponents(be.saveWithoutMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());
                h.assertTrue(be.stage()==step+1,"Reload changed completed display stage");
            }
            ticks(be,200);h.assertTrue(be.stage()==1&&be.tank.isEmpty()&&!be.items.getStackInSlot(2).isEmpty(),"Fourth debit did not harvest/reset");
            if(seed==Items.TORCHFLOWER_SEEDS)h.assertTrue(be.items.getStackInSlot(2).is(Items.TORCHFLOWER)&&be.items.getStackInSlot(2).getCount()==2,"Torchflower harvested immature seeds");
            if(seed==Items.PITCHER_POD)h.assertTrue(be.items.getStackInSlot(2).is(Items.PITCHER_PLANT)&&be.items.getStackInSlot(2).getCount()==2,"Pitcher harvested immature pods");
            for(int batch=0;batch<20;batch++){if(be.items.extractItem(2,64,false).isEmpty())break;ticks(be,1);}
        }
        for(var holder:h.getLevel().getRecipeManager().getAllRecipesFor(CatalysisContent.TYPE.get()))
            h.assertTrue(holder.value().fluid().getAmount()==100&&holder.value().stages()==4,"Data catalysis not normalized");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void fungiCatalysisMatchesSoilAndLiquid(GameTestHelper h){
        var be=chamber(h);
        var soils=new Item[]{Items.CRIMSON_NYLIUM,Items.WARPED_NYLIUM,Items.MYCELIUM,Items.MYCELIUM};
        var seeds=new Item[]{Items.CRIMSON_FUNGUS,Items.WARPED_FUNGUS,Items.RED_MUSHROOM,Items.BROWN_MUSHROOM};
        for(int i=0;i<seeds.length;i++){
            var fluid=i<2?Fluids.LAVA:Fluids.WATER;
            be.items.setStackInSlot(2,ItemStack.EMPTY);fill(be,soils[i],seeds[i],fluid,400);
            h.assertTrue(be.plan()!=null&&be.plan().stages()==4,"Missing fungal catalysis plan: "+seeds[i]);
            ticks(be,799);h.assertTrue(be.tank.getFluidAmount()==100&&be.items.getStackInSlot(2).isEmpty(),"Fungal harvest completed early");
            int expected=16;var product=seeds[i];
            if(i<2){
                product=i==0?Items.CRIMSON_STEM:Items.WARPED_STEM;
                h.getLevel().random.setSeed(42);
                var nativeDrops=CatalysisTreeGrowth.harvest(h.getLevel(),Block.byItem(seeds[i]).defaultBlockState(),Block.byItem(soils[i]).defaultBlockState());
                var stem=product;
                h.assertTrue(!nativeDrops.isEmpty()&&nativeDrops.stream().allMatch(s->s.is(stem)),"Native fungus simulation produced no stems or extra items");
                expected=nativeDrops.stream().mapToInt(ItemStack::getCount).sum()*2;
                h.getLevel().random.setSeed(42);
            }
            ticks(be,1);h.assertTrue(be.tank.isEmpty(),"Wrong fungal liquid cost");
            int received=0;
            for(int batch=0;batch<20;batch++){
                var output=be.items.extractItem(2,64,false);if(output.isEmpty())break;
                h.assertTrue(output.is(product),"Wrong fungal output");received+=output.getCount();ticks(be,1);
            }
            h.assertTrue(received==expected,"Fungal yield did not follow configured rule: "+received+" / "+expected);
            h.assertTrue(be.items.getStackInSlot(0).getCount()==1&&be.items.getStackInSlot(1).getCount()==1,"Fungal growth consumed substrate or seed");
            h.assertTrue(CatalysisGrowth.resolve(h.getLevel(),new ItemStack(soils[i]),new ItemStack(seeds[i]),new FluidStack(i<2?Fluids.WATER:Fluids.LAVA,1000))==null,"Wrong liquid accepted");
            h.assertTrue(CatalysisGrowth.resolve(h.getLevel(),new ItemStack(Items.NETHERRACK),new ItemStack(seeds[i]),new FluidStack(fluid,1000))==null,"Plain netherrack substituted for fungal substrate");
        }
        h.assertTrue(CatalysisGrowth.resolve(h.getLevel(),new ItemStack(Items.CRIMSON_NYLIUM),new ItemStack(Items.WARPED_FUNGUS),new FluidStack(Fluids.LAVA,1000))==null,"Mismatched nether fungus accepted");
        h.assertTrue(CatalysisGrowth.resolve(h.getLevel(),new ItemStack(Items.WARPED_NYLIUM),new ItemStack(Items.CRIMSON_FUNGUS),new FluidStack(Fluids.LAVA,1000))==null,"Mismatched crimson fungus accepted");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void everyCropUsesFourCatalyses(GameTestHelper h){
        var original=chamber(h);
        for(var seed:new Item[]{Items.WHEAT_SEEDS,Items.BEETROOT_SEEDS,Items.CARROT,Items.POTATO,Items.SWEET_BERRIES,Items.PUMPKIN_SEEDS,Items.MELON_SEEDS}){
            var be=new CatalysisBlockEntity(original.getBlockPos(),original.getBlockState());be.setLevel(h.getLevel());
            be.items.setStackInSlot(2,ItemStack.EMPTY);fill(be,Items.DIRT,seed,Fluids.WATER,400);
            ticks(be,399);h.assertTrue(be.tank.getFluidAmount()==300&&be.items.getStackInSlot(2).isEmpty(),"Second debit was early for "+seed+": "+be.tank.getFluidAmount());
            be.loadWithComponents(be.saveWithoutMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());
            ticks(be,400);h.assertTrue(be.tank.getFluidAmount()==100&&be.items.getStackInSlot(2).isEmpty(),"Premature harvest for "+seed);
            ticks(be,1);h.assertTrue(be.tank.isEmpty()&&!be.items.getStackInSlot(2).isEmpty(),"Four catalyses failed for "+seed);
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void rearManualInputPrefersImportAndRejectsConflict(GameTestHelper h){
        var be=chamber(h);var level=h.getLevel();var pos=be.getBlockPos();var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        for(var facing:Direction.Plane.HORIZONTAL){
            for(int i=0;i<3;i++)level.setBlockAndUpdate(pos.above(i),level.getBlockState(pos.above(i)).setValue(CatalysisBlock.FACING,facing));
            be.tank.setFluid(FluidStack.EMPTY);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.WATER_BUCKET));
            clickPort(be,player,facing.getClockWise());h.assertTrue(be.tank.isEmpty()&&player.getMainHandItem().is(Items.WATER_BUCKET),"Non-port accepted liquid");
            clickPort(be,player,facing.getOpposite());h.assertTrue(be.tank.getFluidAmount()==1000&&player.getMainHandItem().is(Items.BUCKET),"Rear bucket input or return failed");
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.LAVA_BUCKET));
            clickPort(be,player,facing.getOpposite());h.assertTrue(be.tank.getFluidAmount()==1000&&be.tank.getFluid().is(Fluids.WATER)&&player.getMainHandItem().is(Items.LAVA_BUCKET),"Mixed incompatible fluids");
        }
        var orbPos=pos.offset(4,0,0);level.setBlockAndUpdate(orbPos,ModBlocks.LIQUID_CRYSTAL_BALL.get().defaultBlockState());
        var orb=(com.mcmagic.omnira.block.entity.LiquidCrystalBallBlockEntity)level.getBlockEntity(orbPos);
        orb.upgrades.insertItem(0,new ItemStack(ModItems.SPACETIME_STABILIZATION_UPGRADE.get()),false);orb.tank.setFluid(new FluidStack(Fluids.WATER,5000));
        var portable=new ItemStack(ModItems.LIQUID_CRYSTAL_BALL.get());portable.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.CustomData.of(orb.saveWithFullMetadata(level.registryAccess())));
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,portable);
        var rear=be.getBlockState().getValue(CatalysisBlock.FACING).getOpposite();clickPort(be,player,rear);
        h.assertTrue(be.tank.getFluidAmount()==4000&&player.getMainHandItem().getCapability(Capabilities.FluidHandler.ITEM).getFluidInTank(0).getAmount()==2000,"Portable input drained backwards or overfilled");
        clickPort(be,player,rear);h.assertTrue(be.tank.getFluidAmount()==4000,"Full tank exported into a filled input container");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.BUCKET));clickPort(be,player,rear);
        h.assertTrue(be.tank.getFluidAmount()==3000&&player.getMainHandItem().is(Items.WATER_BUCKET),"Empty bucket cannot withdraw");h.succeed();
    }
    private static void clickPort(CatalysisBlockEntity be,net.minecraft.world.entity.player.Player player,Direction side){
        var pos=be.getBlockPos();be.getLevel().getBlockState(pos).useItemOn(player.getMainHandItem(),be.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(pos.getCenter(),side,pos,false));
    }
    private static net.minecraft.util.RandomSource successfulGrowth(){return new net.minecraft.world.level.levelgen.LegacyRandomSource(1){
        @Override public int nextInt(int bound){return bound==6||bound==3?1:0;}
        @Override public double nextDouble(){return 0;}
        @Override public float nextFloat(){return 0;}
    };}
    @GameTest(template="spell_arena") public static void coreAnimationFollowsCatalyticWork(GameTestHelper h){
        var be=chamber(h);
        be.items.setStackInSlot(0,new ItemStack(Items.DIRT));be.items.setStackInSlot(1,new ItemStack(Items.WHEAT_SEEDS));
        h.assertTrue(!be.catalyzing(),"Dry natural growth powers the alchemy core");
        be.tank.fill(new FluidStack(Fluids.WATER,99),FluidAction.EXECUTE);
        h.assertTrue(!be.catalyzing(),"Insufficient liquid powers the core");
        be.tank.fill(new FluidStack(Fluids.WATER,1),FluidAction.EXECUTE);
        h.assertTrue(be.catalyzing(),"Valid catalytic growth does not power the core");
        be.items.setStackInSlot(2,new ItemStack(Items.WHEAT));
        h.assertTrue(!be.catalyzing(),"Blocked output keeps the core working");
        be.items.setStackInSlot(2,ItemStack.EMPTY);
        be.items.setStackInSlot(1,new ItemStack(Items.COBBLESTONE));
        h.assertTrue(!be.catalyzing(),"Invalid recipe powers the core");h.succeed();
    }
    @GameTest(template="spell_arena") public static void dryGrowthUsesRandomTicksAndNormalYield(GameTestHelper h){
        var be=chamber(h);h.getLevel().setDayTime(6000);
        be.items.setStackInSlot(0,new ItemStack(Items.DIRT));be.items.setStackInSlot(1,new ItemStack(Items.WHEAT_SEEDS));
        ticks(be,2000);h.assertTrue(be.stage()==1&&be.items.getStackInSlot(2).isEmpty(),"Dry chamber retained ten-second acceleration");
        var random=successfulGrowth();for(int i=0;i<7;i++)be.naturalTick(random);
        h.assertTrue(be.items.getStackInSlot(2).is(Items.WHEAT)&&be.items.getStackInSlot(2).getCount()==1,"Dry wheat yield is not native");
        h.assertTrue(be.tank.isEmpty()&&be.items.getStackInSlot(1).getCount()==1,"Dry growth consumed seed or made fluid");
        h.assertTrue(be.getBlockState().isRandomlyTicking()&&!h.getLevel().getBlockState(be.getBlockPos().above()).isRandomlyTicking(),"Multiple parts tick growth");h.succeed();
    }
    @GameTest(template="spell_arena") public static void naturalSpecialYieldIsNotHalfCatalystYield(GameTestHelper h){
        var be=chamber(h);be.items.setStackInSlot(0,new ItemStack(Items.JUNGLE_LOG));be.items.setStackInSlot(1,new ItemStack(Items.COCOA_BEANS));
        be.naturalTick(successfulGrowth());be.naturalTick(successfulGrowth());
        h.assertTrue(be.items.getStackInSlot(2).is(Items.COCOA_BEANS)&&be.items.getStackInSlot(2).getCount()==3,"Natural cocoa must yield 3, not 4 or 8");
        be.items.setStackInSlot(2,ItemStack.EMPTY);be.items.setStackInSlot(0,new ItemStack(TimeNatureContent.SPATIAL_MATRIX.get()));be.items.setStackInSlot(1,new ItemStack(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get()));
        for(int i=0;i<3;i++)be.naturalTick(successfulGrowth());h.assertTrue(be.items.getStackInSlot(2).isEmpty(),"Matrix skipped a native growth stage");
        be.naturalTick(successfulGrowth());h.assertTrue(be.items.getStackInSlot(2).is(TimeNatureContent.SPATIAL_SHARD.get())&&be.items.getStackInSlot(2).getCount()==4,"Native crystal harvest changed");h.succeed();
    }
    @GameTest(template="spell_arena") public static void dryGrowthCannotSynthesizeMaterials(GameTestHelper h){
        var level=h.getLevel();
        Item[][] pairs={{ModBlocks.SPACETIME_SPELL_CORE.get().asItem(),ModItems.TIME_WARP_POINT.get()},
                {DreamContent.EXCITED_SPATIAL_CRYSTAL.get().asItem(),Items.DIAMOND},
                {DreamContent.LIVING_TEMPORAL_SILT.get().asItem(),TimeNatureContent.FLOWER.get().asItem()},
                {Items.DIRT,Items.AZALEA},{Items.SAND,Items.KELP}};
        for(var pair:pairs)h.assertTrue(CatalysisNaturalGrowth.resolve(level,new ItemStack(pair[0]),new ItemStack(pair[1]))==null,"Invented natural recipe for "+pair[1]);
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void dryProgressSurvivesReloadAndRefill(GameTestHelper h){
        var be=chamber(h);h.getLevel().setDayTime(6000);be.items.setStackInSlot(0,new ItemStack(Items.DIRT));be.items.setStackInSlot(1,new ItemStack(Items.WHEAT_SEEDS));
        be.naturalTick(successfulGrowth());h.assertTrue(be.stage()==2,"Natural wheat failed to advance");
        be.loadWithComponents(be.saveWithoutMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());
        be.tank.fill(new FluidStack(Fluids.WATER,4000),FluidAction.EXECUTE);ticks(be,800);
        h.assertTrue(be.items.getStackInSlot(2).is(Items.WHEAT)&&be.items.getStackInSlot(2).getCount()==1,"Refill lost progress or doubled a partly natural crop");
        be.items.extractItem(2,64,false);be.tank.fill(new FluidStack(Fluids.WATER,4000),FluidAction.EXECUTE);ticks(be,800);
        h.assertTrue(be.items.getStackInSlot(2).getCount()==2,"Next fully catalyzed crop did not regain doubled yield");h.succeed();
    }
    @GameTest(template="spell_arena") public static void smallLiquidRemainderIsNotDryMode(GameTestHelper h){
        var be=chamber(h);h.getLevel().setDayTime(6000);fill(be,Items.DIRT,Items.WHEAT_SEEDS,Fluids.WATER,99);
        for(int i=0;i<30;i++)be.naturalTick(successfulGrowth());ticks(be,2000);
        h.assertTrue(be.stage()==1&&be.tank.getFluidAmount()==99&&be.items.getStackInSlot(2).isEmpty(),"Partial liquid got free growth");h.succeed();
    }
    @GameTest(template="spell_arena") public static void naturalFruitFollowsMatureStem(GameTestHelper h){
        var be=chamber(h);h.getLevel().setDayTime(6000);be.items.setStackInSlot(0,new ItemStack(Items.DIRT));be.items.setStackInSlot(1,new ItemStack(Items.PUMPKIN_SEEDS));
        for(int i=0;i<7;i++)be.naturalTick(successfulGrowth());
        h.assertTrue(be.stemReady()&&be.items.getStackInSlot(2).isEmpty(),"Native stem maturity incorrect");
        be.naturalTick(successfulGrowth());h.assertTrue(be.items.getStackInSlot(2).is(Items.PUMPKIN)&&be.items.getStackInSlot(2).getCount()==1,"Natural fruit reused catalyzed stages or yield");
        be.items.extractItem(2,64,false);be.naturalTick(successfulGrowth());h.assertTrue(be.items.getStackInSlot(2).getCount()==1,"Natural harvest reset mature stem");h.succeed();
    }
    private static CatalysisBlockEntity chamber(GameTestHelper h){
        var p=h.absolutePos(new BlockPos(4,2,4));h.assertTrue(CatalysisBlock.place(h.getLevel(),p,Direction.NORTH),"Failed to place chamber");return CatalysisBlock.controller(h.getLevel(),p);
    }
    private static void fill(CatalysisBlockEntity be,Item soil,Item seed,net.minecraft.world.level.material.Fluid fluid,int amount){
        be.items.setStackInSlot(0,new ItemStack(soil));be.items.setStackInSlot(1,new ItemStack(seed));be.tank.setFluid(new FluidStack(fluid,amount));
    }
    private static void ticks(CatalysisBlockEntity be,int count){for(int i=0;i<count;i++)be.tick();}
    @GameTest(template="spell_arena") public static void assemblyAndBottle(GameTestHelper h){
        var level=h.getLevel();var p=h.absolutePos(new BlockPos(4,2,4));var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        for(int i=0;i<3;i++)level.setBlockAndUpdate(p.above(i),CatalysisStructure.required(i).defaultBlockState());
        var crystal=new ItemStack(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get(),2);
        var ball=(com.mcmagic.omnira.block.entity.CrystalBallBlockEntity)level.getBlockEntity(p.above(2));ball.setItem(0,new ItemStack(Items.DIAMOND));
        h.assertTrue(!CatalysisStructure.activate(level,p.above(),player,crystal)&&crystal.getCount()==2,"Lost occupied component");ball.clearContent();
        h.assertTrue(CatalysisStructure.activate(level,p.above(),player,crystal)&&crystal.getCount()==1,"Activation failed");
        var be=CatalysisBlock.controller(level,p);h.assertTrue(be.complete()&&level.getBlockState(p).getDestroySpeed(level,p)<0,"Not complete or mineable");
        fill(be,Items.DIRT,Items.WHEAT_SEEDS,Fluids.WATER,4000);ticks(be,225);
        var bottle=new ItemStack(ModItems.POCKET_MAGIC_BOTTLE.get());
        h.assertTrue(com.mcmagic.omnira.item.bottle.MultiblockBottleCapture.capture(player,p.above(2),bottle),"Capture failed");
        var captured=com.mcmagic.omnira.item.bottle.BottleStorage.get(level).get(bottle.get(ModDataComponents.BOTTLE_CAPTURE));
        h.assertTrue(com.mcmagic.omnira.item.bottle.MultiblockBottleCapture.release(level,captured,net.minecraft.world.phys.Vec3.atLowerCornerOf(p),player),"Release failed");
        var restored=CatalysisBlock.controller(level,p);h.assertTrue(restored.complete()&&restored.stage()==2&&restored.tank.getFluidAmount()==3900,"Bottle lost progress/fluid");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void portsAndResonance(GameTestHelper h){
        var be=chamber(h);var level=h.getLevel();var p=be.getBlockPos();
        for(var front:Direction.Plane.HORIZONTAL){
            for(int layer=0;layer<3;layer++)level.setBlockAndUpdate(p.above(layer),level.getBlockState(p.above(layer)).setValue(CatalysisBlock.FACING,front));
            for(int layer=0;layer<3;layer++)for(var side:Direction.values()){
                var f=level.getCapability(Capabilities.FluidHandler.BLOCK,p.above(layer),side);
                h.assertTrue((f!=null)==(layer==0&&side==front.getOpposite()),"Fluid exposed outside rear connector");
            }
        }
        be=CatalysisBlock.controller(level,p);
        be.items.setStackInSlot(0,new ItemStack(Items.DIRT));be.items.setStackInSlot(1,new ItemStack(Items.CARROT));be.items.setStackInSlot(2,new ItemStack(Items.CARROT,2));
        level.setBlockAndUpdate(p.above(3),ModBlocks.RESONANCE_CORE.get().defaultBlockState());
        var node=(com.mcmagic.omnira.block.entity.ResonanceCoreBlockEntity)level.getBlockEntity(p.above(3));
        h.assertTrue(node.items().getSlots()==1&&node.items().extractItem(0,1,true).is(Items.CARROT),"Resonance cannot extract product");
        h.assertTrue(node.items().insertItem(0,new ItemStack(Items.DIAMOND),false).getCount()==1,"Resonance inserted an item");
        h.assertTrue(node.fluids().fill(new FluidStack(Fluids.WATER,5000),FluidAction.EXECUTE)==4000,"Resonance extra fluid input/capacity broken");
        h.assertTrue(node.fluids().drain(500,FluidAction.EXECUTE).isEmpty(),"Resonance extracted liquid instead of products");
        var upper=level.getCapability(Capabilities.ItemHandler.BLOCK,p.above(2),Direction.DOWN);
        h.assertTrue(upper.extractItem(0,1,true).is(Items.DIRT)&&upper.insertItem(0,new ItemStack(Items.DIRT),false).getCount()==1,"Upper extraction-only handler broken");
        h.assertTrue(com.mcmagic.omnira.storage.StorageContainerIdentity.key(level,p).equals(com.mcmagic.omnira.storage.StorageContainerIdentity.key(level,p.above(2))),"Parts have distinct identities");h.succeed();
    }
    @GameTest(template="spell_arena") public static void wheatGrowthAndRetainedIngredients(GameTestHelper h){
        var be=chamber(h);fill(be,Items.DIRT,Items.WHEAT_SEEDS,Fluids.WATER,4000);
        ticks(be,799);h.assertTrue(be.items.getStackInSlot(2).isEmpty()&&be.tank.getFluidAmount()==3700,"Early maturity or wrong stage costs");
        ticks(be,1);h.assertTrue(be.items.getStackInSlot(2).is(Items.WHEAT)&&be.items.getStackInSlot(2).getCount()==2&&be.tank.getFluidAmount()==3600,"Wheat yield/consumption wrong");
        h.assertTrue(be.items.getStackInSlot(0).getCount()==1&&be.items.getStackInSlot(1).getCount()==1&&be.stage()==1,"Consumed substrate/seed or failed to reset");
        ticks(be,400);h.assertTrue(be.tank.getFluidAmount()==3600,"Occupied output consumed fluid");h.succeed();
    }
    @GameTest(template="spell_arena") public static void shortageAndReload(GameTestHelper h){
        var be=chamber(h);fill(be,Items.DIRT,Items.WHEAT_SEEDS,Fluids.WATER,100);ticks(be,200);ticks(be,300);
        h.assertTrue(be.tank.isEmpty()&&be.plan().visual(be.stage()).getValue(CropBlock.AGE)==2,"Empty tank discarded growth");
        var tag=be.saveWithoutMetadata(h.getLevel().registryAccess());be.loadWithComponents(tag,h.getLevel().registryAccess());
        be.tank.fill(new FluidStack(Fluids.WATER,100),FluidAction.EXECUTE);ticks(be,200);
        h.assertTrue(be.stage()==3&&be.tank.isEmpty(),"Reload/refill reset progress");
        be.items.setStackInSlot(1,new ItemStack(Items.CARROT));h.assertTrue(be.stage()==1,"Changed seed inherited growth");h.succeed();
    }
    @GameTest(template="spell_arena") public static void specialRecipes(GameTestHelper h){
        var level=h.getLevel();var be=chamber(h);
        Item[][] cases={{Items.JUNGLE_LOG,Items.COCOA_BEANS},{Items.VINE,Items.GLOW_BERRIES},{Items.DIRT,Items.SUGAR_CANE},{Items.SAND,Items.SUGAR_CANE},
                {Items.GRAVEL,Items.KELP},{Items.SAND,Items.KELP},{Items.STONE,Items.POINTED_DRIPSTONE},{Items.DIRT,Items.BAMBOO}};
        for(var pair:cases){be.items.setStackInSlot(2,ItemStack.EMPTY);fill(be,pair[0],pair[1],Fluids.WATER,4000);var plan=be.plan();h.assertTrue(plan!=null,"Missing special recipe "+pair[1]);
            ticks(be,800);h.assertTrue(be.items.getStackInSlot(2).is(pair[1])&&be.items.getStackInSlot(2).getCount()==8,"Special yield is not 8: "+pair[1]);}
        fill(be,DreamContent.EXCITED_SPATIAL_CRYSTAL.get().asItem(),Items.DIAMOND,com.mcmagic.omnira.space.sea.VoidSeaContent.SOURCE.get(),4000);
        h.assertTrue(be.plan()!=null&&be.plan().product().is(Items.DIAMOND_ORE),"Mineral tag -> ore rule failed");
        h.assertTrue(CatalysisGrowth.ore(new ItemStack(Items.IRON_INGOT)).is(Items.IRON_ORE),"Iron ore mapping failed");h.succeed();
    }
    @GameTest(template="spell_arena") public static void cropsDoubleDropsAndExcludeOnlySeparateSeeds(GameTestHelper h){
        var level=h.getLevel();var p=h.absolutePos(new BlockPos(4,2,4));
        for(var seed:new Item[]{Items.WHEAT_SEEDS,Items.BEETROOT_SEEDS,Items.CARROT,Items.POTATO,Items.SWEET_BERRIES,Items.TORCHFLOWER_SEEDS,Items.PITCHER_POD}){
            var s=new ItemStack(seed);var plan=CatalysisGrowth.resolve(level,new ItemStack(Items.DIRT),s,new FluidStack(Fluids.WATER,500));
            h.assertTrue(plan!=null,"Missing crop "+seed);var drops=plan.harvest(level,p,new ItemStack(Items.DIRT),s);
            h.assertTrue(!drops.isEmpty()&&drops.stream().allMatch(d->d.getCount()%2==0),"Not doubled loot for "+seed);
            if(seed==Items.WHEAT_SEEDS||seed==Items.BEETROOT_SEEDS)h.assertTrue(drops.stream().noneMatch(d->d.is(seed)),"Leaked separate seed");
            if(seed==Items.CARROT||seed==Items.POTATO)h.assertTrue(drops.stream().anyMatch(d->d.is(seed)),"Removed edible seed produce");
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void shadowTreeCatalysis(GameTestHelper h){
        var be=chamber(h);var seed=com.mcmagic.omnira.registry.ShadowWoodContent.SAPLING.get().asItem();
        for(var soil:new Block[]{DreamContent.SHADOW_ROCK.get(),DreamContent.MOSSY_SHADOW_ROCK.get()}){
            be.items.setStackInSlot(2,ItemStack.EMPTY);fill(be,soil.asItem(),seed,Fluids.WATER,4000);
            h.assertTrue(be.plan()!=null&&be.plan().yield()==CatalysisGrowth.Yield.TREE,"Missing shadow tree plan");
            ticks(be,800);
            h.assertTrue(be.items.getStackInSlot(2).is(DreamContent.SHADOW_LOG.get().asItem())&&be.items.getStackInSlot(2).getCount()>=6,"Missing actual shadow trunk yield");
            h.assertTrue(be.tank.getFluidAmount()==3600,"Shadow tree water cost");
            h.assertTrue(be.items.getStackInSlot(0).is(soil.asItem())&&be.items.getStackInSlot(1).is(seed),"Consumed catalysts");
            h.assertTrue(CatalysisNaturalGrowth.resolve(h.getLevel(),new ItemStack(soil),new ItemStack(seed))!=null,"Missing natural shadow growth");
        }
        h.assertTrue(CatalysisGrowth.resolve(h.getLevel(),new ItemStack(Items.STONE),new ItemStack(seed),new FluidStack(Fluids.WATER,500))==null,"Wrong shadow substrate accepted");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void isolatedTreeYield(GameTestHelper h){
        var level=h.getLevel();var guard=h.absolutePos(new BlockPos(3,3,3));level.setBlockAndUpdate(guard,Blocks.GOLD_BLOCK.defaultBlockState());
        for(var sapling:new Block[]{Blocks.OAK_SAPLING,Blocks.BIRCH_SAPLING,Blocks.SPRUCE_SAPLING,Blocks.JUNGLE_SAPLING,Blocks.ACACIA_SAPLING,Blocks.DARK_OAK_SAPLING,
                Blocks.MANGROVE_PROPAGULE,Blocks.CHERRY_SAPLING,Blocks.AZALEA,com.mcmagic.omnira.registry.TimeNatureContent.SAPLING.get()}){
            java.util.List<ItemStack> drops=java.util.List.of();
            for(int retry=0;retry<4&&drops.isEmpty();retry++)drops=CatalysisTreeGrowth.harvest(level,sapling.defaultBlockState(),sapling==TimeNatureContent.SAPLING.get()?DreamContent.TEMPORAL_SILT.get().defaultBlockState():Blocks.DIRT.defaultBlockState());
            h.assertTrue(!drops.isEmpty(),"Tree simulation failed: "+sapling);
            h.assertTrue(drops.stream().allMatch(s->Block.byItem(s.getItem()).defaultBlockState().is(net.minecraft.tags.BlockTags.LOGS)),"Non-log tree output");
            h.assertTrue(drops.stream().mapToInt(ItemStack::getCount).sum()>0,"Tree yield is empty: "+sapling);
        }
        h.assertTrue(level.getBlockState(guard).is(Blocks.GOLD_BLOCK),"Simulation changed real world");h.succeed();
    }
    @GameTest(template="spell_arena") public static void crystalMatricesAndReactions(GameTestHelper h){
        var level=h.getLevel();var be=chamber(h);
        var matrices=new Block[]{Blocks.BUDDING_AMETHYST,DreamContent.DREAM_CRYSTAL_BEDROCK.get(),TimeNatureContent.SPATIAL_MATRIX.get(),com.mcmagic.omnira.space.geodes.DeflectionContent.MATRIX.get()};
        var crystals=new Item[]{Items.AMETHYST_SHARD,ModItems.DREAM_CRYSTAL_SHARD.get(),TimeNatureContent.SPATIAL_SHARD.get(),com.mcmagic.omnira.space.geodes.DeflectionContent.CRYSTAL.get()};
        for(int i=0;i<matrices.length;i++){
            var matrix=matrices[i];
            be.items.setStackInSlot(2,ItemStack.EMPTY);fill(be,matrix.asItem(),ModItems.INFUSED_SPIRITUAL_CRYSTAL.get(),com.mcmagic.omnira.alchemy.AlchemyMaterials.DILUTE.source.get(),4000);
            h.assertTrue(be.plan()!=null,"Missing matrix recipe "+matrix);
            h.assertTrue(!be.plan().visual(1).equals(be.plan().visual(4)),"Crystal growth stages have identical models");
            ticks(be,800);h.assertTrue(be.items.getStackInSlot(2).is(crystals[i])&&be.items.getStackInSlot(2).getCount()==4,"Expected four crystal materials, not clusters");
        }
        long count=level.getRecipeManager().getAllRecipesFor(ModRecipes.REACTION_TYPE.get()).stream().map(net.minecraft.world.item.crafting.RecipeHolder::value)
                .filter(r->r.fluid().getFluid()==com.mcmagic.omnira.alchemy.AlchemyMaterials.REFINED.source.get()&&r.fluid().getAmount()==500).count();
        h.assertTrue(count==4,"Expected four crystal matrix reactions");h.succeed();
    }
    @GameTest(template="spell_arena") public static void stemsMatureOnceThenFourStageFruit(GameTestHelper h){
        var be=chamber(h);fill(be,Items.DIRT,Items.PUMPKIN_SEEDS,Fluids.WATER,4000);ticks(be,799);
        h.assertTrue(!be.stemReady()&&be.items.getStackInSlot(2).isEmpty(),"Fruit matured before four catalyses");
        ticks(be,1);
        h.assertTrue(be.items.getStackInSlot(2).is(Items.PUMPKIN)&&be.items.getStackInSlot(2).getCount()==2,"Four-stage pumpkin yield not doubled");
        h.assertTrue(be.stemReady()&&be.stemStage()==8&&be.tank.getFluidAmount()==3600,"Stem or first cycle cost incorrect");
        be.items.extractItem(2,64,false);ticks(be,800);
        h.assertTrue(be.items.getStackInSlot(2).getCount()==2&&be.stemReady(),"Repeated harvest regrew the stem");
        var seed=new ItemStack(Items.MELON_SEEDS);var plan=CatalysisGrowth.resolve(h.getLevel(),new ItemStack(Items.DIRT),seed,new FluidStack(Fluids.WATER,500));
        h.assertTrue(plan.stages()==4,"Melon did not use four fruit stages");
        for(int i=0;i<24;i++){var drops=plan.harvest(h.getLevel(),be.getBlockPos(),new ItemStack(Items.DIRT),seed);
            h.assertTrue(drops.size()==1&&drops.getFirst().is(Items.MELON_SLICE)&&drops.getFirst().getCount()>=6&&drops.getFirst().getCount()<=14&&drops.getFirst().getCount()%2==0,"Melon is not one normal broken melon doubled");}
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void potatoRareDropsKeepPotatoes(GameTestHelper h){
        var drops=CatalysisGrowth.doubledProducts(java.util.List.of(new ItemStack(Items.POTATO,4),new ItemStack(Items.POISONOUS_POTATO)),new ItemStack(Items.POTATO));
        h.assertTrue(drops.size()==2&&drops.get(0).is(Items.POTATO)&&drops.get(0).getCount()==8&&drops.get(1).getCount()==2,"Rare poison drop filtered edible potatoes");h.succeed();
    }
    @GameTest(template="spell_arena") public static void upgradeLayersAndFilter(GameTestHelper h){
        var be=chamber(h);var level=h.getLevel();var p=be.getBlockPos();
        level.setBlockAndUpdate(p.north(),Blocks.CHEST.defaultBlockState());level.setBlockAndUpdate(p.above().south(),Blocks.CHEST.defaultBlockState());
        var source=(net.minecraft.world.level.block.entity.ChestBlockEntity)level.getBlockEntity(p.north());source.setItem(0,new ItemStack(Items.DIRT,2));
        var target=(net.minecraft.world.level.block.entity.ChestBlockEntity)level.getBlockEntity(p.above().south());
        be.upgrades.setStackInSlot(0,new ItemStack(ModItems.INTAKE_UPGRADE.get()));
        var plate=new ItemStack(ModItems.OUTPUT_UPGRADE.get());com.mcmagic.omnira.block.entity.OrbUpgrades.setFilter(plate,0,new ItemStack(Items.WHEAT),level.registryAccess());
        var tag=com.mcmagic.omnira.block.entity.OrbUpgrades.settings(plate);tag.putBoolean("Whitelist",true);tag.putInt("Directions",1<<3);
        plate.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));be.upgrades.setStackInSlot(1,plate);
        be.items.setStackInSlot(2,new ItemStack(Items.WHEAT,2));CatalysisAutomation.tick(be);
        h.assertTrue(be.items.getStackInSlot(0).is(Items.DIRT)&&source.getItem(0).getCount()==1,"Intake did not scan first layer");
        h.assertTrue(target.getItem(0).is(Items.WHEAT)&&target.getItem(0).getCount()==1&&be.items.getStackInSlot(0).getCount()==1,"Output layer/filter failed");h.succeed();
    }
    @GameTest(template="spell_arena") public static void intakeFluidOnlyAtRear(GameTestHelper h){
        var be=chamber(h);var level=h.getLevel();var p=be.getBlockPos();var plate=new ItemStack(ModItems.INTAKE_UPGRADE.get());
        var tag=new net.minecraft.nbt.CompoundTag();tag.putInt("Directions",63);plate.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));
        be.upgrades.setStackInSlot(0,plate);
        level.setBlockAndUpdate(p.east(),ModBlocks.LIQUID_CRYSTAL_BALL.get().defaultBlockState());
        var wrong=(com.mcmagic.omnira.block.entity.LiquidCrystalBallBlockEntity)level.getBlockEntity(p.east());wrong.tank.setFluid(new FluidStack(Fluids.WATER,1000));
        CatalysisAutomation.tick(be);h.assertTrue(be.tank.isEmpty(),"Intake stole liquid from a non-port side");
        level.setBlockAndUpdate(p.south(),ModBlocks.LIQUID_CRYSTAL_BALL.get().defaultBlockState());
        var right=(com.mcmagic.omnira.block.entity.LiquidCrystalBallBlockEntity)level.getBlockEntity(p.south());right.tank.setFluid(new FluidStack(Fluids.WATER,1000));
        be.upgrades.setStackInSlot(0,ItemStack.EMPTY);
        h.runAfterDelay(21,()->{
            be.upgrades.setStackInSlot(0,plate);CatalysisAutomation.tick(be);
            h.assertTrue(be.tank.getFluidAmount()==500,"Intake did not use rear port");h.succeed();
        });
    }
}
