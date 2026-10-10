package com.mcmagic.omnira.space;

import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public final class SpacetimePortTuning {
    private static CompoundTag data(ItemStack rod){return rod.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();}
    public static boolean selected(ItemStack rod){return data(rod).contains("SpacetimePortSelection");}
    public static void cancel(ItemStack rod,Level l){
        var data=data(rod);var t=data.getCompound("SpacetimePortSelection");
        if(!l.isClientSide&&t.getString("Dimension").equals(l.dimension().location().toString())){
            var first=SpacetimePortState.at(l,BlockPos.of(t.getLong("Pos")));
            if(first!=null&&t.hasUUID("Identity")&&first.identity.equals(t.getUUID("Identity"))){first.selectedUntil=0;first.sync();}
        }
        data.remove("SpacetimePortSelection");rod.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
    }
    public static InteractionResult use(UseOnContext c){
        var l=c.getLevel();var player=c.getPlayer();var port=SpacetimePortState.at(l,c.getClickedPos());
        if(player==null||port==null)return InteractionResult.PASS;
        if(!player.mayBuild()||!l.mayInteract(player,port.host.getBlockPos())||!l.mayInteract(player,port.attached()))return InteractionResult.FAIL;
        if(l.isClientSide)return InteractionResult.SUCCESS;
        var rod=c.getItemInHand();
        if(port.peer!=null){port.unlink();cancel(rod,l);return InteractionResult.SUCCESS;}
        var data=data(rod);
        if(selected(rod)){
            var t=data.getCompound("SpacetimePortSelection");
            var first=t.getString("Dimension").equals(l.dimension().location().toString())?SpacetimePortState.at(l,BlockPos.of(t.getLong("Pos"))):null;
            boolean success=first!=null&&t.hasUUID("Identity")&&first.identity.equals(t.getUUID("Identity"))
                    &&l.mayInteract(player,first.host.getBlockPos())&&l.mayInteract(player,first.attached())&&SpacetimePortState.pair(first,port);
            cancel(rod,l);
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(success?"message.omnira.port.linked":"message.omnira.port.failed"),true);
            return success?InteractionResult.SUCCESS:InteractionResult.FAIL;
        }
        var t=new CompoundTag();t.putString("Dimension",l.dimension().location().toString());t.putLong("Pos",port.host.getBlockPos().asLong());t.putUUID("Identity",port.identity);
        data.put("SpacetimePortSelection",t);rod.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
        port.selectedUntil=Long.MAX_VALUE;port.sync();return InteractionResult.SUCCESS;
    }
}
