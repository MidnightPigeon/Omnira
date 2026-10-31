package com.mcmagic.omnira.item;

import com.mcmagic.omnira.fate.ManuscriptReward;
import com.mcmagic.omnira.mana.*;
import com.mcmagic.omnira.registry.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import top.theillusivec4.curios.api.CuriosApi;

@net.neoforged.fml.common.EventBusSubscriber(modid="omnira")
public final class FulfillmentStarItem extends Item {
    public static final int ACTIVATION_TICKS=64, SHATTER_TICK=44;
    private static final int AFFINITY_EFFECT=1, CURSE_EFFECT=2;
    @net.neoforged.bus.api.SubscribeEvent public static void tick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event){
        if(event.getEntity() instanceof ServerPlayer player)advance(player);
    }
    @net.neoforged.bus.api.SubscribeEvent public static void dimension(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event){
        event.getEntity().setData(ModAttachments.FULFILLMENT_ACTIVATION,0);
        event.getEntity().setData(ModAttachments.FULFILLMENT_EFFECTS,0);
    }
    public static void advance(ServerPlayer player){
        int timer=player.getData(ModAttachments.FULFILLMENT_ACTIVATION);
        if(timer==0)return;
        var stack=player.getItemInHand(timer>0?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND);
        if(!player.isAlive()||player.isSpectator()||!stack.is(ModItems.FULFILLMENT_STAR.get())){
            player.setData(ModAttachments.FULFILLMENT_ACTIVATION,0);player.setData(ModAttachments.FULFILLMENT_EFFECTS,0);return;
        }
        int left=Math.abs(timer)-1;
        player.setData(ModAttachments.FULFILLMENT_ACTIVATION,Integer.signum(timer)*left);
        if(left==ACTIVATION_TICKS-SHATTER_TICK)player.level().playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_BREAK,net.minecraft.sounds.SoundSource.PLAYERS,.8F,1.3F);
        if(left==0){
            int effects=player.getData(ModAttachments.FULFILLMENT_EFFECTS);
            player.setData(ModAttachments.FULFILLMENT_EFFECTS,0);
            if(effects!=0)complete(stack,player,effects);
        }
    }
    public FulfillmentStarItem(Properties properties){super(properties);}
    private static int evaluateEffects(Player player){
        var state=player.getData(ModAttachments.AFFINITY);
        boolean affinity=state.active()!=Affinity.NONE&&!state.perfected();
        boolean curse=CuriosApi.getCuriosInventory(player).flatMap(i->i.getStacksHandler("curse")).map(h->{
            for(int i=0;i<h.getSlots();i++)if(!h.getStacks().getStackInSlot(i).isEmpty())return true;
            return false;
        }).orElse(false);
        return (affinity?AFFINITY_EFFECT:0)|(curse?CURSE_EFFECT:0);
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        // Ignore repeat clicks during an accepted activation before inspecting either qualification.
        if(!level.isClientSide&&player.getData(ModAttachments.FULFILLMENT_ACTIVATION)==0){
            int effects=evaluateEffects(player);
            if(effects==0){
                player.displayClientMessage(Component.translatable("message.omnira.fulfillment_star.no_effect"),true);
                return InteractionResultHolder.fail(stack);
            }
            player.setData(ModAttachments.FULFILLMENT_EFFECTS,effects);
            player.setData(ModAttachments.FULFILLMENT_ACTIVATION,hand==InteractionHand.MAIN_HAND?ACTIVATION_TICKS:-ACTIVATION_TICKS);
            level.playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,net.minecraft.sounds.SoundSource.PLAYERS,.6F,1.1F);
        }
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    private static void complete(ItemStack stack,ServerPlayer player,int effects){
        var state=player.getData(ModAttachments.AFFINITY);
        if((effects&AFFINITY_EFFECT)!=0)
            player.setData(ModAttachments.AFFINITY,new AffinityState(state.kind(),state.penaltyTicks(),true));
        if((effects&CURSE_EFFECT)!=0)ManuscriptReward.cleanseCurse(player);
        AffinityEffects.refresh(player);
        var mana=player.getData(ModAttachments.MANA);
        player.setData(ModAttachments.MANA,new ManaState(mana.current(),player.getAttributeValue(ModAttributes.MAX_MANA),player.getData(ModAttachments.AFFINITY).active().ordinal()));
        player.setHealth(player.getMaxHealth());player.getFoodData().setFoodLevel(20);
        for(var slot:EquipmentSlot.values())repair(player.getItemBySlot(slot));
        CuriosApi.getCuriosInventory(player).ifPresent(inv->inv.getCurios().values().forEach(h->{
            for(int i=0;i<h.getSlots();i++)repair(h.getStacks().getStackInSlot(i));
        }));
        if(!player.getAbilities().instabuild)stack.shrink(1);
        player.inventoryMenu.broadcastChanges();
        for(int i=0;i<32;i++){
            double angle=i*Math.PI*2/32,r=.15+player.getRandom().nextDouble()*.55;
            player.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                    player.getX()+Math.cos(angle)*r,player.getY()+player.getBbHeight()+.35,player.getZ()+Math.sin(angle)*r,
                    0,Math.cos(angle)*.02,-.06-player.getRandom().nextDouble()*.04,Math.sin(angle)*.02,1);
        }
        player.playSound(net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,1,1.3F);
    }
    private static void repair(ItemStack stack){if(stack.isDamageableItem())stack.setDamageValue(0);}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,java.util.List<Component> lines,TooltipFlag flag){
        lines.add(Component.translatable("tooltip.omnira.fulfillment_star").withStyle(net.minecraft.ChatFormatting.WHITE,net.minecraft.ChatFormatting.ITALIC));
    }
}
