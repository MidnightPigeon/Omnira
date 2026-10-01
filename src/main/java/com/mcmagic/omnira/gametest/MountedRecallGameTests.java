package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.block.WaymarkBlock;
import com.mcmagic.omnira.block.entity.WaymarkBlockEntity;
import com.mcmagic.omnira.mana.ManaState;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.travel.RecallTravel;
import com.mcmagic.omnira.vehicle.CruiseOrbEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_mounted_recall")
@PrefixGameTestTemplate(false)
public final class MountedRecallGameTests {
    @GameTest(template="spell_arena",timeoutTicks=100) public static void normalOrb(GameTestHelper h){travel(h,false,false);}
    @GameTest(template="spell_arena",timeoutTicks=100) public static void normalBroom(GameTestHelper h){travel(h,false,true);}
    @GameTest(template="spell_arena",timeoutTicks=100) public static void enhancedOrbAcrossDimensions(GameTestHelper h){travel(h,true,false);}
    @GameTest(template="spell_arena",timeoutTicks=100) public static void enhancedBroomAcrossDimensions(GameTestHelper h){travel(h,true,true);}

    private static void travel(GameTestHelper h,boolean cross,boolean broom){
        var source=h.getLevel();
        var destination=cross?source.getServer().getLevel(Level.NETHER):source;
        var pos=cross?new BlockPos(broom?112:80,110,80):h.absolutePos(new BlockPos(7,2,7));
        for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++){
            destination.getChunkAt(pos.offset(x,0,z));
            destination.setBlockAndUpdate(pos.offset(x,-1,z),Blocks.STONE.defaultBlockState());
            for(int y=0;y<5;y++)destination.setBlockAndUpdate(pos.offset(x,y,z),Blocks.AIR.defaultBlockState());
        }
        destination.setBlock(pos,ModBlocks.WAYMARK.get().defaultBlockState(),2);
        destination.setBlock(pos.above(),ModBlocks.WAYMARK.get().defaultBlockState().setValue(WaymarkBlock.HALF,DoubleBlockHalf.UPPER),3);
        var mark=(WaymarkBlockEntity)destination.getBlockEntity(pos);
        Entity mount=broom?ModEntityTypes.CRYSTAL_BROOM.get().create(source):ModEntityTypes.CRUISE_ORB.get().create(source);
        mount.setPos(h.absoluteVec(new Vec3(1,15,1)));
        if(mount instanceof CruiseOrbEntity orb){
            orb.setCore(new ItemStack(ModItems.DREAM_SPELL_CORE.get()));
            orb.storage.items.setStackInSlot(0,new ItemStack(Items.DIAMOND,17));
            orb.storage.upgrades.insertItem(0,new ItemStack(ModItems.SPACETIME_STABILIZATION_UPGRADE.get()),false);
            orb.storage.tank.fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.LAVA,1500),
                    net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        }
        var profile=new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"recall-mounted");
        var player=new ServerPlayer(source.getServer(),source,profile,ClientInformation.createDefault());
        player.connection=new net.neoforged.neoforge.common.util.FakePlayer(source,profile).connection;
        var network=new io.netty.channel.embedded.EmbeddedChannel(new io.netty.channel.ChannelInboundHandlerAdapter());
        try {
            player.connection.getConnection().channelActive(network.pipeline().firstContext());
        } catch (Exception error) {
            throw new IllegalStateException("Cannot initialize test connection",error);
        }
        player.setPos(mount.position());h.assertTrue(player.startRiding(mount,true),"Cannot mount");
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(cross?ModItems.ENHANCED_RECALL_CRYSTAL.get():ModItems.RECALL_CRYSTAL.get()));
        player.setData(ModAttachments.MANA,new ManaState(500,500));mark.discover(player);
        h.assertTrue(RecallTravel.start(player,InteractionHand.MAIN_HAND,mark.id()),"Mounted channel refused");
        h.runAfterDelay(61,()->{
            RecallTravel.advance(player);
            Entity arrived=player.getRootVehicle();
            h.assertTrue(arrived!=player && arrived.getUUID().equals(mount.getUUID()) && arrived.level()==destination,"Vehicle/rider split or destination wrong");
            h.assertTrue(arrived.position().distanceToSqr(pos.getCenter())<70,"Vehicle did not arrive near mark");
            h.assertTrue(player.getData(ModAttachments.MANA).current()==400,"Mounted recall charged wrong amount");
            if(arrived instanceof CruiseOrbEntity orb)h.assertTrue(orb.storage.stabilized() && orb.storage.items.getStackInSlot(0).getCount()==17
                    && orb.storage.tank.getFluidAmount()==1500 && orb.core().is(ModItems.DREAM_SPELL_CORE.get()),"Vehicle cargo/core/upgrades lost");
            player.stopRiding();player.discard();arrived.discard();network.finishAndReleaseAll();destination.removeBlock(pos,false);h.succeed();
        });
    }
}
