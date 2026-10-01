package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.item.staff.*;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.spell.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;
import java.util.List;

@GameTestHolder("omnira_staff_enhancement")
@PrefixGameTestTemplate(false)
public final class StaffEnhancementGameTests {
    @GameTest(template="spell_arena")
    public static void actualCastsUseOnlyTheirOwnStaff(GameTestHelper h) {
        var player=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"staff-modifier-test"));
        player.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(5,3,5)));player.setXRot(-90);
        player.setData(ModAttachments.MANA,com.mcmagic.omnira.mana.ManaState.initial());
        var crystal=new ItemStack(ModItems.LOW_TIER_MAGIC_CRYSTAL.get());
        crystal.set(ModDataComponents.SPELL_PATTERN,new SpellPattern(ElementType.AIR,ElementType.AIR));
        crystal.set(ModDataComponents.SPELL_PAYLOAD,new SpellPayload(20,0,List.of(new SpellEffect(false,1,0))));
        var original=crystal.copy();
        var grid=new ItemStack(ModItems.BASIC_CRYSTAL_GRID.get());
        grid.set(DataComponents.CONTAINER,net.minecraft.world.item.component.ItemContainerContents.fromItems(List.of(crystal)));
        top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).orElseThrow().setEquippedCurio("crystal_grid",0,grid);
        var plain=StaffAssembly.basic().create();
        var holy=StaffEnhancements.assemble(input(plain,ModItems.LIGHT_MICROCORE.get()));
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,holy);
        h.assertTrue(com.mcmagic.omnira.item.StaffItem.castSequence(player,holy,1)==1,"Enhanced staff did not cast");
        var shots=h.getLevel().getEntitiesOfClass(com.mcmagic.omnira.spell.entity.SpellEntity.class,player.getBoundingBox().inflate(4),e->e.ownedBy(player));
        h.assertTrue(shots.size()==1 && shots.getFirst().holy(),"Holy modifier absent from actual projectile");
        shots.forEach(com.mcmagic.omnira.spell.entity.SpellEntity::discard);
        player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,plain);
        h.assertTrue(com.mcmagic.omnira.item.StaffItem.castSequence(player,plain,1)==1,"Plain staff did not cast");
        shots=h.getLevel().getEntitiesOfClass(com.mcmagic.omnira.spell.entity.SpellEntity.class,player.getBoundingBox().inflate(4),e->e.ownedBy(player));
        h.assertTrue(shots.size()==1 && !shots.getFirst().holy(),"Offhand borrowed main-hand enhancement");
        h.assertTrue(ItemStack.matches(original,grid.get(DataComponents.CONTAINER).getStackInSlot(0)),"Grid crystal was rewritten");
        h.assertTrue(player.getData(ModAttachments.MANA).current()==60,"Permanent modifier changed mana charge");
        shots.forEach(com.mcmagic.omnira.spell.entity.SpellEntity::discard);h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void timeLootMigrationPreservesIndependentPools(GameTestHelper h)throws Exception {
        var config=com.mcmagic.omnira.config.OmniraLootConfig.defaults();
        var tables=config.getAsJsonObject("treasureTables");
        for(var entry:tables.entrySet()) {
            if(entry.getKey().startsWith("omnira:memory/"))continue;
            h.assertTrue(!entry.getValue().toString().contains("\"omnira:space_microcore\"")
                    && !entry.getValue().toString().contains("\"omnira:spatial_crystal_shard\""),"Space ingredient remains in time treasure");
        }
        h.assertTrue(tables.get("omnira:memory/space").toString().contains("space_microcore"),"Independent memory pool lost its microcore");
        try(var stream=StaffEnhancementGameTests.class.getResourceAsStream("/loot_migrations/time_only_v1.json")) {
            var previous=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(java.util.Objects.requireNonNull(stream),
                    java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            var custom=previous.deepCopy();var id="omnira:chests/golden_throne";
            custom.getAsJsonObject(id).addProperty("random_sequence","omnira:custom");
            var saved=custom.get(id).deepCopy();
            h.assertTrue(com.mcmagic.omnira.config.OmniraLootConfig.migrateDefaultTimeOnly(previous),"Time migration missing");
            h.assertTrue(!com.mcmagic.omnira.config.OmniraLootConfig.migrateDefaultTimeOnly(previous),"Migration repeated");
            for(var e:previous.entrySet())h.assertTrue(e.getValue().equals(tables.get(e.getKey())),"Migration differs from bundled defaults");
            com.mcmagic.omnira.config.OmniraLootConfig.migrateDefaultTimeOnly(custom);
            h.assertTrue(saved.equals(custom.get(id)),"Custom table overwritten");
        }
        h.succeed();
    }
    private static SimpleContainer input(ItemStack staff,Item material) {
        var input=new SimpleContainer(9);
        for(int i=0;i<9;i++)input.setItem(i,i==7?staff:new ItemStack(material));
        return input;
    }
    @GameTest(template="spell_arena")
    public static void recipesPreserveComponentsAndCapacity(GameTestHelper h) {
        var assembly=new StaffAssembly(new ItemStack(ModItems.SHADOW_STAFF_SHAFT.get()),StaffAssembly.basic().reinforcement(),
                StaffAssembly.basic().tip(),List.of(new ItemStack(ModItems.AMPLIFICATION_RUNE.get())));
        var staff=assembly.create();staff.set(DataComponents.CUSTOM_NAME,Component.literal("Preserved"));
        for(var material:List.of(Items.IRON_BLOCK,Items.CLOCK,ModItems.LIGHT_MICROCORE.get(),ModItems.SPIRITUAL_CRYSTAL.get())) {
            var inputs=input(staff,material);
            var recipe=h.getLevel().getRecipeManager().getRecipeFor(ModRecipes.ADVANCED_FORGE_TYPE.get(),
                    new com.mcmagic.omnira.forging.AdvancedForgeRecipe.Input(inputs),h.getLevel()).orElseThrow();
            var result=recipe.value().assemble(new com.mcmagic.omnira.forging.AdvancedForgeRecipe.Input(inputs),h.getLevel().registryAccess());
            h.assertTrue(result.getHoverName().getString().equals("Preserved"),"Custom components lost");
            h.assertTrue(StaffAssembly.of(result).usedSlots(StaffPart.SlotKind.RUNE)==1,"Rune lost");
            h.assertTrue(StaffEnhancements.keywords(result).equals(List.of(StaffEnhancements.keyword(new ItemStack(material)))),"Wrong modifier");
            var saved=ItemStack.CODEC.encodeStart(h.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE),result).getOrThrow();
            var loaded=ItemStack.CODEC.parse(h.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE),saved).getOrThrow();
            h.assertTrue(ItemStack.matches(result,loaded),"Enhancement did not persist");
            var second=StaffEnhancements.assemble(input(result,material));
            h.assertTrue(!second.isEmpty() && StaffEnhancements.keywords(second).size()==2,"Second shaft slot rejected");
            h.assertTrue(StaffEnhancements.assemble(input(second,material)).isEmpty(),"Exceeded shaft capacity");
            inputs.setItem(8,new ItemStack(Items.DIRT));
            h.assertTrue(StaffEnhancements.assemble(inputs).isEmpty(),"Mixed ingredients accepted");
            inputs.setItem(8,ItemStack.EMPTY);
            h.assertTrue(StaffEnhancements.assemble(inputs).isEmpty(),"Seven ingredients accepted");
        }
        h.assertTrue(StaffAssembly.of(staff).usedSlots(StaffPart.SlotKind.ENHANCEMENT)==0,"Input mutated");
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void modifiersMatchWritingAndDoNotMutateCrystal(GameTestHelper h) {
        var original=new SpellPayload(40,0,List.of(new SpellEffect(false,1,0),new SpellEffect(true,1,0),
                SpellEffect.utility("dissociation",0,0,0),SpellEffect.utility("construction",0,0,0),
                SpellEffect.utility("dark_breath",0,0,0)),List.of("harm"));
        var iron=StaffEnhancements.apply(original,List.of("enhancement"));
        h.assertTrue(iron.effects().getFirst().physicalDamage(1)==9 && iron.effects().get(1).amplifier()==2,"Iron mismatch");
        h.assertTrue(iron.effects().get(2).enhancement()==1 && iron.effects().get(4).enhancement()==1,"Utility/darkness mismatch");
        var delayed=StaffEnhancements.apply(original,List.of("delay","enhancement"));
        h.assertTrue(delayed.effects().getFirst().duration()==800 && delayed.effects().getFirst().amplifier()==1,"Delay order mismatch");
        var twice=StaffEnhancements.apply(StaffEnhancements.apply(original,List.of("delay")),List.of("delay"));
        h.assertTrue(twice.effects().getFirst().duration()==2000 && twice.effects().get(2).delay()==2,"Repeated delay mismatch");
        var infusion=StaffEnhancements.apply(original,List.of("infusion","holy"));
        h.assertTrue(infusion.effects().stream().allMatch(SpellEffect::infused) && infusion.keywords().contains("holy"),"Infusion/holy missing");
        h.assertTrue(original.effects().getFirst().amplifier()==1 && original.effects().getFirst().duration()==0
                && original.keywords().equals(List.of("harm")),"Stored spell changed");
        h.assertTrue(delayed.baseCost()==40 && infusion.baseCost()==40,"Permanent enhancement charged per cast");
        h.succeed();
    }
}
