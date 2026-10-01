package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.block.entity.*;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.menu.CrystalBallMenu;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.*;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_orb_storage")
@PrefixGameTestTemplate(false)
public final class OrbStorageGameTests {
    @GameTest(template="spell_arena",timeoutTicks=60) public static void speedUpgradeSharesFluidBudget(GameTestHelper h) {
        var ball=(LiquidCrystalBallBlockEntity)place(h,true);
        ball.upgrades.insertItem(0,new ItemStack(ModItems.SPEED_UPGRADE.get()),false);
        ball.tank.fill(new FluidStack(Fluids.LAVA,3000),FluidAction.EXECUTE);
        h.runAfterDelay(22,()->{
            h.assertTrue(ball.fluidHandler.drain(2000,FluidAction.SIMULATE).getAmount()==1000,"Accelerated pipe budget is not 1000 mB");
            h.assertTrue(ball.fluidHandler.drain(2000,FluidAction.EXECUTE).getAmount()==1000,"Pipe debit differs from simulation");
            h.assertTrue(ball.fluidHandler.drain(1,FluidAction.EXECUTE).isEmpty(),"Repeated calls bypass fluid limit");
        });
        h.runAfterDelay(44,()->{
            ball.upgrades.extractItem(0,1,false);
            h.assertTrue(ball.fluidHandler.drain(2000,FluidAction.EXECUTE).getAmount()==500,"Removing speed upgrade does not restore pipe limit");h.succeed();
        });
    }
    @GameTest(template="spell_arena") public static void speedDoesNotGrantAutomationOrBoostProduction(GameTestHelper h) {
        var ball=(LiquidCrystalBallBlockEntity)place(h,true);
        ball.upgrades.insertItem(0,new ItemStack(ModItems.SPEED_UPGRADE.get()),false);
        for(int i=0;i<20;i++)ball.serverTick();h.assertTrue(ball.tank.isEmpty(),"Speed upgrade produces contents");
        ball.upgrades.insertItem(1,new ItemStack(ModItems.LAVA_PRODUCTION_UPGRADE.get()),false);
        for(int i=0;i<20;i++)ball.serverTick();h.assertTrue(ball.tank.getFluidAmount()==100,"Speed upgrade boosts production");h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=60) public static void speedUpgradeBucketAndRecipe(GameTestHelper h) {
        var container=new net.minecraft.world.SimpleContainer(7);
        container.setItem(6,new ItemStack(ModItems.SWIFTNESS_RUNE.get()));
        for(int i=0;i<6;i++)container.setItem(i,new ItemStack(ModItems.RESONANCE_CRYSTAL.get()));
        var work=com.mcmagic.omnira.recipe.AssemblyWork.find(h.getLevel(),container);
        h.assertTrue(work!=null && work.finish().get().getFirst().is(ModItems.SPEED_UPGRADE.get()),"Missing speed upgrade forging recipe");
        var ball=(LiquidCrystalBallBlockEntity)place(h,true);
        ball.upgrades.insertItem(0,new ItemStack(ModItems.SPEED_UPGRADE.get()),false);
        h.assertTrue(!ball.upgrades.isItemValid(1,new ItemStack(ModItems.SPEED_UPGRADE.get())),"Duplicate speed upgrade accepted");
        ball.setItem(0,new ItemStack(Items.LAVA_BUCKET));
        h.runAfterDelay(18,()->h.assertTrue(ball.tank.isEmpty(),"Bucket completed early"));
        h.runAfterDelay(22,()->{
            h.assertTrue(ball.getItem(0).is(Items.BUCKET) && ball.tank.getFluidAmount()==1000,"Accelerated bucket did not finish in one second");h.succeed();
        });
    }
    @GameTest(template="spell_arena") public static void speedUpgradeItemCadence(GameTestHelper h) {
        var ball=place(h,false);
        ball.upgrades.insertItem(0,new ItemStack(ModItems.SPEED_UPGRADE.get()),false);
        ball.upgrades.insertItem(1,new ItemStack(ModItems.OUTPUT_UPGRADE.get()),false);
        var pos=ball.getBlockPos().below();h.getLevel().setBlockAndUpdate(pos,Blocks.BARREL.defaultBlockState());
        var output=(net.minecraft.world.Container)h.getLevel().getBlockEntity(pos);
        ball.setItem(0,new ItemStack(Items.STONE,8));
        for(int i=0;i<9;i++)ball.serverTick();h.assertTrue(output.isEmpty(),"Accelerated output ran too early");
        ball.serverTick();h.assertTrue(output.countItem(Items.STONE)==1,"Missing half-second transfer");
        for(int i=0;i<10;i++)ball.serverTick();h.assertTrue(output.countItem(Items.STONE)==2,"Missing second transfer");
        ball.upgrades.extractItem(0,1,false);
        for(int i=0;i<10;i++)ball.serverTick();h.assertTrue(output.countItem(Items.STONE)==2,"Removed speed upgrade still active");
        for(int i=0;i<10;i++)ball.serverTick();h.assertTrue(output.countItem(Items.STONE)==3,"Base output cadence changed");h.succeed();
    }
    @GameTest(template="spell_arena") public static void bucketAndBottleFluidFilters(GameTestHelper h) {
        var ball=place(h,true);
        var water=new FluidStack(Fluids.WATER,1000);var lava=new FluidStack(Fluids.LAVA,1000);
        var bottle=net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.POTION,net.minecraft.world.item.alchemy.Potions.WATER);
        var healing=net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.POTION,net.minecraft.world.item.alchemy.Potions.HEALING);
        for(var upgrade:java.util.List.of(ModItems.INTAKE_UPGRADE.get(),ModItems.OUTPUT_UPGRADE.get())) {
            ball.upgrades.setStackInSlot(0,new ItemStack(upgrade));
            for(var sample:java.util.List.of(new ItemStack(Items.WATER_BUCKET),bottle)) {
                ball.upgrades.filter(0,0,sample);
                for(boolean whitelist:new boolean[]{false,true}) {
                    var data=OrbUpgrades.settings(ball.upgrades.getStackInSlot(0));data.putBoolean("Whitelist",whitelist);ball.upgrades.update(0,data);
                    h.assertTrue(ball.upgrades.matches(0,ItemStack.EMPTY,water)==whitelist,"Water sample mode mismatch");
                    h.assertTrue(ball.upgrades.matches(0,ItemStack.EMPTY,lava)!=whitelist,"Water filter matched lava");
                }
                h.assertTrue(sample.getCount()==1,"Filter consumed sample");
            }
            ball.upgrades.filter(0,0,healing);
            h.assertTrue(healing.get(DataComponents.POTION_CONTENTS).equals(ball.upgrades.filter(0,0).get(DataComponents.POTION_CONTENTS)),"Potion filter lost components");
            h.assertTrue(!ball.upgrades.matches(0,ItemStack.EMPTY,water),"Healing potion treated as water");
            if(net.neoforged.fml.ModList.get().isLoaded("create")) {
                var potion=com.mcmagic.omnira.compat.OrbCreateFilters.bottledFluid(healing);
                h.assertTrue(!potion.isEmpty() && ball.upgrades.matches(0,ItemStack.EMPTY,potion),"Potion bottle failed to match potion fluid");
                var other=net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.POTION,net.minecraft.world.item.alchemy.Potions.SWIFTNESS);
                h.assertTrue(!ball.upgrades.matches(0,ItemStack.EMPTY,com.mcmagic.omnira.compat.OrbCreateFilters.bottledFluid(other)),"Different potion effects matched");
                var honey=new ItemStack(Items.HONEY_BOTTLE);ball.upgrades.filter(0,0,honey);
                h.assertTrue(ball.upgrades.matches(0,ItemStack.EMPTY,com.mcmagic.omnira.compat.OrbCreateFilters.bottledFluid(honey)),"Honey bottle failed");
            }
            ball.upgrades.filter(0,0,new ItemStack(Items.GLASS_BOTTLE));
            h.assertTrue(!ball.upgrades.matches(0,ItemStack.EMPTY,water),"Empty bottle matched water");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void heldUpgradeSettingsPersistAndLockSource(GameTestHelper h) {
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        var plate=new ItemStack(ModItems.INTAKE_UPGRADE.get());player.getInventory().setItem(0,plate);
        var menu=new com.mcmagic.omnira.menu.OrbUpgradeMenu(1,player.getInventory(),0,true);
        h.assertTrue(menu.stillValid(player) && menu.slots.size()==39,"Invalid held configuration menu");
        menu.setCarried(new ItemStack(Items.STONE,16));
        menu.clicked(0,0,net.minecraft.world.inventory.ClickType.PICKUP,player);
        h.assertTrue(menu.getCarried().getCount()==16 && menu.slots.get(0).getItem().is(Items.STONE),"Ghost filter consumed its sample");
        menu.clickMenuButton(player,0);menu.clickMenuButton(player,2);
        h.assertTrue(OrbUpgrades.settings(plate).getBoolean("Whitelist") && OrbUpgrades.settings(plate).getInt("Directions")==3,"Held settings not saved");
        menu.setCarried(ItemStack.EMPTY);
        menu.clicked(30,0,net.minecraft.world.inventory.ClickType.PICKUP,player);
        menu.clicked(3,0,net.minecraft.world.inventory.ClickType.SWAP,player);
        h.assertTrue(player.getInventory().getItem(0)==plate && menu.getCarried().isEmpty(),"Bound plate could move during editing");
        var ball=place(h,false);ball.upgrades.insertItem(0,plate.copy(),false);
        h.assertTrue(ball.upgrades.matches(0,new ItemStack(Items.STONE),null) && !ball.upgrades.matches(0,new ItemStack(Items.DIRT),null),"Installed plate lost held filters");
        player.getInventory().setItem(0,ItemStack.EMPTY);
        h.assertTrue(!menu.stillValid(player) && !menu.clickMenuButton(player,0),"Detached menu modified old plate");
        player.getInventory().setItem(40,new ItemStack(ModItems.OUTPUT_UPGRADE.get()));
        var offhand=new com.mcmagic.omnira.menu.OrbUpgradeMenu(2,player.getInventory(),40,false);
        offhand.clicked(3,40,net.minecraft.world.inventory.ClickType.SWAP,player);
        h.assertTrue(player.getOffhandItem().is(ModItems.OUTPUT_UPGRADE.get()) && !offhand.clickMenuButton(player,3),"Offhand lock or output direction validation failed");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void recipesAreShapelessAndReturnOnlyLavaBuckets(GameTestHelper h) {
        var container=new net.minecraft.world.SimpleContainer(7);
        container.setItem(6,new ItemStack(ModItems.GRID_FRAME.get()));
        for(int i=0;i<6;i++)container.setItem(i,new ItemStack(i%2==0?Items.LAVA_BUCKET:Items.POINTED_DRIPSTONE));
        var work=com.mcmagic.omnira.recipe.AssemblyWork.find(h.getLevel(),container);
        h.assertTrue(work!=null,"Unordered lava recipe missing");var result=work.finish().get();
        h.assertTrue(result.getFirst().is(ModItems.LAVA_PRODUCTION_UPGRADE.get()) && result.stream().filter(s->s.is(Items.BUCKET)).mapToInt(ItemStack::getCount).sum()==3,"Lava recipe did not return three buckets");
        container.setItem(6,new ItemStack(ModItems.CRYSTAL_BALL.get()));for(int i=0;i<6;i++)container.setItem(i,new ItemStack(Items.BUCKET));
        work=com.mcmagic.omnira.recipe.AssemblyWork.find(h.getLevel(),container);
        h.assertTrue(work!=null && work.finish().get().getFirst().is(ModItems.LIQUID_CRYSTAL_BALL.get()),"Liquid crystal ball recipe missing");h.succeed();
    }
    @GameTest(template="spell_arena") public static void shatteringLiquidReleasesSourcesAndPane(GameTestHelper h) {
        var ball=(LiquidCrystalBallBlockEntity)place(h,true);var p=ball.getBlockPos();ball.tank.setFluid(new FluidStack(Fluids.WATER,3000));
        var player=h.makeMockPlayer(GameType.SURVIVAL);for(int i=0;i<9;i++)ball.landedOn(player);
        h.assertTrue(h.getLevel().getBlockEntity(p)==null,"Liquid orb survived shattering");int sources=0;
        for(var pos:BlockPos.betweenClosed(p.offset(-2,0,-2),p.offset(2,0,2)))if(h.getLevel().getFluidState(pos).isSource())sources++;
        h.assertTrue(sources==3,"Shatter source count differs from stored volume");
        var drops=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(p).inflate(2));
        h.assertTrue(drops.stream().filter(e->e.getItem().is(Items.GLASS_PANE)).mapToInt(e->e.getItem().getCount()).sum()==1,"Missing glass pane");
        h.assertTrue(drops.stream().noneMatch(e->e.getItem().is(ModItems.LIQUID_CRYSTAL_BALL.get())),"Shattered liquid orb dropped itself");h.succeed();
    }
    private static CrystalBallBlockEntity place(GameTestHelper h,boolean liquid) {
        var pos=h.absolutePos(new BlockPos(5,2,5));
        h.getLevel().setBlockAndUpdate(pos,(liquid?ModBlocks.LIQUID_CRYSTAL_BALL:ModBlocks.CRYSTAL_BALL).get().defaultBlockState());
        return (CrystalBallBlockEntity)h.getLevel().getBlockEntity(pos);
    }
    @GameTest(template="spell_arena") public static void externalItemRequestsAreNotUpgradeRateLimited(GameTestHelper h) {
        var ball=place(h,false);ball.upgrades.insertItem(0,new ItemStack(ModItems.INFUSED_STACKING_UPGRADE.get()),false);ball.setItem(0,new ItemStack(Items.STONE,256));
        h.assertTrue(ball.itemHandler.extractItem(0,128,true).getCount()==128 && ball.getItem(0).getCount()==256,"External extraction simulation limited or mutated");
        h.assertTrue(ball.itemHandler.extractItem(0,128,false).getCount()==128 && ball.getItem(0).getCount()==128,"External pipe request rate was overridden");h.succeed();
    }
    @GameTest(template="spell_arena") public static void almostFullInventoryAndHotbarRespectNormalLimits(GameTestHelper h) {
        var ball=place(h,false);ball.upgrades.insertItem(0,new ItemStack(ModItems.INFUSED_STACKING_UPGRADE.get()),false);
        var player=h.makeMockPlayer(GameType.SURVIVAL);var menu=new CrystalBallMenu(1,player.getInventory(),ball);
        for(int i=0;i<36;i++)player.getInventory().setItem(i,new ItemStack(Items.DIRT,64));
        player.getInventory().setItem(8,new ItemStack(Items.STONE,60));ball.setItem(0,new ItemStack(Items.STONE,256));
        menu.quickMoveStack(player,0);
        h.assertTrue(player.getInventory().getItem(8).getCount()==64 && ball.getItem(0).getCount()==252,"Full player inventory lost remainder");
        menu.clicked(0,1,net.minecraft.world.inventory.ClickType.PICKUP,player);
        h.assertTrue(menu.getCarried().getCount()==32 && ball.getItem(0).getCount()==220,"Secondary pickup not half a normal stack");
        menu.setCarried(ItemStack.EMPTY);player.getInventory().setItem(0,ItemStack.EMPTY);
        menu.clicked(0,0,net.minecraft.world.inventory.ClickType.SWAP,player);
        h.assertTrue(player.getInventory().getItem(0).getCount()==64 && ball.getItem(0).getCount()==156,"Hotbar swap exceeded normal limit");
        h.assertTrue(menu.slots.get(menu.upgradeStart).y>menu.slots.get(11).y && menu.slots.get(menu.upgradeStart).y<menu.slots.get(menu.playerStart).y,"Upgrade row not between storage and player inventory");h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=110) public static void identicalReplacementBucketRestartsExchange(GameTestHelper h) {
        var ball=(LiquidCrystalBallBlockEntity)place(h,true);ball.tank.setFluid(new FluidStack(Fluids.LAVA,2000));ball.setItem(0,new ItemStack(Items.BUCKET));
        h.runAfterDelay(43,()->{
            h.assertTrue(ball.getItem(0).is(Items.LAVA_BUCKET),"First bucket not filled");
            ball.removeItem(0,1);ball.setItem(0,new ItemStack(Items.BUCKET));
        });
        h.runAfterDelay(86,()->{h.assertTrue(ball.getItem(0).is(Items.LAVA_BUCKET) && ball.tank.isEmpty(),"Replacement bucket did not resume");h.succeed();});
    }
    @GameTest(template="spell_arena") public static void fluidFiltersRecognizeFilledContainers(GameTestHelper h) {
        var ball=(LiquidCrystalBallBlockEntity)place(h,true);ball.upgrades.insertItem(0,new ItemStack(ModItems.INTAKE_UPGRADE.get()),false);
        ball.upgrades.filter(0,0,new ItemStack(Items.WATER_BUCKET));var tag=OrbUpgrades.settings(ball.upgrades.getStackInSlot(0));tag.putBoolean("Whitelist",true);ball.upgrades.update(0,tag);
        h.assertTrue(ball.upgrades.matches(0,ItemStack.EMPTY,new FluidStack(Fluids.WATER,1000)),"Water bucket filter rejected water");
        h.assertTrue(!ball.upgrades.matches(0,ItemStack.EMPTY,new FluidStack(Fluids.LAVA,1000)),"Water filter accepted lava");
        tag.putBoolean("Whitelist",false);ball.upgrades.update(0,tag);
        h.assertTrue(ball.upgrades.matches(0,ItemStack.EMPTY,new FluidStack(Fluids.LAVA,1000)),"Denylist inversion failed");h.succeed();
    }
    @GameTest(template="spell_arena") public static void stackingPersistsAndCannotOverflowPlayer(GameTestHelper h) {
        var ball=place(h,false);
        ball.upgrades.insertItem(0,new ItemStack(ModItems.INFUSED_STACKING_UPGRADE.get()),false);
        h.assertTrue(ball.multiplier()==4,"Wrong multiplier");
        h.assertTrue(!ball.upgrades.insertItem(1,new ItemStack(ModItems.BASIC_STACKING_UPGRADE.get()),false).isEmpty(),"Duplicate stacking family accepted");
        h.assertTrue(ball.itemHandler.insertItem(0,new ItemStack(Items.STONE,256),false).isEmpty(),"Expanded slot rejected items");
        h.assertTrue(ball.itemHandler.insertItem(0,new ItemStack(Items.STONE),false).getCount()==1,"Expanded slot overflowed");
        h.assertTrue(ball.itemHandler.insertItem(1,new ItemStack(Items.IRON_SWORD,2),false).getCount()==1,"Unstackable item stacked");
        h.assertTrue(ball.upgrades.extractItem(0,1,false).isEmpty(),"Capacity upgrade removed while overfull");
        var copy=new CrystalBallBlockEntity(ball.getBlockPos(),ball.getBlockState());copy.setLevel(h.getLevel());
        copy.loadWithComponents(ball.saveWithFullMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(copy.getItem(0).getCount()==256,"Large count lost across save codec");
        var player=h.makeMockPlayer(GameType.SURVIVAL);var menu=new CrystalBallMenu(1,player.getInventory(),ball);
        menu.clicked(0,0,net.minecraft.world.inventory.ClickType.PICKUP,player);
        h.assertTrue(menu.getCarried().getCount()==64 && ball.getItem(0).getCount()==192,"Oversized cursor stack");
        menu.setCarried(ItemStack.EMPTY);menu.quickMoveStack(player,0);
        h.assertTrue(player.getInventory().countItem(Items.STONE)==192 && ball.getItem(0).isEmpty(),"Shift extraction lost items");h.succeed();
    }
    @GameTest(template="spell_arena") public static void capacityAndRenewableSourceRules(GameTestHelper h) {
        var ball=(LiquidCrystalBallBlockEntity)place(h,true);
        h.assertTrue(ball.tank.getCapacity()==24000,"Wrong base capacity");
        h.assertTrue(ball.getContainerSize()==1 && ball.getMaxStackSize()==1 && !ball.canPlaceItem(0,new ItemStack(Items.STONE)),"Hopper could use phantom item storage in liquid orb");
        ball.upgrades.insertItem(0,new ItemStack(ModItems.INFUSED_STACKING_UPGRADE.get()),false);
        h.assertTrue(ball.tank.fill(new FluidStack(Fluids.LAVA,96000),FluidAction.EXECUTE)==96000,"Tank did not expand");
        h.assertTrue(ball.tank.fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE)==0,"Mixed fluids accepted");
        h.assertTrue(ball.upgrades.extractItem(0,1,false).isEmpty(),"Overfull fluid upgrade removable");
        h.assertTrue(ball.tank.drain(1000,FluidAction.EXECUTE).getAmount()==1000 && ball.tank.getFluidAmount()==95000,"Lava incorrectly infinite");
        ball.tank.setFluid(new FluidStack(Fluids.WATER,1000));
        h.assertTrue(ball.infinite() && ball.tank.drain(1000,FluidAction.EXECUTE).getAmount()==1000 && ball.tank.getFluidAmount()==1000,"Renewable source depleted");
        var copy=new LiquidCrystalBallBlockEntity(ball.getBlockPos(),ball.getBlockState());copy.setLevel(h.getLevel());
        copy.loadWithComponents(ball.saveWithFullMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(copy.tank.getCapacity()==96000 && copy.tank.getFluidAmount()==1000,"Fluid persistence lost");h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=80) public static void bucketTakesTwoSecondsAndStops(GameTestHelper h) {
        var ball=(LiquidCrystalBallBlockEntity)place(h,true);ball.setItem(0,new ItemStack(Items.LAVA_BUCKET));
        h.runAfterDelay(38,()->h.assertTrue(ball.getItem(0).is(Items.LAVA_BUCKET) && ball.tank.isEmpty(),"Bucket completed early"));
        h.runAfterDelay(43,()->h.assertTrue(ball.getItem(0).is(Items.BUCKET) && ball.tank.getFluidAmount()==1000,"Bucket not committed after two seconds"));
        h.runAfterDelay(65,()->{h.assertTrue(ball.getItem(0).is(Items.BUCKET) && ball.tank.getFluidAmount()==1000,"Completed bucket reversed automatically");h.succeed();});
    }
    @GameTest(template="spell_arena",timeoutTicks=80) public static void sharedPipeRateAndSimulation(GameTestHelper h) {
        var ball=(LiquidCrystalBallBlockEntity)place(h,true);
        h.runAfterDelay(22,()->{
            var handler=ball.fluidHandler;var lava=new FluidStack(Fluids.LAVA,1000);
            h.assertTrue(handler.fill(lava,FluidAction.SIMULATE)==500 && ball.tank.isEmpty(),"Simulation altered tank or limit");
            h.assertTrue(handler.fill(lava,FluidAction.EXECUTE)==500,"Wrong aggregate allowance");
            for(var direction:Direction.values()) {
                var side=h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,ball.getBlockPos(),direction);
                h.assertTrue(side!=null && side.fill(lava,FluidAction.EXECUTE)==0 && side.drain(500,FluidAction.EXECUTE).isEmpty(),"Side bypassed shared limit");
            }
        });
        h.runAfterDelay(43,()->{h.assertTrue(ball.fluidHandler.drain(1000,FluidAction.EXECUTE).getAmount()==500,"Rate failed to refill");h.succeed();});
    }
    @GameTest(template="spell_arena",timeoutTicks=230) public static void lavaFillsBucketAfterTenSeconds(GameTestHelper h) {
        var ball=place(h,false);ball.upgrades.insertItem(0,new ItemStack(ModItems.LAVA_PRODUCTION_UPGRADE.get()),false);ball.setItem(0,new ItemStack(Items.BUCKET,2));
        h.runAfterDelay(185,()->h.assertTrue(ball.getItem(1).isEmpty(),"Lava bucket completed early"));
        h.runAfterDelay(205,()->{h.assertTrue(ball.getItem(0).getCount()==1 && ball.getItem(1).is(Items.LAVA_BUCKET),"Lava production lost bucket or failed");h.succeed();});
    }
    @GameTest(template="spell_arena") public static void stabilizedDropRestoresAndResistsTrampling(GameTestHelper h) {
        var ball=place(h,false);var p=ball.getBlockPos();
        ball.upgrades.insertItem(0,new ItemStack(ModItems.SPACETIME_STABILIZATION_UPGRADE.get()),false);
        ball.upgrades.insertItem(1,new ItemStack(ModItems.INFUSED_STACKING_UPGRADE.get()),false);ball.setItem(0,new ItemStack(Items.DIAMOND,256));
        var player=h.makeMockPlayer(GameType.SURVIVAL);for(int i=0;i<12;i++)ball.landedOn(player);
        h.assertTrue(ball.impactCount()==0 && h.getLevel().getBlockEntity(p)==ball,"Stabilized orb cracked");
        var drops=Block.getDrops(ball.getBlockState(),h.getLevel(),p,ball);
        h.assertTrue(drops.size()==1 && drops.getFirst().has(DataComponents.BLOCK_ENTITY_DATA),"Portable state absent");
        h.getLevel().removeBlock(p,false);h.getLevel().setBlockAndUpdate(p,ModBlocks.CRYSTAL_BALL.get().defaultBlockState());
        BlockItem.updateCustomBlockEntityTag(h.getLevel(),player,p,drops.getFirst());
        var restored=(CrystalBallBlockEntity)h.getLevel().getBlockEntity(p);
        h.assertTrue(restored.stabilized() && restored.multiplier()==4 && restored.getItem(0).getCount()==256,"Portable contents failed to restore");h.succeed();
    }
    @GameTest(template="spell_arena") public static void heronInjectsMilkWithoutRatePenalty(GameTestHelper h) {
        var ball=(LiquidCrystalBallBlockEntity)place(h,true);var pos=ball.getBlockPos().east();
        h.getLevel().setBlockAndUpdate(pos,ModBlocks.NIGHT_HERON_STATUE.get().defaultBlockState().setValue(com.mcmagic.omnira.block.NightHeronStatueBlock.FACING,Direction.NORTH));
        ((NightHeronStatueBlockEntity)h.getLevel().getBlockEntity(pos)).ejectMilk();
        h.assertTrue(ball.tank.getFluidAmount()==1000 && ball.tank.getFluid().getFluid()==net.neoforged.neoforge.common.NeoForgeMod.MILK.get(),"Heron milk was not injected");h.succeed();
    }
    @GameTest(template="spell_arena") public static void directionalFiltersAndRoundRobin(GameTestHelper h) {
        var ball=place(h,false);var p=ball.getBlockPos();
        ball.upgrades.insertItem(0,new ItemStack(ModItems.OUTPUT_UPGRADE.get()),false);
        var tag=OrbUpgrades.settings(ball.upgrades.getStackInSlot(0));tag.putInt("Directions",3);tag.putBoolean("Whitelist",true);ball.upgrades.update(0,tag);
        ball.upgrades.filter(0,0,new ItemStack(Items.DIAMOND));ball.setItem(0,new ItemStack(Items.STONE,3));ball.setItem(1,new ItemStack(Items.DIAMOND,3));
        for(var direction:new Direction[]{Direction.UP,Direction.DOWN})h.getLevel().setBlockAndUpdate(p.relative(direction),Blocks.BARREL.defaultBlockState());
        OrbAutomation.tick(ball);OrbAutomation.tick(ball);
        for(var direction:new Direction[]{Direction.UP,Direction.DOWN}) {
            var container=(net.minecraft.world.Container)h.getLevel().getBlockEntity(p.relative(direction));
            h.assertTrue(container.countItem(Items.DIAMOND)==1 && container.countItem(Items.STONE)==0,"Filtered round robin incorrect");
        }
        h.assertTrue(ball.getItem(0).getCount()==3 && ball.getItem(1).getCount()==1,"Wrong item rate");h.succeed();
    }
}
