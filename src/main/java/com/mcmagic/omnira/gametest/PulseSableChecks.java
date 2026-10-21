package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.pulse.*;
import com.mcmagic.omnira.block.entity.*;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import java.util.*;

/** Native physics fixture; never resolved by test discovery without Sable. */
final class PulseSableChecks {
    static void run(GameTestHelper h){
        var level=h.getLevel();var base=h.absolutePos(new BlockPos(5,3,5));
        level.setBlockAndUpdate(base,ModBlocks.MANA_ENGINE.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.EAST));
        level.setBlockAndUpdate(base.east(),PulseContent.ORB.get().defaultBlockState());
        (PulseOrbEntity.from(level.getBlockEntity(base.east()))).upgrade.setStackInSlot(0,new ItemStack(ModItems.BASIC_STACKING_UPGRADE.get()));
        level.setBlockAndUpdate(base.east(2),ModBlocks.ARCANE_ASSEMBLY_TABLE.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.EAST));
        var engine=((ManaEngineAccess)level.getBlockEntity(base)).engineState();
        engine.inventory.setItem(0,new ItemStack(ModItems.TEST_SPELL_CORE.get()));for(int i=1;i<=2;i++)engine.inventory.setItem(i,new ItemStack(ModItems.ARCANE_CRYSTAL_GRID.get()));
        var selected=Set.of(base,base.east(),base.east(2));
        var gathered=dev.ryanhcode.sable.api.SubLevelAssemblyHelper.gatherConnectedBlocks(base,level,32,(a,b,c,d,e)->selected.contains(c));
        h.assertTrue(gathered.assemblyState()==dev.ryanhcode.sable.api.SubLevelAssemblyHelper.GatherResult.State.SUCCESS,"Cannot gather pulse fixture");
        var ship=dev.ryanhcode.sable.api.SubLevelAssemblyHelper.assembleBlocks(level,base,gathered.blocks(),gathered.boundingBox());
        h.assertTrue(ship!=null,"Cannot assemble pulse fixture");var retained=new SableTestBody(ship);var delta=ship.getPlot().getCenterBlock().subtract(base);
        h.runAfterDelay(10,()->{
            try(retained){
                var at=base.east(2).offset(delta);var be=level.getBlockEntity(at);h.assertTrue(be instanceof AssemblyAccess,"Physical table disappeared");
                var power=PulseNetworks.power(be.getLevel(),at);h.assertTrue(power.running()&&Math.abs(power.allocated()-2048)<.001,"Native pulse failed after physical assembly: "+power);
                h.assertTrue(ship.getSelfMassTracker().getMass()==0,"Pulse relay gained physical mass");
                h.assertTrue((PulseOrbEntity.from(level.getBlockEntity(base.east().offset(delta)))).upgrade.getStackInSlot(0).is(ModItems.BASIC_STACKING_UPGRADE.get()),"Physical assembly lost relay upgrade");
                ((AssemblyAccess)be).assembly().tickPower(false);h.assertTrue(((AssemblyAccess)be).assembly().pulsePowered(),"Physical work host did not consume pulses");
                level.removeBlock(at,false);
                PulseNetworks.changed(level,at);
                var source=base.offset(delta);var output=source.east();
                var player=new net.neoforged.neoforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"pulse-ship"));
                player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                player.moveTo(ship.logicalPose().transformPosition(net.minecraft.world.phys.Vec3.atCenterOf(output)));
                player.setData(ModAttachments.MANA,new com.mcmagic.omnira.mana.ManaState(0,100));
                h.assertTrue(com.mcmagic.omnira.compat.SablePulseFeet.positions(player).contains(output),"Moving output did not resolve player foot cell");
                PulseNetworks.updatePlayers(level.getServer(),List.of(player));
                double supply=((ManaEngineAccess)level.getBlockEntity(source)).engineState().ratedCapacity();
                h.assertTrue(Math.abs(PulseNetworks.sourceOccupied(level,source,Direction.EAST)-supply)<.001,"Moving source did not reserve player absorption");
                PulseNetworks.absorbPlayers(level.getServer());
                h.assertTrue(player.getData(ModAttachments.MANA).current()==Math.ceil(supply/10),"Moving source mana: expected "+Math.ceil(supply/10)+", got "+player.getData(ModAttachments.MANA).current());
                PulseNetworks.updatePlayers(level.getServer(),List.of());h.succeed();
            }
        });
    }
}
