package com.mcmagic.omnira.ancient;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class AncientSummonItem extends Item {
    private final Supplier<? extends EntityType<? extends AncientCompanion>> type;
    public AncientSummonItem(Properties properties,Supplier<? extends EntityType<? extends AncientCompanion>> type) {
        super(properties); this.type=type;
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        if(context.getPlayer()==null)return InteractionResult.FAIL;
        BlockPos pos=context.getClickedPos();
        if(!context.getLevel().getBlockState(pos).canBeReplaced())pos=pos.relative(context.getClickedFace());
        return summon(context.getLevel(),context.getPlayer(),context.getItemInHand(),Vec3.atBottomCenterOf(pos));
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        ItemStack stack=player.getItemInHand(hand);
        Vec3 position=player.position().add(player.getLookAngle().multiply(2,0,2));
        return new InteractionResultHolder<>(summon(level,player,stack,position),stack);
    }
    private InteractionResult summon(Level level,Player player,ItemStack stack,Vec3 pos) {
        if(player.isSpectator() || !level.hasChunkAt(BlockPos.containing(pos))
                || !level.mayInteract(player,BlockPos.containing(pos)))return InteractionResult.FAIL;
        if(level.isClientSide)return InteractionResult.SUCCESS;
        AncientCompanion entity=type.get().create(level);
        if(entity==null)return InteractionResult.FAIL;
        entity.moveTo(pos.x,pos.y,pos.z,player.getYRot(),0);
        if(!level.noCollision(entity) || !level.getWorldBorder().isWithinBounds(entity.getBoundingBox()))return InteractionResult.FAIL;
        entity.setTame(true,true);
        entity.setOwnerUUID(player.getUUID());
        entity.setOrderedToSit(false);
        if(!level.addFreshEntity(entity))return InteractionResult.FAIL;
        stack.consume(1,player);
        return InteractionResult.CONSUME;
    }
}
