package com.mcmagic.omnira.client.guide;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;

/** Rebuild the localized item-name index after a client language reload. */
final class GuideItemCatalog {
    private static Language generation;
    private static GuideItemNames cached;

    public static GuideItemNames current(){
        if(generation!=Language.getInstance()){
            var mc=Minecraft.getInstance();
            String locale=mc.getLanguageManager().getSelected();
            String fallback=locale.startsWith("en")?"en_us":"zh_cn";
            var language=locale.equals(fallback)?Language.getInstance():
                    ClientLanguage.loadFrom(mc.getResourceManager(),List.of("en_us",fallback),false);
            var path=ResourceLocation.fromNamespaceAndPath("omnira","guide/inline_items.json");
            com.google.gson.JsonObject rules;
            try(var reader=mc.getResourceManager().getResourceOrThrow(path).openAsReader()){
                rules=com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
            }catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}
            var named=new HashSet<ResourceLocation>();
            rules.getAsJsonArray("named").forEach(id->named.add(ResourceLocation.parse(id.getAsString())));
            var names=new LinkedHashMap<String,Item>();
            for(var item:BuiltInRegistries.ITEM){
                var id=BuiltInRegistries.ITEM.getKey(item);
                if(item==Items.AIR || item instanceof com.mcmagic.omnira.item.FateCurioItem
                        || id.toString().equals("omnira:modular_staff") || id.toString().equals("omnira:guide_book")
                        || !(id.getNamespace().equals("omnira") || named.contains(id)))continue;
                String name=language.getOrDefault(item.getDescriptionId(),"").strip();
                if(name.isEmpty() || name.contains("%"))continue;
                add(names,name,item);
                if(fallback.equals("en_us") && name.matches(".*[a-zA-Z]") && !name.endsWith("s")){
                    String plural=name.matches(".*[^aeiou]y")?name.substring(0,name.length()-1)+"ies":
                            name.matches(".*(?:ch|sh|x|z)")?name+"es":name+"s";
                    add(names,plural,item);
                }
            }
            // Short forms used in prose differ from the inventory names.
            alias(names,"primordial_spell_core","原始核心","Primordial Core","Primordial Cores");
            alias(names,"dream_spell_core","梦境核心","Dream Core","Dream Cores");
            alias(names,"spacetime_spell_core","时空核心","Spacetime Core","Spacetime Cores");
            alias(names,"light_dark_spell_core","光暗核心","Light/Dark Core","Light/Dark Cores");
            alias(names,"unbound_mana_core","无羁核心","Unbound Core","Unbound Cores");
            alias(names,"infused_spiritual_crystal","Infused Crystal","Infused Crystals");
            alias(names,"arcane_assembly_table","Assembly Table","普通装配台","装配台");
            alias(names,"dream_mirror_spawn_egg","梦之镜","Dream Mirror","Dream Mirrors");
            cached=new GuideItemNames(names);
            for(var rule:rules.getAsJsonObject("contexts").entrySet()){
                BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(rule.getKey())).ifPresent(item->
                        rule.getValue().getAsJsonArray().forEach(context->cached.addContext(context.getAsString(),item)));
            }
            generation=Language.getInstance();
        }
        return cached;
    }

    private static void add(Map<String,Item> names,String name,Item item){
        names.merge(name.toLowerCase(Locale.ROOT),item,(a,b)->a==b?a:Items.AIR);
    }
    private static void alias(Map<String,Item> names,String path,String... aliases){
        BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath("omnira",path))
                .ifPresent(item->{for(String alias:aliases)names.putIfAbsent(alias.toLowerCase(Locale.ROOT),item);});
    }
}
