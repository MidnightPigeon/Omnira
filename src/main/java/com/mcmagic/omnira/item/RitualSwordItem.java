package com.mcmagic.omnira.item;

import com.mcmagic.omnira.menu.ArquebusMenu;
import com.mcmagic.omnira.menu.CrystalGridMenu;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.spell.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import java.util.List;

public final class RitualSwordItem extends SwordItem {
    public enum Kind { NAIL, NEEDLE }
    public final Kind kind;
    public RitualSwordItem(Properties properties,Kind kind) {
        super(CrystalPickaxeItem.CrystalTier.INFUSED,properties.attributes(SwordItem.createAttributes(CrystalPickaxeItem.CrystalTier.INFUSED,3,-2.2F)));
        this.kind=kind;
    }
    public static boolean accepts(ItemStack crystal) {
        if(!CrystalGridMenu.isCrystal(crystal))return false;
        var pattern=crystal.get(ModDataComponents.SPELL_PATTERN);
        return pattern.targetKeyword()==SpellTargetKeyword.SELF && pattern.shapeKeyword()==SpellShapeKeyword.BARRIER;
    }
    public static ItemStack crystal(ItemStack weapon) {return ArcaneArquebusItem.crystal(weapon);}
    @Override public float getAttackDamageBonus(Entity target,float base,net.minecraft.world.damagesource.DamageSource source) {
        // Vanilla calls this after separating enchantment damage, before critical hits.
        if(source.getEntity() instanceof Player player)
            return (float)(base*(CastAttributes.power(player,SwordActions.spellBonus(player))-1
                    +CrystalArmorEffects.meleeBonus(player)));
        return 0;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        var stack=player.getItemInHand(hand);
        if(kind==Kind.NEEDLE && FlyingNeedle.isOut(player))return InteractionResultHolder.fail(stack);
        if(player.isShiftKeyDown()) {
            if(!level.isClientSide) {
                int source=hand==InteractionHand.OFF_HAND?40:player.getInventory().selected;
                player.openMenu(new SimpleMenuProvider((id,inventory,p)->ArquebusMenu.sword(id,inventory,source),stack.getHoverName()),b->b.writeInt(source));
            }
            return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
        }
        if(!SwordFocus.canBegin(player,kind))return InteractionResultHolder.fail(stack);
        player.startUsingItem(hand);
        SwordFocus.begin(player,stack,kind);
        return InteractionResultHolder.consume(stack);
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        var player=context.getPlayer();
        return player!=null && player.isShiftKeyDown()?use(context.getLevel(),player,context.getHand()).getResult():InteractionResult.PASS;
    }
    @Override public int getUseDuration(ItemStack stack,LivingEntity entity) {return 60;}
    @Override public UseAnim getUseAnimation(ItemStack stack) {return UseAnim.NONE;}
    @Override public void onUseTick(Level level,LivingEntity entity,ItemStack stack,int remaining) {
        if(entity instanceof ServerPlayer player)SwordFocus.advance(player,stack,61-remaining);
    }
    @Override public void releaseUsing(ItemStack stack,Level level,LivingEntity entity,int remaining) {if(entity instanceof Player player)SwordFocus.stop(player);}
    @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity entity) {if(entity instanceof Player player)SwordFocus.stop(player);return stack;}
    @Override public void inventoryTick(ItemStack stack,Level level,Entity entity,int slot,boolean selected) {
        super.inventoryTick(stack,level,entity,slot,selected);
        if(entity instanceof ServerPlayer player && level.getGameTime()%10==0)HeldManaRepair.repair(stack,player);
    }
    public static void contact(Player attacker,Entity hit,boolean hurt) {
        if(!(attacker instanceof ServerPlayer player) || !player.isAlive() || player.isSpectator()
                || !(player.getMainHandItem().getItem() instanceof RitualSwordItem sword))return;
        var target=SpellCasting.livingTarget(hit);
        if(target==null || target==player || (!hurt && !(target instanceof com.mcmagic.omnira.entity.DreamMirror)))return;
        var weapon=player.getMainHandItem();
        var crystal=crystal(weapon);
        if(target.isAlive() && accepts(crystal) && (SwordActions.isCharged(player) || SwordSpellCooldowns.ready(player,weapon))) {
            var payload=SpellPayload.of(crystal);
            var mana=player.getData(ModAttachments.MANA);
            double cost=com.mcmagic.omnira.mana.ManaCosts.cost(payload.baseCost(),1,CastAttributes.reduction(player,0));
            if(mana.canSpend(cost) && (payload.damage()>0 || !payload.effects().isEmpty())) {
                player.setData(ModAttachments.MANA,mana.spend(cost));
                SwordSpellCooldowns.trigger(player,weapon);
                int invulnerability=target.invulnerableTime;
                // The sword's physical strike must not suppress its separate spell payload.
                target.invulnerableTime=0;
                try {
                    double power=CastAttributes.power(player,SwordActions.spellBonus(player));
                    boolean holy=HolyMagic.enabled(payload);
                    if(holy)HolyMagic.touch(player,target);
                    HolyMagic.cloud(player.serverLevel(),player,payload,power,1,target.position());
                    if(payload.damage()>0)hit.hurt(SpellDamageSource.physical(target,player,player,"spell"),(float)(payload.damage()*power*(holy?1.5:1)));
                    for(var effect:payload.effects())effect.apply(player,player,hit,power,holy);
                } finally {target.invulnerableTime=Math.max(invulnerability,target.invulnerableTime);}
            }
        }
        if(!player.onGround() && player.getXRot()>0) {
            SwordFocus.stop(player);player.stopUsingItem();
            // A directed airborne hit also works during ascent. The impulse leaves
            // time for the sword's attack recovery before the next pogo.
            var motion=player.getDeltaMovement();player.setDeltaMovement(motion.x,.72,motion.z);player.fallDistance=0;player.hurtMarked=true;
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));
        }
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag) {
        super.appendHoverText(stack,context,lines,flag);
        lines.add(Component.translatable("tooltip.omnira.ritual_sword."+(kind==Kind.NAIL?"nail":"needle")+".lore")
                .withStyle(style->style.withColor(kind==Kind.NAIL?0xFFFFFF:0xFFAAAA).withItalic(true)));
        lines.add(Component.translatable("tooltip.omnira.ritual_sword."+(kind==Kind.NAIL?"nail":"needle")+".charge").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.omnira.ritual_sword.load").withStyle(ChatFormatting.DARK_GRAY));
        if(!crystal(stack).isEmpty())lines.add(crystal(stack).getHoverName().copy().withStyle(ChatFormatting.GRAY));
    }
}
