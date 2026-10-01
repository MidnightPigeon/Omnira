package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.entity.DreamMirror;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.spell.entity.SpellEntity;
import com.mcmagic.omnira.spell.SpellPayload;
import com.mcmagic.omnira.spell.SpellEffect;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_dream_mirror")
@PrefixGameTestTemplate(false)
public final class DreamMirrorGameTests {
    @GameTest(template="spell_arena")
    public static void reflectionKeepsNonphysicalSpellEffects(GameTestHelper h) {
        var mirror=mirror(h);
        h.getLevel().addFreshEntity(mirror);
        var caster=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"mirror-effects"));
        caster.setPos(mirror.position().add(0,0,5));h.getLevel().addFreshEntity(caster);
        var lock=SpellEntity.spawn(h.getLevel(),caster,SpellEntity.Kind.PROJECTILE,mirror.position());
        lock.configure(1,new SpellPayload(20,0,java.util.List.of(SpellEffect.utility("construction",0,0,0))));
        lock.setDeltaMovement(0,0,-.3);
        h.assertTrue(lock.reflectFrom(mirror) && lock.ownedBy(mirror),"Construction projectile did not bounce");
        h.assertTrue(com.mcmagic.omnira.spell.ConstructionLock.locked(mirror),"Reflected construction did not lock mirror");
        h.assertTrue(mirror.getHealth()==30,"Construction gained unintended damage");
        com.mcmagic.omnira.spell.ConstructionLock.release(mirror);lock.discard();
        var magic=SpellEntity.spawn(h.getLevel(),caster,SpellEntity.Kind.PROJECTILE,mirror.position());
        magic.configure(1,new SpellPayload(20,0,java.util.List.of(new SpellEffect(false,1,0,true))));
        magic.setDeltaMovement(0,0,-.3);
        h.assertTrue(magic.reflectFrom(mirror) && magic.ownedBy(mirror),"Magic projectile did not bounce");
        h.assertTrue(mirror.getHealth()==18 && mirror.getTarget()==caster,"Magic damage or original caster attribution lost");
        h.assertTrue(!magic.reflectFrom(mirror) && mirror.getHealth()==18,"One contact applied effects twice");
        magic.discard();mirror.discard();caster.discard();h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void mirrorIslandSurface(GameTestHelper h) {
        var p=h.absolutePos(new net.minecraft.core.BlockPos(8,2,8));
        for(int y=1;y<=8;y++) h.getLevel().setBlockAndUpdate(p.above(y),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        h.getLevel().setBlockAndUpdate(p,com.mcmagic.omnira.registry.DreamContent.MIRROR_ROCK.get().defaultBlockState());
        for(int y=1;y<=6;y++) h.assertTrue(DreamMirror.hasSpawnSurface(h.getLevel(),p.above(y)),"Mirror surface rejected at height "+y);
        h.assertTrue(!DreamMirror.hasSpawnSurface(h.getLevel(),p.above(7)),"Spawn surface search too deep");
        h.getLevel().setBlockAndUpdate(p,com.mcmagic.omnira.registry.DreamContent.DREAM_CRYSTAL_BEDROCK.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(p.above(),net.minecraft.world.level.block.Blocks.AMETHYST_CLUSTER.defaultBlockState());
        h.assertTrue(DreamMirror.hasSpawnSurface(h.getLevel(),p.above(2)),"Crystal growth hid valid surface");
        h.getLevel().setBlockAndUpdate(p,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        h.assertTrue(!DreamMirror.hasSpawnSurface(h.getLevel(),p.above(2)),"Ordinary stone accepted");
        h.succeed();
    }
    private static DreamMirror mirror(GameTestHelper h) {
        var mirror=ModEntityTypes.DREAM_MIRROR.get().create(h.getLevel());
        mirror.setPos(h.absoluteVec(new Vec3(5,3,5)));mirror.setYRot(0);return mirror;
    }
    private static net.minecraft.world.entity.player.Player player(GameTestHelper h,DreamMirror mirror) {
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setPos(mirror.position().add(0,0,10));
        h.getLevel().addFreshEntity(player);return player;
    }
    @GameTest(template="spell_arena")
    public static void physicalAndMagic(GameTestHelper h) {
        var mirror=mirror(h);var player=player(h,mirror);
        h.assertTrue(mirror.getMaxHealth()==30,"Wrong health");
        float health=player.getHealth();
        mirror.hurt(h.getLevel().damageSources().playerAttack(player),6);
        h.assertTrue(mirror.getHealth()==30 && player.getHealth()==health-6,"Physical damage was not reflected: mirror="+mirror.getHealth()+" player="+player.getHealth()+" original="+health);
        h.assertTrue(mirror.getTarget()==null,"Physical attack provoked the mirror");
        mirror.hurt(h.getLevel().damageSources().indirectMagic(player,player),6);
        h.assertTrue(mirror.getHealth()==24,"Magic was reflected or blocked");
        h.assertTrue(mirror.getTarget()==player,"Actual magic damage did not provoke the mirror");
        var other=mirror(h);
        other.hurt(h.getLevel().damageSources().mobAttack(mirror),5);
        h.assertTrue(other.getHealth()==30 && mirror.getHealth()==24,"Mirror recursion damaged either mirror");
        player.discard();h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void copiesOnlyEffects(GameTestHelper h) {
        var mirror=mirror(h);var player=player(h,mirror);
        player.getAttribute(Attributes.ARMOR).setBaseValue(12);
        player.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(4);
        player.getAttribute(ModAttributes.SPELL_POWER).setBaseValue(1.7);
        mirror.provoke(player);mirror.updateCopies();
        h.assertTrue(mirror.getArmorValue()==12 && mirror.getAttributeValue(Attributes.ARMOR_TOUGHNESS)==4
                && Math.abs(mirror.getAttributeValue(ModAttributes.SPELL_POWER)-1.7)<.001,"Copy failed");
        for(var slot:net.minecraft.world.entity.EquipmentSlot.values()) h.assertTrue(mirror.getItemBySlot(slot).isEmpty(),"Copied physical equipment");
        mirror.setYRot(180);mirror.updateCopies();
        h.assertTrue(mirror.getArmorValue()==0 && mirror.getAttributeValue(ModAttributes.SPELL_POWER)==1,"Back retained copied bonuses");
        player.discard();h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void orbitIsContactOnlyAndReflectable(GameTestHelper h) {
        var mirror=mirror(h);var player=player(h,mirror);
        var center=h.spawn(net.minecraft.world.entity.EntityType.COW,11,3,11);center.setNoAi(true);
        var orb=SpellEntity.spawn(h.getLevel(),player,SpellEntity.Kind.ORB,center.position());
        orb.configure(1,new SpellPayload(20,0,java.util.List.of(new SpellEffect(false,1,0))));
        orb.orbit(center.getBoundingBox().getCenter(),0,java.util.List.of());orb.follow(center);
        float health=center.getHealth();orb.tick();h.assertTrue(center.getHealth()==health&&!orb.isRemoved(),"Orbit applied effect to its center");
        var contact=h.spawn(net.minecraft.world.entity.EntityType.COW,3,3,3);contact.setPos(orb.position());
        float before=contact.getHealth();orb.tick();h.assertTrue(contact.getHealth()==before-6&&orb.isRemoved(),"Orbit contact did not trigger exactly once");
        h.getLevel().addFreshEntity(mirror);
        var incoming=SpellEntity.spawn(h.getLevel(),player,SpellEntity.Kind.ORB,mirror.position());
        incoming.configure(1,new SpellPayload(20,0,java.util.List.of(new SpellEffect(false,1,0))));
        incoming.orbit(mirror.position().add(-1.2,.5,0),0,java.util.List.of());
        incoming.tick();
        h.assertTrue(!incoming.isRemoved()&&incoming.kind()==SpellEntity.Kind.PROJECTILE&&incoming.ownedBy(mirror),"Orbit reflection was consumed instead of returned");
        h.assertTrue(incoming.getDeltaMovement().lengthSqr()>0&&mirror.getHealth()==30&&mirror.getTarget()==null,"Reflected orbit lacks motion or harmed mirror");
        var motion=incoming.getDeltaMovement();var pos=incoming.position();incoming.tick();
        h.assertTrue(incoming.position().distanceTo(pos.add(motion))<1E-6,"Reflected orb resumed old orbit");
        incoming.discard();player.discard();mirror.discard();h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void projectileAndPassengerReflection(GameTestHelper h) {
        var mirror=mirror(h);var player=player(h,mirror);
        var spell=SpellEntity.spawn(h.getLevel(),player,SpellEntity.Kind.PROJECTILE,mirror.position().add(0,.9,1));
        spell.setDeltaMovement(0,0,-.3);spell.home(mirror,mirror.getBoundingBox().getCenter());
        h.assertTrue(spell.reflectFrom(mirror) && spell.ownedBy(mirror) && spell.follows(player)
                && spell.getDeltaMovement().z>.29,"Homing reflection failed");
        h.assertTrue(!spell.reflectFrom(mirror),"Reflected twice on one contact");spell.discard();
        var flight=SpellEntity.spawn(h.getLevel(),player,SpellEntity.Kind.SELF_FLIGHT,player.position());
        player.startRiding(flight,true);Vec3 motion=new Vec3(.12,.18,-.2);flight.setDeltaMovement(motion);
        h.assertTrue(flight.reflectFrom(mirror) && player.getVehicle()==flight && flight.ownedBy(player)
                && flight.getDeltaMovement().distanceTo(motion.scale(-1))<1E-6,"Passenger lost or trajectory changed: vehicle="+player.getVehicle()+" motion="+flight.getDeltaMovement()+" owner="+flight.ownedBy(player));
        flight.discard();
        var block=SpellEntity.spawn(h.getLevel(),player,SpellEntity.Kind.FLYING_BLOCK,player.position());
        block.setDeltaMovement(motion);
        h.assertTrue(block.reflectFrom(mirror) && block.ownedBy(mirror) && block.getDeltaMovement().distanceTo(motion.scale(-1))<1E-6,"Flying block was not reflected");
        h.assertTrue(mirror.getTarget()==null && mirror.getHealth()==30,"Projectile reflection provoked or hurt the mirror");
        block.discard();player.discard();h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void sweptCollision(GameTestHelper h) {
        var mirror=mirror(h);h.getLevel().addFreshEntity(mirror);mirror.setNoAi(true);
        var player=player(h,mirror);
        var spell=SpellEntity.spawn(h.getLevel(),player,SpellEntity.Kind.PROJECTILE,mirror.position().add(0,.9,1.2));
        spell.setDeltaMovement(0,0,-.3);
        for(int i=0;i<5 && !spell.ownedBy(mirror);i++) spell.tick();
        h.assertTrue(spell.isAlive() && spell.ownedBy(mirror) && spell.getDeltaMovement().z>0,"Collision consumed projectile instead of reflecting");
        spell.discard();mirror.discard();player.discard();h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=100)
    public static void attackAndPotionTiming(GameTestHelper h) {
        var mirror=mirror(h);var player=player(h,mirror);mirror.provoke(player);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION,1200,1));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,900,2));
        for(int i=0;i<99;i++) {mirror.tickCount++;mirror.tick();}
        h.assertTrue(h.getLevel().getEntitiesOfClass(SpellEntity.class,mirror.getBoundingBox().inflate(40),s->s.ownedBy(mirror)).isEmpty(),"Shot before five seconds");
        mirror.tickCount++;mirror.tick();
        h.assertTrue(h.getLevel().getEntitiesOfClass(SpellEntity.class,mirror.getBoundingBox().inflate(40),s->s.ownedBy(mirror)).size()==1,"Missing first shot");
        for(int i=100;i<599;i++) {mirror.tickCount++;mirror.tick();}
        h.assertTrue(!mirror.hasEffect(MobEffects.REGENERATION),"Copied effects before thirty seconds");
        mirror.tickCount++;mirror.tick();
        for(var effect:player.getActiveEffects()) {
            var copied=mirror.getEffect(effect.getEffect());
            h.assertTrue(copied!=null && copied.getAmplifier()==effect.getAmplifier() && Math.abs(copied.getDuration()-effect.getDuration())<=1,"Potion duration/amplifier changed");
        }
        for(var spell:h.getLevel().getEntitiesOfClass(SpellEntity.class,mirror.getBoundingBox().inflate(40),s->s.ownedBy(mirror))) spell.discard();
        player.discard();h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void protectionWithoutEquipment(GameTestHelper h) {
        var mirror=mirror(h);var player=player(h,mirror);
        var armor=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_CHESTPLATE);
        armor.enchant(h.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.PROTECTION),4);
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,armor);
        mirror.provoke(player);mirror.updateCopies();
        mirror.hurt(h.getLevel().damageSources().indirectMagic(player,player),10);
        h.assertTrue(Math.abs(mirror.getHealth()-21.6F)<.02,"Protection did not reduce magic: "+mirror.getHealth());
        h.assertTrue(mirror.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).isEmpty(),"Armor became extractable");
        player.discard();h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void vanillaProjectileReflection(GameTestHelper h) {
        var mirror=mirror(h);var player=player(h,mirror);
        var arrow=new net.minecraft.world.entity.projectile.Arrow(net.minecraft.world.entity.EntityType.ARROW,h.getLevel());
        arrow.setOwner(player);arrow.setPos(mirror.getEyePosition());arrow.setDeltaMovement(.1,.2,-.3);
        arrow.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,120,1));
        h.assertTrue(com.mcmagic.omnira.event.MirrorProjectileEvents.bounce(arrow,mirror,arrow.position())
                && arrow.getOwner()==mirror && arrow.getDeltaMovement().distanceTo(new Vec3(-.1,-.2,.3))<1E-6,"Arrow not reversed");
        var bullet=new net.minecraft.world.entity.projectile.ShulkerBullet(h.getLevel(),player,mirror,net.minecraft.core.Direction.Axis.Y);
        bullet.setPos(mirror.getEyePosition());bullet.setDeltaMovement(0,0,-.3);h.getLevel().addFreshEntity(bullet);
        h.assertTrue(com.mcmagic.omnira.event.MirrorProjectileEvents.bounce(bullet,mirror,bullet.position()) && bullet.isRemoved(),"Homing bullet not replaced");
        var reflected=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.projectile.ShulkerBullet.class,mirror.getBoundingBox().inflate(3));
        h.assertTrue(reflected.size()==1 && reflected.getFirst().getOwner()==mirror,"Homing bullet duplicated");
        h.assertTrue(mirror.getTarget()==null && mirror.getHealth()==30,"Vanilla reflection provoked or hurt the mirror");
        h.assertTrue(mirror.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && mirror.hasEffect(MobEffects.LEVITATION),
                "Reflection stripped arrow/bullet secondary effects");
        reflected.forEach(net.minecraft.world.entity.Entity::discard);player.discard();h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void physicalProjectileDamageFallback(GameTestHelper h) {
        var mirror=mirror(h);var player=player(h,mirror);
        var arrow=new net.minecraft.world.entity.projectile.Arrow(net.minecraft.world.entity.EntityType.ARROW,h.getLevel());
        arrow.setOwner(player);arrow.setPos(mirror.getEyePosition());arrow.setDeltaMovement(0,0,-.3);
        mirror.hurt(h.getLevel().damageSources().arrow(arrow,player),6);
        h.assertTrue(mirror.getHealth()==30 && mirror.getTarget()==null && player.getHealth()==14,"Physical projectile did not return its incoming damage immediately");
        h.assertTrue(arrow.getOwner()==mirror && arrow.getDeltaMovement().z>.29,"Damage fallback did not reverse projectile");
        mirror.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,100),player);
        h.assertTrue(mirror.getTarget()==null,"Non-damaging effect provoked mirror");
        player.discard();h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void trading(GameTestHelper h) {
        var mirror=mirror(h);var player=player(h,mirror);
        var hand=net.minecraft.world.InteractionHand.MAIN_HAND;
        player.setItemInHand(hand,new net.minecraft.world.item.ItemStack(ModItems.DREAM_CRYSTAL_SHARD.get(),2));
        h.assertTrue(mirror.mobInteract(player,hand).consumesAction(),"Offering not accepted");
        h.assertTrue(player.getMainHandItem().getCount()==1 && mirror.getTarget()==null,"Offering consumption or neutrality incorrect");
        long rewards=player.getInventory().items.stream().filter(s->!s.isEmpty() && !s.is(ModItems.DREAM_CRYSTAL_SHARD.get())).count();
        h.assertTrue(rewards==1,"Trade did not yield exactly one reward kind");
        player.getAbilities().instabuild=true;
        var offering=player.getMainHandItem();
        mirror.mobInteract(player,hand);
        h.assertTrue(offering.isEmpty() && !player.getMainHandItem().is(ModItems.DREAM_CRYSTAL_SHARD.get()),"Creative offering not consumed");
        player.discard();h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void tradePool(GameTestHelper h) {
        var mirror=mirror(h);
        var table=h.getLevel().getServer().reloadableRegistries().getLootTable(DreamMirror.TRADE_LOOT);
        var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(h.getLevel())
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,mirror.position())
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY,mirror)
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.GIFT);
        var counts=new java.util.HashMap<net.minecraft.world.item.Item,Integer>();
        for(int i=0;i<4000;i++) {
            var rewards=table.getRandomItems(params);
            h.assertTrue(rewards.size()==1,"Trade rolled multiple rewards or none");
            var stack=rewards.getFirst();
            h.assertTrue(stack.getCount()==(stack.is(ModItems.SPIRITUAL_CRYSTAL.get())?16:1),"Wrong reward count");
            counts.merge(stack.getItem(),1,Integer::sum);
        }
        h.assertTrue(counts.size()==10,"Missing or unexpected reward type");
        int mothers=counts.getOrDefault(DreamContent.DREAM_CRYSTAL_BEDROCK.get().asItem(),0);
        int cores=counts.getOrDefault(ModItems.TEST_SPELL_CORE.get(),0);
        int grids=counts.getOrDefault(ModItems.ARCANE_CRYSTAL_GRID.get(),0);
        h.assertTrue(mothers>450 && mothers<750 && cores>270 && cores<530 && grids>100 && grids<300,"Reward probabilities outside expected ranges");
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void offeringAttraction(GameTestHelper h) {
        var mirror=mirror(h);
        var player=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"mirror-offering"));
        player.setPos(mirror.position().add(0,0,10));
        // Register only for the nearest-player query, without a network login or ticking fake connection.
        h.getLevel().players().add(player);
        try {
            player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,new net.minecraft.world.item.ItemStack(ModItems.DREAM_CRYSTAL_SHARD.get()));
            mirror.tickCount=10;mirror.tick();
            h.assertTrue(mirror.getMoveControl().hasWanted() && Math.abs(mirror.getMoveControl().getWantedZ()-player.getZ())<.01
                    && mirror.getTarget()==null,"Offhand crystal did not attract peacefully");
            player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,net.minecraft.world.item.ItemStack.EMPTY);
            mirror.tickCount=20;mirror.tick();
            h.assertTrue(!mirror.getMoveControl().hasWanted(),"Mirror kept following after crystal was put away");
        } finally {h.getLevel().players().remove(player);}
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void homingPhysicalDoubleDamage(GameTestHelper h) {
        var mirror=mirror(h);h.getLevel().addFreshEntity(mirror);mirror.setNoAi(true);
        var player=player(h,mirror);
        player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(60);player.setHealth(60);
        var spell=SpellEntity.spawn(h.getLevel(),player,SpellEntity.Kind.PROJECTILE,mirror.getEyePosition());
        spell.configure(1.5,new com.mcmagic.omnira.spell.SpellPayload(0,0,java.util.List.of(com.mcmagic.omnira.spell.SpellEffect.compose(false,0,1))));
        spell.home(mirror,mirror.getEyePosition());spell.setDeltaMovement(0,0,-.3);
        h.assertTrue(spell.reflectFrom(mirror) && player.getHealth()==46.5F,"Physical spell power was not returned immediately");
        h.assertTrue(!spell.reflectFrom(mirror) && player.getHealth()==46.5F,"One contact returned damage twice");
        h.assertTrue(spell.follows(player) && spell.ownedBy(mirror),"Returned spell lost homing or ownership");
        // A return flight from ten blocks away outlasts the ordinary hurt cooldown.
        player.invulnerableTime=0;
        spell.applyEffects(player);
        h.assertTrue(player.getHealth()==33 && mirror.getHealth()==30 && mirror.getTarget()==null,"Return hit was weakened, consumed, or provoked the mirror");
        spell.discard();mirror.discard();player.discard();h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void arrowDoubleDamage(GameTestHelper h) {
        var mirror=mirror(h);var player=player(h,mirror);
        var arrow=new net.minecraft.world.entity.projectile.Arrow(net.minecraft.world.entity.EntityType.ARROW,h.getLevel()) {
            public void hitTarget(net.minecraft.world.entity.Entity target) {super.onHitEntity(new net.minecraft.world.phys.EntityHitResult(target));}
        };
        arrow.setOwner(player);arrow.setPos(mirror.getEyePosition());arrow.setDeltaMovement(0,0,-1);arrow.setBaseDamage(4);
        h.assertTrue(com.mcmagic.omnira.event.MirrorProjectileEvents.bounce(arrow,mirror,arrow.position()) && player.getHealth()==16,"Arrow impact amount was not returned");
        h.assertTrue(!com.mcmagic.omnira.event.MirrorProjectileEvents.bounce(arrow,mirror,arrow.position()) && player.getHealth()==16,"Arrow contact returned damage twice");
        player.invulnerableTime=0;arrow.hitTarget(player);
        h.assertTrue(player.getHealth()==12 && mirror.getHealth()==30 && mirror.getTarget()==null,"Returned arrow failed to damage again");
        arrow.discard();player.discard();h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void fullInventoryTrade(GameTestHelper h) {
        var mirror=mirror(h);var player=player(h,mirror);
        for(int i=0;i<player.getInventory().items.size();i++)
            player.getInventory().items.set(i,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE,64));
        var hand=net.minecraft.world.InteractionHand.OFF_HAND;
        player.setItemInHand(hand,new net.minecraft.world.item.ItemStack(ModItems.DREAM_CRYSTAL_SHARD.get()));
        var area=player.getBoundingBox().inflate(3);
        var before=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,area);
        mirror.mobInteract(player,hand);
        player.getAbilities().instabuild=true;
        player.setItemInHand(hand,new net.minecraft.world.item.ItemStack(ModItems.DREAM_CRYSTAL_SHARD.get()));
        mirror.mobInteract(player,hand);
        var drops=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,area,e->!before.contains(e));
        h.assertTrue(player.getOffhandItem().isEmpty() && drops.size()==2,"Full survival/creative inventory lost or duplicated reward");
        drops.forEach(net.minecraft.world.entity.Entity::discard);player.discard();h.succeed();
    }
}
