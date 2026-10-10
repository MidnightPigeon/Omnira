package com.mcmagic.omnira.space;

import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;

public final class FoldGateTuning {
    private FoldGateTuning(){}
    public static boolean selected(ItemStack stack){return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().contains("FoldGateSelection");}
    public static void cancel(ItemStack stack){var t=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();t.remove("FoldGateSelection");stack.set(DataComponents.CUSTOM_DATA,CustomData.of(t));}
    public static void cancel(ItemStack stack,net.minecraft.world.level.Level level){
        var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getCompound("FoldGateSelection");
        if(level instanceof ServerLevel server){
            var id=ResourceLocation.tryParse(data.getString("Dimension"));
            var first=id==null?null:FoldGateBlockEntity.resolve(server.getServer(),GlobalPos.of(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,id),BlockPos.of(data.getLong("Pos"))));
            if(first!=null&&data.hasUUID("Identity")&&first.identity.equals(data.getUUID("Identity"))){first.selectedUntil=0;first.sync();}
        }
        cancel(stack);
    }
    public static InteractionResult use(UseOnContext c){
        var l=c.getLevel();var s=l.getBlockState(c.getClickedPos());
        if(!(s.getBlock() instanceof FoldGateBlock)||!FoldGateBlock.stable(s))return InteractionResult.PASS;
        var p=FoldGateBlock.base(s,c.getClickedPos());var player=c.getPlayer();
        if(player==null||!player.mayBuild()||!l.mayInteract(player,p))return InteractionResult.FAIL;
        if(l.isClientSide)return InteractionResult.SUCCESS;
        if(!(l.getBlockEntity(p) instanceof FoldGateBlockEntity gate))return InteractionResult.FAIL;
        var rod=c.getItemInHand();var data=rod.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        if(gate.peer!=null){gate.unlink();cancel(rod,l);return InteractionResult.SUCCESS;}
        if(selected(rod)){
            var t=data.getCompound("FoldGateSelection");var id=ResourceLocation.tryParse(t.getString("Dimension"));
            var first=id==null?null:FoldGateBlockEntity.resolve(((ServerLevel)l).getServer(),GlobalPos.of(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,id),BlockPos.of(t.getLong("Pos"))));
            boolean linked=first!=null&&t.hasUUID("Identity")&&first.identity.equals(t.getUUID("Identity"))
                    &&first.getLevel().mayInteract(player,first.getBlockPos())&&FoldGateBlockEntity.pair(first,gate);
            cancel(rod,l);player.displayClientMessage(net.minecraft.network.chat.Component.translatable(linked?"message.omnira.fold_gate.linked":"message.omnira.fold_gate.failed"),true);
            return InteractionResult.SUCCESS;
        }
        var t=new net.minecraft.nbt.CompoundTag();t.putString("Dimension",l.dimension().location().toString());t.putLong("Pos",p.asLong());t.putUUID("Identity",gate.identity);data.put("FoldGateSelection",t);
        rod.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
        gate.selectedUntil=l.getGameTime()+1200;gate.sync();
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.omnira.fold_gate.selected"),true);return InteractionResult.SUCCESS;
    }
}
