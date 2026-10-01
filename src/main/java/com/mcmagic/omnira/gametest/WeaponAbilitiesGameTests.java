package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.item.*;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.spell.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("omnira_weapon_abilities")
@PrefixGameTestTemplate(false)
public final class WeaponAbilitiesGameTests {
    private static void loadSpell(ServerPlayer player) {
        var crystal=new ItemStack(ModItems.LOW_TIER_MAGIC_CRYSTAL.get());
        crystal.set(ModDataComponents.SPELL_PATTERN,new SpellPattern(ElementType.WATER,ElementType.EARTH));
        crystal.set(ModDataComponents.SPELL_PAYLOAD,new SpellPayload(10,4));
        player.getMainHandItem().set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(crystal)));
    }
    @GameTest(template="spell_arena",timeoutTicks=50)
    public static void bothWeaponsHaveTwoSecondSpellCooldown(GameTestHelper h) {
        for(var item:List.of(ModItems.CRYSTALLIZED_NAIL.get(),ModItems.ARCANE_NEEDLE.get())) {
            var player=player(h,item);var mob=target(h,player,2);loadSpell(player);
            RitualSwordItem.contact(player,mob,true);float health=mob.getHealth();
            RitualSwordItem.contact(player,mob,true);
            h.assertTrue(mob.getHealth()==health && player.getData(ModAttachments.MANA).current()==90,"Spell cooldown failed to suppress repeat hit");
            h.assertTrue(!player.getCooldowns().isOnCooldown(item),"Spell cooldown blocked the whole weapon");
            h.assertTrue(SwordSpellCooldowns.ready(player,new ItemStack(item)),"Another weapon inherited the first one's cooldown");
            h.runAfterDelay(39,()->h.assertTrue(!SwordSpellCooldowns.ready(player,player.getMainHandItem()),"Spell cooldown shorter than two seconds"));
            h.runAfterDelay(40,()->{
                h.assertTrue(SwordSpellCooldowns.ready(player,player.getMainHandItem()),"Spell cooldown longer than two seconds");
                RitualSwordItem.contact(player,mob,true);
                h.assertTrue(player.getData(ModAttachments.MANA).current()==80,"Spell did not resume after cooldown");
            });
        }
        h.runAfterDelay(41,h::succeed);
    }
    @GameTest(template="spell_arena")
    public static void nailWaveHitsEachTargetAndBypassesSpellCooldown(GameTestHelper h) {
        var player=player(h,ModItems.CRYSTALLIZED_NAIL.get());loadSpell(player);
        var first=target(h,player,4);first.setPos(player.position().add(-1.4,.5,4));
        var second=target(h,player,4);second.setPos(player.position().add(1.4,.5,4));
        var behind=target(h,player,-3);var far=target(h,player,8);
        SwordSpellCooldowns.trigger(player,player.getMainHandItem());
        NailSlash.release(player);
        h.assertTrue(Math.abs(first.getHealth()-70)<.01 && Math.abs(second.getHealth()-70)<.01,"Fan did not deal power-scaled doubled melee plus enhanced spell to each target");
        h.assertTrue(player.getData(ModAttachments.MANA).current()==80,"AOE did not debit exactly once per spell target");
        h.assertTrue(behind.getHealth()==100 && far.getHealth()==100,"Slash hit behind or outside doubled range");
        RitualSwordItem.contact(player,first,true);
        h.assertTrue(player.getData(ModAttachments.MANA).current()==80,"AOE bypass leaked into ordinary contact");
        var wall=BlockPos.containing(player.getEyePosition().add(0,0,2));
        for(int x=-3;x<=3;x++)for(int y=-2;y<=2;y++)h.getLevel().setBlockAndUpdate(wall.offset(x,y,0),Blocks.STONE.defaultBlockState());
        h.assertTrue(NailSlash.targets(player,6).isEmpty(),"Slash fan selected entities through a wall");
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void thrownNeedleLocksOrdinaryAttacksUntilReturn(GameTestHelper h) {
        var player=player(h,ModItems.ARCANE_NEEDLE.get());var mob=target(h,player,2);
        var needle=ModEntityTypes.FLYING_NEEDLE.get().create(h.getLevel());needle.launch(player,player.getMainHandItem());h.getLevel().addFreshEntity(needle);
        h.assertTrue(FlyingNeedle.isOut(player),"Active needle not tracked");
        player.attack(mob);h.assertTrue(mob.getHealth()==100,"Ring performed an ordinary melee attack");
        h.assertTrue(!SwordActions.eligible(player),"Ring could charge another needle");
        SwordActions.strike(player,mob,false);h.assertTrue(mob.getHealth()<100,"Needle impact was blocked by its own lock");
        mob.setHealth(100);mob.invulnerableTime=0;needle.discard();
        player.attack(mob);h.assertTrue(mob.getHealth()<100 && !FlyingNeedle.isOut(player),"Attack lock persisted after return");
        h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=40)
    public static void damageInterruptsBothChargedAttacks(GameTestHelper h) {
        var nail=player(h,ModItems.CRYSTALLIZED_NAIL.get());var mob=target(h,nail,5);
        var needle=player(h,ModItems.ARCANE_NEEDLE.get());needle.setPos(needle.position().add(10,0,0));
        SwordActions.input(nail,0);SwordActions.input(needle,0);
        h.runAfterDelay(25,()->{
            // Fresh ServerPlayers have 60 ticks of spawn protection; bypass it in this fixture.
            h.assertTrue(nail.hurt(nail.damageSources().genericKill(),1),"Test hit rejected");
            needle.setAbsorptionAmount(4);
            needle.hurt(needle.damageSources().genericKill(),1);
            SwordActions.input(nail,1);SwordActions.input(needle,1);
            h.assertTrue(mob.getHealth()==100,"Damaged nail still released charged slash");
            h.assertTrue(h.getLevel().getEntitiesOfClass(FlyingNeedle.class,needle.getBoundingBox().inflate(12),e->e.getOwner()==needle).isEmpty(),
                    "Absorbed hit did not interrupt flying needle charge");
            h.succeed();
        });
    }
    @GameTest(template="spell_arena")
    public static void damageInterruptsBothHealingChannels(GameTestHelper h) {
        for(var item:new Item[]{ModItems.CRYSTALLIZED_NAIL.get(),ModItems.ARCANE_NEEDLE.get()}) {
            var player=player(h,item);var sword=(RitualSwordItem)item;
            player.setHealth(5);
            sword.use(h.getLevel(),player,InteractionHand.MAIN_HAND);
            SwordFocus.advance(player,player.getMainHandItem(),20);
            double paid=player.getData(ModAttachments.MANA).current();
            player.hurt(player.damageSources().genericKill(),1);
            float health=player.getHealth();
            h.assertTrue(!player.isUsingItem(),"Hit did not stop healing use");
            SwordFocus.advance(player,player.getMainHandItem(),60);
            h.assertTrue(player.getHealth()==health && player.getData(ModAttachments.MANA).current()==paid,
                    "Interrupted healing completed, charged more mana or refunded costs");
        }
        h.succeed();
    }
    private static ServerPlayer player(GameTestHelper h,Item item) {
        var profile=new GameProfile(UUID.randomUUID(),"weapon-abilities");
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),profile,ClientInformation.createDefault());p.connection=new FakePlayer(h.getLevel(),profile).connection;
        p.setPos(h.absoluteVec(new Vec3(4.5,40,4.5)));p.setYRot(0);p.setXRot(0);p.setOnGround(true);
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item));p.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(8);
        p.setData(ModAttachments.MANA,new com.mcmagic.omnira.mana.ManaState(100,100));return p;
    }
    private static LivingEntity target(GameTestHelper h,ServerPlayer p,double distance) {
        var cow=h.spawn(EntityType.COW,4,40,4);cow.setNoAi(true);cow.setNoGravity(true);
        cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);cow.setHealth(100);cow.setPos(p.position().add(0,.5,distance));return cow;
    }
    @GameTest(template="spell_arena") public static void chargedDamageAndTemporaryPower(GameTestHelper h) {
        var p=player(h,ModItems.CRYSTALLIZED_NAIL.get());var mob=target(h,p,2);
        float normal=(float)p.getAttributeValue(Attributes.ATTACK_DAMAGE);mob.setHealth(100);mob.invulnerableTime=0;
        SwordActions.strike(p,mob,true);h.assertTrue(Math.abs(100-mob.getHealth()-normal*1.5*2)<.001,"Charged melee did not apply power before doubling");
        var crystal=new ItemStack(ModItems.LOW_TIER_MAGIC_CRYSTAL.get());crystal.set(ModDataComponents.SPELL_PATTERN,new SpellPattern(ElementType.WATER,ElementType.EARTH));crystal.set(ModDataComponents.SPELL_PAYLOAD,new SpellPayload(30,4));
        p.getAttribute(ModAttributes.SPELL_POWER).addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("omnira","test_power_bonus"),.3,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        p.getMainHandItem().set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(crystal)));
        mob.setHealth(100);mob.invulnerableTime=0;SwordActions.strike(p,mob,true);
        h.assertTrue(Math.abs(100-mob.getHealth()-(normal*1.8*2+7.2))<.01,"Charged spell power must add 0.5, not multiply existing bonuses");
        h.assertTrue(p.getAttributeValue(ModAttributes.SPELL_POWER)==1.3 && SwordActions.spellBonus(p)==0 && p.getData(ModAttachments.MANA).current()==70,"Strike bonus leaked or mana cost changed");
        p.getAttribute(ModAttributes.SPELL_POWER).setBaseValue(2);
        mob.setHealth(100);mob.invulnerableTime=0;SwordActions.strike(p,mob,true);
        h.assertTrue(Math.abs(100-mob.getHealth()-(normal*3.6*2+14.4))<.01,"Charged bonus must use the actual base power");
        h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=50) public static void chargeTimingRangeAndWallOcclusion(GameTestHelper h) {
        var p=player(h,ModItems.CRYSTALLIZED_NAIL.get());var mob=target(h,p,5);
        h.assertTrue(SwordActions.pick(p,3)==null && SwordActions.pick(p,6)==mob,"Double reach selection wrong");
        var wall=BlockPos.containing(p.getEyePosition().add(0,0,2));h.getLevel().setBlockAndUpdate(wall,Blocks.STONE.defaultBlockState());
        h.assertTrue(SwordActions.pick(p,6)==null,"Charged slash can select through walls");h.getLevel().removeBlock(wall,false);
        SwordActions.input(p,1);h.assertTrue(mob.getHealth()==100,"Release without a start attacked");SwordActions.input(p,0);
        h.runAfterDelay(19,()->{SwordActions.input(p,1);h.assertTrue(mob.getHealth()==100,"Sub-second charge gained extended reach");SwordActions.input(p,0);});
        h.runAfterDelay(39,()->{SwordActions.input(p,1);h.assertTrue(mob.getHealth()<100,"One-second charge failed");float health=mob.getHealth();SwordActions.input(p,1);h.assertTrue(mob.getHealth()==health,"Repeated release attacked twice");h.succeed();});
    }
    @GameTest(template="spell_arena") public static void flyingNeedleReturnsAtTenBlocks(GameTestHelper h) {
        var p=player(h,ModItems.ARCANE_NEEDLE.get());var needle=ModEntityTypes.FLYING_NEEDLE.get().create(h.getLevel());needle.launch(p,p.getMainHandItem());h.getLevel().addFreshEntity(needle);
        var origin=p.getEyePosition();double max=0;
        for(int t=0;t<40 && !needle.isRemoved();t++){needle.tick();max=Math.max(max,needle.position().distanceTo(origin));}
        h.assertTrue(max<=10.001 && max>9 && needle.isRemoved(),"Missed needle exceeded ten blocks or did not return");
        h.assertTrue(p.getMainHandItem().is(ModItems.ARCANE_NEEDLE.get()) && p.getMainHandItem().getCount()==1,"Throw consumed or duplicated weapon");h.succeed();
    }
    @GameTest(template="spell_arena") public static void needleHitAttacksCastsAndPulls(GameTestHelper h) {
        var p=player(h,ModItems.ARCANE_NEEDLE.get());var mob=target(h,p,6);
        var crystal=new ItemStack(ModItems.LOW_TIER_MAGIC_CRYSTAL.get());crystal.set(ModDataComponents.SPELL_PATTERN,new SpellPattern(ElementType.WATER,ElementType.EARTH));crystal.set(ModDataComponents.SPELL_PAYLOAD,new SpellPayload(30,4));
        p.getMainHandItem().set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(crystal)));
        var needle=ModEntityTypes.FLYING_NEEDLE.get().create(h.getLevel());needle.launch(p,p.getMainHandItem());h.getLevel().addFreshEntity(needle);var origin=p.position();
        for(int i=0;i<35 && !needle.isRemoved();i++)needle.tick();
        h.assertTrue(mob.getHealth()<96 && p.getData(ModAttachments.MANA).current()==70,"Needle failed to perform one melee/spell hit");
        h.assertTrue(p.position().distanceTo(origin)>3 && needle.isRemoved(),"Needle failed to pull/recover");h.succeed();
    }
    @GameTest(template="spell_arena") public static void wallPullDoesNotEmbedPlayer(GameTestHelper h) {
        var p=player(h,ModItems.ARCANE_NEEDLE.get());var origin=p.position();var wall=BlockPos.containing(p.getEyePosition().add(0,0,6));
        for(int y=-2;y<=2;y++)h.getLevel().setBlockAndUpdate(wall.above(y),Blocks.STONE.defaultBlockState());
        var needle=ModEntityTypes.FLYING_NEEDLE.get().create(h.getLevel());needle.launch(p,p.getMainHandItem());h.getLevel().addFreshEntity(needle);
        for(int i=0;i<35 && !needle.isRemoved();i++)needle.tick();
        h.assertTrue(p.position().distanceTo(origin)>3 && h.getLevel().noCollision(p) && p.getZ()<wall.getZ(),"Wall pull embedded player or did not pull");h.succeed();
    }
    @GameTest(template="spell_arena") public static void bothSwordsBounceOnDownstrike(GameTestHelper h) {
        for(var item:List.of(ModItems.CRYSTALLIZED_NAIL.get(),ModItems.ARCANE_NEEDLE.get())) {
            var p=player(h,item);var mob=target(h,p,0);p.setOnGround(false);p.setXRot(80);p.setPos(mob.getX(),mob.getBoundingBox().maxY+.1,mob.getZ());p.setDeltaMovement(0,-.3,0);
            p.attack(mob);h.assertTrue(p.getDeltaMovement().y==.72,"Actual downward attack did not bounce: "+item);
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void repeatedPogoSurvivesAttackRecovery(GameTestHelper h) {
        for(var item:List.of(ModItems.CRYSTALLIZED_NAIL.get(),ModItems.ARCANE_NEEDLE.get())) {
            var p=player(h,item);var mob=target(h,p,0);var ground=p.position();mob.setPos(ground);
            p.setPos(ground.add(0,.55,0));p.setOnGround(false);p.setXRot(90);p.setDeltaMovement(0,.2,0);
            p.attack(mob);h.assertTrue(p.getDeltaMovement().y==.72,"Rising jump hit was excluded");
            int hits=1,sinceHit=0;
            for(int tick=0;tick<160 && hits<5;tick++) {
                p.setPos(p.position().add(p.getDeltaMovement()));
                p.setDeltaMovement(0,(p.getDeltaMovement().y-.08)*.98,0);sinceHit++;
                h.assertTrue(p.getY()>ground.y,"Pogo landed before next hit");
                if(sinceHit>=13 && p.getDeltaMovement().y<0 && SwordActions.pick(p,p.entityInteractionRange())==mob) {
                    mob.invulnerableTime=0;p.attack(mob);sinceHit=0;hits++;
                    h.assertTrue(p.getDeltaMovement().y==.72,"Repeated attack did not renew pogo impulse");
                }
            }
            h.assertTrue(hits==5,"Could not sustain five airborne attacks");mob.discard();
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void lowHitsAboveSolidFloorPullPlayer(GameTestHelper h) {
        for(boolean entity:List.of(false,true)) {
            var p=player(h,ModItems.ARCANE_NEEDLE.get());var origin=p.position();
            int floor=BlockPos.containing(origin).getY()-1;
            for(int x=-2;x<=2;x++)for(int z=-1;z<=10;z++)
                h.getLevel().setBlockAndUpdate(new BlockPos((int)Math.floor(origin.x)+x,floor,(int)Math.floor(origin.z)+z),Blocks.STONE.defaultBlockState());
            p.setXRot(entity?8:18);
            LivingEntity mob=null;
            if(entity) {mob=target(h,p,6);mob.setPos(origin.add(0,0,6));}
            var needle=ModEntityTypes.FLYING_NEEDLE.get().create(h.getLevel());needle.launch(p,p.getMainHandItem());h.getLevel().addFreshEntity(needle);
            // A synchronized inventory replacement must not invalidate a throw.
            p.setItemInHand(InteractionHand.MAIN_HAND,p.getMainHandItem().copy());
            for(int t=0;t<40 && !needle.isRemoved();t++)needle.tick();
            h.assertTrue(p.position().distanceTo(origin)>3,"Low "+(entity?"entity":"floor")+" hit did not pull");
            h.assertTrue(h.getLevel().noCollision(p) && p.getY()>=floor+1,"Pull put feet inside terrain");
            if(mob!=null){h.assertTrue(mob.getHealth()<100,"Low entity was not attacked");mob.discard();}
        }
        h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=60) public static void worldTicksSendPullPositionsToTheOwner(GameTestHelper h) {
        var p=player(h,ModItems.ARCANE_NEEDLE.get());var origin=p.position();
        var packets=new ArrayList<net.minecraft.network.protocol.Packet<?>>();
        p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(h.getLevel().getServer(),
                new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p,
                net.minecraft.server.network.CommonListenerCookie.createInitial(p.getGameProfile(),false)) {
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet){packets.add(packet);}
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet,net.minecraft.network.PacketSendListener listener){packets.add(packet);}
        };
        var wall=BlockPos.containing(origin.add(0,0,7));
        for(int x=-2;x<=2;x++)for(int z=-1;z<=8;z++)h.getLevel().setBlockAndUpdate(BlockPos.containing(origin).offset(x,-1,z),Blocks.STONE.defaultBlockState());
        for(int y=0;y<4;y++)h.getLevel().setBlockAndUpdate(wall.above(y),Blocks.STONE.defaultBlockState());
        var needle=ModEntityTypes.FLYING_NEEDLE.get().create(h.getLevel());needle.launch(p,p.getMainHandItem());h.getLevel().addFreshEntity(needle);
        h.runAfterDelay(30,()->{
            h.assertTrue(p.position().distanceTo(origin)>4 && needle.isRemoved(),"World-ticked needle did not pull to wall");
            var positions=packets.stream().filter(net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket.class::isInstance).toList();
            h.assertTrue(positions.size()>=3,"Pull positions were not sent to the real owner's listener");
            var last=(net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket)positions.getLast();
            h.assertTrue(new Vec3(last.getX(),last.getY(),last.getZ()).distanceTo(p.position())<.001,"Owner received a different pull position");
            h.assertTrue(packets.stream().anyMatch(net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket.class::isInstance),"Client motion was not synchronized");
            h.assertTrue(h.getLevel().noCollision(p),"Networked pull embedded owner in wall");h.succeed();
        });
    }
    @GameTest(template="spell_arena") public static void lifeEnderCancelsDamageAndRebounds(GameTestHelper h) {
        var p=player(h,ModItems.LIFE_ENDER.get());var mob=target(h,p,2);p.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(100);
        for(var entry:p.getMainHandItem().get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers())p.getAttribute(entry.attribute()).addTransientModifier(entry.modifier());
        h.assertTrue(p.getAttributeValue(Attributes.ATTACK_DAMAGE)==0 && !p.getMainHandItem().isDamageableItem(),"Life Ender attribute/durability wrong");
        float health=p.getHealth();p.attack(mob);
        h.assertTrue(mob.getHealth()==100 && p.getHealth()==health && p.getDeltaMovement().z<0,"Life Ender hurt someone or failed to rebound");
        var slow=p.getEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
        h.assertTrue(slow!=null && slow.getAmplifier()==1 && slow.getDuration()==20,"Life Ender slowness wrong");h.succeed();
    }
    @GameTest(template="spell_arena") public static void logsConvertWithoutRitual(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(5,2,5));h.getLevel().setBlockAndUpdate(pos,ModBlocks.PURE_VESSEL.get().defaultBlockState());
        var player=player(h,Items.AIR);player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(pos).add(0,0,1));
        int expected=0;
        for(var item:List.of(Items.OAK_LOG,Items.STRIPPED_BIRCH_LOG,Items.OAK_WOOD,Items.STRIPPED_CRIMSON_HYPHAE)) {
            h.getLevel().setBlockAndUpdate(pos,ModBlocks.PURE_VESSEL.get().defaultBlockState());
            var v=(com.mcmagic.omnira.block.entity.PureVesselBlockEntity)h.getLevel().getBlockEntity(pos);
            var stack=new ItemStack(item,2);
            h.assertTrue(v.activate(player,stack) && stack.getCount()==1 && !h.getLevel().getBlockState(pos).isAir(),"Wood conversion must begin without instant output");
            h.assertTrue(!v.activate(player,stack) && stack.getCount()==1,"Repeated use consumed another log");
            for(int tick=0;tick<39;tick++)v.tick();
            h.assertTrue(!h.getLevel().getBlockState(pos).isAir() && v.ritual.collapseProgress(0)>.9F,"Collapse did not advance gradually");
            var restored=new com.mcmagic.omnira.block.entity.PureVesselBlockEntity(pos,v.getBlockState());
            restored.loadWithComponents(v.saveWithFullMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());
            h.assertTrue(restored.ritual.active() && restored.ritual.progress()==39,"Wood collapse lost on reload");
            v.tick();h.assertTrue(h.getLevel().getBlockState(pos).isAir(),"Vessel did not finish collapsing");
            expected++;
            var drops=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(2));
            h.assertTrue(drops.stream().filter(e->e.getItem().is(ModItems.LIFE_ENDER.get())).mapToInt(e->e.getItem().getCount()).sum()==expected,"Missing or duplicated output");
            h.assertTrue(drops.stream().noneMatch(e->e.getItem().is(ModItems.PURE_VESSEL.get())),"Consumed vessel dropped itself");
        }h.succeed();
    }
}
