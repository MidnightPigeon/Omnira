package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.pulse.*;
import com.mcmagic.omnira.block.entity.*;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("omnira_pulse")
@PrefixGameTestTemplate(false)
public final class PulseGameTests {
    @GameTest(template="spell_arena",timeoutTicks=140) public static void poolCombinesIndependentPulseInputsAndReleasesClaims(GameTestHelper h){
        var center=new BlockPos(6,3,6);var level=h.getLevel();
        h.assertTrue(com.mcmagic.omnira.alchemy.ReactionPoolBlock.place(level,h.absolutePos(center),Direction.NORTH),"Pool placement failed");
        var pool=com.mcmagic.omnira.alchemy.ReactionPoolBlock.controller(level,h.absolutePos(center));
        var sources=new ArrayList<ManaEngineState>();var positions=new ArrayList<BlockPos>();var faces=new ArrayList<Direction>();
        var relays=new ArrayList<PulseOrbEntity>();
        for(int part:new int[]{3,5}){
            var face=com.mcmagic.omnira.alchemy.ReactionPoolBlock.outward(Direction.NORTH,part);
            var relay=center.offset(com.mcmagic.omnira.alchemy.ReactionPoolBlock.offset(Direction.NORTH,part)).relative(face);
            h.setBlock(relay,PulseContent.ORB.get());relays.add(PulseOrbEntity.from(h.getBlockEntity(relay)));
            var source=relay.relative(face);faces.add(face.getOpposite());positions.add(h.absolutePos(source));sources.add(engine(h,source,face.getOpposite(),0));
        }
        h.runAfterDelay(10,()->{
            var power=PulseNetworks.power(level,pool.getBlockPos());
            h.assertTrue(power.running(),"Two separate 512 MPU inputs could not start the pool");close(h,power.allocated(),1024);
            for(int i=0;i<2;i++){close(h,PulseNetworks.sourceOccupied(level,positions.get(i),faces.get(i)),512);close(h,PulseNetworks.occupied(relays.get(i)),512);}
            for(var source:sources)for(int i=1;i<=3;i++)source.inventory.setItem(i,new ItemStack(ModItems.ARCANE_CRYSTAL_GRID.get()));
        });
        h.runAfterDelay(20,()->{
            var power=PulseNetworks.power(level,pool.getBlockPos());close(h,power.allocated(),8192);close(h,power.factor(1024),8);
            for(int i=0;i<2;i++)close(h,PulseNetworks.sourceOccupied(level,positions.get(i),faces.get(i)),4096);
            sources.get(0).enabled=false;
        });
        h.runAfterDelay(30,()->{
            var power=PulseNetworks.power(level,pool.getBlockPos());h.assertTrue(power.running(),"One live input failed after the other stopped");close(h,power.allocated(),4096);close(h,power.factor(1024),4);
            close(h,PulseNetworks.sourceOccupied(level,positions.get(0),faces.get(0)),0);close(h,PulseNetworks.sourceOccupied(level,positions.get(1),faces.get(1)),4096);
            for(int i=1;i<=3;i++)sources.get(1).inventory.setItem(i,ItemStack.EMPTY);
        });
        h.runAfterDelay(40,()->{
            var power=PulseNetworks.power(level,pool.getBlockPos());h.assertTrue(power.connected()&&!power.running(),"Pool ran below 1024 MPU combined");
            close(h,PulseNetworks.sourceOccupied(level,positions.get(1),faces.get(1)),0);
            for(int i=1;i<=3;i++){sources.get(1).inventory.setItem(i,new ItemStack(ModItems.ARCANE_CRYSTAL_GRID.get()));sources.get(0).inventory.setItem(i,ItemStack.EMPTY);}sources.get(0).enabled=true;
        });
        h.runAfterDelay(50,()->{
            close(h,PulseNetworks.power(level,pool.getBlockPos()).allocated(),4608);
            double sum=0;for(int i=0;i<2;i++)sum+=PulseNetworks.sourceOccupied(level,positions.get(i),faces.get(i));close(h,sum,4608);
            h.succeed();
        });
    }
    @GameTest(template="spell_arena") public static void wireVisualDirectionUsesSourceAndVerticalBranches(GameTestHelper h){
        engine(h,new BlockPos(5,3,2),Direction.SOUTH,0);
        var first=new BlockPos(5,3,3);var second=first.above();h.setBlock(first,PulseContent.WIRE.get());h.setBlock(second,PulseContent.WIRE.get());h.setBlock(second.south(),PulseContent.ORB.get());
        int a=PulseNetworks.visualFlow((PulseWireEntity)h.getBlockEntity(first)),b=PulseNetworks.visualFlow((PulseWireEntity)h.getBlockEntity(second));
        h.assertTrue((a&(1<<Direction.NORTH.ordinal()))!=0&&(a&(1<<(Direction.NORTH.ordinal()+6)))==0,"Wire animated toward source");
        h.assertTrue((a&(1<<(Direction.UP.ordinal()+6)))!=0&&(b&(1<<Direction.DOWN.ordinal()))!=0&&(b&(1<<(Direction.SOUTH.ordinal()+6)))!=0,"Vertical branch direction reversed");h.succeed();
    }
    @GameTest(template="spell_arena") public static void disconnectAndReconnectRebuildsQuota(GameTestHelper h){
        engine(h,new BlockPos(5,3,2),Direction.SOUTH,0);
        var at=new BlockPos(5,3,3);h.setBlock(at,PulseContent.WIRE.get());h.setBlock(new BlockPos(5,3,4),PulseContent.ORB.get());
        var orb=PulseOrbEntity.from(h.getBlockEntity(new BlockPos(5,3,4)));close(h,PulseNetworks.available(orb),512);
        h.getLevel().removeBlock(h.absolutePos(at),false);close(h,PulseNetworks.available(orb),0);
        h.setBlock(at,PulseContent.WIRE.get());close(h,PulseNetworks.available(orb),512);h.succeed();
    }
    @GameTest(template="spell_arena") public static void frequencyChangeSeparatesAndRejoinsNetwork(GameTestHelper h){
        engine(h,new BlockPos(3,3,2),Direction.SOUTH,0);
        h.setBlock(new BlockPos(3,3,3),PulseContent.RESONANT.get());h.setBlock(new BlockPos(9,3,3),PulseContent.RESONANT.get());
        var a=PulseOrbEntity.from(h.getBlockEntity(new BlockPos(3,3,3)));var b=PulseOrbEntity.from(h.getBlockEntity(new BlockPos(9,3,3)));
        a.frequency(0,new ItemStack(Items.EMERALD));b.frequency(0,new ItemStack(Items.EMERALD));a.frequency(1,new ItemStack(Items.QUARTZ));b.frequency(1,new ItemStack(Items.QUARTZ));
        table(h,new BlockPos(9,3,4),Direction.SOUTH);var pos=h.absolutePos(new BlockPos(9,3,4));
        close(h,PulseNetworks.power(h.getLevel(),pos).allocated(),512);
        b.frequency(0,new ItemStack(Items.DIAMOND));h.assertTrue(!PulseNetworks.power(h.getLevel(),pos).running(),"Old frequency retained power");
        b.frequency(0,new ItemStack(Items.EMERALD));close(h,PulseNetworks.power(h.getLevel(),pos).allocated(),512);h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=140) public static void nativePulseAfterPhysicsAssembly(GameTestHelper h){
        if(net.neoforged.fml.ModList.get().isLoaded("sable")&&net.neoforged.fml.ModList.get().isLoaded("create"))PulseSableChecks.run(h);else h.succeed();
    }
    private static PulseAllocation.Node source(double n){return new PulseAllocation.Node(n,0,0,0,0);}
    private static PulseAllocation.Node load(int base,int maximum,long order){return new PulseAllocation.Node(0,0,base,maximum,order);}
    private static void close(GameTestHelper h,double actual,double expected){h.assertTrue(Math.abs(actual-expected)<.0001,"Expected "+expected+" but got "+actual);}
    @GameTest(template="spell_arena") public static void baseBeforeAcceleration(GameTestHelper h){
        var result=PulseAllocation.solve(List.of(source(2048),load(256,2048,1),load(256,2048,2)),List.of(new PulseAllocation.Edge(0,1,1),new PulseAllocation.Edge(0,2,1)));
        h.assertTrue(result.running(),"Feasible network stopped");close(h,result.demand()[1],1792);close(h,result.demand()[2],256);h.succeed();
    }
    @GameTest(template="spell_arena") public static void wholeNetworkOverload(GameTestHelper h){
        var result=PulseAllocation.solve(List.of(source(511),load(256,2048,1),load(256,2048,2)),List.of(new PulseAllocation.Edge(0,1,1),new PulseAllocation.Edge(0,2,1)));
        h.assertTrue(!result.running(),"Overloaded network still runs");close(h,Arrays.stream(result.demand()).sum(),0);h.succeed();
    }
    @GameTest(template="spell_arena") public static void throughputAndParallelRouting(GameTestHelper h){
        var nodes=List.of(source(1024),source(1024),new PulseAllocation.Node(0,512,0,0,0),load(256,2048,1),load(256,2048,2));
        var edges=List.of(new PulseAllocation.Edge(0,2,1),new PulseAllocation.Edge(2,3,1),new PulseAllocation.Edge(1,3,1),new PulseAllocation.Edge(0,4,1));
        var result=PulseAllocation.solve(nodes,edges);h.assertTrue(result.running(),"Parallel sources could not share routing");close(h,result.demand()[3],1536);close(h,result.demand()[4],512);h.succeed();
    }
    @GameTest(template="spell_arena") public static void reverseLossOccupancy(GameTestHelper h){
        close(h,PulseAllocation.efficiency(1),.95);close(h,PulseAllocation.efficiency(20),0);close(h,PulseAllocation.efficiency(40),0);
        var result=PulseAllocation.solve(List.of(source(512*.95),load(256,256,1)),List.of(new PulseAllocation.Edge(0,1,1)));
        close(h,result.generation()[0]/.95,256/.95);h.succeed();
    }
    private static ManaEngineState engine(GameTestHelper h,BlockPos relative,Direction face,int grids){
        h.setBlock(relative,ModBlocks.MANA_ENGINE.get().defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING,face));
        var state=((ManaEngineAccess)h.getBlockEntity(relative)).engineState();state.inventory.setItem(0,new ItemStack(ModItems.TEST_SPELL_CORE.get()));
        for(int i=1;i<=grids;i++)state.inventory.setItem(i,new ItemStack(ModItems.ARCANE_CRYSTAL_GRID.get()));return state;
    }
    private static void table(GameTestHelper h,BlockPos pos,Direction facing){h.setBlock(pos,ModBlocks.ARCANE_ASSEMBLY_TABLE.get().defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING,facing));}
    @GameTest(template="spell_arena") public static void directNativePriorityAndFirstConnection(GameTestHelper h){
        engine(h,new BlockPos(5,3,6),Direction.NORTH,2);h.setBlock(new BlockPos(5,3,5),PulseContent.ORB.get());
        var a=new BlockPos(4,3,5);table(h,a,Direction.WEST);var level=h.getLevel();var first=PulseNetworks.power(level,h.absolutePos(a));close(h,first.allocated(),2048);
        var b=new BlockPos(6,3,5);table(h,b,Direction.EAST);var second=PulseNetworks.power(level,h.absolutePos(b));close(h,second.allocated(),256);close(h,PulseNetworks.power(level,h.absolutePos(a)).allocated(),1792);
        close(h,PulseNetworks.occupied(PulseOrbEntity.from(h.getBlockEntity(new BlockPos(5,3,5)))),2048);
        close(h,PulseNetworks.available(PulseOrbEntity.from(h.getBlockEntity(new BlockPos(5,3,5)))),0);
        close(h,PulseNetworks.sourceOccupied(level,h.absolutePos(new BlockPos(5,3,6)),Direction.NORTH),2048);
        var work=((AssemblyAccess)h.getBlockEntity(a)).assembly();work.tickPower(true);h.assertTrue(work.pulsePowered()&&!work.mechanicalPowered(),"Native pulse did not take priority over Create");h.succeed();
    }
    @GameTest(template="spell_arena") public static void parallelSourceAdjacencyExemption(GameTestHelper h){
        engine(h,new BlockPos(4,3,2),Direction.SOUTH,0);engine(h,new BlockPos(6,3,4),Direction.WEST,0);engine(h,new BlockPos(4,3,6),Direction.NORTH,0);
        for(var pos:List.of(new BlockPos(4,3,3),new BlockPos(5,3,4),new BlockPos(4,3,5),new BlockPos(4,3,4)))h.setBlock(pos,PulseContent.WIRE.get());
        table(h,new BlockPos(3,3,4),Direction.WEST);var result=PulseNetworks.power(h.getLevel(),h.absolutePos(new BlockPos(3,3,4)));
        h.assertTrue(result.connected()&&result.running(),"Parallel wire network disconnected");close(h,result.allocated(),1536*.95);close(h,result.occupied(),1536*.95);
        close(h,PulseNetworks.input((PulseWireEntity)h.getBlockEntity(new BlockPos(4,3,4))),1536*.95);h.succeed();
    }
    @GameTest(template="spell_arena") public static void noExternalBypassWhenPulseInsufficient(GameTestHelper h){
        var source=engine(h,new BlockPos(4,3,5),Direction.NORTH,0);source.enabled=false;table(h,new BlockPos(4,3,4),Direction.NORTH);
        var work=((AssemblyAccess)h.getBlockEntity(new BlockPos(4,3,4))).assembly();work.tickPower(true);
        h.assertTrue(!work.powered()&&work.pulseInsufficient(),"Insufficient pulse silently fell back to mechanical power");h.succeed();
    }
    @GameTest(template="spell_arena") public static void relayUpgradeCompatibilityAndFrequency(GameTestHelper h){
        h.setBlock(new BlockPos(3,3,3),PulseContent.ORB.get());h.setBlock(new BlockPos(6,3,3),PulseContent.RESONANT.get());
        var ordinary=PulseOrbEntity.from(h.getBlockEntity(new BlockPos(3,3,3)));var resonant=PulseOrbEntity.from(h.getBlockEntity(new BlockPos(6,3,3)));
        var stabilization=new ItemStack(ModItems.SPACETIME_STABILIZATION_UPGRADE.get());
        h.assertTrue(!ordinary.accepts(stabilization)&&resonant.accepts(stabilization),"Relay accepted an incompatible upgrade");resonant.upgrade.setStackInSlot(0,stabilization);close(h,resonant.capacity(),8192);
        resonant.frequency(0,new ItemStack(Items.DIAMOND));var saved=resonant.saveWithFullMetadata(h.getLevel().registryAccess());var copy=new PulseOrbEntity(resonant.getBlockPos(),resonant.getBlockState());copy.loadWithComponents(saved,h.getLevel().registryAccess());copy.setLevel(h.getLevel());
        h.assertTrue(copy.frequency[0].is(Items.DIAMOND)&&copy.stabilized(),"Relay lost frequency or upgrade on reload");h.succeed();
    }
    @GameTest(template="spell_arena") public static void relayReadoutRespectsUpstreamBottleneck(GameTestHelper h){
        var engine=engine(h,new BlockPos(5,3,3),Direction.SOUTH,0);for(int i=1;i<=3;i++)engine.inventory.setItem(i,new ItemStack(ModItems.OMNI_CRYSTAL_GRID.get()));
        h.setBlock(new BlockPos(5,3,4),PulseContent.ORB.get());h.setBlock(new BlockPos(5,3,5),PulseContent.ORB.get());
        var high=PulseOrbEntity.from(h.getBlockEntity(new BlockPos(5,3,5)));high.upgrade.setStackInSlot(0,new ItemStack(ModItems.BASIC_STACKING_UPGRADE.get()));
        close(h,PulseNetworks.available(high),4096);h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=40) public static void inputReadingsRemainLiveWhenConsumersOverload(GameTestHelper h){
        var source=engine(h,new BlockPos(5,3,2),Direction.SOUTH,0);
        h.setBlock(new BlockPos(5,3,3),PulseContent.WIRE.get());h.setBlock(new BlockPos(5,3,4),PulseContent.ORB.get());
        for(var face:List.of(Direction.EAST,Direction.WEST,Direction.SOUTH))table(h,new BlockPos(5,3,4).relative(face),face);
        var orb=PulseOrbEntity.from(h.getBlockEntity(new BlockPos(5,3,4)));var wire=(PulseWireEntity)h.getBlockEntity(new BlockPos(5,3,3));
        h.assertTrue(!PulseNetworks.power(h.getLevel(),h.absolutePos(new BlockPos(6,3,4))).running(),"Overload fixture still runs");
        close(h,PulseNetworks.available(orb),512);close(h,PulseNetworks.input(wire),512);
        close(h,PulseNetworks.occupied(orb),0);close(h,PulseNetworks.sourceOccupied(h.getLevel(),h.absolutePos(new BlockPos(5,3,2)),Direction.SOUTH),0);
        source.enabled=false;
        h.runAfterDelay(1,()->{close(h,PulseNetworks.available(orb),0);close(h,PulseNetworks.input(wire),0);h.succeed();});
    }
    @GameTest(template="spell_arena") public static void wireRingDoesNotMultiplySourceReading(GameTestHelper h){
        engine(h,new BlockPos(5,3,2),Direction.SOUTH,0);
        for(int x=4;x<=6;x++)for(int z=3;z<=5;z++)if(x!=5||z!=4)h.setBlock(new BlockPos(x,3,z),PulseContent.WIRE.get());
        h.setBlock(new BlockPos(5,3,6),PulseContent.ORB.get());
        close(h,PulseNetworks.available(PulseOrbEntity.from(h.getBlockEntity(new BlockPos(5,3,6)))),512*.8);
        close(h,PulseNetworks.input((PulseWireEntity)h.getBlockEntity(new BlockPos(5,3,5))),512*.8);h.succeed();
    }
    @GameTest(template="spell_arena") public static void readoutIgnoresBranchesWithoutAnInputSource(GameTestHelper h){
        engine(h,new BlockPos(5,3,2),Direction.SOUTH,0);h.setBlock(new BlockPos(5,3,3),PulseContent.WIRE.get());
        h.setBlock(new BlockPos(5,3,4),PulseContent.ORB.get());h.setBlock(new BlockPos(5,3,5),PulseContent.WIRE.get());h.setBlock(new BlockPos(5,3,6),PulseContent.WIRE.get());
        var orb=PulseOrbEntity.from(h.getBlockEntity(new BlockPos(5,3,4)));close(h,PulseNetworks.available(orb),256);
        close(h,PulseNetworks.input((PulseWireEntity)h.getBlockEntity(new BlockPos(5,3,6))),256*.9);h.succeed();
    }
    @GameTest(template="spell_arena") public static void sharedFrequencyHasOneThroughputLimit(GameTestHelper h){
        for(int x:new int[]{3,9}){
            var source=engine(h,new BlockPos(x,3,2),Direction.SOUTH,0);for(int i=1;i<=3;i++)source.inventory.setItem(i,new ItemStack(ModItems.OMNI_CRYSTAL_GRID.get()));
            for(int z:new int[]{3,10}){var at=new BlockPos(x,3,z);h.setBlock(at,PulseContent.RESONANT.get());var orb=PulseOrbEntity.from(h.getBlockEntity(at));orb.frequency(0,new ItemStack(Items.SAND));orb.frequency(1,new ItemStack(Items.REDSTONE));}
            var center=new BlockPos(x,3,10);for(var face:Direction.Plane.HORIZONTAL)table(h,center.relative(face),face);
        }
        double total=0;for(int x:new int[]{3,9})for(var face:Direction.Plane.HORIZONTAL)total+=PulseNetworks.power(h.getLevel(),h.absolutePos(new BlockPos(x,3,10).relative(face))).allocated();
        close(h,total,8192);h.succeed();
    }
    @GameTest(template="spell_arena") public static void sharedSourcesCannotInventAvailablePotential(GameTestHelper h){
        for(int x:new int[]{3,7}){var source=engine(h,new BlockPos(x,3,2),Direction.SOUTH,2);source.inventory.setItem(3,new ItemStack(ModItems.OMNI_CRYSTAL_GRID.get()));}
        for(int x:new int[]{3,7,11}){
            var pos=new BlockPos(x,3,3);h.setBlock(pos,PulseContent.RESONANT.get());var orb=PulseOrbEntity.from(h.getBlockEntity(pos));orb.frequency(0,new ItemStack(Items.AMETHYST_SHARD));orb.frequency(1,new ItemStack(Items.QUARTZ));
            for(var face:Direction.Plane.HORIZONTAL)if(x==11||face!=Direction.NORTH)table(h,pos.relative(face),face);
        }
        double total=0;for(int x:new int[]{3,7,11})for(var face:Direction.Plane.HORIZONTAL)if(x==11||face!=Direction.NORTH)total+=PulseNetworks.power(h.getLevel(),h.absolutePos(new BlockPos(x,3,3).relative(face))).allocated();
        close(h,total,8192);for(int x:new int[]{3,7,11})close(h,PulseNetworks.available(PulseOrbEntity.from(h.getBlockEntity(new BlockPos(x,3,3)))),0);h.succeed();
    }
    @GameTest(template="spell_arena") public static void accumulatorRecipesAndUpgradePreservation(GameTestHelper h){
        var manager=h.getLevel().getRecipeManager();var inventory=new net.minecraft.world.SimpleContainer(7);var input=new com.mcmagic.omnira.recipe.AssemblyRecipe.Input(inventory);
        var recipe=(com.mcmagic.omnira.recipe.AssemblyRecipe)manager.byKey(net.minecraft.resources.ResourceLocation.parse("omnira:assembly/pulse_accumulator_orb")).orElseThrow().value();
        for(int i=0;i<6;i++)inventory.setItem(i,new ItemStack(i%2==0?ModItems.ARCANE_DUST.get():ModItems.INFUSED_SPIRITUAL_CRYSTAL.get()));inventory.setItem(6,new ItemStack(ModItems.CRYSTAL_BALL.get()));
        h.assertTrue(recipe.matches(input,h.getLevel())&&recipe.assemble(input,h.getLevel().registryAccess()).is(PulseContent.ORB_ITEM.get()),"Pulse recipe missing or not shapeless");
        inventory.getItem(6).set(net.minecraft.core.component.DataComponents.CONTAINER,net.minecraft.world.item.component.ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND))));
        h.assertTrue(!recipe.matches(input,h.getLevel()),"Pulse recipe swallowed stored items");
        h.setBlock(new BlockPos(2,3,2),PulseContent.ORB.get());var orb=PulseOrbEntity.from(h.getBlockEntity(new BlockPos(2,3,2)));orb.upgrade.setStackInSlot(0,new ItemStack(ModItems.BASIC_STACKING_UPGRADE.get()));
        var source=new ItemStack(PulseContent.ORB_ITEM.get());source.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.CustomData.of(orb.saveWithFullMetadata(h.getLevel().registryAccess())));
        for(int i=0;i<6;i++)inventory.setItem(i,new ItemStack(ModItems.RESONANCE_CRYSTAL.get()));inventory.setItem(6,source);
        recipe=(com.mcmagic.omnira.recipe.AssemblyRecipe)manager.byKey(net.minecraft.resources.ResourceLocation.parse("omnira:assembly/resonant_accumulator_orb")).orElseThrow().value();
        h.assertTrue(recipe.matches(input,h.getLevel()),"Serialized upgrade source rejected");var result=recipe.assemble(input,h.getLevel().registryAccess());
        h.assertTrue(result.is(PulseContent.RESONANT_ITEM.get())&&result.getComponents().equals(source.getComponents()),"Accumulator upgrade lost state");h.succeed();
    }
    @GameTest(template="spell_arena") public static void remotePortsCarryNativePotential(GameTestHelper h){
        engine(h,new BlockPos(3,3,3),Direction.EAST,0);table(h,new BlockPos(9,3,8),Direction.SOUTH);
        var level=h.getLevel();var from=h.absolutePos(new BlockPos(4,3,3));var to=h.absolutePos(new BlockPos(9,3,7));
        level.setBlockAndUpdate(from,ModBlocks.SPACETIME_PORT.get().defaultBlockState().setValue(com.mcmagic.omnira.space.SpacetimePortBlock.FACING,Direction.EAST));
        level.setBlockAndUpdate(to,ModBlocks.SPACETIME_PORT.get().defaultBlockState().setValue(com.mcmagic.omnira.space.SpacetimePortBlock.FACING,Direction.NORTH));
        var a=com.mcmagic.omnira.space.SpacetimePortState.at(level,from);var b=com.mcmagic.omnira.space.SpacetimePortState.at(level,to);
        h.assertTrue(com.mcmagic.omnira.space.SpacetimePortState.pair(a,b),"Native port pairing failed");close(h,PulseNetworks.power(level,h.absolutePos(new BlockPos(9,3,8))).allocated(),512);
        close(h,PulseOutput.mode(level,h.absolutePos(new BlockPos(3,3,3)),Direction.EAST),PulseOutput.PULSE);h.succeed();
    }
    @GameTest(template="spell_arena") public static void remoteWireUsesLocalPortFaceAndInvalidatesPair(GameTestHelper h){
        engine(h,new BlockPos(3,3,3),Direction.EAST,0);
        var wire=new BlockPos(9,3,8);h.setBlock(wire,PulseContent.WIRE.get());h.setBlock(wire.south(),PulseContent.ORB.get());
        var level=h.getLevel();var from=h.absolutePos(new BlockPos(4,3,3));var to=h.absolutePos(wire.north());
        level.setBlockAndUpdate(from,ModBlocks.SPACETIME_PORT.get().defaultBlockState().setValue(com.mcmagic.omnira.space.SpacetimePortBlock.FACING,Direction.EAST));
        level.setBlockAndUpdate(to,ModBlocks.SPACETIME_PORT.get().defaultBlockState().setValue(com.mcmagic.omnira.space.SpacetimePortBlock.FACING,Direction.NORTH));
        var a=com.mcmagic.omnira.space.SpacetimePortState.at(level,from);var b=com.mcmagic.omnira.space.SpacetimePortState.at(level,to);
        h.assertTrue(com.mcmagic.omnira.space.SpacetimePortState.pair(a,b),"Port pairing failed");
        int flow=PulseNetworks.visualFlow((PulseWireEntity)h.getBlockEntity(wire));
        h.assertTrue((flow&(1<<Direction.NORTH.ordinal()))!=0&&(flow&(1<<(Direction.SOUTH.ordinal()+6)))!=0,"Remote source coordinates replaced the physical input face");
        h.assertTrue(PulseOutput.nativeConnection(level,h.absolutePos(new BlockPos(3,3,3)),Direction.EAST),"Remote native line also permits FE conversion");
        a.unlink();close(h,PulseNetworks.input((PulseWireEntity)h.getBlockEntity(wire)),0);close(h,PulseNetworks.visualFlow((PulseWireEntity)h.getBlockEntity(wire)),0);h.succeed();
    }
    @GameTest(template="spell_arena") public static void stabilizedFrequencyRequiredAcrossDimensions(GameTestHelper h){
        var level=h.getLevel();var remote=level.getServer().getLevel(net.minecraft.world.level.Level.NETHER);h.assertTrue(remote!=null,"Missing vanilla Nether");
        var aPos=h.absolutePos(new BlockPos(5,3,5));h.setBlock(new BlockPos(5,3,5),PulseContent.RESONANT.get());engine(h,new BlockPos(5,3,6),Direction.NORTH,0);
        var bPos=new BlockPos(aPos.getX(),80,aPos.getZ());remote.getChunkAt(bPos);remote.setBlockAndUpdate(bPos,PulseContent.RESONANT.get().defaultBlockState());
        var a=PulseOrbEntity.from(level.getBlockEntity(aPos));var b=PulseOrbEntity.from(remote.getBlockEntity(bPos));a.frequency(0,new ItemStack(Items.CLAY_BALL));b.frequency(0,new ItemStack(Items.CLAY_BALL));a.frequency(1,new ItemStack(Items.QUARTZ));b.frequency(1,new ItemStack(Items.QUARTZ));
        var target=bPos.east();remote.setBlockAndUpdate(target,ModBlocks.ARCANE_ASSEMBLY_TABLE.get().defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING,Direction.EAST));
        PulseNetworks.available(b);h.assertTrue(!PulseNetworks.power(remote,target).running(),"Unstabilized frequency crossed dimensions");
        a.upgrade.setStackInSlot(0,new ItemStack(ModItems.SPACETIME_STABILIZATION_UPGRADE.get()));close(h,PulseNetworks.power(remote,target).allocated(),512);
        h.assertTrue(b.stabilized(),"Stabilization was not shared");
        var plate=b.upgrade.extractItem(0,1,false);h.assertTrue(plate.is(ModItems.SPACETIME_STABILIZATION_UPGRADE.get())&&!a.stabilized(),"Remote removal copied the shared upgrade");
        h.assertTrue(!PulseNetworks.power(remote,target).running(),"Removing shared stabilization retained cross-dimension power");
        remote.removeBlock(target,false);remote.removeBlock(bPos,false);h.succeed();
    }
}
