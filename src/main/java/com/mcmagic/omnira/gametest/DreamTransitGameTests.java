package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.world.dimension.ModDimensions;
import com.mcmagic.omnira.block.entity.DreamPortalBlockEntity;
import com.mcmagic.omnira.vehicle.CruiseOrbEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_dream_transit")
@PrefixGameTestTemplate(false)
public final class DreamTransitGameTests {
    @GameTest(template="spell_arena")
    public static void mechanicalStructuresCannotUsePortal(GameTestHelper h){
        int checked=0;
        for(var type:net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE){
            var id=net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if(!id.getNamespace().equals("create") || !id.getPath().contains("contraption"))continue;
            var entity=type.create(h.getLevel());if(entity==null)continue;
            h.assertTrue(!com.mcmagic.omnira.world.dimension.DreamTransitRules.allowed(entity),"Mechanical structure accepted: "+id);
            var cart=EntityType.MINECART.create(h.getLevel());
            var position=net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(new BlockPos(6,3,6)));
            cart.setPos(position);entity.setPos(position);
            entity.startRiding(cart,true);
            h.assertTrue(!com.mcmagic.omnira.world.dimension.DreamTransitRules.allowed(cart),"Carrier bypassed mechanical structure restriction");
            entity.stopRiding();entity.discard();cart.discard();checked++;
        }
        if(net.neoforged.fml.ModList.get().isLoaded("create"))h.assertTrue(checked>0,"No Create structures checked");
        h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=400)
    public static void vehiclePassengerAndCargoRoundTrip(GameTestHelper h)throws Exception{
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(6,3,6));
        level.setBlockAndUpdate(pos,ModBlocks.DREAM_PORTAL.get().defaultBlockState());
        var portal=(DreamPortalBlockEntity)level.getBlockEntity(pos);portal.initialize(true);
        var orb=ModEntityTypes.CRUISE_ORB.get().create(level);
        orb.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);level.addFreshEntity(orb);
        orb.setCore(new ItemStack(ModItems.DREAM_SPELL_CORE.get()));
        orb.storage.items.setStackInSlot(0,new ItemStack(Items.DIAMOND,17));
        orb.storage.upgrades.insertItem(0,new ItemStack(ModItems.SPEED_UPGRADE.get()),false);
        orb.storage.tank.fill(new FluidStack(Fluids.LAVA,1500),FluidAction.EXECUTE);
        var profile=new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"portal-rider");
        var player=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,profile,net.minecraft.server.level.ClientInformation.createDefault());
        player.connection=new net.neoforged.neoforge.common.util.FakePlayer(level,profile).connection;
        var channel=new io.netty.channel.embedded.EmbeddedChannel(new io.netty.channel.ChannelInboundHandlerAdapter());
        player.connection.getConnection().channelActive(channel.pipeline().firstContext());
        player.setPos(orb.position());level.addFreshEntity(player);h.assertTrue(player.startRiding(orb,true),"Initial mounting failed");
        var id=orb.getUUID();var dream=level.getServer().getLevel(ModDimensions.DREAM_REALM);
        h.runAfterDelay(50,()->h.assertTrue(!orb.isRemoved(),"Portal skipped warmup"));
        h.runAfterDelay(75,()->{
            var arrived=(CruiseOrbEntity)dream.getEntity(id);
            h.assertTrue(arrived!=null && orb.isRemoved(),"Vehicle did not cross dimensions");
            h.assertTrue(player.getVehicle()==arrived && player.level()==dream,"Passenger detached or left behind");
            h.assertTrue(arrived.core().is(ModItems.DREAM_SPELL_CORE.get()) && arrived.storage.items.getStackInSlot(0).getCount()==17
                    && arrived.storage.upgrades.getStackInSlot(0).is(ModItems.SPEED_UPGRADE.get()) && arrived.storage.tank.getFluidAmount()==1500,"Cargo or independent fittings lost");
            var returnPos=arrived.blockPosition();
            dream.setBlockAndUpdate(returnPos,ModBlocks.DREAM_PORTAL.get().defaultBlockState());
            ((DreamPortalBlockEntity)dream.getBlockEntity(returnPos)).initialize(true);
            arrived.getPersistentData().putLong("OmniraDreamCooldown",0);
            arrived.setPos(returnPos.getX()+.5,returnPos.getY(),returnPos.getZ()+.5);
            arrived.placePortalTicket(returnPos);
            player.setPos(arrived.position());
            h.runAfterDelay(75,()->{
                var returned=(CruiseOrbEntity)level.getEntity(id);
                h.assertTrue(returned!=null && player.getVehicle()==returned,"Return trip lost vehicle or rider: returned="+returned
                        +", dream="+dream.getEntity(id)+", riderLevel="+player.level().dimension().location()+", vehicle="+player.getVehicle()
                        +", origin="+arrived.getPersistentData()+", returnPortal="+dream.getBlockEntity(returnPos));
                h.assertTrue(returned.storage.items.getStackInSlot(0).getCount()==17 && returned.storage.tank.getFluidAmount()==1500,"Round trip changed storage");
                player.stopRiding();player.discard();returned.discard();channel.finishAndReleaseAll();dream.removeBlock(returnPos,false);h.succeed();
            });
        });
    }
    @GameTest(template="spell_arena",timeoutTicks=200)
    public static void mobsItemsAndCancelledWarmup(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(6,3,6));var level=h.getLevel();
        level.setBlockAndUpdate(pos,ModBlocks.DREAM_PORTAL.get().defaultBlockState());
        ((DreamPortalBlockEntity)level.getBlockEntity(pos)).initialize(true);
        var cow=h.spawn(EntityType.COW,6,3,6);cow.setNoAi(true);cow.setNoGravity(true);
        var pig=h.spawn(EntityType.PIG,6,3,6);pig.setNoAi(true);pig.setNoGravity(true);
        var item=new ItemEntity(level,pos.getX()+.5,pos.getY()+.2,pos.getZ()+.5,new ItemStack(Items.PAPER,3));item.setNoGravity(true);item.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);level.addFreshEntity(item);
        h.runAfterDelay(48,()->cow.setPos(pos.getX()+5,pos.getY(),pos.getZ()+5));
        h.runAfterDelay(80,()->{
            var dream=level.getServer().getLevel(ModDimensions.DREAM_REALM);
            h.assertTrue(!cow.isRemoved() && dream.getEntity(cow.getUUID())==null,"Leaving portal failed to cancel warmup");
            var result=dream.getEntity(item.getUUID());
            h.assertTrue(result instanceof ItemEntity moved && moved.getItem().getCount()==3,"Dropped item missing or duplicated");
            var movedPig=dream.getEntity(pig.getUUID());h.assertTrue(movedPig!=null && pig.isRemoved(),"Living mob did not transfer");
            movedPig.discard();result.discard();cow.discard();h.succeed();
        });
    }
}
