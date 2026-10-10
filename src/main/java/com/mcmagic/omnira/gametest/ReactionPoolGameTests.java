package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.alchemy.*;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.block.entity.OrbUpgrades;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.*;
import java.util.List;

@GameTestHolder("omnira_alchemy")
@PrefixGameTestTemplate(false)
public final class ReactionPoolGameTests {
    @GameTest(template="spell_arena") public static void waymarkReaction(GameTestHelper h){
        var pool=pool(h);var level=h.getLevel();
        var recipe=level.getRecipeManager().getAllRecipesFor(ModRecipes.REACTION_TYPE.get()).stream()
                .filter(r->r.id().equals(net.minecraft.resources.ResourceLocation.parse("omnira:reaction/spacetime_waymark"))).findFirst().orElseThrow().value();
        var fluid=net.minecraft.core.registries.BuiltInRegistries.FLUID.get(net.minecraft.resources.ResourceLocation.parse("omnira:dilute_spacetime"));
        pool.manualFluid.fill(new FluidStack(fluid,1000),EXECUTE);
        pool.items.setStackInSlot(0,new ItemStack(ModItems.SPIRITUAL_CRYSTAL.get(),3));
        h.assertTrue(!pool.start(recipe)&&pool.items.getStackInSlot(0).getCount()==3&&pool.fluids.total()==1000,"Insufficient crystals started or consumed materials");
        pool.items.setStackInSlot(0,new ItemStack(ModItems.SPIRITUAL_CRYSTAL.get(),4));
        h.assertTrue(recipe.support().isEmpty()&&recipe.duration()==2000&&pool.start(recipe),"Waymark reaction requires a catalyst or has wrong duration");
        h.assertTrue(pool.items.getStackInSlot(0).isEmpty(),"Four crystals were not consumed");
        pool.advance(200);h.assertTrue(pool.fluids.total()==900,"Wrong waymark fluid installment");pool.advance(1800);
        var output=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("omnira:spacetime_waymark"));
        h.assertTrue(pool.fluids.total()==0&&!pool.reacting()&&java.util.stream.IntStream.range(0,9)
                .map(i->pool.items.getStackInSlot(i).is(output)?pool.items.getStackInSlot(i).getCount():0).sum()==1,"Wrong waymark yield or fluid cost");h.succeed();
    }
    @GameTest(template="spell_arena") public static void pocketBottleReaction(GameTestHelper h){
        var pool=pool(h);var level=h.getLevel();
        var recipe=level.getRecipeManager().getAllRecipesFor(ModRecipes.REACTION_TYPE.get()).stream()
                .filter(r->r.id().equals(net.minecraft.resources.ResourceLocation.parse("omnira:reaction/pocket_magic_bottle"))).findFirst().orElseThrow().value();
        pool.items.setStackInSlot(0,new ItemStack(Items.GLASS_BOTTLE));
        pool.items.setStackInSlot(1,new ItemStack(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get()));
        var fluid=net.minecraft.core.registries.BuiltInRegistries.FLUID.get(net.minecraft.resources.ResourceLocation.parse("omnira:dilute_spacetime"));
        pool.manualFluid.fill(new FluidStack(fluid,1000),EXECUTE);
        h.assertTrue(!pool.start(recipe)&&pool.items.getStackInSlot(0).getCount()==1,"Missing catalyst consumed materials");
        var catalyst=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("omnira:infused_crystal_block"));
        var at=pool.getBlockPos().below();level.setBlockAndUpdate(at,catalyst.defaultBlockState());
        h.assertTrue(recipe.duration()==2000&&pool.start(recipe),"Bottle reaction failed or duration changed");
        h.assertTrue(pool.items.getStackInSlot(0).isEmpty()&&pool.items.getStackInSlot(1).isEmpty(),"Ingredients not consumed");
        pool.advance(200);h.assertTrue(pool.fluids.total()==900,"Wrong first fluid installment");pool.advance(1800);
        h.assertTrue(pool.fluids.total()==0&&!pool.reacting()&&level.getBlockState(at).is(catalyst),"Fluid cost or catalyst consumption wrong");
        h.assertTrue(java.util.stream.IntStream.range(0,9).map(i->pool.items.getStackInSlot(i).is(ModItems.POCKET_MAGIC_BOTTLE.get())?pool.items.getStackInSlot(i).getCount():0).sum()==1,"Expected one magic bottle");h.succeed();
    }
    @GameTest(template="spell_arena") public static void reactionsHaveAtMostFourItemInputs(GameTestHelper h){
        var input=new ReactionRecipe.InputItem(Ingredient.of(Items.DIRT),1);
        new ReactionRecipe(java.util.Collections.nCopies(4,input),FluidStack.EMPTY,Ingredient.EMPTY,List.of(new ItemStack(Items.STONE)),FluidStack.EMPTY,2000);
        boolean rejected=false;
        try{new ReactionRecipe(java.util.Collections.nCopies(5,input),FluidStack.EMPTY,Ingredient.EMPTY,List.of(new ItemStack(Items.STONE)),FluidStack.EMPTY,2000);}
        catch(IllegalArgumentException expected){rejected=true;}
        h.assertTrue(rejected,"Reaction accepted more than four item ingredients");
        h.assertTrue(pool(h).items.getSlots()==9,"Recipe limit changed pool storage capacity");h.succeed();
    }
    @GameTest(template="spell_arena") public static void roughEnergyCoreInPool(GameTestHelper h){
        var pool=pool(h);var level=h.getLevel();
        var recipe=level.getRecipeManager().getAllRecipesFor(ModRecipes.REACTION_TYPE.get()).stream()
                .filter(r->r.id().equals(net.minecraft.resources.ResourceLocation.parse("omnira:reaction/rough_alchemical_energy_core"))).findFirst().orElseThrow().value();
        pool.items.setStackInSlot(0,new ItemStack(ModItems.EARTH_MICROCORE.get()));
        pool.items.setStackInSlot(2,new ItemStack(ModItems.WATER_MICROCORE.get()));
        pool.items.setStackInSlot(5,new ItemStack(ModItems.FIRE_MICROCORE.get()));
        pool.items.setStackInSlot(8,new ItemStack(ModItems.AIR_MICROCORE.get()));
        pool.manualFluid.fill(new FluidStack(Fluids.LAVA,4000),EXECUTE);
        h.assertTrue(!pool.start(recipe),"Pool ignored the core catalyst");
        var support=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("omnira:spiritual_crystal_block"));
        level.setBlockAndUpdate(pool.getBlockPos().offset(1,-1,1),support.defaultBlockState());
        h.assertTrue(pool.start(recipe),"Pool rejected catalyst at floor corner or scattered ingredients");
        pool.advance(200);h.assertTrue(pool.fluids.total()==3600,"Pool lava batch incorrect");pool.advance(1800);
        h.assertTrue(!pool.reacting()&&pool.fluids.total()==0&&java.util.stream.IntStream.range(0,9)
                .map(i->pool.items.getStackInSlot(i).is(ModItems.ROUGH_ALCHEMICAL_ENERGY_CORE.get())?pool.items.getStackInSlot(i).getCount():0).sum()==1,"Pool core output incorrect");
        h.assertTrue(java.util.stream.IntStream.range(0,9).map(i->pool.items.getStackInSlot(i).getCount()).sum()==1,"Pool left or duplicated ingredients");h.succeed();
    }
    private static final IFluidHandler.FluidAction EXECUTE=IFluidHandler.FluidAction.EXECUTE;
    private static final IFluidHandler.FluidAction SIMULATE=IFluidHandler.FluidAction.SIMULATE;
    private static ReactionPoolBlockEntity pool(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(5,3,5));
        h.assertTrue(ReactionPoolBlock.place(h.getLevel(),pos,Direction.NORTH),"Pool placement failed");
        return ReactionPoolBlock.controller(h.getLevel(),pos);
    }
    private static ReactionRecipe recipe(Item input,Item output,int time){return new ReactionRecipe(List.of(new ReactionRecipe.InputItem(Ingredient.of(input),1)),FluidStack.EMPTY,Ingredient.EMPTY,List.of(new ItemStack(output)),FluidStack.EMPTY,time);}
    @GameTest(template="spell_arena") public static void liquidLayerClickReordersAndPersists(GameTestHelper h){
        var pool=pool(h);var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setPos(pool.getBlockPos().getX()+.5,pool.getBlockPos().getY()+1,pool.getBlockPos().getZ()+.5);
        var timeflow=com.mcmagic.omnira.mire.MireContent.SOURCE.get();
        pool.manualFluid.fill(new FluidStack(timeflow,10000),EXECUTE);pool.manualFluid.fill(new FluidStack(Fluids.WATER,10000),EXECUTE);
        var menu=new ReactionPoolMenu(1,player.getInventory(),pool);
        var top=ReactionPoolFluidLayout.layers(pool.fluids,pool.capacity()).getLast();
        int selected=ReactionPoolFluidLayout.rowAt(pool.fluids,pool.capacity(),40,(top.top()+top.bottom())/2.0);
        h.assertTrue(selected==0&&menu.clickMenuButton(player,selected),"Click on visible upper timeflow layer failed");
        h.assertTrue(pool.fluids.getFluidInTank(1).getFluid()==timeflow,"Selected liquid not moved to bottom");
        h.assertTrue(pool.fluids.total()==20000&&pool.manualFluid.drain(1000,SIMULATE).getFluid()==Fluids.WATER,"Reorder lost liquid or changed top extraction");
        var copy=new ReactionPoolBlockEntity(pool.getBlockPos(),pool.getBlockState());copy.setLevel(h.getLevel());
        copy.loadWithComponents(pool.getUpdateTag(h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(copy.fluids.getFluidInTank(1).getFluid()==timeflow&&copy.fluids.total()==20000,"Client update/persistence lost liquid order");
        h.assertTrue(ReactionPoolFluidLayout.rowAt(pool.fluids,pool.capacity(),80,28)==0,"Fluid list click no longer works");
        h.assertTrue(ReactionPoolFluidLayout.rowAt(pool.fluids,pool.capacity(),40,31)==-1&&!menu.clickMenuButton(player,3),"Empty area or invalid row reordered fluids");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void panelOnlyInteractionAndNineTopNodes(GameTestHelper h){
        var pool=pool(h);var level=h.getLevel();var player=h.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);
        var facing=pool.getBlockState().getValue(ReactionPoolBlock.FACING);var right=ReactionPoolBlock.offset(facing,5);var back=ReactionPoolBlock.offset(facing,7);
        var panelPos=pool.getBlockPos().offset(ReactionPoolBlock.offset(facing,10));
        var panelPoint=new net.minecraft.world.phys.Vec3(pool.getBlockPos().getX()+.5+back.getX()*(-25/16.0),pool.getBlockPos().getY()+25.25/16,pool.getBlockPos().getZ()+.5+back.getZ()*(-25/16.0));
        var panelHit=new net.minecraft.world.phys.BlockHitResult(panelPoint,facing.getOpposite(),panelPos,false);
        h.assertTrue(level.getBlockState(panelPos).useWithoutItem(level,player,panelHit).consumesAction(),"Control panel did not open UI");
        com.mcmagic.omnira.block.entity.ResonanceCoreBlockEntity first=null;
        for(int part=9;part<18;part++){
            var pos=pool.getBlockPos().offset(ReactionPoolBlock.offset(facing,part));
            var hit=new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5),Direction.UP,pos,false);
            h.assertTrue(!level.getBlockState(pos).useWithoutItem(level,player,hit).consumesAction(),"Pool body intercepted placement with UI");
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(ModItems.RESONANCE_CORE.get()));
            var clicked=part%2==0?pos.below():pos;
            var nodeHit=new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(clicked.getX()+.5,clicked.getY()+.9,clicked.getZ()+.5),Direction.UP,clicked,false);
            var action=level.getBlockState(clicked).useItemOn(player.getMainHandItem(),level,player,net.minecraft.world.InteractionHand.MAIN_HAND,nodeHit);
            h.assertTrue(action==net.minecraft.world.ItemInteractionResult.SUCCESS,"Top node placement failed from pool body or floor");
            var core=(com.mcmagic.omnira.block.entity.ResonanceCoreBlockEntity)level.getBlockEntity(pos.above());
            h.assertTrue(core!=null&&core.items()!=null&&core.fluids()!=null,"Top node cannot access shared pool storage");
            if(first==null)first=core;else h.assertTrue(com.mcmagic.omnira.storage.StorageContainerIdentity.sameStorage(first,core),"Nodes misidentified one pool as separate containers");
        }
        pool.items.insertItem(0,new ItemStack(Items.DIAMOND,2),false);
        h.assertTrue(first.items().extractItem(0,1,false).getCount()==1,"Node cannot extract stored items");
        pool.manualFluid.fill(new FluidStack(Fluids.WATER,1000),EXECUTE);
        h.assertTrue(first.fluids().drain(500,EXECUTE).getAmount()==500,"Node cannot extract stored liquid");h.succeed();
    }
    @GameTest(template="spell_arena") public static void panelHitRotatesWithFacing(GameTestHelper h){
        for(var facing:Direction.Plane.HORIZONTAL){
            var back=ReactionPoolBlock.offset(facing,7);
            var hit=new net.minecraft.world.phys.Vec3(.5+back.getX()*(-25/16.0),25.25/16,.5+back.getZ()*(-25/16.0));
            h.assertTrue(ReactionPoolGeometry.panelHit(facing,hit),"Rotated panel hit missed");
            h.assertTrue(!ReactionPoolGeometry.panelHit(facing,new net.minecraft.world.phys.Vec3(.5,2,.5)),"Pool opening opens UI");
            h.assertTrue(!ReactionPoolGeometry.panelHit(facing,new net.minecraft.world.phys.Vec3(.5,.5,.5)),"Pool base opens UI");
        }h.succeed();
    }
    private static void reportedCapacity(GameTestHelper h,ReactionPoolBlockEntity pool){
        for(var handler:List.of(pool.fluids,pool.manualFluid,pool.fluidInput,pool.fluidOutput)){
            int sum=0;for(int i=0;i<handler.getTanks();i++){
                int capacity=handler.getTankCapacity(i);sum+=capacity;
                h.assertTrue(capacity>=handler.getFluidInTank(i).getAmount(),"Reported capacity below stored amount");
            }
            h.assertTrue(sum==pool.capacity(),"Capability reports shared capacity more than once");
        }
    }
    @GameTest(template="spell_arena") public static void sharedCapacityReporting(GameTestHelper h){
        var pool=pool(h);reportedCapacity(h,pool);
        for(var fluid:List.of(Fluids.WATER,Fluids.LAVA,com.mcmagic.omnira.mire.MireContent.SOURCE.get(),Fluids.FLOWING_WATER)){
            h.assertTrue(pool.manualFluid.fill(new FluidStack(fluid,8000),EXECUTE)==8000,"Four fluid kinds rejected");reportedCapacity(h,pool);
        }
        h.assertTrue(pool.manualFluid.fill(new FluidStack(Fluids.WATER,16000),SIMULATE)==8000&&pool.fluids.total()==32000,"Simulation changed shared storage");
        h.assertTrue(pool.manualFluid.fill(new FluidStack(Fluids.WATER,16000),EXECUTE)==8000,"Existing kind cannot use shared free capacity");reportedCapacity(h,pool);
        h.assertTrue(pool.manualFluid.fill(new FluidStack(Fluids.LAVA,1),EXECUTE)==0,"Shared tank exceeded 40B");
        pool.manualFluid.drain(new FluidStack(Fluids.LAVA,8000),EXECUTE);reportedCapacity(h,pool);
        pool.upgrades.insertItem(0,new ItemStack(ModItems.BASIC_STACKING_UPGRADE.get()),false);reportedCapacity(h,pool);
        h.assertTrue(pool.capacity()==80000,"Upgrade did not extend shared capacity");h.succeed();
    }
    @GameTest(template="spell_arena") public static void sharedCapacityUpgradesAndOrder(GameTestHelper h){
        var pool=pool(h);var water=new FluidStack(Fluids.WATER,30000);var lava=new FluidStack(Fluids.LAVA,30000);
        h.assertTrue(pool.manualFluid.fill(water,EXECUTE)==30000&&pool.manualFluid.fill(lava,EXECUTE)==10000,"Not one shared 40000 capacity");
        pool.upgrades.insertItem(0,new ItemStack(ModItems.BASIC_STACKING_UPGRADE.get()),false);
        h.assertTrue(pool.capacity()==80000&&pool.manualFluid.fill(lava,EXECUTE)==30000,"Basic storage multiplier missing");
        h.assertTrue(pool.upgrades.extractItem(0,1,false).isEmpty(),"Removed capacity upgrade while overfull");
        h.assertTrue(pool.items.insertItem(0,new ItemStack(Items.DIAMOND,128),false).isEmpty(),"Item limit did not double");
        h.assertTrue(pool.items.insertItem(1,new ItemStack(Items.DIAMOND),false).getCount()==1,"Duplicate item kind accepted");
        pool.manualFluid.drain(new FluidStack(Fluids.LAVA,40000),EXECUTE);pool.items.extractItem(0,64,false);
        h.assertTrue(!pool.upgrades.extractItem(0,1,false).isEmpty(),"Safe upgrade removal rejected");
        pool.upgrades.insertItem(0,new ItemStack(ModItems.INFUSED_STACKING_UPGRADE.get()),false);
        h.assertTrue(pool.capacity()==160000&&pool.items.insertItem(0,new ItemStack(Items.DIAMOND,192),false).isEmpty(),"Infused storage multiplier missing");
        pool.fluids.moveLast(0);h.assertTrue(pool.fluids.getFluidInTank(pool.fluids.count()-1).getFluid()==Fluids.WATER,"Fluid queue reorder failed");
        var copy=new ReactionPoolBlockEntity(pool.getBlockPos(),pool.getBlockState());copy.setLevel(h.getLevel());copy.loadWithComponents(pool.saveWithFullMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(copy.capacity()==160000&&copy.fluids.total()==pool.fluids.total()&&copy.items.getStackInSlot(0).getCount()==256,"Storage persistence lost capacity/content");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void parallelItemsIndependentProgressAndReload(GameTestHelper h){
        var pool=pool(h);pool.items.insertItem(0,new ItemStack(Items.STICK,2),false);pool.items.insertItem(1,new ItemStack(Items.FLINT,2),false);
        var a=recipe(Items.STICK,Items.DIAMOND,2000);var b=recipe(Items.FLINT,Items.EMERALD,1000);
        h.assertTrue(pool.start(a)&&pool.start(b)&&pool.reactionCount()==2,"Item recipes did not run in parallel");
        h.assertTrue(!pool.start(a),"Duplicate concurrent recipe started");pool.advance(500);
        var copy=new ReactionPoolBlockEntity(pool.getBlockPos(),pool.getBlockState());copy.setLevel(h.getLevel());copy.loadWithComponents(pool.saveWithFullMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(copy.reactionCount()==2&&!copy.start(a),"Reload duplicated recipe");
        copy.advance(500);h.assertTrue(copy.reactionCount()==1,"Independent short recipe cooldown lost");
        copy.advance(1000);h.assertTrue(!copy.reacting(),"Long reaction did not finish independently");
        boolean diamond=false,emerald=false;for(int i=0;i<9;i++){diamond|=copy.items.getStackInSlot(i).is(Items.DIAMOND);emerald|=copy.items.getStackInSlot(i).is(Items.EMERALD);}
        h.assertTrue(diamond&&emerald,"Parallel outputs lost");h.succeed();
    }
    @GameTest(template="spell_arena") public static void parallelLiquidsAndCatalystFloor(GameTestHelper h){
        var pool=pool(h);var level=h.getLevel();
        level.setBlockAndUpdate(pool.getBlockPos().offset(-1,-1,-1),DreamContent.DREAM_CRYSTAL_BLOCK.get().defaultBlockState());
        level.setBlockAndUpdate(pool.getBlockPos().offset(1,-1,1),com.mcmagic.omnira.aggregation.AggregationContent.SPATIAL_BLOCK.get().defaultBlockState());
        pool.manualFluid.fill(new FluidStack(Fluids.WATER,4000),EXECUTE);
        pool.manualFluid.fill(new FluidStack(com.mcmagic.omnira.mire.MireContent.SOURCE.get(),1000),EXECUTE);
        for(var holder:level.getRecipeManager().getAllRecipesFor(ModRecipes.REACTION_TYPE.get()))
            if(holder.value().matches(new ReactionRecipe.Input(pool.items,pool.fluids,pool::supportMatches),level))
                h.assertTrue(pool.start(holder.value()),"Matching liquid recipe failed to start");
        h.assertTrue(pool.reactionCount()==2,"Liquid recipes not concurrent");pool.advance(200);
        h.assertTrue(pool.fluids.total()==4500&&pool.manualFluid.drain(40000,SIMULATE).getAmount()==3600,"Batch consumption or top-first container extraction incorrect");
        level.setBlockAndUpdate(pool.getBlockPos().offset(-1,-1,-1),Blocks.AIR.defaultBlockState());pool.advance(1800);
        h.assertTrue(pool.reactionCount()==1&&pool.fluids.amount(new FluidStack(Fluids.WATER,1))==3600,"Missing catalyst paused unrelated reaction");
        level.setBlockAndUpdate(pool.getBlockPos().offset(-1,-1,-1),DreamContent.DREAM_CRYSTAL_BLOCK.get().defaultBlockState());pool.advance(1800);
        h.assertTrue(!pool.reacting()&&pool.fluids.total()==0,"Paused recipe did not resume");h.succeed();
    }
    @GameTest(template="spell_arena") public static void progressiveFluidOutputAndReservation(GameTestHelper h){
        var pool=pool(h);pool.manualFluid.fill(new FluidStack(Fluids.WATER,1000),EXECUTE);
        var transform=new ReactionRecipe(List.of(),new FluidStack(Fluids.WATER,1000),Ingredient.EMPTY,List.of(),new FluidStack(Fluids.LAVA,370),2000);
        h.assertTrue(pool.start(transform),"Fluid transformation rejected");pool.advance(200);
        h.assertTrue(pool.fluids.amount(new FluidStack(Fluids.WATER,1))==900&&pool.fluids.amount(new FluidStack(Fluids.LAVA,1))==37,"Liquid result did not appear in first batch");
        var copy=new ReactionPoolBlockEntity(pool.getBlockPos(),pool.getBlockState());copy.setLevel(h.getLevel());copy.loadWithComponents(pool.saveWithFullMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());copy.advance(1800);
        h.assertTrue(copy.fluids.total()==370&&!copy.reacting(),"Fluid transformation reload/batches lost amounts");
        pool.manualFluid.drain(new FluidStack(Fluids.LAVA,370),EXECUTE);
        h.assertTrue(pool.manualFluid.drain(new FluidStack(Fluids.WATER,900),SIMULATE).getAmount()==900,"Player cannot extract reactive liquid");
        var pos=h.absolutePos(new BlockPos(10,3,5));h.getLevel().setBlockAndUpdate(pos,ModBlocks.PRIMITIVE_REACTION_CAULDRON.get().defaultBlockState());
        var primitive=(RoughAlchemyCauldronBlockEntity)h.getLevel().getBlockEntity(pos);primitive.tank.fill(new FluidStack(Fluids.WATER,1000),EXECUTE);
        h.assertTrue(!primitive.start(transform),"Primitive cauldron allowed mixed liquids");h.succeed();
    }
    @GameTest(template="spell_arena") public static void filtersDirectInteractionAndOutputBudget(GameTestHelper h){
        var pool=pool(h);var plate=new ItemStack(ModItems.INTAKE_UPGRADE.get());
        var settings=OrbUpgrades.settings(plate);settings.putBoolean("Whitelist",true);plate.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(settings));
        OrbUpgrades.setFilter(plate,0,new ItemStack(Items.WATER_BUCKET),h.getLevel().registryAccess());pool.upgrades.insertItem(0,plate,false);
        h.assertTrue(pool.fluidInput.fill(new FluidStack(Fluids.LAVA,1000),SIMULATE)==0,"External liquid bypassed intake whitelist");
        h.assertTrue(pool.fluidInput.fill(new FluidStack(Fluids.WATER,1000),EXECUTE)==1000,"Whitelisted liquid denied");
        h.assertTrue(pool.itemInput.insertItem(0,new ItemStack(Items.WATER_BUCKET),false).isEmpty(),"Bucket sample did not whitelist bucket item");
        h.assertTrue(!pool.itemInput.insertItem(1,new ItemStack(Items.DIAMOND),false).isEmpty(),"External item bypassed intake filter");
        h.assertTrue(pool.manualFluid.fill(new FluidStack(Fluids.LAVA,1000),EXECUTE)==1000&&pool.items.insertItem(1,new ItemStack(Items.DIAMOND,3),false).isEmpty(),"Direct player insertion was filtered");
        pool.energyInput.receiveEnergy(512,false);
        h.runAfterDelay(1,()->pool.energyInput.receiveEnergy(512,false));
        h.runAfterDelay(2,()->{
        ReactionPoolBlockEntity.tick(h.getLevel(),pool.getBlockPos(),pool.getBlockState(),pool);
        h.assertTrue(pool.powered()&&pool.electricalPowered(),"Electric supply did not start machine in electrical mode");
        h.assertTrue(pool.manualFluid.drain(250,SIMULATE).getFluid()==Fluids.WATER&&pool.fluidOutput.drain(250,SIMULATE).getFluid()==Fluids.LAVA,"Player and automatic layer priorities not opposite");
        pool.itemOutput.extractItem(1,1,true);
        h.assertTrue(pool.outputItem().isEmpty()&&pool.outputFluid().isEmpty(),"Simulated output started pipe animation");
        var output=new ItemStack(ModItems.OUTPUT_UPGRADE.get());OrbUpgrades.setFilter(output,0,new ItemStack(Items.WATER_BUCKET),h.getLevel().registryAccess());pool.upgrades.insertItem(1,output,false);
        h.assertTrue(pool.fluidOutput.drain(new FluidStack(Fluids.WATER,1000),EXECUTE).isEmpty(),"Output bucket blacklist did not filter fluid");
        h.assertTrue(pool.itemOutput.extractItem(0,1,false).isEmpty(),"Output bucket blacklist did not filter bucket");
        h.assertTrue(pool.itemOutput.extractItem(1,64,false).getCount()==1&&pool.itemOutput.extractItem(1,64,false).isEmpty(),"Item output exceeds 1 per second");
        h.assertTrue(pool.fluidOutput.drain(new FluidStack(Fluids.LAVA,1000),EXECUTE).getAmount()==500&&pool.fluidOutput.drain(new FluidStack(Fluids.LAVA,1000),EXECUTE).isEmpty(),"Liquid output exceeds 500 per second");
        h.assertTrue(pool.outputItem().is(Items.DIAMOND)&&pool.outputFluid().getFluid()==Fluids.LAVA&&pool.outputItemTick()==h.getLevel().getGameTime()&&pool.outputFluidTick()==h.getLevel().getGameTime(),"Actual output did not synchronize correct pipe contents");
        var copy=new ReactionPoolBlockEntity(pool.getBlockPos(),pool.getBlockState());copy.setLevel(h.getLevel());copy.loadWithComponents(pool.saveWithFullMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(copy.outputItem().is(Items.DIAMOND)&&copy.outputFluid().getFluid()==Fluids.LAVA&&copy.electricalPowered(),"Output animation packet lost contents or power mode");
        h.succeed();});
    }
    @GameTest(template="spell_arena") public static void octagonalCollisionAndFixedInterfaces(GameTestHelper h){
        for(var facing:Direction.Plane.HORIZONTAL){
            var upper=ReactionPoolGeometry.shape(facing,13);
            h.assertTrue(upper.toAabbs().stream().noneMatch(b->b.contains(.5,.5,.5)),"Octagonal opening covered by invisible collision");
            var lower=ReactionPoolGeometry.shape(facing,4);
            h.assertTrue(lower.toAabbs().stream().anyMatch(b->b.contains(.5,ReactionPoolGeometry.VESSEL_FLOOR-.02,.5)),"Crucible floor collision missing");
            for(int part:new int[]{3,5,6,8})h.assertTrue(!ReactionPoolGeometry.shape(facing,part).isEmpty(),"Connector collision disconnected");
            var right=ReactionPoolBlock.offset(facing,5);
            for(int part:new int[]{3,5}){
                var port=ReactionPoolBlock.offset(facing,part);var outward=ReactionPoolBlock.outward(facing,part);
                double local=(part==3?-16:32)/16.0-.5;
                h.assertTrue(Math.abs(.5+right.getX()*local-(port.getX()+.5+outward.getStepX()*.5))<.00001
                    &&Math.abs(.5+right.getZ()*local-(port.getZ()+.5+outward.getStepZ()*.5))<.00001,"Rendered shaft tip not on connector block face");
            }
        }
        var item=ReactionPoolGeometry.outputPoint(false,1);var fluid=ReactionPoolGeometry.outputPoint(true,1);
        h.assertTrue(Math.abs(item[0]*16+7)<.001&&Math.abs(item[1]*16-10)<.001&&Math.abs(item[2]*16-34)<.001,"Item output endpoint moved");
        h.assertTrue(Math.abs(fluid[0]*16-23)<.001&&Math.abs(fluid[1]*16-10)<.001&&Math.abs(fluid[2]*16-34)<.001,"Fluid output endpoint moved");h.succeed();
    }
    @GameTest(template="spell_arena") public static void pendingOutputCapacityAndIncompleteStructure(GameTestHelper h){
        var pool=pool(h);pool.upgrades.insertItem(0,new ItemStack(ModItems.BASIC_STACKING_UPGRADE.get()),false);
        pool.items.insertItem(0,new ItemStack(Items.STICK),false);pool.items.insertItem(1,new ItemStack(Items.DIAMOND,64),false);
        var recipe=recipe(Items.STICK,Items.DIAMOND,2000);h.assertTrue(pool.start(recipe),"Upgraded recipe failed");
        h.assertTrue(pool.upgrades.extractItem(0,1,false).isEmpty(),"Removed capacity needed by pending output");
        h.assertTrue(!pool.items.insertItem(1,new ItemStack(Items.DIAMOND,64),false).isEmpty(),"Insertion stole pending output space");
        h.getLevel().setBlockAndUpdate(pool.getBlockPos().offset(1,1,1),Blocks.AIR.defaultBlockState());
        pool.energyInput.receiveEnergy(1024,false);ReactionPoolBlockEntity.tick(h.getLevel(),pool.getBlockPos(),pool.getBlockState(),pool);
        h.assertTrue(!pool.powered()&&pool.progressPercent()==0,"Incomplete structure kept working");h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=120) public static void boundedElectricalThroughput(GameTestHelper h){
        var pool=pool(h);int[] received={0};
        for(int i=1;i<=40;i++)h.runAfterDelay(i,()->{
            int amount=pool.energyInput.receiveEnergy(Integer.MAX_VALUE,false);received[0]+=amount;
            h.assertTrue(amount<=410&&pool.energyInput.receiveEnergy(Integer.MAX_VALUE,false)==0,"Pool input burst exceeded limit");
            h.assertTrue(pool.electricalEfficiency()<=16&&pool.efficiency()<=16,"Electrical acceleration exceeded 16x");
        });
        h.runAfterDelay(41,()->{h.assertTrue(received[0]==16384,"Two-second FE intake is not exact");h.succeed();});
    }
    @GameTest(template="spell_arena",timeoutTicks=120) public static void kineticPowerAndHigherSourceSelection(GameTestHelper h){
        var pool=pool(h);
        if(!net.neoforged.fml.ModList.get().isLoaded("create")){h.succeed();return;}
        ReactionPoolCompatibilityChecks.verify(h,pool);
    }
    private static net.minecraft.world.entity.animal.Cow target(GameTestHelper h,BlockPos at){
        var cow=new net.minecraft.world.entity.animal.Cow(net.minecraft.world.entity.EntityType.COW,h.getLevel());
        cow.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);cow.setHealth(100);
        cow.setPos(net.minecraft.world.phys.Vec3.atCenterOf(at));h.getLevel().addFreshEntity(cow);return cow;
    }
    @GameTest(template="spell_arena") public static void failureAtDebitNodeAndStrictHalfThreshold(GameTestHelper h){
        var pool=pool(h);var reaction=new ReactionRecipe(List.of(),new FluidStack(Fluids.WATER,1000),Ingredient.EMPTY,List.of(new ItemStack(Items.DIAMOND)),FluidStack.EMPTY,2000);
        var cow=target(h,pool.getBlockPos().offset(3,0,0));
        pool.manualFluid.fill(new FluidStack(Fluids.WATER,1000),EXECUTE);h.assertTrue(pool.start(reaction),"Failure recipe start rejected");
        pool.advance(1000);pool.manualFluid.drain(500,EXECUTE);pool.advance(199);
        h.assertTrue(pool.reacting()&&cow.getHealth()==100,"Failure checked before debit node");pool.advance(1);
        h.assertTrue(!pool.reacting()&&cow.getHealth()==100,"Exactly half triggered explosion");
        pool.manualFluid.fill(new FluidStack(Fluids.WATER,1000),EXECUTE);pool.start(reaction);pool.advance(1200);pool.manualFluid.drain(400,EXECUTE);
        pool.items.insertItem(0,new ItemStack(Items.STICK),false);h.assertTrue(pool.start(recipe(Items.STICK,Items.EMERALD,2000)),"Independent job did not start");
        pool.advance(199);h.assertTrue(pool.reactionCount()==2&&cow.getHealth()==100,"Liquid shortage caused early failure");pool.advance(1);
        h.assertTrue(pool.reactionCount()==1&&cow.getHealth()==70&&cow.getDeltaMovement().length()>2,"Large failure damage, knockback or independent reaction incorrect");
        pool.advance(200);h.assertTrue(cow.getHealth()==70,"Failed reaction exploded repeatedly");
        h.assertTrue(java.util.stream.IntStream.range(0,9).noneMatch(i->pool.items.getStackInSlot(i).is(Items.DIAMOND)),"Failed reaction produced output");h.succeed();
    }
    @GameTest(template="spell_arena") public static void primitiveFailureDamageAndRadius(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(5,3,5));h.getLevel().setBlockAndUpdate(pos,ModBlocks.PRIMITIVE_REACTION_CAULDRON.get().defaultBlockState());
        var vessel=(RoughAlchemyCauldronBlockEntity)h.getLevel().getBlockEntity(pos);
        var cow=target(h,pos.offset(3,0,0));var outside=target(h,pos.offset(11,0,0));
        var reaction=new ReactionRecipe(List.of(),new FluidStack(Fluids.WATER,1000),Ingredient.EMPTY,List.of(new ItemStack(Items.DIAMOND)),FluidStack.EMPTY,2000);
        vessel.tank.fill(new FluidStack(Fluids.WATER,1000),EXECUTE);vessel.start(reaction);
        for(int i=0;i<1200;i++)RoughAlchemyCauldronBlockEntity.tick(h.getLevel(),pos,vessel.getBlockState(),vessel);
        vessel.tank.drain(400,EXECUTE);
        for(int i=0;i<199;i++)RoughAlchemyCauldronBlockEntity.tick(h.getLevel(),pos,vessel.getBlockState(),vessel);
        h.assertTrue(vessel.working()&&cow.getHealth()==100,"Primitive failure before debit node");
        RoughAlchemyCauldronBlockEntity.tick(h.getLevel(),pos,vessel.getBlockState(),vessel);
        h.assertTrue(!vessel.working()&&cow.getHealth()==85&&outside.getHealth()==100&&cow.getDeltaMovement().length()>2,"Primitive blast damage/radius wrong");
        for(int i=0;i<300;i++)RoughAlchemyCauldronBlockEntity.tick(h.getLevel(),pos,vessel.getBlockState(),vessel);
        h.assertTrue(cow.getHealth()==85,"Primitive explosion repeated");h.succeed();
    }
    private ReactionPoolGameTests(){}
}
