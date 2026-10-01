package com.mcmagic.omnira.reversal;

import net.minecraft.world.item.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.*;
import net.minecraft.server.level.ServerPlayer;
import com.mcmagic.omnira.fate.ManuscriptReward;

public final class TalentEssenceItem extends Item {
    private final java.util.function.Supplier<Item> talent;
    private final boolean drink;
    public TalentEssenceItem(Properties p,java.util.function.Supplier<Item> talent,boolean drink){super(p);this.talent=talent;this.drink=drink;}
    public static boolean eligible(Player p){return top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(p).flatMap(i->i.getStacksHandler("talent")).map(h->{for(int i=0;i<h.getSlots();i++)if(!h.getStacks().getStackInSlot(i).isEmpty())return false;return true;}).orElse(false);}
    @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand hand){if(!eligible(p))return InteractionResultHolder.fail(p.getItemInHand(hand));p.startUsingItem(hand);return InteractionResultHolder.consume(p.getItemInHand(hand));}
    @Override public int getUseDuration(ItemStack s,net.minecraft.world.entity.LivingEntity e){return 32;}
    @Override public UseAnim getUseAnimation(ItemStack s){return drink?UseAnim.DRINK:UseAnim.EAT;}
    @Override public ItemStack finishUsingItem(ItemStack s,Level l,net.minecraft.world.entity.LivingEntity e){
        if(e instanceof ServerPlayer p&&eligible(p)&&ManuscriptReward.grant(p,"talent",new ItemStack(talent.get()))){
            ManuscriptReward.grant(p,"curse",new ItemStack(ReversalContent.CURSE.get()));
            if(!p.getAbilities().instabuild)s.shrink(1);
        }
        return s;
    }
}
