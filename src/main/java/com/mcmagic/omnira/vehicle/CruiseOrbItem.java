package com.mcmagic.omnira.vehicle;

import com.mcmagic.omnira.registry.ModEntityTypes;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

public final class CruiseOrbItem extends Item {
    public CruiseOrbItem(Properties properties){super(properties.stacksTo(1));}
    @Override public boolean canFitInsideContainerItems(ItemStack stack){return !com.mcmagic.omnira.item.PortableStorageRules.stabilized(stack);}
    @Override public InteractionResult useOn(UseOnContext context){return place(context.getLevel(),context.getPlayer(),context.getHand(),context.getClickLocation().add(0,.02,0));}
    @Override public InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level,net.minecraft.world.entity.player.Player player,InteractionHand hand){
        var hit=getPlayerPOVHitResult(level,player,ClipContext.Fluid.SOURCE_ONLY);
        if(hit.getType()!=HitResult.Type.BLOCK)return InteractionResultHolder.pass(player.getItemInHand(hand));
        return new InteractionResultHolder<>(place(level,player,hand,hit.getLocation().add(0,.02,0)),player.getItemInHand(hand));
    }
    private InteractionResult place(net.minecraft.world.level.Level level,net.minecraft.world.entity.player.Player player,InteractionHand hand,Vec3 pos){
        if(player==null || !player.mayBuild() || !level.mayInteract(player,net.minecraft.core.BlockPos.containing(pos)))return InteractionResult.FAIL;
        var orb=ModEntityTypes.CRUISE_ORB.get().create(level);if(orb==null)return InteractionResult.FAIL;
        orb.moveTo(pos.x,pos.y,pos.z,player.getYRot()+180,0);
        if(!level.noCollision(orb,orb.getBoundingBox()) || !level.getWorldBorder().isWithinBounds(orb.getBoundingBox()))return InteractionResult.FAIL;
        if(!level.isClientSide){orb.restoreItem(player.getItemInHand(hand));if(!level.addFreshEntity(orb))return InteractionResult.FAIL;player.getItemInHand(hand).consume(1,player);}
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
