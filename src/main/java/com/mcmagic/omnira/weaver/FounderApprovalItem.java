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
    public static final String TIMERS = "OmniraFounderTicks";
    private static final String LEGACY = "OmniraFounderCooldowns";

    private static CompoundTag timers(Player player) {
        var data = player.getPersistentData();
        var timers = data.getCompound(TIMERS);
        if (data.contains(LEGACY)) {
            var old = data.getCompound(LEGACY);
            long now = player.level().getGameTime();
            for (Action action : Action.values()) {
                long end = old.getLong(action.name());
                if (com.mcmagic.omnira.util.GameTimeCooldown.active(now, end, action.ticks))
                    timers.putInt(action.name(), (int)(end - now));
            }
            data.remove(LEGACY);
            if (!timers.isEmpty()) data.put(TIMERS, timers);
        }
        return timers;
    }

    public static boolean ready(Player player, Action action) {
        return timers(player).getInt(action.name()) <= 0;
    }

    public static boolean hasTimers(Player player) {
        var data = player.getPersistentData();
        return data.contains(TIMERS) || data.contains(LEGACY);
    }

    public static void advance(Player player) {
        var data = player.getPersistentData();
        if (!data.contains(TIMERS) && !data.contains(LEGACY)) return;
        var timers = timers(player);
        for (Action action : Action.values()) {
            int ticks = timers.getInt(action.name());
            if (ticks > 1 && ticks <= action.ticks) timers.putInt(action.name(), ticks - 1);
            else timers.remove(action.name());
        }
        if (timers.isEmpty()) data.remove(TIMERS);
    }

    public static boolean cast(ServerPlayer player,Action action){
        if(!player.isAlive()||player.isSpectator()||!held(player)||player.containerMenu!=player.inventoryMenu||!ready(player,action))return false;
        var clocks = timers(player);
        clocks.putInt(action.name(), action.ticks);
        player.getPersistentData().put(TIMERS, clocks);
        switch(action){
            case BEND -> WeaverCombat.bend(player,player.getRandom().nextBoolean());
            case RAY -> WeaverCombat.ray(player,player.getLookAngle());
            case BOLT -> {
                Vec3 from=player.getEyePosition(),end=from.add(player.getLookAngle().scale(64));
                var hit=net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(player,from,end,
                        player.getBoundingBox().expandTowards(end.subtract(from)).inflate(1),e->{var living=WeaverBolt.contactTarget(e);return living!=null&&WeaverCombat.eligible(living,player);},64*64);
                LivingEntity target=hit==null?null:WeaverBolt.contactTarget(hit.getEntity());
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
