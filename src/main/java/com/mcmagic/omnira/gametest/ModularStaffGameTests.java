package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.item.StaffItem;
import com.mcmagic.omnira.item.staff.StaffAssembly;
import com.mcmagic.omnira.item.staff.StaffEvents;
import com.mcmagic.omnira.item.staff.StaffPart;
import com.mcmagic.omnira.mana.ManaState;
import com.mcmagic.omnira.menu.CrystalGridMenu;
import com.mcmagic.omnira.recipe.AssemblyRecipe;
import com.mcmagic.omnira.recipe.StaffAssemblyRecipes;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.spell.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;
import java.util.UUID;

@GameTestHolder("omnira_modular_staff")
@PrefixGameTestTemplate(false)
public final class ModularStaffGameTests {
    @GameTest(template="spell_arena")
    public static void unrelatedCastsDoNotBorrowHeldRunes(GameTestHelper h) {
        var player=caster(h).player();
        var staff=new StaffAssembly(StaffAssembly.basic().shaft(),StaffAssembly.basic().reinforcement(),
                new ItemStack(ModItems.MULTI_ARCANE_TIP.get()),
                List.of(new ItemStack(ModItems.WIDE_AREA_RUNE.get()),new ItemStack(ModItems.SWIFTNESS_RUNE.get()))).create();
        player.setItemInHand(InteractionHand.MAIN_HAND,staff);
        h.assertTrue(SpellCasting.cast(player,new SpellPattern(ElementType.AIR,ElementType.AIR)),"Plain cast failed");
        var shots=h.getLevel().getEntitiesOfClass(com.mcmagic.omnira.spell.entity.SpellEntity.class,
                player.getBoundingBox().inflate(3),e->e.ownedBy(player));
        h.assertTrue(shots.size()==1 && shots.getFirst().speedMultiplier()==1 && shots.getFirst().rangeMultiplier()==1,
                "Independent spell borrowed the held staff's runes");
        shots.forEach(com.mcmagic.omnira.spell.entity.SpellEntity::discard);
        var context=com.mcmagic.omnira.item.staff.StaffCastContext.capture(player,staff);
        player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        SpellCasting.cast(player,new SpellPattern(ElementType.AIR,ElementType.AIR),SpellPayload.EMPTY,context.power(),
                new SpellCasting.Modifiers(context.stats().speedMultiplier(),context.stats().rangeMultiplier()));
        shots=h.getLevel().getEntitiesOfClass(com.mcmagic.omnira.spell.entity.SpellEntity.class,
                player.getBoundingBox().inflate(3),e->e.ownedBy(player));
        h.assertTrue(shots.size()==1 && shots.getFirst().speedMultiplier()==1.5 && shots.getFirst().rangeMultiplier()==1.5,
                "Explicit cast lost its source modifiers");
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void implementBonusesReplaceBeforeClamp(GameTestHelper h) {
        var player=caster(h).player();
        var cooldown=player.getAttribute(ModAttributes.COOLDOWN_REDUCTION);
        cooldown.setBaseValue(.4);
        cooldown.addTransientModifier(new AttributeModifier(CastAttributes.STAFF_COOLDOWN,.2,AttributeModifier.Operation.ADD_VALUE));
        h.assertTrue(cooldown.getValue()==.5,"Live cooldown cap missing");
        h.assertTrue(Math.abs(CastAttributes.cooldownReduction(player,0)-.4)<1e-6,"Subtracted from a clamped value");
        var power=player.getAttribute(ModAttributes.SPELL_POWER);
        power.setBaseValue(2);
        power.addTransientModifier(new AttributeModifier(CastAttributes.STAFF_POWER,.5,AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        power.addTransientModifier(new AttributeModifier(CastAttributes.ARQUEBUS_POWER,.3,AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        h.assertTrue(Math.abs(CastAttributes.power(player,.3)-2.6)<1e-6,"Gun inherited an old implement bonus");
        h.assertTrue(com.mcmagic.omnira.mana.ManaCosts.cost(30,2,5)==55,"Mana reduction applied before multiplier");
        h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=100)
    public static void offhandBurstDuringMainHandUse(GameTestHelper h) {
        var caster=caster(h);var player=caster.player();
        player.setItemInHand(InteractionHand.OFF_HAND,caster.staff());
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BOW));
        var main=player.getMainHandItem();
        ((StaffItem)caster.staff().getItem()).castHeld(player,caster.staff());
        state(h,caster,80,1);
        player.startUsingItem(InteractionHand.MAIN_HAND);
        for(int t=1;t<=65;t++) {
            final int elapsed=t;
            h.runAfterDelay(t,()->{
                tick(caster);
                // Vanilla keeps attempting use while held; it must never start another offhand cast.
                ((StaffItem)caster.staff().getItem()).use(h.getLevel(),player,InteractionHand.OFF_HAND);
                state(h,caster,elapsed<6?80:60,elapsed<6?1:2);
                h.assertTrue(player.getMainHandItem()==main && player.isUsingItem(),"Offhand cast interrupted or swapped the main hand");
                if(elapsed==65){player.stopUsingItem();h.succeed();}
            });
        }
    }
    @GameTest(template="spell_arena")
    public static void offhandAttributeSnapshotIsIsolated(GameTestHelper h) {
        var player=caster(h).player();
        var upgraded=StaffAssembly.basic().withUpgrades(List.of(new ItemStack(ModItems.AMPLIFICATION_RUNE.get()))).orElseThrow().create();
        var attribute=player.getAttribute(ModAttributes.SPELL_POWER);
        attribute.setBaseValue(2);
        attribute.addTransientModifier(new AttributeModifier(CastAttributes.STAFF_POWER,.8,AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        attribute.addTransientModifier(new AttributeModifier(ResourceLocation.fromNamespaceAndPath("omnira","test_equipment"),.2,AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        double before=attribute.getValue();
        var snapshot=com.mcmagic.omnira.item.staff.StaffCastContext.capture(player,upgraded);
        h.assertTrue(Math.abs(snapshot.power()-3.4)<1e-6,"Staff or external power was duplicated/multiplied");
        h.assertTrue(attribute.getValue()==before,"Capturing a cast changed live attributes");
        player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        attribute.setBaseValue(1);
        h.assertTrue(Math.abs(snapshot.power()-3.4)<1e-6,"Existing snapshot changed with the player");
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void cooldownAttributeAddsAndPreservesBurst(GameTestHelper h) {
        var assembly=StaffAssembly.basic().withUpgrades(List.of(new ItemStack(ModItems.SWIFTNESS_RUNE.get()))).orElseThrow();
        var attribute=new net.minecraft.world.entity.ai.attributes.AttributeInstance(ModAttributes.COOLDOWN_REDUCTION,ignored->{});
        for(var entry:assembly.create().getAttributeModifiers().modifiers()) if(entry.attribute().equals(ModAttributes.COOLDOWN_REDUCTION)) {
            h.assertTrue(entry.modifier().operation()==AttributeModifier.Operation.ADD_VALUE,"Cooldown reduction must add");
            h.assertTrue(entry.slot()==EquipmentSlotGroup.MAINHAND,"Cooldown bonus must require main hand");
            attribute.addTransientModifier(entry.modifier());
        }
        h.assertTrue(Math.abs(attribute.getValue()-.2)<1e-6,"Rune attribute missing");
        attribute.addTransientModifier(new AttributeModifier(ResourceLocation.fromNamespaceAndPath("omnira","test_cooldown"),.1,AttributeModifier.Operation.ADD_VALUE));
        h.assertTrue(SpellCooldowns.ticks(60,attribute.getValue(),1)==42,"Cooldown bonuses did not add");
        h.assertTrue(SpellCooldowns.ticks(60,-.5,1)==90,"Cooldown penalty ignored");
        attribute.addTransientModifier(new AttributeModifier(ResourceLocation.fromNamespaceAndPath("omnira","test_cooldown_cap"),1,AttributeModifier.Operation.ADD_VALUE));
        h.assertTrue(attribute.getValue()==.5,"Combined attribute exceeds fifty percent");
        h.assertTrue(SpellCooldowns.ticks(60,1,1)==30 && SpellCooldowns.ticks(60,1,2)==30,"Reduction exceeds fifty percent");
        h.assertTrue(SpellCooldowns.ticks(1,0,1)==20 && SpellCooldowns.ticks(1,1,1)==10,"Base cooldown below one second");
        h.assertTrue(SpellCooldowns.ticks(20,.5,4)==19,"Cooldown overlaps burst");
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void amplificationUsesAdditiveBasePower(GameTestHelper h) {
        var rune=new ItemStack(ModItems.AMPLIFICATION_RUNE.get());
        var assembly=new StaffAssembly(new ItemStack(ModItems.SHADOW_STAFF_SHAFT.get()),
                new ItemStack(ModItems.LIGHT_CORE_REINFORCEMENT.get()),new ItemStack(ModItems.MULTI_ARCANE_TIP.get()),List.of(rune));
        h.assertTrue(assembly.valid() && Math.abs(assembly.stats().power()-.7)<1e-6,"Part bonuses multiplied instead of adding");
        h.assertTrue(assembly.withUpgrades(List.of(rune)).isEmpty(),"Duplicate amplification rune accepted");
        h.assertTrue(StaffAssembly.of(roundTrip(h,assembly.create())).equals(assembly),"Amplification data did not persist");
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var attribute=new net.minecraft.world.entity.ai.attributes.AttributeInstance(ModAttributes.SPELL_POWER,ignored->{});
        attribute.setBaseValue(2);
        double sum=0;
        for(var entry:assembly.create().getAttributeModifiers().modifiers()) if(entry.attribute().equals(ModAttributes.SPELL_POWER)) {
            h.assertTrue(entry.modifier().operation()==AttributeModifier.Operation.ADD_MULTIPLIED_BASE,"Staff power is multiplicative");
            sum+=entry.modifier().amount();attribute.addTransientModifier(entry.modifier());
        }
        var core=new ItemStack(ModItems.TEST_SPELL_CORE.get());
        var grid=new ItemStack(ModItems.ARCANE_CRYSTAL_GRID.get());
        var coreModifiers=((com.mcmagic.omnira.item.SpellCoreItem)core.getItem()).getAttributeModifiers(
                new top.theillusivec4.curios.api.SlotContext("spell_core",player,0,false,true),ResourceLocation.fromNamespaceAndPath("omnira","audit_core"),core);
        var gridModifiers=((com.mcmagic.omnira.item.CrystalGridItem)grid.getItem()).getAttributeModifiers(
                new top.theillusivec4.curios.api.SlotContext("crystal_grid",player,0,false,true),ResourceLocation.fromNamespaceAndPath("omnira","audit_grid"),grid);
        for(var modifiers:List.of(coreModifiers,gridModifiers)) for(var modifier:modifiers.get(ModAttributes.SPELL_POWER)) {
            h.assertTrue(modifier.operation()==AttributeModifier.Operation.ADD_MULTIPLIED_BASE,"Curio power is multiplicative");
            sum+=modifier.amount();attribute.addTransientModifier(modifier);
        }
        h.assertTrue(Math.abs(attribute.getValue()-2*(1+sum))<1e-6,"Power does not sum against base");
        var input=new SimpleContainer(7);input.setItem(6,StaffAssembly.basic().create());input.setItem(2,rune);
        var installed=StaffAssemblyRecipes.assemble(new AssemblyRecipe.Input(input),"staff_upgrade");
        h.assertTrue(!installed.isEmpty() && StaffAssembly.of(installed).stats().power()==.5,"Amplification installation failed");
        h.succeed();
    }
    @GameTest(template = "spell_arena")
    public static void swiftnessRuneInstallationAndSpeed(GameTestHelper h) {
        var rune = new ItemStack(ModItems.SWIFTNESS_RUNE.get());
        var upgraded = StaffAssembly.basic().withUpgrades(List.of(rune)).orElseThrow();
        h.assertTrue(upgraded.usedSlots(StaffPart.SlotKind.RUNE)==1 && upgraded.usedSlots(StaffPart.SlotKind.ENHANCEMENT)==0,"Rune used wrong slot");
        h.assertTrue(upgraded.stats().cooldownTicks()==60 && upgraded.stats().cooldownReduction()==.2 && Math.abs(upgraded.stats().speedMultiplier()-1.5)<1e-6,"Incorrect speed/cooldown bonus");
        h.assertTrue(upgraded.withUpgrades(List.of(rune)).isEmpty(),"Duplicate rune accepted");
        h.assertTrue(StaffAssembly.of(roundTrip(h,upgraded.create())).equals(upgraded),"Rune did not persist");
        var fusion = new StaffAssembly(upgraded.shaft(),upgraded.reinforcement(),new ItemStack(ModItems.ELEMENTAL_FUSION_TIP.get()),List.of(rune));
        h.assertTrue(com.mcmagic.omnira.spell.SpellCooldowns.ticks(fusion.stats().cooldownTicks(),fusion.stats().cooldownReduction(),1)==16,"Fusion cooldown not reduced by twenty percent");
        var multi = new StaffAssembly(upgraded.shaft(),upgraded.reinforcement(),new ItemStack(ModItems.MULTI_ARCANE_TIP.get()),List.of(rune));
        h.assertTrue(com.mcmagic.omnira.spell.SpellCooldowns.ticks(multi.stats().cooldownTicks(),multi.stats().cooldownReduction(),2)==48 && multi.stats().shots()==2 && StaffEvents.SHOT_INTERVAL==6,"Multi-shot timing changed");
        var input = new SimpleContainer(7);
        input.setItem(6,StaffAssembly.basic().create());input.setItem(4,rune);
        var loaded = h.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("omnira","assembly/staff_upgrade")).orElseThrow();
        var recipe = (AssemblyRecipe)loaded.value();
        h.assertTrue(recipe.matches(new AssemblyRecipe.Input(input),h.getLevel()),"Registered upgrade recipe missing/mismatched");
        h.assertTrue(StaffAssembly.of(recipe.assemble(new AssemblyRecipe.Input(input),h.getLevel().registryAccess())).equals(upgraded),"Recipe discarded rune");
        var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setPos(h.absoluteVec(new Vec3(8,4,8)));player.setXRot(-90);
        player.setItemInHand(InteractionHand.MAIN_HAND,upgraded.create());
        SpellCasting.cast(player,new SpellPattern(ElementType.AIR,ElementType.AIR),SpellPayload.EMPTY,1,
                new SpellCasting.Modifiers(upgraded.stats().speedMultiplier(),upgraded.stats().rangeMultiplier()));
        var spell = h.getLevel().getEntitiesOfClass(com.mcmagic.omnira.spell.entity.SpellEntity.class,player.getBoundingBox().inflate(2),e->e.ownedBy(player)).getFirst();
        h.assertTrue(Math.abs(spell.getDeltaMovement().length()-.45)<1e-6,"Projectile speed not boosted");
        player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        spell.tick();
        h.assertTrue(Math.abs(spell.speedMultiplier()-1.5)<1e-6,"Changing hand changed existing projectile");
        spell.discard();h.succeed();
    }
    @GameTest(template = "spell_arena")
    public static void typedUpgradeSlots(GameTestHelper h) {
        h.assertTrue(StaffPart.of(new ItemStack(ModItems.WOODEN_STAFF_SHAFT.get())).enhancementSlots()==1
                && StaffPart.of(new ItemStack(ModItems.BLAZE_STAFF_SHAFT.get())).enhancementSlots()==1
                && StaffPart.of(new ItemStack(ModItems.SHADOW_STAFF_SHAFT.get())).enhancementSlots()==2,"Shaft capacities incorrect");
        h.assertTrue(StaffPart.of(new ItemStack(ModItems.SPIRITUAL_CRYSTAL_TIP.get())).runeSlots()==1
                && StaffPart.of(new ItemStack(ModItems.ELEMENTAL_FUSION_TIP.get())).runeSlots()==2
                && StaffPart.of(new ItemStack(ModItems.MULTI_ARCANE_TIP.get())).runeSlots()==2,"Tip capacities incorrect");
        var enhancement=new ItemStack(Items.PAPER);
        var rune=new ItemStack(Items.STICK);
        enhancement.set(ModDataComponents.STAFF_PART,new StaffPart(StaffPart.Role.UPGRADE,0,0,0,0,
                StaffPart.EMPTY_MODEL,3,64,64,StaffPart.SlotKind.ENHANCEMENT));
        rune.set(ModDataComponents.STAFF_PART,new StaffPart(StaffPart.Role.UPGRADE,0,0,0,0,
                StaffPart.EMPTY_MODEL,3,0,0,StaffPart.SlotKind.RUNE));
        var large=new StaffAssembly(new ItemStack(ModItems.SHADOW_STAFF_SHAFT.get()),
                new ItemStack(ModItems.IRON_REINFORCEMENT.get()),new ItemStack(ModItems.MULTI_ARCANE_TIP.get()),List.of());
        var secondRune=new ItemStack(Items.BONE);
        secondRune.set(ModDataComponents.STAFF_PART,StaffPart.of(rune));
        h.assertTrue(large.withUpgrades(List.of(rune,rune)).isEmpty(),"Rune maxCopies bypassed one-per-kind restriction");
        var full=large.withUpgrades(List.of(enhancement,rune,enhancement,secondRune)).orElseThrow();
        for(var kind:StaffPart.SlotKind.values()) {
            h.assertTrue(full.slotCapacity(kind)==2 && full.usedSlots(kind)==2,"Independent slots or owner capacity incorrect");
        }
        h.assertTrue(full.withUpgrades(List.of(enhancement)).isEmpty() && full.withUpgrades(List.of(rune)).isEmpty(),"Full slots accepted more plugins");
        h.assertTrue(StaffAssembly.basic().withUpgrades(List.of(enhancement,rune)).isPresent(),"Mixed plugin types conflict");
        h.assertTrue(StaffAssembly.basic().withUpgrades(List.of(rune,rune)).isEmpty(),"Runes borrowed unused enhancement capacity");
        h.assertTrue(StaffAssembly.basic().withUpgrades(List.of(enhancement,enhancement)).isEmpty(),"Enhancements borrowed unused rune capacity");
        var decoded=StaffAssembly.of(roundTrip(h,full.create()));
        h.assertTrue(decoded.equals(full) && decoded.usedSlots(StaffPart.SlotKind.RUNE)==2,"Installed slot data did not persist");
        var slots=new SimpleContainer(7);slots.setItem(6,full.create());slots.setItem(0,enhancement);
        rejects(h,recipe("staff_upgrade"),slots);
        h.assertTrue(StaffAssembly.of(slots.getItem(6)).equals(full) && !slots.getItem(0).isEmpty(),"Rejected upgrade mutated inputs");
        var ops=h.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        var overfull=new StaffAssembly(large.shaft(),large.reinforcement(),large.tip(),List.of(rune,rune,rune));
        h.assertTrue(StaffAssembly.CODEC.encodeStart(ops,overfull).error().isPresent(),"Overfilled assembly passed serialization validation");
        var old=StaffPart.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,
                StaffPart.of(large.shaft())).getOrThrow().getAsJsonObject();
        old.remove("enhancement_slots");old.remove("rune_slots");old.remove("upgrade_slot");
        h.assertTrue(StaffPart.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,old).getOrThrow().enhancementSlots()==1,"Legacy part cannot load");
        h.succeed();
    }
    private static AssemblyRecipe recipe(String operation) {
        return new AssemblyRecipe(StaffAssemblyRecipes.displaySlots(), StaffAssembly.basic().create(),
                false, operation);
    }

    private static SimpleContainer layout(ItemStack shaft, ItemStack reinforcement, ItemStack tip) {
        var slots = new SimpleContainer(7);
        slots.setItem(3, shaft.copy());
        slots.setItem(6, reinforcement.copy());
        slots.setItem(0, tip.copy());
        return slots;
    }

    private static ItemStack assemble(GameTestHelper h, AssemblyRecipe recipe, SimpleContainer slots) {
        var input = new AssemblyRecipe.Input(slots);
        h.assertTrue(recipe.matches(input, h.getLevel()), "Valid dynamic recipe did not match");
        var result = recipe.assemble(input, h.getLevel().registryAccess());
        h.assertTrue(result.is(ModItems.MODULAR_STAFF.get()), "Dynamic recipe did not create a staff");
        return result;
    }

    private static void rejects(GameTestHelper h, AssemblyRecipe recipe, SimpleContainer slots) {
        var input = new AssemblyRecipe.Input(slots);
        h.assertTrue(!recipe.matches(input, h.getLevel()), "Invalid layout matched " + recipe.operation());
        h.assertTrue(recipe.assemble(input, h.getLevel().registryAccess()).isEmpty(),
                "Invalid layout assembled " + recipe.operation());
    }

    private static void stats(GameTestHelper h, StaffAssembly assembly, List<ItemStack> parts) {
        double power = 0, reduction = 0;
        int shots = 0, cooldown = 0;
        for (var stack : parts) {
            var part = StaffPart.of(stack);
            power += part.power();
            reduction += part.reduction();
            shots += part.shots();
            cooldown += part.cooldownTicks();
        }
        var actual = assembly.stats();
        h.assertTrue(Math.abs(actual.power() - Math.max(-.99, power)) < .000001,
                "Component power sum differs");
        h.assertTrue(Math.abs(actual.reduction() - Math.max(0, reduction)) < .000001,
                "Component reduction sum differs");
        h.assertTrue(actual.shots() == Math.clamp(shots, 1, 64), "Component shot sum differs");
        int boundedShots = Math.clamp(shots, 1, 64);
        h.assertTrue(actual.cooldownTicks() == Math.clamp(cooldown, (boundedShots - 1) * 6 + 1, 72000),
                "Component cooldown sum differs");
    }

    @GameTest(template = "spell_arena")
    public static void currentPartsAssembleDynamically(GameTestHelper h) {
        var recipe = recipe("staff");
        int combinations = 0;
        for (var shaft : StaffAssemblyRecipes.parts(StaffPart.Role.SHAFT)) {
            for (var reinforcement : StaffAssemblyRecipes.parts(StaffPart.Role.REINFORCEMENT)) {
                for (var tip : StaffAssemblyRecipes.parts(StaffPart.Role.TIP)) {
                    var result = assemble(h, recipe, layout(shaft, reinforcement, tip));
                    var assembly = result.get(ModDataComponents.STAFF_ASSEMBLY);
                    h.assertTrue(assembly != null && assembly.valid(), "Missing or invalid assembly component");
                    h.assertTrue(ItemStack.matches(shaft, assembly.shaft())
                            && ItemStack.matches(reinforcement, assembly.reinforcement())
                            && ItemStack.matches(tip, assembly.tip()) && assembly.upgrades().isEmpty(),
                            "Assembly did not retain input parts");
                    stats(h, assembly, List.of(shaft, reinforcement, tip));
                    combinations++;
                }
            }
        }
        // This is a snapshot of today's registry, never a crafting limit or variant table.
        h.assertTrue(combinations == 36, "Current registered part combination count changed: " + combinations);
        var basic = StaffAssembly.basic();
        for (int slot : new int[]{0, 3, 6}) {
            var missing = layout(basic.shaft(), basic.reinforcement(), basic.tip());
            missing.setItem(slot, ItemStack.EMPTY);
            rejects(h, recipe, missing);
        }
        for (int slot : new int[]{1, 2, 4, 5}) {
            var extra = layout(basic.shaft(), basic.reinforcement(), basic.tip());
            extra.setItem(slot, new ItemStack(Items.STICK));
            rejects(h, recipe, extra);
        }
        rejects(h, recipe, layout(basic.tip(), basic.reinforcement(), basic.shaft()));
        rejects(h, recipe, layout(basic.shaft(), basic.tip(), basic.reinforcement()));
        h.succeed();
    }

    private static ItemStack roundTrip(GameTestHelper h, ItemStack stack) {
        var ops = h.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        var saved = ItemStack.CODEC.encodeStart(ops, stack).getOrThrow();
        var loaded = ItemStack.CODEC.parse(ops, saved).getOrThrow();
        h.assertTrue(ItemStack.matches(stack, loaded), "ItemStack components did not survive NBT round trip");
        return loaded;
    }

    @GameTest(template = "spell_arena")
    public static void customPartAndAssemblyRoundTrip(GameTestHelper h) {
        var shaft = new ItemStack(Items.STICK);
        var definition = new StaffPart(StaffPart.Role.SHAFT, .37, 7, 0, -4,
                ResourceLocation.fromNamespaceAndPath("omnira_modular_staff", "synthetic_shaft"), 1);
        shaft.set(ModDataComponents.STAFF_PART, definition);
        shaft.set(DataComponents.CUSTOM_NAME, Component.literal("External shaft"));
        var loadedShaft = roundTrip(h, shaft);
        h.assertTrue(definition.equals(StaffPart.of(loadedShaft)), "Custom STAFF_PART changed");
        h.assertTrue(!new ItemStack(Items.STICK).has(ModDataComponents.STAFF_PART),
                "Per-stack part definition leaked to ordinary sticks");
        var basic = StaffAssembly.basic();
        var staff = assemble(h, recipe("staff"), layout(loadedShaft, basic.reinforcement(), basic.tip()));
        staff.set(DataComponents.CUSTOM_NAME, Component.literal("External staff"));
        var loaded = roundTrip(h, staff);
        h.assertTrue(ItemStack.matches(loadedShaft, StaffAssembly.of(loaded).shaft()),
                "Nested custom shaft did not persist");
        stats(h, StaffAssembly.of(loaded), List.of(shaft, basic.reinforcement(), basic.tip()));
        h.succeed();
    }

    @GameTest(template = "spell_arena")
    public static void syntheticPluginUpgrade(GameTestHelper h) {
        var plugin = new ItemStack(Items.PAPER);
        plugin.set(ModDataComponents.STAFF_PART, new StaffPart(StaffPart.Role.UPGRADE, .25, 3, 1, -12,
                ResourceLocation.fromNamespaceAndPath("omnira_modular_staff", "synthetic_plugin"), 1));
        plugin.set(DataComponents.CUSTOM_NAME, Component.literal("External plugin"));
        var staff = StaffAssembly.basic().create();
        staff.set(DataComponents.CUSTOM_NAME, Component.literal("Named staff"));
        var recipe = recipe("staff_upgrade");
        for (int outer = 0; outer < 6; outer++) {
            var slots = new SimpleContainer(7);
            slots.setItem(6, staff.copy());
            slots.setItem(outer, plugin.copy());
            var upgraded = assemble(h, recipe, slots);
            h.assertTrue(staff.get(DataComponents.CUSTOM_NAME).equals(upgraded.get(DataComponents.CUSTOM_NAME)),
                    "Upgrade lost staff name");
            var assembly = StaffAssembly.of(upgraded);
            h.assertTrue(assembly.upgrades().size() == 1 && ItemStack.matches(plugin, assembly.upgrades().getFirst()),
                    "Upgrade lost plugin components");
            var expected = StaffAssembly.basic();
            h.assertTrue(ItemStack.matches(expected.shaft(), assembly.shaft())
                    && ItemStack.matches(expected.reinforcement(), assembly.reinforcement())
                    && ItemStack.matches(expected.tip(), assembly.tip()), "Upgrade changed base parts");
            stats(h, assembly, List.of(expected.shaft(), expected.reinforcement(), expected.tip(), plugin));
            roundTrip(h, upgraded);
            h.assertTrue(ItemStack.matches(staff, slots.getItem(6)), "Upgrade mutated input staff");
            slots.setItem(6, upgraded);
            rejects(h, recipe, slots);
            slots.setItem(6, staff.copy());
            slots.setItem((outer + 1) % 6, plugin.copy());
            rejects(h, recipe, slots);
        }
        var invalid = new SimpleContainer(7);
        invalid.setItem(6, staff);
        rejects(h, recipe, invalid);
        invalid.setItem(0, new ItemStack(Items.PAPER));
        rejects(h, recipe, invalid);
        invalid.setItem(0, staff);
        invalid.setItem(6, plugin);
        rejects(h, recipe, invalid);
        h.succeed();
    }

    private record Caster(FakePlayer player, ItemStack staff, ItemStack grid) {}

    @GameTest(template = "spell_arena", timeoutTicks = 240)
    public static void assemblyChargesPerStrikeAndCreatesOneStaff(GameTestHelper h) {
        AssemblyGameTests.assemblyChargesPerStrikeAndCreatesOneStaff(h);
    }

    @GameTest(template = "spell_arena")
    public static void finishedStaffAttributesAndUnequip(GameTestHelper h) {
        var staff = assemble(h, recipe("staff"), layout(new ItemStack(ModItems.SHADOW_STAFF_SHAFT.get()),
                new ItemStack(ModItems.DIAMOND_REINFORCEMENT.get()),
                new ItemStack(ModItems.SPIRITUAL_CRYSTAL_TIP.get())));
        // Query the stack so the registered attribute event must actually supply the modifiers.
        var modifiers = staff.getAttributeModifiers().modifiers();
        var power = modifiers.stream().filter(e -> e.attribute().equals(ModAttributes.SPELL_POWER)).toList();
        var reduction = modifiers.stream().filter(e -> e.attribute().equals(ModAttributes.COST_REDUCTION)).toList();
        h.assertTrue(power.size() == 1 && reduction.size() == 1, "Missing or duplicate staff attribute modifiers");
        h.assertTrue(power.getFirst().slot() == EquipmentSlotGroup.MAINHAND
                && power.getFirst().modifier().operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                && Math.abs(power.getFirst().modifier().amount() - .1) < .000001,
                "Wrong power amount, operation or equipment slot");
        h.assertTrue(reduction.getFirst().slot() == EquipmentSlotGroup.MAINHAND
                && reduction.getFirst().modifier().operation() == AttributeModifier.Operation.ADD_VALUE
                && reduction.getFirst().modifier().amount() == 5,
                "Wrong reduction amount, operation or equipment slot");
        var player = caster(h).player();
        player.setItemInHand(InteractionHand.MAIN_HAND, staff);
        double originalPower = player.getAttributeValue(ModAttributes.SPELL_POWER);
        double originalReduction = player.getAttributeValue(ModAttributes.COST_REDUCTION);
        var applied = List.of(power.getFirst(), reduction.getFirst());
        try {
            applied.forEach(e -> player.getAttribute(e.attribute()).addTransientModifier(e.modifier()));
            h.assertTrue(Math.abs(player.getAttributeValue(ModAttributes.SPELL_POWER) - originalPower * 1.1) < .000001,
                    "Equipped staff power was not applied");
            h.assertTrue(Math.abs(player.getAttributeValue(ModAttributes.COST_REDUCTION) - originalReduction - 5) < .000001,
                    "Equipped staff reduction was not applied");
        } finally {
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            applied.forEach(e -> player.getAttribute(e.attribute()).removeModifier(e.modifier().id()));
        }
        h.assertTrue(Math.abs(player.getAttributeValue(ModAttributes.SPELL_POWER) - originalPower) < .000001
                && Math.abs(player.getAttributeValue(ModAttributes.COST_REDUCTION) - originalReduction) < .000001,
                "Unequipping staff did not restore attributes");
        h.succeed();
    }

    private static Caster caster(GameTestHelper h) {
        var player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "staff-test"));
        player.setPos(h.absoluteVec(new Vec3(5, 3, 5)));
        player.setXRot(-90);
        player.setData(ModAttachments.MANA, ManaState.initial());
        var staff = new StaffAssembly(new ItemStack(ModItems.WOODEN_STAFF_SHAFT.get()),
                new ItemStack(ModItems.IRON_REINFORCEMENT.get()),
                new ItemStack(ModItems.MULTI_ARCANE_TIP.get()), List.of()).create();
        player.setItemInHand(InteractionHand.MAIN_HAND, staff);
        h.assertTrue(player.getMainHandItem() == staff, "Main hand must hold the actual staff reference");
        h.assertTrue(StaffAssembly.of(staff).stats().power() == 0
                && StaffAssembly.of(staff).stats().reduction() == 0, "Fixture needs no equipment bonuses");
        var grid = new ItemStack(ModItems.BASIC_CRYSTAL_GRID.get());
        var crystals = NonNullList.withSize(3, ItemStack.EMPTY);
        for (int i = 0; i < crystals.size(); i++) {
            var crystal = new ItemStack(ModItems.LOW_TIER_MAGIC_CRYSTAL.get());
            crystal.set(ModDataComponents.SPELL_PATTERN, new SpellPattern(ElementType.AIR, ElementType.AIR));
            crystals.set(i, crystal);
        }
        grid.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(crystals));
        grid.set(ModDataComponents.GRID_CURSOR, 0);
        CuriosApi.getCuriosInventory(player).orElseThrow().setEquippedCurio("crystal_grid", 0, grid);
        h.assertTrue(CrystalGridMenu.locate(player, -2) == grid
                && CrystalGridMenu.locate(player, -2) == CrystalGridMenu.locate(player, -2),
                "Curios locate must retain the equipped grid reference");
        return new Caster(player, staff, grid);
    }

    private static void use(GameTestHelper h, Caster caster, boolean success) {
        var result = ((StaffItem) caster.staff().getItem()).use(h.getLevel(), caster.player(), InteractionHand.MAIN_HAND);
        h.assertTrue(success ? result.getResult().consumesAction() : result.getResult() == InteractionResult.FAIL,
                "Unexpected staff use result");
    }

    private static void state(GameTestHelper h, Caster caster, double mana, int cursor) {
        h.assertTrue(Math.abs(caster.player().getData(ModAttachments.MANA).current() - mana) < .000001,
                "Unexpected mana at player tick " + caster.player().tickCount);
        h.assertTrue(caster.grid().getOrDefault(ModDataComponents.GRID_CURSOR, 0) == cursor,
                "Unexpected cursor at player tick " + caster.player().tickCount);
    }

    private static void tick(Caster caster) {
        // FakePlayers are not in the normal world tick list. Advance their clock before events.
        ++caster.player().tickCount;
        caster.player().getCooldowns().tick();
        StaffEvents.tick(new PlayerTickEvent.Post(caster.player()));
    }

    @GameTest(template = "spell_arena", timeoutTicks = 100)
    public static void doubleShotTimingAndCooldown(GameTestHelper h) {
        var caster = caster(h);
        h.assertTrue(StaffEvents.SHOT_INTERVAL == 6, "Burst interval changed");
        use(h, caster, true);
        state(h, caster, 80, 1);
        use(h, caster, false);
        state(h, caster, 80, 1);
        long started = h.getLevel().getGameTime();
        int cooldown = StaffAssembly.of(caster.staff()).stats().cooldownTicks()+StaffEvents.SHOT_INTERVAL;
        h.assertTrue(cooldown == 66, "Cooldown must start after the second shot");
        for (int t = 1; t <= cooldown; t++) {
            final int elapsed = t;
            h.runAfterDelay(t, () -> {
                h.assertTrue(h.getLevel().getGameTime() - started == elapsed, "Callback did not advance by real ticks");
                tick(caster);
                state(h, caster, elapsed < 6 ? 80 : 60, elapsed < 6 ? 1 : 2);
                h.assertTrue(caster.player().getCooldowns().isOnCooldown(caster.staff().getItem()) == (elapsed < cooldown),
                        "Wrong cooldown boundary at tick " + elapsed);
                if (elapsed == 7 || elapsed == cooldown - 1) {
                    use(h, caster, false);
                    state(h, caster, 60, 2);
                }
                if (elapsed == cooldown) {
                    use(h, caster, true);
                    state(h, caster, 40, 0);
                    caster.player().setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    StaffEvents.tick(new PlayerTickEvent.Post(caster.player()));
                    h.succeed();
                }
            });
        }
    }

    @GameTest(template = "spell_arena")
    public static void swapCancelsQueuedShot(GameTestHelper h) {
        cancellation(h, true);
    }

    @GameTest(template = "spell_arena")
    public static void insufficientManaCancelsQueuedShot(GameTestHelper h) {
        cancellation(h, false);
    }

    private static void cancellation(GameTestHelper h, boolean swap) {
        var caster = caster(h);
        use(h, caster, true);
        state(h, caster, 80, 1);
        for (int t = 1; t <= 13; t++) {
            final int elapsed = t;
            h.runAfterDelay(t, () -> {
                if (elapsed == 1) {
                    if (swap) caster.player().setItemInHand(InteractionHand.MAIN_HAND, caster.staff().copy());
                    else caster.player().setData(ModAttachments.MANA, new ManaState(19, 100));
                }
                if (swap && elapsed == 2) caster.player().setItemInHand(InteractionHand.MAIN_HAND, caster.staff());
                if (!swap && elapsed == 7) caster.player().setData(ModAttachments.MANA, new ManaState(80, 100));
                tick(caster);
                state(h, caster, !swap && elapsed < 7 ? 19 : 80, 1);
                h.assertTrue(caster.player().getCooldowns().isOnCooldown(caster.staff().getItem()),
                        "Cancellation unexpectedly removed cooldown");
                if (elapsed == 13) h.succeed();
            });
        }
    }
}
