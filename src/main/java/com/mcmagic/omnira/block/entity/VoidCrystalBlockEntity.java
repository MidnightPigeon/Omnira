package com.mcmagic.omnira.block.entity;
import com.mcmagic.omnira.block.VoidCrystalBlock;
import com.mcmagic.omnira.registry.ModBlockEntityTypes;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
public final class VoidCrystalBlockEntity extends BlockEntity {
    private long expires=-1;
    private int lifetime=400;
    private java.util.UUID prisoner,seal;
    private float hardness=3;
    public java.util.UUID prisoner() {return prisoner;}
    public java.util.UUID seal() {return seal;}
    public float hardness() {return hardness;}
    public void bind(java.util.UUID entity,java.util.UUID seal,float hardness,int ticks) {
        this.prisoner=entity;this.seal=seal;this.hardness=hardness;configure(ticks,false);
        level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
    }
    public static void tick(net.minecraft.world.level.Level level,BlockPos pos,BlockState state,VoidCrystalBlockEntity be) {
        if(!level.isClientSide&&(be.expires<=level.getGameTime()||level.getGameTime()%10==0))be.advance();
    }
    public VoidCrystalBlockEntity(BlockPos pos,BlockState state) {super(ModBlockEntityTypes.VOID_CRYSTAL.get(),pos,state);}
    @Override public void onLoad() {
        super.onLoad();
        if(level!=null && !level.isClientSide) {
            if(expires<0) expires=level.getGameTime()+lifetime;
            level.scheduleTick(worldPosition,getBlockState().getBlock(),1);
        }
    }
    public void configure(int ticks,boolean permanent) {
        lifetime=Math.max(1,ticks);expires=permanent?0:level.getGameTime()+lifetime;setChanged();
        level.scheduleTick(worldPosition,getBlockState().getBlock(),1);
    }
    public void advance() {
        if(level==null || level.isClientSide) return;
        if(expires<0){expires=level.getGameTime()+lifetime;setChanged();}
        // Upgrade legacy permanent spell blocks once, without retaining per-block NBT.
        if(expires==0) {
            var blocks=getBlockState().is(com.mcmagic.omnira.registry.ModBlocks.REINFORCED_VOID_CRYSTAL.get())
                    ? com.mcmagic.omnira.registry.ModBlocks.PERMANENT_REINFORCED_VOID_CRYSTAL
                    : com.mcmagic.omnira.registry.ModBlocks.PERMANENT_VOID_CRYSTAL;
            level.setBlockAndUpdate(worldPosition,blocks.get().defaultBlockState());
            return;
        }
        if(level.getGameTime()>=expires) {level.removeBlock(worldPosition,false);return;}
        int fade=(int)Math.clamp((lifetime-expires+level.getGameTime())*8/lifetime,0,7);
        if(getBlockState().getValue(VoidCrystalBlock.FADE)!=fade)
            level.setBlock(worldPosition,getBlockState().setValue(VoidCrystalBlock.FADE,fade),3);
        level.scheduleTick(worldPosition,getBlockState().getBlock(),10);
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        super.saveAdditional(tag,registries);tag.putLong("Expires",expires);tag.putInt("Lifetime",lifetime);
        if(prisoner!=null){tag.putUUID("Prisoner",prisoner);tag.putUUID("Seal",seal);tag.putFloat("Hardness",hardness);}
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        super.loadAdditional(tag,registries);expires=tag.contains("Expires")?tag.getLong("Expires"):-1;lifetime=tag.contains("Lifetime")?Math.max(1,tag.getInt("Lifetime")):400;
        prisoner=tag.hasUUID("Prisoner")?tag.getUUID("Prisoner"):null;seal=tag.hasUUID("Seal")?tag.getUUID("Seal"):null;hardness=Math.max(1,tag.getFloat("Hardness"));
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {return saveWithoutMetadata(registries);}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
}
