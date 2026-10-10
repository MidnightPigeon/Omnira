package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.item.*;
import com.mcmagic.omnira.registry.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_needle_donkey")
@PrefixGameTestTemplate(false)
public final class NeedleDonkeyGameTests {
    private static ServerPlayer player(GameTestHelper h,Item item){
        var profile=new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"needle-donkey");
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),profile,ClientInformation.createDefault());
        p.connection=new FakePlayer(h.getLevel(),profile).connection;p.setPos(h.absoluteVec(new Vec3(4.5,40,4.5)));p.setYRot(0);p.setXRot(0);
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item));p.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(8);return p;
    }
    private static LivingEntity target(GameTestHelper h,ServerPlayer p,EntityType<? extends LivingEntity> type,float health){
        var mob=h.spawn(type,4,40,4);mob.setNoGravity(true);if(mob instanceof net.minecraft.world.entity.Mob m)m.setNoAi(true);
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);mob.setHealth(health);mob.setPos(p.position().add(0,.5,3));return mob;
    }
    private static void throwNeedle(GameTestHelper h,ServerPlayer player){
        var needle=ModEntityTypes.FLYING_NEEDLE.get().create(h.getLevel());needle.launch(player,player.getMainHandItem());h.getLevel().addFreshEntity(needle);
        for(int i=0;i<7&&!needle.isRemoved();i++)needle.tick();needle.discard();
    }
    private static boolean awarded(GameTestHelper h,ServerPlayer p){return p.getAdvancements().getOrStartProgress(h.getLevel().getServer().getAdvancements().get(ResourceLocation.parse("omnira:needle_sick_donkey"))).isDone();}
    @GameTest(template="spell_arena") public static void allNeedleStagesAwardOnlyTheirOwner(GameTestHelper h){
        var items=new java.util.ArrayList<Item>();items.add(ModItems.ARCANE_NEEDLE.get());ModItems.NEEDLE_UPGRADES.forEach(i->items.add(i.get()));
        var observer=player(h,ModItems.ARCANE_NEEDLE.get());
        for(var item:items){var p=player(h,item);var donkey=target(h,p,EntityType.DONKEY,1);throwNeedle(h,p);
            h.assertTrue(!donkey.isAlive()&&awarded(h,p),"Needle kill did not award owner for "+item);donkey.discard();}
        h.assertTrue(!awarded(h,observer),"Unrelated player awarded");
        h.assertTrue(h.getLevel().getServer().getAdvancements().get(ResourceLocation.parse("omnira:needle_sick_donkey")).value().display().orElseThrow().isHidden(),"Advancement is not hidden");h.succeed();
    }
    @GameTest(template="spell_arena") public static void nonlethalOtherSpeciesAndMeleeDoNotAward(GameTestHelper h){
        var p=player(h,ModItems.ARCANE_NEEDLE.get());var donkey=target(h,p,EntityType.DONKEY,100);throwNeedle(h,p);
        h.assertTrue(donkey.isAlive()&&donkey.getHealth()<100&&!awarded(h,p),"Nonlethal hit awarded or missed");donkey.discard();
        p=player(h,ModItems.ARCANE_NEEDLE.get());var horse=target(h,p,EntityType.HORSE,1);throwNeedle(h,p);
        h.assertTrue(!horse.isAlive()&&!awarded(h,p),"Horse kill awarded donkey advancement");horse.discard();
        p=player(h,ModItems.ARCANE_NEEDLE.get());donkey=target(h,p,EntityType.DONKEY,1);SwordActions.strike(p,donkey,false);
        h.assertTrue(!donkey.isAlive()&&!awarded(h,p),"Ordinary needle melee awarded");donkey.discard();h.succeed();
    }
    private NeedleDonkeyGameTests(){}
}
