package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.block.entity.ResonanceCoreBlockEntity;
import com.mcmagic.omnira.registry.ModBlocks;
import com.mcmagic.omnira.storage.NodeTransport;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.*;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_logistics_edges")
@PrefixGameTestTemplate(false)
@EventBusSubscriber(modid="omnira")
public final class LogisticsEdgeGameTests {
    private static final Map<BlockEntity,IFluidHandler> TEST_TANKS = new IdentityHashMap<>();

    @SubscribeEvent public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlock(Capabilities.FluidHandler.BLOCK,
                (level,pos,state,entity,side) -> TEST_TANKS.get(entity), Blocks.BARREL);
    }

    @GameTest(template="spell_arena")
    public static void reactionPoolSkipsUnusableOutput(GameTestHelper h) {
        var level=h.getLevel();
        var pos=h.absolutePos(new BlockPos(5,3,5));
        var facing=net.minecraft.core.Direction.NORTH;
        h.assertTrue(com.mcmagic.omnira.alchemy.ReactionPoolBlock.place(level,pos,facing),"Pool assembly");
        var pool=com.mcmagic.omnira.alchemy.ReactionPoolBlock.controller(level,pos);
        var outputs=new java.util.ArrayList<BlockEntity>();
        for(int part:new int[]{6,8}){
            var at=pos.offset(com.mcmagic.omnira.alchemy.ReactionPoolBlock.offset(facing,part))
                    .relative(com.mcmagic.omnira.alchemy.ReactionPoolBlock.outward(facing,part));
            level.setBlockAndUpdate(at,Blocks.BARREL.defaultBlockState());
            outputs.add(level.getBlockEntity(at));
        }
        var barrel=(net.minecraft.world.Container)outputs.getFirst();
        for(int i=0;i<barrel.getContainerSize();i++)barrel.setItem(i,new net.minecraft.world.item.ItemStack(
                i==0?net.minecraft.world.item.Items.DIAMOND:net.minecraft.world.item.Items.STONE,i==0?63:64));
        pool.items.setStackInSlot(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND,4));
        var waterReceiver=new FluidTank(10000);
        waterReceiver.setFluid(new FluidStack(Fluids.WATER,1000));
        TEST_TANKS.put(outputs.getLast(),waterReceiver);
        pool.manualFluid.fill(new FluidStack(Fluids.WATER,5000),IFluidHandler.FluidAction.EXECUTE);
        pool.manualFluid.fill(new FluidStack(Fluids.LAVA,1000),IFluidHandler.FluidAction.EXECUTE);
        pool.energyInput.receiveEnergy(512,false);
        h.runAfterDelay(1,()->pool.energyInput.receiveEnergy(512,false));
        h.runAfterDelay(2,()->{
            try {
                h.assertTrue(pool.powered(),"Pool unpowered");
                h.assertTrue(barrel.getItem(0).getCount()==64 && pool.items.getStackInSlot(0).getCount()==3,"Partial item transfer");
                h.assertTrue(waterReceiver.getFluidAmount()>=3000 && pool.fluids.snapshot().getLast().getFluid()==Fluids.LAVA,
                        "Liquid transfer or layer order");
                h.assertTrue(pool.fluids.total()+waterReceiver.getFluidAmount()==7000,"Fluid conservation");
                h.succeed();
            } finally { TEST_TANKS.remove(outputs.getLast()); }
        });
    }
    @GameTest(template="spell_arena")
    public static void partialFillFromOutputOnlyTank(GameTestHelper h) {
        var source = new FluidTank(1000) {
            @Override public int fill(FluidStack fluid, FluidAction action) { return 0; }
        };
        source.setFluid(new FluidStack(Fluids.WATER,500));
        var receiver = new FluidTank(1000) {
            @Override public int fill(FluidStack fluid, FluidAction action) {
                return super.fill(fluid.copyWithAmount(action.execute() ? Math.min(125,fluid.getAmount()) : fluid.getAmount()),action);
            }
        };
        var a = new BlockPos(3,3,4);
        var b = a.east(4);
        h.setBlock(a,Blocks.BARREL); h.setBlock(b,Blocks.BARREL);
        var left=h.getBlockEntity(a); var right=h.getBlockEntity(b);
        TEST_TANKS.put(left,source); TEST_TANKS.put(right,receiver);
        try {
            h.setBlock(a.above(),ModBlocks.RESONANCE_CORE.get());
            h.setBlock(b.above(),ModBlocks.RESONANCE_CORE.get());
            var from=(ResonanceCoreBlockEntity)h.getBlockEntity(a.above());
            var to=(ResonanceCoreBlockEntity)h.getBlockEntity(b.above());
            h.assertTrue(NodeTransport.pair(from,to),"Link failed");
            NodeTransport.tick(from);
            h.assertTrue(receiver.getFluidAmount()==125,"Receiver must execute a partial fill");
            var saved=from.saveWithFullMetadata(h.getLevel().registryAccess());
            from.loadWithComponents(saved,h.getLevel().registryAccess());
            for(int i=0;i<3;i++)NodeTransport.tick(from);
            h.assertTrue(receiver.getFluidAmount()==500 && source.isEmpty(),
                    "Expected 500 mB after retry, receiver="+receiver.getFluidAmount()+" source="+source.getFluidAmount());
            h.succeed();
        } finally {
            TEST_TANKS.remove(left); TEST_TANKS.remove(right);
        }
    }
}
