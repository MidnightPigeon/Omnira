package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.space.labyrinth.*;
import com.mcmagic.omnira.registry.ModAttachments;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_compass") @PrefixGameTestTemplate(false)
public final class MappingCompassGameTests {
    private static void stableBedReturn(GameTestHelper h,boolean stellar){
        var level=h.getLevel();var profile=new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"compass-native");
        var player=new ServerPlayer(level.getServer(),level,profile,ClientInformation.createDefault());
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        var channel=new io.netty.channel.embedded.EmbeddedChannel(connection);
        new net.minecraft.server.network.ServerGamePacketListenerImpl(player.server,connection,player,
                net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false)){
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet){}
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet,net.minecraft.network.PacketSendListener listener){}
        };
        var field=h.absolutePos(new BlockPos(8,4,8));var foot=h.absolutePos(new BlockPos(4,2,4));var head=foot.north();
        var bed=Blocks.BLUE_BED.defaultBlockState().setValue(BedBlock.FACING,Direction.NORTH);
        try{
            for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)level.setBlock(head.offset(x,-1,z),Blocks.STONE.defaultBlockState(),2);
            level.setBlock(foot,bed,2);level.setBlock(head,bed.setValue(BedBlock.PART,BedPart.HEAD),2);
            if(stellar){
                level.setBlockAndUpdate(field,com.mcmagic.omnira.space.sea.StellarCraftContent.ORB.get().defaultBlockState());
                var orb=(com.mcmagic.omnira.space.sea.StarClusterOrbEntity)level.getBlockEntity(field);
                orb.upgrades.setStackInSlot(1,new ItemStack(com.mcmagic.omnira.registry.ModItems.SPACETIME_STABILIZATION_UPGRADE.get()));
            }else level.setBlockAndUpdate(field,com.mcmagic.omnira.space.anchor.AnchorContent.ACTIVE.get().defaultBlockState());
            player.setRespawnPosition(level.dimension(),head,0,false,false);
            var stack=new ItemStack(LabyrinthContent.COMPASS.get());MappingCompassItem.bind(player,stack);
            var target=MappingCompassItem.destination(player,stack);
            h.assertTrue(target.pos().distanceToSqr(head.getCenter())<16,"Ordinary bed had no safe destination");
            h.assertTrue(com.mcmagic.omnira.space.anchor.StabilityField.contains(level,target.pos()),"Destination field inactive");
            // Exercise the real ServerPlayer teleport overload and its mixins, not a fake override.
            for(var origin:java.util.List.of(field.getCenter().add(2,0,0),field.getCenter().add(100,0,0))){
                player.setPos(origin);player.setData(ModAttachments.MANA,new com.mcmagic.omnira.mana.ManaState(37,200));
                h.assertTrue(!player.teleportTo(level,target.pos().x,target.pos().y,target.pos().z,java.util.Set.of(),0,0),"Ordinary teleport bypassed field");
                h.assertTrue(MappingCompassItem.returnHome(player,stack)&&player.position().distanceToSqr(target.pos())<.001,"Compass return blocked by field");
                h.assertTrue(BranchAdvancementGameTests.recorded(player,"bound_return"),"Successful bound return did not record branch");
                h.assertTrue(player.getData(ModAttachments.MANA).current()==0,"Successful stable return did not spend mana");
                h.assertTrue(!player.teleportTo(level,origin.x,origin.y,origin.z,java.util.Set.of(),0,0),"Compass exemption leaked after return");
            }
        }finally{level.setBlockAndUpdate(field,Blocks.AIR.defaultBlockState());channel.finishAndReleaseAll();}
        h.succeed();
    }
    @GameTest(template="spell_arena",batch="compass_anchor") public static void bedReturnPassesActiveAnchorOnlyDuringRecall(GameTestHelper h){stableBedReturn(h,false);}
    @GameTest(template="spell_arena",batch="compass_stellar") public static void bedReturnPassesUpgradedStellarOrbOnlyDuringRecall(GameTestHelper h){stableBedReturn(h,true);}

    @GameTest(template="spell_arena") public static void failedReturnKeepsManaAndClearsExemption(GameTestHelper h){
        var player=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"compass-failure")){
            @Override public boolean teleportTo(ServerLevel level,double x,double y,double z,java.util.Set<net.minecraft.world.entity.RelativeMovement> flags,float yaw,float pitch){return false;}
        };
        var field=h.absolutePos(new BlockPos(8,4,8));
        h.getLevel().setBlockAndUpdate(field,com.mcmagic.omnira.space.anchor.AnchorContent.ACTIVE.get().defaultBlockState());
        try{
            player.setPos(field.getCenter());player.setData(ModAttachments.MANA,new com.mcmagic.omnira.mana.ManaState(37,200));
            player.setRespawnPosition(h.getLevel().dimension(),field.above(3),0,true,false);
            var stack=new ItemStack(LabyrinthContent.COMPASS.get());MappingCompassItem.bind(player,stack);
            h.assertTrue(!MappingCompassItem.returnHome(player,stack)&&player.getData(ModAttachments.MANA).current()==37,"Failed return spent mana");
            h.assertTrue(com.mcmagic.omnira.space.anchor.StabilityField.blocks(player,h.getLevel(),field.getCenter().add(1,0,0)),"Failed return leaked exemption");
        }finally{h.getLevel().setBlockAndUpdate(field,Blocks.AIR.defaultBlockState());}
        h.succeed();
    }
    private static ServerPlayer player(GameTestHelper h){
        return new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"compass-test")){
            @Override public boolean teleportTo(ServerLevel level,double x,double y,double z,java.util.Set<net.minecraft.world.entity.RelativeMovement> flags,float yaw,float pitch){
                setServerLevel(level);setPos(x,y,z);return true;
            }
        };
    }
    @GameTest(template="spell_arena") public static void bedBindingPersistsAndMissingBedFallsBack(GameTestHelper h){
        var player=player(h);var level=h.getLevel();var foot=h.absolutePos(new BlockPos(5,2,5));var head=foot.north();
        var bed=Blocks.BLUE_BED.defaultBlockState().setValue(BedBlock.FACING,Direction.NORTH);
        level.setBlock(foot,bed,2);level.setBlock(head,bed.setValue(BedBlock.PART,BedPart.HEAD),2);
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)level.setBlock(head.offset(x,-1,z),Blocks.STONE.defaultBlockState(),2);
        player.setRespawnPosition(level.dimension(),head,20,false,false);
        var stack=new ItemStack(LabyrinthContent.COMPASS.get());h.assertTrue(MappingCompassItem.bind(player,stack),"Bed was not bound");
        var copy=stack.copy();player.setRespawnPosition(level.dimension(),head.offset(20,0,0),0,true,false);
        var target=MappingCompassItem.destination(player,copy);
        h.assertTrue(target.newLevel()==level&&target.pos().distanceToSqr(head.getCenter())<16,"Saved binding followed the player's new spawn instead");
        level.removeBlock(head,false);
        h.assertTrue(MappingCompassItem.destination(player,copy).newLevel()==player.server.overworld(),"Removed bed did not fall back");h.succeed();
    }
    @GameTest(template="spell_arena") public static void anchorWorksAcrossDimensionsWithoutSpendingCharge(GameTestHelper h){
        var player=player(h);var nether=player.server.getLevel(Level.NETHER);var pos=new BlockPos(30_000,60,30_000);
        var saved=new java.util.HashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)for(int y=-1;y<=2;y++){
            var p=pos.offset(x,y,z);saved.put(p,nether.getBlockState(p));nether.setBlock(p,y==-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
        }
        try{
            nether.setBlock(pos,Blocks.RESPAWN_ANCHOR.defaultBlockState().setValue(RespawnAnchorBlock.CHARGE,1),2);
            player.setRespawnPosition(Level.NETHER,pos,0,false,false);var stack=new ItemStack(LabyrinthContent.COMPASS.get());MappingCompassItem.bind(player,stack);
            var target=MappingCompassItem.destination(player,stack);
            h.assertTrue(target.newLevel()==nether&&target.pos().distanceToSqr(pos.getCenter())<16,"Cross-dimensional anchor unresolved");
            h.assertTrue(nether.getBlockState(pos).getValue(RespawnAnchorBlock.CHARGE)==1,"Recall consumed respawn anchor charge");
            nether.setBlock(pos,Blocks.RESPAWN_ANCHOR.defaultBlockState(),2);
            h.assertTrue(MappingCompassItem.destination(player,stack).newLevel()==player.server.overworld(),"Empty anchor remained valid");
        }finally{saved.forEach((p,s)->nether.setBlock(p,s,2));}
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void forcedSpawnFullManaAndUseCancellation(GameTestHelper h){
        var player=player(h);var stack=new ItemStack(LabyrinthContent.COMPASS.get());var pos=h.absolutePos(new BlockPos(5,3,5));
        h.getLevel().setBlock(pos,Blocks.AIR.defaultBlockState(),2);h.getLevel().setBlock(pos.above(),Blocks.AIR.defaultBlockState(),2);
        player.setRespawnPosition(h.getLevel().dimension(),pos,45,true,false);MappingCompassItem.bind(player,stack);
        var target=MappingCompassItem.destination(player,stack);
        h.assertTrue(target.pos().distanceToSqr(new net.minecraft.world.phys.Vec3(pos.getX()+.5,pos.getY()+.1,pos.getZ()+.5))<.001,"Forced non-bed spawn unsupported");
        player.setData(ModAttachments.MANA,new com.mcmagic.omnira.mana.ManaState(37,200));player.setItemInHand(InteractionHand.MAIN_HAND,stack);
        var item=LabyrinthContent.COMPASS.get();item.use(h.getLevel(),player,InteractionHand.MAIN_HAND);
        h.assertTrue(player.isUsingItem()&&item.getUseDuration(stack,player)==60,"Three-second channel missing");
        player.stopUsingItem();h.assertTrue(player.getData(ModAttachments.MANA).current()==37,"Cancelled recall spent mana");
        h.assertTrue(MappingCompassItem.returnHome(player,stack)&&player.getData(ModAttachments.MANA).current()==0,"Completed recall did not empty remaining mana");
        h.assertTrue(player.getMainHandItem()==stack&&MappingCompassItem.bound(stack),"Recall consumed the compass or binding");
        h.assertTrue(MappingCompassItem.destination(player,new ItemStack(item)).newLevel()==player.server.overworld(),"Unbound compass did not use world spawn");h.succeed();
    }
}
