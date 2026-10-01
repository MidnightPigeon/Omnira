package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.spell.*;
import com.mcmagic.omnira.spell.entity.SpellEntity;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.menu.CrystalProcessingTableMenu;
import com.mcmagic.omnira.block.entity.CrystalProcessingTableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.common.util.FakePlayer;
import com.mojang.authlib.GameProfile;
import java.util.*;

@GameTestHolder("omnira_spell_elements")
@PrefixGameTestTemplate(false)
public final class SpellElementGameTests {
    @GameTest(template="spell_arena")
    public static void compositionAndSlotRoles(GameTestHelper h) {
        var inventory=new SimpleContainer(9);
        inventory.setItem(4,new ItemStack(ModItems.SHARP_BREATH.get()));
        inventory.setItem(5,new ItemStack(ModItems.HEALING_DEW.get()));
        for(int clocks=0;clocks<=2;clocks++) for(int iron=0;iron<=2-clocks;iron++) {
            inventory.setItem(2,ItemStack.EMPTY);inventory.setItem(3,ItemStack.EMPTY);
            for(int i=0;i<clocks+iron;i++) inventory.setItem(2+i,new ItemStack(i<clocks?Items.CLOCK:Items.IRON_BLOCK,64));
            var effects=CrystalProcessingTableMenu.composeEffects(inventory);
            h.assertTrue(effects.size()==2 && !effects.getFirst().healing() && effects.getLast().healing(),"Wrong element mapping");
            for(var effect:effects) {
                h.assertTrue(effect.amplifier()==(clocks>0?0:1)+iron,"Wrong potion level");
                h.assertTrue(effect.duration()==(clocks==0?0:800+1200*(clocks-1)),"Wrong duration");
            }
        }
        inventory.setItem(2,new ItemStack(ModItems.SPIRITUAL_CRYSTAL.get()));
        inventory.setItem(3,new ItemStack(Items.CLOCK));
        h.assertTrue(CrystalProcessingTableMenu.composeEffects(inventory).stream().allMatch(e->e.infused() && e.duration()==800),
                "Infusion must preserve delayed composition");
        h.assertTrue(CrystalProcessingTableMenu.mayPlace(4,new ItemStack(ModItems.SHARP_BREATH.get())),"Tag missing");
        h.assertTrue(!CrystalProcessingTableMenu.mayPlace(5,new ItemStack(Items.CLOCK)),"Clock accepted as element");
        h.assertTrue(CrystalProcessingTableMenu.mayPlace(2,new ItemStack(Items.DIAMOND)),"Modifiers incorrectly whitelisted");
        inventory.setItem(5,new ItemStack(ModItems.SHARP_BREATH.get()));
        h.assertTrue(CrystalProcessingTableMenu.composeEffects(inventory).size()==1,"Repeated elements must apply once");
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void optionalShapeAndDirectSelf(GameTestHelper h) {
        var inventory=new SimpleContainer(9);
        inventory.setItem(0,new ItemStack(ModItems.WATER_MICROCORE.get()));
        inventory.setItem(4,new ItemStack(ModItems.HEALING_DEW.get()));
        inventory.setItem(5,new ItemStack(ModItems.HEALING_DEW.get()));
        var crystal=CrystalProcessingTableMenu.previewCrystal(inventory);
        var pattern=crystal.get(ModDataComponents.SPELL_PATTERN);
        var payload=SpellPayload.of(crystal);
        h.assertTrue(pattern!=null && pattern.shapeElement()==null && payload.effects().size()==1
                && payload.baseCost()==30 && payload.keywords().size()==1,"Optional shape or duplicate element writing failed");
        var encoded=SpellPattern.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,pattern).getOrThrow();
        h.assertTrue(SpellPattern.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,encoded).getOrThrow().equals(pattern),
                "Shape-free crystal did not persist");
        crystal.set(ModDataComponents.SPELL_PAYLOAD,new SpellPayload(30,0,
                List.of(payload.effects().getFirst(),payload.effects().getFirst()),List.of("healing","healing")));
        h.assertTrue(SpellPayload.of(crystal).effects().size()==1 && SpellPayload.of(crystal).keywords().size()==1,
                "Previously written duplicate elements still stack");
        crystal.set(ModDataComponents.SPELL_PAYLOAD,payload);
        var player=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"direct-self"));
        player.setPos(h.absoluteVec(new Vec3(5.5,2,5.5)));
        h.getLevel().addFreshEntity(player);
        player.setHealth(4);
        h.assertTrue(SpellCasting.cast(player,pattern,payload,1) && player.getHealth()>4,"Direct self spell failed");
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void vanillaInstantAndTimedEffects(GameTestHelper h) {
        var cow=h.spawn(EntityType.COW,5,2,5);
        var undead=h.spawn(EntityType.ZOMBIE,8,2,8);
        var spell=SpellEntity.spawn(h.getLevel(),cow,SpellEntity.Kind.PROJECTILE,cow.position());
        cow.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(20);
        cow.setHealth(20);
        SpellEffect harm=new SpellEffect(false,1,0,true),heal=SpellEffect.compose(true,0,0);
        harm.apply(spell,cow,cow,1);
        h.assertTrue(cow.getHealth()==8,"Instant harm II must deal vanilla 12 damage");
        cow.setHealth(2);heal.apply(spell,cow,cow,1);
        h.assertTrue(cow.getHealth()==10,"Instant health II must heal vanilla 8");
        undead.setHealth(2);harm.apply(spell,cow,undead,1);
        h.assertTrue(undead.getHealth()==10,"Undead inversion missing");
        SpellEffect.compose(false,1,1).apply(spell,cow,cow,1);
        h.assertTrue(cow.getEffect(MobEffects.POISON).getAmplifier()==1 && cow.getEffect(MobEffects.POISON).getDuration()==800,"Poison II/40s missing");
        SpellEffect.compose(true,2,0).apply(spell,cow,cow,1);
        h.assertTrue(cow.getEffect(MobEffects.REGENERATION).getAmplifier()==0 && cow.getEffect(MobEffects.REGENERATION).getDuration()==2000,"Regeneration I/100s missing");
        cow.setHealth(1);MobEffects.POISON.value().applyEffectTick(cow,0);
        h.assertTrue(cow.getHealth()==1,"Poison must not kill");
        var payload=new SpellPayload(60,0,List.of(harm,SpellEffect.compose(true,1,1)));
        var encoded=SpellPayload.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,payload).getOrThrow();
        h.assertTrue(SpellPayload.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,encoded).getOrThrow().equals(payload),"Payload does not persist");
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void potionWritingAndConsumption(GameTestHelper h) {
        h.setBlock(4,2,4,ModBlocks.CRYSTAL_PROCESSING_TABLE.get());
        var table=(CrystalProcessingTableBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(4,2,4)));
        var player=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"potion-test"));
        player.setPos(h.absoluteVec(new Vec3(4.5,2,6.5)));
        var menu=new CrystalProcessingTableMenu(1,player.getInventory(),table,table);
        table.setItem(0,new ItemStack(ModItems.WATER_MICROCORE.get()));
        table.setItem(1,new ItemStack(ModItems.FIRE_MICROCORE.get()));
        table.setItem(2,new ItemStack(Items.CLOCK,4));
        table.setItem(3,new ItemStack(Items.IRON_BLOCK,4));
        table.setItem(4,new ItemStack(ModItems.SHARP_BREATH.get(),4));
        table.setItem(5,new ItemStack(ModItems.HEALING_DEW.get(),4));
        table.setItem(6,new ItemStack(Items.INK_SAC));
        table.setItem(7,new ItemStack(ModItems.SPIRITUAL_CRYSTAL.get()));
        h.assertTrue(menu.baseManaCost()==60 && menu.clickMenuButton(player,0),"Writing rejected valid potion ingredients");
        h.runAtTickTime(65,()->{
            menu.broadcastChanges();
            var payload=SpellPayload.of(table.getItem(8));
            h.assertTrue(payload.effects().size()==2 && payload.effects().stream().allMatch(e->e.amplifier()==1 && e.duration()==800),"Wrong written payload");
            for(int i=2;i<6;i++) h.assertTrue(table.getItem(i).getCount()==3,"Must consume one component per slot");
            h.assertTrue(player.getData(ModAttachments.MANA).current()==40,"Wrong mana charge");
            h.succeed();
        });
    }
}
