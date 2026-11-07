package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.fate.*;
import com.mcmagic.omnira.item.FateCurioItem;
import com.mcmagic.omnira.registry.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.common.util.FakePlayer;

@GameTestHolder("omnira_predator") @PrefixGameTestTemplate(false)
public final class PredatorFateGameTests {
    private static FakePlayer player(GameTestHelper h){
        var p=new FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"predator")){
            @Override public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source){return false;}
            // FakePlayer's network listener intentionally ignores teleport packets.
            @Override public void teleportTo(double x,double y,double z){setPos(x,y,z);}
        };
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        try{var field=net.minecraft.server.level.ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");field.setAccessible(true);field.setInt(p,0);}
        catch(ReflectiveOperationException e){throw new RuntimeException(e);}
        p.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(5,3,5)));return p;
    }
    @GameTest(template="spell_arena") public static void grantAndCleanseRemainExclusive(GameTestHelper h){
        var p=player(h);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.PLUNDER_SCYTHE.get()));
        ModItems.PLUNDER_SCYTHE.get().use(h.getLevel(),p,InteractionHand.MAIN_HAND);
        h.assertTrue(p.getMainHandItem().isEmpty()&&PredatorFate.extraLoot(p),"Scythe did not grant talent and consume");
        h.assertTrue(ManuscriptReward.equipped(p,"curse")==FateCurioItem.Kind.SPATIAL_OVERLOAD,"Missing curse");
        var item=(FateCurioItem)ModItems.SPATIAL_OVERLOAD.get();
        var attrs=item.getAttributeModifiers(new top.theillusivec4.curios.api.SlotContext("curse",p,0,false,true),net.minecraft.resources.ResourceLocation.parse("omnira:test"),new ItemStack(item));
        h.assertTrue(attrs.get(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).iterator().next().amount()==-.2&&attrs.get(ModAttributes.DAMAGE_TAKEN).iterator().next().amount()==.5,"Wrong curse modifiers");
        ManuscriptReward.cleanseCurse(p);
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.PLUNDER_SCYTHE.get()));
        ModItems.PLUNDER_SCYTHE.get().use(h.getLevel(),p,InteractionHand.MAIN_HAND);
        h.assertTrue(p.getMainHandItem().getCount()==1&&ManuscriptReward.equipped(p,"curse")==null,"Cleansing allowed second pair");h.succeed();
    }
    @GameTest(template="spell_arena") public static void customKillTableRollsTwice(GameTestHelper h){
        var p=player(h);ManuscriptReward.grant(p,"talent",new ItemStack(ModItems.APEX_PREDATOR.get()));
        var cow=h.spawn(net.minecraft.world.entity.EntityType.COW,5,3,5);cow.setNoAi(true);
        var data=new net.minecraft.nbt.CompoundTag();cow.addAdditionalSaveData(data);
        data.putString("DeathLootTable","omnira:tests/predator");cow.readAdditionalSaveData(data);
        var area=cow.getBoundingBox().inflate(2);cow.hurt(p.damageSources().playerAttack(p),100);
        int count=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,area).stream().filter(e->e.getItem().is(Items.DIAMOND)).mapToInt(e->e.getItem().getCount()).sum();
        h.assertTrue(count==2,"Expected two independent custom-table rolls, got "+count);h.succeed();
    }
    @GameTest(template="spell_arena") public static void actualDamageTeleportsAndCleanseStopsIt(GameTestHelper h){
        for(int x=0;x<24;x++)for(int z=0;z<24;z++)h.setBlock(new net.minecraft.core.BlockPos(x,2,z),net.minecraft.world.level.block.Blocks.STONE);
        var p=player(h);p.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(12,3,12)));
        ManuscriptReward.grant(p,"curse",new ItemStack(ModItems.SPATIAL_OVERLOAD.get()));
        p.getRandom().setSeed(42);var before=p.position();p.hurt(p.damageSources().generic(),1);
        h.assertTrue(p.position().distanceToSqr(before)>=1,"Curse did not teleport on actual damage");
        ManuscriptReward.cleanseCurse(p);before=p.position();p.invulnerableTime=0;p.hurt(p.damageSources().generic(),1);
        h.assertTrue(p.position().equals(before),"Cleansed curse still teleports");h.succeed();
    }
    @GameTest(template="spell_arena") public static void fishingRollsTwiceAndCancellationStillWorks(GameTestHelper h) throws ReflectiveOperationException {
        var p=player(h);ManuscriptReward.grant(p,"talent",new ItemStack(ModItems.APEX_PREDATOR.get()));
        var rod=new ItemStack(Items.FISHING_ROD);p.setItemInHand(InteractionHand.MAIN_HAND,rod);
        int[] calls={0};
        java.util.function.Consumer<net.neoforged.neoforge.event.entity.player.ItemFishedEvent> listener=e->{
            if(e.getHookEntity().getPlayerOwner()==p){calls[0]++;h.assertTrue(e.getDrops().size()==2,"Fishing did not roll twice");e.setCanceled(true);}
        };
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(listener);
        try{
            var hook=new net.minecraft.world.entity.projectile.FishingHook(p,h.getLevel(),0,0);hook.setPos(p.position());
            h.getLevel().addFreshEntity(hook);
            var nibble=net.minecraft.world.entity.projectile.FishingHook.class.getDeclaredField("nibble");nibble.setAccessible(true);nibble.setInt(hook,20);
            hook.retrieve(rod);
            h.assertTrue(calls[0]==1&&hook.isRemoved(),"Canceled fishing did not settle once");
            h.assertTrue(h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,p.getBoundingBox().inflate(3)).isEmpty(),"Canceled catch leaked drops");
        }finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(listener);}h.succeed();
    }
}
