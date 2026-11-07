package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.fate.ManuscriptReward;
import com.mcmagic.omnira.item.FateCurioItem;
import com.mcmagic.omnira.registry.ModItems;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.*;

/** Exercise the installed Curios death and respawn handlers, not only item flags. */
@GameTestHolder("omnira_fate_death") @PrefixGameTestTemplate(false)
public final class FateDeathGameTests {
    private static FakePlayer player(GameTestHelper h){
        var p=new FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"fate_death"));
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);return p;
    }
    @GameTest(template="spell_arena") public static void everyFateSurvivesNormalDeath(GameTestHelper h){checkAll(h,false);}
    @GameTest(template="spell_arena") public static void everyFateSurvivesKeepInventoryDeath(GameTestHelper h){checkAll(h,true);}
    private static void checkAll(GameTestHelper h,boolean keep){
        var rule=h.getLevel().getGameRules().getRule(GameRules.RULE_KEEPINVENTORY);boolean previous=rule.get();
        rule.set(keep,h.getLevel().getServer());
        try {
            int checked=0;
            for(var item:net.minecraft.core.registries.BuiltInRegistries.ITEM){
                if(!(item instanceof FateCurioItem fate))continue;
                var old=player(h);String slot=fate.kind().talent()?"talent":"curse";
                h.assertTrue(ManuscriptReward.grant(old,slot,new ItemStack(item)),"Could not equip fate fixture");
                var drops=new java.util.ArrayList<net.minecraft.world.entity.item.ItemEntity>();
                NeoForge.EVENT_BUS.post(new LivingDropsEvent(old,old.damageSources().generic(),drops,true));
                h.assertTrue(drops.stream().noneMatch(e->e.getItem().getItem() instanceof FateCurioItem),"Fate dropped on death: "+fate.kind());
                h.assertTrue(ManuscriptReward.equipped(old,slot)==fate.kind(),"Death removed fate before clone: "+fate.kind());
                var next=player(h);NeoForge.EVENT_BUS.post(new PlayerEvent.Clone(next,old,true));
                h.assertTrue(ManuscriptReward.equipped(next,slot)==fate.kind(),"Respawn lost fate or its earned slot: "+fate.kind());
                var inv=top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(next).orElseThrow();
                h.assertTrue(inv.getStacksHandler(slot).orElseThrow().getStacks().getStackInSlot(0).getCount()==1,"Respawn duplicated fate");checked++;
            }
            h.assertTrue(checked==FateCurioItem.Kind.values().length,"Some registered fate kinds were not tested");
        }finally{rule.set(previous,h.getLevel().getServer());}h.succeed();
    }
    @GameTest(template="spell_arena") public static void cleansedCurseDoesNotReturnAfterDeath(GameTestHelper h){
        var old=player(h);ManuscriptReward.grant(old,"talent",new ItemStack(ModItems.TIME_THIEF_TALENT.get()));
        ManuscriptReward.grant(old,"curse",new ItemStack(ModItems.AMON_GAZE.get()));ManuscriptReward.cleanseCurse(old);
        NeoForge.EVENT_BUS.post(new LivingDropsEvent(old,old.damageSources().generic(),new java.util.ArrayList<>(),true));
        var next=player(h);NeoForge.EVENT_BUS.post(new PlayerEvent.Clone(next,old,true));
        h.assertTrue(ManuscriptReward.equipped(next,"talent")==FateCurioItem.Kind.TIME_THIEF&&ManuscriptReward.equipped(next,"curse")==null,"Respawn restored cleansed curse or lost retained talent");
        h.assertTrue(!ManuscriptReward.grant(next,"talent",new ItemStack(ModItems.ARCANIST_TALENT.get())),"Respawn bypassed talent exclusion");h.succeed();
    }
}
