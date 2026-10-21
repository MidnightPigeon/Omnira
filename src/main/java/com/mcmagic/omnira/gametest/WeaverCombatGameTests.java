package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.entity.WeaverSpirit;
import com.mcmagic.omnira.weaver.*;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.combat.OmniraDamage;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("omnira_weaver_combat") @PrefixGameTestTemplate(false)
public final class WeaverCombatGameTests {
    private static WeaverSpirit boss(GameTestHelper h){return h.spawn(ModEntityTypes.WEAVER_SPIRIT.get(),new BlockPos(5,5,5));}
    private static ServerPlayer player(GameTestHelper h,int x,int z){
        var p=BranchAdvancementGameTests.player(h);p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);p.setHealth(1000);
        try{var field=ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");field.setAccessible(true);field.setInt(p,0);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
        p.moveTo(h.absolutePos(new BlockPos(x,3,z)).getCenter());h.getLevel().addFreshEntity(p);return p;
    }
    private static void cleanup(GameTestHelper h,WeaverSpirit boss,ServerPlayer... players){
        h.getLevel().getEntitiesOfClass(WeaverBolt.class,boss.getBoundingBox().inflate(80),b->b.getOwner()==boss||Arrays.asList(players).contains(b.getOwner())).forEach(Entity::discard);
        boss.discard();for(var p:players)p.discard();
    }
    @GameTest(template="spell_arena") public static void combatPhasesAndRangeExitHealsButPreservesCounters(GameTestHelper h){
        var b=boss(h);var p=player(h,2,2);
        try{
            var tag=b.saveWithoutId(new net.minecraft.nbt.CompoundTag());
            tag.putInt("WeaverStrength",2);tag.putInt("WeaverNormals",23);tag.putInt("WeaverSinceRay",4);b.load(tag);
            b.hurt(b.damageSources().playerAttack(p),1);b.tick();
            h.assertTrue(b.active()&&b.phasing()&&b.noPhysics,"Combat did not enable phasing");
            for(int y=3;y<=9;y++)for(int z=2;z<=8;z++)h.setBlock(new BlockPos(7,y,z),Blocks.STONE);
            b.move(MoverType.SELF,new Vec3(5,0,0));
            h.assertTrue(b.getX()>h.absolutePos(new BlockPos(7,5,5)).getX(),"Combat body could not cross terrain");
            p.setPos(b.position().add(48,0,0));h.assertTrue(b.opponents().contains(p),"48-block boundary excluded");
            p.setPos(b.position().add(48.01,0,0));b.tick();
            var saved=b.saveWithoutId(new net.minecraft.nbt.CompoundTag());
            h.assertTrue(!b.active()&&b.getHealth()==1024&&b.strength()==2&&b.normalAttacks()==23&&saved.getInt("WeaverSinceRay")==4,"Range exit lost counters, strength, or full healing");
            p.setPos(b.position().add(4,0,0));b.tick();
            h.assertTrue(!b.active()&&b.opponents().isEmpty(),"Expired opponent automatically reacquired");
        }finally{cleanup(h,b,p);}h.succeed();
    }
    @GameTest(template="spell_arena") public static void embeddedDisengagementStaysPhasedUntilClear(GameTestHelper h){
        var b=boss(h);var p=player(h,2,2);
        try{
            b.hurt(b.damageSources().playerAttack(p),1);b.tick();
            for(int x=3;x<=7;x++)for(int y=4;y<=9;y++)for(int z=3;z<=7;z++)h.setBlock(new BlockPos(x,y,z),Blocks.STONE);
            b.setPos(h.absolutePos(new BlockPos(5,5,5)).getCenter());
            p.setHealth(0);b.tick();
            h.assertTrue(!b.active()&&b.phasing()&&b.noPhysics&&b.getHealth()==1024,"Embedded boss solidified or failed to heal");
            var tag=b.saveWithoutId(new net.minecraft.nbt.CompoundTag());b.load(tag);
            h.assertTrue(b.phasing()&&b.noPhysics,"Reload lost recovery phasing");
            for(int i=0;i<200&&b.phasing();i++)b.tick();
            h.assertTrue(!b.phasing()&&!b.noPhysics&&!b.isNoGravity()&&com.mcmagic.omnira.weaver.WeaverPhasing.clear(b,b.position()),"Boss did not safely leave terrain and materialize");
            h.assertTrue(h.getBlockState(new BlockPos(5,5,5)).is(Blocks.STONE),"Phasing destroyed terrain");
        }finally{cleanup(h,b,p);}h.succeed();
    }
    @GameTest(template="spell_arena") public static void neutralLandsAndCannotCrossWalls(GameTestHelper h){
        for(int x=2;x<9;x++)for(int z=2;z<9;z++)h.setBlock(new BlockPos(x,2,z),Blocks.STONE);
        var b=boss(h);
        try{
            for(int i=0;i<60;i++)b.tick();
            h.assertTrue(!b.active()&&!b.noPhysics&&b.onGround()&&Math.abs(b.getY()-h.absolutePos(new BlockPos(5,3,5)).getY())<.02,"Neutral boss does not settle on ground");
            float clock=b.motionClock(0);b.tick();h.assertTrue(b.motionClock(0)==clock,"Idle mechanisms animate");
            for(int y=3;y<=7;y++)for(int z=2;z<=8;z++)h.setBlock(new BlockPos(7,y,z),Blocks.STONE);
            b.move(MoverType.SELF,new Vec3(8,0,0));
            h.assertTrue(b.getBoundingBox().maxX<=h.absolutePos(new BlockPos(7,3,5)).getX()+.001,"Boss moved through a solid wall");
        }finally{b.discard();}h.succeed();
    }
    @GameTest(template="spell_arena") public static void idleSlowFallLandsAndDoesNotAffectCombatFlight(GameTestHelper h){
        for(int x=2;x<9;x++)for(int z=2;z<9;z++)h.setBlock(new BlockPos(x,2,z),Blocks.STONE);
        var b=h.spawn(ModEntityTypes.WEAVER_SPIRIT.get(),new BlockPos(5,8,5));var p=player(h,2,2);
        try{
            double start=b.getY();
            h.assertTrue(Math.abs(b.getGravity()-.01)<.00001,"Idle descent lacks vanilla slow-fall gravity");
            for(int i=0;i<20;i++)b.tick();
            h.assertTrue(b.getY()<start&&b.getY()>start-2&&!b.onGround(),"Idle boss fell too quickly or failed to descend");
            for(int i=0;i<160&&!b.onGround();i++)b.tick();
            h.assertTrue(b.onGround()&&!b.active()&&b.getHealth()==1024,"Slow-falling boss failed to land safely");
            b.hurt(b.damageSources().playerAttack(p),1);b.tick();
            h.assertTrue(b.active()&&b.isNoGravity()&&b.getGravity()==0,"Idle slow fall interfered with combat flight");
            p.setHealth(0);b.tick();
            b.setDeltaMovement(new Vec3(0,-.1,0));
            h.assertTrue(!b.active()&&!b.isNoGravity()&&b.getGravity()<=.01,"Returning to idle did not restore slow fall");
        }finally{cleanup(h,b,p);}h.succeed();
    }
    @GameTest(template="spell_arena") public static void retaliationOnlyTracksAttackersAndNormalClock(GameTestHelper h){
        var b=boss(h);var p=player(h,4,2);var bystander=player(h,8,8);
        try{
            b.hurt(b.damageSources().playerAttack(p),1);
            h.assertTrue(b.active()&&b.opponents().equals(List.of(p)),"Neutral bystander was added to hostility");
            for(int i=0;i<59;i++)b.tick();h.assertTrue(b.normalAttacks()==0,"Normal attack fired before three seconds");
            b.tick();h.assertTrue(b.normalAttacks()==1&&b.isNoGravity(),"Normal clock or flight activation failed");
            b.hurt(WeaverCombat.source(b,net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK,bystander,bystander,false,false),1);
            h.assertTrue(b.opponents().size()==2,"Second attacker not tracked");
        }finally{cleanup(h,b,p,bystander);}h.succeed();
    }
    @GameTest(template="spell_arena") public static void missingOpponentsReturnToIdleEvenDuringReload(GameTestHelper h){
        for(int x=1;x<10;x++)for(int z=1;z<10;z++)h.setBlock(new BlockPos(x,2,z),Blocks.STONE);
        var b=boss(h);var p=player(h,2,2);
        try{
            b.hurt(b.damageSources().playerAttack(p),1);b.tick();
            h.assertTrue(b.active()&&b.isNoGravity(),"Provoked boss did not fly");
            p.setHealth(0);b.tick();
            h.assertTrue(!b.active()&&!b.isNoGravity()&&b.getTarget()==null&&b.getNavigation().isDone(),"Dead opponent left boss flying or targeting");
            p.setHealth(1000);OmniraDamage.trueDamage(b,512,b.damageSources().playerAttack(p));b.tick();
            h.assertTrue(b.reloading()&&b.active(),"Reload did not start after renewed provocation");
            p.discard();b.tick();float clock=b.motionClock(1),hp=b.getHealth();
            h.assertTrue(hp==b.getMaxHealth(),"Disengagement failed to restore full health");
            for(int i=0;i<80;i++)b.tick();
            h.assertTrue(!b.active()&&!b.isNoGravity()&&b.onGround()&&b.getTarget()==null&&b.motionClock(1)==clock
                    &&b.getHealth()==hp&&b.normalAttacks()==0&&!b.reloading()&&!b.redCore(),"Idle state: active="+b.active()+", phased="+b.phasing()+", gravity="+b.isNoGravity()+", ground="+b.onGround()+", hp="+b.getHealth()+", position="+b.position()+", clock="+b.motionClock(1)+"/"+clock);
        }finally{cleanup(h,b,p);}h.succeed();
    }
    @GameTest(template="spell_arena") public static void volleysCountOnceRayDoesNotCountAndStrengthCaps(GameTestHelper h){
        var b=boss(h);var p=player(h,2,2);var q=player(h,8,8);
        try{
            for(int i=0;i<5;i++)b.attackRound(List.of(p,q));
            h.assertTrue(b.normalAttacks()==5&&h.getLevel().getEntitiesOfClass(WeaverBolt.class,b.getBoundingBox().inflate(10),bolt->bolt.getOwner()==b).size()==10,"Multiplayer volley counted as multiple attacks");
            b.attackRound(List.of(p,q));h.assertTrue(b.normalAttacks()==5,"Ray advanced normal attack count");
            for(int i=0;i<43;i++)b.attackRound(List.of(p,q));
            h.assertTrue(b.normalAttacks()==41&&b.strength()==3,"Ten-shot strengthening cadence or cap incorrect");
        }finally{cleanup(h,b,p,q);}h.succeed();
    }
    @GameTest(template="spell_arena") public static void halfHealthSequenceRestoresOnlyOnceAndPersists(GameTestHelper h){
        var b=boss(h);var p=player(h,3,3);
        try{
            OmniraDamage.trueDamage(b,512,b.damageSources().playerAttack(p));
            h.assertTrue(b.reloading(),"Half-health phase not triggered");
            for(int i=0;i<26;i++)b.tick();var tag=b.saveWithoutId(new net.minecraft.nbt.CompoundTag());b.load(tag);
            for(int i=26;i<51;i++)b.tick();
            h.assertTrue(b.redCore()&&!b.reloading()&&b.getHealth()==1024&&b.normalAttacks()==0,"Four phase rays/heals or phase persistence failed");
            OmniraDamage.trueDamage(b,512,b.damageSources().playerAttack(p));h.assertTrue(!b.reloading(),"Half-health phase retriggered after healing");
        }finally{cleanup(h,b,p);}h.succeed();
    }
    @GameTest(template="spell_arena") public static void idleResetsCoreAndRearmsHalfHealthWithoutLosingStrength(GameTestHelper h){
        var b=boss(h);var p=player(h,2,2);
        try{
            var tag=b.saveWithoutId(new net.minecraft.nbt.CompoundTag());tag.putInt("WeaverStrength",2);b.load(tag);
            OmniraDamage.trueDamage(b,512,b.damageSources().playerAttack(p));
            for(int i=0;i<51;i++)b.tick();
            h.assertTrue(b.redCore()&&!b.reloading(),"First reload did not finish");
            p.setHealth(0);b.tick();
            h.assertTrue(!b.active()&&!b.redCore()&&!b.reloading()&&b.strength()==2,"Idle failed to reset core or changed strength");
            p.setHealth(1000);OmniraDamage.trueDamage(b,512,b.damageSources().playerAttack(p));
            h.assertTrue(b.active()&&b.reloading()&&!b.redCore(),"New combat did not rearm half-health reload");
            for(int i=0;i<51;i++)b.tick();
            h.assertTrue(b.redCore()&&!b.reloading(),"Second combat reload failed");
        }finally{cleanup(h,b,p);}h.succeed();
    }
    @GameTest(template="spell_arena") public static void boltHasBothDamageChannelsAndPermanentOwnerImmunity(GameTestHelper h){
        var b=boss(h);var p=player(h,2,2);var cow=h.spawn(EntityType.COW,new BlockPos(6,4,6));cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40);cow.setHealth(40);
        try{
            cow.invulnerableTime=100;var bolt=WeaverCombat.bolt(b,cow,b.getEyePosition(),cow.position().subtract(b.position()));bolt.impact(cow);
            h.assertTrue(cow.getHealth()==34,"Physical or magical bolt damage was lost to invulnerability");
            var reflected=WeaverCombat.bolt(b,p,b.getEyePosition(),p.position().subtract(b.position()));reflected.setOwner(p);
            float hp=b.getHealth();h.assertTrue(!reflected.impact(b)&&b.getHealth()==hp,"Reflected projectile harmed its original boss");
            var saved=reflected.saveWithoutId(new net.minecraft.nbt.CompoundTag());var restored=ModEntityTypes.WEAVER_BOLT.get().create(h.getLevel());restored.load(saved);
            h.assertTrue(!restored.impact(b)&&b.getHealth()==hp,"Reloaded reflected projectile lost owner immunity");restored.discard();reflected.discard();
        }finally{cow.discard();cleanup(h,b,p);}h.succeed();
    }
    @GameTest(template="spell_arena") public static void shieldBlocksEntireSplitProjectile(GameTestHelper h){
        var b=boss(h);var p=player(h,5,3);
        try{
            p.setYRot(0);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.SHIELD));p.startUsingItem(InteractionHand.MAIN_HAND);
            for(int i=0;i<6;i++)p.doTick();
            var bolt=WeaverCombat.bolt(b,p,p.getEyePosition().add(0,0,2),new Vec3(0,0,-1));
            h.assertTrue(p.isDamageSourceBlocked(WeaverCombat.source(p,net.minecraft.world.damagesource.DamageTypes.MOB_PROJECTILE,bolt,b,true,false)),"Shield wind-up/facing invalid: blocking="+p.isBlocking()+" useTicks="+p.getTicksUsingItem());
            float hp=p.getHealth();bolt.impact(p);h.assertTrue(p.getHealth()==hp,"Magic component bypassed a shield-blocked bolt");
        }finally{cleanup(h,b,p);}h.succeed();
    }
    @GameTest(template="spell_arena") public static void armorBreakBlastIsSingleUnattributedAndNotStrengthened(GameTestHelper h){
        var b=boss(h);var p=player(h,2,2);
        try{
            var tag=b.saveWithoutId(new net.minecraft.nbt.CompoundTag());tag.putInt("WeaverStrength",3);tag.putBoolean("WeaverRed",true);b.load(tag);b.setHealth(500);
            p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_SWORD));p.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.SHIELD));
            Item[] items={Items.IRON_HELMET,Items.IRON_CHESTPLATE,Items.IRON_LEGGINGS,Items.IRON_BOOTS};
            EquipmentSlot[] slots={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
            for(int i=0;i<4;i++){var stack=new ItemStack(items[i]);stack.setDamageValue(stack.getMaxDamage()-1);p.setItemSlot(slots[i],stack);}
            p.getInventory().setItem(10,new ItemStack(Items.IRON_SWORD));
            p.invulnerableTime=100;
            WeaverCombat.rayHit(b,p);
            h.assertTrue(p.getHealth()==986&&b.getHealth()==501,"Broken armor explosion amounts incorrect: player="+p.getHealth()+" boss="+b.getHealth());
            for(var slot:slots)h.assertTrue(p.getItemBySlot(slot).isEmpty(),"Armor was not broken");
            h.assertTrue(p.getMainHandItem().getDamageValue()==25&&p.getOffhandItem().getDamageValue()==34&&p.getInventory().getItem(10).getDamageValue()==0,"Equipment percentage, ceiling or backpack exclusion incorrect");
            h.assertTrue(p.getLastDamageSource()!=null&&p.getLastDamageSource().getEntity()==null,"Armor explosion has a killer attribution");
        }finally{cleanup(h,b,p);}h.succeed();
    }
    @GameTest(template="spell_arena") public static void bookBonusLifestealAndEitherHandDoNotStack(GameTestHelper h){
        var p=player(h,2,2);var b=boss(h);var cow=h.spawn(EntityType.COW,new BlockPos(7,3,7));cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);cow.setHealth(100);
        try{
            p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(ModItems.FOUNDER_APPROVAL.get()));p.setHealth(5);
            cow.invulnerableTime=100;cow.hurt(cow.damageSources().playerAttack(p),2);cow.hurt(cow.damageSources().playerAttack(p),2);
            h.assertTrue(cow.getHealth()==92&&p.getHealth()==7,"Offhand flat bonus, i-frame bypass or ceil lifesteal incorrect");
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.FOUNDER_APPROVAL.get()));OmniraDamage.trueDamage(cow,3,cow.damageSources().playerAttack(p));
            h.assertTrue(cow.getHealth()==87&&p.getHealth()==9,"Dual wield stacked, true damage missed the bonus or lifesteal was fractional");
        }finally{cow.discard();cleanup(h,b,p);}h.succeed();
    }
    @GameTest(template="spell_arena") public static void splitBoltAddsEachChannelAndMultipliesEachChannel(GameTestHelper h){
        var p=player(h,2,2);var b=boss(h);var cow=h.spawn(EntityType.COW,new BlockPos(7,3,7));cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
        var channels=new ArrayList<String>();
        java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post> capture=event->{if(event.getEntity()==cow)channels.add(event.getSource().type().msgId()+":"+event.getNewDamage());};
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(capture);
        try{
            p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(ModItems.FOUNDER_APPROVAL.get()));cow.setHealth(100);cow.invulnerableTime=100;
            WeaverCombat.bolt(p,cow,p.getEyePosition(),cow.position().subtract(p.position())).impact(cow);
            h.assertTrue(cow.getHealth()==90,"Flat +2 was not applied separately to physical and magic damage: health="+cow.getHealth()+", flat="+p.getAttributeValue(ModAttributes.DAMAGE_BONUS)+", scale="+p.getAttributeValue(ModAttributes.DAMAGE_DEALT)+", channels="+channels);
            h.assertTrue(channels.equals(List.of("mob:5.0","indirectMagic:5.0")),"Two final damage channels were merged or lost");
            p.getAttribute(ModAttributes.DAMAGE_DEALT).setBaseValue(1.5);cow.setHealth(100);cow.invulnerableTime=100;
            WeaverCombat.bolt(p,cow,p.getEyePosition(),cow.position().subtract(p.position())).impact(cow);
            h.assertTrue(cow.getHealth()==85,"Percentage damage did not scale both enhanced channels");
            var tag=b.saveWithoutId(new net.minecraft.nbt.CompoundTag());tag.putInt("WeaverStrength",2);b.load(tag);cow.setHealth(100);cow.invulnerableTime=100;
            WeaverCombat.bolt(b,cow,b.getEyePosition(),cow.position().subtract(b.position())).impact(cow);
            h.assertTrue(cow.getHealth()==86,"Boss strength did not add +4 to each channel");
        }finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(capture);cow.discard();cleanup(h,b,p);}h.succeed();
    }
    @GameTest(template="spell_arena") public static void bookPowerPrecedesFlatBonusAcrossAllActions(GameTestHelper h){
        var p=player(h,2,2);var b=boss(h);var cow=h.spawn(EntityType.COW,new BlockPos(3,3,2));
        cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);cow.setHealth(100);
        try{
            p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(ModItems.FOUNDER_APPROVAL.get()));
            p.getAttribute(ModAttributes.SPELL_POWER).setBaseValue(2);
            WeaverCombat.bolt(p,cow,p.getEyePosition(),cow.position().subtract(p.position())).impact(cow);
            h.assertTrue(cow.getHealth()==84,"Bolt must deal (3*2+2) in each channel, not (3+2)*2");
            cow.setHealth(100);cow.invulnerableTime=100;WeaverCombat.rayHit(p,cow);
            h.assertTrue(cow.getHealth()==92,"Ray must multiply its base before adding flat damage");
            b.discard();
            cow.setHealth(100);WeaverCombat.bend(p,true);
            h.assertTrue(cow.getHealth()==88,"Outward bend must deal 5*2+2");
            cow.setHealth(100);cow.invulnerableTime=100;WeaverCombat.bend(p,false);
            h.assertTrue(cow.getHealth()==82,"Inward bend must deal 8*2+2");
            cow.setHealth(100);cow.invulnerableTime=100;
            var spell=com.mcmagic.omnira.spell.entity.SpellEntity.spawn(h.getLevel(),p,com.mcmagic.omnira.spell.entity.SpellEntity.Kind.PROJECTILE,p.getEyePosition());
            try{spell.configure(2,3);spell.applyEffects(cow);h.assertTrue(cow.getHealth()==92,"Existing spell did not preserve power-before-flat order");}finally{spell.discard();}
            h.assertTrue(WeaverCombat.damage(b,3)==3,"Player spell power changed boss attack scaling");
            var lines=new ArrayList<net.minecraft.network.chat.Component>();
            ModItems.FOUNDER_APPROVAL.get().appendHoverText(new ItemStack(ModItems.FOUNDER_APPROVAL.get()),Item.TooltipContext.EMPTY,lines,TooltipFlag.NORMAL);
            var last=lines.getLast();
            h.assertTrue(lines.size()==4&&last.getStyle().isItalic()&&net.minecraft.network.chat.TextColor.fromLegacyFormat(net.minecraft.ChatFormatting.RED).equals(last.getStyle().getColor()),"Book tooltip contains redundant lines or incorrect bypass styling");
        }finally{cow.discard();cleanup(h,b,p);}h.succeed();
    }
    @GameTest(template="spell_arena") public static void registeredHeldAttributesSurviveHandRemovalAndAddLifeSteal(GameTestHelper h){
        var p=player(h,2,2);var b=boss(h);
        try{
            var stack=new ItemStack(ModItems.FOUNDER_APPROVAL.get());var modifiers=stack.get(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS).modifiers();
            h.assertTrue(modifiers.stream().anyMatch(e->e.attribute().equals(ModAttributes.DAMAGE_BONUS)&&e.modifier().amount()==2)
                    &&modifiers.stream().anyMatch(e->e.attribute().equals(ModAttributes.LIFESTEAL)&&e.modifier().amount()==.25),"Book lacks native attribute tooltip components");
            p.setItemInHand(InteractionHand.MAIN_HAND,stack);p.setItemInHand(InteractionHand.OFF_HAND,stack.copy());
            p.doTick();
            com.mcmagic.omnira.combat.HeldCombatAttributes.refresh(p);
            h.assertTrue(p.getAttributeValue(ModAttributes.DAMAGE_BONUS)==2&&p.getAttributeValue(ModAttributes.LIFESTEAL)==.25,"Dual wield stacked attributes");
            p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.doTick();com.mcmagic.omnira.combat.HeldCombatAttributes.refresh(p);
            h.assertTrue(p.getAttributeValue(ModAttributes.DAMAGE_BONUS)==2&&p.getAttributeValue(ModAttributes.LIFESTEAL)==.25,"Removing one book disabled the other hand");
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.CORRUPTED_GRIMOIRE.get()));p.getAttribute(ModAttributes.LIFESTEAL).setBaseValue(.25);
            h.assertTrue(com.mcmagic.omnira.combat.HeldCombatAttributes.lifeSteal(p,p.damageSources().playerAttack(p))==1,"Grimoire, ring-equivalent and founder rates did not add");
            h.assertTrue(com.mcmagic.omnira.combat.HeldCombatAttributes.lifeSteal(p,p.damageSources().magic())==.5,"Corrupted grimoire expanded to unrelated damage");
            p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);com.mcmagic.omnira.combat.HeldCombatAttributes.refresh(p);
            h.assertTrue(p.getAttributeValue(ModAttributes.DAMAGE_BONUS)==0&&p.getAttributeValue(ModAttributes.LIFESTEAL)==.25,"Held attributes leaked after removal");
        }finally{cleanup(h,b,p);}h.succeed();
    }
    @GameTest(template="spell_arena") public static void corruptedProjectileUsesOneSharedRateAndKeepsItsSnapshot(GameTestHelper h){
        var p=player(h,2,2);var b=boss(h);var cow=h.spawn(EntityType.COW,new BlockPos(7,3,7));cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);cow.setHealth(100);
        var spell=com.mcmagic.omnira.spell.entity.SpellEntity.spawn(h.getLevel(),p,com.mcmagic.omnira.spell.entity.SpellEntity.Kind.PROJECTILE,p.getEyePosition());spell.configure(1,10);spell.knowledgeShock(false,true);
        try{
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.CORRUPTED_GRIMOIRE.get()));p.getAttribute(ModAttributes.LIFESTEAL).setBaseValue(.25);p.setHealth(5);
            spell.applyEffects(cow);h.assertTrue(cow.getHealth()==90&&p.getHealth()==12.5F,"Corrupted projectile double-counted its life steal or accessory rate");
            p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setHealth(5);cow.invulnerableTime=0;
            spell.applyEffects(cow);h.assertTrue(cow.getHealth()==80&&p.getHealth()==12.5F,"Already-fired projectile lost its life steal when the book left the hand");
        }finally{spell.discard();cow.discard();cleanup(h,b,p);}h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=40) public static void synchronizedMotionRunsWithoutReplicaTicksAndFreezesOnIdle(GameTestHelper h){
        var p=player(h,2,2);var b=boss(h);b.hurt(b.damageSources().playerAttack(p),1);b.setNoAi(true);
        var replica=ModEntityTypes.WEAVER_SPIRIT.get().create(h.getLevel());replica.getEntityData().assignValues(b.getEntityData().getNonDefaultValues());
        float start=replica.motionClock(0);
        h.runAtTickTime(5,()->{
            h.assertTrue(replica.motionClock(.5F)>start+4&&Math.abs(replica.motionClock(.5F)-b.motionClock(.5F))<.001,"Unticked replica's gears remain frozen");
            p.setHealth(0);b.setNoAi(false);b.tick();float frozen=b.motionClock(0);
            h.runAfterDelay(5,()->{try{h.assertTrue(!b.active()&&b.motionClock(.5F)==frozen,"Idle animation did not freeze");}finally{replica.discard();cleanup(h,b,p);}h.succeed();});
        });
    }
    @GameTest(template="spell_arena") public static void threeActionCooldownsAreIndependentAndSharedAcrossCopies(GameTestHelper h){
        var b=boss(h);var p=player(h,2,2);
        try{
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.FOUNDER_APPROVAL.get()));
            for(var action:FounderApprovalItem.Action.values())h.assertTrue(FounderApprovalItem.cast(p,action),"Independent ability cooldown was blocked by another ability");
            p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(ModItems.FOUNDER_APPROVAL.get()));
            for(var action:FounderApprovalItem.Action.values())h.assertTrue(!FounderApprovalItem.cast(p,action),"Changing hand or copy bypassed cooldown");
            var clocks=p.getPersistentData().getCompound("OmniraFounderCooldowns");long now=h.getLevel().getGameTime();
            h.assertTrue(clocks.getLong("BOLT")==now+20&&clocks.getLong("RAY")==now+200&&clocks.getLong("BEND")==now+200,"Authored cooldown durations incorrect");
        }finally{cleanup(h,b,p);}h.succeed();
    }
    @GameTest(template="spell_arena") public static void deathAlwaysDropsExactlyOneApproval(GameTestHelper h){
        var b=boss(h);var area=b.getBoundingBox().inflate(4);
        OmniraDamage.execute(b,b.damageSources().genericKill());
        h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class,area,e->e.getItem().is(ModItems.FOUNDER_APPROVAL.get())).isEmpty(),"Death paid before finale");
        for(int i=0;i<com.mcmagic.omnira.weaver.WeaverDeparture.DEATH_TICKS;i++)b.tick();
        var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,area,e->e.getItem().is(ModItems.FOUNDER_APPROVAL.get()));
        h.assertTrue(drops.stream().mapToInt(e->e.getItem().getCount()).sum()==1,"Death did not drop exactly one approval");drops.forEach(Entity::discard);h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class,area).forEach(Entity::discard);b.discard();h.succeed();
    }
    @GameTest(template="spell_arena") public static void wavesUseSphericalRangeOppositeForcesAndNeverBreakTerrain(GameTestHelper h){
        var b=boss(h);var near=EntityType.COW.create(h.getLevel());var far=EntityType.COW.create(h.getLevel());
        for(var cow:List.of(near,far)){cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);cow.setHealth(100);}
        near.setPos(b.position().add(7,0,0));far.setPos(b.position().add(0,17,0));h.getLevel().addFreshEntity(near);h.getLevel().addFreshEntity(far);
        try{
            h.setBlock(new BlockPos(5,4,5),Blocks.BEDROCK);near.invulnerableTime=100;
            WeaverCombat.bend(b,true);h.assertTrue(near.getHealth()==95&&near.getDeltaMovement().x>1&&far.getHealth()==100,"Spatial shock damage, range or outward impulse incorrect");
            near.setDeltaMovement(Vec3.ZERO);WeaverCombat.bend(b,false);
            h.assertTrue(near.getHealth()==87&&near.getDeltaMovement().x< -1&&far.getHealth()==100,"Temporal distortion damage, range or inward force incorrect");
            h.assertTrue(h.getBlockState(new BlockPos(5,4,5)).is(Blocks.BEDROCK),"Wave changed terrain");
        }finally{near.discard();far.discard();b.discard();}h.succeed();
    }
    @GameTest(template="spell_arena") public static void actualHomingProjectilePassesThroughSolidWall(GameTestHelper h){
        var b=boss(h);var cow=h.spawn(EntityType.COW,new BlockPos(9,4,5));cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40);cow.setHealth(40);
        for(int y=2;y<=8;y++)for(int z=3;z<=7;z++)h.setBlock(new BlockPos(7,y,z),Blocks.STONE);
        var bolt=WeaverCombat.bolt(b,cow,b.getEyePosition(),new Vec3(1,0,0));
        try{
            for(int i=0;i<25&&!bolt.isRemoved();i++)bolt.tick();
            h.assertTrue(bolt.isRemoved()&&cow.getHealth()==34,"Homing bolt stopped at terrain or failed to hit");
            h.assertTrue(h.getBlockState(new BlockPos(7,5,5)).is(Blocks.STONE),"Projectile broke terrain");
        }finally{bolt.discard();cow.discard();b.discard();}h.succeed();
    }
    @GameTest(template="spell_arena") public static void bookRayPassesWallsButStopsAt64Blocks(GameTestHelper h){
        var p=player(h,2,2);var b=boss(h);var near=EntityType.COW.create(h.getLevel());var far=EntityType.COW.create(h.getLevel());
        p.setYRot(0);p.setXRot(0);p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(ModItems.FOUNDER_APPROVAL.get()));
        for(var cow:List.of(near,far)){cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40);cow.setHealth(40);}
        near.setPos(p.getEyePosition().add(0,-.6,8));far.setPos(p.getEyePosition().add(0,-.6,66));h.getLevel().getChunkAt(far.blockPosition());h.getLevel().addFreshEntity(near);h.getLevel().addFreshEntity(far);
        try{
            for(int y=2;y<=7;y++)h.setBlock(new BlockPos(2,y,5),Blocks.STONE);
            WeaverCombat.ray(p,p.getLookAngle());
            h.assertTrue(near.getHealth()==35&&far.getHealth()==40,"Ray was blocked by terrain, exceeded 64 blocks or missed the book's flat bonus");
        }finally{near.discard();far.discard();cleanup(h,b,p);}h.succeed();
    }
}
