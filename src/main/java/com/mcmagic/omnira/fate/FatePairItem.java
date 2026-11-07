package com.mcmagic.omnira.fate;

import com.mcmagic.omnira.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;

public class FatePairItem extends Item {
    private final java.util.function.Supplier<? extends Item> talent,curse;
    public FatePairItem(Properties properties,java.util.function.Supplier<? extends Item> talent,java.util.function.Supplier<? extends Item> curse){super(properties);this.talent=talent;this.curse=curse;}
    private static boolean available(ServerPlayer player,String slot){
        var handler=top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).flatMap(i->i.getStacksHandler(slot)).orElse(null);
        if(handler==null)return false;
        for(int i=0;i<handler.getSlots();i++)if(!handler.getStacks().getStackInSlot(i).isEmpty())return false;
        return true;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(player instanceof ServerPlayer server){
            if(!available(server,"talent")||!available(server,"curse")){
                server.displayClientMessage(Component.translatable("message.omnira.fate_pair.occupied"),true);return InteractionResultHolder.fail(stack);
            }
            if(!ManuscriptReward.grant(server,"talent",new ItemStack(talent.get())))return InteractionResultHolder.fail(stack);
            if(!ManuscriptReward.grant(server,"curse",new ItemStack(curse.get()))){ManuscriptReward.cleanse(server);return InteractionResultHolder.fail(stack);}
            stack.consume(1,player);server.swing(hand,true);
            level.playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,net.minecraft.sounds.SoundSource.PLAYERS,.8F,.7F);
        }
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
}
