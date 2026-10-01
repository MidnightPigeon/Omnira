package com.mcmagic.omnira.item.staff;

import com.mcmagic.omnira.registry.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Stores actual components, not an index into a finite table of staff variants. */
public record StaffAssembly(ItemStack shaft,ItemStack reinforcement,ItemStack tip,List<ItemStack> upgrades) {
    public static final Codec<StaffAssembly> CODEC=RecordCodecBuilder.<StaffAssembly>create(i->i.group(
            ItemStack.STRICT_CODEC.fieldOf("shaft").forGetter(StaffAssembly::shaft),
            ItemStack.STRICT_CODEC.fieldOf("reinforcement").forGetter(StaffAssembly::reinforcement),
            ItemStack.STRICT_CODEC.fieldOf("tip").forGetter(StaffAssembly::tip),
            ItemStack.STRICT_CODEC.listOf().optionalFieldOf("upgrades",List.of()).forGetter(StaffAssembly::upgrades)
    ).apply(i,StaffAssembly::new)).validate(a->a.valid()?DataResult.success(a):DataResult.error(()->"Invalid staff components, duplicate upgrades or exceeded slot capacity"));

    public StaffAssembly {
        shaft=shaft.copyWithCount(1);reinforcement=reinforcement.copyWithCount(1);tip=tip.copyWithCount(1);
        upgrades=upgrades.stream().map(s->s.copyWithCount(1)).toList();
    }
    public static StaffAssembly basic() {
        return new StaffAssembly(new ItemStack(ModItems.WOODEN_STAFF_SHAFT.get()),new ItemStack(ModItems.IRON_REINFORCEMENT.get()),
                new ItemStack(ModItems.SPIRITUAL_CRYSTAL_TIP.get()),List.of());
    }
    public static StaffAssembly of(ItemStack staff) {
        var assembly=staff.get(ModDataComponents.STAFF_ASSEMBLY);
        return assembly==null?basic():assembly;
    }
    public boolean valid() {
        if(!StaffPart.is(shaft,StaffPart.Role.SHAFT) || !StaffPart.is(reinforcement,StaffPart.Role.REINFORCEMENT)
                || !StaffPart.is(tip,StaffPart.Role.TIP)) return false;
        var counts=new HashMap<net.minecraft.world.item.Item,Integer>();
        var limits=new HashMap<net.minecraft.world.item.Item,Integer>();
        for(var stack:upgrades) {
            if(!StaffPart.is(stack,StaffPart.Role.UPGRADE)) return false;
            var part=StaffPart.of(stack);
            int allowedCopies=part.upgradeSlot()==StaffPart.SlotKind.RUNE?1:part.maxCopies();
            int limit=limits.merge(stack.getItem(),allowedCopies,Math::min);
            if(counts.merge(stack.getItem(),1,Integer::sum)>limit) return false;
        }
        for(var kind:StaffPart.SlotKind.values()) if(usedSlots(kind)>slotCapacity(kind)) return false;
        return true;
    }
    public int slotCapacity(StaffPart.SlotKind kind) {
        var owner=StaffPart.of(kind==StaffPart.SlotKind.ENHANCEMENT?shaft:tip);
        return owner==null?0:kind==StaffPart.SlotKind.ENHANCEMENT?owner.enhancementSlots():owner.runeSlots();
    }
    public int usedSlots(StaffPart.SlotKind kind) {
        int used=0;
        for(var stack:upgrades) {
            var part=StaffPart.of(stack);
            if(part!=null && part.role()==StaffPart.Role.UPGRADE && part.upgradeSlot()==kind) used++;
        }
        return used;
    }
    public List<ItemStack> components() {
        return internalComponents().stream().map(ItemStack::copy).toList();
    }
    private List<ItemStack> internalComponents() {
        var result=new ArrayList<ItemStack>(3+upgrades.size());
        result.add(shaft);result.add(reinforcement);result.add(tip);result.addAll(upgrades);
        return result;
    }
    @Override public ItemStack shaft() {return shaft.copy();}
    @Override public ItemStack reinforcement() {return reinforcement.copy();}
    @Override public ItemStack tip() {return tip.copy();}
    @Override public List<ItemStack> upgrades() {return upgrades.stream().map(ItemStack::copy).toList();}
    @Override public boolean equals(Object other) {
        if(!(other instanceof StaffAssembly a) || !ItemStack.matches(shaft,a.shaft) || !ItemStack.matches(reinforcement,a.reinforcement)
                || !ItemStack.matches(tip,a.tip) || upgrades.size()!=a.upgrades.size()) return false;
        for(int i=0;i<upgrades.size();i++) if(!ItemStack.matches(upgrades.get(i),a.upgrades.get(i))) return false;
        return true;
    }
    @Override public int hashCode() {
        int hash=1;for(var part:internalComponents()) hash=31*hash+ItemStack.hashItemAndComponents(part);return hash;
    }
    public Stats stats() {
        double power=0,reduction=0,speed=0,cooldownReduction=0,range=0;int shots=0,cooldown=0;
        for(var stack:internalComponents()) {
            var part=StaffPart.of(stack);
            if(part==null) continue;
            power+=part.power();reduction+=part.reduction();shots+=part.shots();cooldown+=part.cooldownTicks();
            speed+=part.speed();cooldownReduction+=part.cooldownReduction();range+=part.range();
        }
        // Bound runtime work even if a custom data pack supplies extreme values.
        int count=Math.clamp(shots,1,64);
        return new Stats(Math.max(-.99,power),Math.max(0,reduction),count,
                Math.clamp(cooldown,Math.max(com.mcmagic.omnira.spell.SpellCooldowns.MIN_BASE_TICKS,(count-1)*StaffEvents.SHOT_INTERVAL+1),72000),
                Math.clamp(1+speed,1,11),Math.clamp(1+range,1,3),Math.clamp(cooldownReduction,0,com.mcmagic.omnira.spell.SpellCooldowns.MAX_REDUCTION));
    }
    public record Stats(double power,double reduction,int shots,int cooldownTicks,double speedMultiplier,double rangeMultiplier,double cooldownReduction) {}
    public ItemStack create() {
        var result=new ItemStack(ModItems.MODULAR_STAFF.get());result.set(ModDataComponents.STAFF_ASSEMBLY,this);return result;
    }
    public Optional<StaffAssembly> withUpgrades(List<ItemStack> additions) {
        var all=new ArrayList<>(upgrades);all.addAll(additions);
        var candidate=new StaffAssembly(shaft,reinforcement,tip,all);
        return candidate.valid()?Optional.of(candidate):Optional.empty();
    }
}
