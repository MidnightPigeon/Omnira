package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.block.entity.CrystalPedestalBlockEntity;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.spacetime.*;
import com.mcmagic.omnira.world.dimension.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_corridor_ritual")
@PrefixGameTestTemplate(false)
public final class CorridorRitualGameTests {
    private static BlockPos setup(GameTestHelper h){
        var core=h.absolutePos(new BlockPos(5,3,5));var level=h.getLevel();
        level.setBlockAndUpdate(core,ModBlocks.ADVANCED_RITUAL_ENERGY_CORE.get().defaultBlockState());
        level.removeBlock(core.above(),false);level.removeBlock(core.above(2),false);
        int i=0;
        for(var a:DreamRitual.ANCHORS){
            var pos=core.offset(a[0],0,a[1]);level.setBlockAndUpdate(pos,ModBlocks.CRYSTAL_PEDESTAL.get().defaultBlockState());
            ((CrystalPedestalBlockEntity)level.getBlockEntity(pos)).setItem(0,new ItemStack(CorridorRitual.offering(i++)));
        }
        return core;
    }
    private static com.mcmagic.omnira.entity.DreamMirror mirror(GameTestHelper h,BlockPos core){
        var mirror=ModEntityTypes.DREAM_MIRROR.get().create(h.getLevel());mirror.setPos(Vec3.atBottomCenterOf(core.above()).add(1,0,0));
        h.getLevel().addFreshEntity(mirror);return mirror;
    }
    private static net.minecraft.world.entity.player.Player player(GameTestHelper h,BlockPos core){
        var player=h.makeMockPlayer(GameType.SURVIVAL);player.setPos(Vec3.atBottomCenterOf(core).add(0,0,3));
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.UNSTABLE_SPACETIME_AGGREGATE.get()));return player;
    }
    private static void clock(GameTestHelper h,long value){
        ((net.minecraft.world.level.storage.ServerLevelData)h.getLevel().getLevelData()).setGameTime(value);
    }
    @GameTest(template="spell_arena") public static void validationAndOppositePairs(GameTestHelper h){
        var core=setup(h);var player=player(h,core);var level=h.getLevel();
        h.assertTrue(!CorridorRitual.activate(level,core,player,InteractionHand.MAIN_HAND),"Accepted ritual without mirror");
        h.assertTrue(player.getMainHandItem().getCount()==1,"Invalid ritual consumed aggregate");
        var mirror=mirror(h,core);
        level.setBlockAndUpdate(core,ModBlocks.RITUAL_ENERGY_CORE.get().defaultBlockState());
        h.assertTrue(!CorridorRitual.activate(level,core,player,InteractionHand.MAIN_HAND),"Ordinary core accepted");
        level.setBlockAndUpdate(core,ModBlocks.ADVANCED_RITUAL_ENERGY_CORE.get().defaultBlockState());
        int i=0;for(var a:DreamRitual.ANCHORS){
            var item=CorridorRitual.offering(i++);
            if(item!=ModItems.SPACETIME_KNOT.get())((CrystalPedestalBlockEntity)level.getBlockEntity(core.offset(a[0],0,a[1])))
                    .setItem(0,new ItemStack(item==ModItems.PARADOX_DUST.get()?ModItems.INFUSED_SPIRITUAL_CRYSTAL.get():ModItems.PARADOX_DUST.get()));
        }
        h.assertTrue(CorridorRitual.activate(level,core,player,InteractionHand.MAIN_HAND),"Rotated diagonal pairs rejected");
        h.assertTrue(player.getMainHandItem().isEmpty() && mirror.isRemoved(),"Aggregate or mirror duplicated at activation");
        var gateway=(CorridorGatewayBlockEntity)level.getBlockEntity(core.above());
        var data=gateway.saveWithFullMetadata(level.registryAccess());data.remove("Aggregate");gateway.loadWithComponents(data,level.registryAccess());
        level.removeBlock(core.above(),false);h.succeed();
    }
    @GameTest(template="spell_arena") public static void completionReloadAndExpiry(GameTestHelper h){
        var core=setup(h);var mirror=mirror(h,core);var id=mirror.getUUID();var player=player(h,core);var level=h.getLevel();
        player.getMainHandItem().setDamageValue(127);
        h.assertTrue(CorridorRitual.activate(level,core,player,InteractionHand.MAIN_HAND),"Could not activate");
        var gateway=(CorridorGatewayBlockEntity)level.getBlockEntity(core.above());
        for(int i=0;i<30;i++)gateway.tick();
        var saved=gateway.saveWithFullMetadata(level.registryAccess());
        h.assertTrue(ItemStack.parseOptional(level.registryAccess(),saved.getCompound("Aggregate")).getDamageValue()==127,"Accepted aggregate decayed");
        // Use a positive clock even when the test world has only just started.
        saved.putLong("Started",0);gateway.loadWithComponents(saved,level.registryAccess());
        long previous=level.getGameTime();
        try {
        clock(h,Math.max(previous,240));gateway.tick();
        h.assertTrue(gateway.open() && CorridorRitual.pedestals(level,core).size()==8,"Completion consumed pedestal offerings");
        var openSave=gateway.saveWithFullMetadata(level.registryAccess());gateway.loadWithComponents(openSave,level.registryAccess());
        h.assertTrue(gateway.open() && !openSave.contains("Aggregate"),"Open portal reload restored consumed aggregate");
        long opened=openSave.getLong("Opened");clock(h,opened+CorridorRitual.OPEN_TICKS-1);gateway.tick();
        h.assertTrue(level.getBlockEntity(core.above())==gateway,"Gateway expired early");
        clock(h,opened+CorridorRitual.OPEN_TICKS);gateway.tick();
        h.assertTrue(level.isEmptyBlock(core.above()) && level.getEntity(id) instanceof com.mcmagic.omnira.entity.DreamMirror,"Expiry did not restore mirror");
        h.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(core).inflate(3)).isEmpty(),"Successful ritual ejected aggregate");
        } finally {clock(h,previous);}
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void spacetimeCoreOpensPermanentGateway(GameTestHelper h){
        var core=setup(h);mirror(h,core);var player=player(h,core);var level=h.getLevel();
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.SPACETIME_SPELL_CORE.get()));
        player.setShiftKeyDown(true);
        level.getBlockState(core).useItemOn(player.getMainHandItem(),level,player,InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(core),net.minecraft.core.Direction.UP,core,false));
        h.assertTrue(level.getBlockEntity(core.above())==null && player.getMainHandItem().is(ModItems.SPACETIME_SPELL_CORE.get()),
                "Sneaking triggered ritual instead of allowing item placement");
        player.setShiftKeyDown(false);
        level.getBlockState(core).useItemOn(player.getMainHandItem(),level,player,InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(core),net.minecraft.core.Direction.UP,core,false));
        h.assertTrue(level.getBlockEntity(core.above()) instanceof CorridorGatewayBlockEntity,"Core right-click did not open ritual gateway");
        var gateway=(CorridorGatewayBlockEntity)level.getBlockEntity(core.above());
        var saved=gateway.saveWithFullMetadata(level.registryAccess());saved.putLong("Started",0);
        gateway.loadWithComponents(saved,level.registryAccess());
        long previous=level.getGameTime();
        try{
            clock(h,Math.max(previous,240));gateway.tick();
            h.assertTrue(gateway.open() && gateway.permanent() && CorridorRitual.pedestals(level,core).size()==8,
                    "Permanent gateway did not retain offerings");
            clock(h,Math.max(previous,240)+CorridorRitual.OPEN_TICKS+1);gateway.tick();
            h.assertTrue(level.getBlockEntity(core.above())==gateway,"Permanent gateway expired");
            level.removeBlock(core.above(),false);
            h.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(core).inflate(3)).isEmpty(),
                    "Consumed spacetime core was ejected");
        }finally{clock(h,previous);}
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void interruptionEjectsAndDetonates(GameTestHelper h){
        var core=setup(h);mirror(h,core);var player=player(h,core);var level=h.getLevel();
        h.assertTrue(CorridorRitual.activate(level,core,player,InteractionHand.MAIN_HAND),"Could not activate");
        var gateway=(CorridorGatewayBlockEntity)level.getBlockEntity(core.above());
        ((CrystalPedestalBlockEntity)level.getBlockEntity(core.offset(0,0,-3))).removeItem(0,1);gateway.tick();
        var drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(core).inflate(3));
        h.assertTrue(drops.size()==1 && UnstableAggregateItem.isAggregate(drops.getFirst().getItem()),"Interruption must eject aggregate exactly once");
        var victim=EntityType.COW.create(level);victim.setPos(Vec3.atCenterOf(core).add(2,1,0));level.addFreshEntity(victim);
        drops.getFirst().tick();h.assertTrue(victim.isRemoved(),"Ejected aggregate did not trigger storm");h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=200) public static void vehicleGroupOneWayTransit(GameTestHelper h)throws Exception{
        var level=h.getLevel();var pos=Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(5,10,5)));
        var orb=ModEntityTypes.CRUISE_ORB.get().create(level);orb.setPos(pos);level.addFreshEntity(orb);
        var profile=new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"corridor-rider");
        var player=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,profile,net.minecraft.server.level.ClientInformation.createDefault());
        player.connection=new net.neoforged.neoforge.common.util.FakePlayer(level,profile).connection;
        var channel=new io.netty.channel.embedded.EmbeddedChannel(new io.netty.channel.ChannelInboundHandlerAdapter());
        player.connection.getConnection().channelActive(channel.pipeline().firstContext());
        player.setPos(pos);level.addFreshEntity(player);h.assertTrue(player.startRiding(orb,true),"Initial mounting failed");
        orb.positionRider(player);
        orb.setCore(new ItemStack(ModItems.DREAM_SPELL_CORE.get()));
        orb.storage.items.setStackInSlot(0,new ItemStack(Items.DIAMOND,17));
        orb.storage.upgrades.insertItem(0,new ItemStack(ModItems.SPEED_UPGRADE.get()),false);
        orb.storage.tank.fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,1500),net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        var id=orb.getUUID();
        h.assertTrue(CorridorGatewayBlockEntity.transfer(level,orb),"Orb with passenger failed corridor transit");
        var world=level.getServer().getLevel(ModDimensions.SPACETIME_CORRIDOR);var arrived=world.getEntity(id);
        h.assertTrue(arrived!=null && player.getVehicle()==arrived && player.level()==world,"Passenger relation lost");
        var moved=(com.mcmagic.omnira.vehicle.CruiseOrbEntity)arrived;
        h.assertTrue(moved.core().is(ModItems.DREAM_SPELL_CORE.get()) && moved.storage.items.getStackInSlot(0).getCount()==17
                && moved.storage.upgrades.getStackInSlot(0).is(ModItems.SPEED_UPGRADE.get()) && moved.storage.tank.getFluidAmount()==1500,"Cargo or fittings lost");
        h.assertTrue(Math.abs(arrived.getX()-.5)<.01 && arrived.getY()==129,"Not in corridor interior");
        h.assertTrue(!world.getBlockState(arrived.blockPosition()).is(ModBlocks.CORRIDOR_GATEWAY.get()),"Created forbidden return gateway");
        arrived.getPersistentData().remove("OmniraCorridorCooldown");
        h.assertTrue(!CorridorGatewayBlockEntity.transfer(world,arrived),"Gateway unexpectedly supports return travel");
        player.stopRiding();player.discard();arrived.discard();channel.finishAndReleaseAll();h.succeed();
    }
}
