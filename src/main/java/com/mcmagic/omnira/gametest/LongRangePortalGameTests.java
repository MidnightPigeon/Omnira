package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.space.labyrinth.*;
import com.mcmagic.omnira.spacetime.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_long_range_portal") @PrefixGameTestTemplate(false)
public final class LongRangePortalGameTests {
    @GameTest(template="spell_arena") public static void closedRoofsHaveUpwardPortals(GameTestHelper h){
        int closed=0,open=0;
        for(int x=-40;x<=40;x++)for(int z=-40;z<=40;z++){
            var room=new LabyrinthTerrain.Room(x,2,z);if(!LabyrinthTerrain.exists(room))continue;
            var plan=LabyrinthTerrain.plan(room);long portals=plan.blocks().values().stream().filter(s->s.is(LabyrinthContent.LONG_RANGE_PORTAL.get())).count();
            if(LabyrinthTerrain.entryRoof(room)){h.assertTrue(portals==0,"Diggable roof has escape portal");open++;}
            else{
                h.assertTrue(portals==1,"Closed room lacks upward portal");closed++;
                for(var cell:plan.blocks().entrySet())if(cell.getValue().is(LabyrinthContent.LONG_RANGE_PORTAL.get())){
                    h.assertTrue(cell.getKey().getY()==40&&cell.getValue().getValue(VoidPortalBlock.FACING)==Direction.UP,"Portal is not in ceiling");
                    h.assertTrue(CorridorLayout.block(cell.getKey().getX(),40,cell.getKey().getZ()).is(LabyrinthContent.LONG_RANGE_PORTAL.get()),"Generated roof overwrites portal");
                    h.assertTrue(!plan.doors().containsKey(Direction.UP),"Escape portal treated as maze random exit");
                }
            }
        }
        h.assertTrue(closed>20&&open>5,"Not enough roof samples");h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=200) public static void deepCliffsHaveUncoveredSandVortices(GameTestHelper h){
        int samples=0;
        search:for(int x=-12;x<=12;x++)for(int z=-12;z<=12;z++){
            var portal=LongRangeTerrain.portal(x,z);if(portal==null)continue;
            h.assertTrue(StrataBiomeSource.epochalCliffs(portal.getX(),portal.getZ())&&portal.getY()==171,"Portal outside deep cliffs");
            var state=CorridorLayout.block(portal.getX(),portal.getY(),portal.getZ());
            h.assertTrue(state.is(LabyrinthContent.LONG_RANGE_PORTAL.get())&&state.getValue(VoidPortalBlock.FACING)==Direction.DOWN,"Missing downward portal");
            for(int y=172;y<=230;y++)h.assertTrue(CorridorLayout.block(portal.getX(),y,portal.getZ()).isAir(),"Sand covers portal");
            h.assertTrue(CorridorLayout.block(portal.getX(),170,portal.getZ()).is(com.mcmagic.omnira.registry.ModBlocks.PURE_SOLIDIFIED_SPACETIME.get()),"Solidified seal excavated");
            var floors=new java.util.HashSet<Integer>();
            for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++)if(dx*dx+dz*dz<=16)floors.add(LongRangeTerrain.swirlFloor(portal,portal.getX()+dx,portal.getZ()+dz));
            h.assertTrue(floors.size()>3,"Sand vortex has no stepped silhouette");
            if(++samples==5)break search;
        }
        h.assertTrue(samples==5,"Deep cliffs lack portals");h.succeed();
    }
    private static java.util.Map<BlockPos,net.minecraft.world.level.block.state.BlockState> volume(GameTestHelper h,BlockPos p,int low,int high){
        var saved=new java.util.HashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)for(int y=low;y<=high;y++){
            var pos=new BlockPos(p.getX()+x,y,p.getZ()+z);saved.put(pos,h.getLevel().getBlockState(pos));h.getLevel().setBlock(pos,Blocks.AIR.defaultBlockState(),2);
        }
        return saved;
    }
    @GameTest(template="spell_arena") public static void preserveXZThenUseNearbyLanding(GameTestHelper h){
        var level=h.getLevel();var p=h.absolutePos(new BlockPos(5,3,5));int top=level.getMaxBuildHeight()-1;var saved=volume(h,p,170,top);
        var pig=h.spawn(net.minecraft.world.entity.EntityType.PIG,new BlockPos(5,3,5));pig.setNoAi(true);pig.moveTo(p.getX()+.37,40,p.getZ()+.61);
        try{
            for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)level.setBlock(new BlockPos(p.getX()+x,224,p.getZ()+z),Blocks.STONE.defaultBlockState(),2);
            var point=LongRangeTransit.landing(level,pig,true);
            h.assertTrue(point!=null&&point.x==pig.getX()&&point.z==pig.getZ()&&point.y==225,"Clear destination changes XZ");
            for(int y=171;y<=top;y++)level.setBlock(new BlockPos(p.getX(),y,p.getZ()),Blocks.STONE.defaultBlockState(),2);
            point=LongRangeTransit.landing(level,pig,true);
            h.assertTrue(point!=null&&Math.pow(point.x-pig.getX(),2)+Math.pow(point.z-pig.getZ(),2)<=16.001,"Nearby fallback missing or too distant");
            h.assertTrue(point.x!=pig.getX()||point.z!=pig.getZ(),"Blocked exact column accepted");
            for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)for(int y=171;y<=top;y++)level.setBlock(new BlockPos(p.getX()+x,y,p.getZ()+z),Blocks.STONE.defaultBlockState(),2);
            h.assertTrue(LongRangeTransit.landing(level,pig,true)==null,"Fully blocked destination accepted");
        }finally{pig.discard();saved.forEach((pos,state)->level.setBlock(pos,state,2));}
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void immediateVerticalTransfersBothWays(GameTestHelper h){
        var level=h.getLevel();var p=h.absolutePos(new BlockPos(9,3,9));var saved=volume(h,p,23,255);
        var pig=h.spawn(net.minecraft.world.entity.EntityType.PIG,new BlockPos(9,3,9));pig.setNoAi(true);pig.setNoGravity(true);
        try{
            for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++){
                level.setBlock(new BlockPos(p.getX()+x,40,p.getZ()+z),Blocks.STONE.defaultBlockState(),2);
                level.setBlock(new BlockPos(p.getX()+x,224,p.getZ()+z),Blocks.STONE.defaultBlockState(),2);
            }
            var below=new BlockPos(p.getX(),40,p.getZ());var above=new BlockPos(p.getX(),224,p.getZ());
            level.setBlock(below,LabyrinthContent.LONG_RANGE_PORTAL.get().defaultBlockState().setValue(VoidPortalBlock.FACING,Direction.UP),2);
            pig.moveTo(p.getX()+.5,39,p.getZ()+.5);float yaw=37; pig.setYRot(yaw);pig.fallDistance=14;
            LabyrinthTransit.tick(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(pig));
            h.assertTrue(pig.getY()==225&&pig.getX()==p.getX()+.5&&pig.getZ()==p.getZ()+.5&&pig.getYRot()==yaw&&pig.fallDistance==0,"Upward touch transfer not immediate or changes XZ/rotation");
            level.setBlock(above,LabyrinthContent.LONG_RANGE_PORTAL.get().defaultBlockState().setValue(VoidPortalBlock.FACING,Direction.DOWN),2);
            pig.getPersistentData().putLong("OmniraVoidPortalCooldown",0);pig.getPersistentData().putBoolean("OmniraVoidPortalAtExit",false);
            LabyrinthTransit.tick(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(pig));
            h.assertTrue(pig.getY()==41&&pig.getX()==p.getX()+.5&&pig.getZ()==p.getZ()+.5,"Downward transfer not immediate");
        }finally{pig.discard();saved.forEach((pos,state)->level.setBlock(pos,state,2));}
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void bothPortalsCannotBeBroken(GameTestHelper h){
        var level=h.getLevel();var p=h.absolutePos(new BlockPos(4,3,4));var player=h.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);
        var pig=h.spawn(net.minecraft.world.entity.EntityType.PIG,new BlockPos(8,3,8));
        for(var block:new VoidPortalBlock[]{LabyrinthContent.PORTAL.get(),LabyrinthContent.LONG_RANGE_PORTAL.get()}){
            var state=block.defaultBlockState();level.setBlock(p,state,2);
            h.assertTrue(state.getDestroySpeed(level,p)<0&&!block.onDestroyedByPlayer(state,level,p,player,true,state.getFluidState())&&!state.canEntityDestroy(level,p,pig),"Portal can be mined or destroyed by mob");
            var event=new net.neoforged.neoforge.event.level.BlockEvent.BreakEvent(level,p,state,player);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);h.assertTrue(event.isCanceled(),"Creative/drill break event accepted");
            h.assertTrue(state.getExplosionResistance(level,p,null)>1000000,"Portal is vulnerable to explosions");
            h.assertTrue(level.getBlockEntity(p) instanceof VoidPortalEntity,"Portal block entity not registered");
        }
        level.removeBlock(p,false);pig.discard();h.succeed();
    }
}
