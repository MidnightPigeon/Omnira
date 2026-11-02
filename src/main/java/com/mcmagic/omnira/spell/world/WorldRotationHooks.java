package com.mcmagic.omnira.spell.world;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import java.util.*;
import java.util.function.*;

/** Integration API: each adapter completes at most one current job and preserves recipe checks. */
public final class WorldRotationHooks {
    private record Machine(Predicate<BlockEntity> supports,BiConsumer<BlockEntity,ServerPlayer> action){}
    private static final List<Machine> MACHINES=new ArrayList<>();
    private static final List<BiConsumer<LivingEntity,ServerPlayer>> ENTITIES=new ArrayList<>();
    public static synchronized void registerMachine(Predicate<BlockEntity> supports,BiConsumer<BlockEntity,ServerPlayer> action){MACHINES.add(new Machine(supports,action));}
    public static synchronized void registerEntity(BiConsumer<LivingEntity,ServerPlayer> action){ENTITIES.add(action);}
    public static void machine(BlockEntity machine,ServerPlayer caster){
        var level=machine.getLevel();if(level==null||machine.isRemoved())return;
        if(level instanceof net.minecraft.server.level.ServerLevel server)for(var player:server.getServer().getPlayerList().getPlayers())
            if(player.containerMenu instanceof com.mcmagic.omnira.menu.WorldRotationWork work)work.worldRotation(server,machine.getBlockPos());
        if(machine instanceof com.mcmagic.omnira.block.entity.SimpleCondensationTableBlockEntity table)table.work.worldRotation();
        if(machine instanceof com.mcmagic.omnira.block.entity.AssemblyAccess access)access.assembly().worldRotation();
        if(machine instanceof com.mcmagic.omnira.workstation.WorkstationAccess access)access.workstation().worldRotation();
        if(machine instanceof com.mcmagic.omnira.reversal.ReversalBlockEntity reversal)reversal.worldRotation();
        if(machine instanceof com.mcmagic.omnira.alchemy.ReactionPoolBlockEntity pool&&pool.controller())pool.worldRotation();
        if(machine instanceof com.mcmagic.omnira.alchemy.RoughAlchemyCauldronBlockEntity vessel)vessel.worldRotation();
        if(machine instanceof com.mcmagic.omnira.alchemy.catalysis.CatalysisBlockEntity catalysis)catalysis.worldRotation();
        if(machine instanceof com.mcmagic.omnira.forging.AdvancedForgeBlockEntity forge)forge.worldRotation();
        if(machine instanceof com.mcmagic.omnira.aggregation.AggregationRingBlockEntity ring)ring.worldRotation();
        if(machine instanceof com.mcmagic.omnira.space.sea.StarClusterOrbEntity orb)orb.worldRotation();
        if(machine instanceof com.mcmagic.omnira.block.entity.ManaEngineAccess engine)engine.engineState().fillEnergy();
        if(machine instanceof com.mcmagic.omnira.engine.SpacetimeEngineController engine)engine.energy.fill();
        if(machine instanceof net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity furnace){
            var work=(com.mcmagic.omnira.mixin.WorldRotationFurnaceAccessor)furnace;
            if(work.omnira$progress()>0){work.omnira$progress(Math.max(0,work.omnira$total()-1));net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity.serverTick(level,machine.getBlockPos(),machine.getBlockState(),furnace);}
        }
        if(machine instanceof net.minecraft.world.level.block.entity.BrewingStandBlockEntity brewing){
            var work=(com.mcmagic.omnira.mixin.WorldRotationBrewingAccessor)brewing;
            if(work.omnira$brewTime()>0){work.omnira$brewTime(1);net.minecraft.world.level.block.entity.BrewingStandBlockEntity.serverTick(level,machine.getBlockPos(),machine.getBlockState(),brewing);}
        }
        if(machine instanceof net.minecraft.world.level.block.entity.CampfireBlockEntity campfire&&machine.getBlockState().getValue(net.minecraft.world.level.block.CampfireBlock.LIT)){
            var work=(com.mcmagic.omnira.mixin.WorldRotationCampfireAccessor)campfire;
            for(int i=0;i<4;i++)if(work.omnira$progress()[i]>0)work.omnira$progress()[i]=Math.max(0,work.omnira$total()[i]-1);
            net.minecraft.world.level.block.entity.CampfireBlockEntity.cookTick(level,machine.getBlockPos(),machine.getBlockState(),campfire);
        }
        if(net.neoforged.fml.ModList.get().isLoaded("create"))com.mcmagic.omnira.compat.create.CreateWorldRotation.complete(machine);
        for(var adapter:List.copyOf(MACHINES))if(adapter.supports.test(machine))adapter.action.accept(machine,caster);
        Set<IEnergyStorage> seen=Collections.newSetFromMap(new IdentityHashMap<>());
        fill(level.getCapability(Capabilities.EnergyStorage.BLOCK,machine.getBlockPos(),null),seen);
        for(var face:Direction.values())fill(level.getCapability(Capabilities.EnergyStorage.BLOCK,machine.getBlockPos(),face),seen);
        machine.setChanged();
        level.sendBlockUpdated(machine.getBlockPos(),machine.getBlockState(),machine.getBlockState(),2);
    }
    private static void fill(IEnergyStorage energy,Set<IEnergyStorage> seen){
        if(energy==null||!seen.add(energy)||energy.getMaxEnergyStored()<=0)return;
        if(energy instanceof net.neoforged.neoforge.energy.EnergyStorage)
            ((com.mcmagic.omnira.mixin.WorldRotationEnergyAccessor)energy).omnira$energy(energy.getMaxEnergyStored());
        else for(int i=0;i<256;i++){
            int remaining=energy.getMaxEnergyStored()-energy.getEnergyStored();if(remaining<=0||energy.receiveEnergy(remaining,false)<=0)break;
        }
    }
    public static void entity(LivingEntity entity,ServerPlayer caster){for(var adapter:List.copyOf(ENTITIES))adapter.accept(entity,caster);}
    private WorldRotationHooks(){}
}
