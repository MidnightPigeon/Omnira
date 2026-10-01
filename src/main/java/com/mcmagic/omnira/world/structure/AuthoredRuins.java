package com.mcmagic.omnira.world.structure;

import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.world.level.block.*;
import com.mcmagic.omnira.registry.ModBlocks;
import java.util.*;

final class AuthoredRuins {
    private static final Map<BlockPos,RuinsPiece.Cell> HUT=read("abandoned_apprentice_hut");
    private static final Map<BlockPos,RuinsPiece.Cell> WORKSHOP=read("ruined_crystal_workshop");
    private static final Map<BlockPos,RuinsPiece.Cell> TOWER=read("abandoned_wizard_tower");
    private static Map<BlockPos,RuinsPiece.Cell> read(String name) {
        try(var input=AuthoredRuins.class.getResourceAsStream("/data/omnira/structure/authored/"+name+".nbt")) {
            if(input==null)throw new IllegalStateException("Missing authored ruin "+name);
            var root=NbtIo.readCompressed(input,NbtAccounter.unlimitedHeap());var palette=root.getList("palette",10);
            Map<BlockPos,RuinsPiece.Cell> result=new LinkedHashMap<>();
            for(var entry:root.getList("blocks",10)) {
                var block=(CompoundTag)entry;var xyz=block.getList("pos",3);var pos=new BlockPos(xyz.getInt(0),xyz.getInt(1),xyz.getInt(2));
                var state=NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(),palette.getCompound(block.getInt("state")));
                var data=block.contains("nbt",10)?block.getCompound("nbt").copy():null;
                if(state.is(ModBlocks.WAYMARK.get())) {
                    if(data==null) data=new CompoundTag();
                    data.remove("Id");data.remove("Name");data.putBoolean("Unclaimed",true);
                }
                String loot=data!=null&&data.contains("LootTable")?data.getString("LootTable").replace("omnira:chests/",""):null;
                result.put(pos,new RuinsPiece.Cell(state,loot,data));
            }
            return Collections.unmodifiableMap(result);
        } catch(java.io.IOException e) {throw new java.io.UncheckedIOException(e);}
    }
    static void build(RuinsPiece.Plan b,int variant) {
        b.cells.putAll(variant==0?HUT:variant==1?WORKSHOP:TOWER);
        if(variant==2) {
            boolean assembly=b.random.nextFloat()<.25F,processing=b.random.nextFloat()<.25F;
            b.cells.replaceAll((p,c)->p.getY()==17&&(!assembly&&c.state().is(ModBlocks.ARCANE_ASSEMBLY_TABLE.get())
                    ||!processing&&c.state().is(ModBlocks.CRYSTAL_PROCESSING_TABLE.get()))
                    ?new RuinsPiece.Cell(Blocks.AIR.defaultBlockState(),null):c);
            return;
        }
        boolean ruined=b.random.nextBoolean();
        if(!ruined) {
            // Heal only the original sample's deliberate damage, never rewrite the author's furnishings.
            if(variant==0)copyIfAir(b,new BlockPos(10,8,11),new BlockPos(10,8,10));
            else {
                for(int x=12;x<=14;x++)for(int z=10;z<=12;z++) {
                    int y=5+(int)Math.round(3*(1-Math.pow((x-9)/8.0,2)));
                    copyIfAir(b,new BlockPos(x,y,z),new BlockPos(x,y,9));
                }
                for(int z=10;z<=11;z++)for(int y=2;y<=3;y++)copyIfAir(b,new BlockPos(16,y,z),new BlockPos(16,y,9));
            }
            b.cells.replaceAll((p,c)->c.state().is(Blocks.COBWEB)?new RuinsPiece.Cell(Blocks.AIR.defaultBlockState(),null):c);
        } else {
            for(var entry:new ArrayList<>(b.cells.entrySet())) {
                var p=entry.getKey();var c=entry.getValue();
                if(c.state().getBlock() instanceof StainedGlassPaneBlock && b.random.nextInt(30)==0)
                    b.put(p.getX(),p.getY(),p.getZ(),Blocks.AIR);
                else if(c.state().is(Blocks.STONE_BRICKS)&&b.random.nextInt(5)==0)b.put(p.getX(),p.getY(),p.getZ(),Blocks.CRACKED_STONE_BRICKS);
            }
        }
        if(variant==1) {
            boolean waymark=b.random.nextFloat()<.25F,engine=b.random.nextFloat()<.25F;
            b.cells.replaceAll((p,c)->(!waymark&&c.state().is(ModBlocks.WAYMARK.get())||!engine&&c.state().is(ModBlocks.MANA_ENGINE.get()))
                    ?new RuinsPiece.Cell(Blocks.AIR.defaultBlockState(),null):c);
        }
    }
    private static void copyIfAir(RuinsPiece.Plan b,BlockPos to,BlockPos from) {
        var current=b.cells.get(to);var source=b.cells.get(from);
        if(current!=null&&current.state().isAir()&&source!=null&&!source.state().isAir()&&source.data()==null)b.cells.put(to,source);
    }
}
