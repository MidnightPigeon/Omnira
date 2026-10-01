package com.mcmagic.omnira.archaeology;

import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;

public final class HyperDrillItem extends BlockItem {
    public HyperDrillItem(Block b,Properties p){super(b,p);}
    public static int charge(ItemStack s){var n=s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();return n.contains("DrillCharge")?Math.clamp(n.getInt("DrillCharge"),0,1200):1200;}
    public static ItemStack withCharge(ItemStack s,int charge){var n=s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();n.putInt("DrillCharge",charge);s.set(DataComponents.CUSTOM_DATA,CustomData.of(n));return s;}
    @Override public Component getName(ItemStack s){return Component.translatable(charge(s)>=1200?"block.omnira.hyperdimensional_drill":"block.omnira.hyperdimensional_drill.depleted");}
    @Override public InteractionResult place(BlockPlaceContext c){
        var level=c.getLevel();var player=c.getPlayer();var root=c.getClickedPos();if(player==null)return InteractionResult.FAIL;
        for(int n=0;n<18;n++){var p=root.offset(HyperDrillBlock.offset(n));
            if(level.isOutsideBuildHeight(p)||!level.hasChunkAt(p)||!level.getWorldBorder().isWithinBounds(p)||!level.getBlockState(p).canBeReplaced()
                    ||!level.mayInteract(player,p)||!player.mayUseItemAt(p,Direction.UP,c.getItemInHand())||!level.getEntities(player,new AABB(p),e->e.isAlive()&&e.isPickable()).isEmpty())return InteractionResult.FAIL;
        }
        if(!level.isClientSide){
            var snapshots=new java.util.ArrayList<net.neoforged.neoforge.common.util.BlockSnapshot>();
            for(int n=0;n<18;n++){var p=root.offset(HyperDrillBlock.offset(n));snapshots.add(net.neoforged.neoforge.common.util.BlockSnapshot.create(level.dimension(),level,p));}
            if(net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.level.BlockEvent.EntityMultiPlaceEvent(snapshots,level.getBlockState(root.below()),player)).isCanceled())return InteractionResult.FAIL;
            HyperDrillBlockEntity.install(level,root,charge(c.getItemInHand()));if(!player.isCreative())c.getItemInHand().shrink(1);
        }return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack s,TooltipContext c,java.util.List<Component> text,TooltipFlag flag){text.add(Component.translatable("tooltip.omnira.drill.charge",charge(s)*100/1200));}
}
