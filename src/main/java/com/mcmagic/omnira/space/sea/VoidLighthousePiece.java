package com.mcmagic.omnira.space.sea;

import com.google.gson.JsonParser;
import com.mcmagic.omnira.registry.DreamContent;
import com.mcmagic.omnira.shop.KirisameShopPlan;
import com.mcmagic.omnira.world.structure.RuinsStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import java.util.*;

public final class VoidLighthousePiece extends StructurePiece {
    public static final int BASE_Y=VoidSeaTerrain.SEA_LEVEL+2;
    public static final int ISLAND_PADDING=12;
    private final BlockPos origin;
    public static final List<KirisameShopPlan.Cell> CELLS=load();
    public VoidLighthousePiece(BlockPos origin) { super(RuinsStructure.LIGHTHOUSE_PIECE.get(),0,
            new BoundingBox(origin.getX()-ISLAND_PADDING,24,origin.getZ()-ISLAND_PADDING,
                    origin.getX()+24+ISLAND_PADDING,origin.getY()+22,origin.getZ()+24+ISLAND_PADDING));this.origin=origin.immutable(); }
    public VoidLighthousePiece(StructurePieceSerializationContext c, CompoundTag t) {
        super(RuinsStructure.LIGHTHOUSE_PIECE.get(),t);
        origin=t.contains("AnchorY")?new BlockPos(t.getInt("AnchorX"),t.getInt("AnchorY"),t.getInt("AnchorZ"))
                :new BlockPos(boundingBox.minX(),65,boundingBox.minZ());
    }
    @Override protected void addAdditionalSaveData(StructurePieceSerializationContext c,CompoundTag t) {
        t.putInt("AnchorX",origin.getX());t.putInt("AnchorY",origin.getY());t.putInt("AnchorZ",origin.getZ());
    }
    public static int islandTop(BlockPos anchor,int x,int z){
        int floor=VoidSeaTerrain.floor(x,z);
        double dx=x-anchor.getX()-12,dz=z-anchor.getZ()-12;
        double radius=Math.sqrt(dx*dx+dz*dz/0.90);
        // A broad submerged apron joins the seabed; only the central crown is level.
        double ripple=(Math.sin(x/4.0)+Math.cos(z/5.0))*.45;
        double t=Math.clamp((24-radius+ripple)/12,0,1);
        double slope=t*t*(3-2*t);
        return Math.max(floor,(int)Math.round(floor+(anchor.getY()-floor)*slope));
    }
    @Override public void postProcess(WorldGenLevel l,StructureManager m,ChunkGenerator g,RandomSource r,BoundingBox clip,ChunkPos chunk,BlockPos pivot) {
        for(int x=-ISLAND_PADDING;x<=24+ISLAND_PADDING;x++)for(int z=-ISLAND_PADDING;z<=24+ISLAND_PADDING;z++) {
            int top=islandTop(origin,origin.getX()+x,origin.getZ()+z);
            for(int y=VoidSeaTerrain.floor(origin.getX()+x,origin.getZ()+z)+1;y<=top;y++) {
                BlockPos p=new BlockPos(origin.getX()+x,y,origin.getZ()+z);
                if(clip.isInside(p))l.setBlock(p,DreamContent.EXCITED_SPATIAL_CRYSTAL.get().defaultBlockState(),2);
            }
        }
        for(var cell:CELLS) {
            BlockPos p=origin.offset(cell.pos()); if(!clip.isInside(p))continue;
            l.setBlock(p,KirisameShopPlan.state(cell),2);
            var tag=KirisameShopPlan.blockEntity(cell,p,r.nextLong());
            if(tag!=null&&l.getBlockEntity(p)!=null)l.getBlockEntity(p).loadWithComponents(tag,l.registryAccess());
        }
    }
    private static List<KirisameShopPlan.Cell> load() {
        try(var in=Objects.requireNonNull(VoidLighthousePiece.class.getResourceAsStream("/data/omnira/structures/void_lighthouse.json"));
            var reader=new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)) {
            var out=new ArrayList<KirisameShopPlan.Cell>();
            for(var raw:JsonParser.parseReader(reader).getAsJsonArray()) {
                var c=raw.getAsJsonObject();var props=new HashMap<String,String>();
                if(c.has("properties"))c.getAsJsonObject("properties").entrySet().forEach(e->props.put(e.getKey(),e.getValue().getAsString()));
                out.add(new KirisameShopPlan.Cell(new BlockPos(c.get("x").getAsInt(),c.get("y").getAsInt(),c.get("z").getAsInt()),c.get("name").getAsString(),props,
                        c.has("loot")?c.get("loot").getAsString():"",List.of(),c.has("nbt")?c.get("nbt").getAsString():"",false));
            }
            return List.copyOf(out);
        } catch(java.io.IOException e) { throw new IllegalStateException("Cannot load lighthouse",e); }
    }
}
