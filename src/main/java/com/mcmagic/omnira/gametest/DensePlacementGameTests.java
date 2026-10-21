package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.block.entity.CrystalBallBlockEntity;
import com.mcmagic.omnira.registry.ModBlocks;
import com.mcmagic.omnira.workstation.CrystalWorkstations;
import java.util.ArrayList;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_dense_placement") @PrefixGameTestTemplate(false)
public final class DensePlacementGameTests {
    @GameTest(template="spell_arena",timeoutTicks=450)
    public static void mixedDistrictSurvivesTicksAndRemoval(GameTestHelper h){
        var positions=new ArrayList<BlockPos>();
        var orbs=new ArrayList<CrystalBallBlockEntity>();
        var baseline=new long[1][];
        h.runAfterDelay(120,()->{baseline[0]=timing(h);place(h,positions,orbs);});
        var samples=new long[100];
        for(int i=0;i<samples.length;i++){
            final int index=i;
            h.runAfterDelay(160+i,()->{
                long start=System.nanoTime();
                for(var orb:orbs){
                    h.assertTrue(orb.getItem(0).getCount()==32,"Dense storage changed contents");
                    h.assertTrue(orb.storage()==orb.storage(),"Stable storage view rebuilt");
                }
                samples[index]=System.nanoTime()-start;
            });
        }
        h.runAfterDelay(280,()->{
            for(var p:positions)h.assertTrue(h.getLevel().getBlockState(h.absolutePos(p)).getBlock()!=Blocks.AIR,"Dense machine disappeared");
            long entities=positions.stream().filter(p->h.getLevel().getBlockEntity(h.absolutePos(p))!=null).count();
            h.assertTrue(entities==384&&orbs.size()==128,"Dense fixture lost a machine or storage endpoint");
            long produced=positions.stream().map(p->h.getLevel().getBlockEntity(h.absolutePos(p))).map(CrystalWorkstations::work)
                    .filter(java.util.Objects::nonNull).filter(w->!w.getItem(2).isEmpty()).count();
            h.assertTrue(produced==128,"Not all fueled furnaces worked: "+produced);
            Arrays.sort(samples);var ticks=timing(h);
            org.slf4j.LoggerFactory.getLogger("OmniraDenseAudit").info(
                    "512 blocks/384 block entities/16x16 footprint; 128 working furnaces; 128 orb read sweep median={}us p95={}us (not whole-tick time or client FPS)",samples[50]/1000.0,samples[94]/1000.0);
            org.slf4j.LoggerFactory.getLogger("OmniraDenseAudit").info(
                    "Server 100-tick window median/p95: baseline={}/{}ms dense={}/{}ms; includes other mod/world ticking, not client FPS",
                    baseline[0][50]/1e6,baseline[0][94]/1e6,ticks[50]/1e6,ticks[94]/1e6);
            for(var orb:orbs)orb.detachNetwork();
            for(var p:positions)h.setBlock(p,Blocks.AIR);
        });
        h.runAfterDelay(300,()->{
            for(var p:positions)h.assertTrue(h.getLevel().getBlockEntity(h.absolutePos(p))==null,"Removed machine retained its block entity");
            h.succeed();
        });
    }
    private static long[] timing(GameTestHelper h){
        var times=h.getLevel().getServer().getTickTimesNanos().clone();Arrays.sort(times);return times;
    }
    private static void place(GameTestHelper h,java.util.List<BlockPos> positions,java.util.List<CrystalBallBlockEntity> orbs){
        var marker=new ItemStack(Items.PAPER);
        marker.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal(java.util.UUID.randomUUID().toString()));
        // A bounded 16x16 footprint, two layers: no placement outside this template.
        for(int layer=0;layer<2;layer++)for(int z=0;z<16;z++)for(int x=0;x<16;x++){
            var p=new BlockPos(3+x,3+layer*3,3+z);positions.add(p);
            int kind=(x+z+layer)%4;
            var block=switch(kind){
                case 0 -> (x%2==0?ModBlocks.CRYSTAL_BALL:com.mcmagic.omnira.storage.CrystalStorageContent.ITEM_ORB).get();
                case 1 -> CrystalWorkstations.BLOCKS.get(0).get();
                case 2 -> ModBlocks.ARCANE_ASSEMBLY_TABLE.get();
                default -> net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("omnira:spiritual_crystal_block"));
            };
            h.setBlock(p,block);
            var entity=h.getLevel().getBlockEntity(h.absolutePos(p));
            if(entity instanceof CrystalBallBlockEntity orb){
                if(orb.resonant())orb.setFrequency(0,marker);
                orb.setItem(0,new ItemStack(Items.DIAMOND,32));orbs.add(orb);
            }
            var work=CrystalWorkstations.work(entity);
            if(work!=null){work.setItem(0,new ItemStack(Items.COBBLESTONE,32));work.setItem(1,new ItemStack(Items.COAL,8));}
        }
    }
}
