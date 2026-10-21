package com.mcmagic.omnira.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import java.util.*;
import static com.mcmagic.omnira.item.OrbUpgradeItem.Kind.*;

/** Type compatibility only; individual slots still enforce placement, duplication and capacity. */
public final class UpgradeCompatibility {
    public enum Host {
        CRYSTAL_BALL("crystal_ball",storageKinds()),
        LIQUID_CRYSTAL_BALL("liquid_crystal_ball",storageKinds()),
        RESONANT_CRYSTAL_BALL("resonant_crystal_ball",storageKinds()),
        RESONANT_LIQUID_CRYSTAL_BALL("resonant_liquid_crystal_ball",storageKinds()),
        VOID_ORB("void_crystal_ball",INTAKE,SPEED),
        STAR_ORB("wandering_star_cluster_orb",STABILIZATION,RECIPE_CONTROL,SPEED),
        CRUISE_ORB("cruise_orb",STACKING,INFUSED_STACKING,STABILIZATION,LAVA,SPEED),
        REACTION_POOL("large_alchemical_reaction_pool",INTAKE,OUTPUT,STACKING,INFUSED_STACKING,RECIPE_CONTROL,SPEED),
        CATALYSIS_CHAMBER("alchemical_catalysis_chamber",INTAKE,OUTPUT,RECIPE_CONTROL,SPEED),
        PULSE_ORB("pulse_accumulator_orb",STACKING,INFUSED_STACKING),
        RESONANT_PULSE_ORB("resonant_accumulator_orb",STACKING,INFUSED_STACKING,STABILIZATION);

        public final String id;
        private final Set<OrbUpgradeItem.Kind> kinds;
        Host(String id,OrbUpgradeItem.Kind... kinds){this.id=id;var accepted=EnumSet.noneOf(OrbUpgradeItem.Kind.class);Collections.addAll(accepted,kinds);this.kinds=Collections.unmodifiableSet(accepted);}
        public boolean accepts(OrbUpgradeItem.Kind kind){return kinds.contains(kind);}
        public String translationKey(){return (this==CRUISE_ORB?"item.omnira.":"block.omnira.")+id;}
    }
    private static OrbUpgradeItem.Kind[] storageKinds(){return Arrays.stream(OrbUpgradeItem.Kind.values()).filter(kind->kind!=RECIPE_CONTROL).toArray(OrbUpgradeItem.Kind[]::new);}
    public static List<Host> hosts(OrbUpgradeItem.Kind kind){return Arrays.stream(Host.values()).filter(host->host.accepts(kind)).toList();}
    public static void appendTooltip(OrbUpgradeItem.Kind kind,boolean expanded,List<Component> lines){
        if(!expanded){lines.add(Component.translatable("tooltip.omnira.upgrade.hold_shift").withStyle(ChatFormatting.DARK_GRAY));return;}
        lines.add(Component.translatable("tooltip.omnira.upgrade.compatible").withStyle(ChatFormatting.AQUA));
        for(var host:hosts(kind))lines.add(Component.literal("  ").append(Component.translatable(host.translationKey())).withStyle(ChatFormatting.GREEN));
    }
    private UpgradeCompatibility(){}
}
