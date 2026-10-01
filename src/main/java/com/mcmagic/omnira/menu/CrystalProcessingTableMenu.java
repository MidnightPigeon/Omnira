package com.mcmagic.omnira.menu;

import com.mcmagic.omnira.registry.ModBlocks;
import com.mcmagic.omnira.registry.ModDataComponents;
import com.mcmagic.omnira.registry.ModItems;
import com.mcmagic.omnira.registry.ModMenuTypes;
import com.mcmagic.omnira.spell.ElementType;
import com.mcmagic.omnira.spell.SpellPattern;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class CrystalProcessingTableMenu extends AbstractContainerMenu {
    public static final int WRITE_BUTTON = 0;
    public static final int MANA_COST = com.mcmagic.omnira.spell.SpellPayload.componentCost(2);
    public static final int TARGET_CORE_SLOT = 0;
    public static final int SHAPE_CORE_SLOT = 1;
    public static final int MODIFIER_SLOT_START = 2;
    public static final int MODIFIER_SLOT_END = 4;
    public static final int ELEMENT_SLOT_START = 4;
    public static final int ELEMENT_SLOT_END = 6;
    public static final int INK_SLOT = 6;
    public static final int SUBSTRATE_SLOT = 7;
    public static final int OUTPUT_SLOT = 8;
    public static final int TABLE_SLOT_COUNT = 9;

    private static final int PLAYER_INVENTORY_START = TABLE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container container;
    private final ContainerLevelAccess access;
    private final Player owner;
    private final TimedWork work = new TimedWork(60);

    public CrystalProcessingTableMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, createClientContainer(), createAccess(playerInventory.player.level(), extraData.readBlockPos()));
    }

    public CrystalProcessingTableMenu(int containerId, Inventory playerInventory, Container container) {
        this(containerId, playerInventory, container, ContainerLevelAccess.create(playerInventory.player.level(), BlockPos.ZERO));
    }

    public CrystalProcessingTableMenu(int containerId, Inventory playerInventory, Container container, BlockEntity blockEntity) {
        this(containerId, playerInventory, container, ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()));
    }

    private CrystalProcessingTableMenu(int containerId, Inventory playerInventory, Container container, ContainerLevelAccess access) {
        super(ModMenuTypes.CRYSTAL_PROCESSING_TABLE.get(), containerId);
        checkContainerSize(container, TABLE_SLOT_COUNT);
        this.container = container;
        this.access = access;
        this.owner = playerInventory.player;
        addDataSlot(work.progress);
        container.startOpen(playerInventory.player);

        addTableSlots(container);
        addPlayerInventorySlots(playerInventory);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.CRYSTAL_PROCESSING_TABLE.get());
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != WRITE_BUTTON || player != owner || player.level().isClientSide || !stillValid(player) || !canProcessCrystal()) return false;
        return work.start(player,container);
    }

    public boolean isWorking() { return work.active(); }
    public float workProgress() { return work.fraction(); }

    @Override
    public void broadcastChanges() {
        work.tick(owner,container,stillValid(owner) && canProcessCrystal(),() -> finishWriting(owner,WRITE_BUTTON));
        super.broadcastChanges();
    }

    private boolean finishWriting(Player player, int id) {
        if (id != WRITE_BUTTON || player != owner || !(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
                || !stillValid(player) || !canProcessCrystal()) {
            return false;
        }

        if (!com.mcmagic.omnira.mana.ManaEvents.trySpend(serverPlayer, baseManaCost())) return false;
        processCrystal();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            if (index < TABLE_SLOT_COUNT) {
                if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (isTargetMicrocore(stack)) {
                if (!moveItemStackTo(stack, TARGET_CORE_SLOT, SHAPE_CORE_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (isInk(stack)) {
                if (!moveItemStackTo(stack, INK_SLOT, INK_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (stack.is(ModItems.SPIRITUAL_CRYSTAL.get())) {
                if (!moveItemStackTo(stack, SUBSTRATE_SLOT, SUBSTRATE_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (isElement(stack)) {
                if(!moveItemStackTo(stack,ELEMENT_SLOT_START,ELEMENT_SLOT_END,false)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, MODIFIER_SLOT_START, MODIFIER_SLOT_END, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return result;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }

    public static boolean isPrimaryMicrocore(ItemStack stack) {
        return ElementType.byMicrocore(stack).filter(ElementType::isPrimary).isPresent();
    }
    public static boolean isTargetMicrocore(ItemStack stack){return isPrimaryMicrocore(stack)||SpellPattern.composite(stack);}

    public static boolean isElement(ItemStack stack) {return stack.is(com.mcmagic.omnira.registry.ModItemTags.SPELL_ELEMENTS);}
    public static boolean mayPlace(int slot,ItemStack stack) {
        return switch(slot) {
            case 0 -> isTargetMicrocore(stack);
            case 1 -> isPrimaryMicrocore(stack);
            case 2,3 -> true;
            case 4,5 -> isElement(stack);
            case 6 -> isInk(stack);
            case 7 -> stack.is(ModItems.SPIRITUAL_CRYSTAL.get());
            default -> false;
        };
    }

    public static boolean isInk(ItemStack stack) {
        return stack.is(ModItems.SPELL_INK.get()) || stack.is(Items.INK_SAC) || stack.is(Items.GLOW_INK_SAC);
    }

    public int baseManaCost() {
        int components=0;
        for(int i=TARGET_CORE_SLOT;i<ELEMENT_SLOT_END;i++) if(!container.getItem(i).isEmpty()) components++;
        return com.mcmagic.omnira.spell.SpellPayload.componentCost(components);
    }
    public double manaCost() {return com.mcmagic.omnira.mana.ManaCosts.cost(owner,baseManaCost());}

    public boolean canProcessCrystal() {
        for(int i=ELEMENT_SLOT_START;i<ELEMENT_SLOT_END;i++)
            if(!container.getItem(i).isEmpty() && !isElement(container.getItem(i))) return false;
        return com.mcmagic.omnira.mana.ManaCosts.canSpend(owner,baseManaCost())
                && SpellPattern.fromMicrocores(container.getItem(TARGET_CORE_SLOT),container.getItem(SHAPE_CORE_SLOT)).isPresent()
                && isInk(container.getItem(INK_SLOT))
                && container.getItem(SUBSTRATE_SLOT).is(ModItems.SPIRITUAL_CRYSTAL.get())
                && container.getItem(OUTPUT_SLOT).isEmpty();
    }

    private void addTableSlots(Container container) {
        addSlot(new FilteredSlot(container, TARGET_CORE_SLOT, 52, 32, s->isTargetMicrocore(s)&&SpellPattern.compatibleSlots(container,0,s)));
        addSlot(new FilteredSlot(container, SHAPE_CORE_SLOT, 108, 32, s->isPrimaryMicrocore(s)&&SpellPattern.compatibleSlots(container,1,s)));

        addSlot(new Slot(container, 2, 24, 68));
        addSlot(new Slot(container, 3, 52, 104));
        addSlot(new FilteredSlot(container, 4, 108, 104, CrystalProcessingTableMenu::isElement));
        addSlot(new FilteredSlot(container, 5, 136, 68, CrystalProcessingTableMenu::isElement));

        addSlot(new FilteredSlot(container, INK_SLOT, 66, 64, CrystalProcessingTableMenu::isInk));
        addSlot(new FilteredSlot(container, SUBSTRATE_SLOT, 94, 64, stack -> stack.is(ModItems.SPIRITUAL_CRYSTAL.get())));
        addSlot(new OutputSlot(container, OUTPUT_SLOT, 80, 88));
    }

    private void addPlayerInventorySlots(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 158 + row * 18));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 216));
        }
    }

    private static Container createClientContainer() {
        return new SimpleContainer(TABLE_SLOT_COUNT);
    }

    private static ContainerLevelAccess createAccess(Level level, BlockPos pos) {
        return ContainerLevelAccess.create(level, pos);
    }

    public static java.util.List<com.mcmagic.omnira.spell.SpellEffect> composeEffects(Container container) {
        return composeEffects(container,MODIFIER_SLOT_START,MODIFIER_SLOT_END,ELEMENT_SLOT_START,ELEMENT_SLOT_END);
    }
    public static java.util.List<com.mcmagic.omnira.spell.SpellEffect> composeEffects(Container container,int modifierStart,int modifierEnd,int elementStart,int elementEnd) {
        int clocks=0,iron=0,infusions=0;
        boolean infused=false;
        for(int i=modifierStart;i<modifierEnd;i++) {
            if(container.getItem(i).is(Items.CLOCK)) clocks++;
            if(container.getItem(i).is(Items.IRON_BLOCK)) iron++;
            if(container.getItem(i).is(ModItems.SPIRITUAL_CRYSTAL.get())) {infused=true;infusions++;}
        }
        var effects=new java.util.ArrayList<com.mcmagic.omnira.spell.SpellEffect>();
        var seen=new java.util.HashSet<net.minecraft.world.item.Item>();
        for(int i=elementStart;i<elementEnd;i++) {
            var stack=container.getItem(i);
            if(stack.isEmpty() || !seen.add(stack.getItem())) continue;
            if(stack.is(ModItems.DISSOCIATION_THREAD.get())) effects.add(com.mcmagic.omnira.spell.SpellEffect.utility("dissociation",iron,clocks,infusions));
            if(stack.is(ModItems.DARK_MICROCORE.get())) effects.add(com.mcmagic.omnira.spell.SpellEffect.utility("dark_breath",iron,clocks,infusions));
            if(stack.is(ModItems.CONSTRUCTION_MATRIX.get())) effects.add(com.mcmagic.omnira.spell.SpellEffect.utility("construction",iron,clocks,infusions));
            if(stack.is(ModItems.SHARP_BREATH.get()) || stack.is(ModItems.HEALING_DEW.get()))
                effects.add(new com.mcmagic.omnira.spell.SpellEffect(stack.is(ModItems.HEALING_DEW.get()),
                        (clocks>0?0:1)+iron,clocks>0?800+(clocks-1)*1200:0,infused));
        }
        return effects;
    }

    public static ItemStack previewCrystal(Container container) {
        return previewCrystal(container,MODIFIER_SLOT_START,MODIFIER_SLOT_END,ELEMENT_SLOT_START,ELEMENT_SLOT_END,ModItems.LOW_TIER_MAGIC_CRYSTAL.get());
    }
    public static ItemStack previewCrystal(Container container,int modifierStart,int modifierEnd,int elementStart,int elementEnd,net.minecraft.world.item.Item outputItem) {
        SpellPattern pattern = SpellPattern.fromMicrocores(
                container.getItem(TARGET_CORE_SLOT),
                container.getItem(SHAPE_CORE_SLOT)
        ).orElseThrow();

        int components=0;
        for(int i=0;i<Math.max(modifierEnd,elementEnd);i++) if(!container.getItem(i).isEmpty()) components++;
        int baseCost=com.mcmagic.omnira.spell.SpellPayload.componentCost(components);
        var effects=composeEffects(container,modifierStart,modifierEnd,elementStart,elementEnd);
        var keywords=new java.util.ArrayList<String>();
        var seenKeywords=new java.util.HashSet<net.minecraft.world.item.Item>();
        for(int i=elementStart;i<elementEnd;i++) {
            var stack=container.getItem(i);
            if(stack.isEmpty() || !seenKeywords.add(stack.getItem())) continue;
            if(stack.is(ModItems.SHARP_BREATH.get())) keywords.add("harm");
            if(stack.is(ModItems.DARK_MICROCORE.get())) keywords.add("dark_breath");
            if(stack.is(ModItems.DISSOCIATION_THREAD.get())) keywords.add("dissociation");
            if(stack.is(ModItems.CONSTRUCTION_MATRIX.get())) keywords.add("construction");
            if(stack.is(ModItems.HEALING_DEW.get())) keywords.add("healing");
        }
        for(int i=modifierStart;i<modifierEnd;i++) {
            var stack=container.getItem(i);
            if(stack.is(Items.CLOCK)) keywords.add("delay");
            if(stack.is(ModItems.LIGHT_MICROCORE.get())) keywords.add("holy");
            if(stack.is(Items.IRON_BLOCK)) keywords.add("enhancement");
            if(stack.is(ModItems.SPIRITUAL_CRYSTAL.get())) keywords.add("infusion");
        }
        ItemStack output = outputItem.getDefaultInstance();
        output.set(ModDataComponents.SPELL_PATTERN.get(), pattern);
        output.set(ModDataComponents.SPELL_PAYLOAD,new com.mcmagic.omnira.spell.SpellPayload(baseCost,0,effects,keywords));
        return output;
    }

    private void processCrystal() {
        ItemStack output=previewCrystal(container);
        for(int i=MODIFIER_SLOT_START;i<ELEMENT_SLOT_END;i++) container.removeItem(i,1);
        container.removeItem(TARGET_CORE_SLOT, 1);
        container.removeItem(SHAPE_CORE_SLOT, 1);
        consumeInk();
        container.removeItem(SUBSTRATE_SLOT, 1);

        container.setItem(OUTPUT_SLOT, output);
        container.setChanged();
        broadcastChanges();
    }

    private void consumeInk() {
        ItemStack ink = container.getItem(INK_SLOT);
        if (ink.is(ModItems.SPELL_INK.get()) && ink.isDamageableItem()) {
            int nextDamage = ink.getDamageValue() + 1;
            if (nextDamage >= ink.getMaxDamage()) {
                ink.shrink(1);
            } else {
                ink.setDamageValue(nextDamage);
            }
        } else {
            container.removeItem(INK_SLOT, 1);
        }
    }
}
