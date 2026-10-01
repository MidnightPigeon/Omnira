package com.mcmagic.omnira.spell;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;

public record SpellPattern(ElementType targetElement, ElementType shapeElement) {
    public static final Codec<SpellPattern> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ElementType.CODEC.fieldOf("target_element").forGetter(SpellPattern::targetElement),
            ElementType.CODEC.optionalFieldOf("shape_element").forGetter(p -> Optional.ofNullable(p.shapeElement()))
    ).apply(instance, (target, shape) -> new SpellPattern(target, shape.orElse(null))));

    public SpellPattern {
        if (!targetElement.isPrimary() && targetElement!=ElementType.TIME && targetElement!=ElementType.SPACE) {
            throw new IllegalArgumentException("Low-tier spell target element must be primary: " + targetElement);
        }
        if (shapeElement != null && !shapeElement.isPrimary()) {
            throw new IllegalArgumentException("Low-tier spell shape element must be primary: " + shapeElement);
        }
        if((targetElement==ElementType.TIME || targetElement==ElementType.SPACE) && shapeElement!=null)
            throw new IllegalArgumentException("Composite microcores reserve the second shape slot");
    }

    public boolean composite(){return targetElement==ElementType.TIME || targetElement==ElementType.SPACE;}
    public String compositeName(){return targetElement==ElementType.TIME?"reverse_timeflow":"spatial_blade";}
    public static boolean composite(ItemStack stack){return ElementType.byMicrocore(stack).filter(e->e==ElementType.TIME||e==ElementType.SPACE).isPresent();}
    public static boolean compatibleSlots(net.minecraft.world.Container inventory,int slot,ItemStack stack){
        if(slot==0)return !composite(stack)||inventory.getItem(1).isEmpty();
        return slot!=1 || !composite(inventory.getItem(0));
    }

    public SpellTargetKeyword targetKeyword() {
        return targetElement.targetKeyword().orElseThrow();
    }

    public SpellShapeKeyword shapeKeyword() {
        return composite()?targetElement.shapeKeyword().orElseThrow():shapeElement == null ? null : shapeElement.shapeKeyword().orElseThrow();
    }

    public String serializedName() {
        return composite()?compositeName():targetElement.getSerializedName() + "_" + (shapeElement == null ? "direct" : shapeElement.getSerializedName());
    }

    public SpellTargetingRule targetingRule() {
        return SpellTargetingRule.forPattern(targetKeyword(), shapeKeyword());
    }

    public static Optional<SpellPattern> fromMicrocores(ItemStack targetMicrocore, ItemStack shapeMicrocore) {
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
