package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.spell.*;
import com.mcmagic.omnira.spell.entity.SpellEntity;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.gametest.*;
import java.util.List;

@GameTestHolder("omnira_dragon_spell")
@PrefixGameTestTemplate(false)
public final class DragonSpellGameTests {
    @GameTest(template="spell_arena")
    public static void enhancedReloadConvertsWitherAndDragonThroughPart(GameTestHelper h) {
        var caster=player(h);
        var level=h.getLevel();
        var wither=h.spawn(EntityType.WITHER,6,48,6);
        var area=wither.getBoundingBox().inflate(2);
        var effect=SpellEffect.utility("reload",1,0,0);
        try {
            wither.setHealth(wither.getMaxHealth()/2);
            var pillar=wither.blockPosition().offset(5,1,0);
            level.setBlockAndUpdate(pillar,net.minecraft.world.level.block.Blocks.OBSIDIAN.defaultBlockState());
            SpellCasting.burst(level,caster,1,new SpellPayload(0,0,List.of(effect)),wither.getBoundingBox().getCenter(),true);
            var dragons=level.getEntitiesOfClass(net.minecraft.world.entity.boss.enderdragon.EnderDragon.class,area);
            h.assertTrue(wither.isRemoved() && dragons.size()==1,"Enhanced vanilla reload did not create a dragon");
            var dragon=dragons.getFirst();
            h.assertTrue(dragon.getHealth()==dragon.getMaxHealth(),"Replacement dragon did not spawn at full health");
            dragon.setHealth(1);
            for(var part:dragon.getSubEntities())part.setPos(dragon.position());
            var contact=dragon.head.getBoundingBox().getCenter();
            var shot=SpellEntity.spawn(level,caster,SpellEntity.Kind.PROJECTILE,contact);
            shot.configure(1,new SpellPayload(0,0,List.of(effect)));
            shot.tick();
            var results=level.getEntitiesOfClass(net.minecraft.world.entity.boss.wither.WitherBoss.class,area);
            h.assertTrue(dragon.isRemoved() && results.size()==1,"Projectile striking dragon parts did not produce one wither");
            h.assertTrue(results.getFirst().getHealth()==results.getFirst().getMaxHealth(),"Replacement wither did not spawn at full health");
        } finally {
            for(var entity:level.getEntities(caster,area,e->e instanceof net.minecraft.world.entity.Mob || e instanceof SpellEntity))entity.discard();
            caster.discard();
        }
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void weaverBoltsHitDragonPartsTwice(GameTestHelper h) {
        var level=h.getLevel();
        var caster=player(h);
        var boss=h.spawn(com.mcmagic.omnira.registry.ModEntityTypes.WEAVER_SPIRIT.get(),11,8,11);
        var dragon=h.spawn(EntityType.ENDER_DRAGON,6,8,6);
        try {
            for(var owner:List.of(caster,boss)){
                for(var part:dragon.getSubEntities()){
                    if(part!=dragon.head && !part.name.equals("body"))continue;
                    for(var other:dragon.getSubEntities())other.setPos(dragon.position().add(0,12,0));
                    part.setPos(dragon.position());
                    var box=part.getBoundingBox();
                    var start=new net.minecraft.world.phys.Vec3(box.minX-1,box.getCenter().y,box.getCenter().z);
                    dragon.setHealth(200);
                    dragon.invulnerableTime=20;
                    var bolt=com.mcmagic.omnira.weaver.WeaverCombat.bolt(owner,null,start,new net.minecraft.world.phys.Vec3(1,0,0));
                    for(int i=0;i<4&&!bolt.isRemoved();i++)bolt.tick();
                    float expected=part==dragon.head?194:196.5F;
                    h.assertTrue(bolt.isRemoved()&&Math.abs(dragon.getHealth()-expected)<.001,
                            "Wrong crystal bolt damage for "+owner.getType()+" / "+part.name+": "+dragon.getHealth());
                }
            }
        } finally {dragon.discard();boss.discard();caster.discard();}
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void heldBookBoostsBothDragonHits(GameTestHelper h) {
        var level=h.getLevel();
        var player=player(h);
        var dragon=h.spawn(EntityType.ENDER_DRAGON,6,8,6);
        var book=com.mcmagic.omnira.registry.ModItems.FOUNDER_APPROVAL.get();
        var hits=new java.util.ArrayList<Float>();
        java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post> capture=e->{
            if(e.getEntity()==dragon)hits.add(e.getNewDamage());
        };
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(capture);
        try {
            for(var hand:net.minecraft.world.InteractionHand.values()){
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,net.minecraft.world.item.ItemStack.EMPTY);
                player.setItemInHand(hand,new net.minecraft.world.item.ItemStack(book));
                for(var part:dragon.getSubEntities()){
                    if(part!=dragon.head && !part.name.equals("body"))continue;
                    for(var other:dragon.getSubEntities())other.setPos(dragon.position().add(0,12,0));
                    part.setPos(dragon.position());
                    var box=part.getBoundingBox();
                    if(part!=dragon.head)dragon.head.setPos(box.maxX+3,box.getCenter().y-.5,box.getCenter().z);
                    player.setPos(box.minX-3,box.getCenter().y-player.getEyeHeight(),box.getCenter().z);
                    player.setYRot(-90);player.setXRot(0);
                    dragon.setHealth(200);dragon.invulnerableTime=20;hits.clear();
                    book.use(level,player,hand);
                    var bolts=level.getEntitiesOfClass(com.mcmagic.omnira.weaver.WeaverBolt.class,box.inflate(5),b->b.getOwner()==player);
                    h.assertTrue(bolts.size()==1,"Expected one right-click projectile");
                    var bolt=bolts.getFirst();
                    for(int i=0;i<10&&!bolt.isRemoved();i++)bolt.tick();
                    float expected=part==dragon.head?5:3.75F;
                    h.assertTrue(hits.equals(List.of(expected,expected)) && dragon.getHealth()==200-2*expected,
                            "Held bonus on "+part.name+" with "+hand+": "+hits+" health="+dragon.getHealth());
                    for(int i=0;i<20;i++)com.mcmagic.omnira.weaver.FounderApprovalItem.advance(player);
                }
            }
        } finally {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(capture);
            dragon.discard();player.discard();
        }
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void compareRaysAcrossDragonParts(GameTestHelper h){
        var player=player(h);
        var dragon=h.spawn(EntityType.ENDER_DRAGON,8,12,8);
        var start=dragon.position();
        try {
            player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE).setBaseValue(20);
            player.setPos(start.x,start.y-player.getEyeHeight(),start.z);
            player.setYRot(-90);player.setXRot(0);
            for(var p:dragon.getSubEntities())p.setPos(start.add(0,10,0));
            var body=java.util.Arrays.stream(dragon.getSubEntities()).filter(p->p.name.equals("body")).findFirst().orElseThrow();
            var tail=java.util.Arrays.stream(dragon.getSubEntities()).filter(p->p.name.equals("tail")).findFirst().orElseThrow();
            dragon.head.setPos(start.add(3,-.5,0));body.setPos(start.add(8,-1,0));tail.setPos(start.add(12,-1,0));
            var book=com.mcmagic.omnira.registry.ModItems.FOUNDER_APPROVAL.get();
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(book));
            player.setShiftKeyDown(true);
            book.use(h.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND);
            h.assertTrue(dragon.getHealth()==187.5F,"Held ray should hit head for 5 and body and tail for 3.75 each: "+dragon.getHealth());
            player.setShiftKeyDown(false);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,net.minecraft.world.item.ItemStack.EMPTY);
            var pattern=new SpellPattern(ElementType.AIR,null);
            var payload=new SpellPayload(0,8);
            for(int i=0;i<2;i++){
                dragon.invulnerableTime=0;dragon.setHealth(200);
                SpellCasting.cast(player,pattern,payload,1);
                float expected=i==0?192:197;
                h.assertTrue(dragon.getHealth()==expected,"Single-core ray hit wrong part: "+dragon.getHealth());
                SpellCasting.cast(player,pattern,payload,1);
                h.assertTrue(dragon.getHealth()==expected,"Repeated spell ignored hurt cooldown");
                dragon.head.setPos(start.add(13,-.5,0));
            }
            var nearer=h.spawn(EntityType.COW,8,12,8);
            nearer.setPos(start.add(2,-.5,0));
            float before=nearer.getHealth();
            dragon.setHealth(200);dragon.invulnerableTime=0;
            SpellCasting.cast(player,pattern,payload,1);
            h.assertTrue(nearer.getHealth()<before && dragon.getHealth()==200,"Wind ray skipped the nearer creature");
            nearer.discard();
            var lock=new SpellPayload(0,0,List.of(SpellEffect.utility("construction",1,0,1)));
            SpellCasting.cast(player,pattern,lock,1);
            h.assertTrue(!ConstructionLock.locked(dragon)&&!dragon.isNoAi(),"Dragon part bypassed potion immunity");
        } finally {dragon.discard();player.discard();}
        h.succeed();
    }
    private static net.neoforged.neoforge.common.util.FakePlayer player(GameTestHelper h) {
        var player=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"dragon-spell-test"));
        player.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(2,2,2)));
        h.getLevel().addNewPlayer(player);
        return player;
    }
    @GameTest(template="spell_arena")
    public static void dragonPartsReceivePhysicalSpellDamage(GameTestHelper h) {
        var player=player(h);
        var dragon=h.spawn(EntityType.ENDER_DRAGON,5,4,5);
        var spell=SpellEntity.spawn(h.getLevel(),player,SpellEntity.Kind.PROJECTILE,dragon.position());
        spell.configure(1,new SpellPayload(0,0,List.of(new SpellEffect(false,1,0))));
        var body=java.util.Arrays.stream(dragon.getSubEntities()).filter(p->p.name.equals("body")).findFirst().orElseThrow();
        h.assertTrue(SpellCasting.validTarget(dragon.head) && SpellCasting.validTarget(body),"Dragon parts excluded from targeting");
        spell.applyEffects(dragon.head);
        h.assertTrue(Math.abs(dragon.getHealth()-194)<.01,"Head did not receive six physical damage");
        dragon.invulnerableTime=0;
        spell.applyEffects(body);
        h.assertTrue(Math.abs(dragon.getHealth()-191.5)<.01,"Body damage reduction lost");
        dragon.discard();spell.discard();player.discard();h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void projectileContactAndWardAttachment(GameTestHelper h) {
        var player=player(h);
        var dragon=h.spawn(EntityType.ENDER_DRAGON,5,4,5);
        var center=dragon.position();
        for(var part:dragon.getSubEntities()) part.setPos(center.add(8,0,0));
        dragon.head.setPos(center);
        var projectile=SpellEntity.spawn(h.getLevel(),player,SpellEntity.Kind.PROJECTILE,dragon.head.getBoundingBox().getCenter());
        projectile.configure(1,new SpellPayload(0,0,List.of(new SpellEffect(false,1,0))));
        projectile.tick();
        h.assertTrue(projectile.isRemoved() && dragon.getHealth()==194,"Projectile contact did not damage dragon head");
        var ward=SpellEntity.spawn(h.getLevel(),player,SpellEntity.Kind.WARD,center);
        ward.follow(dragon.head);
        h.assertTrue(ward.follows(dragon),"Ward attached to part instead of living bearer");
        dragon.discard();ward.discard();player.discard();h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void burstDoesNotRepeatEffectsAcrossDragonParts(GameTestHelper h) {
        var player=player(h);
        var dragon=h.spawn(EntityType.ENDER_DRAGON,5,4,5);
        var center=dragon.position();
        for(var part:dragon.getSubEntities()) part.setPos(center);
        // Healing has no hurt cooldown, so duplicate part application would be observable.
        dragon.setHealth(100);
        SpellCasting.burst(h.getLevel(),player,1,new SpellPayload(0,0,List.of(new SpellEffect(true,1,0))),center,true);
        h.assertTrue(Math.abs(dragon.getHealth()-108)<.01,"Burst applied healing more or less than once");
        dragon.discard();player.discard();h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void spellPowerScalesPotionAmountsAndLevels(GameTestHelper h) {
        var player=player(h);
        var cow=h.spawn(EntityType.COW,4,2,4);
        cow.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(40);
        cow.setHealth(4);
        new SpellEffect(true,1,0).apply(player,player,cow,1.5);
        h.assertTrue(cow.getHealth()==16,"Healing II should restore 12 at 150% power");
        cow.setHealth(40);cow.invulnerableTime=0;
        new SpellEffect(false,1,0,true).apply(player,player,cow,1.5);
        h.assertTrue(cow.getHealth()==22,"Harming II should deal 18 at 150% power");
        new SpellEffect(true,0,800).apply(player,player,cow,1.5);
        h.assertTrue(cow.getEffect(MobEffects.REGENERATION).getDuration()==800 && cow.getEffect(MobEffects.REGENERATION).getAmplifier()==1,"Regeneration must scale level, not duration");
        new SpellEffect(false,0,800).apply(player,player,cow,1.5);
        h.assertTrue(cow.getEffect(MobEffects.POISON).getDuration()==800 && cow.getEffect(MobEffects.POISON).getAmplifier()==1,"Physical poison conversion must use damage and preserve duration");
        h.assertTrue(new SpellEffect(true,0,800).sustainedAmplifier(1)==0,"Base regeneration lost its original level gap");
        h.assertTrue(new SpellEffect(true,0,800).sustainedAmplifier(2)==1,"Exact doubling threshold rounded too high");
        h.assertTrue(new SpellEffect(true,0,800).sustainedAmplifier(2.1)==2,"Healing amount did not round up to the next tier");
        h.assertTrue(new SpellEffect(false,2,800).sustainedAmplifier(1)==1,"Physical enhanced damage should map 12 damage to Poison II");
        h.assertTrue(new SpellEffect(false,2,800,true).sustainedAmplifier(1)==2,"Infused damage lost its level gap");
        h.assertTrue(new SpellEffect(true,0,800).sustainedAmplifier(.1)==0,"Low power created a negative amplifier");
        cow.removeEffect(MobEffects.REGENERATION);
        new SpellEffect(true,0,800,true).apply(player,player,cow,1.5);
        h.assertTrue(cow.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier()==1 && cow.getEffect(MobEffects.DAMAGE_RESISTANCE).getDuration()==800,"Resistance did not follow regeneration level and duration");
        var zombie=h.spawn(EntityType.ZOMBIE,6,2,6);zombie.setHealth(4);
        new SpellEffect(false,1,0,true).apply(player,player,zombie,1.5);
        h.assertTrue(zombie.getHealth()==16,"Undead harm-to-heal inversion lost");
        cow.discard();zombie.discard();player.discard();h.succeed();
    }
}
