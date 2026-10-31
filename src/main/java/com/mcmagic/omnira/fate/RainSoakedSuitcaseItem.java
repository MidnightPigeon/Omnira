package com.mcmagic.omnira.fate;

import com.mcmagic.omnira.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import java.util.List;

public final class RainSoakedSuitcaseItem extends BlockItem {
    public static final int ACTIVATION_TICKS=80;
    public RainSoakedSuitcaseItem(Properties properties){super(com.mcmagic.omnira.registry.ModBlocks.RAIN_SOAKED_SUITCASE.get(),properties);}
    @Override public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context){
        if(context.getPlayer()!=null&&context.getPlayer().isShiftKeyDown()){
            if(context.getClickedFace()!=net.minecraft.core.Direction.UP)return InteractionResult.FAIL;
            return super.useOn(context);
        }
        return InteractionResult.PASS;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(player.isShiftKeyDown())return InteractionResultHolder.pass(stack);
        if(player instanceof ServerPlayer server){
            if(!emptySlot(server,"talent")||!emptySlot(server,"curse")){
                server.displayClientMessage(Component.translatable("message.omnira.suitcase.occupied"),true);
                return InteractionResultHolder.fail(stack);
            }
            if(ManuscriptReward.grant(server,"talent",new ItemStack(ModItems.ARCANIST_TALENT.get()))){
                ManuscriptReward.grant(server,"curse",new ItemStack(ModItems.STORM_CURSE.get()));
                FateRuntime.data(server).remove(ArcanistFate.KEY);
                stack.set(com.mcmagic.omnira.registry.ModDataComponents.SUITCASE_OPEN_UNTIL,level.getGameTime()+ACTIVATION_TICKS);
                server.swing(hand,true);
                server.displayClientMessage(Component.translatable("message.omnira.suitcase.awakened"),true);
                level.playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,net.minecraft.sounds.SoundSource.PLAYERS,.7F,.8F);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    private static boolean emptySlot(ServerPlayer p,String name){
        var handler=top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(p).flatMap(i->i.getStacksHandler(name)).orElse(null);
        if(handler==null)return false;
        for(int i=0;i<handler.getSlots();i++)if(!handler.getStacks().getStackInSlot(i).isEmpty())return false;
        return true;
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){
        lines.add(Component.translatable("tooltip.omnira.rain_soaked_suitcase").withStyle(net.minecraft.ChatFormatting.BLUE));
    }
}
