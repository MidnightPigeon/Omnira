package com.mcmagic.omnira.gametest;
import com.mcmagic.omnira.spell.*;
import com.mcmagic.omnira.spell.world.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_cataclysm") @PrefixGameTestTemplate(false)
public final class CataclysmReloadGameTests {
    @GameTest(template="spell_arena") public static void replacementIgnoresOldBodyButKeepsRealObstacles(GameTestHelper h){
        var level=h.getLevel();var at=h.absoluteVec(new net.minecraft.world.phys.Vec3(8,35,8));
        var old=EntityType.SHULKER.create(level);old.moveTo(at);level.addFreshEntity(old);
        var next=EntityType.COW.create(level);next.moveTo(at);
        h.assertTrue(ReloadEffect.replacementFits(level,old,next),"Old solid entity blocks its own replacement");
        var boat=EntityType.BOAT.create(level);boat.moveTo(at);level.addFreshEntity(boat);
        h.assertTrue(!ReloadEffect.replacementFits(level,old,next),"Unrelated solid entity was ignored");boat.discard();
        level.setBlockAndUpdate(old.blockPosition(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        h.assertTrue(!ReloadEffect.replacementFits(level,old,next),"Replacement ignores a wall");
        old.discard();next.discard();h.succeed();
    }
    @GameTest(template="spell_arena") public static void multipartBodyIsOneReplacement(GameTestHelper h){
        var level=h.getLevel();var at=h.absoluteVec(new net.minecraft.world.phys.Vec3(8,55,8));
        var old=EntityType.ENDER_DRAGON.create(level);old.moveTo(at);level.addFreshEntity(old);
        for(var part:old.getSubEntities())part.setPos(at);
        var next=EntityType.COW.create(level);next.moveTo(at);
        h.assertTrue(ReloadEffect.replacementFits(level,old,next),"Old multipart child blocks replacement");
        old.discard();next.discard();h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=400) public static void enhancedInfusedReloadConvertsBosses(GameTestHelper h){
        if(!net.neoforged.fml.ModList.get().isLoaded("cataclysm")){h.succeed();return;}
        var effect=new SpellEffect(false,0,0,true,"reload",1,0,1);
        var ordinary=new SpellEffect(false,0,0,true,"reload",0,0,1);
        var player=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"reload-boss"));
        int count=0;
        for(var type:BuiltInRegistries.ENTITY_TYPE){
            if(!BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace().equals("cataclysm"))continue;
            var entity=type.create(h.getLevel());
            if(!(entity instanceof Mob target)||!WorldEntityKinds.boss(target)){if(entity!=null)entity.discard();continue;}
            var id=BuiltInRegistries.ENTITY_TYPE.getKey(type);
            target.moveTo(h.absoluteVec(new net.minecraft.world.phys.Vec3(8,45,8)));
            h.assertTrue(ReloadEffect.livingCandidates(ordinary,target).isEmpty(),"Boss accepted without enhancement: "+id);
            h.assertTrue(ReloadEffect.livingCandidates(effect,target).contains(com.mcmagic.omnira.registry.ModEntityTypes.WEAVER_SPIRIT.get()),"Enhanced+infused boss candidates incomplete: "+id);
            h.getLevel().addFreshEntity(target);
            UtilitySpellEffects.applyLiving(effect,player,player,target,1);
            h.assertTrue(target.isRemoved(),"Cataclysm boss resisted reload: "+id);
            com.mcmagic.omnira.Omnira.LOGGER.info("Cataclysm reload converted {}",id);count++;
            for(var replacement:h.getLevel().getEntitiesOfClass(Mob.class,new net.minecraft.world.phys.AABB(target.blockPosition()).inflate(20)))replacement.discard();
        }
        h.assertTrue(count>=5,"Too few Cataclysm bosses tested: "+count);h.succeed();
    }
}
