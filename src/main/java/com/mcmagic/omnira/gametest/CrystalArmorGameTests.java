package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.item.CrystalArmorEffects;
import com.mcmagic.omnira.registry.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;
import java.util.UUID;

@GameTestHolder("omnira_armor")
@PrefixGameTestTemplate(false)
public final class CrystalArmorGameTests {
    @GameTest(template="spell_arena")
    public static void setBonusesAndRemoval(GameTestHelper h){
        var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"armor-test")){
            @Override public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source){return false;}
        };
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        p.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(4,2,4)));
        double gravity=p.getAttributeValue(Attributes.GRAVITY);
        for(boolean dress:new boolean[]{false,true}){
            var set=dress?ArmorContent.CLOTH:ArmorContent.PLATE;
            set.forEach((type,item)->p.setItemSlot(type.getSlot(),new ItemStack(item.get())));
            p.doTick();CrystalArmorEffects.refresh(p);
            h.assertTrue(p.getAttributeValue(ModAttributes.MAX_MANA)==300,"Four pieces must add 200 mana");
            h.assertTrue(Math.abs(p.getAttributeValue(ModAttributes.SPELL_POWER)-(dress?1.3:1.2))<.00001,"Set spell power");
            h.assertTrue(Math.abs(p.getAttributeValue(Attributes.GRAVITY)-gravity*(dress?.7:1))<.00001,"Set gravity");
            if(dress) {
                var gravityAttribute=p.getAttribute(Attributes.GRAVITY);
                var id=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("omnira","gravity_addition_test");
                gravityAttribute.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        id,.1,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                h.assertTrue(Math.abs(p.getAttributeValue(Attributes.GRAVITY)-gravity*.8)<.00001,"Gravity percentages must add");
                gravityAttribute.removeModifier(id);
            }
            CrystalArmorEffects.refresh(p);
            h.assertTrue(p.getAttributeValue(ModAttributes.MAX_MANA)==300,"Repeated refresh stacks mana");
            // Fake players retain server spawn protection; exercise the actual registered damage event.
            var damage=new net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre(p,
                    new net.neoforged.neoforge.common.damagesource.DamageContainer(p.damageSources().magic(),10));
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(damage);
            h.assertTrue(Math.abs(damage.getNewDamage()-(dress?9:8))<.001,"Final damage reduction");
            var cow=h.spawn(EntityType.COW,2,2,2);
            cow.hurt(p.damageSources().playerAttack(p),5);
            h.assertTrue(Math.abs(cow.getHealth()-(dress?5:4))<.001,"Melee bonus or dress leakage");
            cow.discard();
            for(var weapon:new Item[]{ModItems.CRYSTALLIZED_NAIL.get(),ModItems.ARCANE_NEEDLE.get()}){
                p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(weapon));
                float extra=weapon.getAttackDamageBonus(p,10,p.damageSources().playerAttack(p));
                h.assertTrue(Math.abs(extra-(dress?3:4))<.001,"Sword spell and melee bonuses must add");
                h.assertTrue(Math.abs(com.mcmagic.omnira.spell.CastAttributes.power(p,0)-(dress?1.3:1.2))<.001,"Melee bonus leaked into spell power");
            }
            p.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
            var other=dress?ArmorContent.PLATE:ArmorContent.CLOTH;
            p.setItemSlot(EquipmentSlot.HEAD,new ItemStack(other.get(ArmorItem.Type.HELMET).get()));
            p.doTick();CrystalArmorEffects.refresh(p);
            h.assertTrue(CrystalArmorEffects.equipped(p)==CrystalArmorEffects.SetBonus.NONE,"Mixed set activated");
            h.assertTrue(p.getAttributeValue(ModAttributes.SPELL_POWER)==1 && p.getAttributeValue(Attributes.GRAVITY)==gravity,"Mixed set retains bonus");
            h.assertTrue(p.getAttributeValue(ModAttributes.MAX_MANA)==300,"Mixed pieces lose mana");
            p.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);p.doTick();CrystalArmorEffects.refresh(p);
            h.assertTrue(p.getAttributeValue(ModAttributes.MAX_MANA)==250,"Removal retains piece mana");
        }
        for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET})p.setItemSlot(slot,ItemStack.EMPTY);
        p.doTick();CrystalArmorEffects.refresh(p);
        h.assertTrue(p.getAttributeValue(ModAttributes.MAX_MANA)==100,"Empty set retains mana");
        h.succeed();
    }
}
