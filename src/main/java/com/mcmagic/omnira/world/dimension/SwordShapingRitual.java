package com.mcmagic.omnira.world.dimension;

import com.mcmagic.omnira.block.entity.*;
import com.mcmagic.omnira.recipe.SwordShapingRecipe;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Absorbed offerings are escrowed until completion, so interrupted rituals lose nothing. */
public final class SwordShapingRitual {
    public static final int[][] ANCHORS={{0,-2},{2,0},{0,2},{-2,0}};
    public static final int DURATION=160;
    public static final int COLLAPSE_DURATION=40;
    private final PureVesselBlockEntity vessel;
    private ItemStack weapon=ItemStack.EMPTY,result=ItemStack.EMPTY;
    private List<ItemStack> offerings=List.of();
    private int elapsed,absorbed;
    private boolean wood;
    public SwordShapingRitual(PureVesselBlockEntity vessel) {this.vessel=vessel;}
    public boolean active() {return !weapon.isEmpty();}
    public int progress() {return active()?elapsed:0;}
    public float collapseProgress(float partial) {
        return active()?net.minecraft.util.Mth.clamp((elapsed+partial-(wood?0:DURATION-COLLAPSE_DURATION))/COLLAPSE_DURATION,0,1):0;
    }
    public void startWood(ItemStack input,ItemStack output) {
        weapon=input.copyWithCount(1);result=output.copy();wood=true;elapsed=absorbed=0;
        offerings=List.of();vessel.setItem(0,weapon.copy());
    }
    private void collapseParticles(ServerLevel level) {
        var color=wood?new org.joml.Vector3f(.7F,.8F,.22F):result.is(ModItems.ARCANE_NEEDLE.get())
                ?new org.joml.Vector3f(.77F,.64F,.94F):new org.joml.Vector3f(.57F,.84F,1F);
        var pos=Vec3.atCenterOf(vessel.getBlockPos());
        level.sendParticles(new net.minecraft.core.particles.DustParticleOptions(color,1.25F),pos.x,pos.y,pos.z,2,.27,.32,.27,.015);
        if(elapsed%4==0)level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK,Blocks.WHITE_CONCRETE.defaultBlockState()),pos.x,pos.y,pos.z,1,.3,.3,.3,.02);
    }
    private BlockPos anchor(int index) {return vessel.getBlockPos().offset(ANCHORS[index][0],0,ANCHORS[index][1]);}
    private CrystalPedestalBlockEntity pedestal(int index) {
        var level=vessel.getLevel();var pos=anchor(index);
        return level!=null && level.hasChunkAt(pos) && level.getBlockState(pos).is(ModBlocks.CRYSTAL_PEDESTAL.get())
                && level.getBlockEntity(pos) instanceof CrystalPedestalBlockEntity p?p:null;
    }
    public SwordShapingRecipe recipe() {
        if(vessel.getLevel()==null || active())return null;
        var inputs=new ArrayList<ItemStack>();
        for(int i=0;i<4;i++){var p=pedestal(i);if(p==null)return null;inputs.add(p.getItem(0));}
        var input=new SwordShapingRecipe.Input(vessel.getItem(0),inputs);
        return vessel.getLevel().getRecipeManager().getRecipeFor(ModRecipes.SWORD_SHAPING_TYPE.get(),input,vessel.getLevel()).map(h->h.value()).orElse(null);
    }
    public boolean start(ServerPlayer player) {
        var level=vessel.getLevel();var recipe=recipe();
        if(recipe==null || !player.isAlive() || player.isSpectator() || !vessel.stillValid(player) || !level.mayInteract(player,vessel.getBlockPos()))return false;
        for(int i=0;i<4;i++)if(!level.mayInteract(player,anchor(i)))return false;
        weapon=vessel.getItem(0).copy();result=recipe.result().copy();offerings=new ArrayList<>();
        for(int i=0;i<4;i++)offerings.add(pedestal(i).getItem(0).copy());
        elapsed=absorbed=0;vessel.setChanged();return true;
    }
    public void tick() {
        if(!(vessel.getLevel() instanceof ServerLevel level) || !active())return;
        if(wood) {
            if(!ItemStack.matches(weapon,vessel.getItem(0)) || result.isEmpty()){cancel();return;}
            elapsed++;if(elapsed%2==0)collapseParticles(level);vessel.setChanged();
            if(elapsed>=COLLAPSE_DURATION)complete(level);
            return;
        }
        // Wait for adjacent chunks instead of treating an unload as a removed pedestal.
        for(int i=0;i<4;i++)if(!level.hasChunkAt(anchor(i)))return;
        boolean valid=ItemStack.matches(weapon,vessel.getItem(0)) && offerings.size()==4 && !result.isEmpty();
        for(int i=0;i<4 && valid;i++) {
            var p=pedestal(i);valid=p!=null && (i<absorbed?p.getItem(0).isEmpty():ItemStack.matches(offerings.get(i),p.getItem(0)));
        }
        if(!valid){cancel();return;}
        elapsed++;
        int source=Math.min(3,elapsed/40);
        if(elapsed%2==0) {
            double phase=(elapsed%40)/40.0;
            var from=Vec3.atCenterOf(anchor(source)).add(0,.35,0);
            var to=Vec3.atCenterOf(vessel.getBlockPos());
            var point=from.lerp(to,phase).add(0,Math.sin(phase*Math.PI)*.4,0);
            level.sendParticles(ParticleTypes.ENCHANT,point.x,point.y,point.z,2,.025,.025,.025,0);
        }
        if(elapsed%40==0 && absorbed<4){pedestal(absorbed).removeItem(0,1);absorbed++;}
        if(elapsed>DURATION-COLLAPSE_DURATION && elapsed%2==0)collapseParticles(level);
        vessel.setChanged();
        if(elapsed>=DURATION)complete(level);
    }
    private void complete(ServerLevel level) {
        var output=result.copy();var source=weapon.copy();var pos=vessel.getBlockPos();
        vessel.removeItemNoUpdate(0);
        // Clear the transaction before onRemove, which would otherwise refund offerings.
        weapon=ItemStack.EMPTY;
        if(!level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState())) {weapon=source;vessel.setItem(0,source);cancel();return;}
        Block.popResource(level,pos,output);
        level.playSound(null,pos,net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_RESONATE,net.minecraft.sounds.SoundSource.BLOCKS,1,.7F);
        clear();
    }
    public void cancel() {
        if(!(vessel.getLevel() instanceof ServerLevel level) || !active())return;
        for(int i=0;i<absorbed && i<offerings.size();i++) {
            var p=pedestal(i);var refund=offerings.get(i).copyWithCount(1);
            if(p!=null && p.getItem(0).isEmpty())p.setItem(0,refund);else Block.popResource(level,vessel.getBlockPos(),refund);
        }
        clear();vessel.setChanged();
    }
    private void clear() {weapon=ItemStack.EMPTY;result=ItemStack.EMPTY;offerings=List.of();elapsed=absorbed=0;wood=false;}
    public void save(CompoundTag tag,HolderLookup.Provider registries) {
        if(!active())return;
        var data=new CompoundTag();data.put("Weapon",weapon.save(registries));data.put("Result",result.save(registries));
        var list=new ListTag();for(var item:offerings)list.add(item.save(registries));data.put("Offerings",list);
        data.putInt("Elapsed",elapsed);data.putInt("Absorbed",absorbed);data.putBoolean("Wood",wood);tag.put("SwordShaping",data);
    }
    public void load(CompoundTag tag,HolderLookup.Provider registries) {
        clear();if(!tag.contains("SwordShaping"))return;var data=tag.getCompound("SwordShaping");
        weapon=ItemStack.parseOptional(registries,data.getCompound("Weapon"));result=ItemStack.parseOptional(registries,data.getCompound("Result"));
        var items=new ArrayList<ItemStack>();var list=data.getList("Offerings",Tag.TAG_COMPOUND);
        for(int i=0;i<Math.min(4,list.size());i++)items.add(ItemStack.parseOptional(registries,list.getCompound(i)));
        wood=data.getBoolean("Wood");offerings=items;elapsed=Math.clamp(data.getInt("Elapsed"),0,(wood?COLLAPSE_DURATION:DURATION)-1);absorbed=wood?0:Math.clamp(data.getInt("Absorbed"),0,4);
    }
}
