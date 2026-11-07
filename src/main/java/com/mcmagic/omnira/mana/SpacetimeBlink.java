package com.mcmagic.omnira.mana;

import com.mcmagic.omnira.item.*;
import com.mcmagic.omnira.item.staff.StaffCastContext;
import com.mcmagic.omnira.registry.ModAttachments;
import com.mcmagic.omnira.space.anchor.StabilityField;
import com.mcmagic.omnira.spell.ConstructionLock;
import com.mcmagic.omnira.spell.SpellCooldowns;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.*;

public final class SpacetimeBlink {
    private static final int RANGE=24;
    private SpacetimeBlink(){}
    public static boolean staff(ItemStack stack){
        return stack.getItem() instanceof StaffItem || stack.getItem() instanceof IndustrialWheelItem;
    }
    public static double manaCost(double reduction){return ManaCosts.cost(50,1,reduction);}
    public static boolean cast(ServerPlayer player){
        var stack=player.getMainHandItem();
        if(!player.isAlive() || player.isSpectator() || player.isPassenger() || ConstructionLock.locked(player)
                || player.containerMenu!=player.inventoryMenu || player.getData(ModAttachments.AFFINITY).active()!=Affinity.SPACETIME
                || !staff(stack) || player.getCooldowns().isOnCooldown(stack.getItem()))return false;
        var hit=(BlockHitResult)player.pick(RANGE,1,false);
        Vec3 dest;
        if(hit.getType()==HitResult.Type.MISS){
            dest=player.position().add(player.getLookAngle().normalize().scale(RANGE));
        }else{
            var pos=hit.getBlockPos();
            dest=Vec3.atBottomCenterOf(pos.relative(hit.getDirection()));
            if(hit.getDirection().getAxis().isHorizontal()){
                var above=Vec3.atBottomCenterOf(pos.above());
                if(fits(player,above))dest=above;
            }
        }
        if(!fits(player,dest) || StabilityField.blocks(player,player.serverLevel(),dest))return false;
        var context=stack.getItem() instanceof IndustrialWheelItem?IndustrialWheelItem.context(player,stack):StaffCastContext.capture(player,stack);
        if(!ManaEvents.trySpendExact(player,manaCost(context.reduction())))return false;
        player.teleportTo(dest.x,dest.y,dest.z);
        player.fallDistance=0;
        player.getCooldowns().addCooldown(stack.getItem(),SpellCooldowns.ticks(context.stats().cooldownTicks(),context.cooldownReduction(),1));
        return true;
    }
    private static boolean fits(ServerPlayer player,Vec3 dest){
        var level=player.serverLevel();
        var box=player.getBoundingBox().move(dest.subtract(player.position()));
        return player.position().distanceToSqr(dest)<=RANGE*RANGE+.00001 && box.minY>=level.getMinBuildHeight()
                && box.maxY<=level.getMaxBuildHeight() && level.getWorldBorder().isWithinBounds(box)
                && level.hasChunksAt(BlockPos.containing(box.minX,box.minY,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ))
                && level.noCollision(player,box);
    }
}
