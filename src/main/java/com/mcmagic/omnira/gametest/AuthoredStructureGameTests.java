package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.world.structure.*;
import com.mcmagic.omnira.shop.*;
import com.mcmagic.omnira.mire.*;
import com.mcmagic.omnira.spacetime.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.structure.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_structure_alignment")
@PrefixGameTestTemplate(false)
public final class AuthoredStructureGameTests {
    @GameTest(template="spell_arena",timeoutTicks=200)
    public static void threeNaturalOrigins(GameTestHelper h){
        var l=h.getLevel();
        String[] ids={"frozen_terra_palace","kirisame_magic_shop","rusty_lake_mill"};
        // Use registered structures and their real placement path, not the shared helper alone.
        for(String id:ids){
            var structure=l.registryAccess().registryOrThrow(Registries.STRUCTURE).get(ResourceLocation.parse("omnira:"+id));
            h.assertTrue(structure!=null,"Missing structure "+id);
            boolean found=false;
            for(int x=-64;x<64&&!found;x++)for(int z=-64;z<64&&!found;z++){
                var generator=l.getChunkSource().getGenerator();
                var context=new Structure.GenerationContext(l.registryAccess(),generator,generator.getBiomeSource(),l.getChunkSource().randomState(),l.getStructureManager(),l.getSeed(),new ChunkPos(x,z),l,b->true);
                var stub=switch(structure){
                    case FrozenTerraPalaceStructure palace -> palace.findGenerationPoint(context);
                    case KirisameShopStructure shop -> shop.findGenerationPoint(context);
                    case MillStructure mill -> mill.findGenerationPoint(context);
                    default -> throw new IllegalStateException("Unexpected structure "+id);
                };
                if(stub.isEmpty())continue;
                var piece=stub.get().getPiecesBuilder().build().pieces().getFirst();var box=piece.getBoundingBox();
                int high=Integer.MIN_VALUE;
                for(int dx=box.minX();dx<=box.maxX();dx++)for(int dz=box.minZ();dz<=box.maxZ();dz++)high=Math.max(high,CorridorLayout.timeSurface(dx,dz));
                int originY=box.minY()-(id.equals("rusty_lake_mill")?MillPlan.MIN_Y:0);
                h.assertTrue(originY==high-1,"Structure floats above sample reference: "+id);
                found=true;
            }
            h.assertTrue(found,"No eligible placement for "+id);
        }
        h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=200)
    public static void palaceEntranceContinuous(GameTestHelper h){
        var l=h.getLevel();var origin=h.absolutePos(new BlockPos(96,8,96));
        var piece=new FrozenTerraPalacePiece(origin);place(h,piece,origin,false);
        for(int x=19;x<=29;x++){
            var lower=l.getBlockState(origin.offset(x,0,64));var upper=l.getBlockState(origin.offset(x,1,63));
            h.assertTrue(lower.getBlock() instanceof StairBlock&&upper.getBlock() instanceof StairBlock,"Entrance became a full block");
            h.assertTrue(lower.getValue(StairBlock.FACING)==Direction.NORTH&&upper.getValue(StairBlock.FACING)==Direction.NORTH,"Entrance faces away from palace");
            h.assertTrue(!lower.getCollisionShape(l,origin.offset(x,0,64)).equals(net.minecraft.world.phys.shapes.Shapes.block()),"Stair collision is a cube");
            h.assertTrue(l.isEmptyBlock(origin.offset(x,1,64)),"Lower stair headroom blocked");
            h.assertTrue(l.getBlockState(origin.offset(x,1,62)).is(com.mcmagic.omnira.registry.DreamContent.SOLIDIFIED_LIGHT_CRYSTAL_CORE.get()),"Final landing missing");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=200)
    public static void shopConnectionsAfterClippedPlacement(GameTestHelper h){
        var l=h.getLevel();var origin=h.absolutePos(new BlockPos(96,8,96));var piece=new KirisameShopPiece(origin);
        place(h,piece,origin,true);
        var merchant=KirisameShopPlan.cells().stream().filter(c->c.name().equals("omnira:marisa_crystal_ball")).findFirst().orElseThrow();
        var anchor=l.getBlockEntity(merchant.pos().offset(origin));
        AuthoredStructureConnections.initialize(anchor);
        for(var cell:KirisameShopPlan.cells()){
            var p=cell.pos().offset(origin);var state=l.getBlockState(p);
            if(state.getBlock() instanceof FenceBlock||state.getBlock() instanceof StairBlock||state.getBlock() instanceof WallBlock||state.getBlock() instanceof IronBarsBlock)
                h.assertTrue(state.equals(Block.updateFromNeighbourShapes(state,l,p)),"Stale connection at "+cell.pos());
        }
        h.assertTrue(!l.getBlockState(origin.offset(8,8,17)).getValue(FenceBlock.NORTH),"Railing extends into stair opening");
        h.assertTrue(!l.getBlockState(origin.offset(8,8,22)).getValue(FenceBlock.SOUTH),"Railing extends beyond end");
        h.succeed();
    }
    private static void place(GameTestHelper h,StructurePiece piece,BlockPos origin,boolean reverse){
        var l=h.getLevel();var b=piece.getBoundingBox();var chunks=new java.util.ArrayList<ChunkPos>();
        for(int x=b.minX()>>4;x<=b.maxX()>>4;x++)for(int z=b.minZ()>>4;z<=b.maxZ()>>4;z++){l.getChunk(x,z);chunks.add(new ChunkPos(x,z));}
        if(reverse)java.util.Collections.reverse(chunks);
        for(var c:chunks)piece.postProcess(l,l.structureManager(),l.getChunkSource().getGenerator(),net.minecraft.util.RandomSource.create(7),new BoundingBox(c.getMinBlockX(),l.getMinBuildHeight(),c.getMinBlockZ(),c.getMaxBlockX(),l.getMaxBuildHeight()-1,c.getMaxBlockZ()),c,origin);
    }
}
