package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.pulse.*;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_wire_shape")
@PrefixGameTestTemplate(false)
public final class PulseWireShapeTests {
    private static final BlockPos AT=new BlockPos(6,5,6);
    private static net.neoforged.neoforge.common.util.FakePlayer player(GameTestHelper h){
        var player=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"wire-test"));
        player.setGameMode(GameType.SURVIVAL);return player;
    }

    private static boolean hits(net.minecraft.world.level.block.state.BlockState state,GameTestHelper h,Direction face){
        var pos=h.absolutePos(AT);var axis=face.getAxis()==Direction.Axis.X?Direction.UP:Direction.EAST;
        var target=Vec3.atCenterOf(pos).add(face.getStepX()*.375,face.getStepY()*.375,face.getStepZ()*.375);
        var offset=new Vec3(axis.getStepX(),axis.getStepY(),axis.getStepZ());
        return state.getShape(h.getLevel(),pos).clip(target.add(offset),target.subtract(offset),pos)!=null;
    }

    @GameTest(template="spell_arena") public static void allConnectionMasksAreSelectableButNeverSolid(GameTestHelper h){
        h.setBlock(AT,PulseContent.WIRE.get());var level=h.getLevel();var pos=h.absolutePos(AT);
        for(int mask=0;mask<64;mask++){
            for(var face:Direction.values())h.setBlock(AT.relative(face),(mask&(1<<face.ordinal()))!=0?PulseContent.WIRE.get():Blocks.AIR);
            var state=level.getBlockState(pos);var outline=state.getShape(level,pos);
            for(boolean powered:new boolean[]{false,true}){
                var variant=state.setValue(PulseWireBlock.POWERED,powered);
                h.assertTrue(variant.getShape(level,pos)==outline,"Power changed cached wire outline");
                h.assertTrue(variant.getCollisionShape(level,pos).isEmpty(),"Wire blocks entities: "+mask);
                for(var face:Direction.values())h.assertTrue(hits(variant,h,face)==((mask&(1<<face.ordinal()))!=0),
                        "Wrong ray selection for mask "+mask+" face "+face);
            }
        }
        for(var face:Direction.values())h.setBlock(AT.relative(face),Blocks.AIR);
        h.assertTrue(level.getBlockState(pos).getShape(level,pos).bounds().getXsize()==.125,"Disconnected wire retained branches");h.succeed();
    }

    @GameTest(template="spell_arena") public static void outlineTracksOrbAndDirectionalSocketImmediately(GameTestHelper h){
        h.setBlock(AT,PulseContent.WIRE.get());var state=h.getLevel().getBlockState(h.absolutePos(AT));
        for(var face:Direction.values()){
            h.setBlock(AT.relative(face),PulseContent.ORB.get());h.assertTrue(hits(state,h,face),"Orb branch missing: "+face);
            h.setBlock(AT.relative(face),Blocks.STONE);h.assertTrue(!hits(state,h,face),"Solid neighbour invented a connection");
            h.setBlock(AT.relative(face),Blocks.AIR);
        }
        var engine=ModBlocks.MANA_ENGINE.get().defaultBlockState();
        h.setBlock(AT.north(),engine.setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.SOUTH));
        h.assertTrue(hits(state,h,Direction.NORTH),"Engine output has no selectable branch");
        h.setBlock(AT.north(),engine.setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.NORTH));
        h.assertTrue(!hits(state,h,Direction.NORTH),"Engine back invented a selectable branch");h.succeed();
    }

    @GameTest(template="spell_arena") public static void bareHandsBreakTheSelectedNodeAndReturnOneDust(GameTestHelper h){
        h.setBlock(AT,PulseContent.WIRE.get());h.setBlock(AT.east(),PulseContent.WIRE.get());
        var player=player(h);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        h.assertTrue(hits(h.getLevel().getBlockState(h.absolutePos(AT)),h,Direction.EAST),"Connected arm cannot be targeted");
        var pos=h.absolutePos(AT);var target=Vec3.atCenterOf(pos).add(.375,0,0);
        var hit=h.getLevel().clip(new net.minecraft.world.level.ClipContext(target.add(0,1,0),target.add(0,-1,0),
                net.minecraft.world.level.ClipContext.Block.OUTLINE,net.minecraft.world.level.ClipContext.Fluid.NONE,player));
        h.assertTrue(hit.getType()==HitResult.Type.BLOCK&&hit.getBlockPos().equals(pos),"World ray missed the wire arm");
        h.assertTrue(player.gameMode.destroyBlock(hit.getBlockPos()),"Bare-hand removal rejected");
        h.assertTrue(h.getLevel().getBlockState(pos).isAir()&&h.getBlockState(AT.east()).is(PulseContent.WIRE.get()),"Breaking one node removed the wrong segment");
        int dust=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(1))
                .stream().filter(entity->entity.getItem().is(ModItems.ARCANE_DUST.get())).mapToInt(entity->entity.getItem().getCount()).sum();
        h.assertTrue(dust==1,"Wire did not return exactly one arcane dust: "+dust);h.succeed();
    }

    @GameTest(template="spell_arena") public static void playerWalksThroughFullyConnectedWire(GameTestHelper h){
        h.setBlock(AT,PulseContent.WIRE.get());for(var face:Direction.values())h.setBlock(AT.relative(face),PulseContent.WIRE.get());
        var player=player(h);
        var center=Vec3.atCenterOf(h.absolutePos(AT));player.setPos(center.x-1.5,center.y-.5,center.z);
        h.assertTrue(h.getBlockState(AT).getCollisionShape(h.getLevel(),h.absolutePos(AT),CollisionContext.of(player)).isEmpty(),"Player collision is not empty");
        player.move(MoverType.SELF,new Vec3(3,0,0));
        h.assertTrue(Math.abs(player.getX()-(center.x+1.5))<.001,"Wire obstructed player movement");h.succeed();
    }
}
