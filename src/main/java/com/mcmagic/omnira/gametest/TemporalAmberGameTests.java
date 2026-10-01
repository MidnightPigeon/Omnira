package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.spacetime.*;
import com.mcmagic.omnira.registry.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
import top.theillusivec4.curios.api.CuriosApi;

@GameTestHolder("omnira_spacetime")
@PrefixGameTestTemplate(false)
public final class TemporalAmberGameTests {
    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h){
        var profile=new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"amber-test");
        var p=new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(),h.getLevel(),profile,net.minecraft.server.level.ClientInformation.createDefault());
        p.connection=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),profile).connection;
        return p;
    }
    @GameTest(template="spell_arena") public static void restoresExactSlotsAndCuriosAfterReload(GameTestHelper h){
        var p=player(h);var tool=new ItemStack(Items.DIAMOND_SWORD);tool.setDamageValue(87);
        tool.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Saved sword"));
        p.getInventory().setItem(7,tool.copy());p.getInventory().setItem(38,new ItemStack(Items.DIAMOND_CHESTPLATE));
        p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.SHIELD));
        var curios=CuriosApi.getCuriosInventory(p).orElseThrow();var core=new ItemStack(ModItems.DREAM_SPELL_CORE.get());
        curios.setEquippedCurio("spell_core",0,core.copy());
        var cosmetics=curios.getStacksHandler("spell_core").orElseThrow().getCosmeticStacks();
        cosmetics.setStackInSlot(0,core.copy());
        p.containerMenu.setCarried(new ItemStack(Items.EMERALD,3));
        p.inventoryMenu.getSlot(1).set(new ItemStack(Items.GOLD_INGOT,2));
        var amber=TemporalAmber.capture(p);
        h.assertTrue(amber.storedStacks()==7 && p.getInventory().isEmpty() && p.containerMenu.getCarried().isEmpty(),"Not all carried inventories sealed");
        h.assertTrue(curios.getEquippedCurios().getStackInSlot(0).isEmpty() && cosmetics.getStackInSlot(0).isEmpty(),"Curios retained duplicate");
        var tag=new net.minecraft.nbt.CompoundTag();amber.saveWithoutId(tag);
        var loaded=ModEntityTypes.TEMPORAL_AMBER.get().create(h.getLevel());loaded.load(tag);
        loaded.interact(player(h),InteractionHand.MAIN_HAND);
        h.assertTrue(loaded.storedStacks()==7,"Non-owner retrieved contents");
        loaded.interact(p,InteractionHand.MAIN_HAND);
        h.assertTrue(ItemStack.matches(p.getInventory().getItem(7),tool),"Original slot or item components lost");
        h.assertTrue(p.getInventory().getItem(38).is(Items.DIAMOND_CHESTPLATE) && p.getOffhandItem().is(Items.SHIELD),"Armor/offhand not restored");
        h.assertTrue(ItemStack.matches(curios.findCurio("spell_core",0).orElseThrow().stack(),core) && ItemStack.matches(cosmetics.getStackInSlot(0),core),"Curios/cosmetics not restored");
        h.assertTrue(loaded.isRemoved() && loaded.storedStacks()==0,"Claim did not consume stored contents");h.succeed();
    }
    @GameTest(template="spell_arena") public static void fullInventoryRetainsOverflowAndBreakDropsOnce(GameTestHelper h){
        var p=player(h);p.getInventory().setItem(0,new ItemStack(Items.DIAMOND,5));var amber=TemporalAmber.capture(p);
        amber.setPos(Vec3.atCenterOf(h.absolutePos(new net.minecraft.core.BlockPos(5,20,5))));h.getLevel().addFreshEntity(amber);
        for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));
        amber.interact(p,InteractionHand.MAIN_HAND);
        h.assertTrue(amber.storedStacks()==1 && !amber.isRemoved() && p.getInventory().getItem(0).is(Items.COBBLESTONE),"Claim overwrote occupied inventory or lost overflow");
        amber.hurt(h.getLevel().damageSources().playerAttack(p),1);amber.hurt(h.getLevel().damageSources().playerAttack(p),1);
        var items=h.getLevel().getEntitiesOfClass(ItemEntity.class,amber.getBoundingBox().inflate(2));
        h.assertTrue(items.size()==1 && items.getFirst().getItem().getCount()==5,"Break duplicated or omitted contents");h.succeed();
    }
    @GameTest(template="spell_arena") public static void stormDeathSealsOnceAndNextStormErases(GameTestHelper h){
        var level=h.getLevel();var p=player(h);var center=Vec3.atCenterOf(h.absolutePos(new net.minecraft.core.BlockPos(5,35,5)));
        p.setPos(center);p.getInventory().setItem(0,new ItemStack(Items.DIAMOND,4));level.addFreshEntity(p);
        CuriosApi.getCuriosInventory(p).orElseThrow().setEquippedCurio("spell_core",0,new ItemStack(ModItems.TEST_SPELL_CORE.get()));
        var pig=EntityType.PIG.create(level);pig.setPos(center.add(2,0,0));level.addFreshEntity(pig);
        SpacetimeStorm.trigger(level,center);
        var area=new AABB(center,center).inflate(10);
        var amber=level.getEntitiesOfClass(TemporalAmber.class,area);
        h.assertTrue(p.getHealth()==0 && pig.isRemoved() && amber.size()==1 && amber.getFirst().storedStacks()==2,"Death did not seal player or erase mob");
        h.assertTrue(level.getEntitiesOfClass(ItemEntity.class,area).isEmpty(),"Storm death generated loose loot");
        SpacetimeStorm.trigger(level,center);
        h.assertTrue(amber.getFirst().isRemoved() && level.getEntitiesOfClass(ItemEntity.class,area).isEmpty(),"Second storm spilled amber contents");h.succeed();
    }
    @GameTest(template="spell_arena") public static void keepInventoryAndVanishingCannotDuplicateOrDestroySealedItems(GameTestHelper h){
        var level=h.getLevel();var rule=level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY);boolean old=rule.get();
        try{
            for(boolean keep:new boolean[]{false,true}){
                rule.set(keep,level.getServer());var p=player(h);p.setPos(h.absoluteVec(new Vec3(8,55+(keep?20:0),8)));level.addFreshEntity(p);
                var sword=new ItemStack(Items.DIAMOND_SWORD);
                sword.enchant(level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.VANISHING_CURSE),1);
                p.getInventory().setItem(2,sword);
                java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.LivingDropsEvent> extra=e->{
                    if(e.getEntity()==p)e.getDrops().add(new ItemEntity(level,p.getX(),p.getY(),p.getZ(),new ItemStack(Items.EMERALD)));
                };
                net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(extra);
                try{SpacetimeStorm.affect(level,p);}finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(extra);}
                var amber=level.getEntitiesOfClass(TemporalAmber.class,p.getBoundingBox().inflate(2));
                h.assertTrue(amber.size()==1 && amber.getFirst().storedStacks()==2 && p.getInventory().isEmpty(),"Keep-inventory/vanishing/death-event drop was not sealed");
                h.assertTrue(level.getEntitiesOfClass(ItemEntity.class,p.getBoundingBox().inflate(2)).isEmpty(),"Death event leaked extra drop");
            }
        }finally{rule.set(old,level.getServer());}
        h.succeed();
    }
}
