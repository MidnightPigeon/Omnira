package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.space.phase.*;
import com.mcmagic.omnira.shop.KirisameShopPlan;
import com.mcmagic.omnira.alchemy.catalysis.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.TagParser;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_phase_greenhouse") @PrefixGameTestTemplate(false)
public final class PhaseGreenhouseGameTests {
    @GameTest(template="spell_arena") public static void templateAndInterior(GameTestHelper h)throws Exception{
        var cells=PhaseGreenhousePiece.CELLS;var occupied=new java.util.HashMap<BlockPos,KirisameShopPlan.Cell>();
        int items=0,fluids=0,chambers=0,panels=0;
        for(var c:cells){
            h.assertTrue(occupied.put(c.pos(),c)==null,"Duplicate greenhouse cell");KirisameShopPlan.state(c);
            if(c.name().equals("omnira:crystal_ball")){items++;h.assertTrue(TagParser.parseTag(c.nbt()).contains("LootTable"),"Item treasure not unopened");}
            if(c.name().equals("omnira:liquid_crystal_ball")){fluids++;h.assertTrue(TagParser.parseTag(c.nbt()).getBoolean("FluidTreasure"),"Fluid orb is not treasure");}
            if(c.name().equals("omnira:alchemical_catalysis_chamber")){
                h.assertTrue(TagParser.parseTag(c.nbt()).getString("id").equals(c.name()),"Authored machine missing persistent block entity");
                if(c.properties().get("part").equals("0"))chambers++;
            }
            if(c.name().equals("omnira:phase_control_panel"))panels++;
        }
        h.assertTrue(items==2&&fluids==1&&chambers==2&&panels==1,"Room furniture counts incorrect");
        for(int x:new int[]{9,35})for(int y:new int[]{1,2}){
            var c=occupied.get(new BlockPos(x,y,9));h.assertTrue(c.name().equals("omnira:light_crystal_torch")&&c.properties().get("facing").equals("up"),"Room central light missing");
        }
        for(var c:cells)if(c.pos().getY()==0)h.assertTrue(c.name().equals("omnira:engraved_mirror_rock"),"Textured mirror floor missing");
        var shell=new java.util.HashSet<BlockPos>();for(var c:cells)if(c.name().equals("omnira:phase_crystal_block"))shell.add(c.pos());
        var remaining=new java.util.HashSet<>(shell);var queue=new java.util.ArrayDeque<BlockPos>();queue.add(shell.iterator().next());
        while(!queue.isEmpty()){var p=queue.removeFirst();if(!remaining.remove(p))continue;for(var face:Direction.values())if(remaining.contains(p.relative(face)))queue.add(p.relative(face));}
        h.assertTrue(remaining.isEmpty(),"Walls/roofs do not form one controlled component");
        h.assertTrue(occupied.get(new BlockPos(22,2,10)).name().equals("omnira:phase_control_panel")&&shell.contains(new BlockPos(22,2,11)),"Panel is not attached on second wall block");
        for(int y=1;y<=2;y++)h.assertTrue(occupied.get(new BlockPos(22,y,7)).name().equals("minecraft:air"),"Door does not face panel");
        for(int x=19;x<=25;x++)for(int y=1;y<=3;y++)h.assertTrue(shell.contains(new BlockPos(x,y,11)),"Corridor wall not three high");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void panelLocksAndRetargets(GameTestHelper h){
        var level=h.getLevel();var start=h.absolutePos(new BlockPos(2,4,6));
        for(int i=0;i<10;i++)level.setBlockAndUpdate(start.east(i),PhaseContent.BLOCK.get().defaultBlockState());
        var panel=start.north();level.setBlockAndUpdate(panel,PhaseContent.CONTROL.get().defaultBlockState());
        var group=PhaseNetworks.component(level,start);
        var random=new net.minecraft.world.level.levelgen.LegacyRandomSource(1){@Override public int nextInt(int bound){throw new AssertionError("Controlled wall randomly changes phase");}};
        PhaseNetworks.tick(level,start,random);
        h.assertTrue(group.blocks().stream().noneMatch(p->level.getBlockState(p).getValue(PhaseBlock.TRANSITIONING)),"Default panel does not hold solid");
        level.setBlockAndUpdate(panel,level.getBlockState(panel).setValue(PhaseControlBlock.INVERTED,true));PhaseNetworks.control(level,group);
        for(int i=0;i<4;i++)PhaseNetworks.advance(level,group);
        h.assertTrue(level.getBlockState(start).getValue(PhaseBlock.PHASE)==4,"Panel ignores four-second transition");
        level.setBlockAndUpdate(panel,level.getBlockState(panel).setValue(PhaseControlBlock.INVERTED,false));PhaseNetworks.control(level,group);
        for(int i=0;i<4;i++)PhaseNetworks.advance(level,group);
        h.assertTrue(level.getBlockState(start).getValue(PhaseBlock.PHASE)==0&&!level.getBlockState(start).getValue(PhaseBlock.INVERTED),"Mid-transition reversal fails");
        level.setBlockAndUpdate(panel,level.getBlockState(panel).setValue(PhaseControlBlock.INVERTED,true));PhaseNetworks.control(level,group);
        for(int i=0;i<16;i++)PhaseNetworks.advance(level,group);
        h.assertTrue(level.getBlockState(start).getValue(PhaseBlock.INVERTED),"Panel cannot hold inverted state");
        level.setBlockAndUpdate(start,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        h.assertTrue(level.getBlockState(panel).isAir(),"Panel survives without attached crystal");h.succeed();
    }
    @GameTest(template="spell_arena") public static void contiguousGlassOmitsAllInternalFaces(GameTestHelper h){
        var block=PhaseContent.BLOCK.get();var normal=block.defaultBlockState();
        for(int phase=0;phase<=16;phase++)for(var face:Direction.values()){
            var neighbor=normal.setValue(PhaseBlock.PHASE,phase).setValue(PhaseBlock.INVERTED,phase==16);
            h.assertTrue(block.skipRendering(normal,neighbor,face)&&block.skipRendering(neighbor,normal,face),"Transition crystal seam retains an internal face");
        }
        h.assertTrue(!block.skipRendering(normal,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),Direction.NORTH),"Exterior crystal face omitted");h.succeed();
    }
    @GameTest(template="spell_arena") public static void authoredChambersAreComplete(GameTestHelper h){
        var level=h.getLevel();
        for(int x:new int[]{3,7}){
            var base=h.absolutePos(new BlockPos(x,3,5));
            h.assertTrue(CatalysisBlock.place(level,base,Direction.SOUTH),"Template chamber cannot be placed");
            var entity=CatalysisBlock.controller(level,base);
            h.assertTrue(entity!=null&&entity.complete(),"Template chamber is not operational multiblock");
        }h.succeed();
    }
}
