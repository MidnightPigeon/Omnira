package com.mcmagic.omnira.item;

import com.mcmagic.omnira.block.entity.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public final class ResonanceTerminalItem extends Item {
    private final int capacity;
    public ResonanceTerminalItem(Properties properties){this(properties,4);}
    public ResonanceTerminalItem(Properties properties,int capacity){super(properties.stacksTo(1));this.capacity=capacity;}
    public static boolean isTerminal(ItemStack stack){return stack.getItem() instanceof ResonanceTerminalItem;}
    public static int capacity(ItemStack stack){return stack.getItem() instanceof ResonanceTerminalItem terminal?terminal.capacity:0;}
    @Override public void inventoryTick(ItemStack stack,Level level,net.minecraft.world.entity.Entity entity,int slot,boolean selected){
        if(level instanceof net.minecraft.server.level.ServerLevel server&&level.getGameTime()%20==0)ResonanceLinks.prune(stack,server,false);
    }
    public static CompoundTag data(ItemStack stack){return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();}
    public static void save(ItemStack stack,CompoundTag tag){stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));}
    public static boolean authorize(ItemStack stack,Player player) {
        var tag=data(stack);
        if(tag.hasUUID("Owner") && !tag.getUUID("Owner").equals(player.getUUID()))return false;
        if(!tag.hasUUID("Owner"))tag.putUUID("Owner",player.getUUID());
        if(!tag.hasUUID("Terminal"))tag.putUUID("Terminal",java.util.UUID.randomUUID());
        save(stack,tag);return true;
    }
    private static void message(Player player,String key){player.displayClientMessage(Component.translatable("message.omnira.resonance."+key),true);}
    @Override public InteractionResult useOn(UseOnContext context) {
        var player=context.getPlayer();if(player==null || !player.isShiftKeyDown())return InteractionResult.PASS;
        if(!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof ResonanceCoreBlockEntity core))return InteractionResult.PASS;
        if(context.getLevel().isClientSide)return InteractionResult.SUCCESS;
        var stack=context.getItemInHand();
        if(!authorize(stack,player) || !core.permits(player)){message(player,"denied");return InteractionResult.CONSUME;}
        ResonanceLinks.prune(stack,(net.minecraft.server.level.ServerLevel)context.getLevel(),false);
        var tag=data(stack);var id=tag.getUUID("Terminal");
        for(int i=0;i<capacity(stack);i++) {
            var link=tag.getCompound("Link"+i);
            if(link.hasUUID("Identity") && link.getUUID("Identity").equals(core.identity)) {
                tag.remove("Link"+i);save(stack,tag);
                if(id.equals(core.terminal)){core.terminal=null;core.setChanged();core.refresh();}
                message(player,"unbound");return InteractionResult.CONSUME;
            }
        }
        if(id.equals(core.terminal)) {
            core.terminal=null;core.setChanged();core.refresh();message(player,"unbound");return InteractionResult.CONSUME;
        }
        if(core.terminal!=null || core.kind()==0){message(player,"unavailable");return InteractionResult.CONSUME;}
        for(int i=0;i<capacity(stack);i++)if(!tag.contains("Link"+i)) {
            var link=new CompoundTag();link.putUUID("Identity",core.identity);link.putString("Dimension",context.getLevel().dimension().location().toString());
            link.putLong("Pos",core.getBlockPos().asLong());link.putInt("Kind",core.kind());
            tag.put("Link"+i,link);save(stack,tag);core.terminal=id;core.setChanged();core.refresh();
            message(player,"bound");return InteractionResult.CONSUME;
        }
        player.displayClientMessage(Component.translatable("message.omnira.resonance.full",capacity(stack)),true);return InteractionResult.CONSUME;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        var stack=player.getItemInHand(hand);
        if(!level.isClientSide) {
            if(!authorize(stack,player)){message(player,"denied");return InteractionResultHolder.fail(stack);}
            ResonanceLinks.prune(stack,(net.minecraft.server.level.ServerLevel)level,true);
            int source=hand==InteractionHand.OFF_HAND?40:player.getInventory().selected;
            player.openMenu(new SimpleMenuProvider((id,inventory,p)->new com.mcmagic.omnira.menu.ResonanceMenu(id,inventory,source),stack.getHoverName()),b->{b.writeInt(source);b.writeVarInt(capacity(stack));});
        }
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,java.util.List<Component> lines,TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.omnira.resonance_terminal",capacity(stack)).withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
