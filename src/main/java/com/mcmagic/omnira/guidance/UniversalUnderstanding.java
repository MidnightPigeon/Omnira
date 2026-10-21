package com.mcmagic.omnira.guidance;

import com.mcmagic.omnira.item.*;
import com.mcmagic.omnira.item.staff.StaffAssembly;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.spell.SpellPayload;
import java.util.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;

/** Inverse of the stored spell/staff composition, not a crafting-history ledger. */
public final class UniversalUnderstanding {
    private UniversalUnderstanding() {}
    public static List<ItemStack> materials(ItemStack stack) {
        if (stack.getItem() instanceof StaffItem) {
            var assembly=StaffAssembly.of(stack);
            return assembly.valid()?assembly.components():List.of();
        }
        if (!(stack.getItem() instanceof LowTierMagicCrystalItem)) return List.of();
        var result=new ArrayList<ItemStack>();
        boolean advanced=stack.is(com.mcmagic.omnira.aggregation.AggregationContent.CRYSTAL.get())||stack.getItem() instanceof WorldRotationCrystalItem;
        result.add(new ItemStack(advanced?ModItems.INFUSED_SPIRITUAL_CRYSTAL.get():ModItems.SPIRITUAL_CRYSTAL.get()));
        var pattern=stack.get(ModDataComponents.SPELL_PATTERN);
        if (pattern==null) return result;
        result.add(new ItemStack(pattern.targetElement().microcoreItem().get()));
        if (pattern.shapeElement()!=null) result.add(new ItemStack(pattern.shapeElement().microcoreItem().get()));
        if (pattern.worldRotation()) return result;
        // Element keywords are unique, while repeated modifier keywords encode their multiplicity.
        var seen=new HashSet<String>();
        for (var keyword:keywords(SpellPayload.of(stack))) {
            Item item=switch(keyword) {
                case "harm" -> ModItems.SHARP_BREATH.get();
                case "healing" -> ModItems.HEALING_DEW.get();
                case "dark_breath" -> ModItems.DARK_MICROCORE.get();
                case "dissociation" -> ModItems.DISSOCIATION_THREAD.get();
                case "construction" -> ModItems.CONSTRUCTION_MATRIX.get();
                case "reload" -> com.mcmagic.omnira.myriad.WorldSeedContent.ACTIVE.get();
                case "delay" -> Items.CLOCK;
                case "holy" -> ModItems.LIGHT_MICROCORE.get();
                case "enhancement" -> Items.IRON_BLOCK;
                case "infusion" -> ModItems.SPIRITUAL_CRYSTAL.get();
                default -> null;
            };
            boolean modifier=Set.of("delay","holy","enhancement","infusion").contains(keyword);
            if (item!=null && (modifier||seen.add(keyword))) result.add(new ItemStack(item));
        }
        return List.copyOf(result);
    }
    private static List<String> keywords(SpellPayload payload) {
        var result=new ArrayList<>(payload.keywords());
        int enhancement=0,delay=0,infusion=0;
        for(var effect:payload.effects()) {
            String element=effect.utility()||effect.darkness()?effect.operation():effect.healing()?"healing":"harm";
            if(!result.contains(element))result.add(element);
            boolean potion=!effect.utility()&&!effect.darkness();
            enhancement=Math.max(enhancement,potion?Math.max(0,effect.amplifier()-(effect.duration()>0?0:1)):effect.enhancement());
            delay=Math.max(delay,potion&&effect.duration()>0?1+Math.max(0,(effect.duration()-800)/1200):effect.delay());
            infusion=Math.max(infusion,Math.max(effect.infusion(),effect.infused()?1:0));
        }
        // Older crystals may predate keywords; shared modifiers are inferred once, not once per effect.
        if(!result.contains("enhancement"))for(int i=0;i<enhancement;i++)result.add("enhancement");
        if(!result.contains("delay"))for(int i=0;i<delay;i++)result.add("delay");
        if(!result.contains("infusion"))for(int i=0;i<infusion;i++)result.add("infusion");
        return result;
    }
    public static void use(Player player) {
        if (player.level().isClientSide || player.isSpectator() || !player.isAlive()) return;
        var mana=player.getData(ModAttachments.MANA);
        double maximum=player.getAttributeValue(ModAttributes.MAX_MANA);
        player.setData(ModAttachments.MANA,new com.mcmagic.omnira.mana.ManaState(Math.max(mana.current(),maximum),maximum,mana.affinity()));
        var held=player.getMainHandItem();
        var result=materials(held);
        if (!result.isEmpty()) {
            held.shrink(1);
            for(var material:result) {
                var output=material.copy();player.addItem(output);
                if(!output.isEmpty())player.drop(output,false);
            }
        }
    }
}
