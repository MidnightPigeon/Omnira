package com.mcmagic.omnira.gametest;
import com.mcmagic.omnira.item.*;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.mana.ManaState;
import com.mcmagic.omnira.spell.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import java.util.*;
@GameTestHolder("omnira_industrial_wheel") @PrefixGameTestTemplate(false)
public class IndustrialWheelGameTests {
    private static FakePlayer caster(GameTestHelper h){
        var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"wheel-test"));
        p.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(5,3,5)));p.setXRot(-90);p.setData(ModAttachments.MANA,ManaState.initial());
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.INDUSTRIAL_WHEEL.get()));
        var crystal=new ItemStack(ModItems.LOW_TIER_MAGIC_CRYSTAL.get());crystal.set(ModDataComponents.SPELL_PATTERN,new SpellPattern(ElementType.AIR,ElementType.AIR));
        var grid=new ItemStack(ModItems.BASIC_CRYSTAL_GRID.get());grid.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(crystal)));
        top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(p).orElseThrow().setEquippedCurio("crystal_grid",0,grid);return p;
    }
    private static void ticks(FakePlayer p,int n){for(int i=0;i<n;i++){p.tickCount++;p.getCooldowns().tick();IndustrialWheelItem.tick(new PlayerTickEvent.Post(p));}}
    @GameTest(template="spell_arena") public static void deployOneRune(GameTestHelper h){
        if(!IndustrialWheelItem.available()){h.succeed();return;}
        var wheel=new ItemStack(ModItems.INDUSTRIAL_WHEEL.get());
        wheel.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Workshop wheel"));
        var pos=h.absolutePos(new net.minecraft.core.BlockPos(3,3,3));
        h.getLevel().setBlockAndUpdate(pos,com.simibubi.create.AllBlocks.DEPLOYER.getDefaultState());
        var deployer=(com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity)h.getLevel().getBlockEntity(pos);
        var inputs=new net.neoforged.neoforge.items.ItemStackHandler(2);
        inputs.setStackInSlot(0,wheel);
        for(var rune:List.of(ModItems.SWIFTNESS_RUNE,ModItems.WIDE_AREA_RUNE,ModItems.AMPLIFICATION_RUNE)){
            inputs.setStackInSlot(1,new ItemStack(rune.get()));
            var event=new com.simibubi.create.content.kinetics.deployer.DeployerRecipeSearchEvent(deployer,new net.neoforged.neoforge.items.wrapper.RecipeWrapper(inputs));
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
            var recipe=(com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe)event.getRecipe().value();
            var result=recipe.rollResults(h.getLevel().random).getFirst();
            var stats=IndustrialWheelItem.stats(result);
            h.assertTrue(!recipe.shouldKeepHeldItem() && result.getHoverName().equals(wheel.getHoverName()) && IndustrialWheelItem.rune(result).is(rune.get()),"Rune or wheel data lost");
            h.assertTrue(com.mcmagic.omnira.compat.WheelRuneDeploying.recipe(result,inputs.getStackInSlot(1)).isEmpty(),"Occupied slot accepted another rune");
            h.assertTrue(switch(rune.getId().getPath()){
                case "swiftness_rune" -> stats.speedMultiplier()==1.5 && stats.cooldownReduction()==.2;
                case "wide_area_rune" -> stats.rangeMultiplier()==1.5;
                default -> Math.abs(stats.power()-.7)<.00001;
            },"Rune did not affect casting stats");
        }
        h.assertTrue(com.mcmagic.omnira.compat.WheelRuneDeploying.recipe(wheel,new ItemStack(Items.DIAMOND)).isEmpty(),"Non-rune accepted");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void continuousWrapAndImmediateStop(GameTestHelper h){
        if(!IndustrialWheelItem.available()){h.succeed();return;}
        var p=caster(h);var s=p.getMainHandItem();IndustrialWheelItem.toggle(p,InteractionHand.MAIN_HAND);
        h.assertTrue(IndustrialWheelItem.active(p)&&s.getOrDefault(ModDataComponents.WHEEL_STEP,0)==1,"First cast did not start");
        h.assertTrue(s.has(ModDataComponents.WHEEL_SPIN_START),"Continuous spin not started");
        double now=p.level().getGameTime();h.assertTrue(Math.abs(IndustrialWheelItem.rotation(s,now+20)-IndustrialWheelItem.rotation(s,now)-30)<.001,"Spin must advance between casts");
        ticks(p,39);h.assertTrue(s.getOrDefault(ModDataComponents.WHEEL_STEP,0)==1,"Cast before cooldown");
        ticks(p,1);h.assertTrue(s.getOrDefault(ModDataComponents.WHEEL_STEP,0)==2,"Single-crystal chain incorrectly used chain-end cooldown");
        h.assertTrue(Math.abs(p.getData(ModAttachments.MANA).current()-80)<.001,"Incorrect per-cast discount or duplicate charge");
        IndustrialWheelItem.toggle(p,InteractionHand.MAIN_HAND);h.assertTrue(!IndustrialWheelItem.active(p),"Cooldown prevented stop");
        h.assertTrue(!s.has(ModDataComponents.WHEEL_SPIN_START)&&IndustrialWheelItem.rotation(s,now)==IndustrialWheelItem.rotation(s,now+80),"Stopped wheel kept spinning");
        ticks(p,80);h.assertTrue(s.getOrDefault(ModDataComponents.WHEEL_STEP,0)==2,"Casting continued after stop");h.succeed();
    }
    @GameTest(template="spell_arena") public static void swapAndExhaustionStop(GameTestHelper h){
        if(!IndustrialWheelItem.available()){h.succeed();return;}
        var p=caster(h);var s=p.getMainHandItem();IndustrialWheelItem.toggle(p,InteractionHand.MAIN_HAND);
        p.setItemInHand(InteractionHand.MAIN_HAND,s.copy());ticks(p,1);h.assertTrue(!IndustrialWheelItem.active(p),"Replacement inherited session");
        ticks(p,40);p.setData(ModAttachments.MANA,new ManaState(10,100));IndustrialWheelItem.toggle(p,InteractionHand.MAIN_HAND);
        ticks(p,40);h.assertTrue(!IndustrialWheelItem.active(p)&&p.getData(ModAttachments.MANA).current()==0,"Mana exhaustion did not stop");
        p.setData(ModAttachments.MANA,ManaState.initial());ticks(p,40);h.assertTrue(!IndustrialWheelItem.active(p),"Mana regeneration restarted session");h.succeed();
    }
    @GameTest(template="spell_arena") public static void rotationRecoversOldWorldDeadline(GameTestHelper h){
        if(!IndustrialWheelItem.available()){h.succeed();return;}
        var p=caster(h);long now=p.server.overworld().getGameTime();
        var grid=com.mcmagic.omnira.menu.CrystalGridMenu.locate(p,-2);
        grid.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(new ItemStack(ModItems.WORLD_ROTATION_CRYSTAL.get()))));
        p.setData(ModAttachments.WORLD_ROTATION_READY,now+800000L);
        IndustrialWheelItem.toggle(p,InteractionHand.MAIN_HAND);
        h.assertTrue(IndustrialWheelItem.active(p)&&p.getData(ModAttachments.MANA).current()==0,"Wheel could not cast after world clock reset");
        h.assertTrue(p.getData(ModAttachments.WORLD_ROTATION_READY)==now+1200,"Wheel changed fixed cooldown");
        IndustrialWheelItem.stop(p);com.mcmagic.omnira.spell.world.WorldRotation.cancel(p.getUUID());h.succeed();
    }
    @GameTest(template="spell_arena") public static void propertiesAndConditionalRecipe(GameTestHelper h){
        var stack=new ItemStack(ModItems.INDUSTRIAL_WHEEL.get());h.assertTrue(stack.getRarity()==Rarity.UNCOMMON&&stack.getMaxStackSize()==1,"Yellow rarity/stack size");
        var attrs=stack.getAttributeModifiers().modifiers();
        for(var attr:List.of(net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_INTERACTION_RANGE,net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE))
            h.assertTrue(attrs.stream().anyMatch(a->a.attribute().equals(attr)&&a.modifier().amount()==2),"Missing native reach attribute");
        var recipe=h.getLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.parse("omnira:industrial_wheel"));
        h.assertTrue(recipe.isPresent()==IndustrialWheelItem.available(),"Create recipe condition failed");
        if(recipe.isPresent())h.assertTrue(recipe.get().value().getResultItem(h.getLevel().registryAccess()).is(ModItems.INDUSTRIAL_WHEEL.get()),"Wrong sequence result");
        h.succeed();
    }
}
