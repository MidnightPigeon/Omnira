package com.mcmagic.omnira.block.entity;

import com.mcmagic.omnira.registry.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class PureVesselBlockEntity extends net.minecraft.world.level.block.entity.BlockEntity {
    public final com.mcmagic.omnira.world.dimension.SwordShapingRitual ritual=new com.mcmagic.omnira.world.dimension.SwordShapingRitual(this);
    public static final TagKey<Item> WEAPONS=TagKey.create(Registries.ITEM,ResourceLocation.fromNamespaceAndPath("omnira","pure_vessel_weapons"));
    private ItemStack offering=ItemStack.EMPTY;
    public PureVesselBlockEntity(BlockPos pos,BlockState state) {super(ModBlockEntityTypes.PURE_VESSEL.get(),pos,state);}
    public static boolean accepts(ItemStack stack) {return stack.is(WEAPONS) || stack.is(net.minecraft.tags.ItemTags.LOGS);}
    public void tick() {
        ritual.tick();
        if(level==null || level.isClientSide || ritual.active() || getItem(0).isEmpty())return;
        // Return legacy stored items and interrupted offerings; this is no longer storage.
        refundOffering();
    }
    public ItemStack getItem(int slot) {return slot==0?offering:ItemStack.EMPTY;}
    public void setItem(int slot,ItemStack stack) {if(slot==0){offering=stack;setChanged();}}
    public ItemStack removeItemNoUpdate(int slot) {var old=getItem(slot);if(slot==0)offering=ItemStack.EMPTY;return old;}
    public boolean stillValid(net.minecraft.world.entity.player.Player player) {
        return level!=null && level.getBlockEntity(worldPosition)==this && player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(worldPosition))<=64;
    }
    public void refundOffering() {
        if(level!=null && !level.isClientSide && !offering.isEmpty()) {
            net.minecraft.world.level.block.Block.popResource(level,worldPosition,removeItemNoUpdate(0));setChanged();
        }
    }
    public boolean activate(net.minecraft.server.level.ServerPlayer player,ItemStack held) {
        if(level==null || ritual.active() || !offering.isEmpty() || !accepts(held) || !stillValid(player)
                || !player.isAlive() || player.isSpectator() || !player.mayBuild() || !level.mayInteract(player,worldPosition))return false;
        if(held.is(net.minecraft.tags.ItemTags.LOGS)) {
            var input=new net.minecraft.world.item.crafting.SingleRecipeInput(held.copyWithCount(1));
            var recipe=level.getRecipeManager().getRecipeFor(com.mcmagic.omnira.registry.ModRecipes.VESSEL_CONVERSION_TYPE.get(),input,level);
            if(recipe.isEmpty())return false;
            var output=recipe.get().value().assemble(input,level.registryAccess());
            if(output.isEmpty())return false;
            ritual.startWood(held,output);
            held.consume(1,player);
            return true;
        }
        setItem(0,held.copyWithCount(1));
        if(!ritual.start(player)){removeItemNoUpdate(0);setChanged();return false;}
        held.consume(1,player);return true;
    }
    @Override protected void saveAdditional(net.minecraft.nbt.CompoundTag tag,net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag,registries);
        if(!offering.isEmpty())tag.put("Offering",offering.save(registries));
        ritual.save(tag,registries);
    }
    @Override public void setChanged() {
        super.setChanged();
        if(level!=null && !level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
    }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
    @Override public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        var tag=new net.minecraft.nbt.CompoundTag();saveAdditional(tag,registries);return tag;
    }
    @Override protected void loadAdditional(net.minecraft.nbt.CompoundTag tag,net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag,registries);
        offering=ItemStack.parseOptional(registries,tag.getCompound("Offering"));
        if(offering.isEmpty() && tag.contains("Items")) {
            var legacy=net.minecraft.core.NonNullList.withSize(1,ItemStack.EMPTY);
            net.minecraft.world.ContainerHelper.loadAllItems(tag,legacy,registries);offering=legacy.get(0);
        }
        ritual.load(tag,registries);
    }
    @Override protected void collectImplicitComponents(net.minecraft.core.component.DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(net.minecraft.core.component.DataComponents.CONTAINER,null);
    }
    @Override public void removeComponentsFromTag(net.minecraft.nbt.CompoundTag tag) {super.removeComponentsFromTag(tag);tag.remove("SwordShaping");tag.remove("Offering");tag.remove("Items");}
}
