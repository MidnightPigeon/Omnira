package com.mcmagic.omnira.block.entity;

import com.mcmagic.omnira.menu.CrystalBallMenu;
import com.mcmagic.omnira.registry.ModBlockEntityTypes;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class CrystalBallBlockEntity extends RandomizableContainerBlockEntity {
    public final OrbUpgrades upgrades=new OrbUpgrades(this);
    public boolean stabilized() {return upgrades.has(com.mcmagic.omnira.item.OrbUpgradeItem.Kind.STABILIZATION);}
    public int multiplier() {return upgrades.multiplier(-1);}
    public int itemLimit(ItemStack stack,int multiplier) {return stack.getMaxStackSize()>1?stack.getMaxStackSize()*multiplier:1;}
    public boolean fitsMultiplier(int multiplier) {
        for(var stack:items)if(stack.getCount()>itemLimit(stack,multiplier))return false;
        return true;
    }
    @Override public int getMaxStackSize() {return 64*multiplier();}
    @Override public int getMaxStackSize(ItemStack stack) {return itemLimit(stack,multiplier());}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){return !com.mcmagic.omnira.item.bottle.PocketBottleItem.restricted(stack);}
    @Override public boolean isEmpty() {return hasPendingLoot() || super.isEmpty();}
    @Override public ItemStack getItem(int slot) {return hasPendingLoot()?ItemStack.EMPTY:super.getItem(slot);}
    @Override public ItemStack removeItem(int slot,int amount) {return hasPendingLoot()?ItemStack.EMPTY:super.removeItem(slot,amount);}
    @Override public ItemStack removeItemNoUpdate(int slot) {return hasPendingLoot()?ItemStack.EMPTY:super.removeItemNoUpdate(slot);}
    @Override public void setItem(int slot,ItemStack stack) {if(hasPendingLoot())return;items.set(slot,stack);setChanged();}
    private int automationTicks;
    public void serverTick() {
        if(++automationTicks>=20){automationTicks=0;com.mcmagic.omnira.world.structure.AuthoredStructureConnections.initialize(this);com.mcmagic.omnira.mire.MillPlan.initialize(this);OrbAutomation.tick(this);}
        else if(automationTicks==10 && upgrades.speedMultiplier()>1)OrbAutomation.tick(this,false);
    }
    protected void shattered(net.minecraft.server.level.ServerLevel server) {}
    public static final int SIZE=12;
    private NonNullList<ItemStack> items=NonNullList.withSize(SIZE,ItemStack.EMPTY);
    private boolean pendingLootDisplay;
    private boolean lootContainer;
    @Override public void unpackLootTable(net.minecraft.world.entity.player.Player player) {
        if(lootTable!=null) lootContainer=true;
        super.unpackLootTable(player);
    }
    @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,net.minecraft.world.entity.player.Player player) {
        var menu=super.createMenu(id,inventory,player);
        if(menu!=null && lootContainer && !player.isSpectator() && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            var advancement=serverPlayer.server.getAdvancements().get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("omnira","beyond_the_mist"));
            if(advancement!=null)serverPlayer.getAdvancements().award(advancement,"open_loot_ball");
        }
        return menu;
    }
    public static final int REPAIR_DELAY=200;
    private int impacts;
    private long repairAt;
    public int crackStage() {return impacts/3;}
    public int impactCount() {return impacts;}
    public void landedOn(net.minecraft.world.entity.LivingEntity entity) {
        if(!(level instanceof net.minecraft.server.level.ServerLevel server) || isRemoved() || stabilized()) return;
        if(++impacts>=9) {
            unpackLootTable(entity instanceof net.minecraft.world.entity.player.Player player?player:null);
            // onRemove drops the inventory once; setting air skips the ball's own loot table.
            if(server.setBlockAndUpdate(worldPosition,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState())) {
                shattered(server);
                net.minecraft.world.level.block.Block.popResource(server,worldPosition,new ItemStack(this instanceof LiquidCrystalBallBlockEntity?net.minecraft.world.item.Items.GLASS_PANE:net.minecraft.world.item.Items.GLASS));
                server.levelEvent(2001,worldPosition,net.minecraft.world.level.block.Block.getId(net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState()));
            }
            return;
        }
        repairAt=server.getGameTime()+REPAIR_DELAY;
        server.scheduleTick(worldPosition,getBlockState().getBlock(),REPAIR_DELAY);
        setChanged();
        if(impacts%3==0)server.playSound(null,worldPosition,net.minecraft.sounds.SoundEvents.GLASS_HIT,
                net.minecraft.sounds.SoundSource.BLOCKS,.8F,.65F);
    }
    public void repairCracks() {
        if(level==null || level.isClientSide || impacts==0) return;
        long now=level.getGameTime();
        if(now>=repairAt) {impacts--;repairAt=now+REPAIR_DELAY;setChanged();}
        if(impacts>0) level.scheduleTick(worldPosition,getBlockState().getBlock(),(int)Math.clamp(repairAt-now,1,REPAIR_DELAY));
    }
    @Override public void onLoad() {
        super.onLoad();
        if(level!=null && !level.isClientSide && impacts>0)
            level.scheduleTick(worldPosition,getBlockState().getBlock(),(int)Math.clamp(repairAt-level.getGameTime(),1,REPAIR_DELAY));
    }
    public boolean hasPendingLoot() {return lootTable!=null || pendingLootDisplay;}
    public final net.neoforged.neoforge.items.IItemHandler itemHandler=new OrbItemHandler(this);
    public CrystalBallBlockEntity(BlockPos pos,BlockState state) {super(ModBlockEntityTypes.CRYSTAL_BALL.get(),pos,state);}
    protected CrystalBallBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type,BlockPos pos,BlockState state) {super(type,pos,state);}
    @Override public int getContainerSize() {return SIZE;}
    @Override protected NonNullList<ItemStack> getItems() {return items;}
    @Override protected void setItems(NonNullList<ItemStack> items) {this.items=items;}
    @Override protected void collectImplicitComponents(net.minecraft.core.component.DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        // Only the explicit stabilized block drop may carry storage in BLOCK_ENTITY_DATA.
        components.set(net.minecraft.core.component.DataComponents.CONTAINER,null);
        components.set(net.minecraft.core.component.DataComponents.CONTAINER_LOOT,null);
    }
    /** Read without unpacking a structure's pending loot table. */
    public ItemStack displayItem(int slot) {return hasPendingLoot()?ItemStack.EMPTY:items.get(slot);}
    @Override public void setChanged() {
        super.setChanged();
        if(level!=null && !level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var tag=new CompoundTag();
        tag.putInt("Impacts",impacts);
        tag.putBoolean("PendingLoot",lootTable!=null);
        tag.put("Upgrades",upgrades.serializeNBT(registries));
        if(lootTable==null)saveItems(tag,registries);
        return tag;
    }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
    @Override protected Component getDefaultName() {return Component.translatable("block.omnira.crystal_ball");}
    @Override protected AbstractContainerMenu createMenu(int id,Inventory inventory) {return new CrystalBallMenu(id,inventory,this);}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        super.saveAdditional(tag,registries);
        tag.putBoolean("LootContainer",lootContainer || lootTable!=null);
        tag.putInt("Impacts",impacts);tag.putLong("RepairAt",repairAt);
        tag.put("Upgrades",upgrades.serializeNBT(registries));
        if(!trySaveLootTable(tag))saveItems(tag,registries);
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        if(tag.contains("LootTable")){
            tag=tag.copy();tag.putString("LootTable",com.mcmagic.omnira.config.OmniraLootConfig.canonicalTableId(tag.getString("LootTable")));
        }
        super.loadAdditional(tag,registries);items=NonNullList.withSize(SIZE,ItemStack.EMPTY);
        pendingLootDisplay=tag.getBoolean("PendingLoot");
        impacts=Math.clamp(tag.getInt("Impacts"),0,8);repairAt=tag.getLong("RepairAt");
        upgrades.deserializeNBT(registries,tag.getCompound("Upgrades"));
        if(!tryLoadLootTable(tag)) ContainerHelper.loadAllItems(tag,items,registries);
        lootContainer=tag.getBoolean("LootContainer") || lootTable!=null;
        var counts=tag.getIntArray("OrbCounts");
        for(int i=0;i<Math.min(counts.length,SIZE);i++)if(!items.get(i).isEmpty())items.get(i).setCount(Math.max(1,counts[i]));
    }
    private void saveItems(CompoundTag tag,HolderLookup.Provider registries) {
        var compact=NonNullList.withSize(SIZE,ItemStack.EMPTY);int[] counts=new int[SIZE];
        for(int i=0;i<SIZE;i++){counts[i]=items.get(i).getCount();compact.set(i,items.get(i).copyWithCount(1));}
        ContainerHelper.saveAllItems(tag,compact,registries);tag.putIntArray("OrbCounts",counts);
    }
}
