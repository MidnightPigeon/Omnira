package com.mcmagic.omnira.item;

import com.mcmagic.omnira.registry.ModItems;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.registries.*;
import java.util.List;

/** Assembly transfers explicit source components, retaining the new stage's default attributes. */
public final class RitualSwordUpgrades {
    public static final List<String> NAIL_IDS=List.of("sharpened_crystallized_bone_nail","channelled_crystallized_bone_nail","threaded_paradox_bone_nail","pure_paradox_bone_nail");
    public static final List<String> NEEDLE_IDS=List.of("sharpened_arcane_needle","shining_arcane_needle","tempered_paradox_needle","pale_paradox_needle");
    public static List<DeferredItem<Item>> register(DeferredRegister.Items registry,RitualSwordItem.Kind kind){
        var ids=kind==RitualSwordItem.Kind.NAIL?NAIL_IDS:NEEDLE_IDS;
        return java.util.stream.IntStream.range(0,4).mapToObj(i->registry.<Item>register(ids.get(i),
                ()->new RitualSwordItem(new Item.Properties().rarity(switch(i){case 1->Rarity.UNCOMMON;case 2->Rarity.RARE;case 3->Rarity.EPIC;default->Rarity.COMMON;}),kind,i+1))).toList();
    }
    public static Item item(RitualSwordItem.Kind kind,int level){
        if(level==0)return kind==RitualSwordItem.Kind.NAIL?ModItems.CRYSTALLIZED_NAIL.get():ModItems.ARCANE_NEEDLE.get();
        return (kind==RitualSwordItem.Kind.NAIL?ModItems.NAIL_UPGRADES:ModItems.NEEDLE_UPGRADES).get(level-1).get();
    }
    public static boolean valid(ItemStack source,ItemStack target){
        return source.getItem() instanceof RitualSwordItem old && target.getItem() instanceof RitualSwordItem next
                && old.kind==next.kind && old.upgradeLevel<4 && next.upgradeLevel==old.upgradeLevel+1;
    }
    public static ItemStack assemble(ItemStack source,ItemStack target){
        if(!valid(source,target))return ItemStack.EMPTY;
        var result=target.copyWithCount(1);
        result.applyComponents(source.getComponentsPatch());
        return result;
    }
    private RitualSwordUpgrades(){}
}
