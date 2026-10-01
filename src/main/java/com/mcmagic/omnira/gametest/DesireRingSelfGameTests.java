package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.archaeology.*;
import com.mcmagic.omnira.registry.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.*;
import top.theillusivec4.curios.api.SlotContext;

@GameTestHolder("omnira_desire_self")
@PrefixGameTestTemplate(false)
public final class DesireRingSelfGameTests {
    @GameTest(template="spell_arena") public static void additiveAndSelfDamage(GameTestHelper h){
        var profile=new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"ring-self");
        // GameTest disables PvP; allow the explicitly owned self-hit in this fixture only.
        var p=new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(),h.getLevel(),profile,net.minecraft.server.level.ClientInformation.createDefault()){
            @Override public boolean canHarmPlayer(net.minecraft.world.entity.player.Player other){return true;}
        };
        p.connection=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),profile).connection;
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var ring=(ArchaeologyCurio)ArchaeologyContent.LUST_RING.get();
        ring.getAttributeModifiers(new SlotContext("ring",p,0,false,true),ResourceLocation.parse("omnira:ring_test"),new ItemStack(ring))
                .forEach((attribute,modifier)->p.getAttribute(attribute).addTransientModifier(modifier));
        p.setData(ModAttachments.DREAM_AFFINITY,true);
        h.assertTrue(Math.abs(com.mcmagic.omnira.item.CrystalArmorEffects.meleeBonus(p)-.55)<.0001,"Dream melee not additive");
        p.setData(ModAttachments.DREAM_AFFINITY,false);p.setHealth(10);
        var source=com.mcmagic.omnira.spell.SpellDamageSource.of(p,net.minecraft.world.damagesource.DamageTypes.GENERIC_KILL,p,p,"spell");
        h.assertTrue(p.hurt(source,4),"Self damage was ignored by the test player");
        h.assertTrue(Math.abs(p.getHealth()-6)<.001,"Self damage generated healing: "+p.getHealth());
        p.invulnerableTime=0;p.setHealth(5);
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(ModItems.CORRUPTED_GRIMOIRE.get()));
        var cow=h.spawn(EntityType.COW,2,2,2);cow.hurt(p.damageSources().playerAttack(p),4);
        h.assertTrue(Math.abs(p.getHealth()-8)<.001,"Book and accessory lifesteal did not add to 75 percent");cow.discard();h.succeed();
    }
}
