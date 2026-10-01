package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.vehicle.*;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_cruise_orb")
@PrefixGameTestTemplate(false)
public final class CruiseOrbGameTests {
    @GameTest(template="spell_arena") public static void speedUpgradeAndCompatibility(GameTestHelper h){
        var orb=orb(h);
        for(var item:java.util.List.of(ModItems.INTAKE_UPGRADE.get(),ModItems.OUTPUT_UPGRADE.get())) {
            h.assertTrue(!orb.storage.upgrades.isItemValid(0,new ItemStack(item)),"Vehicle accepts incompatible upgrade");
            h.assertTrue(!orb.storage.upgrades.insertItem(0,new ItemStack(item),false).isEmpty(),"Insertion bypasses compatibility");
        }
        orb.storage.upgrades.insertItem(0,new ItemStack(ModItems.SPEED_UPGRADE.get()),false);
        h.assertTrue(orb.speed()==2 && !orb.storage.upgrades.isItemValid(1,new ItemStack(ModItems.SPEED_UPGRADE.get())),"Speed or duplicate rule incorrect");
        orb.setCore(new ItemStack(ModItems.TEST_SPELL_CORE.get()));h.assertTrue(orb.speed()==4,"Original core speed not doubled");
        orb.setCore(new ItemStack(ModItems.DREAM_SPELL_CORE.get()));h.assertTrue(orb.speed()==6,"Dream core speed not doubled");
        orb.storage.port.setStackInSlot(0,new ItemStack(Items.LAVA_BUCKET));
        for(int i=0;i<19;i++)orb.storage.tick();
        h.assertTrue(orb.storage.tank.isEmpty(),"Vehicle bucket transferred before one second");
        orb.storage.tick();h.assertTrue(orb.storage.tank.getFluidAmount()==1000 && orb.storage.port.getStackInSlot(0).is(Items.BUCKET),"Vehicle bucket did not transfer in one second");
        orb.storage.upgrades.extractItem(0,1,false);h.assertTrue(orb.speed()==3,"Removed upgrade still boosts speed");
        orb.discard();h.succeed();
    }
    @GameTest(template="spell_arena") public static void trashReplacementAndRetrieval(GameTestHelper h){
        var orb=orb(h);var p=rider(h,orb);var menu=new CruiseOrbMenu(1,p.getInventory(),orb);
        var pickup=net.minecraft.world.inventory.ClickType.PICKUP;
        menu.setCarried(new ItemStack(Items.DIRT,32));menu.clicked(CruiseOrbMenu.TRASH,0,pickup,p);
        for(int i=0;i<220;i++)orb.storage.tick();
        h.assertTrue(orb.storage.trash.getStackInSlot(0).getCount()==32,"Trash deleted retained item on a timer");
        menu.setCarried(new ItemStack(Items.DIRT,4));menu.clicked(CruiseOrbMenu.TRASH,0,pickup,p);
        h.assertTrue(orb.storage.trash.getStackInSlot(0).getCount()==4 && menu.getCarried().isEmpty(),"Trash merged instead of replacing previous stack");
        menu.setCarried(new ItemStack(Items.DIAMOND,3));menu.clicked(CruiseOrbMenu.TRASH,1,pickup,p);
        h.assertTrue(orb.storage.trash.getStackInSlot(0).is(Items.DIAMOND) && orb.storage.trash.getStackInSlot(0).getCount()==1 && menu.getCarried().getCount()==2,"Right-click replacement incorrect");
        menu.setCarried(ItemStack.EMPTY);menu.clicked(CruiseOrbMenu.TRASH,0,pickup,p);
        h.assertTrue(menu.getCarried().is(Items.DIAMOND) && orb.storage.trash.getStackInSlot(0).isEmpty(),"Latest trash item cannot be recovered");
        menu.setCarried(ItemStack.EMPTY);p.getInventory().setItem(0,new ItemStack(Items.STONE,8));
        menu.clicked(CruiseOrbMenu.TRASH,0,net.minecraft.world.inventory.ClickType.SWAP,p);
        h.assertTrue(orb.storage.trash.getStackInSlot(0).getCount()==8 && p.getInventory().getItem(0).isEmpty(),"Hotbar placement failed");
        menu.quickMoveStack(p,CruiseOrbMenu.TRASH);
        h.assertTrue(orb.storage.trash.getStackInSlot(0).isEmpty() && p.getInventory().countItem(Items.STONE)==8,"Shift retrieval lost trash item");
        h.assertTrue(!menu.canDragTo(menu.slots.get(CruiseOrbMenu.TRASH)),"Drag distribution can accidentally discard items");
        var core=menu.slots.get(CruiseOrbMenu.CORE);var trash=menu.slots.get(CruiseOrbMenu.TRASH);
        h.assertTrue(core.x==trash.x && core.y+trash.y+16==126,"Utility slots are not symmetric around vortex");
        p.stopRiding();orb.discard();h.succeed();
    }
    @GameTest(template="spell_arena") public static void trashBucketsAndPersistence(GameTestHelper h){
        var orb=orb(h);orb.storage.upgrades.insertItem(0,new ItemStack(ModItems.SPEED_UPGRADE.get()),false);
        for(var item:java.util.List.of(Items.WATER_BUCKET,Items.LAVA_BUCKET,Items.MILK_BUCKET,Items.COD_BUCKET,
                Items.SALMON_BUCKET,Items.TROPICAL_FISH_BUCKET,Items.PUFFERFISH_BUCKET,Items.AXOLOTL_BUCKET,Items.TADPOLE_BUCKET)){
            orb.storage.trash.setStackInSlot(0,new ItemStack(item));
            for(int i=0;i<199;i++)orb.storage.tick();
            h.assertTrue(orb.storage.trash.getStackInSlot(0).is(item),"Bucket emptied before 10 seconds");
            orb.storage.tick();h.assertTrue(orb.storage.trash.getStackInSlot(0).is(Items.BUCKET),"Filled bucket was not emptied");
        }
        orb.storage.trash.setStackInSlot(0,new ItemStack(Items.WATER_BUCKET));
        for(int i=0;i<100;i++)orb.storage.tick();
        var restored=orb(h);restored.storage.load(orb.storage.save());
        for(int i=0;i<99;i++)restored.storage.tick();
        h.assertTrue(restored.storage.trash.getStackInSlot(0).is(Items.WATER_BUCKET),"Stored timer ended early");
        restored.storage.tick();h.assertTrue(restored.storage.trash.getStackInSlot(0).is(Items.BUCKET),"Stored timer was lost");
        orb.storage.trash.extractItem(0,1,false);orb.storage.trash.insertItem(0,new ItemStack(Items.MILK_BUCKET),false);
        for(int i=0;i<100;i++)orb.storage.tick();
        h.assertTrue(orb.storage.trash.getStackInSlot(0).is(Items.MILK_BUCKET),"Replacement inherited previous bucket timer");
        h.assertTrue(!orb.storage.trash.isItemValid(0,new ItemStack(ModItems.CRUISE_ORB.get())),"Trash bypasses container restrictions");
        orb.discard();restored.discard();h.succeed();
    }
    @GameTest(template="spell_arena") public static void normalSteersOverloadStrafes(GameTestHelper h){
        var normal=orb(h);var pilot=rider(h,normal);aim(pilot,normal,0);normal.action(pilot,1);
        Vec3 start=normal.position();normal.input(pilot,8);normal.tick();
        h.assertTrue(normal.getYRot()==3 && normal.position().distanceToSqr(start)<1e-8,"Normal right key strafes instead of turning");
        normal.input(pilot,4);normal.tick();h.assertTrue(normal.getYRot()==0,"Normal left key fails to turn back");
        normal.input(pilot,4|8);normal.tick();h.assertTrue(normal.getYRot()==0,"Opposite steering keys do not cancel");
        normal.storage.upgrades.insertItem(0,new ItemStack(ModItems.SPEED_UPGRADE.get()),false);
        for(var core:java.util.List.of(ModItems.TEST_SPELL_CORE.get(),ModItems.DREAM_SPELL_CORE.get(),ModItems.LIGHT_DARK_SPELL_CORE.get())){
            normal.setCore(new ItemStack(core));float before=normal.getYRot();normal.input(pilot,8);normal.tick();
            h.assertTrue(normal.turnSpeed()==3 && normal.getYRot()-before==3,"Core or speed upgrade changed fixed turning speed");
        }
        normal.storage.upgrades.extractItem(0,1,false);normal.setCore(ItemStack.EMPTY);
        h.assertTrue(normal.turnSpeed()==3,"Removed modifiers still boost turning");
        pilot.stopRiding();normal.discard();
        var overload=orb(h);var p=rider(h,overload);overload.setCore(new ItemStack(ModItems.TEST_SPELL_CORE.get()));
        aim(p,overload,1);overload.action(p,2);start=overload.position();overload.input(p,8);overload.tick();
        h.assertTrue(overload.getYRot()==0 && overload.getX()>start.x,"Overload right key no longer strafes");
        p.stopRiding();overload.discard();h.succeed();
    }
    @GameTest(template="spell_arena") public static void collisionShattersIntoEmptyContainers(GameTestHelper h){
        var orb=orb(h);var area=orb.getBoundingBox().inflate(1);
        orb.storage.insert(new ItemStack(Items.DIAMOND,3),false);
        orb.storage.upgrades.setStackInSlot(0,new ItemStack(ModItems.BASIC_STACKING_UPGRADE.get()));
        orb.setCore(new ItemStack(ModItems.DREAM_SPELL_CORE.get()));
        orb.storage.tank.fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE);
        for(int i=0;i<9;i++){orb.impact();if(i<8)for(int tick=0;tick<10;tick++)orb.tick();}
        h.assertTrue(orb.isRemoved(),"Nine spaced collisions did not shatter vehicle");
        var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,area);
        for(var item:java.util.List.of(ModItems.CRYSTAL_BALL.get(),ModItems.LIQUID_CRYSTAL_BALL.get())){
            var balls=drops.stream().filter(e->e.getItem().is(item)).toList();
            h.assertTrue(balls.size()==1 && ItemStack.isSameItemSameComponents(balls.getFirst().getItem(),new ItemStack(item)),"Dropped inner container is missing or retains contents");
        }
        h.assertTrue(drops.stream().noneMatch(e->e.getItem().is(ModItems.CRUISE_ORB.get())),"Shatter also drops intact vehicle");
        h.assertTrue(drops.stream().filter(e->e.getItem().is(Items.GLASS_PANE)).mapToInt(e->e.getItem().getCount()).sum()==3,"Missing glass fragments");
        h.assertTrue(drops.stream().filter(e->e.getItem().is(Items.DIAMOND)).mapToInt(e->e.getItem().getCount()).sum()==3,"Contents not released exactly once");
        h.assertTrue(drops.stream().anyMatch(e->e.getItem().is(ModItems.BASIC_STACKING_UPGRADE.get())),"Upgrade not released");
        h.assertTrue(drops.stream().filter(e->e.getItem().is(ModItems.DREAM_SPELL_CORE.get())).mapToInt(e->e.getItem().getCount()).sum()==1,"Mounted core not released exactly once");
        h.assertTrue(h.getLevel().getFluidState(orb.blockPosition()).isSource(),"Stored fluid not released");
        drops.forEach(Entity->Entity.discard());h.getLevel().removeBlock(orb.blockPosition(),false);h.succeed();
    }
    @GameTest(template="spell_arena") public static void attacksDropVehicleAndCracksRepair(GameTestHelper h){
        var orb=orb(h);var p=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        for(int i=0;i<3;i++){orb.impact();for(int tick=0;tick<10;tick++)orb.tick();}
        h.assertTrue(orb.crackStage()==1,"Impact cracks missing");
        for(int tick=0;tick<600;tick++)orb.tick();
        h.assertTrue(orb.crackStage()==0 && orb.isAlive(),"Partial cracks do not repair");
        for(int tick=0;tick<400;tick++)orb.tick();
        var area=orb.getBoundingBox().inflate(1);
        for(int i=0;i<9;i++){orb.hurt(h.getLevel().damageSources().playerAttack(p),1);if(i<8)for(int tick=0;tick<10;tick++)orb.tick();}
        var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,area);
        h.assertTrue(orb.isRemoved() && drops.size()==1 && drops.getFirst().getItem().is(ModItems.CRUISE_ORB.get()),"Ordinary hits do not drop intact vehicle item");
        drops.forEach(Entity->Entity.discard());h.succeed();
    }
    @GameTest(template="spell_arena") public static void landingContactsCountOnce(GameTestHelper h){
        var orb=orb(h);var mob=net.minecraft.world.entity.EntityType.PIG.create(h.getLevel());
        mob.setNoGravity(true);mob.setPos(orb.getX(),orb.getBoundingBox().maxY,orb.getZ());h.getLevel().addFreshEntity(mob);
        for(int landing=0;landing<8;landing++){
            mob.setPos(orb.getX(),orb.getBoundingBox().maxY+1,orb.getZ());orb.tick();
            mob.setPos(orb.getX(),orb.getBoundingBox().maxY,orb.getZ());mob.setDeltaMovement(0,-.2,0);orb.tick();
            for(int tick=0;tick<12;tick++)orb.tick();
            h.assertTrue(orb.crackStage()==1+landing/3,"Incorrect three-stage landing cracks");
        }
        h.assertTrue(orb.isAlive() && orb.crackStage()==3,"Landing not counted once per contact");
        for(int tick=0;tick<30;tick++)orb.tick();
        h.assertTrue(orb.isAlive() && orb.crackStage()==3,"Standing on shell repeatedly damages it");
        mob.discard();orb.discard();h.succeed();
    }
    @GameTest(template="spell_arena") public static void sideAndBottomContactsDoNotCrack(GameTestHelper h){
        var orb=orb(h);var mob=net.minecraft.world.entity.EntityType.PIG.create(h.getLevel());
        mob.setNoGravity(true);h.getLevel().addFreshEntity(mob);
        mob.setPos(orb.getX()+1.4,orb.getY()+1,orb.getZ());mob.setDeltaMovement(-.3,0,0);
        for(int tick=0;tick<20;tick++)orb.tick();
        mob.setPos(orb.getX(),orb.getY()-.5,orb.getZ());mob.setDeltaMovement(0,.3,0);
        for(int tick=0;tick<20;tick++)orb.tick();
        h.assertTrue(!orb.hasCracks(),"Side or underside contact damaged the shell");
        mob.discard();orb.discard();h.succeed();
    }
    private static CruiseOrbEntity orb(GameTestHelper h){
        var orb=ModEntityTypes.CRUISE_ORB.get().create(h.getLevel());orb.setPos(h.absoluteVec(new Vec3(8,20,8)));return orb;
    }
    private static ServerPlayer rider(GameTestHelper h,CruiseOrbEntity orb){
        var profile=new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"cruise-test");
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),profile,net.minecraft.server.level.ClientInformation.createDefault());
        p.connection=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),profile).connection;
        p.setPos(orb.position().add(0,CruiseOrbPanel.RIDER_Y,.18));h.assertTrue(p.startRiding(orb),"Mount failed");return p;
    }
    private static void aim(ServerPlayer p,CruiseOrbEntity orb,int button){
        var panelPoint=CruiseOrbPanel.transform().transformPosition(new org.joml.Vector3f(button==0?-CruiseOrbPanel.BUTTON_X:CruiseOrbPanel.BUTTON_X,1.8F,-.9F));
        Vec3 target=orb.position().add(new Vec3(panelPoint).yRot(-orb.getYRot()*(float)Math.PI/180));
        Vec3 d=target.subtract(p.getEyePosition());
        p.setYRot((float)(Math.atan2(-d.x,d.z)*180/Math.PI));p.setXRot((float)(-Math.atan2(d.y,Math.sqrt(d.x*d.x+d.z*d.z))*180/Math.PI));
    }
    @GameTest(template="spell_arena") public static void inputAndSpeed(GameTestHelper h){
        var orb=orb(h);var p=rider(h,orb);aim(p,orb,0);
        h.assertTrue(orb.pointedButton(p)==0,"Panel ray does not hit left button");orb.action(p,1);
        h.assertTrue(orb.started() && !orb.overloaded(),"Left button not normal start");
        h.assertTrue(orb.buttonPressed(0),"Button press not synchronized");
        Vec3 before=orb.position();orb.input(p,1|8|16);orb.tick();
        h.assertTrue(Math.abs(orb.position().distanceTo(before)-.05)<1e-6,"Diagonal speed exceeds 1 m/s");
        h.assertTrue(orb.motionAxes().equals(new Vec3(0,1,1)) && orb.getYRot()==3,"Normal right key must steer, not strafe");
        orb.input(p,0);orb.tick();h.assertTrue(orb.getDeltaMovement().lengthSqr()==0,"Normal mode latches input");
        orb.tick();orb.tick();h.assertTrue(!orb.buttonPressed(0),"Button did not spring back");
        h.assertTrue(CruiseOrbEntity.latchAxis(1,-1)==0 && CruiseOrbEntity.latchAxis(0,-1)==-1,"Opposite input does not cancel latch");
        p.stopRiding();orb.discard();h.succeed();
    }
    @GameTest(template="spell_arena") public static void overloadManaAndCoast(GameTestHelper h){
        var orb=orb(h);var p=rider(h,orb);p.setData(ModAttachments.MANA,new com.mcmagic.omnira.mana.ManaState(100,100));
        aim(p,orb,1);orb.action(p,2);h.assertTrue(orb.started() && orb.overloaded(),"Right button not overload start");
        orb.input(p,1);orb.tick();orb.input(p,0);
        for(int i=0;i<19;i++)orb.tick();
        h.assertTrue(p.getData(ModAttachments.MANA).current()==80,"Overload exact per-second cost incorrect");
        h.assertTrue(orb.motionAxes().z==1,"Released key cancels overload movement");
        orb.input(p,2);orb.tick();h.assertTrue(orb.motionAxes().z==0,"Opposite key failed to cancel overload");
        orb.input(p,0);orb.tick();orb.input(p,1);orb.tick();orb.stop();
        double initial=orb.getDeltaMovement().length();orb.tick();
        h.assertTrue(!orb.started() && orb.getDeltaMovement().length()>0 && orb.getDeltaMovement().length()<initial,"Stopping lacks short coast");
        for(int i=0;i<40;i++)orb.tick();h.assertTrue(orb.getDeltaMovement().lengthSqr()==0,"Coast never settles");
        p.stopRiding();orb.discard();h.succeed();
    }
    @GameTest(template="spell_arena") public static void coresMountAndRemove(GameTestHelper h){
        var orb=orb(h);var p=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        for(var item:java.util.List.of(ModItems.TEST_SPELL_CORE.get(),ModItems.DREAM_SPELL_CORE.get(),ModItems.LIGHT_DARK_SPELL_CORE.get())){
            p.setShiftKeyDown(false);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item));orb.interact(p,InteractionHand.MAIN_HAND);
            h.assertTrue(p.getMainHandItem().isEmpty() && orb.core().is(item),"Mount duplicates or loses core");
            h.assertTrue(orb.speed()==(item==ModItems.TEST_SPELL_CORE.get()?2:3),"Core multiplier incorrect");
            p.setShiftKeyDown(true);orb.interact(p,InteractionHand.MAIN_HAND);
            h.assertTrue(p.getMainHandItem().is(item) && orb.core().isEmpty(),"Core removal failed");
        }
        orb.discard();h.succeed();
    }
    @GameTest(template="spell_arena") public static void sharedUpgradesAndPersistence(GameTestHelper h){
        var orb=orb(h);var bank=orb.storage;bank.upgrades.setStackInSlot(0,new ItemStack(ModItems.INFUSED_STACKING_UPGRADE.get()));
        h.assertTrue(bank.insert(new ItemStack(Items.PAPER,256),false).isEmpty(),"Cannot insert expanded stack");
        h.assertTrue(bank.items.getStackInSlot(0).getCount()==256 && bank.tank.getCapacity()==96000,"Upgrade is not shared");
        bank.tank.fill(new FluidStack(Fluids.LAVA,50000),FluidAction.EXECUTE);
        h.assertTrue(!bank.upgrades.isItemValid(1,new ItemStack(ModItems.BASIC_STACKING_UPGRADE.get())),"Same-family upgrades allowed");
        h.assertTrue(bank.upgrades.extractItem(0,1,false).isEmpty(),"Capacity upgrade removed while overfull");
        var copy=orb(h);copy.storage.load(bank.save());
        h.assertTrue(copy.storage.items.getStackInSlot(0).getCount()==256 && copy.storage.tank.getFluidAmount()==50000,"Large stack/fluid save lost contents");
        h.assertTrue(copy.storage.items.insertItem(1,new ItemStack(Items.IRON_SWORD,2),false).getCount()==1,"Unstackable tools stacked");
        h.assertTrue(copy.storage.insert(new ItemStack(ModItems.CRUISE_ORB.get()),true).getCount()==1,"Vehicle nesting permitted");
        orb.discard();copy.discard();h.succeed();
    }
    @GameTest(template="spell_arena") public static void manualLiquidPort(GameTestHelper h){
        var orb=orb(h);var bank=orb.storage;bank.port.setStackInSlot(0,new ItemStack(Items.LAVA_BUCKET));
        for(int i=0;i<39;i++)bank.tick();h.assertTrue(bank.tank.isEmpty(),"Bucket empties faster than 2 seconds");
        bank.tick();h.assertTrue(bank.tank.getFluidAmount()==1000 && bank.port.getStackInSlot(0).is(Items.BUCKET),"Bucket transfer failed");
        for(int i=0;i<50;i++)bank.tick();h.assertTrue(bank.tank.getFluidAmount()==1000,"Completed bucket reverses automatically");
        bank.port.setStackInSlot(0,new ItemStack(Items.BUCKET));for(int i=0;i<20;i++)bank.tick();
        bank.port.setStackInSlot(0,ItemStack.EMPTY);bank.port.setStackInSlot(0,new ItemStack(Items.BUCKET));
        for(int i=0;i<20;i++)bank.tick();h.assertTrue(bank.port.getStackInSlot(0).is(Items.BUCKET),"Port removal retained transfer credit");
        for(int i=0;i<20;i++)bank.tick();h.assertTrue(bank.port.getStackInSlot(0).is(Items.LAVA_BUCKET) && bank.tank.isEmpty(),"Bucket extraction failed");
        bank.tank.fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE);bank.tank.drain(1000,FluidAction.EXECUTE);
        h.assertTrue(bank.tank.getFluidAmount()==1000,"Renewable water not infinite");orb.discard();h.succeed();
    }
    @GameTest(template="spell_arena") public static void stabilizedBreakKeepsContents(GameTestHelper h){
        var orb=orb(h);orb.storage.upgrades.setStackInSlot(0,new ItemStack(ModItems.SPACETIME_STABILIZATION_UPGRADE.get()));
        orb.storage.insert(new ItemStack(Items.DIAMOND,3),false);orb.storage.tank.fill(new FluidStack(Fluids.LAVA,2000),FluidAction.EXECUTE);
        for(int i=0;i<12;i++)orb.impact();h.assertTrue(orb.isAlive(),"Stabilized orb shatters from impact");
        var area=orb.getBoundingBox().inflate(1);orb.destroyOrb();
        var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,area,e->e.getItem().is(ModItems.CRUISE_ORB.get()));
        h.assertTrue(drops.size()==1,"Destroyed stabilized orb not dropped exactly once");
        var restored=orb(h);restored.restoreItem(drops.getFirst().getItem());
        h.assertTrue(restored.storage.items.getStackInSlot(0).getCount()==3 && restored.storage.tank.getFluidAmount()==2000 && restored.storage.stabilized(),"Stored content lost in item");
        drops.getFirst().discard();restored.discard();h.succeed();
    }
    @GameTest(template="spell_arena") public static void goldenToiletSeatDropsWithoutStabilization(GameTestHelper h){
        var orb=orb(h);var p=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        p.setShiftKeyDown(true);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.GOLDEN_TOILET.get(),2));
        orb.interact(p,InteractionHand.MAIN_HAND);
        h.assertTrue(orb.storage.hasGoldenToilet() && p.getMainHandItem().getCount()==1,"Golden toilet was not installed exactly once");
        orb.interact(p,InteractionHand.MAIN_HAND);
        h.assertTrue(p.getMainHandItem().getCount()==1,"Duplicate toilet installation consumed another item");
        var loaded=orb(h);loaded.storage.load(orb.storage.save());
        h.assertTrue(loaded.storage.hasGoldenToilet(),"Installed toilet was lost on save/load");
        var area=orb.getBoundingBox().inflate(2);orb.destroyOrb();
        var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,area);
        h.assertTrue(drops.stream().filter(e->e.getItem().is(ModItems.GOLDEN_TOILET.get())).mapToInt(e->e.getItem().getCount()).sum()==1,
                "Unstabilized orb did not drop exactly one toilet");
        h.assertTrue(drops.stream().anyMatch(e->e.getItem().is(ModItems.CRUISE_ORB.get())),"Orb item was lost");
        drops.forEach(ItemEntity::discard);loaded.discard();h.succeed();
    }
    @GameTest(template="spell_arena") public static void goldenToiletSeatBuffAndStabilizedItem(GameTestHelper h){
        var orb=orb(h);orb.storage.installGoldenToilet(new ItemStack(ModItems.GOLDEN_TOILET.get()));
        orb.storage.upgrades.setStackInSlot(0,new ItemStack(ModItems.SPACETIME_STABILIZATION_UPGRADE.get()));
        var p=rider(h,orb);
        h.assertTrue(com.mcmagic.omnira.throne.GoldenToilet.empowered(p),"Mounted toilet does not grant its damage bonus");
        com.mcmagic.omnira.throne.GoldenToilet.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(p));
        h.assertTrue(p.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION)
                && p.hasEffect(net.minecraft.world.effect.MobEffects.SATURATION),"Mounted toilet did not grant its status effects");
        p.stopRiding();h.assertTrue(!com.mcmagic.omnira.throne.GoldenToilet.empowered(p),"Toilet bonus persisted after dismount");
        var area=orb.getBoundingBox().inflate(2);orb.destroyOrb();
        var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,area);
        h.assertTrue(drops.stream().noneMatch(e->e.getItem().is(ModItems.GOLDEN_TOILET.get())),"Stabilized orb dropped installed toilet separately");
        var saved=drops.stream().filter(e->e.getItem().is(ModItems.CRUISE_ORB.get())).findFirst().orElseThrow().getItem();
        var restored=orb(h);restored.restoreItem(saved);
        h.assertTrue(restored.storage.hasGoldenToilet(),"Stabilized orb item lost installed toilet");
        drops.forEach(ItemEntity::discard);restored.discard();h.succeed();
    }
    @GameTest(template="spell_arena") public static void vortexCollectionAndProtection(GameTestHelper h){
        var orb=orb(h);var p=rider(h,orb);p.setData(ModAttachments.MANA,new com.mcmagic.omnira.mana.ManaState(1000,1000));
        var center=BlockPos.containing(orb.getX(),orb.getY()+1.5,orb.getZ());var level=h.getLevel();
        BlockPos slab=center.east(2),chest=center.west(2),stone=center.north(2),source=center.south(2),protectedSlab=center.above(2);
        level.setBlockAndUpdate(slab,Blocks.OAK_SLAB.defaultBlockState());level.setBlockAndUpdate(chest,Blocks.CHEST.defaultBlockState());
        level.setBlockAndUpdate(stone,Blocks.STONE.defaultBlockState());level.setBlockAndUpdate(source,Blocks.LAVA.defaultBlockState());
        level.setBlockAndUpdate(protectedSlab,Blocks.OAK_SLAB.defaultBlockState());
        var forgePart=center.above();level.setBlockAndUpdate(forgePart,ModBlocks.ADVANCED_FORGE.get().defaultBlockState().setValue(com.mcmagic.omnira.forging.AdvancedForgeBlock.PART,10));
        java.util.function.Consumer<net.neoforged.neoforge.event.level.BlockEvent.BreakEvent> protect=e->{if(e.getPos().equals(protectedSlab))e.setCanceled(true);};
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(protect);
        try {
            h.assertTrue(CruiseVortex.activate(orb,p),"Vortex activation failed");
            h.assertTrue(level.getBlockState(slab).is(Blocks.OAK_SLAB) && orb.vortexTicks()==160,"Vortex collected instantly");
            for(int i=0;i<80;i++)orb.tick();
            h.assertTrue(level.getBlockState(slab).isAir() && !bankContains(orb,Items.OAK_SLAB) && level.getBlockState(source).is(Blocks.LAVA),"Expansion must drop items, not store items or fluids");
            for(var item:level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,orb.getBoundingBox().inflate(4)))item.setNoPickUpDelay();
            h.assertTrue(!CruiseVortex.activate(orb,p),"Active vortex restarted");
            for(int i=0;i<80;i++)orb.tick();
            h.assertTrue(level.getBlockState(slab).isAir() && bankContains(orb,Items.OAK_SLAB),"Slab not collected");
            h.assertTrue(level.getBlockState(chest).is(Blocks.CHEST) && level.getBlockState(stone).is(Blocks.STONE),"Vortex destroyed excluded blocks");
            h.assertTrue(level.getBlockState(protectedSlab).is(Blocks.OAK_SLAB),"Vortex bypasses protection hook");
            h.assertTrue(level.getBlockState(forgePart).is(ModBlocks.ADVANCED_FORGE.get()),"Vortex removed forge placeholder");
            h.assertTrue(orb.storage.tank.getFluidAmount()==1000 && level.getBlockState(source).isAir(),"Source not collected");
            h.assertTrue(p.getData(ModAttachments.MANA).current()==700 && orb.vortexTicks()==0,"Cost or duration incorrect");
        } finally {net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(protect);}
        for(var pos:java.util.List.of(slab,chest,stone,source,protectedSlab,forgePart))level.removeBlock(pos,false);
        p.stopRiding();orb.discard();h.succeed();
    }
    @GameTest(template="spell_arena") public static void vortexRecoversLowerAndSweptDrops(GameTestHelper h){
        var orb=orb(h);var p=rider(h,orb);var level=h.getLevel();
        p.setData(ModAttachments.MANA,new com.mcmagic.omnira.mana.ManaState(1000,1000));
        var center=orb.position().add(0,1.5,0);
        var edge=new ItemEntity(level,center.x+3.8,center.y,center.z,new ItemStack(Items.DIAMOND));
        edge.setNoGravity(true);edge.setDeltaMovement(Vec3.ZERO);edge.setNoPickUpDelay();level.addFreshEntity(edge);
        var outside=new ItemEntity(level,center.x-4.3,center.y,center.z,new ItemStack(Items.EMERALD));
        outside.setNoGravity(true);outside.setDeltaMovement(Vec3.ZERO);outside.setNoPickUpDelay();level.addFreshEntity(outside);
        h.assertTrue(CruiseVortex.activate(orb,p),"Cannot start vortex");
        for(int i=0;i<80;i++){orb.tick();edge.tick();}
        h.assertTrue(edge.position().distanceToSqr(center)>16,"Test did not exercise outward-swept boundary");
        var below=new ItemEntity(level,center.x,center.y-2.2,center.z,new ItemStack(Items.GOLD_INGOT));
        below.setDeltaMovement(Vec3.ZERO);below.setNoPickUpDelay();level.addFreshEntity(below);
        for(int i=0;i<8;i++){orb.tick();if(below.isAlive())below.tick();}
        h.assertTrue(!below.isAlive() || below.getY()>center.y-2.2,"Vortex cannot lift lower drops against gravity");
        for(int i=8;i<80;i++){orb.tick();if(below.isAlive())below.tick();if(edge.isAlive())edge.tick();}
        h.assertTrue(bankContains(orb,Items.DIAMOND) && bankContains(orb,Items.GOLD_INGOT),"Swept/lower drops missed");
        h.assertTrue(outside.isAlive() && !bankContains(orb,Items.EMERALD),"Acquisition exceeded radius four");
        edge.discard();below.discard();outside.discard();p.stopRiding();orb.discard();h.succeed();
    }
    @GameTest(template="spell_arena") public static void vortexSwallowsDropsFromEveryDirection(GameTestHelper h){
        var orb=orb(h);var p=rider(h,orb);var level=h.getLevel();
        p.setData(ModAttachments.MANA,new com.mcmagic.omnira.mana.ManaState(1000,1000));
        var center=orb.position().add(0,1.5,0);
        h.assertTrue(CruiseVortex.activate(orb,p),"Cannot start vortex");
        for(int i=0;i<80;i++)orb.tick();
        var drops=new java.util.ArrayList<ItemEntity>();
        for(var direction:net.minecraft.core.Direction.values()){
            var outward=Vec3.atLowerCornerOf(direction.getNormal());var pos=center.add(outward.scale(3.2));
            var item=new ItemEntity(level,pos.x,pos.y,pos.z,new ItemStack(Items.DIAMOND));
            item.setDeltaMovement(outward.scale(.5));item.setPickUpDelay(200);level.addFreshEntity(item);drops.add(item);
        }
        orb.tick();
        for(var item:drops){
            var toward=center.subtract(item.getBoundingBox().getCenter()).normalize();
            h.assertTrue(item.getDeltaMovement().dot(toward)>.65,"Drop not strongly pulled from "+item.position());
            double before=item.position().distanceToSqr(center);item.tick();
            h.assertTrue(item.position().distanceToSqr(center)<before,"Gravity or drift defeated suction");
            h.assertTrue(item.isAlive(),"Pickup delay was bypassed");item.setNoPickUpDelay();
        }
        for(int i=1;i<80;i++){orb.tick();for(var item:drops)if(item.isAlive())item.tick();}
        int total=0;for(int i=0;i<12;i++)if(orb.storage.items.getStackInSlot(i).is(Items.DIAMOND))total+=orb.storage.items.getStackInSlot(i).getCount();
        h.assertTrue(total==6,"Not all six directions were collected");
        for(var item:drops)item.discard();p.stopRiding();orb.discard();h.succeed();
    }
    private static boolean bankContains(CruiseOrbEntity orb,Item item){for(int i=0;i<12;i++)if(orb.storage.items.getStackInSlot(i).is(item))return true;return false;}
    @GameTest(template="spell_arena") public static void menuCoreAndLargeStacks(GameTestHelper h){
        var orb=orb(h);var p=rider(h,orb);var menu=new CruiseOrbMenu(1,p.getInventory(),orb);
        menu.setCarried(new ItemStack(ModItems.DREAM_SPELL_CORE.get()));menu.clicked(CruiseOrbMenu.CORE,0,net.minecraft.world.inventory.ClickType.PICKUP,p);
        h.assertTrue(orb.core().is(ModItems.DREAM_SPELL_CORE.get()) && menu.getCarried().isEmpty(),"Menu core mount failed");
        menu.clicked(CruiseOrbMenu.CORE,0,net.minecraft.world.inventory.ClickType.PICKUP,p);
        h.assertTrue(orb.core().isEmpty() && menu.getCarried().is(ModItems.DREAM_SPELL_CORE.get()),"Menu core removal failed");menu.setCarried(ItemStack.EMPTY);
        orb.storage.upgrades.setStackInSlot(0,new ItemStack(ModItems.INFUSED_STACKING_UPGRADE.get()));orb.storage.insert(new ItemStack(Items.PAPER,200),false);
        menu.clicked(0,0,net.minecraft.world.inventory.ClickType.PICKUP,p);
        h.assertTrue(menu.getCarried().getCount()==64 && orb.storage.items.getStackInSlot(0).getCount()==136,"Pickup leaks overstack to cursor");menu.setCarried(ItemStack.EMPTY);
        menu.quickMoveStack(p,0);h.assertTrue(orb.storage.items.getStackInSlot(0).isEmpty() && p.getInventory().countItem(Items.PAPER)==136,"Shift transfer loses oversized stack");
        p.stopRiding();orb.discard();h.succeed();
    }
    @GameTest(template="spell_arena") public static void coreEliminatesIdleOverloadCost(GameTestHelper h){
        var orb=orb(h);orb.setCore(new ItemStack(ModItems.LIGHT_DARK_SPELL_CORE.get()));var p=rider(h,orb);
        p.setData(ModAttachments.MANA,new com.mcmagic.omnira.mana.ManaState(1,100));aim(p,orb,1);orb.action(p,2);
        for(int i=0;i<45;i++)orb.tick();
        h.assertTrue(orb.overloaded() && p.getData(ModAttachments.MANA).current()==1,"Mounted core does not waive overload mana");
        orb.setCore(ItemStack.EMPTY);for(int i=0;i<20;i++)orb.tick();
        h.assertTrue(!orb.started(),"Unaffordable overload does not stop");p.stopRiding();orb.discard();h.succeed();
    }
    @GameTest(template="spell_arena") public static void fullVortexPreservesBlocks(GameTestHelper h){
        var orb=orb(h);var p=rider(h,orb);p.setData(ModAttachments.MANA,new com.mcmagic.omnira.mana.ManaState(1000,1000));
        for(int i=0;i<12;i++)orb.storage.items.setStackInSlot(i,new ItemStack(Items.STONE,64));
        orb.storage.tank.fill(new FluidStack(Fluids.LAVA,24000),FluidAction.EXECUTE);
        var center=BlockPos.containing(orb.getX(),orb.getY()+1.5,orb.getZ());var level=h.getLevel();
        BlockPos slab=center.east(2),lava=center.west(2),water=center.north(2);
        level.setBlockAndUpdate(slab,Blocks.OAK_SLAB.defaultBlockState());level.setBlockAndUpdate(lava,Blocks.LAVA.defaultBlockState());level.setBlockAndUpdate(water,Blocks.WATER.defaultBlockState());
        h.assertTrue(CruiseVortex.activate(orb,p),"Full storage should not block activation itself");
        for(int i=0;i<160;i++)orb.tick();
        h.assertTrue(level.getBlockState(slab).is(Blocks.OAK_SLAB) && level.getBlockState(lava).is(Blocks.LAVA),"Full storage destroyed collectible block or matching source");
        h.assertTrue(level.getBlockState(water).isAir() && orb.storage.tank.getFluidAmount()==24000,"Conflicting source not removed or mixed");
        for(var pos:java.util.List.of(slab,lava,water))level.removeBlock(pos,false);p.stopRiding();orb.discard();h.succeed();
    }
    @GameTest(template="spell_arena") public static void sphericalVortexPushAndGentlePull(GameTestHelper h){
        var orb=orb(h);var p=rider(h,orb);var level=h.getLevel();p.setData(ModAttachments.MANA,new com.mcmagic.omnira.mana.ManaState(1000,1000));
        var center=orb.position().add(0,1.5,0);
        var mob=net.minecraft.world.entity.EntityType.ZOMBIE.create(level);mob.setNoAi(true);mob.setPos(center.add(2,-.9,0));level.addFreshEntity(mob);
        var item=new net.minecraft.world.entity.item.ItemEntity(level,center.x+2,center.y,center.z,new ItemStack(Items.DIRT));item.setNoPickUpDelay();level.addFreshEntity(item);
        var corner=BlockPos.containing(center).offset(3,0,3);level.setBlockAndUpdate(corner,Blocks.OAK_SLAB.defaultBlockState());
        h.assertTrue(CruiseVortex.activate(orb,p),"Cannot start spherical vortex");
        for(int i=0;i<80;i++)orb.tick();
        h.assertTrue(mob.getDeltaMovement().x>.1 && mob.getDeltaMovement().x<.13,"Expansion must push once, not continuously");
        h.assertTrue(level.getBlockState(corner).is(Blocks.OAK_SLAB),"Sphere affected cube corner outside radius four");
        mob.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);item.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);orb.tick();
        h.assertTrue(mob.getDeltaMovement().x<0 && item.getDeltaMovement().x<mob.getDeltaMovement().x*3,"Contraction must pull entities gently and items more strongly");
        h.assertTrue(CruiseVortex.radius(160)==0 && CruiseVortex.radius(120)==2 && CruiseVortex.radius(80)==4 && CruiseVortex.radius(40)==2 && CruiseVortex.radius(0)==0,"Nonuniform sphere expansion");
        mob.discard();item.discard();level.removeBlock(corner,false);p.stopRiding();orb.discard();h.succeed();
    }
}
