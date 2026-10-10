package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.alchemy.*;
import com.mcmagic.omnira.block.entity.*;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_alchemy")
@PrefixGameTestTemplate(false)
public final class ReactionPoolAssemblyGameTests {
    private static void materials(GameTestHelper h,BlockPos center,Direction facing){
        var casing=ModBlocks.CRYSTAL_CASING.get();var infused=ModBlocks.INFUSED_CRYSTAL_CASING.get();
        var engine=ModBlocks.MANA_ENGINE.get();
        Block[] blocks={casing,casing,casing,engine,ModBlocks.ROUGH_ALCHEMICAL_ENERGY_CORE.get(),engine,
                ModBlocks.CRYSTAL_BALL.get(),casing,ModBlocks.LIQUID_CRYSTAL_BALL.get(),
                casing,DreamContent.DREAM_CRYSTAL_BLOCK.get(),casing,infused,ModBlocks.PRIMITIVE_REACTION_CAULDRON.get(),infused,casing,infused,casing};
        for(int part=0;part<18;part++)h.getLevel().setBlockAndUpdate(center.offset(ReactionPoolBlock.offset(facing,part)),blocks[part].defaultBlockState());
    }
    @GameTest(template="spell_arena") public static void fourDirectionsAndCreativeRemoval(GameTestHelper h){
        var level=h.getLevel();var center=h.absolutePos(new BlockPos(5,3,5));
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        for(var facing:Direction.Plane.HORIZONTAL){
            materials(h,center,facing);var crystal=new ItemStack(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get(),2);
            var front=center.offset(ReactionPoolBlock.offset(facing,10));
            h.assertTrue(!ReactionPoolStructure.activate(level,center,player,crystal),"Old core activation still works");
            player.setItemInHand(InteractionHand.MAIN_HAND,crystal);
            var event=new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock(player,InteractionHand.MAIN_HAND,front,new BlockHitResult(front.getCenter(),Direction.UP,front,false));
            ReactionPoolStructure.use(event);
            h.assertTrue(event.isCanceled()&&event.getCancellationResult().consumesAction(),"Dream block interaction did not assemble the pool");
            h.assertTrue(crystal.getCount()==1&&ReactionPoolBlock.complete(level,center,facing),"Assembly was partial, consumed extra crystals or ignored orientation");
            var pool=ReactionPoolBlock.controller(level,center);
            h.assertTrue(pool!=null&&((ReactionPoolBlock)pool.getBlockState().getBlock()).bottleParts(level,center,pool.getBlockState()).size()==18,"Assembled pool is not bottle-compatible");
            h.assertTrue(level.getBlockState(front).getDestroySpeed(level,front)<0,"Survival can mine the pool");
            h.assertTrue(ReactionPoolBlock.itemPort(level.getBlockEntity(center.offset(ReactionPoolBlock.offset(facing,6))),facing)==pool.itemOutput
                    &&ReactionPoolBlock.fluidPort(level.getBlockEntity(center.offset(ReactionPoolBlock.offset(facing,8))),facing)==pool.fluidOutput,"Orbs do not correspond to finished output ports");
            var creative=h.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);
            level.getBlockState(front).getBlock().playerWillDestroy(level,front,level.getBlockState(front),creative);
            for(int i=0;i<18;i++)h.assertTrue(level.getBlockState(center.offset(ReactionPoolBlock.offset(facing,i))).isAir(),"Creative destruction left parts behind");
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void rejectsIncompleteAndOccupiedMaterials(GameTestHelper h){
        var level=h.getLevel();var center=h.absolutePos(new BlockPos(5,3,5));var facing=Direction.NORTH;
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var crystal=new ItemStack(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get(),2);
        var front=center.offset(ReactionPoolBlock.offset(facing,10));
        materials(h,center,facing);var hole=center.above();level.setBlockAndUpdate(hole,Blocks.STONE.defaultBlockState());
        h.assertTrue(!ReactionPoolStructure.activate(level,front,player,crystal)&&crystal.getCount()==2&&level.getBlockState(hole).is(Blocks.STONE),"Wrong cauldron substitute was overwritten");
        materials(h,center,facing);var ballPos=center.offset(ReactionPoolBlock.offset(facing,6));
        var ball=(CrystalBallBlockEntity)level.getBlockEntity(ballPos);ball.setItem(0,new ItemStack(Items.DIAMOND));
        h.assertTrue(!ReactionPoolStructure.activate(level,front,player,crystal)&&ball.getItem(0).is(Items.DIAMOND),"Assembly lost orb contents");ball.clearContent();
        ball.upgrades.setStackInSlot(0,new ItemStack(ModItems.BASIC_STACKING_UPGRADE.get()));
        h.assertTrue(!ReactionPoolStructure.activate(level,front,player,crystal),"Assembly lost orb upgrade");ball.upgrades.setStackInSlot(0,ItemStack.EMPTY);
        var liquid=(LiquidCrystalBallBlockEntity)level.getBlockEntity(center.offset(ReactionPoolBlock.offset(facing,8)));
        liquid.tank.setFluid(new FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000));
        h.assertTrue(!ReactionPoolStructure.activate(level,front,player,crystal)&&liquid.tank.getFluidAmount()==1000,"Assembly destroyed stored liquid");liquid.tank.setFluid(FluidStack.EMPTY);
        var engine=(ManaEngineAccess)level.getBlockEntity(center.offset(ReactionPoolBlock.offset(facing,3)));
        engine.engineState().inventory.setItem(0,new ItemStack(Items.DIAMOND));
        h.assertTrue(!ReactionPoolStructure.activate(level,front,player,crystal)&&crystal.getCount()==2,"Assembly accepted occupied engine or consumed failed activation");
        h.assertTrue(level.getBlockState(center).is(ModBlocks.ROUGH_ALCHEMICAL_ENERGY_CORE.get()),"Failed assembly changed the core");h.succeed();
    }
    @GameTest(template="spell_arena") public static void assembledPoolBottleRoundTrip(GameTestHelper h){
        var level=h.getLevel();var center=h.absolutePos(new BlockPos(5,3,5));var facing=Direction.EAST;
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);materials(h,center,facing);
        h.assertTrue(ReactionPoolStructure.activate(level,center.offset(ReactionPoolBlock.offset(facing,10)),player,new ItemStack(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get())),"Bottle test assembly failed");
        var pool=ReactionPoolBlock.controller(level,center);pool.items.setStackInSlot(0,new ItemStack(Items.DIAMOND,3));
        pool.manualFluid.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000),net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        var bottle=new ItemStack(ModItems.POCKET_MAGIC_BOTTLE.get());
        h.assertTrue(com.mcmagic.omnira.item.bottle.MultiblockBottleCapture.capture(player,center.offset(ReactionPoolBlock.offset(facing,8)),bottle),"Pool did not fit in bottle");
        for(int i=0;i<18;i++)h.assertTrue(level.getBlockState(center.offset(ReactionPoolBlock.offset(facing,i))).isAir(),"Capture left pool parts");
        var captured=com.mcmagic.omnira.item.bottle.BottleStorage.get(level).get(bottle.get(ModDataComponents.BOTTLE_CAPTURE));
        h.assertTrue(com.mcmagic.omnira.item.bottle.MultiblockBottleCapture.release(level,captured,Vec3.atLowerCornerOf(center),player),"Bottled pool did not release");
        var restored=ReactionPoolBlock.controller(level,center);
        h.assertTrue(ReactionPoolBlock.complete(level,center,facing)&&restored.items.getStackInSlot(0).getCount()==3&&restored.fluids.total()==1000,"Bottle lost orientation or stored contents");h.succeed();
    }
    @GameTest(template="spell_arena") public static void cauldronPlacementAndContentsProtection(GameTestHelper h){
        var level=h.getLevel();var center=h.absolutePos(new BlockPos(5,3,5));var facing=Direction.NORTH;
        materials(h,center,facing);var top=center.above();var front=center.offset(ReactionPoolBlock.offset(facing,10));
        var player=new net.neoforged.neoforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"pool_builder"));
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var crystal=new ItemStack(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get(),2);
        level.setBlockAndUpdate(top,Blocks.AIR.defaultBlockState());
        // Removing a primitive cauldron restores its campfire, neither is a valid substitute.
        h.assertTrue(!ReactionPoolStructure.activate(level,front,player,crystal),"Old empty opening accepted");
        level.setBlockAndUpdate(top,Blocks.SOUL_CAMPFIRE.defaultBlockState());
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.ROUGH_ALCHEMY_CAULDRON.get()));
        var result=ModItems.ROUGH_ALCHEMY_CAULDRON.get().useOn(new net.minecraft.world.item.context.UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(top.getCenter(),Direction.UP,top,false)));
        h.assertTrue(result.consumesAction()&&player.getMainHandItem().isEmpty(),"Campfire conversion failed");
        var v=(RoughAlchemyCauldronBlockEntity)level.getBlockEntity(top);
        v.items.setStackInSlot(0,new ItemStack(Items.DIAMOND));
        h.assertTrue(!ReactionPoolStructure.activate(level,front,player,crystal)&&v.items.getStackInSlot(0).is(Items.DIAMOND),"Occupied cauldron was consumed");
        v.items.setStackInSlot(0,ItemStack.EMPTY);v.tank.setFluid(new FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000));
        h.assertTrue(!ReactionPoolStructure.activate(level,front,player,crystal)&&v.tank.getFluidAmount()==1000,"Cauldron fluid was lost");v.tank.setFluid(FluidStack.EMPTY);
        var recipe=new ReactionRecipe(java.util.List.of(new ReactionRecipe.InputItem(net.minecraft.world.item.crafting.Ingredient.of(Items.STICK),1)),FluidStack.EMPTY,
                net.minecraft.world.item.crafting.Ingredient.EMPTY,java.util.List.of(new ItemStack(Items.EMERALD)),FluidStack.EMPTY,2000);
        v.items.setStackInSlot(0,new ItemStack(Items.STICK));h.assertTrue(v.start(recipe),"Test reaction failed");
        h.assertTrue(!ReactionPoolStructure.activate(level,front,player,crystal)&&crystal.getCount()==2,"In-flight reaction was lost");
        for(int i=0;i<2000;i++)RoughAlchemyCauldronBlockEntity.tick(level,top,v.getBlockState(),v);
        v.items.extractItem(0,64,false);
        h.assertTrue(ReactionPoolStructure.activate(level,front,player,crystal)&&crystal.getCount()==1&&ReactionPoolBlock.complete(level,center,facing),"Empty finished cauldron did not assemble");
        h.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(center).inflate(3)).isEmpty(),"Assembly dropped consumed ingredients");h.succeed();
    }
}
