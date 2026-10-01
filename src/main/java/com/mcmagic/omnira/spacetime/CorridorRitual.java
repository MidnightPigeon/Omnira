package com.mcmagic.omnira.spacetime;

import com.mcmagic.omnira.block.entity.CrystalPedestalBlockEntity;
import com.mcmagic.omnira.entity.DreamMirror;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.world.dimension.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.*;
import java.util.*;

/** Cardinal knots; the two diagonal pairs may exchange places. */
public final class CorridorRitual {
    public static final int FORM_TICKS=240,OPEN_TICKS=3600;
    private CorridorRitual() {}
    public static Item offering(int index) {
        var a=DreamRitual.ANCHORS[index];
        return a[0]==0 || a[1]==0?ModItems.SPACETIME_KNOT.get():
                a[0]==a[1]?ModItems.PARADOX_DUST.get():ModItems.INFUSED_SPIRITUAL_CRYSTAL.get();
    }
    public static List<CrystalPedestalBlockEntity> pedestals(ServerLevel level,BlockPos core) {
        var result=new ArrayList<CrystalPedestalBlockEntity>();
        for(var a:DreamRitual.ANCHORS) {
            var pos=core.offset(a[0],0,a[1]);
            if(!level.hasChunkAt(pos) || !level.getBlockState(pos).is(ModBlocks.CRYSTAL_PEDESTAL.get())
                    || !(level.getBlockEntity(pos) instanceof CrystalPedestalBlockEntity pedestal))return List.of();
            result.add(pedestal);
        }
        for(boolean swapped:new boolean[]{false,true}) {
            boolean valid=true;
            for(int i=0;i<8;i++) {
                Item expected=offering(i);
                if(swapped && expected!=ModItems.SPACETIME_KNOT.get()) expected=expected==ModItems.PARADOX_DUST.get()
                        ?ModItems.INFUSED_SPIRITUAL_CRYSTAL.get():ModItems.PARADOX_DUST.get();
                valid &= result.get(i).getItem(0).is(expected);
            }
            if(valid)return result;
        }
        return List.of();
    }
    public static boolean activate(ServerLevel level,BlockPos core,Player player,InteractionHand hand) {
        if(!player.isAlive() || player.isSpectator() || !(player.getItemInHand(hand).is(ModItems.UNSTABLE_SPACETIME_AGGREGATE.get())
                || player.getItemInHand(hand).is(ModItems.SPACETIME_SPELL_CORE.get())))return false;
        var center=core.above();
        var pedestals=pedestals(level,core);
        boolean valid=level.getBlockState(core).is(ModBlocks.ADVANCED_RITUAL_ENERGY_CORE.get()) && pedestals.size()==8
                && level.mayInteract(player,core) && level.mayInteract(player,center)
                && level.isEmptyBlock(center) && level.isEmptyBlock(center.above())
                && level.getServer().getLevel(ModDimensions.SPACETIME_CORRIDOR)!=null;
        for(var pedestal:pedestals)valid &= level.mayInteract(player,pedestal.getBlockPos());
        var mirror=level.getEntitiesOfClass(DreamMirror.class,new AABB(core).inflate(3,2,3),
                e->e.isAlive() && !e.isPassenger() && !e.isVehicle()).stream()
                .min(Comparator.comparingDouble(e->e.position().distanceToSqr(Vec3.atBottomCenterOf(center)))).orElse(null);
        if(!valid || mirror==null) {
            player.displayClientMessage(Component.translatable("message.omnira.corridor.invalid"),true);return false;
        }
        if(!level.setBlockAndUpdate(center,ModBlocks.CORRIDOR_GATEWAY.get().defaultBlockState()))return false;
        if(!(level.getBlockEntity(center) instanceof CorridorGatewayBlockEntity gateway)) {
            level.removeBlock(center,false);return false;
        }
        gateway.initialize(player,hand,mirror);
        return true;
    }
}
