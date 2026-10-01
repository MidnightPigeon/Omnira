package com.mcmagic.omnira.registry;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mcmagic.omnira.Omnira;
import com.mcmagic.omnira.item.staff.StaffAssembly;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

/** Explicit functional groups keep progression and material families together. */
public final class ModCreativeModeTabs {
    public enum Category { SPELLS, EQUIPMENT, RESEARCH, MISC, BUILDING, NATURE }
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS=DeferredRegister.create(Registries.CREATIVE_MODE_TAB,Omnira.MOD_ID);
    // Existing registry IDs are retained; only titles, order and contents change.
    public static final DeferredHolder<CreativeModeTab,CreativeModeTab> MAGIC_MOD_TAB=tab("omnira",Category.SPELLS,CreativeModeTabs.COMBAT,()->StaffAssembly.basic().create());
    public static final DeferredHolder<CreativeModeTab,CreativeModeTab> EQUIPMENT_TAB=tab("equipment",Category.EQUIPMENT,MAGIC_MOD_TAB.getKey(),()->ModItems.CRYSTAL_PICKAXE.get().getDefaultInstance());
    public static final DeferredHolder<CreativeModeTab,CreativeModeTab> RESEARCH_TAB=tab("research",Category.RESEARCH,EQUIPMENT_TAB.getKey(),()->ModItems.ANALYSIS_ARTISAN_TABLE.get().getDefaultInstance());
    public static final DeferredHolder<CreativeModeTab,CreativeModeTab> MISC_TAB=tab("misc",Category.MISC,RESEARCH_TAB.getKey(),()->ModItems.RESONANCE_CRYSTAL.get().getDefaultInstance());
    public static final DeferredHolder<CreativeModeTab,CreativeModeTab> BUILDING_TAB=tab("building",Category.BUILDING,MISC_TAB.getKey(),()->DreamContent.CRYSTAL_COLUMN.get().asItem().getDefaultInstance());
    public static final DeferredHolder<CreativeModeTab,CreativeModeTab> NATURE_TAB=tab("nature",Category.NATURE,BUILDING_TAB.getKey(),()->DreamContent.SHADOW_LEAVES.get().asItem().getDefaultInstance());
    private static DeferredHolder<CreativeModeTab,CreativeModeTab> tab(String id,Category category,ResourceKey<CreativeModeTab> before,java.util.function.Supplier<ItemStack> icon){
        return CREATIVE_MODE_TABS.register(id,()->CreativeModeTab.builder().title(Component.translatable("itemGroup.omnira."+category.name().toLowerCase(Locale.ROOT)))
                .withTabsBefore(before).icon(icon).displayItems((parameters,output)->contents(category).forEach(output::accept)).build());
    }
    private static final JsonObject CATALOG=readCatalog();
    private static JsonObject readCatalog(){
        try(var in=new InputStreamReader(Objects.requireNonNull(ModCreativeModeTabs.class.getResourceAsStream("/omnira-creative-catalog.json")),StandardCharsets.UTF_8)){
            return new Gson().fromJson(in,JsonObject.class);
        }catch(java.io.IOException ex){throw new IllegalStateException("Cannot read creative catalog",ex);}
    }
    public static boolean hidden(Item item){
        return item==ModItems.TEST_DAMAGE_ELEMENT.get()||item==ModItems.PERMANENT_VOID_CRYSTAL.get()
                ||item==DreamContent.CRYSTAL_COLUMN_CAPITAL.get().asItem()
                ||item==ModItems.DREAM_PORTAL.get()||item==ModItems.CORRIDOR_GATEWAY.get();
    }
    public static List<String> declaredIds(Category category){
        var ids=new ArrayList<String>();
        for(var group:CATALOG.getAsJsonArray(category.name().toLowerCase(Locale.ROOT)))
            for(var id:group.getAsJsonObject().getAsJsonArray("items"))ids.add(id.getAsString());
        return List.copyOf(ids);
    }
    public static List<Item> uncategorized(){
        var declared=new HashSet<String>();for(var category:Category.values())declared.addAll(declaredIds(category));
        return ModItems.ITEMS.getEntries().stream().<Item>map(h->h.get()).filter(i->!hidden(i)&&!declared.contains(BuiltInRegistries.ITEM.getKey(i).getPath()))
                .sorted(Comparator.comparing(i->BuiltInRegistries.ITEM.getKey(i).toString())).toList();
    }
    public static List<ItemStack> contents(Category category){
        var result=new ArrayList<ItemStack>();
        for(String id:declaredIds(category)){
            var key=ResourceLocation.fromNamespaceAndPath(Omnira.MOD_ID,id);
            if(!BuiltInRegistries.ITEM.containsKey(key))continue;
            var item=BuiltInRegistries.ITEM.get(key);if(hidden(item))continue;
            result.add(item==ModItems.MODULAR_STAFF.get()?StaffAssembly.basic().create():new ItemStack(item));
            if(item==ModItems.MEMORY_CUBE.get())result.add(com.mcmagic.omnira.item.MemoryCubeBlockItem.withState(new ItemStack(item),true));
        }
        // Keep new content accessible; tests require explicit assignment before release.
        if(category==Category.MISC)uncategorized().forEach(i->result.add(new ItemStack(i)));
        return List.copyOf(result);
    }
    private ModCreativeModeTabs(){}
    public static void register(IEventBus bus){CREATIVE_MODE_TABS.register(bus);}
}
