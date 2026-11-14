package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.config.OmniraConfig;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.spell.SpellEffect;
import com.mcmagic.omnira.spell.world.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("omnira_weaver_spirit") @PrefixGameTestTemplate(false)
public final class WeaverSpiritGameTests {
    @GameTest(template="spell_arena") public static void bossShellAndPersistence(GameTestHelper h){
        var boss=ModEntityTypes.WEAVER_SPIRIT.get().create(h.getLevel());
        h.assertTrue(boss!=null&&boss.getMaxHealth()==1024&&boss.getHealth()==1024,"Boss health is not 1024");
        h.assertTrue(boss.getBbWidth()==3&&boss.getBbHeight()==3&&WorldEntityKinds.kind(boss)==WorldEntityKinds.Kind.BOSS,"Boss size or family incorrect");
        h.assertTrue(boss.getType().canSummon()&&boss.isPersistenceRequired()&&!boss.removeWhenFarAway(10000),"Admin summon disabled or boss despawns");
        var cow=EntityType.COW.create(h.getLevel());float health=cow.getHealth();
        h.assertTrue(!boss.doHurtTarget(cow)&&cow.getHealth()==health,"Unspecified attacks were added");
        boss.setHealth(777);var tag=new net.minecraft.nbt.CompoundTag();boss.saveWithoutId(tag);
        var restored=ModEntityTypes.WEAVER_SPIRIT.get().create(h.getLevel());restored.load(tag);
        h.assertTrue(restored.getMaxHealth()==1024&&restored.getHealth()==777&&restored.isPersistenceRequired(),"Boss state lost on save");
        boss.discard();restored.discard();cow.discard();h.succeed();
    }
    @GameTest(template="spell_arena") public static void adminSummonWorks(GameTestHelper h){
        var at=h.absolutePos(new BlockPos(5,4,5));var server=h.getLevel().getServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withLevel(h.getLevel()).withSuppressedOutput(),
                "summon omnira:weaver_spirit "+at.getX()+" "+at.getY()+" "+at.getZ());
        var bosses=h.getLevel().getEntitiesOfClass(com.mcmagic.omnira.entity.WeaverSpirit.class,new net.minecraft.world.phys.AABB(at).inflate(3));
        h.assertTrue(bosses.size()==1,"Administrator summon command failed");bosses.forEach(Entity::discard);h.succeed();
    }
    @GameTest(template="spell_arena") public static void fallImmunityDoesNotBlockOtherDamage(GameTestHelper h){
        var boss=h.spawn(ModEntityTypes.WEAVER_SPIRIT.get(),new BlockPos(5,4,5));
        try{
            h.assertTrue(boss.isInvulnerableTo(boss.damageSources().fall())&&!boss.causeFallDamage(40,1,boss.damageSources().fall())
                    &&!boss.hurt(boss.damageSources().fall(),100)&&boss.getHealth()==1024&&!boss.active(),"Fall damage harmed or provoked the boss");
            h.assertTrue(boss.hurt(boss.damageSources().magic(),8)&&boss.getHealth()==1016,"Fall immunity blocked unrelated damage");
        }finally{boss.discard();}h.succeed();
    }
    @GameTest(template="spell_arena") public static void fireLavaAndSuffocationAreHarmless(GameTestHelper h){
        var boss=h.spawn(ModEntityTypes.WEAVER_SPIRIT.get(),new BlockPos(5,4,5));
        try{
            h.assertTrue(boss.fireImmune(),"Boss type lacks native fire immunity");
            for(var source:List.of(boss.damageSources().inFire(),boss.damageSources().onFire(),boss.damageSources().lava(),boss.damageSources().inWall()))
                h.assertTrue(boss.isInvulnerableTo(source)&&!boss.hurt(source,200)&&boss.getHealth()==1024&&!boss.active(),"Environmental immunity failed: "+source);
            boss.igniteForSeconds(10);boss.tick();
            h.assertTrue(!boss.isOnFire(),"Fire-immune boss still displays burning");
            h.assertTrue(boss.hurt(boss.damageSources().magic(),8)&&boss.getHealth()==1016,"Environmental immunity blocked ordinary magic");
        }finally{boss.discard();}h.succeed();
    }
    @GameTest(template="spell_arena") public static void ancestorBlastDoesNotReloadReplacementAgain(GameTestHelper h){
        var old=OmniraConfig.RELOAD_ENTITY_BLACKLIST.get();var p=BranchAdvancementGameTests.player(h);
        p.moveTo(h.absolutePos(new BlockPos(5,4,1)).getCenter());h.getLevel().addFreshEntity(p);
        var wither=h.spawn(EntityType.WITHER,new BlockPos(5,4,6));wither.setNoAi(true);
        try{
            OmniraConfig.RELOAD_ENTITY_BLACKLIST.set(BuiltInRegistries.ENTITY_TYPE.keySet().stream()
                    .filter(id->!id.toString().equals("omnira:weaver_spirit")&&!id.toString().equals("minecraft:wither")).map(Object::toString).toList());
            var slots=new net.minecraft.world.SimpleContainer(9);
            slots.setItem(0,new net.minecraft.world.item.ItemStack(ModItems.FIRE_MICROCORE.get()));
            slots.setItem(1,new net.minecraft.world.item.ItemStack(ModItems.AIR_MICROCORE.get()));
            slots.setItem(5,new net.minecraft.world.item.ItemStack(ModItems.AMPLIFICATION_STAR.get()));
            slots.setItem(6,new net.minecraft.world.item.ItemStack(ModItems.INFUSION_BUD.get()));
            slots.setItem(2,new net.minecraft.world.item.ItemStack(com.mcmagic.omnira.registry.ModItems.MINIATURE_WORLD.get()));
            var crystal=com.mcmagic.omnira.menu.CrystalProcessingTableMenu.previewCrystal(slots,5,8,2,5,com.mcmagic.omnira.aggregation.AggregationContent.CRYSTAL.get());
            p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,wither.getBoundingBox().getCenter());
            h.assertTrue(com.mcmagic.omnira.spell.SpellCasting.castArquebus(p,crystal.get(ModDataComponents.SPELL_PATTERN),crystal.get(ModDataComponents.SPELL_PAYLOAD),1,
                    com.mcmagic.omnira.item.ArquebusPlugin.ANCESTOR_LAUNCHER),"Authored crystal failed to cast");
            for(int i=0;i<80&&!wither.isRemoved();i++)for(var spell:h.getLevel().getEntitiesOfClass(com.mcmagic.omnira.spell.entity.SpellEntity.class,wither.getBoundingBox().inflate(15)))spell.tick();
            h.assertTrue(wither.isRemoved(),"Actual enhanced infused projectile did not convert wither");
            h.assertTrue(h.getLevel().getEntitiesOfClass(com.mcmagic.omnira.entity.WeaverSpirit.class,wither.getBoundingBox().inflate(15)).size()==1,
                    "Ancestor blast reloaded the new boss back into another creature");
        }finally{
            OmniraConfig.RELOAD_ENTITY_BLACKLIST.set(old);
            h.getLevel().getEntitiesOfClass(com.mcmagic.omnira.entity.WeaverSpirit.class,wither.getBoundingBox().inflate(15)).forEach(Entity::discard);
            h.getLevel().getEntitiesOfClass(com.mcmagic.omnira.spell.entity.SpellEntity.class,wither.getBoundingBox().inflate(15)).forEach(Entity::discard);
            wither.discard();p.discard();
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void defaultBlacklistProtectsBothBlockTargets(GameTestHelper h){
        var p=BranchAdvancementGameTests.player(h);var at=new BlockPos(5,4,5);
        for(var block:List.of(Blocks.BEDROCK,ModBlocks.PURE_SOLIDIFIED_SPACETIME.get())){
            h.setBlock(at,block);
            h.assertTrue(ReloadEffect.blocked(block)&&!ReloadEffect.block(SpellEffect.utility("reload",1,0,1),p,h.absolutePos(at))
                    &&h.getBlockState(at).is(block),"Default blacklisted block transformed");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void createInternalBlocksAreExcludedInBothDirections(GameTestHelper h){
        var old=OmniraConfig.RELOAD_BLOCK_BLACKLIST.get();var defaults=OmniraConfig.RELOAD_BLOCK_BLACKLIST.getDefault();
        var p=BranchAdvancementGameTests.player(h);var at=new BlockPos(5,4,5);var effect=SpellEffect.utility("reload",0,0,1);
        try{
            OmniraConfig.RELOAD_BLOCK_BLACKLIST.set(defaults);
            var internal=List.of("create:wooden_bracket","create:copycat_base","create:metal_bracket","create:water_wheel_structure",
                    "create:copycat_bars","minecraft:frosted_ice","omnira:permanent_reinforced_void_crystal");
            for(var name:internal){
                h.assertTrue(defaults.contains(name),"Missing default Create blacklist entry: "+name);
                var id=net.minecraft.resources.ResourceLocation.parse(name);
                if(!BuiltInRegistries.BLOCK.containsKey(id))continue;
                var block=BuiltInRegistries.BLOCK.get(id);h.setBlock(at,block);
                h.assertTrue(ReloadEffect.blocked(block)&&!ReloadEffect.block(effect,p,h.absolutePos(at))&&h.getBlockState(at).is(block),"Blacklisted Create target transformed: "+name);
            }
            h.setBlock(at,Blocks.STONE);ReloadEffect.block(effect,p,h.absolutePos(at));
            var allowed=new HashSet<String>(internal);allowed.add("minecraft:stone");allowed.add("minecraft:dirt");
            var blacklist=new ArrayList<String>(defaults);
            BuiltInRegistries.BLOCK.keySet().stream().map(Object::toString).filter(id->!allowed.contains(id)).forEach(blacklist::add);
            OmniraConfig.RELOAD_BLOCK_BLACKLIST.set(blacklist);h.setBlock(at,Blocks.STONE);
            h.assertTrue(ReloadEffect.block(effect,p,h.absolutePos(at))&&h.getBlockState(at).is(Blocks.DIRT),"Cached reload results included a blacklisted Create block");
        }finally{OmniraConfig.RELOAD_BLOCK_BLACKLIST.set(old);}h.succeed();
    }
    @GameTest(template="spell_arena") public static void bossRequiresEnhancedInfusedReload(GameTestHelper h){
        var wither=EntityType.WITHER.create(h.getLevel());
        var plain=ReloadEffect.livingCandidates(SpellEffect.utility("reload",0,0,1),wither);
        var enhanced=ReloadEffect.livingCandidates(SpellEffect.utility("reload",1,0,0),wither);
        var infused=ReloadEffect.livingCandidates(SpellEffect.utility("reload",1,0,1),wither);
        h.assertTrue(plain.isEmpty()&&!enhanced.contains(ModEntityTypes.WEAVER_SPIRIT.get())&&infused.contains(ModEntityTypes.WEAVER_SPIRIT.get()),"Boss bypasses enhancement or infusion rules");
        h.assertTrue(enhanced.contains(EntityType.ENDER_DRAGON)&&infused.contains(EntityType.ENDER_DRAGON),"Dragon missing from enhanced boss pools");
        wither.discard();h.succeed();
    }
    @GameTest(template="spell_arena") public static void configChangesFilterCachedPoolsAndConversionWorks(GameTestHelper h){
        var old=OmniraConfig.RELOAD_ENTITY_BLACKLIST.get();
        var wither=h.spawn(EntityType.WITHER,new BlockPos(5,4,5));var effect=SpellEffect.utility("reload",1,0,1);
        try{
            ReloadEffect.livingCandidates(effect,wither);
            OmniraConfig.RELOAD_ENTITY_BLACKLIST.set(BuiltInRegistries.ENTITY_TYPE.keySet().stream()
                    .filter(id->!id.toString().equals("omnira:weaver_spirit")&&!id.toString().equals("minecraft:wither")).map(Object::toString).toList());
            h.assertTrue(ReloadEffect.livingCandidates(effect,wither).equals(List.of(ModEntityTypes.WEAVER_SPIRIT.get())),"Blacklist does not filter cached results");
            wither.setHealth(wither.getMaxHealth()/2);
            h.assertTrue(ReloadEffect.living(effect,wither)&&wither.isRemoved(),"Boss was not reachable through the actual reload conversion");
            var bosses=h.getLevel().getEntitiesOfClass(com.mcmagic.omnira.entity.WeaverSpirit.class,
                    new net.minecraft.world.phys.AABB(h.absolutePos(new BlockPos(2,2,2)).getCenter(),h.absolutePos(new BlockPos(9,9,9)).getCenter()));
            h.assertTrue(bosses.size()==1&&bosses.getFirst().getHealth()==1024,"Replacement Weaver Spirit did not spawn at full health");
            OmniraConfig.RELOAD_ENTITY_BLACKLIST.set(List.of("omnira:weaver_spirit"));
            h.assertTrue(!ReloadEffect.living(effect,bosses.getFirst())&&!bosses.getFirst().isRemoved(),"Custom source blacklist ignored");
            bosses.forEach(Entity::discard);
        }finally{OmniraConfig.RELOAD_ENTITY_BLACKLIST.set(old);wither.discard();}
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void blockResultsRespectConfigAfterPoolCreation(GameTestHelper h){
        var old=OmniraConfig.RELOAD_BLOCK_BLACKLIST.get();var p=BranchAdvancementGameTests.player(h);var at=new BlockPos(5,4,5);
        try{
            h.setBlock(at,Blocks.STONE);ReloadEffect.block(SpellEffect.utility("reload",0,0,1),p,h.absolutePos(at));
            OmniraConfig.RELOAD_BLOCK_BLACKLIST.set(BuiltInRegistries.BLOCK.keySet().stream()
                    .filter(id->!id.toString().equals("minecraft:stone")&&!id.toString().equals("minecraft:dirt")).map(Object::toString).toList());
            h.setBlock(at,Blocks.STONE);
            h.assertTrue(ReloadEffect.block(SpellEffect.utility("reload",0,0,1),p,h.absolutePos(at))&&h.getBlockState(at).is(Blocks.DIRT),"Cached block candidates bypass blacklist");
            OmniraConfig.RELOAD_BLOCK_BLACKLIST.set(List.of("minecraft:dirt"));
            h.assertTrue(!ReloadEffect.block(SpellEffect.utility("reload",0,0,1),p,h.absolutePos(at))&&h.getBlockState(at).is(Blocks.DIRT),"Custom target blacklist ignored");
        }finally{OmniraConfig.RELOAD_BLOCK_BLACKLIST.set(old);}
        h.succeed();
    }
}
