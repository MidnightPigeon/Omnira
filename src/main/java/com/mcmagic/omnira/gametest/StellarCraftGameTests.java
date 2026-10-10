package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.space.sea.*;
import com.mcmagic.omnira.space.anchor.*;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_stellar_crafts")
@PrefixGameTestTemplate(false)
public final class StellarCraftGameTests {
    private static StarClusterOrbEntity orb(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(6,5,6));h.getLevel().setBlockAndUpdate(pos,StellarCraftContent.ORB.get().defaultBlockState());
        return (StarClusterOrbEntity)h.getLevel().getBlockEntity(pos);
    }
    private static Player player(GameTestHelper h){var p=h.makeMockPlayer(GameType.SURVIVAL);GameType.SURVIVAL.updatePlayerAbilities(p.getAbilities());return p;}
    @GameTest(template="spell_arena",batch="stellar_convert") public static void wholeStackConversions(GameTestHelper h){
        var orb=orb(h);Item[] inputs={Items.COD,ModItems.ARCANE_DUST.get(),ModItems.PARADOX_DUST.get(),ModBlocks.NIGHT_HERON_STATUE.asItem(),DreamContent.SPATIAL_CRYSTAL.asItem(),Items.WATER_BUCKET};
        Item[] outputs={VoidSeaContent.JELLY_ITEM.get(),StarlightContent.DUST_ITEM.get(),StarlightContent.DUST_ITEM.get(),ModItems.VOYAGER_VIOLIN.get(),DreamContent.EXCITED_SPATIAL_CRYSTAL.asItem(),VoidSeaContent.BUCKET.get()};
        for(int i=0;i<inputs.length;i++){
            var stack=new ItemStack(inputs[i],new ItemStack(outputs[i]).getMaxStackSize()==1?1:32);orb.setItem(0,stack);
            for(int n=0;n<199;n++)orb.serverTick();
            h.assertTrue(orb.getItem(0).is(inputs[i])&&orb.progress()==199,"Stack converted before 10 seconds");
            orb.serverTick();h.assertTrue(orb.getItem(0).is(outputs[i])&&orb.getItem(0).getCount()==stack.getCount()&&orb.progress()==0,"Wrong whole-stack conversion");
        }
        h.assertTrue(orb.getContainerSize()==1&&orb.getBlockState().getLightEmission(h.getLevel(),orb.getBlockPos())==15,"Wrong orb slots or light");h.succeed();
    }
    @GameTest(template="spell_arena",batch="stellar_save") public static void changedInputsReloadAndUnprocessableStorage(GameTestHelper h){
        var orb=orb(h);var level=h.getLevel();orb.setItem(0,new ItemStack(ModItems.ARCANE_DUST.get(),12));
        for(int n=0;n<70;n++)orb.serverTick();
        orb.setItem(0,new ItemStack(ModItems.PARADOX_DUST.get(),8));orb.serverTick();
        h.assertTrue(orb.progress()==1,"Replacement inherited progress");
        for(int n=0;n<89;n++)orb.serverTick();
        var saved=orb.saveWithFullMetadata(level.registryAccess());
        var copy=new StarClusterOrbEntity(orb.getBlockPos(),orb.getBlockState());copy.setLevel(level);copy.loadWithComponents(saved,level.registryAccess());level.setBlockEntity(copy);
        h.assertTrue(copy.progress()==90,"Progress was not saved");for(int n=0;n<110;n++)copy.serverTick();
        h.assertTrue(copy.getItem(0).is(StarlightContent.DUST_ITEM.get())&&copy.getItem(0).getCount()==8,"Reload lost/doubled material");
        copy.setItem(0,new ItemStack(Items.DIAMOND,16));for(int n=0;n<220;n++)copy.serverTick();
        h.assertTrue(copy.getItem(0).is(Items.DIAMOND)&&copy.getItem(0).getCount()==16&&copy.progress()==0,"Non-recipe item processed or rejected");
        h.assertTrue(copy.itemHandler.getSlots()==1&&copy.getMaxStackSize(new ItemStack(ModBlocks.NIGHT_HERON_STATUE.asItem()))==1,"Invalid one-slot or unstackable output limit");h.succeed();
    }
    @GameTest(template="spell_arena",batch="stellar_field") public static void optionalFieldAndUpgradeBoundary(GameTestHelper h){
        var orb=orb(h);var level=h.getLevel();var center=orb.getBlockPos().getCenter();
        h.assertTrue(!StabilityField.contains(level,center),"Unupgraded orb creates field");
        h.assertTrue(!orb.upgrades.isItemValid(0,new ItemStack(ModItems.SPACETIME_STABILIZATION_UPGRADE.get()))
            &&!orb.upgrades.isItemValid(1,new ItemStack(ModItems.INTAKE_UPGRADE.get())),"Wrong upgrade allowed");
        orb.upgrades.setStackInSlot(1,new ItemStack(ModItems.SPACETIME_STABILIZATION_UPGRADE.get()));
        h.assertTrue(StabilityField.strength(level,center)==1&&StabilityField.contains(level,center.add(32,0,0))
            &&!StabilityField.contains(level,center.add(32.01,0,0)),"Stellar sphere radius/damage wrong");
        var cow=h.spawn(EntityType.COW,new BlockPos(8,5,6));cow.setNoAi(true);var origin=cow.position();
        cow.teleportTo(origin.x+1,origin.y,origin.z);h.assertTrue(cow.position().equals(origin),"Teleport was not blocked");
        h.assertTrue(StabilityField.portalTravel(cow,()->!StabilityField.blocks(cow,level,origin.add(1,0,0))),"Portal exemption changed");
        var anchorPos=orb.getBlockPos().below(2);level.setBlockAndUpdate(anchorPos,AnchorContent.ACTIVE.get().defaultBlockState());
        h.assertTrue(StabilityField.strength(level,center)==2,"Overlapping fields should take stronger damage");level.setBlockAndUpdate(anchorPos,Blocks.AIR.defaultBlockState());
        orb.upgrades.setStackInSlot(1,ItemStack.EMPTY);h.assertTrue(!StabilityField.contains(level,center),"Removing upgrade left field");
        orb.upgrades.setStackInSlot(1,new ItemStack(ModItems.SPACETIME_STABILIZATION_UPGRADE.get()));level.setBlockAndUpdate(orb.getBlockPos(),Blocks.AIR.defaultBlockState());
        h.assertTrue(!StabilityField.contains(level,center),"Removing orb left field");cow.discard();h.succeed();
    }
    @GameTest(template="spell_arena",batch="stellar_activation") public static void activationPreservesMinuteAndSave(GameTestHelper h){
        var orb=orb(h);var level=h.getLevel();var target=orb.getBlockPos().east();level.setBlockAndUpdate(target,DreamContent.SPATIAL_CRYSTAL.get().defaultBlockState());
        var activation=new SpatialActivation(orb,32);h.assertTrue(activation.begin(target),"Cannot begin nearby activation");
        for(int i=0;i<600;i++)activation.tick(level);var saved=new net.minecraft.nbt.CompoundTag();activation.save(saved);
        var restored=new SpatialActivation(orb,32);restored.load(saved,level.registryAccess());
        for(int i=0;i<599;i++)restored.tick(level);h.assertTrue(level.getBlockState(target).is(DreamContent.SPATIAL_CRYSTAL.get()),"Activation completed early");
        restored.tick(level);h.assertTrue(level.getBlockState(target).is(DreamContent.EXCITED_SPATIAL_CRYSTAL.get()),"Activation did not complete at 1 minute");
        h.assertTrue(!restored.begin(orb.getBlockPos().offset(33,0,0)),"Activation escaped radius");h.succeed();
    }
    @GameTest(template="spell_arena",batch="stellar_reflect") public static void projectileDeflectReflectAndBladeException(GameTestHelper h){
        var p=player(h);p.setPos(h.absoluteVec(new Vec3(8,5,6)));var shield=new ItemStack(StellarCraftContent.SHIELD.get());p.setItemInHand(InteractionHand.OFF_HAND,shield);
        var arrow=new net.minecraft.world.entity.projectile.Arrow(h.getLevel(),2,5,6,new ItemStack(Items.ARROW),null);arrow.setDeltaMovement(1,0,0);
        h.assertTrue(DeflectionFieldEvents.redirect(arrow,p,p.position())&&shield.getDamageValue()==1,"Passive deflection/durability missing");
        h.assertTrue(Math.abs(arrow.getDeltaMovement().length()-1)<1e-6&&arrow.getDeltaMovement().distanceToSqr(new Vec3(1,0,0))>.01,"Passive deflection did not preserve speed/change direction");
        h.assertTrue(!DeflectionFieldEvents.redirect(arrow,p,p.position())&&shield.getDamageValue()==1,"Same contact repeatedly damaged shield");
        p.startUsingItem(InteractionHand.OFF_HAND);var shot=new net.minecraft.world.entity.projectile.Arrow(h.getLevel(),2,5,6,new ItemStack(Items.ARROW),null);shot.setDeltaMovement(.8,.2,.1);
        h.assertTrue(DeflectionFieldEvents.redirect(shot,p,p.position())&&shot.getDeltaMovement().distanceToSqr(new Vec3(-.8,-.2,-.1))<1e-8,"Raised shield did not reverse path");
        var spell=com.mcmagic.omnira.spell.entity.SpellEntity.spawn(h.getLevel(),null,com.mcmagic.omnira.spell.entity.SpellEntity.Kind.PROJECTILE,p.position());spell.setDeltaMovement(.5,0,0);
        h.assertTrue(DeflectionFieldEvents.redirect(spell,p,p.position())&&spell.ownedBy(p)&&spell.getDeltaMovement().x==-.5,"Spell projectile reflection failed");
        var blade=com.mcmagic.omnira.spell.entity.SpellEntity.spawn(h.getLevel(),null,com.mcmagic.omnira.spell.entity.SpellEntity.Kind.SPATIAL_BLADE,p.position());blade.setDeltaMovement(1,0,0);
        h.assertTrue(!DeflectionFieldEvents.redirect(blade,p,p.position()),"Spatial blade was incorrectly reflected");spell.discard();blade.discard();h.succeed();
    }
    @GameTest(template="spell_arena",batch="stellar_shield") public static void reductionDrainAndUnbreaking(GameTestHelper h){
        var p=player(h);p.setPos(h.absoluteVec(new Vec3(6,5,6)));var shield=new ItemStack(StellarCraftContent.SHIELD.get());p.setItemInHand(InteractionHand.MAIN_HAND,shield);p.startUsingItem(InteractionHand.MAIN_HAND);
        p.setHealth(20);p.invulnerableTime=0;p.hurt(p.damageSources().magic(),4);
        h.assertTrue(Math.abs(p.getHealth()-17)<.001,"Raised shield must reduce by 25%, not block all damage");
        h.assertTrue(shield.getDamageValue()==0,"Shield took vanilla impact durability");
        var item=(DeflectionFieldItem)shield.getItem();for(int i=0;i<20;i++)item.onUseTick(h.getLevel(),p,shield,item.getUseDuration(shield,p)-i);
        h.assertTrue(shield.getDamageValue()==1,"Raised one second must cost one durability");p.stopUsingItem();
        var enchanted=new ItemStack(StellarCraftContent.SHIELD.get());enchanted.enchant(h.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
            .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING),3);p.setItemInHand(InteractionHand.MAIN_HAND,enchanted);
        for(int i=0;i<128;i++)DeflectionFieldItem.wear(p,enchanted);
        h.assertTrue(enchanted.getDamageValue()>0&&enchanted.getDamageValue()<100,"Unbreaking did not affect wear");h.succeed();
    }
    @GameTest(template="spell_arena",batch="stellar_recipes") public static void recipesAndFoodAreRegistered(GameTestHelper h){
        var level=h.getLevel();var manager=level.getRecipeManager();
        h.assertTrue(VoidSeaContent.JELLY_ITEM.get() instanceof ChorusFruitItem&&new ItemStack(VoidSeaContent.JELLY_ITEM.get()).has(DataComponents.FOOD),"Jellyfish must use chorus-fruit food behavior");
        h.assertTrue(manager.getAllRecipesFor(ModRecipes.STARLIGHT_TYPE.get()).size()==6,"Missing starlight conversions");
        var reaction=manager.getAllRecipesFor(ModRecipes.REACTION_TYPE.get()).stream().map(RecipeHolder::value).filter(r->r.results().stream().anyMatch(s->s.is(ModItems.UNSTABLE_SPACETIME_AGGREGATE.get()))
            &&r.items().size()==1&&r.items().getFirst().ingredient().test(new ItemStack(VoidSeaContent.JELLY_ITEM.get()))).findFirst().orElseThrow();
        h.assertTrue(reaction.support().isEmpty()&&reaction.fluidInputs().isEmpty()&&reaction.duration()==2000,"Jellyfish reaction has unrequested requirement");
        var grid=NonNullList.withSize(9,new ItemStack(StarlightContent.DUST_ITEM.get()));grid.set(4,new ItemStack(ModItems.CRYSTAL_BALL.get()));
        var input=CraftingInput.of(3,3,grid);var craft=manager.getRecipeFor(RecipeType.CRAFTING,input,level).orElseThrow();
        h.assertTrue(craft.value().assemble(input,level.registryAccess()).is(StellarCraftContent.ORB_ITEM.get()),"8-dust manual recipe missing");
        grid.get(4).set(DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.CustomData.of(new net.minecraft.nbt.CompoundTag()));
        h.assertTrue(!craft.value().matches(CraftingInput.of(3,3,grid),level),"Crafting consumed source orb metadata");
        var assembly=manager.getAllRecipesFor(ModRecipes.ASSEMBLY_TYPE.get()).stream().map(RecipeHolder::value).filter(r->r.result().is(StellarCraftContent.ORB_ITEM.get())).findFirst().orElseThrow();
        h.assertTrue(assembly.slots().size()==7&&assembly.slots().get(6).test(new ItemStack(ModItems.CRYSTAL_BALL.get())),"6-dust assembly recipe missing");h.succeed();
    }
    @GameTest(template="spell_arena",batch="stellar_dodge") public static void attackEvasionDoesNotCancelEnvironmentalDamage(GameTestHelper h){
        var p=player(h);p.getRandom().setSeed(71234);p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(StellarCraftContent.SHIELD.get()));
        var attacker=h.spawn(EntityType.COW,new BlockPos(4,5,4));attacker.setNoAi(true);int avoided=0;
        for(int i=0;i<400;i++){
            p.setHealth(20);p.invulnerableTime=0;p.hurt(p.damageSources().mobAttack(attacker),1);
            if(p.getHealth()==20)avoided++;
        }
        h.assertTrue(avoided>=65&&avoided<=135,"25% evasion not applied: "+avoided);
        p.setHealth(20);p.invulnerableTime=0;p.hurt(p.damageSources().magic(),2);
        h.assertTrue(p.getHealth()==18,"Environmental damage was evaded while lowered");attacker.discard();h.succeed();
    }
    @GameTest(template="spell_arena",batch="stellar_repair") public static void manaRepairUsesExistingHeldRule(GameTestHelper h){
        var p=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"stellar-repair"));
        GameType.SURVIVAL.updatePlayerAbilities(p.getAbilities());
        var shield=new ItemStack(StellarCraftContent.SHIELD.get());shield.setDamageValue(10);p.setItemInHand(InteractionHand.OFF_HAND,shield);
        var before=p.getData(ModAttachments.MANA);
        h.assertTrue(before.canSpend(5)&&com.mcmagic.omnira.item.HeldManaRepair.repair(shield,p)&&shield.getDamageValue()==9
            &&p.getData(ModAttachments.MANA).equals(before.spend(5)),"Held field did not repair at standard mana cost");
        p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
        h.assertTrue(!com.mcmagic.omnira.item.HeldManaRepair.repair(shield,p),"Inventory-only field repaired itself");h.succeed();
    }
}
