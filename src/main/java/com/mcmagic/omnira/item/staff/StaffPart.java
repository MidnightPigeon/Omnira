package com.mcmagic.omnira.item.staff;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import com.mcmagic.omnira.registry.ModDataComponents;

/** Per-stack definitions also allow future components supplied by other items or data packs. */
public record StaffPart(Role role, double power, double reduction, int shots, int cooldownTicks,
                        ResourceLocation model, int maxCopies, int enhancementSlots, int runeSlots, SlotKind upgradeSlot,
                        double speed, double cooldownReduction, double range) {
    public StaffPart(Role role, double power, double reduction, int shots, int cooldownTicks,
                     ResourceLocation model, int maxCopies, int enhancementSlots, int runeSlots, SlotKind upgradeSlot,
                     double speed, double cooldownReduction) {
        this(role,power,reduction,shots,cooldownTicks,model,maxCopies,enhancementSlots,runeSlots,upgradeSlot,speed,cooldownReduction,0);
    }
    public StaffPart(Role role, double power, double reduction, int shots, int cooldownTicks,
                     ResourceLocation model, int maxCopies, int enhancementSlots, int runeSlots, SlotKind upgradeSlot) {
        this(role,power,reduction,shots,cooldownTicks,model,maxCopies,enhancementSlots,runeSlots,upgradeSlot,0,0);
    }
    public StaffPart(Role role,double power,double reduction,int shots,int cooldownTicks,ResourceLocation model,int maxCopies) {
        this(role,power,reduction,shots,cooldownTicks,model,maxCopies,role==Role.SHAFT?1:0,role==Role.TIP?1:0,SlotKind.ENHANCEMENT);
    }
    public StaffPart {
        // Older per-stack definitions predate capacities; preserve their default owner slot.
        if(enhancementSlots==-1) enhancementSlots=role==Role.SHAFT?1:0;
        if(runeSlots==-1) runeSlots=role==Role.TIP?1:0;
        if(enhancementSlots<0 || enhancementSlots>64 || runeSlots<0 || runeSlots>64)
            throw new IllegalArgumentException("Staff slot capacity must be between 0 and 64");
    }
    public enum SlotKind implements StringRepresentable {
        ENHANCEMENT, RUNE;
        public static final Codec<SlotKind> CODEC=StringRepresentable.fromEnum(SlotKind::values);
        @Override public String getSerializedName() {return name().toLowerCase(java.util.Locale.ROOT);}
    }
    public static final ResourceLocation EMPTY_MODEL=ResourceLocation.fromNamespaceAndPath("omnira","item/staff_parts/none");
    public enum Role implements StringRepresentable {
        SHAFT, REINFORCEMENT, TIP, UPGRADE;
        public static final Codec<Role> CODEC=StringRepresentable.fromEnum(Role::values);
        @Override public String getSerializedName() {return name().toLowerCase(java.util.Locale.ROOT);}
        public int assemblySlot() {return switch(this) {case SHAFT->3;case REINFORCEMENT->6;case TIP->0;case UPGRADE->-1;};}
    }
    public static final Codec<StaffPart> CODEC=RecordCodecBuilder.create(i->i.group(
            Role.CODEC.fieldOf("role").forGetter(StaffPart::role),
            Codec.doubleRange(-10,10).optionalFieldOf("power",0D).forGetter(StaffPart::power),
            Codec.doubleRange(-100000,100000).optionalFieldOf("reduction",0D).forGetter(StaffPart::reduction),
            Codec.intRange(-64,64).optionalFieldOf("shots",0).forGetter(StaffPart::shots),
            Codec.intRange(-72000,72000).optionalFieldOf("cooldown_ticks",0).forGetter(StaffPart::cooldownTicks),
            ResourceLocation.CODEC.optionalFieldOf("model",EMPTY_MODEL).forGetter(StaffPart::model),
            Codec.intRange(1,64).optionalFieldOf("max_copies",1).forGetter(StaffPart::maxCopies),
            Codec.intRange(-1,64).optionalFieldOf("enhancement_slots",-1).forGetter(StaffPart::enhancementSlots),
            Codec.intRange(-1,64).optionalFieldOf("rune_slots",-1).forGetter(StaffPart::runeSlots),
            SlotKind.CODEC.optionalFieldOf("upgrade_slot",SlotKind.ENHANCEMENT).forGetter(StaffPart::upgradeSlot),
            Codec.doubleRange(0,10).optionalFieldOf("speed",0D).forGetter(StaffPart::speed),
            Codec.doubleRange(0,1).optionalFieldOf("cooldown_reduction",0D).forGetter(StaffPart::cooldownReduction),
            Codec.doubleRange(0,2).optionalFieldOf("range",0D).forGetter(StaffPart::range)
    ).apply(i,StaffPart::new));

    public static StaffPart of(ItemStack stack) {return stack.get(ModDataComponents.STAFF_PART);}
    public StaffPart withSlots(int enhancements,int runes) {
        return new StaffPart(role,power,reduction,shots,cooldownTicks,model,maxCopies,enhancements,runes,upgradeSlot,speed,cooldownReduction,range);
    }
    public static boolean is(ItemStack stack, Role role) {
        var part=of(stack);return !stack.isEmpty() && part!=null && part.role==role;
    }
    public static StaffPart component(String id,Role role,double power,double reduction,int shots,int cooldown) {
        return new StaffPart(role,power,reduction,shots,cooldown,
                ResourceLocation.fromNamespaceAndPath("omnira","item/staff_parts/"+id),1);
    }
}
