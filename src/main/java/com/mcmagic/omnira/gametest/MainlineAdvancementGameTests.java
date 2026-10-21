package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.forging.ForgeLayout;
import com.mcmagic.omnira.myriad.WorldComponent;
import com.mcmagic.omnira.registry.ModItems;
import com.mcmagic.omnira.progression.AdvancementDependencies;
import com.mojang.authlib.GameProfile;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;
import java.util.UUID;

@GameTestHolder("omnira_mainline")
@PrefixGameTestTemplate(false)
public final class MainlineAdvancementGameTests {
    private static final String[] ORDER={"spiritual_awakening","first_creation","beyond_dreams","mana_surge",
            "further_beyond","light_and_dark","ritual_ascension","spacetime_gate","eightfold_gathering",
            "genesis_echo","world_awakening","deconstruction","world_in_hand","myriad_confluence","something_from_nothing","myriad_codex"};

    private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath("omnira",path);}
    private static AdvancementHolder advancement(GameTestHelper h,String name){
        return h.getLevel().getServer().getAdvancements().get(id("mainline/"+name));
    }
    private static ServerPlayer player(GameTestHelper h){
        var profile=new GameProfile(UUID.randomUUID(),"mainline-test");
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),profile,ClientInformation.createDefault());
        p.connection=new FakePlayer(h.getLevel(),profile).connection;
        return p;
    }
    private static boolean done(GameTestHelper h,ServerPlayer p,String name){
        return p.getAdvancements().getOrStartProgress(advancement(h,name)).isDone();
    }
    private static void acquire(ServerPlayer p,String item){
        p.getInventory().clearContent();
        var stack=new ItemStack(BuiltInRegistries.ITEM.get(id(item)));
        p.getInventory().setItem(0,stack);
        CriteriaTriggers.INVENTORY_CHANGED.trigger(p,p.getInventory(),stack);
    }
    private static void completeBefore(GameTestHelper h,ServerPlayer p,String name){
        for(var entry:ORDER){
            if(entry.equals(name))return;
            for(var criterion:advancement(h,entry).value().criteria().keySet())
                if(!criterion.equals(AdvancementDependencies.PREVIOUS))p.getAdvancements().award(advancement(h,entry),criterion);
            h.assertTrue(done(h,p,entry),"Predecessor did not complete: "+entry);
        }
    }
    private static boolean recorded(GameTestHelper h,ServerPlayer p,String name,String criterion){
        return p.getAdvancements().getOrStartProgress(advancement(h,name)).getCriterion(criterion).isDone();
    }

    @GameTest(template="spell_arena")
    public static void onePlayerTraversesAllSixteenWithoutObjectiveGrants(GameTestHelper h){
        var p=player(h);
        acquire(p,"spiritual_crystal");acquire(p,"arcane_assembly_table");
        var dream=ResourceKey.create(Registries.DIMENSION,id("dream_realm"));
        var spacetime=ResourceKey.create(Registries.DIMENSION,id("spacetime_corridor"));
        CriteriaTriggers.CHANGED_DIMENSION.trigger(p,Level.OVERWORLD,dream);
        acquire(p,"primordial_spell_core");
        var center=h.absolutePos(new BlockPos(5,2,5));
        AdvancedForgeGameTests.build(h,center,Direction.NORTH);
        h.assertTrue(ForgeLayout.form(h.getLevel(),center,p,new ItemStack(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get())),"Survival activation failed");
        acquire(p,"light_dark_spell_core");acquire(p,"advanced_ritual_energy_core");
        CriteriaTriggers.CHANGED_DIMENSION.trigger(p,dream,spacetime);
        for(var core:new String[]{"earth","water","fire","air","light","dark","time","space"})acquire(p,core+"_microcore");
        acquire(p,"genesis_particles");acquire(p,"active_world_seed");
        for(var component:WorldComponent.values())acquire(p,component.id);
        acquire(p,"miniature_world_framework");acquire(p,"omni_microcore");
        acquire(p,"world_generation_probability_cloud");acquire(p,"myriad_codex");
        for(var name:ORDER)h.assertTrue(done(h,p,name),"Actual criterion sequence stopped at "+name);
        h.succeed();
    }

    @GameTest(template="spell_arena")
    public static void sixteenNodesKeepOrderedParentsAndLegacyEntries(GameTestHelper h){
        var all=h.getLevel().getServer().getAdvancements();
        long count=all.getAllAdvancements().stream().filter(a->a.id().getNamespace().equals("omnira")
                && a.id().getPath().startsWith("mainline/")).count();
        h.assertTrue(count==16,"Unexpected mainline count");
        for(int i=0;i<ORDER.length;i++){
            var a=advancement(h,ORDER[i]).value();
            h.assertTrue(a.parent().equals(i==0?java.util.Optional.empty():java.util.Optional.of(id("mainline/"+ORDER[i-1]))),"Wrong parent: "+ORDER[i]);
            h.assertTrue(!a.display().orElseThrow().getIcon().isEmpty(),"Missing icon: "+ORDER[i]);
            h.assertTrue(a.rewards().equals(net.minecraft.advancements.AdvancementRewards.EMPTY),"Unapproved advancement reward");
            if(i>0)h.assertTrue(a.criteria().containsKey(AdvancementDependencies.PREVIOUS)
                    &&a.requirements().requirements().contains(java.util.List.of(AdvancementDependencies.PREVIOUS)),"Missing mandatory parent gate: "+ORDER[i]);
        }
        for(var legacy:new String[]{"beyond_the_mist","personal_spaceflight","too_late_scholar","needle_sick_donkey"})
            h.assertTrue(all.get(id(legacy))!=null,"Legacy advancement removed: "+legacy);
        h.succeed();
    }

    @GameTest(template="spell_arena")
    public static void acquisitionRecordsEarlyButWaitsForParent(GameTestHelper h){
        var p=player(h);
        String[][] acquisitions={{"spiritual_awakening","spiritual_crystal"},{"first_creation","arcane_assembly_table"},
                {"mana_surge","primordial_spell_core"},{"light_and_dark","light_dark_spell_core"},
                {"ritual_ascension","advanced_ritual_energy_core"},{"genesis_echo","genesis_particles"},
                {"world_awakening","active_world_seed"},{"world_in_hand","miniature_world_framework"},
                {"myriad_confluence","omni_microcore"},{"something_from_nothing","world_generation_probability_cloud"},{"myriad_codex","myriad_codex"}};
        acquire(p,"myriad_codex");
        h.assertTrue(!done(h,p,"myriad_codex")&&recorded(h,p,"myriad_codex","obtained"),"Early codex was awarded or lost");
        acquire(p,"world_generation_probability_cloud");
        h.assertTrue(!done(h,p,"something_from_nothing")&&recorded(h,p,"something_from_nothing","obtained"),"Early cloud bypassed parent or was lost");
        for(var entry:acquisitions){
            completeBefore(h,p,entry[0]);acquire(p,entry[1]);
            h.assertTrue(done(h,p,entry[0]),"Acquisition did not award after parent: "+entry[0]);
        }
        h.succeed();
    }

    @GameTest(template="spell_arena")
    public static void eightCoresAndComponentsAreCumulative(GameTestHelper h){
        var p=player(h);var cores=new String[]{"earth","water","fire","air","light","dark","time","space"};
        acquire(p,"omni_microcore");
        h.assertTrue(!done(h,p,"eightfold_gathering"),"Omni substitutes for the eight cores");
        for(int i=0;i<cores.length;i++){
            acquire(p,cores[i]+"_microcore");
            h.assertTrue(!done(h,p,"eightfold_gathering")&&recorded(h,p,"eightfold_gathering",cores[i]+"_microcore"),"Early core tracking incorrect at "+i);
        }
        for(var component:WorldComponent.values()){
            acquire(p,component.id);
            h.assertTrue(!done(h,p,"deconstruction")&&recorded(h,p,"deconstruction",component.id),"Early component tracking incorrect");
        }
        completeBefore(h,p,"eightfold_gathering");h.assertTrue(done(h,p,"eightfold_gathering"),"Recorded cores required reacquisition");
        completeBefore(h,p,"deconstruction");h.assertTrue(done(h,p,"deconstruction"),"Recorded components required reacquisition");
        h.succeed();
    }

    @GameTest(template="spell_arena")
    public static void dimensionCriteriaUseDestination(GameTestHelper h){
        var p=player(h);
        CriteriaTriggers.CHANGED_DIMENSION.trigger(p,Level.OVERWORLD,Level.NETHER);
        h.assertTrue(!done(h,p,"beyond_dreams") && !done(h,p,"spacetime_gate"),"Vanilla trip advanced Omnira exploration");
        var dream=ResourceKey.create(Registries.DIMENSION,id("dream_realm"));
        var spacetime=ResourceKey.create(Registries.DIMENSION,id("spacetime_corridor"));
        CriteriaTriggers.CHANGED_DIMENSION.trigger(p,Level.OVERWORLD,dream);
        h.assertTrue(!done(h,p,"beyond_dreams")&&recorded(h,p,"beyond_dreams","entered"),"Early dream trip not gated/recorded");
        CriteriaTriggers.CHANGED_DIMENSION.trigger(p,dream,spacetime);
        h.assertTrue(!done(h,p,"spacetime_gate")&&recorded(h,p,"spacetime_gate","entered"),"Early spacetime trip not gated/recorded");
        completeBefore(h,p,"beyond_dreams");h.assertTrue(done(h,p,"beyond_dreams"),"Dream trip needed repeating");
        completeBefore(h,p,"spacetime_gate");h.assertTrue(done(h,p,"spacetime_gate"),"Spacetime trip needed repeating");
        h.succeed();
    }

    @GameTest(template="spell_arena")
    public static void advancedAssemblyAwardsOnlyAfterSuccessfulFormation(GameTestHelper h){
        var p=player(h);var center=h.absolutePos(new BlockPos(5,2,5));
        AdvancedForgeGameTests.build(h,center,Direction.NORTH);
        h.assertTrue(!ForgeLayout.form(h.getLevel(),center,p,new ItemStack(Items.DIAMOND))
                && !done(h,p,"further_beyond"),"Invalid activation awarded");
        h.assertTrue(ForgeLayout.form(h.getLevel(),center,p,new ItemStack(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get()))
                && !done(h,p,"further_beyond")&&recorded(h,p,"further_beyond","assembled"),"Assembly not gated/recorded");
        completeBefore(h,p,"further_beyond");h.assertTrue(done(h,p,"further_beyond"),"Recorded assembly needed repeating");
        var other=player(h);
        h.assertTrue(ForgeLayout.assemble(h.getLevel(),center,Direction.NORTH)
                && !done(h,other,"further_beyond"),"Administrative assembly awarded an unrelated player");
        h.succeed();
    }

    @GameTest(template="spell_arena")
    public static void reverseOrderRecordsCascadeAndRevocationKeepsObjectives(GameTestHelper h){
        var p=player(h);
        for(int i=ORDER.length-1;i>0;i--){
            var a=advancement(h,ORDER[i]);
            for(var criterion:a.value().criteria().keySet())
                if(!criterion.equals(AdvancementDependencies.PREVIOUS))p.getAdvancements().award(a,criterion);
            h.assertTrue(!done(h,p,ORDER[i]),"Early completion: "+ORDER[i]);
        }
        acquire(p,"spiritual_crystal");
        for(var name:ORDER)h.assertTrue(done(h,p,name),"Cascade stopped at "+name);
        p.getAdvancements().revoke(advancement(h,ORDER[0]),"obtained");
        for(var name:ORDER)h.assertTrue(!done(h,p,name),"Revoked chain remained done: "+name);
        h.assertTrue(recorded(h,p,"myriad_codex","obtained"),"Revocation deleted early objective");
        acquire(p,"spiritual_crystal");h.assertTrue(done(h,p,"myriad_codex"),"Re-earned root did not restore recorded chain");
        h.succeed();
    }

    @GameTest(template="spell_arena")
    public static void loadedObjectivesReconcileOnLoginAndReload(GameTestHelper h){
        var p=player(h);
        for(var name:ORDER){
            var a=advancement(h,name);var progress=p.getAdvancements().getOrStartProgress(a);
            for(var criterion:a.value().criteria().keySet())
                if(!criterion.equals(AdvancementDependencies.PREVIOUS))progress.grantProgress(criterion);
        }
        h.assertTrue(!done(h,p,"myriad_codex"),"Old save unexpectedly included new gate");
        AdvancementDependencies.reload(new net.neoforged.neoforge.event.OnDatapackSyncEvent(p.server.getPlayerList(),p));
        for(var name:ORDER)h.assertTrue(done(h,p,name),"Loaded chain did not settle: "+name);
        AdvancementDependencies.sync(p);
        h.assertTrue(done(h,p,"myriad_codex"),"Repeated synchronization altered completion");
        var other=player(h);var codex=advancement(h,"myriad_codex");
        other.getAdvancements().getOrStartProgress(codex).grantProgress("obtained");
        AdvancementDependencies.sync(other);
        h.assertTrue(!done(h,other,"myriad_codex")&&recorded(h,other,"myriad_codex","obtained"),"Old out-of-order completion bypassed gate or was erased");
        h.succeed();
    }
}
