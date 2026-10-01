package com.mcmagic.omnira.item;

import com.mcmagic.omnira.registry.ModAttachments;
import com.mcmagic.omnira.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import java.util.List;

public final class CrystalPickaxeItem extends PickaxeItem {
    public enum CrystalTier implements Tier {
        BASIC(Tiers.IRON,64), INFUSED(Tiers.NETHERITE,1024);
        private final Tier base;private final int uses;
        CrystalTier(Tier base,int uses) {this.base=base;this.uses=uses;}
        public int getUses() {return uses;}
        public float getSpeed() {return base.getSpeed();}
        public float getAttackDamageBonus() {return base.getAttackDamageBonus();}
        public TagKey<Block> getIncorrectBlocksForDrops() {return base.getIncorrectBlocksForDrops();}
        public int getEnchantmentValue() {return base.getEnchantmentValue();}
        public Ingredient getRepairIngredient() {return Ingredient.of(ModItems.SPIRITUAL_CRYSTAL.get());}
    }
    private final boolean infused;
    public CrystalPickaxeItem(Properties properties,boolean infused) {
        super(infused?CrystalTier.INFUSED:CrystalTier.BASIC,properties.attributes(
                DiggerItem.createAttributes(infused?CrystalTier.INFUSED:CrystalTier.BASIC,1,-2.8F)));
        this.infused=infused;
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> tooltip,TooltipFlag flag) {
        super.appendHoverText(stack,context,tooltip,flag);
        var description=Component.translatable(infused?"tooltip.omnira.infused_crystal_pickaxe":"tooltip.omnira.crystal_pickaxe");
        if(!infused) description.withStyle(net.minecraft.ChatFormatting.ITALIC);
        tooltip.add(description);
    }
    public static boolean repairHeld(ItemStack stack,ServerPlayer player) {
        return stack.is(ModItems.INFUSED_CRYSTAL_PICKAXE.get()) && HeldManaRepair.repair(stack,player);
    }
    @Override public void inventoryTick(ItemStack stack,Level level,Entity entity,int slot,boolean selected) {
        super.inventoryTick(stack,level,entity,slot,selected);
        if(infused && entity instanceof ServerPlayer player && level.getGameTime()%10==0) repairHeld(stack,player);
    }
}
