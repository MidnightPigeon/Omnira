package com.mcmagic.omnira.weaver;

import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.*;
import net.minecraft.nbt.CompoundTag;
import java.util.List;

/** Three per-player action clocks: changing hands or exchanging copies cannot reset them. */
public final class FounderApprovalItem extends Item {
    public enum Action { BOLT(20),RAY(200),BEND(200);final int ticks;Action(int ticks){this.ticks=ticks;}}
    public FounderApprovalItem(Properties properties){super(properties.stacksTo(1).rarity(Rarity.EPIC).attributes(com.mcmagic.omnira.combat.HeldCombatAttributes.founder()));}
    public static boolean held(LivingEntity entity){return entity.getMainHandItem().getItem() instanceof FounderApprovalItem||entity.getOffhandItem().getItem() instanceof FounderApprovalItem;}
    public static boolean ready(Player player,Action action){return player.level().getGameTime()>=player.getPersistentData().getCompound("OmniraFounderCooldowns").getLong(action.name());}
    public static boolean cast(ServerPlayer player,Action action){
        if(!player.isAlive()||player.isSpectator()||!held(player)||player.containerMenu!=player.inventoryMenu||!ready(player,action))return false;
        var clocks=player.getPersistentData().getCompound("OmniraFounderCooldowns");clocks.putLong(action.name(),player.level().getGameTime()+action.ticks);player.getPersistentData().put("OmniraFounderCooldowns",clocks);
        switch(action){
            case BEND -> WeaverCombat.bend(player,player.getRandom().nextBoolean());
            case RAY -> WeaverCombat.ray(player,player.getLookAngle());
            case BOLT -> {
                Vec3 from=player.getEyePosition(),end=from.add(player.getLookAngle().scale(64));
                var hit=net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(player,from,end,
                        player.getBoundingBox().expandTowards(end.subtract(from)).inflate(1),e->e instanceof LivingEntity living&&WeaverCombat.eligible(living,player),64*64);
                LivingEntity target=hit!=null&&hit.getEntity() instanceof LivingEntity living?living:null;
                WeaverCombat.bolt(player,target,from,player.getLookAngle());
            }
        }
        return true;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        if(player instanceof ServerPlayer server)cast(server,player.isShiftKeyDown()?Action.RAY:Action.BOLT);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
    }
    @Override public InteractionResult useOn(UseOnContext context){return context.getPlayer()==null?InteractionResult.PASS:use(context.getLevel(),context.getPlayer(),context.getHand()).getResult();}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){
        for(String key:List.of("bolt","ray","bend"))lines.add(Component.translatable("tooltip.omnira.founder_approval."+key).withStyle(net.minecraft.ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.omnira.founder_approval.bypass").withStyle(net.minecraft.ChatFormatting.RED,net.minecraft.ChatFormatting.ITALIC));
    }
}
