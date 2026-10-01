package com.mcmagic.omnira.item.staff;

import com.mcmagic.omnira.item.StaffItem;
import com.mcmagic.omnira.registry.ModDataComponents;
import com.mcmagic.omnira.registry.ModItems;
import com.mcmagic.omnira.spell.SpellEffect;
import com.mcmagic.omnira.spell.SpellPayload;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.ArrayList;
import java.util.List;

/** Permanent staff slots modify a cast copy, never the stored crystal. */
public final class StaffEnhancements {
    private StaffEnhancements() {}

    public static String keyword(ItemStack stack) {
        if(stack.is(Items.IRON_BLOCK))return "enhancement";
        if(stack.is(Items.CLOCK))return "delay";
        if(stack.is(ModItems.LIGHT_MICROCORE.get()))return "holy";
        if(stack.is(ModItems.SPIRITUAL_CRYSTAL.get()))return "infusion";
        return "";
    }

    public static ItemStack assemble(Container input) {
        var staff=input.getItem(7);
        if(!(staff.getItem() instanceof StaffItem))return ItemStack.EMPTY;
        var source=input.getItem(0);
        if(keyword(source).isEmpty())return ItemStack.EMPTY;
        for(int i=0;i<9;i++)if(i!=7 && !ItemStack.isSameItemSameComponents(source,input.getItem(i)))return ItemStack.EMPTY;
        var material=source.copyWithCount(1);
        material.set(ModDataComponents.STAFF_PART,new StaffPart(StaffPart.Role.UPGRADE,0,0,0,0,
                StaffPart.EMPTY_MODEL,64,0,0,StaffPart.SlotKind.ENHANCEMENT));
        var upgraded=StaffAssembly.of(staff).withUpgrades(List.of(material));
        if(upgraded.isEmpty())return ItemStack.EMPTY;
        var result=staff.copyWithCount(1);
        result.set(ModDataComponents.STAFF_ASSEMBLY,upgraded.get());
        return result;
    }

    public static List<String> keywords(ItemStack staff) {
        return StaffAssembly.of(staff).upgrades().stream()
                .filter(s->StaffPart.is(s,StaffPart.Role.UPGRADE)
                        && StaffPart.of(s).upgradeSlot()==StaffPart.SlotKind.ENHANCEMENT)
                .map(StaffEnhancements::keyword).filter(s->!s.isEmpty()).toList();
    }

    public static SpellPayload apply(SpellPayload payload,List<String> additions) {
        if(additions.isEmpty())return payload;
        int iron=(int)additions.stream().filter("enhancement"::equals).count();
        int clocks=(int)additions.stream().filter("delay"::equals).count();
        int infusions=(int)additions.stream().filter("infusion"::equals).count();
        var effects=new ArrayList<SpellEffect>();
        for(var effect:payload.effects()) {
            boolean potion=!effect.utility() && !effect.darkness();
            int duration=effect.duration(),amplifier=effect.amplifier();
            if(potion) {
                if(clocks>0) {
                    amplifier=Math.max(0,amplifier-(duration==0?1:0));
                    duration=(int)Math.min(Integer.MAX_VALUE,(long)duration+(duration==0?800L+(clocks-1)*1200L:clocks*1200L));
                }
                amplifier=Math.min(255,amplifier+iron);
            }
            effects.add(new SpellEffect(effect.healing(),amplifier,duration,effect.infused()||infusions>0,
                    effect.operation(),Math.min(16,effect.enhancement()+iron),Math.min(8,effect.delay()+clocks),
                    Math.min(16,effect.infusion()+infusions)));
        }
        var keywords=new ArrayList<>(payload.keywords());
        keywords.addAll(additions);
        return new SpellPayload(payload.baseCost(),payload.damage()>0?payload.damage()+3*iron:payload.damage(),effects,keywords);
    }
}
