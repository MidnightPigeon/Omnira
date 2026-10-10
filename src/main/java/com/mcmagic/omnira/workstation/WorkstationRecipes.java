package com.mcmagic.omnira.workstation;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.core.Holder;
import net.minecraft.tags.EnchantmentTags;
import java.util.*;

/** Delegate vanilla repair and enchantment compatibility rules to the vanilla menus. */
public final class WorkstationRecipes {
    public record Result(ItemStack stack,int levels,int materials,int mana,GrindstoneMenu grinder){}
    public static Result calculate(CrystalWorkstationEntity work,Player player){
        if(work.kind()==CrystalWorkstations.Kind.ANVIL){
            var menu=new AnvilMenu(0,player.getInventory());
            menu.getSlot(0).set(work.getItem(0).copy());menu.getSlot(1).set(work.getItem(1).copy());
            if(work.rename!=null)menu.setItemName(work.rename);
            return new Result(menu.getSlot(2).getItem().copy(),menu.getCost(),menu.repairItemCountCost,0,null);
        }
        var menu=new GrindstoneMenu(0,player.getInventory(),ContainerLevelAccess.create(work.getLevel(),work.getBlockPos()));
        menu.getSlot(0).set(work.getItem(0).copy());menu.getSlot(1).set(work.getItem(1).copy());
        var result=menu.getSlot(2).getItem().copy();int mana=0;
        if(work.infused()){
            var source=work.getItem(0).isEmpty()?work.getItem(1):work.getItem(0);
            if(result.isEmpty()&&(work.getItem(0).isEmpty()||work.getItem(1).isEmpty()))result=source.copy();
            if(!result.isEmpty()){
                var keep=new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                for(var e:enchantments(work).entrySet()){
                    if(work.removedEnchantments.contains(e.getKey().getRegisteredName()))mana+=e.getKey().is(EnchantmentTags.CURSE)?500:100;
                    else keep.set(e.getKey(),e.getValue());
                }
                if(source.is(Items.ENCHANTED_BOOK)&&!keep.toImmutable().isEmpty())result=source.copy();
                EnchantmentHelper.setEnchantments(result,keep.toImmutable());
                if(result.is(Items.ENCHANTED_BOOK)&&keep.toImmutable().isEmpty()){
                    var book=new ItemStack(Items.BOOK,result.getCount());book.applyComponents(result.getComponentsPatch());
                    book.remove(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS);result=book;
                }
                if(mana==0&&(work.getItem(0).isEmpty()||work.getItem(1).isEmpty()))result=ItemStack.EMPTY;
            }
        }
        return new Result(result,0,0,mana,menu);
    }
    public static Map<Holder<Enchantment>,Integer> enchantments(CrystalWorkstationEntity work){
        var result=new TreeMap<Holder<Enchantment>,Integer>(Comparator.comparing(Holder::getRegisteredName));
        for(int i=0;i<2;i++)for(var e:EnchantmentHelper.getEnchantmentsForCrafting(work.getItem(i)).entrySet())
            result.merge(e.getKey(),e.getIntValue(),Math::max);
        return result;
    }
    private WorkstationRecipes(){}
}
