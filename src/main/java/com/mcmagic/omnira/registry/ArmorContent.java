package com.mcmagic.omnira.registry;

import com.mcmagic.omnira.Omnira;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;
import java.util.*;

/** Matching base armor stats, reversible shaping and distinct full-set effects. */
public final class ArmorContent {
    private static final DeferredRegister<ArmorMaterial> MATERIALS=DeferredRegister.create(Registries.ARMOR_MATERIAL,Omnira.MOD_ID);
    public static final DeferredHolder<ArmorMaterial,ArmorMaterial> CRYSTAL=material("crystal_armor");
    public static final DeferredHolder<ArmorMaterial,ArmorMaterial> DRESS=material("crystal_dress");
    public static final Map<ArmorItem.Type,DeferredItem<ArmorItem>> PLATE=equipment(false);
    public static final Map<ArmorItem.Type,DeferredItem<ArmorItem>> CLOTH=equipment(true);
    private ArmorContent() {}
    private static DeferredHolder<ArmorMaterial,ArmorMaterial> material(String id){
        return MATERIALS.register(id,()->new ArmorMaterial(Map.of(ArmorItem.Type.HELMET,3,ArmorItem.Type.CHESTPLATE,8,
                ArmorItem.Type.LEGGINGS,6,ArmorItem.Type.BOOTS,3),10,SoundEvents.ARMOR_EQUIP_DIAMOND,
                ()->Ingredient.of(ModItems.SPIRITUAL_CRYSTAL.get()),
                List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Omnira.MOD_ID,id))),2,0));
    }
    private static Map<ArmorItem.Type,DeferredItem<ArmorItem>> equipment(boolean dress){
        var result=new EnumMap<ArmorItem.Type,DeferredItem<ArmorItem>>(ArmorItem.Type.class);
        var types=new ArmorItem.Type[]{ArmorItem.Type.HELMET,ArmorItem.Type.CHESTPLATE,ArmorItem.Type.LEGGINGS,ArmorItem.Type.BOOTS};
        String[] names=dress?new String[]{"crystal_sunhat","crystal_blouse","crystal_skirt","crystal_stocking_shoes"}
                :new String[]{"crystal_helmet","crystal_chestplate","crystal_leggings","crystal_boots"};
        for(int i=0;i<types.length;i++){
            var type=types[i];
            result.put(type,ModItems.ITEMS.register(names[i],()->new com.mcmagic.omnira.item.CrystalArmorItem(dress?DRESS:CRYSTAL,type,
                    new Item.Properties().durability(type.getDurability(33)))));
        }
        return Collections.unmodifiableMap(result);
    }
    public static boolean isOurs(Item item){return PLATE.values().stream().anyMatch(v->v.get()==item)||CLOTH.values().stream().anyMatch(v->v.get()==item);}
    public static ItemStack exchange(ItemStack source,ItemStack target){
        if(!isOurs(source.getItem()) || !isOurs(target.getItem())
                || ((ArmorItem)source.getItem()).getType()!=((ArmorItem)target.getItem()).getType())return ItemStack.EMPTY;
        var result=target.copyWithCount(1);
        result.applyComponents(source.getComponentsPatch());
        return result;
    }
    public static void register(IEventBus bus){MATERIALS.register(bus);}
}
