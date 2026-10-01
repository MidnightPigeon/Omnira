package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.block.*;
import com.mcmagic.omnira.block.entity.*;
import com.mcmagic.omnira.item.bottle.*;
import com.mcmagic.omnira.registry.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;
import java.util.UUID;

@GameTestHolder("omnira_ecology")
@PrefixGameTestTemplate(false)
public final class EcologyGameTests {
    @GameTest(template="spell_arena")
    public static void crystalGrowthAndWoodTags(GameTestHelper h) {
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(6,6,6));
        for(var direction:Direction.values()) h.assertTrue(!LightCrystalCoreBlock.growAt(level,pos,pos.relative(direction),false),"Core grew against its face");
        h.assertTrue(!LightCrystalCoreBlock.growAt(level,pos,pos.offset(6,0,0),false),"Growth escaped radius");
        h.assertTrue(LightCrystalCoreBlock.growAt(level,pos,pos.offset(2,0,0),false),"Condensate did not grow");
        h.assertTrue(LightCrystalCoreBlock.growAt(level,pos,pos.offset(0,2,0),true),"Source crystal did not grow");
        h.assertTrue(!LightCrystalCoreBlock.growAt(level,pos,pos.offset(2,0,0),true),"Growth overwrote a block");
        for(var block:new net.minecraft.world.level.block.Block[]{DreamContent.SHADOW_LOG.get(),ShadowWoodContent.WOOD.get(),ShadowWoodContent.STRIPPED_LOG.get(),ShadowWoodContent.STRIPPED_WOOD.get()})
            h.assertTrue(block.defaultBlockState().is(BlockTags.LOGS),"Wood missing logs tag");
        h.assertTrue(ShadowWoodContent.STAIRS.get().defaultBlockState().is(BlockTags.WOODEN_STAIRS),"Stair tag missing");
        h.assertTrue(DreamContent.SHADOW_LEAVES.get().defaultBlockState().is(BlockTags.LEAVES),"Leaves tag missing");h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=60)
    public static void portalOpensGradually(GameTestHelper h) {
        h.setBlock(4,4,4,ModBlocks.DREAM_PORTAL.get());
        var portal=(DreamPortalBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(4,4,4)));
        portal.initialize(true);h.assertTrue(portal.scale(0)==0,"Portal appeared at full size");
        h.runAfterDelay(20,()->h.assertTrue(portal.scale(0)>.2 && portal.scale(0)<.8,"No intermediate opening scale"));
        h.runAfterDelay(41,()->{h.assertTrue(portal.scale(0)==1,"Portal failed to finish opening");h.succeed();});
    }
    @GameTest(template="spell_arena")
    public static void survivalBottleConsumedAndLocked(GameTestHelper h) {
        var level=h.getLevel();var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"bottle-creative"));
        player.setGameMode(GameType.SURVIVAL);player.setPos(h.absoluteVec(new Vec3(2,3,2)));
        for(int path=0;path<2;path++) {
            var mob=EntityType.PIG.create(level);mob.setPos(h.absoluteVec(new Vec3(5,4,5)));level.addFreshEntity(mob);
            var bottle=new ItemStack(ModItems.POCKET_MAGIC_BOTTLE.get());player.setItemInHand(InteractionHand.MAIN_HAND,bottle);
            h.assertTrue(PocketBottleItem.capture(player,mob,bottle),"Capture failed");
            var clone=bottle.copy();var id=bottle.get(ModDataComponents.BOTTLE_CAPTURE);
            player.getCooldowns().removeCooldown(ModItems.POCKET_MAGIC_BOTTLE.get());
            if(path==0) player.gameMode.useItem(player,level,bottle,InteractionHand.MAIN_HAND);
            else {
                var ground=h.absolutePos(new BlockPos(2,1,2));level.setBlockAndUpdate(ground,Blocks.STONE.defaultBlockState());
                player.gameMode.useItemOn(player,level,bottle,InteractionHand.MAIN_HAND,new BlockHitResult(ground.getCenter(),Direction.UP,ground,false));
            }
            h.assertTrue(player.getMainHandItem().isEmpty(),"Survival retained a used bottle");
            var storage=BottleStorage.get(level);var data=storage.get(id);
            h.assertTrue(data!=null && data.hasUUID("InFlight"),"Throw did not lock capture");
            h.assertTrue(!storage.claim(id,UUID.randomUUID()),"Capture claimed by a second projectile");
            h.assertTrue(PocketBottleItem.release(level,clone,h.absoluteVec(new Vec3(8,4,8)),data.getUUID("InFlight")),"Claimed release failed");
            h.assertTrue(!PocketBottleItem.release(level,clone,h.absoluteVec(new Vec3(10,4,10))),"Capture released twice");
            clone.getItem().inventoryTick(clone,level,player,1,false);h.assertTrue(clone.isEmpty(),"Spent clone survived inventory cleanup");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=230)
    public static void directDriveWithoutCreate(GameTestHelper h) {
        if(net.neoforged.fml.ModList.get().isLoaded("create")) {h.succeed();return;}
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(5,3,5));
        level.setBlockAndUpdate(pos,ModBlocks.ARCANE_ASSEMBLY_TABLE.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.NORTH));
        level.setBlockAndUpdate(pos.south(),ModBlocks.MANA_ENGINE.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.NORTH));
        var engine=((ManaEngineAccess)level.getBlockEntity(pos.south())).engineState();
        engine.redstoneControl=false;engine.inventory.setItem(0,new ItemStack(ModItems.TEST_SPELL_CORE.get()));
        var table=((AssemblyAccess)level.getBlockEntity(pos)).assembly();
        h.assertTrue(ArcaneAssemblyTableBlockEntity.directEngine(level,pos,table.getBlockState()),"Aligned engine not recognized");
        engine.enabled=false;h.assertTrue(!ArcaneAssemblyTableBlockEntity.directEngine(level,pos,table.getBlockState()),"Disabled engine drove table");engine.enabled=true;
        level.setBlockAndUpdate(pos.south(),level.getBlockState(pos.south()).setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.EAST));
        h.assertTrue(!ArcaneAssemblyTableBlockEntity.directEngine(level,pos,table.getBlockState()),"Wrong shaft direction accepted");
        level.setBlockAndUpdate(pos.south(),level.getBlockState(pos.south()).setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.NORTH));
        table.setItem(3,new ItemStack(ModItems.WOODEN_STAFF_SHAFT.get()));table.setItem(6,new ItemStack(ModItems.IRON_REINFORCEMENT.get()));table.setItem(0,new ItemStack(ModItems.SPIRITUAL_CRYSTAL_TIP.get()));
        h.runAfterDelay(210,()->{h.assertTrue(table.finished() && table.getItem(6).is(ModItems.MODULAR_STAFF.get()),"Direct drive did not complete assembly");h.succeed();});
    }
    @GameTest(template="spell_arena",timeoutTicks=230)
    public static void createPressingUsesProgress(GameTestHelper h) {
        if(!net.neoforged.fml.ModList.get().isLoaded("create")) {h.succeed();return;}
        h.setBlock(4,3,4,ModBlocks.ARCANE_ASSEMBLY_TABLE.get());
        var table=((AssemblyAccess)h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(4,3,4)))).assembly();
        table.setItem(6,new ItemStack(Items.IRON_INGOT));
        h.assertTrue(com.mcmagic.omnira.recipe.AssemblyWork.find(h.getLevel(),table)!=null,"Create pressing not inherited");
        table.setItem(0,new ItemStack(Items.STICK));h.assertTrue(com.mcmagic.omnira.recipe.AssemblyWork.find(h.getLevel(),table)==null,"Press accepted peripheral inputs");table.removeItem(0,1);
        for(int i=0;i<20;i++) h.runAfterDelay(i*10+1,()->{
            if(!table.finished()) {table.setMechanicalPowered(true);h.assertTrue(table.automaticStrike(),"Pressing strike failed");}
        });
        h.runAfterDelay(205,()->{
            var id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(table.getItem(6).getItem());
            h.assertTrue(table.finished() && id.toString().equals("create:iron_sheet"),"Wrong pressing result");h.succeed();
        });
    }
}
