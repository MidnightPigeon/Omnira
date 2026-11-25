package com.mcmagic.omnira.item;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.mcmagic.omnira.registry.ModAttributes;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
public final class SpellCoreItem extends net.minecraft.world.item.BlockItem implements ICurioItem,com.mcmagic.omnira.energy.EngineCore {
    public static java.util.List<ItemStack> examples(){
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.stream().filter(item->item instanceof SpellCoreItem).map(ItemStack::new).toList();
    }
    private enum WorkTier {
        BASIC(1,512), ADVANCED(2,1024), SUPERIOR(3,2048);
        final int bonus,output;
        WorkTier(int bonus,int output){this.bonus=bonus;this.output=output;}
    }
    public int workBonus(){return kind.tier.bonus;}
    public enum Kind {
        TEST("primordial_spell_core",com.mcmagic.omnira.registry.ModBlocks.TEST_SPELL_CORE,200,.15,.2,0,WorkTier.BASIC),
        DREAM("dream_spell_core",com.mcmagic.omnira.registry.ModBlocks.DREAM_SPELL_CORE,400,.2,.3,10,WorkTier.ADVANCED),
        LIGHT_DARK("light_dark_spell_core",com.mcmagic.omnira.registry.ModBlocks.LIGHT_DARK_SPELL_CORE,400,.2,.3,0,WorkTier.ADVANCED),
        SPACETIME("spacetime_spell_core",com.mcmagic.omnira.registry.ModBlocks.SPACETIME_SPELL_CORE,600,.3,.3,10,WorkTier.SUPERIOR),
        UNBOUND("unbound_mana_core",com.mcmagic.omnira.registry.ModBlocks.UNBOUND_MANA_CORE,0,.5,.25,0,WorkTier.SUPERIOR);

        private final String id;
        private final java.util.function.Supplier<? extends net.minecraft.world.level.block.Block> block;
        private final double mana,bonus,regen,costReduction;
        private final WorkTier tier;
        Kind(String id,java.util.function.Supplier<? extends net.minecraft.world.level.block.Block> block,
             double mana,double bonus,double regen,double costReduction,WorkTier tier) {
            this.id=id;this.block=block;this.mana=mana;this.bonus=bonus;this.regen=regen;this.costReduction=costReduction;this.tier=tier;
        }
    }
    private final Kind kind;
    private final Output engineOutput;
    public SpellCoreItem(Properties properties) {this(properties,false);}
    public SpellCoreItem(Properties properties,boolean dream) {
        this(properties,dream?Kind.DREAM:Kind.TEST);
    }
    public SpellCoreItem(Properties properties,boolean dream,Output output) {
        this(properties,dream?Kind.DREAM:Kind.TEST,output);
    }
    public SpellCoreItem(Properties properties,Kind kind) {
        this(properties,kind,new Output(kind.tier.output,kind.tier.output));
    }
    public SpellCoreItem(Properties properties,Kind kind,Output output) {
        super(kind.block.get(),properties.stacksTo(1));
        this.kind=kind;
        this.engineOutput=java.util.Objects.requireNonNull(output);
    }
    @Override public Output engineOutput(ItemStack stack) {return engineOutput;}
    public Kind kind() {return kind;}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,java.util.List<net.minecraft.network.chat.Component> tooltip,net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack,context,tooltip,flag);
        if(kind==Kind.UNBOUND){
            tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.omnira.unbound_core.no_leech").withStyle(net.minecraft.ChatFormatting.RED));
            tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.omnira.unbound_core.recoil").withStyle(net.minecraft.ChatFormatting.RED));
        }
        if(kind==Kind.DREAM)tooltip.add(com.mcmagic.omnira.mana.DreamText.colored(net.minecraft.network.chat.Component.translatable("tooltip.omnira.dream_core_resonance").getString()));
    }
    @Override public String getDescriptionId() {return "item.omnira."+kind.id;}
    @Override public boolean canEquip(SlotContext context,ItemStack stack) { return context.identifier().equals("spell_core"); }
    @Override public Multimap<Holder<Attribute>,AttributeModifier> getAttributeModifiers(
            SlotContext context,ResourceLocation id,ItemStack stack) {
        Multimap<Holder<Attribute>,AttributeModifier> modifiers=LinkedHashMultimap.create();
        if(!context.identifier().equals("spell_core") || !PrimarySpellAccessory.primary(context)) return modifiers;
        id=PrimarySpellAccessory.modifierId("spell_core");
        if(kind.mana!=0)modifiers.put(ModAttributes.MAX_MANA,new AttributeModifier(id,kind.mana,AttributeModifier.Operation.ADD_VALUE));
        modifiers.put(ModAttributes.SPELL_POWER,new AttributeModifier(id,kind.bonus,AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        modifiers.put(ModAttributes.MANA_REGEN,new AttributeModifier(id,kind.regen,AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        if(kind.costReduction!=0)modifiers.put(ModAttributes.COST_REDUCTION,new AttributeModifier(id,kind.costReduction,AttributeModifier.Operation.ADD_VALUE));
        return modifiers;
    }
}
