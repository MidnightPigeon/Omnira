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
