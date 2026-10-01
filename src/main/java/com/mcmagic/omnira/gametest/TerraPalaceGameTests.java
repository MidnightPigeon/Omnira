package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.block.GoldenThroneBlock;
import com.mcmagic.omnira.fate.FateEffects;
import com.mcmagic.omnira.registry.ModBlocks;
import com.mcmagic.omnira.registry.DreamContent;
import com.mcmagic.omnira.throne.GoldenThrone;
import com.mcmagic.omnira.throne.GoldenToilet;
import com.mcmagic.omnira.world.structure.FrozenTerraPalacePlan;
import com.mcmagic.omnira.world.structure.TerraPalaceArmorStands;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("omnira_terra_palace")
@PrefixGameTestTemplate(false)
public final class TerraPalaceGameTests {
    @GameTest(template="spell_arena") public static void shellKeepsSanctuaryOpen(GameTestHelper h){
        var plan=FrozenTerraPalacePlan.create();
        h.assertTrue(plan.get(new BlockPos(24,5,11)).name().equals("omnira:golden_throne"),"Throne absent");
        h.assertTrue(plan.get(new BlockPos(24,6,11)).half().equals("upper"),"Throne upper half absent");
        h.assertTrue(plan.get(new BlockPos(24,5,30))==null,"Sanctuary center is blocked");
        h.assertTrue(plan.get(new BlockPos(24,5,45))==null
                || plan.get(new BlockPos(24,5,45)).name().equals("minecraft:air"),"Processional doorway is blocked");
        h.assertTrue(plan.values().stream().noneMatch(cell->cell.name().equals("minecraft:gold_block")),"Palace still uses gold blocks");
        h.assertTrue(plan.values().stream().anyMatch(cell->cell.name().equals("omnira:solidified_light_crystal_core")),"Solidified core absent");
        h.assertTrue(!DreamContent.SOLIDIFIED_LIGHT_CRYSTAL_CORE.get().defaultBlockState().isRandomlyTicking(),"Solidified core must not grow crystals");
        int balls=0,torches=0,lanterns=0;
        for(var entry:plan.entrySet()){
            switch(entry.getValue().name()){
                case "omnira:crystal_ball" -> {balls++;h.assertTrue(entry.getValue().facing().equals("south"),"Tower ball facing differs");}
                case "omnira:light_crystal_torch" -> torches++;
                case "omnira:shadow_lantern" -> lanterns++;
                default -> {}
            }
        }
        h.assertTrue(balls==4 && torches==28 && lanterns==10,"Player furnishings missing from palace plan");
        for(int x:new int[]{6,42})for(int z:new int[]{6,58})
            h.assertTrue(plan.get(new BlockPos(x,2,z)).name().equals("minecraft:chiseled_quartz_block")
                    && plan.get(new BlockPos(x,3,z)).name().equals("omnira:crystal_ball"),"Tower display asymmetry");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void palaceArmorStandsKeepAllFourTrimmedPieces(GameTestHelper h){
        var stand=TerraPalaceArmorStands.create(h.getLevel(),h.absolutePos(new BlockPos(2,1,2)),-90);
        var slots=new net.minecraft.world.entity.EquipmentSlot[]{
                net.minecraft.world.entity.EquipmentSlot.FEET,net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.CHEST,net.minecraft.world.entity.EquipmentSlot.HEAD};
        var items=new net.minecraft.world.item.Item[]{Items.GOLDEN_BOOTS,Items.GOLDEN_LEGGINGS,
                Items.GOLDEN_CHESTPLATE,Items.GOLDEN_HELMET};
        var patterns=new net.minecraft.resources.ResourceKey[]{
                net.minecraft.world.item.armortrim.TrimPatterns.BOLT,net.minecraft.world.item.armortrim.TrimPatterns.DUNE,
                net.minecraft.world.item.armortrim.TrimPatterns.FLOW,net.minecraft.world.item.armortrim.TrimPatterns.SPIRE};
        for(int i=0;i<slots.length;i++){
            var stack=stand.getItemBySlot(slots[i]);
            var trim=stack.get(net.minecraft.core.component.DataComponents.TRIM);
            h.assertTrue(stack.is(items[i]) && trim!=null && trim.material().unwrapKey().orElseThrow()
                    .equals(net.minecraft.world.item.armortrim.TrimMaterials.REDSTONE)
                    && trim.pattern().unwrapKey().orElseThrow().equals(patterns[i]),"Incomplete palace armor at "+slots[i]);
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void throneLocksAndAddsDamage(GameTestHelper h){
        var level=h.getLevel();var base=h.absolutePos(new BlockPos(2,1,2));
        level.setBlock(base,ModBlocks.GOLDEN_THRONE.get().defaultBlockState(),3);
        level.setBlock(base.above(),ModBlocks.GOLDEN_THRONE.get().defaultBlockState().setValue(GoldenThroneBlock.HALF,DoubleBlockHalf.UPPER),3);
        var profile=new GameProfile(UUID.randomUUID(),"throne-test");
        var attacker=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,profile,
                net.minecraft.server.level.ClientInformation.createDefault());
        attacker.connection=new net.neoforged.neoforge.common.util.FakePlayer(level,profile).connection;
        attacker.setPos(base.getX()+.5,base.getY()+1,base.getZ()+.5);
        var victim=new net.neoforged.neoforge.common.util.FakePlayer(level,new GameProfile(UUID.randomUUID(),"throne-target"));
        victim.setPos(base.getX()+3,base.getY()+1,base.getZ());level.addFreshEntity(victim);
        GoldenThrone.sit(attacker,base);
        h.assertTrue(GoldenThrone.empowered(attacker),"Throne did not seat player; lower="+level.getBlockState(base)
                +", vehicle="+attacker.getVehicle()+", lock="+attacker.getPersistentData().getCompound("OmniraGoldenThrone"));
        var dismount=new EntityMountEvent(attacker,attacker.getVehicle(),level,false);
        GoldenThrone.mount(dismount);h.assertTrue(dismount.isCanceled(),"Ten-minute lock did not prevent dismount");
        var otherSeat=com.mcmagic.omnira.registry.ModEntityTypes.GOLDEN_THRONE_SEAT.get().create(level);
        var switchMount=new EntityMountEvent(attacker,otherSeat,level,true);
        GoldenThrone.mount(switchMount);h.assertTrue(switchMount.isCanceled(),"Ten-minute lock did not prevent switching vehicles");
        attacker.setHealth(0);
        var deathDismount=new EntityMountEvent(attacker,attacker.getVehicle(),level,false);
        GoldenThrone.mount(deathDismount);h.assertTrue(!deathDismount.isCanceled(),"Death must release the throne");
        attacker.setHealth(20);
        var damage=new LivingDamageEvent.Pre(victim,new net.neoforged.neoforge.common.damagesource.DamageContainer(
                victim.damageSources().playerAttack(attacker),10));
        FateEffects.damage(damage);h.assertTrue(Math.abs(damage.getNewDamage()-20)<.001,"Outgoing damage bonus should be 100%");
        attacker.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE,21,4,true,false));
        attacker.setPos(base.getX()+10,base.getY()+1,base.getZ());
        GoldenThrone.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(attacker));
        h.assertTrue(!attacker.isPassenger() && !attacker.getPersistentData().contains("OmniraGoldenThrone"),
                "Teleport did not release the player");
        h.assertTrue(attacker.getEffect(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE)==null,
                "Throne resistance lingered after teleport");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void miningThroneLeavesLootAndOneToilet(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(2,1,2));
        var lower=ModBlocks.GOLDEN_THRONE.get().defaultBlockState();
        level.setBlock(pos,lower,3);
        level.setBlock(pos.above(),lower.setValue(GoldenThroneBlock.HALF,DoubleBlockHalf.UPPER),3);
        var miner=new net.neoforged.neoforge.common.util.FakePlayer(level,new GameProfile(UUID.randomUUID(),"throne-miner"));
        miner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_PICKAXE));
        lower.getBlock().playerWillDestroy(level,pos,lower,miner);
        level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        h.runAfterDelay(2,()->{
            h.assertTrue(level.getBlockState(pos).is(ModBlocks.CRYSTAL_BALL.get()),"Mined throne did not leave a loot ball");
            long toilets=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(pos).inflate(2),e->e.getItem().is(com.mcmagic.omnira.registry.ModItems.GOLDEN_TOILET.get())).size();
            h.assertTrue(toilets==1,"Throne did not drop exactly one toilet");
        });
        h.runAfterDelay(40,()->{
            h.assertTrue(level.getBlockEntity(pos) instanceof com.mcmagic.omnira.block.entity.CrystalBallBlockEntity ball
                    && ResourceKey.create(Registries.LOOT_TABLE,ResourceLocation.fromNamespaceAndPath("omnira","chests/golden_throne"))
                    .equals(ball.getLootTable()),"Throne loot table was not installed");
            var ball=(com.mcmagic.omnira.block.entity.CrystalBallBlockEntity)level.getBlockEntity(pos);
            h.assertTrue(ball.hasPendingLoot() && ball.getUpdateTag(level.registryAccess()).getBoolean("PendingLoot"),
                    "Collapsed throne did not create a fogged loot ball");
            for(int slot=0;slot<ball.getContainerSize();slot++)
                h.assertTrue(ball.getItem(slot).isEmpty() && ball.displayItem(slot).isEmpty(),"Loot was revealed before opening");
            h.assertTrue(ball.getLootTable()!=null,"Reading unopened loot generated its contents");
            h.succeed();
        });
    }
    @GameTest(template="spell_arena") public static void expiryCollapsesThrone(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(2,1,2));
        var lower=ModBlocks.GOLDEN_THRONE.get().defaultBlockState();
        level.setBlock(pos,lower,3);
        level.setBlock(pos.above(),lower.setValue(GoldenThroneBlock.HALF,DoubleBlockHalf.UPPER),3);
        var profile=new GameProfile(UUID.randomUUID(),"throne-expiry");
        var player=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,profile,
                net.minecraft.server.level.ClientInformation.createDefault());
        player.connection=new net.neoforged.neoforge.common.util.FakePlayer(level,profile).connection;
        player.setPos(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5);
        GoldenThrone.sit(player,pos);
        player.getPersistentData().getCompound("OmniraGoldenThrone").putLong("until",level.getGameTime());
        GoldenThrone.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
        h.assertTrue(!player.isPassenger(),"Expired throne kept its rider");
        h.runAfterDelay(2,()->{
            h.assertTrue(level.getBlockState(pos).is(ModBlocks.CRYSTAL_BALL.get()),"Expired throne did not collapse");
            h.succeed();
        });
    }
    @GameTest(template="spell_arena") public static void miningUpperHalfCollapsesOnce(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(2,1,2));
        var lower=ModBlocks.GOLDEN_THRONE.get().defaultBlockState();
        var upper=lower.setValue(GoldenThroneBlock.HALF,DoubleBlockHalf.UPPER);
        level.setBlock(pos,lower,3);level.setBlock(pos.above(),upper,3);
        var miner=new net.neoforged.neoforge.common.util.FakePlayer(level,new GameProfile(UUID.randomUUID(),"throne-upper"));
        miner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_PICKAXE));
        upper.getBlock().playerWillDestroy(level,pos.above(),upper,miner);
        level.setBlockAndUpdate(pos.above(),Blocks.AIR.defaultBlockState());
        h.runAfterDelay(2,()->{
            h.assertTrue(level.getBlockState(pos).is(ModBlocks.CRYSTAL_BALL.get())
                    && level.isEmptyBlock(pos.above()),"Upper-half break left a broken throne");
            long toilets=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(pos).inflate(2),e->e.getItem().is(com.mcmagic.omnira.registry.ModItems.GOLDEN_TOILET.get())).size();
            h.assertTrue(toilets==1,"Upper-half break duplicated or lost the toilet");
            h.succeed();
        });
    }
    @GameTest(template="spell_arena") public static void toiletBuffsOnlyWhileSeated(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(2,1,2));
        level.setBlock(pos,ModBlocks.GOLDEN_TOILET.get().defaultBlockState(),3);
        level.setBlock(pos.above(),ModBlocks.GOLDEN_TOILET.get().defaultBlockState()
                .setValue(com.mcmagic.omnira.block.GoldenToiletBlock.HALF,DoubleBlockHalf.UPPER),3);
        var profile=new GameProfile(UUID.randomUUID(),"toilet-test");
        var player=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,profile,
                net.minecraft.server.level.ClientInformation.createDefault());
        player.connection=new net.neoforged.neoforge.common.util.FakePlayer(level,profile).connection;
        player.setPos(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5);
        GoldenToilet.sit(player,pos);
        h.assertTrue(GoldenToilet.empowered(player),"Toilet failed to seat player");
        GoldenToilet.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
        h.assertTrue(player.getEffect(net.minecraft.world.effect.MobEffects.REGENERATION)!=null
                && player.getEffect(net.minecraft.world.effect.MobEffects.REGENERATION).getAmplifier()==1,"Toilet regeneration II missing");
        GoldenToilet.release(player);
        h.assertTrue(!player.isPassenger() && !GoldenToilet.empowered(player)
                && player.getEffect(net.minecraft.world.effect.MobEffects.REGENERATION)==null,"Toilet buffs persisted after dismount");
        h.succeed();
    }
}
