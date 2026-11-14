package com.mcmagic.omnira.block.entity;
import com.mcmagic.omnira.block.VoidCrystalBlock;
import com.mcmagic.omnira.registry.ModBlockEntityTypes;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
public final class VoidCrystalBlockEntity extends BlockEntity {
    private int remaining=400;
    private long legacyExpiry=-1;
    private boolean permanent;
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
        if (!level.isClientSide) be.advance();
    }
    public VoidCrystalBlockEntity(BlockPos pos,BlockState state) {super(ModBlockEntityTypes.VOID_CRYSTAL.get(),pos,state);}
    public void configure(int ticks,boolean permanent) {
        lifetime=Math.max(1,ticks);remaining=lifetime;this.permanent=permanent;legacyExpiry=-1;setChanged();
    }
    private void advance() {
        if (legacyExpiry>=0) {
            permanent=legacyExpiry==0;
            remaining=(int)Math.clamp(legacyExpiry-level.getGameTime(),0L,(long)lifetime);
            legacyExpiry=-1;
        }
        // Upgrade legacy permanent spell blocks once, without retaining per-block NBT.
        if(permanent) {
            var blocks=getBlockState().is(com.mcmagic.omnira.registry.ModBlocks.REINFORCED_VOID_CRYSTAL.get())
                    ? com.mcmagic.omnira.registry.ModBlocks.PERMANENT_REINFORCED_VOID_CRYSTAL
                    : com.mcmagic.omnira.registry.ModBlocks.PERMANENT_VOID_CRYSTAL;
            level.setBlockAndUpdate(worldPosition,blocks.get().defaultBlockState());
            return;
        }
        remaining--;
        if(remaining<=0 && prisoner!=null && level instanceof net.minecraft.server.level.ServerLevel server
                && server.getEntity(prisoner) instanceof net.minecraft.world.entity.LivingEntity captive){
            var lock=captive.getPersistentData().getCompound("OmniraConstructionLock");
            var effect=captive.getEffect(com.mcmagic.omnira.registry.ModEffects.CONSTRUCTION_LOCK);
            if(effect!=null && lock.hasUUID("seal") && lock.getUUID("seal").equals(seal))
                remaining=Math.max(1,effect.getDuration());
        }
        level.getChunkAt(worldPosition).setUnsaved(true);
        if(remaining<=0) {level.removeBlock(worldPosition,false);return;}
        int fade=(int)Math.clamp((long)(lifetime-remaining)*8/lifetime,0,7);
        if(getBlockState().getValue(VoidCrystalBlock.FADE)!=fade)
            level.setBlock(worldPosition,getBlockState().setValue(VoidCrystalBlock.FADE,fade),3);

    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        super.saveAdditional(tag,registries);if(legacyExpiry>=0)tag.putLong("Expires",legacyExpiry);else tag.putInt("Remaining",remaining);tag.putBoolean("Permanent",permanent);tag.putInt("Lifetime",lifetime);
        if(prisoner!=null){tag.putUUID("Prisoner",prisoner);tag.putUUID("Seal",seal);tag.putFloat("Hardness",hardness);}
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        super.loadAdditional(tag,registries);legacyExpiry=tag.contains("Expires")?tag.getLong("Expires"):-1;permanent=tag.getBoolean("Permanent");lifetime=tag.contains("Lifetime")?Math.max(1,tag.getInt("Lifetime")):400;
        remaining=tag.contains("Remaining")?Math.clamp(tag.getInt("Remaining"),0,lifetime):lifetime;
        prisoner=tag.hasUUID("Prisoner")?tag.getUUID("Prisoner"):null;seal=tag.hasUUID("Seal")?tag.getUUID("Seal"):null;hardness=Math.max(1,tag.getFloat("Hardness"));
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {return saveWithoutMetadata(registries);}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
}
