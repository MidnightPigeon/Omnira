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
        p.setPos(h.absoluteVec(new Vec3(8,40,8)));h.getLevel().addFreshEntity(p);
        SpacetimeStorm.affect(h.getLevel(),p);
        var amber=h.getLevel().getEntitiesOfClass(TemporalAmber.class,p.getBoundingBox().inflate(2)).getFirst();
        h.assertTrue(amber.storedStacks()==7 && p.getInventory().isEmpty() && p.containerMenu.getCarried().isEmpty(),"Not all carried inventories sealed");
        h.assertTrue(curios.getEquippedCurios().getStackInSlot(0).isEmpty() && cosmetics.getStackInSlot(0).isEmpty(),"Curios retained duplicate");
        var tag=new net.minecraft.nbt.CompoundTag();amber.saveWithoutId(tag);
        var loaded=ModEntityTypes.TEMPORAL_AMBER.get().create(h.getLevel());loaded.load(tag);
        var survivor=new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(),h.getLevel(),p.getGameProfile(),net.minecraft.server.level.ClientInformation.createDefault());
        survivor.connection=p.connection;p=survivor;
        curios=CuriosApi.getCuriosInventory(p).orElseThrow();
        cosmetics=curios.getStacksHandler("spell_core").orElseThrow().getCosmeticStacks();
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
        pig.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.EMERALD));pig.setDropChance(EquipmentSlot.MAINHAND,2);
        SpacetimeStorm.trigger(level,center);
        var area=new AABB(center,center).inflate(10);
        var amber=level.getEntitiesOfClass(TemporalAmber.class,area);
        h.assertTrue(p.getHealth()==0 && !pig.isAlive() && !pig.isRemoved() && amber.size()==1 && amber.getFirst().storedStacks()==2,"Storm did not use normal mob death or seal the deceased player");
        h.assertTrue(level.getEntitiesOfClass(ItemEntity.class,area).stream().anyMatch(e->e.getItem().is(Items.EMERALD)),"Storm mob death skipped native equipment drops");
        SpacetimeStorm.trigger(level,center);
        h.assertTrue(amber.getFirst().isRemoved() && level.getEntitiesOfClass(ItemEntity.class,area).isEmpty(),"Second storm spilled amber contents");h.succeed();
    }
    @GameTest(template="spell_arena") public static void canceledAmberSpawnAndCanceledDropsRespectBothRules(GameTestHelper h){
        var level=h.getLevel();
        var rule=level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY);
        boolean previous=rule.get();
        var bus=net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        try{
            for(boolean keep:new boolean[]{false,true})for(boolean rejectSpawn:new boolean[]{false,true}){
                rule.set(keep,level.getServer());
                var p=player(h);p.setPos(h.absoluteVec(new Vec3(8,40+(keep?10:0)+(rejectSpawn?5:0),8)));
                level.addFreshEntity(p);p.getInventory().setItem(3,new ItemStack(Items.DIAMOND));
                java.util.function.Consumer<net.neoforged.neoforge.event.entity.EntityJoinLevelEvent> join=e->{
                    if(rejectSpawn && e.getEntity() instanceof TemporalAmber a && p.getUUID().equals(a.owner()))e.setCanceled(true);
                };
                var extra=new ItemEntity(level,p.getX(),p.getY(),p.getZ(),new ItemStack(Items.EMERALD));
                java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.LivingDropsEvent> drops=e->{
                    if(!rejectSpawn && e.getEntity()==p){e.getDrops().add(extra);e.setCanceled(true);}
                };
                bus.addListener(join);bus.addListener(drops);
                try{SpacetimeStorm.affect(level,p);}finally{bus.unregister(join);bus.unregister(drops);}
                var area=p.getBoundingBox().inflate(2);
                var ambers=level.getEntitiesOfClass(TemporalAmber.class,area);
                var items=level.getEntitiesOfClass(ItemEntity.class,area);
                h.assertTrue(p.getInventory().getItem(3).is(Items.DIAMOND)==keep,"Incorrect retained inventory");
                if(rejectSpawn){
                    h.assertTrue(ambers.isEmpty() && AmberDirectory.get(level).grave(p.getUUID())==null,"Rejected amber registered");
                    h.assertTrue(items.stream().filter(e->e.getItem().is(Items.DIAMOND)).count()==(keep?0:1),"Rejected amber lost or duplicated drop");
                }else{
                    h.assertTrue(ambers.size()==1 && ambers.getFirst().storedStacks()==(keep?0:1),"Amber intercepted canceled drops");
                    h.assertTrue(items.isEmpty(),"Canceled drop leaked into world");
                }
            }
        }finally{rule.set(previous,level.getServer());}
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void keepInventoryAndVanishingCannotDuplicateOrDestroySealedItems(GameTestHelper h){
        var level=h.getLevel();var rule=level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY);boolean old=rule.get();
        try{
            for(boolean keep:new boolean[]{false,true}){
                rule.set(keep,level.getServer());var p=player(h);p.setPos(h.absoluteVec(new Vec3(8,55+(keep?20:0),8)));level.addFreshEntity(p);
                var sword=new ItemStack(Items.DIAMOND_SWORD);
                sword.enchant(level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.VANISHING_CURSE),1);
                p.getInventory().setItem(2,sword);
                com.mcmagic.omnira.fate.ManuscriptReward.grant(p,"curse",new ItemStack(ModItems.AMON_GAZE.get()));
                java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.LivingDropsEvent> extra=e->{
                    if(e.getEntity()==p)e.getDrops().add(new ItemEntity(level,p.getX(),p.getY(),p.getZ(),new ItemStack(Items.EMERALD)));
                };
                net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(extra);
                try{SpacetimeStorm.affect(level,p);}finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(extra);}
                var amber=level.getEntitiesOfClass(TemporalAmber.class,p.getBoundingBox().inflate(2));
                h.assertTrue(amber.size()==1 && amber.getFirst().storedStacks()==(keep?0:2),"Incorrect amber contents");
                h.assertTrue(com.mcmagic.omnira.fate.ManuscriptReward.equipped(p,"curse")==com.mcmagic.omnira.item.FateCurioItem.Kind.AMON_GAZE,"Amber took retained curse");
                var drops=level.getEntitiesOfClass(ItemEntity.class,p.getBoundingBox().inflate(2));
                if(keep){
                    h.assertTrue(ItemStack.matches(sword,p.getInventory().getItem(2)),"Keep inventory lost sword");
                    h.assertTrue(drops.size()==1 && drops.getFirst().getItem().is(Items.EMERALD),"Marker intercepted an external drop");
                    var respawn=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,p.getGameProfile(),net.minecraft.server.level.ClientInformation.createDefault());
                    respawn.connection=p.connection;
                    respawn.restoreFrom(p,false);
                    h.assertTrue(ItemStack.matches(sword,respawn.getInventory().getItem(2)),"Respawn lost retained item");
                    var id=amber.getFirst().getUUID();
                    h.assertTrue(com.mcmagic.omnira.travel.WaymarkDirectory.get(level).knows(respawn.getUUID(),id),"Empty amber missing recall marker");
                }else h.assertTrue(p.getInventory().isEmpty() && drops.isEmpty(),"Death leaked sealed items");
            }
        }finally{rule.set(old,level.getServer());}
        h.succeed();
    }
}
