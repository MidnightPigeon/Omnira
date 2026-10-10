package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.forging.*;
import com.mcmagic.omnira.item.*;
import com.mcmagic.omnira.mana.ManaState;
import com.mcmagic.omnira.registry.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("omnira_sword_upgrades")
@PrefixGameTestTemplate(false)
public final class RitualSwordUpgradeGameTests {
    @GameTest(template="spell_arena") public static void renamedStagesReadLegacyItems(GameTestHelper h){
        String[][] legacy={{"sharpened_bone_nail","channelled_bone_nail","coiled_bone_nail","pure_bone_nail"},
                {"sharpened_arcane_needle","shining_arcane_needle","hivesteel_arcane_needle","pale_steel_arcane_needle"}};
        String[][] current={{"sharpened_crystallized_bone_nail","channelled_crystallized_bone_nail","threaded_paradox_bone_nail","pure_paradox_bone_nail"},
                {"sharpened_arcane_needle","shining_arcane_needle","tempered_paradox_needle","pale_paradox_needle"}};
        for(var kind:RitualSwordItem.Kind.values())for(int stage=1;stage<=4;stage++){
            int row=kind==RitualSwordItem.Kind.NAIL?0:1;
            var stack=weapon(kind,stage);stack.setDamageValue(73);stack.set(DataComponents.CUSTOM_NAME,Component.literal("Legacy"));
            stack.enchant(h.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS),2);
            var saved=(net.minecraft.nbt.CompoundTag)stack.save(h.getLevel().registryAccess());saved.putString("id","omnira:"+legacy[row][stage-1]);
            var loaded=ItemStack.parseOptional(h.getLevel().registryAccess(),saved);
            h.assertTrue(ItemStack.matches(stack,loaded),"Renamed stage lost legacy item or components");
            var id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(loaded.getItem()).toString();
            h.assertTrue(id.equals("omnira:"+current[row][stage-1]),"Stage did not use canonical ID: "+id);
            h.assertTrue(((net.minecraft.nbt.CompoundTag)loaded.save(h.getLevel().registryAccess())).getString("id").equals(id),"Resave retained legacy ID");
        }
        h.succeed();
    }
    private static ItemStack weapon(RitualSwordItem.Kind kind,int level){return new ItemStack(RitualSwordUpgrades.item(kind,level));}
    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h,ItemStack weapon){
        var profile=new GameProfile(UUID.randomUUID(),"upgrade-test");
        var p=new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(),h.getLevel(),profile,net.minecraft.server.level.ClientInformation.createDefault());
        p.connection=new FakePlayer(h.getLevel(),profile).connection;
        p.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(5.5,2,6.5)));p.setOnGround(true);
        p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);p.setHealth(1);
        p.getAttribute(ModAttributes.MAX_MANA).setBaseValue(120);p.setData(ModAttachments.MANA,new ManaState(120,120));
        p.setItemInHand(InteractionHand.MAIN_HAND,weapon);return p;
    }
    private static AdvancedForgeRecipe recipe(GameTestHelper h,RitualSwordItem.Kind kind,int level){
        var item=RitualSwordUpgrades.item(kind,level);
        return h.getLevel().getRecipeManager().getAllRecipesFor(ModRecipes.ADVANCED_FORGE_TYPE.get()).stream()
                .map(net.minecraft.world.item.crafting.RecipeHolder::value).filter(r->r.result().is(item)).findFirst().orElseThrow();
    }
    private static SimpleContainer input(AdvancedForgeRecipe recipe){
        var c=new SimpleContainer(9);for(int i=0;i<9;i++)c.setItem(i,recipe.slots().get(i).getItems()[0].copyWithCount(1));return c;
    }
    @GameTest(template="spell_arena") public static void stageStatsAreAdditiveAndSwordEnchantsRemain(GameTestHelper h){
        for(var kind:RitualSwordItem.Kind.values())for(int level=0;level<=4;level++){
            var stack=weapon(kind,level);var sword=(RitualSwordItem)stack.getItem();
            var modifiers=stack.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers();
            double damage=modifiers.stream().filter(m->m.attribute().is(Attributes.ATTACK_DAMAGE)).mapToDouble(m->m.modifier().amount()).sum();
            double speed=modifiers.stream().filter(m->m.attribute().is(Attributes.ATTACK_SPEED)).mapToDouble(m->m.modifier().amount()).sum();
            h.assertTrue(Math.abs(damage-(7+2*level))<1e-6 && Math.abs(speed-(-2.2+.1*level))<1e-6,"Wrong attack attributes at "+level);
            h.assertTrue(stack.getMaxDamage()==1024 && stack.getMaxStackSize()==1,"Stage durability/stacking changed");
            h.assertTrue(sword.completeHeal()==12+3*level && sword.segmentHeal()==4+level && sword.specialRangeMultiplier()==1+.25*level,"Non-additive level bonuses");
            h.assertTrue(stack.is(net.minecraft.tags.ItemTags.SWORDS),"Stage missing sword tag");
            h.assertTrue(stack.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,ResourceLocation.parse("minecraft:enchantable/sword"))),"Stage cannot accept sword enchantments");
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void allRecipesKeepPositionsAndCannotSkipLevels(GameTestHelper h){
        for(var kind:RitualSwordItem.Kind.values())for(int level=1;level<=4;level++){
            var recipe=recipe(h,kind,level);var c=input(recipe);var in=new AdvancedForgeRecipe.Input(c);
            h.assertTrue(recipe.operation().equals("ritual_sword_upgrade") && recipe.matches(in,h.getLevel()),"Stage recipe unavailable");
            for(int i=0;i<6;i++)h.assertTrue(c.getItem(i).is(ModItems.PARADOX_DUST.get()),"Surface must contain six dust");
            h.assertTrue(c.getItem(6).is(RitualSwordUpgrades.item(kind,level-1)),"Weapon is not at left node");
            String aux=List.of("infused_crystal_block","dream_crystal_block","dilute_spacetime_bucket","omni_microcore").get(level-1);
            h.assertTrue(c.getItem(7).is(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.parse("omnira:"+aux))),"Wrong stage auxiliary");
            h.assertTrue(c.getItem(8).is(kind==RitualSwordItem.Kind.NAIL?com.mcmagic.omnira.paradox.ParadoxContent.ORE.get():
                    com.mcmagic.omnira.paradox.ParadoxContent.BOTTLED_FLOW.get()),"Wrong weapon-specific material");
            var right=c.getItem(8);c.setItem(8,c.getItem(6));c.setItem(6,right);
            h.assertTrue(!recipe.matches(in,h.getLevel()),"Swapped nodes accepted");
            c=input(recipe);c.setItem(6,weapon(kind,level));h.assertTrue(!recipe.matches(new AdvancedForgeRecipe.Input(c),h.getLevel()),"Same-stage source accepted");
            h.assertTrue(RitualSwordUpgrades.assemble(weapon(kind,0),weapon(kind,2)).isEmpty(),"Skipped stage");
            var other=kind==RitualSwordItem.Kind.NAIL?RitualSwordItem.Kind.NEEDLE:RitualSwordItem.Kind.NAIL;
            h.assertTrue(RitualSwordUpgrades.assemble(weapon(other,level-1),weapon(kind,level)).isEmpty(),"Changed weapon kind");
            if(level==3)h.assertTrue(recipe.getRemainingItems(new AdvancedForgeRecipe.Input(input(recipe))).get(7).is(Items.BUCKET),"Fluid bucket not returned");
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void chainedUpgradesPreserveLoadedCrystalAndComponents(GameTestHelper h){
        for(var kind:RitualSwordItem.Kind.values()){
            var stack=weapon(kind,0);stack.setDamageValue(321);stack.set(DataComponents.CUSTOM_NAME,Component.literal("Preserved"));
            stack.enchant(h.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.SHARPNESS),3);
            var crystal=new ItemStack(ModItems.LOW_TIER_MAGIC_CRYSTAL.get());
            crystal.set(ModDataComponents.SPELL_PATTERN,new com.mcmagic.omnira.spell.SpellPattern(com.mcmagic.omnira.spell.ElementType.WATER,com.mcmagic.omnira.spell.ElementType.EARTH));
            crystal.set(ModDataComponents.SPELL_PAYLOAD,new com.mcmagic.omnira.spell.SpellPayload(20,3));
            stack.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(crystal)));
            var components=stack.getComponentsPatch();
            for(int level=1;level<=4;level++){
                var c=input(recipe(h,kind,level));c.setItem(6,stack);var original=stack.copy();
                stack=recipe(h,kind,level).assemble(new AdvancedForgeRecipe.Input(c),h.getLevel().registryAccess());
                h.assertTrue(stack.getComponentsPatch().equals(components) && stack.getDamageValue()==321 && !RitualSwordItem.crystal(stack).isEmpty(),"Upgrade lost explicit components");
                h.assertTrue(ItemStack.matches(crystal,RitualSwordItem.crystal(stack)) && RitualSwordItem.accepts(RitualSwordItem.crystal(stack)),"Loaded spell changed or became invalid");
                double damage=stack.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers().stream().filter(m->m.attribute().is(Attributes.ATTACK_DAMAGE)).mapToDouble(m->m.modifier().amount()).sum();
                h.assertTrue(damage==7+2*level,"Inherited source defaults overwrote upgraded damage");
                h.assertTrue(ItemStack.matches(c.getItem(6),original),"Recipe mutated source");
                var nbt=stack.save(h.getLevel().registryAccess());var loaded=ItemStack.parseOptional(h.getLevel().registryAccess(),(net.minecraft.nbt.CompoundTag)nbt);
                h.assertTrue(ItemStack.matches(stack,loaded),"Stage/components lost on save");
            }
            h.assertTrue(!RitualSwordUpgrades.valid(stack,stack),"Maximum stage can upgrade again");
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void focusHealingUsesStageWithoutChangingManaCost(GameTestHelper h){
        for(var kind:RitualSwordItem.Kind.values())for(int level=1;level<=4;level++){
            var stack=weapon(kind,level);var p=player(h,stack);p.startUsingItem(InteractionHand.MAIN_HAND);SwordFocus.begin(p,stack,kind);
            for(int tick=1;tick<=60;tick++){
                SwordFocus.advance(p,stack,tick);
                if(tick==20)h.assertTrue(p.getHealth()==(kind==RitualSwordItem.Kind.NAIL?5+level:1),"Wrong partial healing");
            }
            h.assertTrue(p.getHealth()==13+3*level && Math.abs(p.getData(ModAttachments.MANA).current()-90)<1e-6,"Healing or mana cost mismatch");
            SwordFocus.stop(p);
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void specialRangeAndProjectileStageAreCaptured(GameTestHelper h){
        for(int level=0;level<=4;level++){
            var p=player(h,weapon(RitualSwordItem.Kind.NEEDLE,level));
            var needle=ModEntityTypes.FLYING_NEEDLE.get().create(h.getLevel());needle.launch(p,p.getMainHandItem());
            h.assertTrue(needle.flightRange()==10*(1+.25*level) && needle.getItem().is(p.getMainHandItem().getItem()),"Flying range/model stage mismatch");
            var wide=com.mcmagic.omnira.item.staff.StaffAssembly.basic().withUpgrades(List.of(new ItemStack(ModItems.WIDE_AREA_RUNE.get()))).orElseThrow();p.setItemInHand(InteractionHand.OFF_HAND,wide.create());
            var wideNeedle=ModEntityTypes.FLYING_NEEDLE.get().create(h.getLevel());wideNeedle.launch(p,p.getMainHandItem());
            h.assertTrue(wideNeedle.flightRange()==10*(1.5+.25*level),"Wide range must add rather than multiply");
            p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            h.assertTrue(wideNeedle.flightRange()==10*(1.5+.25*level),"Launched range changed");
            p.setItemInHand(InteractionHand.MAIN_HAND,weapon(RitualSwordItem.Kind.NAIL,level));
            double base=p.entityInteractionRange();h.assertTrue(Math.abs(NailSlash.reach(p)-base*2*(1+.25*level))<1e-6,"Nail range wrong");
            p.setItemInHand(InteractionHand.OFF_HAND,wide.create());
            h.assertTrue(Math.abs(NailSlash.reach(p)-base*2*(1.5+.25*level))<1e-6,"Nail rune range multiplied");
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void realForgeConsumesOneSetAndReturnsBucket(GameTestHelper h){
        var center=h.absolutePos(new BlockPos(5,2,5));h.getLevel().setBlockAndUpdate(center,ModBlocks.ADVANCED_FORGE.get().defaultBlockState());
        var forge=(AdvancedForgeBlockEntity)h.getLevel().getBlockEntity(center);var recipe=recipe(h,RitualSwordItem.Kind.NAIL,3);var input=input(recipe);
        input.getItem(6).setDamageValue(222);
        for(int i=0;i<9;i++)forge.setItem(i,input.getItem(i));forge.setItem(9,new ItemStack(ModItems.TEST_SPELL_CORE.get()));
        var p=AdvancedForgeGameTests.player(h,center);p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        h.assertTrue(forge.start(p),"Upgrade machine did not start");
        for(int i=0;i<1200;i++)forge.tick();
        h.assertTrue(forge.output().is(RitualSwordUpgrades.item(RitualSwordItem.Kind.NAIL,3)) && forge.output().getDamageValue()==222,"Wrong forged output");
        for(int i=0;i<9;i++)h.assertTrue(i==7?forge.getItem(i).is(Items.BUCKET):forge.getItem(i).isEmpty(),"Incorrect consumption/remainder at "+i);
        h.assertTrue(forge.getItem(9).is(ModItems.TEST_SPELL_CORE.get()),"Consumed drive core");h.succeed();
    }
}
