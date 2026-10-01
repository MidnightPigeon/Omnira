package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.recipe.AssemblyRecipe;
import com.mcmagic.omnira.registry.ModItems;
import com.mcmagic.omnira.registry.ModDataComponents;
import com.mcmagic.omnira.spell.SpellPayload;
import com.mcmagic.omnira.spell.entity.SpellEntity;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_guide_revision")
@PrefixGameTestTemplate(false)
public final class GuideRevisionGameTests {
    private static ResourceLocation id(String path) {return ResourceLocation.fromNamespaceAndPath("omnira",path);}
    @GameTest(template="spell_arena")
    public static void engineRequiresFixedMaterials(GameTestHelper h) {
        var manager=h.getLevel().getRecipeManager();
        h.assertTrue(manager.byKey(id("mana_engine")).isEmpty(),"Old crafting recipe still present");
        var recipe=(AssemblyRecipe)manager.byKey(id("assembly/mana_engine")).orElseThrow().value();
        var items=new Item[]{Items.IRON_BLOCK,Items.REDSTONE_BLOCK,ModItems.LOW_TIER_MAGIC_CRYSTAL.get()};
        int combinations=0;
        for(int parity=0;parity<2;parity++) for(int a=0;a<3;a++) for(int b=0;b<3;b++) {
            if(a==b) continue;
            int c=3-a-b;
            var inventory=new SimpleContainer(7);
            for(int i=0;i<3;i++) inventory.setItem(parity+2*i,new ItemStack(ModItems.SPIRITUAL_CRYSTAL.get()));
            int[] order={a,b,c};
            for(int i=0;i<3;i++) {
                var stack=new ItemStack(items[order[i]]);
                if(stack.is(ModItems.LOW_TIER_MAGIC_CRYSTAL.get())) stack.set(ModDataComponents.SPELL_PAYLOAD,new SpellPayload(70,3));
                inventory.setItem(1-parity+2*i,stack);
            }
            inventory.setItem(6,new ItemStack(ModItems.CRYSTAL_CASING.get()));
            var input=new AssemblyRecipe.Input(inventory);
            boolean fixed=parity==0 && a==0 && b==1;
            h.assertTrue(recipe.matches(input,h.getLevel())==fixed,"Engine must accept only the fixed arrangement");
            h.assertTrue(recipe.assemble(input,h.getLevel().registryAccess()).is(ModItems.MANA_ENGINE.get()),"Wrong result");
            var first=inventory.getItem(0);inventory.setItem(0,inventory.getItem(1));inventory.setItem(1,first);
            h.assertTrue(!recipe.matches(input,h.getLevel()),"Adjacent crystals incorrectly accepted");
            combinations++;
        }
        h.assertTrue(combinations==12,"Missing permutation coverage");h.succeed();
    }
    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h) {
        var profile=new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"spaceflight-test");
        var player=new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(),h.getLevel(),profile,
                net.minecraft.server.level.ClientInformation.createDefault());
        // FakePlayer disables riding; use a real server player with its no-op connection.
        player.connection=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),profile).connection;
        player.setPos(h.absoluteVec(new Vec3(4,100,4)));return player;
    }
    private static SpellEntity ride(GameTestHelper h,net.minecraft.server.level.ServerPlayer player) {
        var flight=SpellEntity.spawn(h.getLevel(),player,SpellEntity.Kind.SELF_FLIGHT,player.position());
        h.assertTrue(player.startRiding(flight,true),"Test rider failed to mount");
        flight.setDeltaMovement(0,.25,0);return flight;
    }
    private static void ticks(SpellEntity flight,int count) {for(int i=0;i<count;i++) {flight.tickCount++;flight.tick();}}
    @GameTest(template="spell_arena")
    public static void spaceflightRequiresMoreThanFiftyBlocks(GameTestHelper h) {
        var player=player(h);var flight=ride(h,player);
        var advancement=h.getLevel().getServer().getAdvancements().get(id("personal_spaceflight"));
        h.assertTrue(advancement!=null,"Advancement missing");
        double start=player.getY();
        ticks(flight,200);
        h.assertTrue(!player.getAdvancements().getOrStartProgress(advancement).isDone(),"Exactly fifty must not award");
        ticks(flight,1);
        h.assertTrue(player.getAdvancements().getOrStartProgress(advancement).isDone(),"More than fifty did not award: rise="+(player.getY()-start)+", removed="+flight.isRemoved()+", riding="+(player.getVehicle()==flight));
        player.stopRiding();flight.discard();h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void separateRidesDoNotAccumulate(GameTestHelper h) {
        var player=player(h);var flight=ride(h,player);
        var advancement=h.getLevel().getServer().getAdvancements().get(id("personal_spaceflight"));
        ticks(flight,120);player.stopRiding();player.startRiding(flight,true);ticks(flight,120);
        h.assertTrue(!player.getAdvancements().getOrStartProgress(advancement).isDone(),"Separate rides accumulated altitude");
        player.stopRiding();flight.discard();h.succeed();
    }
}
