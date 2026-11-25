package com.mcmagic.omnira.client.guide;

import com.mcmagic.omnira.block.entity.CrystalPedestalBlockEntity;
import com.mcmagic.omnira.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_guide_spread")
@PrefixGameTestTemplate(false)
public final class RitualLayoutGameTests {
    @GameTest(template="spell_arena")
    public static void projectionReadsExistingBlocks(GameTestHelper h){
        var base=h.absolutePos(new BlockPos(4,2,4));
        var level=h.getLevel();
        for(var pattern:java.util.List.of(RitualLayout.pattern("dream_ritual"),
                com.mcmagic.omnira.guidance.ConstructionPattern.of("advanced_crystal_aggregation_ring"))){
            long expected=0;
            for(int i=0;i<pattern.cells().size();i++){
                var cell=pattern.cells().get(i);
                boolean missing=i%3==0;
                var state=missing?Blocks.DIRT.defaultBlockState():cell.alternate();
                level.setBlockAndUpdate(base.offset(cell.offset()),state);
                if(missing)expected |= 1L<<i;
            }
            h.assertTrue(pattern.missingMask(level,base)==expected,"Existing components marked missing");
            for(var cell:pattern.cells())level.setBlockAndUpdate(base.offset(cell.offset()),cell.primary());
            h.assertTrue(pattern.missingMask(level,base)==0,"Complete structure still projected");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void offersFollowActualPedestals(GameTestHelper h){
        var level=h.getLevel();
        var center=h.absolutePos(new BlockPos(4,2,4));
        for(String scene:new String[]{"affinity_ritual","corridor_ritual"}){
            var layout=new RitualLayout(scene,level);
            var cells=RitualLayout.pattern(scene).cells();
            h.assertTrue(cells.size()==9 && cells.stream().noneMatch(c->c.primary().is(Blocks.SMOOTH_STONE)),"Projected ritual floor");
            var offsets=RitualLayout.anchors(scene);
            for(int i=0;i<offsets.length;i++){
                var a=offsets[i];
                var pos=center.offset(-a[1],0,a[0]);
                level.setBlockAndUpdate(pos,ModBlocks.CRYSTAL_PEDESTAL.get().defaultBlockState());
                ((CrystalPedestalBlockEntity)level.getBlockEntity(pos)).setItem(0,new ItemStack(RitualLayout.offering(scene,i)));
            }
            layout.update(level,center);
            for(int i=0;i<offsets.length;i++)h.assertTrue(layout.complete(i),"Rotated offerings rejected in "+scene);
            var p=center.offset(offsets[0][0],0,offsets[0][1]);
            ((CrystalPedestalBlockEntity)level.getBlockEntity(p)).setItem(0,ItemStack.EMPTY);
            layout.update(level,center);
            h.assertTrue(!layout.complete(0) && !layout.display(0,0).isEmpty(),"Missing offering has no projection");
        }
        h.succeed();
    }
}
