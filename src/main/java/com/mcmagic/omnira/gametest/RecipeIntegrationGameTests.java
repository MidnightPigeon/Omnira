package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.recipe.*;
import com.mcmagic.omnira.forging.AdvancedForgeRecipe;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_recipe_integration")
@PrefixGameTestTemplate(false)
public final class RecipeIntegrationGameTests {
    @GameTest(template="spell_arena")
    public static void registeredRecipesRoundTrip(GameTestHelper h) {
        int count=0;
        var ops=h.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        for(var holder:h.getLevel().getRecipeManager().getRecipes()) {
            var recipe=holder.value();
            var id=BuiltInRegistries.RECIPE_SERIALIZER.getKey(recipe.getSerializer());
            if(!id.getNamespace().equals("omnira"))continue;
            var typeId=BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType());
            if(typeId.getNamespace().equals("omnira"))
                h.assertTrue(recipe.getType().toString().equals(typeId.toString()),"Noncanonical recipe type: "+typeId);
            var json=Recipe.CODEC.encodeStart(ops,recipe).getOrThrow();
            var decoded=Recipe.CODEC.parse(ops,json).getOrThrow();
            h.assertTrue(Recipe.CODEC.encodeStart(ops,decoded).getOrThrow().equals(json),"JSON round trip changed "+holder.id());
            var raw=Unpooled.buffer();
            var repeated=Unpooled.buffer();
            try {
                var buffer=new RegistryFriendlyByteBuf(raw,h.getLevel().registryAccess());
                Recipe.STREAM_CODEC.encode(buffer,recipe);
                int length=buffer.writerIndex();
                var network=Recipe.STREAM_CODEC.decode(buffer);
                // Ingredient packets expand tags to items; JSON identity is not expected after sync.
                Recipe.STREAM_CODEC.encode(new RegistryFriendlyByteBuf(repeated,h.getLevel().registryAccess()),network);
                h.assertTrue(io.netty.buffer.ByteBufUtil.equals(raw,0,repeated,0,length)&&repeated.writerIndex()==length,
                        "Network round trip changed "+holder.id());
                h.assertTrue(buffer.readableBytes()==0,"Trailing recipe packet data");
            } finally { raw.release();repeated.release(); }
            count++;
        }
        h.assertTrue(count>100,"Recipe registry coverage unexpectedly small");
        h.succeed();
    }

    @GameTest(template="spell_arena")
    public static void outputTemplatesAreNotAliased(GameTestHelper h) {
        var source=new ItemStack(Items.DIAMOND);
        var slots=List.of(Ingredient.EMPTY,Ingredient.EMPTY,Ingredient.EMPTY,Ingredient.EMPTY,
                Ingredient.EMPTY,Ingredient.EMPTY,Ingredient.of(Items.STICK));
        var assembly=new AssemblyRecipe(slots,source);
        var advanced=new AdvancedForgeRecipe(List.of(Ingredient.of(Items.STICK),Ingredient.EMPTY,Ingredient.EMPTY,
                Ingredient.EMPTY,Ingredient.EMPTY,Ingredient.EMPTY,Ingredient.EMPTY,Ingredient.EMPTY,Ingredient.EMPTY),source);
        var condensation=new CondensationRecipe(0,source);
        var analysis=new AnalysisRecipe(Ingredient.of(Items.STICK),1,source,0,0);
        var starlight=new StarlightConversionRecipe(Ingredient.of(Items.STICK),source);
        var vessel=new VesselConversionRecipe(Ingredient.of(Items.STICK),source);
        source.setCount(32);
        for(Recipe<?> recipe:List.of(assembly,advanced,condensation,analysis,starlight,vessel)) {
            var output=recipe.getResultItem(h.getLevel().registryAccess());
            h.assertTrue(output.getCount()==1,"Caller changed recipe template");
            output.setCount(16);
            h.assertTrue(recipe.getResultItem(h.getLevel().registryAccess()).getCount()==1,"Display changed recipe template");
        }
        h.succeed();
    }

    private RecipeIntegrationGameTests() {}
}
