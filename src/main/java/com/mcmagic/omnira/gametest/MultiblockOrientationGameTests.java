package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.alchemy.*;
import com.mcmagic.omnira.alchemy.catalysis.*;
import com.mcmagic.omnira.aggregation.*;
import com.mcmagic.omnira.engine.*;
import com.mcmagic.omnira.forging.*;
import com.mcmagic.omnira.registry.ModItems;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_construction")
@PrefixGameTestTemplate(false)
public final class MultiblockOrientationGameTests {
    private static BlockState turned(BlockState state,int turn){
        for(var property:state.getProperties())if(property instanceof DirectionProperty direction){
            var choices=java.util.List.copyOf(direction.getPossibleValues());
            state=state.setValue(direction,choices.get(Math.floorMod(turn,choices.size())));
        }
        return state;
    }
    private static BlockPos reset(GameTestHelper h){
        var center=h.absolutePos(new BlockPos(5,3,5));
        for(int y=2;y>=-1;y--)for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)
            h.getLevel().setBlock(center.offset(x,y,z),Blocks.AIR.defaultBlockState(),18);
        return center;
    }
    private static ItemStack crystal(){return new ItemStack(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get(),2);}

    @GameTest(template="spell_arena") public static void poolDiagramMatchesWorldAndOutputs(GameTestHelper h){
        h.assertTrue(ReactionPoolStructure.required(ReactionPoolStructure.topViewPart(0,0,0))==com.mcmagic.omnira.registry.ModBlocks.LIQUID_CRYSTAL_BALL.get(),"Rear-left diagram cell must be the liquid orb");
        h.assertTrue(ReactionPoolStructure.required(ReactionPoolStructure.topViewPart(0,0,2))==com.mcmagic.omnira.registry.ModBlocks.CRYSTAL_BALL.get(),"Rear-right diagram cell must be the item orb");
        h.assertTrue(ReactionPoolStructure.required(ReactionPoolStructure.topViewPart(1,2,1))==com.mcmagic.omnira.registry.DreamContent.DREAM_CRYSTAL_BLOCK.get(),"Front marker moved");
        for(var facing:Direction.Plane.HORIZONTAL){
            var front=facing.getOpposite();var right=front.getCounterClockWise();
            var parts=new java.util.HashSet<Integer>();
            for(int layer=0;layer<2;layer++)for(int row=0;row<3;row++)for(int col=0;col<3;col++){
                int part=ReactionPoolStructure.topViewPart(layer,row,col);parts.add(part);
                var offset=ReactionPoolBlock.offset(facing,part);
                h.assertTrue(offset.getY()==layer
                        &&offset.getX()*right.getStepX()+offset.getZ()*right.getStepZ()==col-1
                        &&offset.getX()*front.getStepX()+offset.getZ()*front.getStepZ()==row-1,"Diagram mirrored or rotated: "+facing);
            }
            h.assertTrue(parts.size()==18,"Diagram duplicates or omits a part");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void poolIgnoresRawFacings(GameTestHelper h){
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        for(var facing:Direction.Plane.HORIZONTAL)for(int turn=0;turn<6;turn++){
            var center=reset(h);var held=crystal();
            for(int part=0;part<18;part++)h.getLevel().setBlock(center.offset(ReactionPoolBlock.offset(facing,part)),turned(ReactionPoolStructure.required(part).defaultBlockState(),turn+part),18);
            h.assertTrue(ReactionPoolStructure.activate(h.getLevel(),center.offset(ReactionPoolBlock.offset(facing,10)),player,held)
                    &&held.getCount()==1&&ReactionPoolBlock.complete(h.getLevel(),center,facing),"Pool requires a raw block facing: "+facing+"/"+turn);
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void forgeIgnoresRawFacings(GameTestHelper h){
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        for(var facing:Direction.Plane.HORIZONTAL)for(int turn=0;turn<6;turn++){
            var center=reset(h);var held=crystal();
            for(int part=0;part<18;part++)h.getLevel().setBlock(center.offset(ForgeLayout.offset(facing,part)),turned(ForgeLayout.required(part%3-1,part/9,part%9/3-1).defaultBlockState(),turn+part),18);
            h.assertTrue(ForgeLayout.form(h.getLevel(),center,player,held)&&held.getCount()==1
                    &&ForgeLayout.formedParts(h.getLevel(),center,h.getLevel().getBlockState(center)).size()==18,"Forge requires a raw block facing: "+facing+"/"+turn);
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void catalysisIgnoresRawFacings(GameTestHelper h){
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        for(var facing:Direction.Plane.HORIZONTAL)for(int turn=0;turn<6;turn++){
            var base=reset(h);var held=crystal();player.setYRot(facing.toYRot());
            for(int part=0;part<3;part++)h.getLevel().setBlock(base.above(part),turned(CatalysisStructure.required(part).defaultBlockState(),turn+part),18);
            h.assertTrue(CatalysisStructure.activate(h.getLevel(),base.above(),player,held)&&held.getCount()==1,"Catalysis requires a raw block facing: "+facing+"/"+turn);
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void engineIgnoresRawFacings(GameTestHelper h){
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        for(var facing:Direction.Plane.HORIZONTAL)for(int turn=0;turn<6;turn++){
            var root=reset(h);var held=crystal();player.setYRot(facing.toYRot());
            for(int part=0;part<10;part++)h.getLevel().setBlock(root.offset(SpacetimeEngineBlock.offset(part)),turned(SpacetimeEngineStructure.required(part).defaultBlockState(),turn+part),18);
            h.assertTrue(SpacetimeEngineStructure.assemble(h.getLevel(),root.above(),player,held)&&held.getCount()==1
                    &&SpacetimeEngineBlock.controller(h.getLevel(),root.above()).complete(),"Engine requires a raw block facing: "+facing+"/"+turn);
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void aggregationIgnoresRawFacings(GameTestHelper h){
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        for(var facing:Direction.Plane.HORIZONTAL)for(int turn=0;turn<6;turn++){
            var center=reset(h);var held=crystal();player.setYRot(facing.getOpposite().toYRot());
            for(int part=0;part<9;part++){
                var block=part==4?com.mcmagic.omnira.registry.ModBlocks.CRYSTAL_PROCESSING_TABLE.get()
                        :part==1||part==7?(turn%2==0?com.mcmagic.omnira.registry.DreamContent.EXCITED_SPATIAL_CRYSTAL.get():AggregationContent.SPATIAL_BLOCK.get())
                        :part==3||part==5?(turn%2==0?com.mcmagic.omnira.registry.DreamContent.LIVING_TEMPORAL_SILT.get():com.mcmagic.omnira.registry.TimeNatureContent.LIVING_SILT.get())
                        :com.mcmagic.omnira.registry.ModBlocks.INFUSED_CRYSTAL_CASING.get();
                h.getLevel().setBlock(AggregationLayout.part(center,facing,part),turned(block.defaultBlockState(),turn+part),18);
            }
            h.assertTrue(AggregationStructure.activate(h.getLevel(),center,player,held)&&held.getCount()==1
                    &&AggregationStructure.intact(h.getLevel(),center,h.getLevel().getBlockState(center)),"Aggregation requires a raw block facing: "+facing+"/"+turn);
        }
        h.succeed();
    }
    private MultiblockOrientationGameTests(){}
}
