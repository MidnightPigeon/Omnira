package com.mcmagic.omnira.storage;

import com.mcmagic.omnira.block.entity.ResonanceCoreBlockEntity;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.BlockHitResult;

public final class TuningRodItem extends Item {
    public TuningRodItem(Properties properties){super(properties);}
    private static CompoundTag data(ItemStack stack){return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();}
    public static void cancel(ItemStack rod){var t=data(rod);t.remove("Node");t.remove("Identity");t.remove("Dimension");rod.set(DataComponents.CUSTOM_DATA,CustomData.of(t));}
    private static void cancel(ItemStack rod,Level level){
        com.mcmagic.omnira.space.SpacetimePortTuning.cancel(rod,level);
        com.mcmagic.omnira.space.FoldGateTuning.cancel(rod,level);
        var t=data(rod);
        if(t.hasUUID("Identity")&&t.getString("Dimension").equals(level.dimension().location().toString())){
            var pos=BlockPos.of(t.getLong("Node"));
            if(level.hasChunkAt(pos)&&level.getBlockEntity(pos) instanceof ResonanceCoreBlockEntity core&&core.identity.equals(t.getUUID("Identity"))){core.selectedUntil=0;core.setChanged();core.refresh();}
        }
        cancel(rod);
    }
    @Override public boolean isEnchantable(ItemStack stack){return true;}
    @Override public int getEnchantmentValue(){return 15;}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        if(!level.isClientSide)cancel(player.getItemInHand(hand),level);return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
    }
    @Override public InteractionResult useOn(UseOnContext context){
        var level=context.getLevel();var player=context.getPlayer();if(player==null)return InteractionResult.PASS;
        var rod=context.getItemInHand();var pos=context.getClickedPos();
        if(level.getBlockState(pos).getBlock() instanceof com.mcmagic.omnira.space.SpacetimePortBlock){
            if((data(rod).hasUUID("Identity")||com.mcmagic.omnira.space.FoldGateTuning.selected(rod))&&!level.isClientSide)cancel(rod,level);
            return com.mcmagic.omnira.space.SpacetimePortTuning.use(context);
        }
        if(com.mcmagic.omnira.space.SpacetimePortTuning.selected(rod)){
            if(!level.isClientSide)cancel(rod,level);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if(level.getBlockState(pos).getBlock() instanceof com.mcmagic.omnira.space.FoldGateBlock){
            if(data(rod).hasUUID("Identity")&&!level.isClientSide)cancel(rod,level);
            var result=com.mcmagic.omnira.space.FoldGateTuning.use(context);
            if(result!=InteractionResult.PASS)return result;
        }
        if(com.mcmagic.omnira.space.FoldGateTuning.selected(rod)){
            if(!level.isClientSide)com.mcmagic.omnira.space.FoldGateTuning.cancel(rod,level);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if(level.getBlockEntity(pos) instanceof ResonanceCoreBlockEntity node&&(node.kind()!=0||node.peer!=null)){
            if(!level.mayInteract(player,pos)||!level.mayInteract(player,pos.below()))return InteractionResult.FAIL;
            if(level.isClientSide)return InteractionResult.SUCCESS;
            if(node.peer!=null){NodeTransport.unpair(node);cancel(rod,level);return InteractionResult.SUCCESS;}
            var t=data(rod);
            if(t.hasUUID("Identity")&&t.getString("Dimension").equals(level.dimension().location().toString())){
                var firstPos=BlockPos.of(t.getLong("Node"));
                if(firstPos.distSqr(pos)<=32*32&&level.hasChunkAt(firstPos)&&level.getBlockEntity(firstPos) instanceof ResonanceCoreBlockEntity first
                    &&first.identity.equals(t.getUUID("Identity"))&&!firstPos.equals(pos)
                    &&level.mayInteract(player,firstPos)&&level.mayInteract(player,firstPos.below())){
                    if(!NodeTransport.pair(first,node)){
                        String reason=StorageContainerIdentity.sameStorage(first,node)?"same_storage":"output_exists";
                        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.omnira.tuning."+reason),true);
                        cancel(rod,level);return InteractionResult.FAIL;
                    }
                    cancel(rod);return InteractionResult.SUCCESS;
                }
                cancel(rod,level);return InteractionResult.FAIL;
            }
            if(!TuningOutputs.get((net.minecraft.server.level.ServerLevel)level).available(node)){
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.omnira.tuning.output_exists"),true);
                return InteractionResult.CONSUME;
            }
            cancel(rod,level);t=data(rod);t.putLong("Node",pos.asLong());t.putUUID("Identity",node.identity);
            t.putString("Dimension",level.dimension().location().toString());rod.set(DataComponents.CUSTOM_DATA,CustomData.of(t));
            node.selectedUntil=Long.MAX_VALUE;node.setChanged();node.refresh();return InteractionResult.SUCCESS;
        }
        if(data(rod).hasUUID("Identity")){if(!level.isClientSide)cancel(rod,level);return InteractionResult.sidedSuccess(level.isClientSide);}
        return harvest(level,player,rod,new BlockHitResult(context.getClickLocation(),context.getClickedFace(),pos,context.isInside()));
    }
    public static InteractionResult harvest(Level level,Player player,ItemStack rod,BlockHitResult hit){
        var pos=hit.getBlockPos();var state=level.getBlockState(pos);
        if(!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,hit.getDirection(),rod))return InteractionResult.FAIL;
        String path=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        boolean sweet=state.getBlock() instanceof SweetBerryBushBlock&&state.getValue(SweetBerryBushBlock.AGE)>=2;
        boolean cave=state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.BERRIES)&&state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.BERRIES)
            &&state.getBlock() instanceof CaveVines;
        if(sweet||cave){
            if(level.isClientSide)return InteractionResult.SUCCESS;
            var fortune=level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE);
            int bonus=net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(fortune,rod);
            int base=sweet?1+level.random.nextInt(2)+(state.getValue(SweetBerryBushBlock.AGE)==3?1:0):1;
            int amount=base+(bonus>0?level.random.nextInt(bonus+1):0);
            level.setBlockAndUpdate(pos,sweet?state.setValue(SweetBerryBushBlock.AGE,1):state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.BERRIES,false));
            Block.popResource(level,pos,new ItemStack(sweet?Items.SWEET_BERRIES:Items.GLOW_BERRIES,amount));
            return InteractionResult.SUCCESS;
        }
        if(state.is(Blocks.MELON)||state.getBlock() instanceof PumpkinBlock){
            if(level.isClientSide)return InteractionResult.SUCCESS;
            var tool=rod.copy();
            if(state.is(Blocks.MELON)){
                var fortune=level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE);
                net.minecraft.world.item.enchantment.EnchantmentHelper.updateEnchantments(tool,enchants->enchants.set(fortune,0));
            }
            var drops=Block.getDrops(state,(net.minecraft.server.level.ServerLevel)level,pos,level.getBlockEntity(pos),player,tool);
            level.destroyBlock(pos,false,player);
            for(var stack:drops)Block.popResource(level,pos,stack);
            return InteractionResult.SUCCESS;
        }
        boolean reset=state.getBlock() instanceof CropBlock||path.contains("coffee")||state.getBlock() instanceof CocoaBlock;
        var age=state.getProperties().stream().filter(p->p instanceof net.minecraft.world.level.block.state.properties.IntegerProperty&&p.getName().equals("age")).findFirst().orElse(null);
        if(age==null)return InteractionResult.PASS;
        @SuppressWarnings("unchecked") var growth=(net.minecraft.world.level.block.state.properties.IntegerProperty)age;
        int max=java.util.Collections.max(growth.getPossibleValues());
        if(state.getValue(growth)<max)return InteractionResult.PASS;
        if(level.isClientSide)return InteractionResult.SUCCESS;
        if(!reset)return state.useWithoutItem(level,player,hit);
        var drops=Block.getDrops(state,(net.minecraft.server.level.ServerLevel)level,pos,level.getBlockEntity(pos),player,rod);
        if(state.getBlock() instanceof CropBlock){
            var seed=state.getBlock().asItem();
            for(var stack:drops)if(stack.is(seed)){stack.shrink(1);break;}
        }
        level.setBlockAndUpdate(pos,state.setValue(growth,java.util.Collections.min(growth.getPossibleValues())));
        for(var stack:drops)Block.popResource(level,pos,stack);
        return InteractionResult.SUCCESS;
    }
    @net.neoforged.fml.common.EventBusSubscriber(modid="omnira")
    public static final class HarvestEvents {
        @net.neoforged.bus.api.SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.HIGHEST)
        public static void rightClick(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event){
            if(!(event.getItemStack().getItem() instanceof TuningRodItem))return;
            var result=new UseOnContext(event.getEntity(),event.getHand(),event.getHitVec());
            var response=event.getItemStack().getItem().useOn(result);
            if(response!=InteractionResult.PASS){event.setCanceled(true);event.setCancellationResult(response);}
        }
    }
}
