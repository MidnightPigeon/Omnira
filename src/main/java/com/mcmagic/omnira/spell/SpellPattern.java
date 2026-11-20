package com.mcmagic.omnira.spell;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;

public record SpellPattern(ElementType targetElement, ElementType shapeElement, Mode mode) {
    public enum Mode implements net.minecraft.util.StringRepresentable {
        STANDARD, UNORDERED, CHAOS_THEORY;
        public String getSerializedName(){return name().toLowerCase(java.util.Locale.ROOT);}
    }
    public int tooltipColor(){return mode==Mode.STANDARD?targetElement.tooltipColor():0xC5A1DF;}
    public SpellPattern(ElementType target,ElementType shape){this(target,shape,Mode.STANDARD);}
    public boolean chaosTheory(){return mode==Mode.CHAOS_THEORY;}
    public boolean unordered(){return mode==Mode.UNORDERED;}
    public static final Codec<SpellPattern> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ElementType.CODEC.optionalFieldOf("target_element").forGetter(p->Optional.ofNullable(p.targetElement())),
            ElementType.CODEC.optionalFieldOf("shape_element").forGetter(p -> Optional.ofNullable(p.shapeElement())),
            net.minecraft.util.StringRepresentable.fromEnum(Mode::values).optionalFieldOf("mode",Mode.STANDARD).forGetter(SpellPattern::mode)
    ).apply(instance, (target, shape, mode) -> new SpellPattern(target.orElse(null), shape.orElse(null),mode)));

    public SpellPattern {
        if(mode!=Mode.STANDARD && (targetElement!=null || shapeElement!=null))throw new IllegalArgumentException("Chaos patterns roll their own cores");
        if(mode==Mode.STANDARD && targetElement==null)throw new IllegalArgumentException("Missing spell target");
        if (targetElement!=null && !targetElement.isPrimary() && targetElement!=ElementType.TIME && targetElement!=ElementType.SPACE && targetElement!=ElementType.OMNI) {
            throw new IllegalArgumentException("Low-tier spell target element must be primary: " + targetElement);
        }
        if (shapeElement != null && !shapeElement.isPrimary()) {
            throw new IllegalArgumentException("Low-tier spell shape element must be primary: " + shapeElement);
        }
        if((targetElement==ElementType.TIME || targetElement==ElementType.SPACE || targetElement==ElementType.OMNI) && shapeElement!=null)
            throw new IllegalArgumentException("Composite microcores reserve the second shape slot");
    }

    public boolean worldRotation(){return targetElement==ElementType.OMNI;}
    public boolean composite(){return mode!=Mode.STANDARD || targetElement==ElementType.TIME || targetElement==ElementType.SPACE || worldRotation();}
    public String compositeName(){return unordered()?"unordered_combination":chaosTheory()?"chaos_theory":worldRotation()?"world_rotation":targetElement==ElementType.TIME?"reverse_timeflow":"spatial_blade";}
    public static boolean composite(ItemStack stack){return ElementType.byMicrocore(stack).filter(e->e==ElementType.TIME||e==ElementType.SPACE).isPresent();}
    public static boolean compatibleSlots(net.minecraft.world.Container inventory,int slot,ItemStack stack){
        var target=inventory.getItem(0);
        var shape=inventory.getItem(1);
        if(slot==0 && cluster(stack))return shape.isEmpty() || cluster(shape);
        if(slot==0 && cluster(shape))return false;
        if(slot==1 && cluster(stack))return target.isEmpty() || cluster(target);
        if(slot==1 && cluster(target))return false;
        if(slot==0)return !composite(stack)||inventory.getItem(1).isEmpty();
        return slot!=1 || !composite(inventory.getItem(0));
    }

    public static boolean cluster(ItemStack stack){return stack.is(com.mcmagic.omnira.registry.ModItems.UNORDERED_ELEMENT_CLUSTER.get());}
    public SpellTargetKeyword targetKeyword() {
        if(mode!=Mode.STANDARD)return null;
        if(worldRotation())return SpellTargetKeyword.SELF;
        return targetElement.targetKeyword().orElseThrow();
    }

    public SpellShapeKeyword shapeKeyword() {
        if(mode!=Mode.STANDARD)return null;
        if(worldRotation())return SpellShapeKeyword.BURST;
        return composite()?targetElement.shapeKeyword().orElseThrow():shapeElement == null ? null : shapeElement.shapeKeyword().orElseThrow();
    }

    public String serializedName() {
        return composite()?compositeName():targetElement.getSerializedName() + "_" + (shapeElement == null ? "direct" : shapeElement.getSerializedName());
    }

    public SpellTargetingRule targetingRule() {
        return SpellTargetingRule.forPattern(targetKeyword(), shapeKeyword());
    }

    public static Optional<SpellPattern> fromMicrocores(ItemStack targetMicrocore, ItemStack shapeMicrocore) {
        if(cluster(shapeMicrocore) && (targetMicrocore.isEmpty() || cluster(targetMicrocore)))
            return Optional.of(new SpellPattern(null,null,targetMicrocore.isEmpty()?Mode.UNORDERED:Mode.CHAOS_THEORY));
        Optional<ElementType> targetElement = ElementType.byMicrocore(targetMicrocore);
        Optional<ElementType> shapeElement = ElementType.byMicrocore(shapeMicrocore);
        if (targetElement.isEmpty() || (!shapeMicrocore.isEmpty() && shapeElement.isEmpty())) {
            return Optional.empty();
        }
        if(composite(targetMicrocore))return shapeMicrocore.isEmpty()?Optional.of(new SpellPattern(targetElement.get(),null)):Optional.empty();
        if (!targetElement.get().isPrimary() || shapeElement.filter(e -> !e.isPrimary()).isPresent()) {
            return Optional.empty();
        }
        return Optional.of(new SpellPattern(targetElement.get(), shapeElement.orElse(null)));
    }
}
