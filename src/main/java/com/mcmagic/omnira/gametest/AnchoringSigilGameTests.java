package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.spacetime.*;
import com.mcmagic.omnira.travel.*;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.mire.MireCycle;
import com.mcmagic.omnira.forging.AdvancedForgeRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import top.theillusivec4.curios.api.CuriosApi;
import java.util.UUID;

@GameTestHolder("omnira_anchoring")
@PrefixGameTestTemplate(false)
public final class AnchoringSigilGameTests {
    @GameTest(template="spell_arena") public static void wornSigilPreventsMireRewind(GameTestHelper h){
        var p=player(h);
        var level=h.getLevel();
        var holder=level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME).getHolderOrThrow(MireCycle.BIOME);
        level.getChunkAt(p.blockPosition()).fillBiomesFromNoise((x,y,z,sampler)->holder,level.getChunkSource().randomState().sampler());
        MireCycle.update(p,4800);
        p.setPos(p.position().add(2,0,0));
        equip(p);
        MireCycle.update(p,6000);
        h.assertTrue(p.getX()>h.absoluteVec(new Vec3(5,5,5)).x()+1,"Equipped sigil allowed mire position rewind");
        h.succeed();
    }
    private static ServerPlayer player(GameTestHelper h){
        var profile=new com.mojang.authlib.GameProfile(UUID.randomUUID(),"anchor-test");
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),profile,ClientInformation.createDefault());
        p.connection=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),profile).connection;
        p.setPos(h.absoluteVec(new Vec3(5,5,5)));return p;
    }
    private static void equip(ServerPlayer p){CuriosApi.getCuriosInventory(p).orElseThrow().setEquippedCurio("charm",0,new ItemStack(ModItems.SPACETIME_ANCHORING_SIGIL.get()));}
    private static TemporalAmber amber(GameTestHelper h,ServerPlayer p){
        return h.getLevel().getEntitiesOfClass(TemporalAmber.class,p.getBoundingBox().inflate(2)).stream().filter(a->p.getUUID().equals(a.owner())).findFirst().orElseThrow();
    }
    @GameTest(template="spell_arena") public static void recipeSlotsAndImmunity(GameTestHelper h){
        var p=player(h);var inv=CuriosApi.getCuriosInventory(p).orElseThrow();
        h.assertTrue(inv.getStacksHandler("charm").orElseThrow().getSlots()==2,"Charm count must be two");
        h.assertTrue(inv.getStacksHandler("ring").orElseThrow().getSlots()==4,"Ring count must be four");
        p.getInventory().setItem(0,new ItemStack(ModItems.SPACETIME_ANCHORING_SIGIL.get()));
        h.assertTrue(p.addEffect(new MobEffectInstance(MobEffects.WITHER,100)),"Carried sigil grants immunity");p.removeAllEffects();equip(p);
        h.assertTrue(!p.addEffect(new MobEffectInstance(MobEffects.WITHER,100)) && !p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,100)),"Worn sigil did not block effects");
        h.assertTrue(p.addEffect(new MobEffectInstance(MobEffects.POISON,100)),"Unrelated effect blocked");
        float health=p.getHealth();p.hurt(p.damageSources().wither(),3);h.assertTrue(p.getHealth()==health,"Direct wither damage bypassed immunity");
        var c=new SimpleContainer(9);c.setItem(5,new ItemStack(ModItems.SPACETIME_KNOT.get()));
        c.setItem(6,new ItemStack(ModItems.TIME_MICROCORE.get()));c.setItem(7,new ItemStack(ModItems.GRID_FRAME.get()));c.setItem(8,new ItemStack(ModItems.SPACE_MICROCORE.get()));
        var input=new AdvancedForgeRecipe.Input(c);
        var recipe=h.getLevel().getRecipeManager().getRecipeFor(ModRecipes.ADVANCED_FORGE_TYPE.get(),input,h.getLevel()).orElseThrow().value();
        h.assertTrue(recipe.getResultItem(h.getLevel().registryAccess()).is(ModItems.SPACETIME_ANCHORING_SIGIL.get()),"Wrong assembly output");
        c.setItem(0,new ItemStack(Items.STICK));h.assertTrue(!recipe.matches(input,h.getLevel()),"Extra material accepted");c.setItem(0,ItemStack.EMPTY);
        c.setItem(6,new ItemStack(ModItems.SPACE_MICROCORE.get()));c.setItem(8,new ItemStack(ModItems.TIME_MICROCORE.get()));
        h.assertTrue(!recipe.matches(input,h.getLevel()),"Node order ignored");h.succeed();
    }
    @GameTest(template="spell_arena") public static void normalDeathKeepsEquipmentAndMarker(GameTestHelper h){
        var p=player(h);equip(p);p.getInventory().setItem(8,new ItemStack(Items.DIAMOND,7));
        p.getInventory().setItem(38,new ItemStack(Items.DIAMOND_CHESTPLATE));
        p.hurt(p.damageSources().genericKill(),1000);
        var a=amber(h,p);var marks=WaymarkDirectory.get(h.getLevel());
        h.assertTrue(a.storedStacks()==3 && p.getInventory().isEmpty(),"Normal death lost or duplicated items");
        h.assertTrue(marks.knows(p.getUUID(),a.getUUID()),"Death marker absent");
        h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class,a.getBoundingBox().inflate(2)).isEmpty(),"Death leaked items");
        p.setHealth(20);a.interact(p,InteractionHand.MAIN_HAND);
        h.assertTrue(p.getInventory().getItem(8).getCount()==7 && AnchoringSigilItem.equipped(p),"Original slots not restored");
        h.assertTrue(a.isRemoved() && marks.get(a.getUUID())==null,"Empty amber retained marker");h.succeed();
    }
    @GameTest(template="spell_arena") public static void cancelledDeathAndTotemDoNotSeal(GameTestHelper h){
        var p=player(h);equip(p);p.getInventory().setItem(7,new ItemStack(Items.DIAMOND));
        java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.LivingDeathEvent> cancel=e->{if(e.getEntity()==p){e.setCanceled(true);p.setHealth(10);}};
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(cancel);
        try{p.hurt(p.damageSources().genericKill(),1000);}finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(cancel);}
        h.assertTrue(p.isAlive() && p.getInventory().getItem(7).is(Items.DIAMOND) && AmberDirectory.get(h.getLevel()).grave(p.getUUID())==null,"Cancelled death captured inventory");
        var q=player(h);equip(q);q.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.TOTEM_OF_UNDYING));
        q.hurt(q.damageSources().generic(),1000);
        h.assertTrue(q.isAlive() && AnchoringSigilItem.equipped(q) && AmberDirectory.get(h.getLevel()).grave(q.getUUID())==null,"Totem triggered amber");h.succeed();
    }
    @GameTest(template="spell_arena") public static void replacementSpillsOnceAndPersists(GameTestHelper h){
        var p=player(h);p.getInventory().setItem(0,new ItemStack(Items.DIAMOND,4));
        var first=TemporalAmber.capture(p);h.getLevel().addFreshEntity(first);first.tick();
        p.setPos(p.position().add(5,0,0));p.getInventory().setItem(0,new ItemStack(Items.EMERALD,3));
        var second=TemporalAmber.capture(p);h.getLevel().addFreshEntity(second);second.tick();
        var dir=AmberDirectory.get(h.getLevel());var marks=WaymarkDirectory.get(h.getLevel());
        h.assertTrue(first.isRemoved() && !second.isRemoved() && marks.get(first.getUUID())==null && marks.knows(p.getUUID(),second.getUUID()),"Replacement failed");
        var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,first.getBoundingBox().inflate(1));
        h.assertTrue(drops.size()==1 && drops.getFirst().getItem().getCount()==4,"Old amber did not spill once");
        var loaded=AmberDirectory.load(dir.save(new CompoundTag(),h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(loaded.owns(p.getUUID(),second.getUUID()),"Unique owner did not persist");
        second.discard();h.assertTrue(marks.get(second.getUUID())==null && dir.grave(p.getUUID())==null,"Erasure retained marker");h.succeed();
    }
    @GameTest(template="spell_arena") public static void stormWithSigilCreatesOnlyOne(GameTestHelper h){
        var p=player(h);equip(p);p.getInventory().setItem(4,new ItemStack(Items.DIAMOND));
        SpacetimeStorm.affect(h.getLevel(),p);
        var all=h.getLevel().getEntitiesOfClass(TemporalAmber.class,p.getBoundingBox().inflate(2)).stream().filter(a->p.getUUID().equals(a.owner())).toList();
        h.assertTrue(all.size()==1 && all.getFirst().storedStacks()==2,"Storm and sigil captured twice");h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=80) public static void unloadedOldAmberCannotReclaimMarker(GameTestHelper h){
        var p=player(h);p.getInventory().setItem(0,new ItemStack(Items.DIAMOND,4));
        var first=TemporalAmber.capture(p);h.getLevel().addFreshEntity(first);first.tick();
        var tag=new CompoundTag();first.saveWithoutId(tag);
        first.remove(net.minecraft.world.entity.Entity.RemovalReason.UNLOADED_TO_CHUNK);
        p.setPos(p.position().add(5,0,0));p.getInventory().setItem(0,new ItemStack(Items.EMERALD));
        var second=TemporalAmber.capture(p);h.getLevel().addFreshEntity(second);second.tick();
        h.runAfterDelay(3,()->{
            var loaded=ModEntityTypes.TEMPORAL_AMBER.get().create(h.getLevel());loaded.load(tag);
            h.getLevel().addFreshEntity(loaded);loaded.tick();
            h.assertTrue(loaded.isRemoved() && AmberDirectory.get(h.getLevel()).owns(p.getUUID(),second.getUUID()),"Reloaded old amber stole ownership");
            var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,loaded.getBoundingBox().inflate(1));
            h.assertTrue(drops.size()==1 && drops.getFirst().getItem().getCount()==4,"Reloaded old amber lost contents");h.succeed();
        });
    }
    @GameTest(template="spell_arena") public static void crossDimensionReplacement(GameTestHelper h){
        var p=player(h);p.getInventory().setItem(0,new ItemStack(Items.DIAMOND));
        var first=TemporalAmber.capture(p);h.getLevel().addFreshEntity(first);first.tick();
        var other=h.getLevel().getServer().getLevel(net.minecraft.world.level.Level.NETHER);
        var point=new BlockPos(128,110,128);other.getChunkAt(point);p.setServerLevel(other);p.setPos(Vec3.atCenterOf(point));
        p.getInventory().setItem(0,new ItemStack(Items.EMERALD));var second=TemporalAmber.capture(p);other.addFreshEntity(second);second.tick();
        h.assertTrue(first.isRemoved() && AmberDirectory.get(other).owns(p.getUUID(),second.getUUID()),"Cross-dimension singleton failed");
        h.assertTrue(WaymarkDirectory.get(other).get(second.getUUID()).dimension().equals(other.dimension().location()),"Death marker retained old dimension");
        second.discard();h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=160) public static void recallRetainsMarkerUntilClaim(GameTestHelper h){
        var p=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"amber-recall")){
            @Override public boolean teleportTo(ServerLevel level,double x,double y,double z,java.util.Set<net.minecraft.world.entity.RelativeMovement> relative,float yaw,float pitch){setServerLevel(level);setPos(x,y,z);return true;}
        };
        var base=h.absolutePos(new BlockPos(8,2,8));
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)h.getLevel().setBlockAndUpdate(base.offset(x,-1,z),Blocks.STONE.defaultBlockState());
        p.setPos(Vec3.atBottomCenterOf(base));p.getInventory().setItem(7,new ItemStack(Items.DIAMOND,2));
        var a=TemporalAmber.capture(p);h.getLevel().addFreshEntity(a);a.tick();
        p.setPos(h.absoluteVec(new Vec3(1.5,2,1.5)));p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.ENHANCED_RECALL_CRYSTAL.get()));
        var marks=WaymarkDirectory.get(h.getLevel());
        h.assertTrue(RecallTravel.start(p,InteractionHand.MAIN_HAND,a.getUUID()),"Death recall refused");
        h.runAfterDelay(61,()->{
            RecallTravel.advance(p);h.assertTrue(p.position().distanceToSqr(base.getCenter())<25,"Death recall failed");
            h.assertTrue(marks.knows(p.getUUID(),a.getUUID()),"Successful recall consumed marker");
            a.interact(p,InteractionHand.MAIN_HAND);
            h.assertTrue(marks.get(a.getUUID())==null && p.getInventory().getItem(7).getCount()==2,"Claim did not remove marker/restore inventory");h.succeed();
        });
    }
}
