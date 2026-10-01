package com.mcmagic.omnira.spell;

import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.block.entity.VoidCrystalBlockEntity;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.CommonHooks;

public final class UtilitySpellEffects {
    public static boolean allowed(ServerPlayer player,BlockPos pos) {
        return player.isAlive() && !player.isSpectator() && player.mayBuild() && player.serverLevel().hasChunkAt(pos)
                && !player.level().isOutsideBuildHeight(pos) && player.level().getWorldBorder().isWithinBounds(pos)
                && player.level().mayInteract(player,pos);
    }
    public static void applyLiving(SpellEffect effect,Entity source,Entity owner,LivingEntity target,double power) {
        applyLiving(effect,source,owner,target,power,true);
    }
    public static void applyLiving(SpellEffect effect,Entity source,Entity owner,LivingEntity target,double power,boolean physicalDamage) {
        if(!(owner instanceof ServerPlayer player) || !target.isAlive() || target.isSpectator()
                || target instanceof Player p && (p.isCreative() || !player.canHarmPlayer(p))) return;
        if(effect.operation().equals("construction")) {ConstructionLock.apply(target,effect,player);return;}
        int rounds=1+effect.enhancement(),yield=1+effect.infusion();
        for(int round=0;round<rounds && target.isAlive();round++) {
            int remaining=yield;
            for(EquipmentSlot slot:EquipmentSlot.values()) {
                var equipment=target.getItemBySlot(slot);
                if(!equipment.isEmpty() && remaining-->0) {
                    target.setItemSlot(slot,ItemStack.EMPTY);
                    target.spawnAtLocation(equipment.copy());
                }
                if(remaining<=0) break;
            }
            if(target instanceof Sheep sheep && sheep.readyForShearing()) {
                var shears=new ItemStack(Items.SHEARS);
                var wool=sheep.onSheared(player,shears,target.level(),target.blockPosition());
                for(var stack:wool) target.spawnAtLocation(stack.copy());
                if(!wool.isEmpty() && effect.infusion()>0) target.spawnAtLocation(wool.getFirst().copyWithCount(effect.infusion()));
            } else if(target instanceof Chicken chicken && !chicken.isBaby()) {
                target.spawnAtLocation(new ItemStack(Items.EGG,yield));
            } else if(target instanceof Cow cow && !cow.isBaby()) {
                for(int i=0;i<yield;i++) target.spawnAtLocation(new ItemStack(ModItems.MILK_SUSPENSION.get()));
            }
        }
        if(physicalDamage && effect.delay()==0) target.hurt(SpellDamageSource.physical(target,source,owner,"dissociation"),effect.physicalDamage(power));
    }
    public static void applyBlock(SpellEffect effect,Entity owner,BlockPos hit,Direction face,BlockPos placement) {
        applyBlock(effect,owner,hit,face,placement,new BlockBatch());
    }
    public static void applyBurstBlocks(java.util.List<SpellEffect> effects,Entity owner,java.util.List<BlockPos> positions) {
        var utility=effects.stream().filter(SpellEffect::utility).toList();
        var batch=new BlockBatch();
        for(var pos:positions) for(var effect:utility) applyBlock(effect,owner,pos,Direction.UP,pos,batch);
    }
    private static final class BlockBatch {
        final java.util.Map<SpellEffect,ItemStack> tools=new java.util.HashMap<>();
        int breakEffects;
        ItemStack tool(SpellEffect effect,ServerLevel level) {
            return tools.computeIfAbsent(effect,key->{
                var tool=new ItemStack(key.enhancement()==0?Items.IRON_PICKAXE:key.enhancement()==1?Items.DIAMOND_PICKAXE:Items.NETHERITE_PICKAXE);
                tool.enchant(level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.FORTUNE),1+key.infusion());
                return tool;
            });
        }
    }
    private static void applyBlock(SpellEffect effect,Entity owner,BlockPos hit,Direction face,BlockPos placement,BlockBatch batch) {
        if(!effect.utility() || !(owner instanceof ServerPlayer player)) return;
        var level=player.serverLevel();
        if(effect.operation().equals("dissociation")) {
            if(!allowed(player,hit)) return;
            var state=level.getBlockState(hit);
            if(state.isAir() || state.getDestroySpeed(level,hit)<0) return;
            var tool=batch.tool(effect,level);
            if(state.requiresCorrectToolForDrops() && !tool.isCorrectToolForDrops(state)) return;
            if(CommonHooks.fireBlockBreak(level,player.gameMode.getGameModeForPlayer(),player,hit,state).isCanceled()) return;
            var blockEntity=level.getBlockEntity(hit);
            if(level.removeBlock(hit,false)) {
                if(batch.breakEffects++<16) level.levelEvent(2001,hit,Block.getId(state));
                // Custom block loot hooks may mutate the tool; keep the cached template pristine.
                state.getBlock().playerDestroy(level,player,hit,state,blockEntity,tool.copy());
            }
        } else if(effect.operation().equals("construction")) {
            if(com.mcmagic.omnira.spacetime.TimeWarpPointBlock.harvestNearby(level,placement,player))return;
            if(!allowed(player,placement)) return;
            var old=level.getBlockState(placement);
            if(!old.canBeReplaced() || !old.getFluidState().isEmpty() || level.getBlockEntity(placement)!=null) return;
            var block=effect.infusion()>0
                    ? (effect.enhancement()>0?ModBlocks.PERMANENT_REINFORCED_VOID_CRYSTAL:ModBlocks.PERMANENT_VOID_CRYSTAL)
                    : (effect.enhancement()>0?ModBlocks.REINFORCED_VOID_CRYSTAL:ModBlocks.VOID_CRYSTAL);
            var state=block.get().defaultBlockState();
            if(!level.isUnobstructed(state,placement,net.minecraft.world.phys.shapes.CollisionContext.empty())) return;
            var snapshot=net.neoforged.neoforge.common.util.BlockSnapshot.create(level.dimension(),level,placement);
            if(!level.setBlock(placement,state,3)) return;
            if(net.neoforged.neoforge.event.EventHooks.onBlockPlace(player,snapshot,face)) {
                snapshot.restore();return;
            }
            if(level.getBlockEntity(placement) instanceof VoidCrystalBlockEntity crystal)
                crystal.configure(400*(1<<effect.delay()),effect.infusion()>0);
        }
    }
}
