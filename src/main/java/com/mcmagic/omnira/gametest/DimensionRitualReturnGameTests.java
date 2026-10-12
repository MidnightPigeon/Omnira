package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.block.entity.*;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.spacetime.*;
import com.mcmagic.omnira.world.dimension.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_ritual_return") @PrefixGameTestTemplate(false)
public final class DimensionRitualReturnGameTests {
    private static ServerPlayer player(ServerLevel level){
        return new net.neoforged.neoforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"ritual-return"));
    }
    private static BlockPos bed(ServerLevel world,BlockPos foot){
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)for(int y=-1;y<=2;y++)
            world.setBlock(foot.offset(x,y,z),y==-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
        var state=Blocks.BLUE_BED.defaultBlockState().setValue(BedBlock.FACING,Direction.NORTH);
        world.setBlock(foot,state,2);world.setBlock(foot.north(),state.setValue(BedBlock.PART,BedPart.HEAD),2);
        return foot.north();
    }
    @GameTest(template="spell_arena") public static void overworldBedAndInvalidBinding(GameTestHelper h){
        var world=h.getLevel();var source=world.getServer().getLevel(ModDimensions.SPACETIME_CORRIDOR);
        h.assertTrue(source!=null,"Missing spacetime fixture");var player=player(source);
        var head=bed(world,h.absolutePos(new BlockPos(5,3,5)));player.setPos(0,129,0);
        player.setRespawnPosition(Level.OVERWORLD,head,0,false,false);
        var landing=CorridorGatewayBlockEntity.overworldReturn(source,player);
        h.assertTrue(landing!=null && landing.distanceToSqr(head.getCenter())<16,"Valid overworld bed ignored");
        world.removeBlock(head,false);
        var fallback=CorridorGatewayBlockEntity.overworldReturn(source,player);
        h.assertTrue(fallback!=null && fallback.distanceToSqr(head.getCenter())>16,"Destroyed bed remained a destination");
        player.setRespawnPosition(ModDimensions.SPACETIME_CORRIDOR,new BlockPos(0,129,0),0,true,false);
        h.assertTrue(CorridorGatewayBlockEntity.overworldReturn(source,player).equals(fallback),"Non-overworld spawn used for return");h.succeed();
    }
    @GameTest(template="spell_arena") public static void bedsMarkBothDimensions(GameTestHelper h){
        int i=0;
        for(var key:java.util.List.of(ModDimensions.DREAM_REALM,ModDimensions.SPACETIME_CORRIDOR)){
            var world=h.getLevel().getServer().getLevel(key);h.assertTrue(world!=null && world.dimensionType().bedWorks(),"Beds still explode");
            var foot=new BlockPos(24000+i++*20,130,24000);var head=bed(world,foot);var player=player(world);
            player.setPos(Vec3.atBottomCenterOf(foot.east()));player.startSleepInBed(head);
            h.assertTrue(player.getRespawnDimension()==key && head.equals(player.getRespawnPosition()),"Bed failed to mark custom-dimensional spawn");
            h.assertTrue(world.getBlockState(head).is(Blocks.BLUE_BED),"Bed was destroyed");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=200) public static void dreamRitualCreatesMappedPair(GameTestHelper h)throws Exception{
        var source=h.getLevel().getServer().getLevel(ModDimensions.DREAM_REALM);var core=new BlockPos(25000,130,25000);
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++){
            source.setBlock(core.offset(x,-1,z),Blocks.STONE.defaultBlockState(),2);
            source.setBlock(core.offset(x,0,z),Blocks.AIR.defaultBlockState(),2);
        }
        source.setBlock(core,ModBlocks.RITUAL_ENERGY_CORE.get().defaultBlockState(),2);
        source.removeBlock(core.above(),false);source.removeBlock(core.above(2),false);
        for(var a:DreamRitual.ANCHORS){
            var p=core.offset(a[0],0,a[1]);source.setBlockAndUpdate(p,ModBlocks.CRYSTAL_PEDESTAL.get().defaultBlockState());
            ((CrystalPedestalBlockEntity)source.getBlockEntity(p)).setItem(0,new ItemStack(ModItems.SPIRITUAL_CRYSTAL.get()));
        }
        var player=player(source);player.setPos(Vec3.atCenterOf(core));player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BLUE_BED));
        DreamRitual.activate(source,core,player,InteractionHand.MAIN_HAND);
        h.assertTrue(source.getBlockEntity(core.above()) instanceof DreamPortalBlockEntity,"No dream-side ritual gate");
        var portal=(DreamPortalBlockEntity)source.getBlockEntity(core.above());var saved=portal.saveWithFullMetadata(source.registryAccess());
        h.assertTrue(saved.contains("Paired") && player.getMainHandItem().isEmpty(),"No mapped pair or bed not consumed");
        var counterpart=BlockPos.of(saved.getLong("Paired"));var world=source.getServer().overworld();
        h.assertTrue(Math.abs(counterpart.getX()-core.getX())<=6 && Math.abs(counterpart.getZ()-core.getZ())<=6,"Not mapped to source XZ");
        var other=(DreamPortalBlockEntity)world.getBlockEntity(counterpart);var data=other.saveWithFullMetadata(world.registryAccess());
        h.assertTrue(data.getLong("Paired")==core.above().asLong() && data.getBoolean("DreamExit"),"Missing reverse link");
        portal.loadWithComponents(saved,source.registryAccess());
        h.assertTrue(portal.saveWithFullMetadata(source.registryAccess()).getLong("Paired")==counterpart.asLong(),"Reload lost pair");
        saved.putLong("OpenedAt",-1);portal.loadWithComponents(saved,source.registryAccess());
        var cow=net.minecraft.world.entity.EntityType.COW.create(source);
        cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(Vec3.atBottomCenterOf(core.above()));source.addFreshEntity(cow);
        h.assertTrue(DreamRitual.anchorsIntact(source,core.above()),"Source anchors invalid before travel");
        h.assertTrue(DreamPortalBlockEntity.findLanding(world,counterpart,cow)!=null,"Mapped gate has no usable landing: "+counterpart);
        var transfer=DreamPortalBlockEntity.class.getDeclaredMethod("transfer",net.minecraft.world.entity.Entity.class);transfer.setAccessible(true);
        var id=cow.getUUID();var arrived=(net.minecraft.world.entity.Entity)transfer.invoke(portal,cow);
        h.assertTrue(cow.isRemoved(),"Source entity was not transferred");
        h.assertTrue(arrived!=null && arrived.level()==world && arrived.getUUID().equals(id) && !arrived.isRemoved(),"Transfer produced no matching destination entity");
        // Remote entities need not be in the visible-entity lookup without a nearby player.
        data.putLong("OpenedAt",-1);other.loadWithComponents(data,world.registryAccess());
        arrived.getPersistentData().remove("OmniraDreamCooldown");arrived.setPos(Vec3.atBottomCenterOf(counterpart));
        var returned=(net.minecraft.world.entity.Entity)transfer.invoke(other,arrived);
        h.assertTrue(returned!=null && returned.level()==source && returned.getUUID().equals(id)
                && !returned.isRemoved() && returned.position().distanceToSqr(core.getCenter())<64,"Mapped counterpart failed reverse travel");returned.discard();
        source.removeBlock(core.above(),false);world.removeBlock(counterpart,false);h.succeed();
    }
    @GameTest(template="spell_arena") public static void returnGateLifetimeIsUnchanged(GameTestHelper h){
        var world=h.getLevel().getServer().getLevel(ModDimensions.SPACETIME_CORRIDOR);var pos=new BlockPos(26000,130,26000);
        long previous=world.getGameTime(),now=Math.max(previous,4000);
        var clock=(net.minecraft.world.level.storage.ServerLevelData)world.getServer().overworld().getLevelData();clock.setGameTime(now);
        try{
        for(boolean permanent:java.util.List.of(false,true)){
            world.setBlockAndUpdate(pos,ModBlocks.CORRIDOR_GATEWAY.get().defaultBlockState());
            var gate=(CorridorGatewayBlockEntity)world.getBlockEntity(pos);var tag=gate.saveWithFullMetadata(world.registryAccess());
            tag.putLong("Started",now-240);tag.putLong("Opened",now);tag.putBoolean("Permanent",permanent);gate.loadWithComponents(tag,world.registryAccess());gate.tick();
            h.assertTrue(world.getBlockEntity(pos)==gate && gate.open(),"Returning gate vanished immediately");
            tag.putLong("Opened",now-CorridorRitual.OPEN_TICKS);gate.loadWithComponents(tag,world.registryAccess());gate.tick();
            h.assertTrue(permanent?world.getBlockEntity(pos)==gate:world.isEmptyBlock(pos),"Returning gate lifetime changed");world.removeBlock(pos,false);
        }
        }finally{clock.setGameTime(previous);}
        h.succeed();
    }
}
