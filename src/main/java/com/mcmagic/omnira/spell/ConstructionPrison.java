package com.mcmagic.omnira.spell;

import com.mcmagic.omnira.block.entity.VoidCrystalBlockEntity;
import com.mcmagic.omnira.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.*;
import java.util.*;

/** A persisted group of temporary blocks, owned by one lock application rather than a global entity ID. */
public final class ConstructionPrison {
    public static boolean boss(LivingEntity target) {
        return com.mcmagic.omnira.item.bottle.BottleCaptureRules.forbidden(target);
    }
    public static boolean create(LivingEntity target,ServerPlayer caster,CompoundTag lock,int ticks) {
        if(!(target.level() instanceof ServerLevel level)||boss(target))return false;
        var box=target.getBoundingBox();
        var min=BlockPos.containing(box.minX-.01,box.minY,box.minZ-.01);
        var max=BlockPos.containing(box.maxX+.01,box.maxY-.001,box.maxZ+.01);
        long count=(long)(max.getX()-min.getX()+1)*(max.getY()-min.getY()+1)*(max.getZ()-min.getZ()+1);
        if(count>512)return false;
        var positions=new ArrayList<BlockPos>();
        for(var cursor:BlockPos.betweenClosed(min,max)) {
            var p=cursor.immutable();
            if(!level.hasChunkAt(p)||level.isOutsideBuildHeight(p)||!level.getWorldBorder().isWithinBounds(p)
                    ||caster!=null&&!UtilitySpellEffects.allowed(caster,p))return false;
            var state=level.getBlockState(p);
            // Do not overwrite terrain, fluids, containers, or another prison.
            if(!state.isAir()||level.getBlockEntity(p)!=null)return false;
            if(!level.getEntities(target,new net.minecraft.world.phys.AABB(p),e->e instanceof LivingEntity&&e.isAlive()).isEmpty())return false;
            positions.add(p);
        }
        UUID seal=UUID.randomUUID();var list=new ListTag();var placed=new ArrayList<BlockPos>();
        float hardness=(float)Math.clamp(2+target.getMaxHealth()/10.0+target.getArmorValue()*.5
                +target.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS)*.5,2,60);
        for(var p:positions) {
            var snapshot=net.neoforged.neoforge.common.util.BlockSnapshot.create(level.dimension(),level,p);
            if(!level.setBlock(p,ModBlocks.VOID_CRYSTAL.get().defaultBlockState(),3)) {rollback(level,placed);return false;}
            placed.add(p);
            if(caster!=null&&net.neoforged.neoforge.event.EventHooks.onBlockPlace(caster,snapshot,net.minecraft.core.Direction.UP)) {rollback(level,placed);return false;}
            if(!(level.getBlockEntity(p) instanceof VoidCrystalBlockEntity be)){rollback(level,placed);return false;}
            be.bind(target.getUUID(),seal,hardness,ticks);list.add(LongTag.valueOf(p.asLong()));
        }
        lock.putUUID("seal",seal);lock.put("crystals",list);return true;
    }
    private static void rollback(ServerLevel level,List<BlockPos> positions) {
        for(var p:positions)if(level.getBlockEntity(p) instanceof VoidCrystalBlockEntity)level.removeBlock(p,false);
    }
    public static void clear(LivingEntity target,CompoundTag lock) {
        if(!lock.hasUUID("seal")||!(target.level() instanceof ServerLevel current))return;
        var key=net.minecraft.resources.ResourceKey.create(Registries.DIMENSION,net.minecraft.resources.ResourceLocation.parse(lock.getString("dimension")));
        var level=current.getServer().getLevel(key);if(level==null)return;
        for(var value:lock.getList("crystals",4)) {
            var p=BlockPos.of(((LongTag)value).getAsLong());
            if(level.hasChunkAt(p)&&level.getBlockEntity(p) instanceof VoidCrystalBlockEntity be&&lock.getUUID("seal").equals(be.seal()))level.removeBlock(p,false);
        }
    }
    public static void refresh(LivingEntity target,CompoundTag lock,int ticks) {
        for(var value:lock.getList("crystals",4)) {
            var p=BlockPos.of(((LongTag)value).getAsLong());
            if(target.level().hasChunkAt(p)&&target.level().getBlockEntity(p) instanceof VoidCrystalBlockEntity be&&lock.getUUID("seal").equals(be.seal()))be.configure(ticks,false);
        }
    }
    public static void mine(Player miner,VoidCrystalBlockEntity crystal,ItemStack tool) {
        if(!(miner instanceof ServerPlayer player)||!tool.is(net.minecraft.tags.ItemTags.PICKAXES)||ConstructionLock.locked(player))return;
        var entity=player.serverLevel().getEntity(crystal.prisoner());
        if(!(entity instanceof LivingEntity target)||!target.isAlive()||boss(target)||!ConstructionLock.locked(target))return;
        var lock=target.getPersistentData().getCompound(ConstructionLock.KEY);
        if(!lock.hasUUID("seal")||!lock.getUUID("seal").equals(crystal.seal())||lock.getBoolean("executing"))return;
        if(target instanceof Player p&&(p.isCreative()||!player.canHarmPlayer(p)))return;
        lock.putBoolean("executing",true);
        int fortune=EnchantmentHelper.getItemEnchantmentLevel(player.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.FORTUNE),tool);
        var held=player.getMainHandItem();var killingTool=tool.copy();
        killingTool.enchant(player.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.LOOTING),fortune);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,killingTool);
        try {
            target.hurt(SpellDamageSource.of(target,net.minecraft.world.damagesource.DamageTypes.GENERIC_KILL,player,player,"mined"),Float.MAX_VALUE);
        } finally {
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,held);
            ConstructionLock.release(target);
        }
    }
}
