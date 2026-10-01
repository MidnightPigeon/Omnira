package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.registry.ModItems;
import com.mcmagic.omnira.reversal.ReversalContent;
import com.mcmagic.omnira.shop.MarisaTrades;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("omnira_dream_rabbit") @PrefixGameTestTemplate(false)
public final class DreamRabbitGameTests {
    @GameTest(template="spell_arena")
    public static void reversalAndRareStock(GameTestHelper h){
        var input=new ItemStack(ModItems.DREAM_RABBIT_REMAINS.get());
        var recipe=h.getLevel().getRecipeManager().getRecipeFor(ReversalContent.TYPE.get(),
                new net.minecraft.world.item.crafting.SingleRecipeInput(input),h.getLevel()).orElseThrow().value();
        h.assertTrue(recipe.count()==1&&recipe.outputs(input).getFirst().is(ModItems.ALICE_DREAM_RABBIT_SIGIL.get()),"Rabbit reversal recipe missing");
        boolean found=false;
        for(int seed=0;seed<128;seed++)for(var offer:MarisaTrades.roll(h.getLevel(),seed,42))
            if(offer.getResult().is(ModItems.DREAM_RABBIT_REMAINS.get())){
                h.assertTrue(offer.getMaxUses()==2,"Rabbit residue is not a rare offer");found=true;
            }
        h.assertTrue(found,"Rabbit residue never appears in the rare pool");
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void lootTableProbability(GameTestHelper h)throws java.io.IOException{
        var key=ResourceLocation.parse("omnira:loot_table/chests/looking_glass_garden_rabbit.json");
        try(var reader=h.getLevel().getServer().getResourceManager().getResourceOrThrow(key).openAsReader()){
            var table=com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
            var pools=table.getAsJsonArray("pools");
            h.assertTrue(pools.size()==3,"Garden loot pools missing");
            var supplies=pools.get(0).getAsJsonObject();
            h.assertTrue(supplies.getAsJsonObject("rolls").get("min").getAsInt()==2
                    &&supplies.getAsJsonObject("rolls").get("max").getAsInt()==4,"Garden supplies range changed");
            for(var entry:supplies.getAsJsonArray("entries")){
                var id=ResourceLocation.parse(entry.getAsJsonObject().get("name").getAsString());
                h.assertTrue(BuiltInRegistries.ITEM.containsKey(id),"Unknown garden loot item: "+id);
            }
            var rabbit=pools.get(1).getAsJsonObject();
            h.assertTrue(rabbit.get("rolls").getAsInt()==1,"More than one rabbit relic can drop");
            h.assertTrue(rabbit.getAsJsonArray("conditions").get(0).getAsJsonObject().get("chance").getAsDouble()==.25,"Rabbit loot chance changed");
            var id=rabbit.getAsJsonArray("entries").get(0).getAsJsonObject().get("name").getAsString();
            h.assertTrue(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id))==ModItems.DREAM_RABBIT_REMAINS.get(),"Rabbit loot item changed");
            var core=pools.get(2).getAsJsonObject();
            h.assertTrue(core.get("rolls").getAsInt()==1
                    &&core.getAsJsonArray("conditions").get(0).getAsJsonObject().get("chance").getAsDouble()==.05
                    &&core.getAsJsonArray("entries").get(0).getAsJsonObject().get("name").getAsString().equals("omnira:spacetime_spell_core"),
                    "Spacetime core must be an independent 5% roll");
        }
        h.succeed();
    }
}
