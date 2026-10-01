package com.mcmagic.omnira.item;

import com.mcmagic.omnira.mana.ManaCosts;
import com.mcmagic.omnira.registry.ModAttachments;
import com.mcmagic.omnira.registry.ModAttributes;
import com.mcmagic.omnira.spell.CastAttributes;
import com.mcmagic.omnira.spell.IndependentSpellCooldown;
import com.mcmagic.omnira.spell.SpellCasting;
import com.mcmagic.omnira.spell.SpellCooldowns;
import com.mcmagic.omnira.spell.SpellDamageSource;
import com.mcmagic.omnira.spell.SpellTargetingRule;
import com.mcmagic.omnira.spell.entity.SpellEntity;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

public final class InfusedGrimoireItem extends Item {
    public enum Kind { INFUSED, SANCTIFIED, CORRUPTED }
    private final Kind kind;
    public InfusedGrimoireItem(Properties properties,Kind kind) {
        super(properties.stacksTo(1).component(DataComponents.TOOL,SwordItem.createToolProperties())
                .attributes(ItemAttributeModifiers.builder()
                        .add(Attributes.ATTACK_DAMAGE,new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID,4,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND)
                        .add(Attributes.ATTACK_SPEED,new AttributeModifier(Item.BASE_ATTACK_SPEED_ID,-2,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND)
                        .add(ModAttributes.SPELL_POWER,new AttributeModifier(ResourceLocation.fromNamespaceAndPath("omnira","grimoire_power"),kind==Kind.INFUSED?.1:.15,
                                AttributeModifier.Operation.ADD_MULTIPLIED_BASE),EquipmentSlotGroup.MAINHAND).build()));
        this.kind=kind;
    }
    public Kind kind(){return kind;}
    @Override public float getAttackDamageBonus(Entity target,float base,DamageSource source) {
        return source.getEntity() instanceof Player player
                ?(float)(base*(CastAttributes.power(player,0)-1+CrystalArmorEffects.meleeBonus(player))):0;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        ItemStack stack=player.getItemInHand(hand);
        if(!IndependentSpellCooldown.ready(player,stack))return InteractionResultHolder.pass(stack);
        if(!(level instanceof ServerLevel server))return InteractionResultHolder.success(stack);
        if(!player.isAlive() || player.isSpectator())return InteractionResultHolder.fail(stack);
        double range=1,speed=1;
        if(player.getOffhandItem().getItem() instanceof StaffItem) {
            var stats=com.mcmagic.omnira.item.staff.StaffAssembly.of(player.getOffhandItem()).stats();
            range=stats.rangeMultiplier();speed=stats.speedMultiplier();
        }
        double selection=SpellTargetingRule.TARGET_MAX_RANGE*(range>1?2:1);
        HitResult hit=SpellCasting.target(player,selection,true);
        if(hit.getType()==HitResult.Type.MISS)return InteractionResultHolder.fail(stack);
        var mana=player.getData(ModAttachments.MANA);
        double manaCost=ManaCosts.cost(player,150);
        if(kind==Kind.SANCTIFIED && !mana.canSpend(manaCost))return InteractionResultHolder.fail(stack);
        int missing=kind==Kind.INFUSED?Math.max(0,30-player.totalExperience):0;
        int healthCost=kind==Kind.CORRUPTED?6:(missing+4)/5;
        if(kind==Kind.SANCTIFIED)player.setData(ModAttachments.MANA,mana.spend(manaCost));
        if(kind==Kind.INFUSED)player.giveExperiencePoints(-Math.min(30,player.totalExperience));
        if(healthCost>0) {
            player.setHealth(player.getHealth()-healthCost);
            if(!player.isAlive()) {
                player.die(SpellDamageSource.of(player,DamageTypes.GENERIC_KILL,null,null,"knowledge_extraction"));
                return InteractionResultHolder.success(stack);
            }
        }
        var projectile=SpellEntity.spawn(server,player,SpellEntity.Kind.PROJECTILE,player.getEyePosition());
        projectile.configure(com.mcmagic.omnira.fate.FateEffects.spellPower(player,CastAttributes.power(player,0)),10);
        projectile.speedMultiplier(speed);
        projectile.setSpellColor(healthCost>0?0xF1A9B2:kind==Kind.SANCTIFIED?mana.color():0xA9DBF4);
        projectile.knowledgeShock(healthCost>0,kind==Kind.CORRUPTED);
        projectile.setDeltaMovement(player.getLookAngle().scale(SpellCasting.PROJECTILE_SPEED*projectile.speedMultiplier()));
        var entity=hit instanceof EntityHitResult entityHit?entityHit.getEntity():null;
        projectile.home(entity,entity==null?hit.getLocation():entity.getBoundingBox().getCenter());
        if(entity==null && hit instanceof net.minecraft.world.phys.BlockHitResult block)projectile.homeBlock(block);
        IndependentSpellCooldown.start(player,stack,SpellCooldowns.ticks(player,kind==Kind.SANCTIFIED?30:60,1));
        server.playSound(null,player.blockPosition(),SoundEvents.AMETHYST_BLOCK_CHIME,SoundSource.PLAYERS,.8F,kind==Kind.CORRUPTED?.75F:1.3F);
        return InteractionResultHolder.success(stack);
    }
    @Override public int getEnchantmentValue(){return 15;}
    @Override public boolean isEnchantable(ItemStack stack){return !stack.isEnchanted();}
    private static boolean allowed(Holder<Enchantment> enchantment){return !enchantment.is(Enchantments.UNBREAKING)&&!enchantment.is(Enchantments.MENDING);}
    @Override public boolean supportsEnchantment(ItemStack stack,Holder<Enchantment> enchantment){return allowed(enchantment)&&super.supportsEnchantment(stack,enchantment);}
    @Override public boolean isPrimaryItemFor(ItemStack stack,Holder<Enchantment> enchantment){return allowed(enchantment)&&super.isPrimaryItemFor(stack,enchantment);}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.omnira.grimoire."+kind.name().toLowerCase(java.util.Locale.ROOT))
                .withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
