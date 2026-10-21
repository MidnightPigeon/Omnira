package com.mcmagic.omnira.pulse;

import com.mcmagic.omnira.alchemy.*;
import com.mcmagic.omnira.block.entity.*;
import com.mcmagic.omnira.engine.*;
import com.mcmagic.omnira.workstation.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import java.util.*;
import java.util.function.DoubleSupplier;

/** Optional integrations never enter this adapter; native devices exist in every environment. */
final class PulseDevices {
    record Socket(BlockPos pos,Direction face) {}
    record Device(BlockPos root,int output,List<Socket> sockets,DoubleSupplier supply,double throughput,int base,int maximum) {}
    static Device at(ServerLevel level,BlockPos pos,Direction side) {
        if(!level.hasChunkAt(pos))return null;
        var be=level.getBlockEntity(pos);var state=level.getBlockState(pos);
        if(be instanceof com.mcmagic.omnira.codex.CodexAccess){
            var face=state.getValue(com.mcmagic.omnira.codex.MyriadCodexBlock.FACING).getOpposite();
            return side==face?new Device(pos,0,List.of(new Socket(pos,face)),()->0,0,8192,16384):null;
        }
        if(be instanceof ManaEngineAccess access){
            var face=state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            if(side!=face)return null;
            var engine=access.engineState();
            return new Device(pos,0,List.of(new Socket(pos,face)),()->engine.running()?Math.max(0,engine.ratedCapacity()-external(be)):0,0,0,0);
        }
        if(state.is(SpacetimeEngineContent.BLOCK.get())){
            int output=SpacetimeEngineBlock.side(state.getValue(SpacetimeEngineBlock.PART));
            if(output<0||SpacetimeEngineBlock.OUTPUTS[output]!=side)return null;
            var controller=SpacetimeEngineBlock.controller(level,pos);if(controller==null)return null;
            return new Device(pos,output,List.of(new Socket(pos,side)),()->controller.generatedSpeed(output)!=0?Math.max(0,controller.capacity(output)-external(be)):0,0,0,0);
        }
        if(state.is(com.mcmagic.omnira.registry.ModBlocks.REACTION_POOL.get())){
            int part=state.getValue(ReactionPoolBlock.PART);var facing=state.getValue(ReactionPoolBlock.FACING);
            if((part!=3&&part!=5)||side!=ReactionPoolBlock.outward(facing,part))return null;
            var pool=ReactionPoolBlock.controller(level,pos);if(pool==null)return null;
            var root=pool.getBlockPos();var sockets=new ArrayList<Socket>();
            for(int p:new int[]{3,5})sockets.add(new Socket(root.offset(ReactionPoolBlock.offset(facing,p)),ReactionPoolBlock.outward(facing,p)));
            return new Device(root,0,sockets,()->0,0,1024,8192);
        }
        if(be instanceof AssemblyAccess){
            var face=state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();if(side!=face)return null;
            return new Device(pos,0,List.of(new Socket(pos,face)),()->0,0,256,2048);
        }
        if(state.getBlock() instanceof CrystalWorkstationBlock block&&block.shaft(state,side)){
            var work=CrystalWorkstations.work(be);if(work==null)return null;
            var sockets=new ArrayList<Socket>();for(var face:Direction.Plane.HORIZONTAL)if(block.shaft(state,face))sockets.add(new Socket(pos,face));
            return new Device(pos,0,sockets,()->0,0,256,work.kind()==CrystalWorkstations.Kind.ANVIL?2048:256);
        }
        var orb=PulseOrbEntity.from(be);if(orb!=null)return new Device(pos,0,Arrays.stream(Direction.values()).map(face->new Socket(pos,face)).toList(),()->0,orb.capacity(),0,0);
        return null;
    }
    static Device consumer(ServerLevel level,BlockPos pos) {
        var state=level.getBlockState(pos);
        if(state.is(com.mcmagic.omnira.registry.ModBlocks.REACTION_POOL.get())){
            var pool=ReactionPoolBlock.controller(level,pos);if(pool==null)return null;
            var facing=state.getValue(ReactionPoolBlock.FACING);
            return at(level,pool.getBlockPos().offset(ReactionPoolBlock.offset(facing,3)),ReactionPoolBlock.outward(facing,3));
        }
        for(var side:Direction.values()){var device=at(level,pos,side);if(device!=null&&device.base>0)return device;}
        return null;
    }
    private static double external(net.minecraft.world.level.block.entity.BlockEntity source){return net.neoforged.fml.ModList.get().isLoaded("create")?com.mcmagic.omnira.compat.create.PulseKinetics.externalLoad(source):0;}
    private PulseDevices(){}
}
