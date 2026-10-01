package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.mire.*;
import com.mcmagic.omnira.registry.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.core.component.DataComponents;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_time_passage")
@PrefixGameTestTemplate(false)
public final class TimePassageGameTests {
    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h){
        var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"time-test"),false);
        var p=new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(),h.getLevel(),cookie.gameProfile(),cookie.clientInformation());
        p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(h.getLevel().getServer(),new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p,cookie){
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet){}
        };
        p.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(5,3,5)));return p;
    }
    private static void tick(net.minecraft.server.level.ServerPlayer p,int count){
        for(int i=0;i<count;i++){
            var effect=p.getEffect(ModEffects.TIME_PASSAGE);
            if(effect!=null&&!effect.tick(p,()->{}))p.removeEffect(ModEffects.TIME_PASSAGE);
        }
    }
    @GameTest(template="spell_arena") public static void snapshotsAndDurations(GameTestHelper h){
        var p=player(h);p.setHealth(18);p.getFoodData().setFoodLevel(17);var origin=p.position();
        p.addEffect(new MobEffectInstance(ModEffects.TIME_PASSAGE,1800));
        for(int round=0;round<3;round++){
            p.setHealth(4);p.getFoodData().setFoodLevel(3);p.setPos(origin.add(4,0,0));tick(p,599);
            h.assertTrue(p.getHealth()==4,"Rewind was early");tick(p,1);
            h.assertTrue(p.getHealth()==18&&p.getFoodData().getFoodLevel()==17,"Did not reuse initial statistics");
            h.assertTrue(p.position().distanceTo(origin)<.01,"Did not rewind initial position");
        }
        h.assertTrue(!p.hasEffect(ModEffects.TIME_PASSAGE),"Extended effect did not end");
        p.addEffect(new MobEffectInstance(ModEffects.TIME_PASSAGE,200));p.setHealth(5);tick(p,199);
        h.assertTrue(p.getHealth()==5,"Food effect fired early");tick(p,1);h.assertTrue(p.getHealth()==18,"Food effect did not restore");
        p.addEffect(new MobEffectInstance(ModEffects.TIME_PASSAGE,200));p.setHealth(6);p.removeEffect(ModEffects.TIME_PASSAGE);tick(p,201);
        h.assertTrue(p.getHealth()==6,"Cleansing triggered rewind");
        p.addEffect(new MobEffectInstance(ModEffects.TIME_PASSAGE,200));p.setHealth(0);TimePassage.restore(p,1);
        h.assertTrue(p.getHealth()==0,"Effect revived dead player");h.succeed();
    }
    @GameTest(template="spell_arena",templateNamespace="omnira_time_brewing") public static void strongAndBrewing(GameTestHelper h){
        var p=player(h);var sword=new ItemStack(Items.IRON_SWORD);sword.setDamageValue(30);p.setItemSlot(EquipmentSlot.MAINHAND,sword);
        var helmet=new ItemStack(Items.IRON_HELMET);helmet.setDamageValue(20);p.setItemSlot(EquipmentSlot.HEAD,helmet);
        p.addEffect(new MobEffectInstance(ModEffects.TIME_PASSAGE,600,1));p.setHealth(2);p.getFoodData().setFoodLevel(1);
        p.setData(ModAttachments.MANA,p.getData(ModAttachments.MANA).withCurrent(0));tick(p,600);
        h.assertTrue(p.getHealth()==p.getMaxHealth()&&p.getFoodData().getFoodLevel()==20,"Strong restoration not full");
        var mana=p.getData(ModAttachments.MANA);h.assertTrue(mana.current()==mana.maximum()&&sword.getDamageValue()==0&&helmet.getDamageValue()==0,"Mana or equipment not repaired");
        var brewing=h.getLevel().potionBrewing();
        var awkward=PotionContents.createItemStack(Items.POTION,Potions.AWKWARD);
        var normal=brewing.mix(new ItemStack(MireContent.EEL_ITEM.get()),awkward);
        h.assertTrue(normal.get(DataComponents.POTION_CONTENTS).is(TimePassage.NORMAL),"Eel brewing missing");
        h.assertTrue(brewing.mix(new ItemStack(Items.REDSTONE),normal).get(DataComponents.POTION_CONTENTS).is(TimePassage.LONG),"Extended recipe missing");
        h.assertTrue(brewing.mix(new ItemStack(Items.GLOWSTONE_DUST),normal).get(DataComponents.POTION_CONTENTS).is(TimePassage.STRONG),"Strong recipe missing");
        h.assertTrue(MireContent.EEL_ITEM.get().getDefaultInstance().get(DataComponents.FOOD).effects().getFirst().effect().getDuration()==200,"Food duration wrong");h.succeed();
    }
}
