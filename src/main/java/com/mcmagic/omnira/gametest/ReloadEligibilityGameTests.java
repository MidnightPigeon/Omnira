package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.config.OmniraConfig;
import com.mcmagic.omnira.registry.ModEntityTypes;
import com.mcmagic.omnira.spell.SpellEffect;
import com.mcmagic.omnira.spell.world.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("omnira_reload_eligibility") @PrefixGameTestTemplate(false)
public final class ReloadEligibilityGameTests {
    @GameTest(template="spell_arena") public static void allModCreatureFamiliesAreIncluded(GameTestHelper h){
        var representatives=new EnumMap<WorldEntityKinds.Kind,LivingEntity>(WorldEntityKinds.Kind.class);
        for(var type:List.of(EntityType.COW,EntityType.WOLF,EntityType.ZOMBIE,EntityType.WITHER)){
            var mob=(LivingEntity)type.create(h.getLevel());representatives.put(WorldEntityKinds.kind(mob),mob);
        }
        var audit=new ArrayList<String>();
        try{
            ReloadEffect.clear();
            for(var type:BuiltInRegistries.ENTITY_TYPE){
                if(!BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace().equals("omnira"))continue;
                var entity=type.create(h.getLevel());
                try{
                    if(!(entity instanceof LivingEntity living))continue;
                    var kind=WorldEntityKinds.kind(living);
                    boolean eligible=WorldEntityKinds.eligible(living)&&!ReloadEffect.blocked(type);
                    var candidates=ReloadEffect.livingCandidates(SpellEffect.utility("reload",1,0,1),representatives.get(kind));
                    h.assertTrue(candidates.contains(type)==eligible,"Mod creature missing or unexpectedly included: "+BuiltInRegistries.ENTITY_TYPE.getKey(type));
                    audit.add(BuiltInRegistries.ENTITY_TYPE.getKey(type)+"="+kind+":"+(eligible?"included":"excluded"));
                }finally{if(entity!=null)entity.discard();}
            }
            com.mcmagic.omnira.Omnira.LOGGER.info("Verified Omnira reload creature audit: {}",audit);
        }finally{representatives.values().forEach(Entity::discard);ReloadEffect.clear();}
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void dreamCreaturesCanActuallyReplaceTheirOwnFamily(GameTestHelper h){
        var old=OmniraConfig.RELOAD_ENTITY_BLACKLIST.get();
        var targets=List.of(EntityType.COW,EntityType.ZOMBIE,EntityType.WOLF);
        var results=List.of(ModEntityTypes.LIGHT_SPIRIT.get(),ModEntityTypes.SHADOW_GHOST.get(),ModEntityTypes.DREAM_MIRROR.get());
        try{
            for(int i=0;i<targets.size();i++){
                var input=targets.get(i);var output=results.get(i);
                var source=(Mob)input.create(h.getLevel());
                Mob replacement=null;
                try{
                    source.moveTo(h.absolutePos(new BlockPos(5,5,5)),0,0);
                    OmniraConfig.RELOAD_ENTITY_BLACKLIST.set(BuiltInRegistries.ENTITY_TYPE.stream().filter(t->t!=input&&t!=output)
                            .map(t->BuiltInRegistries.ENTITY_TYPE.getKey(t).toString()).toList());
                    var infused=SpellEffect.utility("reload",1,0,1);
                    h.assertTrue(ReloadEffect.livingCandidates(infused,source).equals(List.of(output)),"Dream creature absent from its family");
                    h.assertTrue(ReloadEffect.livingCandidates(SpellEffect.utility("reload",1,0,0),source).isEmpty(),"Uninfused spell included a mod creature");
                    if(i==1)h.assertTrue(ReloadEffect.livingCandidates(SpellEffect.utility("reload",0,0,1),source).isEmpty(),"Hostile transformation bypassed enhancement");
                    replacement=ReloadEffect.replaceLiving(infused,source);
                    h.assertTrue(replacement!=null&&replacement.getType()==output&&source.isRemoved(),"Actual dream creature conversion failed");
                }finally{source.discard();if(replacement!=null)replacement.discard();}
            }
        }finally{OmniraConfig.RELOAD_ENTITY_BLACKLIST.set(old);ReloadEffect.clear();}
        h.succeed();
    }
    private ReloadEligibilityGameTests(){}
}
