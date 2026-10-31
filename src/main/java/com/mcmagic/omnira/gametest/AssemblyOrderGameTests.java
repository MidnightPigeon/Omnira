package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.recipe.AssemblyRecipe;
import com.mcmagic.omnira.registry.ModRecipes;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("omnira_assembly_order")
@PrefixGameTestTemplate(false)
public final class AssemblyOrderGameTests {
    private static SimpleContainer input(AssemblyRecipe r){
        var c=new SimpleContainer(7);
        for(int i=0;i<7;i++)if(!r.slots().get(i).isEmpty())c.setItem(i,r.slots().get(i).getItems()[0].copyWithCount(1));
        return c;
    }
    private static void permute(GameTestHelper h,AssemblyRecipe r,SimpleContainer c,int at,ItemStack expected){
        if(at==6){
            var in=new AssemblyRecipe.Input(c);
            h.assertTrue(r.matches(in,h.getLevel()),"Outer permutation rejected: "+r.result());
            h.assertTrue(ItemStack.matches(expected,r.assemble(in,h.getLevel().registryAccess())),"Permutation changed output/components");return;
        }
        for(int i=at;i<6;i++){
            var a=c.getItem(at);c.setItem(at,c.getItem(i));c.setItem(i,a);
            permute(h,r,c,at+1,expected);
            a=c.getItem(at);c.setItem(at,c.getItem(i));c.setItem(i,a);
        }
    }
    @GameTest(template="spell_arena",timeoutTicks=200)
    public static void allOuterPermutationsAndFixedBase(GameTestHelper h){
        for(var holder:h.getLevel().getRecipeManager().getAllRecipesFor(ModRecipes.ASSEMBLY_TYPE.get())){
            var r=holder.value();var c=input(r);var in=new AssemblyRecipe.Input(c);
            h.assertTrue(r.matches(in,h.getLevel()),"Canonical input rejected: "+holder.id());
            if(r.operation().equals("staff")){
                h.assertTrue(!r.outerShapeless(),"Staff marked unordered");
                var tip=c.getItem(0);c.setItem(0,ItemStack.EMPTY);c.setItem(1,tip);
                h.assertTrue(!r.matches(in,h.getLevel()),"Staff diagonal no longer required");continue;
            }
            h.assertTrue(r.outerShapeless(),"Outer flag not normalized");
            permute(h,r,c,0,r.assemble(in,h.getLevel().registryAccess()));
            if(!c.getItem(6).isEmpty()){c.setItem(6,ItemStack.EMPTY);h.assertTrue(!r.matches(in,h.getLevel()),"Missing base accepted");}
            else {c.setItem(6,new ItemStack(net.minecraft.world.item.Items.BEDROCK));h.assertTrue(!r.matches(in,h.getLevel()),"Empty base requirement ignored");}
        }
        h.succeed();
    }
    private static List<ItemStack> overlap(Ingredient a,Ingredient b){
        if(a.isEmpty()||b.isEmpty())return a.isEmpty()&&b.isEmpty()?List.of(ItemStack.EMPTY):List.of();
        return Arrays.stream(a.getItems()).filter(b::test).toList();
    }
    private static boolean conflict(GameTestHelper h,AssemblyRecipe a,AssemblyRecipe b,SimpleContainer c,int slot,int used){
        if(slot==6){
            var in=new AssemblyRecipe.Input(c);
            return a.matches(in,h.getLevel())&&b.matches(in,h.getLevel())
                    &&!ItemStack.matches(a.assemble(in,h.getLevel().registryAccess()),b.assemble(in,h.getLevel().registryAccess()));
        }
        for(int i=0;i<6;i++)if((used&(1<<i))==0)
            for(var stack:overlap(a.slots().get(slot),b.slots().get(i))){
                c.setItem(slot,stack.copyWithCount(1));
                if(conflict(h,a,b,c,slot+1,used|(1<<i)))return true;
            }
        return false;
    }
    @GameTest(template="spell_arena",timeoutTicks=200)
    public static void noAmbiguousResultsWithResolvedTags(GameTestHelper h){
        var recipes=h.getLevel().getRecipeManager().getAllRecipesFor(ModRecipes.ASSEMBLY_TYPE.get());
        for(int i=0;i<recipes.size();i++)for(int j=i+1;j<recipes.size();j++){
            var a=recipes.get(i);var b=recipes.get(j);
            for(var center:overlap(a.value().slots().get(6),b.value().slots().get(6))){
                var c=new SimpleContainer(7);c.setItem(6,center.copyWithCount(1));
                h.assertTrue(!conflict(h,a.value(),b.value(),c,0,0),"Conflicting assembly: "+a.id()+" and "+b.id());
            }
        }
        h.succeed();
    }
}
