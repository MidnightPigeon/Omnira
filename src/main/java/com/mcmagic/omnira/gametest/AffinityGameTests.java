package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.mana.*;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.world.dimension.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;
import java.util.UUID;

@GameTestHolder("omnira_affinity")
@PrefixGameTestTemplate(false)
public final class AffinityGameTests {
    @GameTest(template="spell_arena") public static void dreamFlightSprintAndTakeoff(GameTestHelper h) throws Exception {
        var p=player(h);state(p,Affinity.DREAM);
        var speed=net.minecraft.world.entity.player.Player.class.getDeclaredMethod("getFlyingSpeed");speed.setAccessible(true);
        p.setSprinting(true);
        h.assertTrue(Math.abs((float)speed.invoke(p)-.026F)<.0001,"Ground sprint was reduced");
        p.getAbilities().flying=true;
        float base=p.getAbilities().getFlyingSpeed();
        h.assertTrue(Math.abs((float)speed.invoke(p)-base*.5F)<.0001,"Sprint takeoff bypassed half-speed flight");
        p.setSprinting(false);
        h.assertTrue(Math.abs((float)speed.invoke(p)-base*.5F)<.0001,"Ordinary dream flight changed");
        p.setSprinting(true);p.getAbilities().setFlyingSpeed(.08F);
        h.assertTrue(Math.abs((float)speed.invoke(p)-.04F)<.0001 && p.getAbilities().getFlyingSpeed()==.08F,"External base speed was overwritten or sprint doubled it");
        p.getAbilities().flying=false;
        h.assertTrue(Math.abs((float)speed.invoke(p)-.026F)<.0001 && p.isSprinting(),"Landing lost normal sprint");
        state(p,Affinity.NONE);p.getAbilities().flying=true;
        h.assertTrue(Math.abs((float)speed.invoke(p)-.16F)<.0001,"Unaligned flight lost vanilla sprint bonus");
        h.succeed();
    }
    private static FakePlayer player(GameTestHelper h) {
        var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"affinity-test"));
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        p.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(4,2,4)));
        return p;
    }
    private static void state(FakePlayer p,Affinity affinity) {
        p.setData(ModAttachments.AFFINITY,new AffinityState(affinity.ordinal(),0));AffinityEffects.refresh(p);
    }
    private static void fill(java.util.List<com.mcmagic.omnira.block.entity.CrystalPedestalBlockEntity> pedestals,int shift) {
        for(int i=0;i<8;i++)pedestals.get(i).setItem(0,new ItemStack(AffinityRitual.ingredients().get((i+shift)%8)));
    }
    @GameTest(template="spell_arena") public static void attributesAndSerialization(GameTestHelper h) {
        var p=player(h);
        for(var affinity:Affinity.values()) {
            state(p,affinity);AffinityEffects.refresh(p);
            h.assertTrue(p.getAttributeValue(ModAttributes.MAX_MANA)==100+affinity.capacity,"Mana bonus duplicated or missing");
            h.assertTrue(Math.abs(p.getAttributeValue(ModAttributes.MANA_REGEN)-1-affinity.regeneration)<.00001,"Regeneration bonus");
            h.assertTrue(Math.abs(p.getAttributeValue(ModAttributes.SPELL_POWER)-1-affinity.power)<.00001,"Power bonus");
            h.assertTrue(p.getAttributeValue(ModAttributes.COST_REDUCTION)==affinity.reduction,"Cost reduction");
            var saved=new AffinityState(affinity.ordinal(),affinity==Affinity.NONE?0:177);
            var tag=AffinityState.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE,saved).getOrThrow();
            h.assertTrue(AffinityState.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE,tag).getOrThrow().equals(saved),"Penalty persistence");
            var mana=new ManaState(50,100,affinity.ordinal());
            h.assertTrue(mana.spend(10).withMaximum(200).regenerate().color()==affinity.color,"Mana operations lose affinity");
        }
        state(p,Affinity.NONE);h.assertTrue(p.getAttributeValue(ModAttributes.MAX_MANA)==100,"Removal retains attributes");h.succeed();
    }
    @GameTest(template="spell_arena") public static void ritualValidationAndConsumption(GameTestHelper h) {
        var p=player(h);var level=h.getLevel();var center=h.absolutePos(new BlockPos(4,2,4));
        level.setBlockAndUpdate(center,ModBlocks.ADVANCED_RITUAL_ENERGY_CORE.get().defaultBlockState());
        var pedestals=new java.util.ArrayList<com.mcmagic.omnira.block.entity.CrystalPedestalBlockEntity>();
        for(var offset:DreamRitual.ANCHORS) {
            var pos=center.offset(offset[0],0,offset[1]);level.setBlockAndUpdate(pos,ModBlocks.CRYSTAL_PEDESTAL.get().defaultBlockState());
            var pedestal=(com.mcmagic.omnira.block.entity.CrystalPedestalBlockEntity)level.getBlockEntity(pos);
            pedestal.setItem(0,new ItemStack(DreamContent.DREAM_CRYSTAL_BEDROCK.get()));pedestals.add(pedestal);
        }
        for(var affinity:new Affinity[]{Affinity.LIGHT,Affinity.DARK,Affinity.ELEMENTAL,Affinity.DREAM}) {
            state(p,Affinity.NONE);
            p.setPos(center.getX()+1.5,center.getY(),center.getZ()+.5);
            fill(pedestals,affinity.ordinal());
            p.setItemInHand(InteractionHand.MAIN_HAND,affinity.offering());
            pedestals.get(0).setItem(0,ItemStack.EMPTY);
            h.assertTrue(!AffinityRitual.activate(p,center,InteractionHand.MAIN_HAND) && !p.getMainHandItem().isEmpty(),"Invalid ritual consumed activator");
            h.assertTrue(pedestals.get(1).getItem(0).getCount()==1,"Invalid ritual consumed offering");
            fill(pedestals,affinity.ordinal());
            h.assertTrue(AffinityRitual.activate(p,center,InteractionHand.MAIN_HAND),"Valid ritual rejected");
            h.assertTrue(!p.getMainHandItem().isEmpty() && pedestals.stream().allMatch(b->!b.getItem(0).isEmpty()),"Consumed before animation completed");
            for(int tick=0;tick<AffinityRitual.DURATION;tick++)AffinityRitual.advance(p);
            h.assertTrue(p.getMainHandItem().isEmpty() && p.getData(ModAttachments.AFFINITY).active()==affinity,"Activator not consumed whole or no affinity");
            h.assertTrue(pedestals.stream().allMatch(b->!b.getItem(0).isEmpty()),"Ritual consumed symbolic offerings");
            fill(pedestals,7);
            p.setPos(center.getX()+1.5,center.getY(),center.getZ()+.5);
            p.setItemInHand(InteractionHand.MAIN_HAND,(affinity==Affinity.LIGHT?Affinity.DARK:Affinity.LIGHT).offering());
            h.assertTrue(!AffinityRitual.activate(p,center,InteractionHand.MAIN_HAND),"Different affinity replaced active affinity");
            h.assertTrue(pedestals.get(1).getItem(0).getCount()==1,"Rejected switch consumed offerings");
            p.setItemInHand(InteractionHand.MAIN_HAND,affinity.offering());
            h.assertTrue(AffinityRitual.activate(p,center,InteractionHand.MAIN_HAND),"Same ritual did not start removal");
            var removalPosition=p.position();
            for(int tick=0;tick<AffinityRitual.DURATION;tick++) {
                AffinityRitual.advance(p);
                h.assertTrue(p.position().equals(removalPosition),"Removal immobilized or lifted player");
                if(affinity==Affinity.DREAM && tick==59)h.assertTrue(Math.abs(p.getData(ModAttachments.DREAM_SOLIDIFY)-.5F)<.001,"Dream removal did not solidify progressively");
            }
            h.assertTrue(p.getData(ModAttachments.AFFINITY).penaltyTicks()==(affinity==Affinity.DREAM?0:affinity.penaltyDuration()),"Wrong removal duration");
            h.assertTrue(p.getAttributeValue(ModAttributes.MAX_MANA)==100,"Penalty retains bonuses");
            if(affinity==Affinity.DREAM){h.assertTrue(p.getData(ModAttachments.AFFINITY).equals(AffinityState.NONE),"Dream removal imposed penalty");continue;}
            p.setItemInHand(InteractionHand.MAIN_HAND,affinity.offering());
            h.assertTrue(!AffinityRitual.activate(p,center,InteractionHand.MAIN_HAND),"Penalty allowed rebinding");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void dreamAttributesLootAndCommands(GameTestHelper h) throws Exception {
        var p=player(h);AffinityCommands.apply(p,Affinity.DREAM);
        h.assertTrue(p.getAttributeValue(ModAttributes.MAX_MANA)==300 && p.mayFly()
                && Math.abs(p.getAttributeValue(ModAttributes.DAMAGE_TAKEN)-1.5)<.001,
                "Dream capacity, vulnerability or creative flight missing");
        h.assertTrue(p.getData(ModAttachments.DREAM_AFFINITY) && p.getData(ModAttachments.MANA).affinity()==4,"Dream visuals not synced");
        var cow=h.spawn(EntityType.COW,2,2,2);cow.hurt(p.damageSources().playerAttack(p),5);
        h.assertTrue(Math.abs(cow.getHealth()-3.5)<.001,"Dream melee damage");cow.discard();
        for(var item:new net.minecraft.world.item.Item[]{ModItems.CRYSTALLIZED_NAIL.get(),ModItems.ARCANE_NEEDLE.get()}) {
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item));
            h.assertTrue(Math.abs(item.getAttackDamageBonus(p,10,p.damageSources().playerAttack(p))-6)<.001,"Dream melee/spell bonuses must add");
        }
        var damage=new net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre(p,new net.neoforged.neoforge.common.damagesource.DamageContainer(p.damageSources().magic(),4));
        com.mcmagic.omnira.fate.FateEffects.damage(damage);h.assertTrue(damage.getNewDamage()==6,"Dream vulnerability missing");
        var ench=h.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        var fortune=ench.getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE);
        var looting=ench.getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING);
        var tool=new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE);tool.enchant(fortune,3);
        h.assertTrue(DreamAffinityLoot.mining(p,()->net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(fortune,tool))==4,"Fortune not added to existing enchantment");
        h.assertTrue(tool.getEnchantmentLevel(fortune)==3 && !DreamAffinityLoot.mining(),"Fortune leaked onto equipment or context");
        h.assertTrue(DreamAffinityLoot.mining(p,()->net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(fortune,ItemStack.EMPTY))==1,"Bare-hand Fortune missing");
        p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        h.assertTrue(net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel(looting,p)==1,"Unarmed Looting missing");
        var sword=new ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD);sword.enchant(looting,3);p.setItemInHand(InteractionHand.MAIN_HAND,sword);
        h.assertTrue(net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel(looting,p)==4 && sword.getEnchantmentLevel(looting)==3,"Looting not additive or mutated sword");
        p.getAbilities().flying=true;
        var speed=net.minecraft.world.entity.player.Player.class.getDeclaredMethod("getFlyingSpeed");speed.setAccessible(true);
        h.assertTrue(Math.abs((float)speed.invoke(p)-p.getAbilities().getFlyingSpeed()*.5F)<.0001,"Horizontal flight not halved");
        var dispatcher=h.getLevel().getServer().getCommands().getDispatcher();
        var source=p.createCommandSourceStack().withPermission(2);
        for(var affinity:Affinity.values()) {
            dispatcher.execute("omnira affinity set "+affinity.name().toLowerCase(java.util.Locale.ROOT),source);
            h.assertTrue(p.getData(ModAttachments.AFFINITY).active()==affinity,"Direct affinity command failed");
        }
        AffinityCommands.apply(p,Affinity.NONE);
        h.assertTrue(!p.mayFly() && !p.getAbilities().flying && !p.getData(ModAttachments.DREAM_AFFINITY),"Dream removal retained flight or translucency");
        p.getAbilities().mayfly=true;AffinityCommands.apply(p,Affinity.DREAM);p.getAbilities().flying=true;AffinityCommands.apply(p,Affinity.NONE);
        h.assertTrue(p.mayFly() && p.getAbilities().flying,"Affinity removal stole external flight permission");h.succeed();
    }
    @GameTest(template="spell_arena") public static void ritualInterruptionsAndUnorderedInputs(GameTestHelper h) {
        var p=player(h);var center=h.absolutePos(new BlockPos(4,2,4));var level=h.getLevel();
        level.setBlockAndUpdate(center,ModBlocks.ADVANCED_RITUAL_ENERGY_CORE.get().defaultBlockState());
        var pedestals=new java.util.ArrayList<com.mcmagic.omnira.block.entity.CrystalPedestalBlockEntity>();
        for(var offset:DreamRitual.ANCHORS) {
            var pos=center.offset(offset[0],0,offset[1]);level.setBlockAndUpdate(pos,ModBlocks.CRYSTAL_PEDESTAL.get().defaultBlockState());
            pedestals.add((com.mcmagic.omnira.block.entity.CrystalPedestalBlockEntity)level.getBlockEntity(pos));
        }
        fill(pedestals,0);p.setItemInHand(InteractionHand.MAIN_HAND,Affinity.LIGHT.offering());
        p.setPos(center.getX()+4,center.getY(),center.getZ()+.5);
        h.assertTrue(!AffinityRitual.activate(p,center,InteractionHand.MAIN_HAND),"Ritual accepted outside player");
        p.setPos(center.getX()+1.5,center.getY(),center.getZ()+.5);
        level.setBlockAndUpdate(center,ModBlocks.RITUAL_ENERGY_CORE.get().defaultBlockState());
        h.assertTrue(!AffinityRitual.activate(p,center,InteractionHand.MAIN_HAND),"Ordinary core accepted affinity ritual");
        level.setBlockAndUpdate(center,ModBlocks.ADVANCED_RITUAL_ENERGY_CORE.get().defaultBlockState());
        pedestals.get(7).setItem(0,pedestals.get(0).getItem(0).copy());
        h.assertTrue(!AffinityRitual.activate(p,center,InteractionHand.MAIN_HAND),"Duplicate material substituted for missing ingredient");
        for(int shift=0;shift<8;shift++) {
            fill(pedestals,shift);
            h.assertTrue(AffinityRitual.activate(p,center,InteractionHand.MAIN_HAND),"Unordered permutation rejected");
            AffinityRitual.advance(p);
            h.assertTrue(Math.abs(p.getX()-center.getX()-.5)<.001 && p.getY()>center.getY()+1,"Acquisition did not hold above core");
            pedestals.get(0).setItem(0,ItemStack.EMPTY);AffinityRitual.advance(p);
            h.assertTrue(!AffinityRitual.active(p) && !p.getMainHandItem().isEmpty() && !pedestals.get(1).getItem(0).isEmpty(),"Interrupted ritual consumed materials or kept hold");
        }
        AffinityCommands.apply(p,Affinity.DREAM);fill(pedestals,3);p.setItemInHand(InteractionHand.MAIN_HAND,Affinity.DREAM.offering());
        p.setPos(center.getX()+1.5,center.getY(),center.getZ()+.5);
        h.assertTrue(AffinityRitual.activate(p,center,InteractionHand.MAIN_HAND),"Dream removal failed to start");
        for(int i=0;i<60;i++)AffinityRitual.advance(p);
        p.setPos(center.getX()+5,center.getY(),center.getZ());AffinityRitual.advance(p);
        h.assertTrue(!AffinityRitual.active(p) && p.getData(ModAttachments.DREAM_SOLIDIFY)==0 && p.getData(ModAttachments.DREAM_AFFINITY),"Interrupted dream removal lost affinity or retained partial opacity");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void damageAndFixedCost(GameTestHelper h) {
        var p=player(h);
        p.getAttribute(ModAttributes.COST_REDUCTION).setBaseValue(100);
        for(var affinity:new Affinity[]{Affinity.LIGHT,Affinity.DARK}) {
            state(p,affinity);p.setData(ModAttachments.MANA,new ManaState(40,100));
            var cow=h.spawn(EntityType.COW,2,2,2);cow.hurt(p.damageSources().playerAttack(p),1);
            h.assertTrue(p.getData(ModAttachments.MANA).current()==20,"Hit cost must ignore reduction");
            h.assertTrue(affinity==Affinity.LIGHT?cow.isOnFire():cow.getEffect(MobEffects.POISON)!=null && cow.getEffect(MobEffects.POISON).getAmplifier()==1,"Missing hit effect");
            cow.discard();p.setData(ModAttachments.MANA,new ManaState(19,100));
            var other=h.spawn(EntityType.COW,2,2,2);other.hurt(p.damageSources().playerAttack(p),1);
            h.assertTrue(p.getData(ModAttachments.MANA).current()==19 && !other.isOnFire() && !other.hasEffect(MobEffects.POISON),"Unaffordable effect applied");other.discard();
        }
        state(p,Affinity.ELEMENTAL);p.setData(ModAttachments.MANA,new ManaState(20,200));
        var event=new net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre(p,
                new net.neoforged.neoforge.common.damagesource.DamageContainer(p.damageSources().magic(),5));
        AffinityEffects.defend(event);
        h.assertTrue(event.getNewDamage()==3 && p.getData(ModAttachments.MANA).current()==0,"Flat defense or charge wrong");
        var unpaid=new net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre(p,
                new net.neoforged.neoforge.common.damagesource.DamageContainer(p.damageSources().magic(),5));
        AffinityEffects.defend(unpaid);h.assertTrue(unpaid.getNewDamage()==5,"Free defense with no mana");h.succeed();
    }
    @GameTest(template="spell_arena") public static void penaltyScheduleAndDeath(GameTestHelper h) {
        final int[] hits={0};final float[] total={0};
        var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"penalty-test")) {
            @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source,float amount) {
                h.assertTrue(source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_COOLDOWN),"Penalty lost to combat cooldown");
                hits[0]++;total[0]+=amount;return true;
            }
        };
        for(var affinity:new Affinity[]{Affinity.LIGHT,Affinity.ELEMENTAL}) {
            hits[0]=0;total[0]=0;p.setData(ModAttachments.AFFINITY,new AffinityState(affinity.ordinal(),2400));
            for(int tick=0;tick<2400;tick++)AffinityEffects.tick(p);
            h.assertTrue(hits[0]==240 && total[0]==240,"Half-second penalty must deal 240 one-point pulses");
            h.assertTrue(p.getData(ModAttachments.AFFINITY).equals(AffinityState.NONE),"Surviving does not clear affinity");
        }
        state(p,Affinity.LIGHT);
        AffinityEffects.died(new net.neoforged.neoforge.event.entity.living.LivingDeathEvent(p,p.damageSources().magic()));
        h.assertTrue(p.getData(ModAttachments.AFFINITY).active()==Affinity.LIGHT,"Ordinary death loses affinity");
        p.setData(ModAttachments.AFFINITY,new AffinityState(Affinity.DARK.ordinal(),1100));
        AffinityEffects.died(new net.neoforged.neoforge.event.entity.living.LivingDeathEvent(p,p.damageSources().magic()));
        h.assertTrue(p.getData(ModAttachments.AFFINITY).equals(AffinityState.NONE),"Penalty death retains affinity");h.succeed();
    }
    @GameTest(template="spell_arena") public static void darkPenaltyClouds(GameTestHelper h) {
        var p=player(h);p.setData(ModAttachments.AFFINITY,new AffinityState(Affinity.DARK.ordinal(),1200));
        var bounds=new net.minecraft.world.phys.AABB(p.position(),p.position()).inflate(3);
        for(int tick=1;tick<=1200;tick++) {
            AffinityEffects.tick(p);
            if(tick%100==0) {
                var clouds=h.getLevel().getEntitiesOfClass(com.mcmagic.omnira.entity.ShadowMist.class,bounds);
                h.assertTrue(clouds.size()==tick/100,"Dark penalty must generate one cloud every five seconds");
                h.assertTrue(clouds.stream().allMatch(c->c.spellCloud() && c.lifetime()==400 && c.diameter(0)==3),"Wrong penalty mist configuration");
            }
        }
        h.assertTrue(p.getData(ModAttachments.AFFINITY).equals(AffinityState.NONE),"Dark penalty never ends");
        var clouds=h.getLevel().getEntitiesOfClass(com.mcmagic.omnira.entity.ShadowMist.class,bounds);
        var cow=h.spawn(EntityType.COW,4,2,4);cow.setNoAi(true);cow.setNoGravity(true);
        for(int i=0;i<20;i++)clouds.getFirst().tick();
        h.assertTrue(cow.getHealth()==9,"Penalty mist does not cause base one-point damage");
        clouds.forEach(net.minecraft.world.entity.Entity::discard);cow.discard();h.succeed();
    }
}
