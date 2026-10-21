package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.pulse.*;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.block.entity.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_pulse_shared")
@PrefixGameTestTemplate(false)
public final class PulseSharedOrbTests {
    private static PulseOrbEntity orb(GameTestHelper h,int x){var pos=new BlockPos(x,3,4);h.setBlock(pos,PulseContent.RESONANT.get());return PulseOrbEntity.from(h.getBlockEntity(pos));}
    private static void source(GameTestHelper h,int x){
        var pos=new BlockPos(x,3,3);h.setBlock(pos,ModBlocks.MANA_ENGINE.get().defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING,Direction.SOUTH));
        var engine=((ManaEngineAccess)h.getBlockEntity(pos)).engineState();engine.inventory.setItem(0,new ItemStack(ModItems.TEST_SPELL_CORE.get()));
        for(int i=1;i<=3;i++)engine.inventory.setItem(i,new ItemStack(ModItems.OMNI_CRYSTAL_GRID.get()));
    }
    private static void same(GameTestHelper h,double actual,double expected){h.assertTrue(Math.abs(actual-expected)<.001,"Expected "+expected+", got "+actual);}
    private static void tune(PulseOrbEntity orb,Item item){
        var marker=new ItemStack(Items.PAPER);marker.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("shared-orb-test:"+orb.getBlockPos().getZ()));
        orb.frequency(1,marker);orb.frequency(0,new ItemStack(item));
    }
    @GameTest(template="spell_arena") public static void frequencyAndStabilizationDoNotHalveQuota(GameTestHelper h){
        source(h,3);source(h,9);var a=orb(h,3);var b=orb(h,9);
        same(h,PulseNetworks.available(a),8192);same(h,PulseNetworks.available(b),8192);
        tune(a,Items.POPPED_CHORUS_FRUIT);tune(b,Items.POPPED_CHORUS_FRUIT);
        same(h,PulseNetworks.available(a),8192);same(h,PulseNetworks.available(b),8192);
        a.upgrade.setStackInSlot(0,new ItemStack(ModItems.SPACETIME_STABILIZATION_UPGRADE.get()));
        same(h,PulseNetworks.available(a),8192);same(h,PulseNetworks.available(b),8192);
        h.assertTrue(b.stabilized(),"Shared slot missing on second orb");
        same(h,PulseNetworks.sourceOccupied(h.getLevel(),h.absolutePos(new BlockPos(3,3,3)),Direction.SOUTH),0);h.succeed();
    }
    @GameTest(template="spell_arena") public static void directAndRemoteAliasesCarryOneSharedLoad(GameTestHelper h){
        source(h,3);var a=orb(h,3);var b=orb(h,4);tune(a,Items.BRICK);tune(b,Items.BRICK);
        same(h,PulseNetworks.available(a),8192);same(h,PulseNetworks.available(b),8192);
        var pos=new BlockPos(4,3,5);h.setBlock(pos,ModBlocks.ARCANE_ASSEMBLY_TABLE.get().defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING,Direction.SOUTH));
        same(h,PulseNetworks.power(h.getLevel(),h.absolutePos(pos)).allocated(),2048);
        same(h,PulseNetworks.occupied(a),2048);same(h,PulseNetworks.occupied(b),2048);
        same(h,PulseNetworks.available(a),6144);same(h,PulseNetworks.available(b),6144);h.succeed();
    }
    @GameTest(template="spell_arena") public static void strongestUpgradeSharedAndExcessReturned(GameTestHelper h){
        var a=orb(h,3);var b=orb(h,9);a.upgrade.setStackInSlot(0,new ItemStack(ModItems.BASIC_STACKING_UPGRADE.get()));b.upgrade.setStackInSlot(0,new ItemStack(ModItems.INFUSED_STACKING_UPGRADE.get()));
        tune(a,Items.PAPER);tune(b,Items.PAPER);same(h,a.capacity(),32768);same(h,b.capacity(),32768);
        h.runAfterDelay(1,()->{
            var drops=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(a.getBlockPos()).inflate(2));
            int count=drops.stream().filter(e->e.getItem().is(ModItems.BASIC_STACKING_UPGRADE.get())).mapToInt(e->e.getItem().getCount()).sum();
            h.assertTrue(count==1,"Expected one returned lower upgrade, got "+count);
            var simulated=a.upgrade.extractItem(0,1,true);h.assertTrue(simulated.is(ModItems.INFUSED_STACKING_UPGRADE.get())&&b.capacity()==32768,"Simulation changed shared upgrade");
            var taken=a.upgrade.extractItem(0,1,false);h.assertTrue(taken.is(ModItems.INFUSED_STACKING_UPGRADE.get())&&b.upgrade.getStackInSlot(0).isEmpty(),"Extracted plate remained on another alias");h.succeed();
        });
    }
    @GameTest(template="spell_arena") public static void detachingOrBreakingAliasDoesNotDuplicateUpgrade(GameTestHelper h){
        var a=orb(h,3);var b=orb(h,9);tune(a,Items.QUARTZ);tune(b,Items.QUARTZ);a.upgrade.setStackInSlot(0,new ItemStack(ModItems.BASIC_STACKING_UPGRADE.get()));
        tune(b,Items.AMETHYST_SHARD);h.assertTrue(b.upgrade.getStackInSlot(0).isEmpty()&&a.capacity()==16384,"Changing frequency copied upgrade");
        tune(b,Items.QUARTZ);h.getLevel().removeBlock(b.getBlockPos(),false);
        h.assertTrue(a.upgrade.extractItem(0,1,false).is(ModItems.BASIC_STACKING_UPGRADE.get()),"Breaking one alias dropped shared plate");
        h.assertTrue(h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(b.getBlockPos()).inflate(2)).stream().noneMatch(e->e.getItem().is(ModItems.BASIC_STACKING_UPGRADE.get())),"Alias destruction duplicated plate");h.succeed();
    }
    @GameTest(template="spell_arena") public static void frequencyMenuUsesNonConsumingSideSlots(GameTestHelper h){
        var a=orb(h,3);var player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());player.setPos(a.getBlockPos().getCenter());
        var menu=new PulseOrbMenu(1,player.getInventory(),a);menu.setCarried(new ItemStack(Items.DIAMOND,12));
        menu.clicked(1,0,net.minecraft.world.inventory.ClickType.PICKUP,player);
        h.assertTrue(a.frequency[0].is(Items.DIAMOND)&&menu.getCarried().getCount()==12&&menu.slots.size()==39&&menu.getSlot(1).x==184,"Frequency ghost slot moved or consumed sample");
        menu.setCarried(ItemStack.EMPTY);menu.clicked(1,0,net.minecraft.world.inventory.ClickType.PICKUP,player);h.assertTrue(a.frequency[0].isEmpty(),"Empty cursor did not reset frequency");h.succeed();
    }
    @GameTest(template="spell_arena") public static void sharedUpgradePersistsWithoutCopiesInEndpointSaves(GameTestHelper h){
        var a=orb(h,3);var b=orb(h,9);tune(a,Items.PRISMARINE_CRYSTALS);tune(b,Items.PRISMARINE_CRYSTALS);
        a.upgrade.setStackInSlot(0,new ItemStack(ModItems.INFUSED_STACKING_UPGRADE.get()));
        var registries=h.getLevel().registryAccess();
        h.assertTrue(a.saveWithFullMetadata(registries).getCompound("Upgrade").getList("Items",10).isEmpty()&&b.saveWithFullMetadata(registries).getCompound("Upgrade").getList("Items",10).isEmpty(),"Endpoint save copied shared plate");
        var restored=PulseOrbGroups.load(PulseOrbGroups.get(h.getLevel()).save(new net.minecraft.nbt.CompoundTag(),registries),registries);
        h.assertTrue(restored.view(a)==restored.view(b)&&restored.view(b).getStackInSlot(0).is(ModItems.INFUSED_STACKING_UPGRADE.get()),"Saved group lost shared ownership");
        a.setRemoved();a.clearRemoved();a.onLoad();h.assertTrue(b.capacity()==32768,"Unloading owner lost upgrade");h.succeed();
    }
    @GameTest(template="spell_arena") public static void sharedUpgradeShiftClickAndInitialClientSnapshot(GameTestHelper h){
        var a=orb(h,3);var b=orb(h,9);tune(a,Items.ECHO_SHARD);tune(b,Items.ECHO_SHARD);a.upgrade.setStackInSlot(0,new ItemStack(ModItems.BASIC_STACKING_UPGRADE.get()));
        var snapshot=new PulseOrbEntity(b.getBlockPos(),b.getBlockState());snapshot.loadWithComponents(b.getUpdateTag(h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(snapshot.upgrade.getStackInSlot(0).is(ModItems.BASIC_STACKING_UPGRADE.get()),"Initial client snapshot omitted shared upgrade");
        var player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());player.getInventory().clearContent();
        var menu=new PulseOrbMenu(1,player.getInventory(),b);h.assertTrue(menu.quickMoveStack(player,0).is(ModItems.BASIC_STACKING_UPGRADE.get())&&a.upgrade.getStackInSlot(0).isEmpty(),"Shift extraction left shared plate behind");
        same(h,a.capacity(),8192);h.succeed();
    }
    @GameTest(template="spell_arena") public static void sideFrequencyPanelFitsScaledViewports(GameTestHelper h){
        for(int width:new int[]{240,320,427,640,1280})for(int height:new int[]{135,180,240,360,640}){
            var view=com.mcmagic.omnira.client.screen.UiViewport.layout(width,height,207,166);
            int left=(view.width()-207)/2,top=(view.height()-166)/2;
            h.assertTrue(left*view.scale()>=4&&(left+207)*view.scale()<=width-4&&(top+166)*view.scale()<=height-4,"Frequency panel escaped viewport");
            for(int i=0;i<2;i++){int x=184,y=16+i*27;h.assertTrue(x>=178&&x+16<=207&&y>=10&&y+16<=68,"Frequency slot escaped side panel");}
        }h.succeed();
    }
}
