package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.item.staff.*;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.spell.*;
import com.mcmagic.omnira.spell.entity.SpellEntity;
import com.mcmagic.omnira.compat.AbyssTipEffects;
import com.mcmagic.omnira.recipe.AssemblyRecipe;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("omnira_boss_tips") @PrefixGameTestTemplate(false)
public class BossStaffTipGameTests {
    private static StaffAssembly assembly(Item item){var base=StaffAssembly.basic();return new StaffAssembly(base.shaft(),base.reinforcement(),new ItemStack(item),List.of());}
    @GameTest(template="spell_arena") public static void propertiesAndCastSnapshots(GameTestHelper h){
        var storm=assembly(ModItems.STORM_TIP.get());var abyss=assembly(ModItems.ABYSS_TIP.get());
        h.assertTrue(storm.valid()&&abyss.valid(),"Invalid assembly");
        var s=storm.stats();var a=abyss.stats();
        h.assertTrue(s.shots()==1&&s.cooldownTicks()==40&&s.power()==.1&&s.cooldownReduction()==.1&&s.speedMultiplier()==1.5&&s.rangeMultiplier()==1&&s.selectionBonus()==.5,"Storm stats");
        h.assertTrue(a.shots()==1&&a.cooldownTicks()==20&&a.power()==.5&&a.selectionBonus()==0,"Abyss stats");
        h.assertTrue(storm.slotCapacity(StaffPart.SlotKind.RUNE)==1&&abyss.slotCapacity(StaffPart.SlotKind.RUNE)==2,"Rune capacities");
        h.assertTrue(StaffPart.of(storm.tip()).independentCooldown()&&!StaffPart.of(abyss.tip()).independentCooldown(),"Chain rules");
        var p=StaffEnhancements.apply(SpellPayload.EMPTY,StaffEnhancements.keywords(abyss.create()));
        var q=StaffEnhancements.apply(SpellPayload.EMPTY,StaffEnhancements.keywords(abyss.create()));
        h.assertTrue(AbyssTipEffects.castId(p)!=null&&!AbyssTipEffects.castId(p).equals(AbyssTipEffects.castId(q)),"Cast identities merged");
        h.assertTrue(!SpellPayload.EMPTY.keywords().contains("omnira:abyss_tip"),"Stored crystal mutated");h.succeed();
    }
    @GameTest(template="spell_arena") public static void centerOnlyRecipes(GameTestHelper h){
        for(String id:List.of("storm_tip","abyss_tip")){
            var recipe=h.getLevel().getRecipeManager().byKey(ResourceLocation.parse("omnira:assembly/"+id));
            if(!net.neoforged.fml.ModList.get().isLoaded("cataclysm")){h.assertTrue(recipe.isEmpty(),"Optional recipe leaked");continue;}
            h.assertTrue(recipe.isPresent(),"Missing recipe");var r=(AssemblyRecipe)recipe.orElseThrow().value();var c=new SimpleContainer(7);
            c.setItem(6,new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id.equals("storm_tip")?"cataclysm:essence_of_the_storm":"cataclysm:tidal_claws"))));
            h.assertTrue(r.matches(new AssemblyRecipe.Input(c),h.getLevel()),"Central material rejected");
            c.setItem(0,new ItemStack(Items.STONE));h.assertTrue(!r.matches(new AssemblyRecipe.Input(c),h.getLevel()),"Extra outer material accepted");
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void abyssDamageAndMistDeduplication(GameTestHelper h){
        var curse=BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse("cataclysm:abyssal_curse"));
        var fear=BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse("cataclysm:abyssal_fear"));
        if(curse.isEmpty()||fear.isEmpty()){h.succeed();return;}
        var target=h.spawn(EntityType.COW,2,2,2);var owner=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var payload=StaffEnhancements.apply(SpellPayload.EMPTY,StaffEnhancements.keywords(assembly(ModItems.ABYSS_TIP.get()).create()));
        var spell=SpellEntity.spawn(h.getLevel(),owner,SpellEntity.Kind.BURST,target.position());spell.configure(1,payload);
        boolean sawCurse=false,sawFear=false;
        for(int i=0;i<32;i++){
            target.removeAllEffects();target.setHealth(target.getMaxHealth());target.invulnerableTime=0;
            target.hurt(h.getLevel().damageSources().indirectMagic(spell,owner),1);
            var c=target.getEffect(curse.get());var f=target.getEffect(fear.get());
            h.assertTrue((c!=null) != (f!=null),"Expected exactly one random effect");
            if(c!=null){sawCurse=true;h.assertTrue(c.getAmplifier()==1&&c.getDuration()==600,"Curse level/duration");}
            if(f!=null){sawFear=true;h.assertTrue(f.getAmplifier()==0&&f.getDuration()==600,"Fear level/duration");}
        }
        h.assertTrue(sawCurse&&sawFear,"Missing random branch");
        var mist=ModEntityTypes.SHADOW_MIST.get().create(h.getLevel());mist.abyssCast(AbyssTipEffects.castId(payload));
        for(int i=0;i<2;i++){
            target.removeAllEffects();target.setHealth(target.getMaxHealth());target.invulnerableTime=0;
            target.hurt(h.getLevel().damageSources().indirectMagic(mist,owner),1);
            boolean applied=target.hasEffect(curse.get())||target.hasEffect(fear.get());h.assertTrue(applied==(i==0),"Mist retriggered or first hit missed");
        }
        var other=ModEntityTypes.SHADOW_MIST.get().create(h.getLevel());other.abyssCast(mist.abyssCast());target.invulnerableTime=0;
        target.hurt(h.getLevel().damageSources().indirectMagic(other,owner),1);
        h.assertTrue(!target.hasEffect(curse.get())&&!target.hasEffect(fear.get()),"Sibling mist retriggered");
        spell.discard();h.succeed();
    }
}
