package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.registry.*;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_alchemy")
@PrefixGameTestTemplate(false)
public final class AlchemyCauldronGameTests {
    @GameTest(template="spell_arena") public static void roughEnergyCoreReaction(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(5,2,5));
        level.setBlockAndUpdate(pos,ModBlocks.PRIMITIVE_REACTION_CAULDRON.get().defaultBlockState());
        var v=(com.mcmagic.omnira.alchemy.RoughAlchemyCauldronBlockEntity)level.getBlockEntity(pos);
        var recipe=level.getRecipeManager().getAllRecipesFor(ModRecipes.REACTION_TYPE.get()).stream()
                .filter(r->r.id().equals(ResourceLocation.parse("omnira:reaction/rough_alchemical_energy_core"))).findFirst().orElseThrow().value();
        h.assertTrue(recipe.items().size()==4&&recipe.duration()==2000&&recipe.results().size()==1,"Incorrect core reaction definition");
        var execute=net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;
        v.items.setStackInSlot(0,new ItemStack(ModItems.AIR_MICROCORE.get()));
        v.items.setStackInSlot(1,new ItemStack(ModItems.FIRE_MICROCORE.get()));
        v.items.setStackInSlot(2,new ItemStack(ModItems.WATER_MICROCORE.get()));
        v.items.setStackInSlot(3,new ItemStack(ModItems.EARTH_MICROCORE.get()));
        v.tank.setFluid(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.LAVA,4000));
        h.assertTrue(!v.start(recipe),"Core reaction ignored its catalyst");
        var support=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(ResourceLocation.parse("omnira:spiritual_crystal_block"));
        level.setBlockAndUpdate(pos.below(),support.defaultBlockState());
        v.tank.drain(1,execute);h.assertTrue(!v.start(recipe),"Core reaction accepted less than four buckets");
        v.tank.setFluid(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,4000));
        h.assertTrue(!v.start(recipe),"Core reaction accepted water instead of lava");
        v.tank.setFluid(recipe.fluid().copy());v.items.setStackInSlot(3,new ItemStack(ModItems.AIR_MICROCORE.get()));
        h.assertTrue(!v.start(recipe),"Duplicate microcore replaced the missing earth microcore");
        v.items.setStackInSlot(3,new ItemStack(ModItems.EARTH_MICROCORE.get()));
        h.assertTrue(v.start(recipe)&&java.util.stream.IntStream.range(0,4).allMatch(i->v.items.getStackInSlot(i).isEmpty()),"Unordered ingredients were not consumed once");
        for(int i=0;i<200;i++)com.mcmagic.omnira.alchemy.RoughAlchemyCauldronBlockEntity.tick(level,pos,v.getBlockState(),v);
        h.assertTrue(v.tank.getFluidAmount()==3600,"Core reaction did not consume 400 mB per batch");
        for(int i=0;i<1800;i++)com.mcmagic.omnira.alchemy.RoughAlchemyCauldronBlockEntity.tick(level,pos,v.getBlockState(),v);
        h.assertTrue(!v.working()&&v.tank.isEmpty()&&v.items.getStackInSlot(0).is(ModItems.ROUGH_ALCHEMICAL_ENERGY_CORE.get())
                &&v.items.getStackInSlot(0).getCount()==1,"Core output or total lava consumption incorrect");
        h.assertTrue(level.getBlockState(pos.below()).is(support),"Core reaction consumed the catalyst");h.succeed();
    }
    @GameTest(template="spell_arena") public static void dreamWaterReaction(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(5,2,5));
        level.setBlockAndUpdate(pos,ModBlocks.PRIMITIVE_REACTION_CAULDRON.get().defaultBlockState());
        var v=(com.mcmagic.omnira.alchemy.RoughAlchemyCauldronBlockEntity)level.getBlockEntity(pos);
        var recipe=level.getRecipeManager().getAllRecipesFor(ModRecipes.REACTION_TYPE.get()).stream()
                .filter(r->r.id().equals(ResourceLocation.parse("omnira:reaction/dream_water"))).findFirst().orElseThrow().value();
        h.assertTrue(recipe.results().size()==4&&recipe.results().stream().allMatch(s->s.getCount()==1)&&recipe.duration()==2000,"Incorrect low-tier results or duration");
        var execute=net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;
        v.tank.fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,4000),execute);
        h.assertTrue(!v.start(recipe),"Water reaction started without dream crystal support");
        level.setBlockAndUpdate(pos.below(),DreamContent.DREAM_CRYSTAL_BLOCK.get().defaultBlockState());
        v.tank.drain(1,execute);h.assertTrue(!v.start(recipe),"Water reaction accepted less than 4000 mB");
        v.tank.fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,1),execute);
        h.assertTrue(v.start(recipe),"Water reaction did not start");
        for(int i=0;i<200;i++)com.mcmagic.omnira.alchemy.RoughAlchemyCauldronBlockEntity.tick(level,pos,v.getBlockState(),v);
        h.assertTrue(v.tank.getFluidAmount()==3600,"Water batch did not consume 400 mB");
        for(int i=0;i<1800;i++)com.mcmagic.omnira.alchemy.RoughAlchemyCauldronBlockEntity.tick(level,pos,v.getBlockState(),v);
        h.assertTrue(v.tank.isEmpty()&&!v.working()&&recipe.results().stream().anyMatch(s->ItemStack.isSameItemSameComponents(s,v.items.getStackInSlot(0))),"Water reaction output or consumption incorrect");
        for(var output:recipe.results()){
            for(int i=0;i<4;i++)v.items.setStackInSlot(i,ItemStack.EMPTY);
            v.items.setStackInSlot(0,output.copyWithCount(output.getMaxStackSize()));v.tank.setFluid(recipe.fluid().copy());
            h.assertTrue(!v.start(recipe),"Full random output failed to stop low-tier reaction");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void reactionProgressAndReload(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(5,2,5));
        level.setBlockAndUpdate(pos,ModBlocks.PRIMITIVE_REACTION_CAULDRON.get().defaultBlockState());
        var vessel=(com.mcmagic.omnira.alchemy.RoughAlchemyCauldronBlockEntity)level.getBlockEntity(pos);
        var recipe=level.getRecipeManager().getAllRecipesFor(ModRecipes.REACTION_TYPE.get()).stream()
                .filter(r->r.id().equals(ResourceLocation.parse("omnira:reaction/elemental_microcore"))).findFirst().orElseThrow().value();
        var execute=net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;
        vessel.tank.fill(new net.neoforged.neoforge.fluids.FluidStack(com.mcmagic.omnira.mire.MireContent.SOURCE.get(),2000),execute);
        h.assertTrue(!vessel.start(recipe),"Started without required support");
        level.setBlockAndUpdate(pos.below(),com.mcmagic.omnira.aggregation.AggregationContent.SPATIAL_BLOCK.get().defaultBlockState());
        h.assertTrue(recipe.duration()==2000&&recipe.results().size()==4&&vessel.start(recipe),"Missing equal four-way reaction");
        level.setBlockAndUpdate(pos.below(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        com.mcmagic.omnira.alchemy.RoughAlchemyCauldronBlockEntity.tick(level,pos,vessel.getBlockState(),vessel);
        h.assertTrue(vessel.progress()==0&&vessel.tank.getFluidAmount()==2000,"Missing support did not pause reaction");
        level.setBlockAndUpdate(pos.below(),com.mcmagic.omnira.aggregation.AggregationContent.SPATIAL_BLOCK.get().defaultBlockState());
        h.assertTrue(vessel.tank.drain(1000,execute).getAmount()==1000,"Player reagent extraction failed");
        for(int i=0;i<199;i++)com.mcmagic.omnira.alchemy.RoughAlchemyCauldronBlockEntity.tick(level,pos,vessel.getBlockState(),vessel);
        h.assertTrue(vessel.tank.getFluidAmount()==1000,"Fluid consumed before ten-second boundary");
        com.mcmagic.omnira.alchemy.RoughAlchemyCauldronBlockEntity.tick(level,pos,vessel.getBlockState(),vessel);
        h.assertTrue(vessel.tank.getFluidAmount()==900,"Ten-second batch did not consume 100 mB");
        for(int i=0;i<800;i++)com.mcmagic.omnira.alchemy.RoughAlchemyCauldronBlockEntity.tick(level,pos,vessel.getBlockState(),vessel);
        h.assertTrue(vessel.tank.getFluidAmount()==500&&vessel.progress()==1000,"Fluid consumption is not batched");
        var saved=vessel.saveWithFullMetadata(level.registryAccess());
        var loaded=new com.mcmagic.omnira.alchemy.RoughAlchemyCauldronBlockEntity(pos,vessel.getBlockState());
        loaded.loadWithComponents(saved,level.registryAccess());level.setBlockEntity(loaded);
        h.assertTrue(loaded.working()&&loaded.progress()==1000,"Active reaction did not persist");
        h.assertTrue(saved.getCompound("Output").equals(loaded.saveWithFullMetadata(level.registryAccess()).getCompound("Output")),"Loading rerolled the output");
        for(int i=0;i<1000;i++)com.mcmagic.omnira.alchemy.RoughAlchemyCauldronBlockEntity.tick(level,pos,loaded.getBlockState(),loaded);
        h.assertTrue(!loaded.working()&&loaded.tank.isEmpty(),"Reaction did not finish exactly");
        h.assertTrue(java.util.stream.IntStream.range(0,4).map(i->loaded.items.getStackInSlot(i).getCount()).sum()==1,"Reaction output lost or duplicated");
        for(int i=0;i<4;i++)loaded.items.setStackInSlot(i,ItemStack.EMPTY);
        loaded.items.setStackInSlot(0,new ItemStack(ModItems.EARTH_MICROCORE.get(),64));
        loaded.tank.fill(new net.neoforged.neoforge.fluids.FluidStack(com.mcmagic.omnira.mire.MireContent.SOURCE.get(),1000),execute);
        h.assertTrue(!loaded.start(recipe),"Full possible random outcome failed to stop reaction");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void itemStorageAndOtherReactions(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(5,2,5));
        level.setBlockAndUpdate(pos,ModBlocks.PRIMITIVE_REACTION_CAULDRON.get().defaultBlockState());
        var v=(com.mcmagic.omnira.alchemy.RoughAlchemyCauldronBlockEntity)level.getBlockEntity(pos);
        h.assertTrue(v.insert(new ItemStack(Items.STONE,80)).getCount()==16,"One-stack limit incorrect");
        v.insert(new ItemStack(Items.DIRT,64));v.insert(new ItemStack(Items.SAND,64));v.insert(new ItemStack(Items.GRAVEL,64));
        h.assertTrue(v.insert(new ItemStack(Items.DIAMOND)).getCount()==1,"Accepted a fifth item kind");
        for(int i=0;i<4;i++)v.items.setStackInSlot(i,ItemStack.EMPTY);
        v.insert(new ItemStack(Items.DIAMOND,2));
        var recipe=new com.mcmagic.omnira.alchemy.ReactionRecipe(java.util.List.of(new com.mcmagic.omnira.alchemy.ReactionRecipe.InputItem(Ingredient.of(Items.DIAMOND),2)),
                net.neoforged.neoforge.fluids.FluidStack.EMPTY,Ingredient.EMPTY,java.util.List.of(new ItemStack(Items.EMERALD)),net.neoforged.neoforge.fluids.FluidStack.EMPTY,20);
        h.assertTrue(v.start(recipe)&&v.items.getStackInSlot(0).isEmpty(),"Item-only reaction did not consume inputs at start");
        v.insert(new ItemStack(Items.STONE));v.insert(new ItemStack(Items.DIRT));v.insert(new ItemStack(Items.SAND));
        h.assertTrue(!v.insert(new ItemStack(Items.GRAVEL)).isEmpty(),"Insertion used reserved output space");
        for(int i=0;i<20;i++)com.mcmagic.omnira.alchemy.RoughAlchemyCauldronBlockEntity.tick(level,pos,v.getBlockState(),v);
        h.assertTrue(java.util.stream.IntStream.range(0,4).anyMatch(i->v.items.getStackInSlot(i).is(Items.EMERALD)),"Item-only reaction output missing");
        for(int i=0;i<4;i++)v.items.setStackInSlot(i,ItemStack.EMPTY);
        v.items.setStackInSlot(0,new ItemStack(Items.DIAMOND,2));
        var fluidRecipe=new com.mcmagic.omnira.alchemy.ReactionRecipe(recipe.items(),net.neoforged.neoforge.fluids.FluidStack.EMPTY,Ingredient.EMPTY,java.util.List.of(),
                new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000),20);
        var execute=net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;
        v.tank.fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.LAVA,1000),execute);
        h.assertTrue(!v.start(fluidRecipe),"Fluid output mixed with incompatible tank content");
        v.tank.drain(4000,execute);h.assertTrue(v.start(fluidRecipe),"Empty tank rejected fluid output");
        h.assertTrue(v.tank.fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.LAVA,1000),execute)==0,"In-flight fluid output was not reserved");
        for(int i=0;i<20;i++)com.mcmagic.omnira.alchemy.RoughAlchemyCauldronBlockEntity.tick(level,pos,v.getBlockState(),v);
        h.assertTrue(v.tank.getFluidAmount()==1000,"Fluid output missing");
        var player=new net.neoforged.neoforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"emptyhand"));
        v.insert(new ItemStack(Items.DIAMOND,3));
        var hit=new net.minecraft.world.phys.BlockHitResult(pos.getCenter(),net.minecraft.core.Direction.UP,pos,false);
        v.getBlockState().useWithoutItem(level,player,hit);
        h.assertTrue(v.items.getStackInSlot(0).isEmpty()&&player.getInventory().countItem(Items.DIAMOND)==3,"Empty-hand retrieval failed");
        v.tank.drain(4000,execute);
        var uneven=new com.mcmagic.omnira.alchemy.ReactionRecipe(java.util.List.of(),
                new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,37),Ingredient.EMPTY,
                java.util.List.of(new ItemStack(Items.EMERALD)),net.neoforged.neoforge.fluids.FluidStack.EMPTY,2000);
        v.tank.fill(uneven.fluid(),execute);h.assertTrue(v.start(uneven),"Uneven fluid quantity rejected");
        for(int i=0;i<200;i++)com.mcmagic.omnira.alchemy.RoughAlchemyCauldronBlockEntity.tick(level,pos,v.getBlockState(),v);
        h.assertTrue(v.tank.getFluidAmount()==34,"Batch did not carry fractional remainder");
        for(int i=0;i<1800;i++)com.mcmagic.omnira.alchemy.RoughAlchemyCauldronBlockEntity.tick(level,pos,v.getBlockState(),v);
        h.assertTrue(v.tank.isEmpty()&&!v.working()&&v.items.getStackInSlot(0).is(Items.EMERALD),"Final batch lost fluid or output");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void fluidCapacityPersistenceAndRemoval(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(5,2,5));var level=h.getLevel();
        level.setBlockAndUpdate(pos,ModBlocks.ROUGH_ALCHEMY_CAULDRON.get().defaultBlockState());
        var vessel=(com.mcmagic.omnira.alchemy.RoughAlchemyCauldronBlockEntity)level.getBlockEntity(pos);
        var action=net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;
        var handler=level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,pos,net.minecraft.core.Direction.UP);
        h.assertTrue(handler!=null&&handler.fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,5000),action)==4000,"Wrong tank limit");
        h.assertTrue(handler.fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.LAVA,1000),action)==0,"Mixed fluids accepted");
        var copy=new com.mcmagic.omnira.alchemy.RoughAlchemyCauldronBlockEntity(pos,vessel.getBlockState());
        copy.loadWithComponents(vessel.saveWithFullMetadata(level.registryAccess()),level.registryAccess());
        h.assertTrue(copy.tank.getFluidAmount()==4000,"Fluid did not persist in the world");
        h.assertTrue(handler.drain(1000,action).getAmount()==1000,"Fluid extraction failed");
        level.destroyBlock(pos,true);
        h.assertTrue(level.getBlockState(pos).isAir()&&level.getFluidState(pos).isEmpty(),"Breaking vessel released liquid into world");
        var drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(2));
        h.assertTrue(drops.size()==1&&drops.getFirst().getItem().is(ModItems.ROUGH_ALCHEMY_CAULDRON.get())
                &&!drops.getFirst().getItem().has(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA),"Dropped vessel retained fluid data");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void bucketsAndCampfireRestoration(GameTestHelper h){
        var level=h.getLevel();
        var player=new net.neoforged.neoforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"alchemy"));
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        for(int i=0;i<2;i++){
            var pos=h.absolutePos(new BlockPos(4+i*4,2,5));
            var original=(i==0?net.minecraft.world.level.block.Blocks.CAMPFIRE:net.minecraft.world.level.block.Blocks.SOUL_CAMPFIRE).defaultBlockState()
                    .setValue(net.minecraft.world.level.block.CampfireBlock.FACING,net.minecraft.core.Direction.EAST)
                    .setValue(net.minecraft.world.level.block.CampfireBlock.LIT,false);
            level.setBlockAndUpdate(pos,original);
            ((net.minecraft.world.level.block.entity.CampfireBlockEntity)level.getBlockEntity(pos)).getItems().set(0,new ItemStack(Items.BEEF));
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(ModItems.ROUGH_ALCHEMY_CAULDRON.get()));
            var hit=new net.minecraft.world.phys.BlockHitResult(pos.getCenter(),net.minecraft.core.Direction.UP,pos,false);
            var result=ModItems.ROUGH_ALCHEMY_CAULDRON.get().useOn(new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,hit));
            h.assertTrue(result.consumesAction()&&player.getMainHandItem().isEmpty(),"Special placement did not consume one vessel");
            h.assertTrue(level.getBlockState(pos).is(ModBlocks.PRIMITIVE_REACTION_CAULDRON.get()),"Campfire did not become reaction vessel");
            var vessel=(com.mcmagic.omnira.alchemy.RoughAlchemyCauldronBlockEntity)level.getBlockEntity(pos);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.WATER_BUCKET));
            h.assertTrue(net.neoforged.neoforge.fluids.FluidUtil.interactWithFluidHandler(player,net.minecraft.world.InteractionHand.MAIN_HAND,vessel.tank)
                    &&vessel.tank.getFluidAmount()==1000,"Bucket filling failed");
            h.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(1)).isEmpty(),"Campfire contents spilled during conversion");
            var saved=vessel.saveWithFullMetadata(level.registryAccess());
            var loaded=new com.mcmagic.omnira.alchemy.RoughAlchemyCauldronBlockEntity(pos,vessel.getBlockState());
            loaded.loadWithComponents(saved,level.registryAccess());level.setBlockEntity(loaded);
            level.destroyBlock(pos,true);
            h.assertTrue(level.getBlockState(pos).equals(original),"Original campfire state was not restored");
            h.assertTrue(((net.minecraft.world.level.block.entity.CampfireBlockEntity)level.getBlockEntity(pos)).getItems().get(0).is(Items.BEEF),"Campfire contents were not restored");
            var drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(1));
            h.assertTrue(drops.size()==1&&drops.getFirst().getItem().is(ModItems.ROUGH_ALCHEMY_CAULDRON.get()),"Reaction vessel did not drop an ordinary cauldron exactly once");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void recipeAndHollowVessel(GameTestHelper h){
        var slots=new ArrayList<ItemStack>();
        for(int i=0;i<9;i++)slots.add(new ItemStack(i==4?Items.CAULDRON:ModItems.SPIRITUAL_CRYSTAL.get()));
        var recipe=h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,CraftingInput.of(3,3,slots),h.getLevel()).orElseThrow().value();
        h.assertTrue(recipe.getResultItem(h.getLevel().registryAccess()).is(ModItems.ROUGH_ALCHEMY_CAULDRON.get()),"Wrong cauldron output");
        slots.set(0,ItemStack.EMPTY);
        h.assertTrue(!recipe.matches(CraftingInput.of(3,3,slots),h.getLevel()),"Recipe accepted fewer than eight crystals");
        var pos=h.absolutePos(new BlockPos(4,2,4));
        var state=ModBlocks.ROUGH_ALCHEMY_CAULDRON.get().defaultBlockState();
        h.getLevel().setBlockAndUpdate(pos,state);
        h.assertTrue(h.getLevel().getBlockEntity(pos).getType()==ModBlockEntityTypes.ROUGH_ALCHEMY_CAULDRON.get(),"Missing cauldron block entity");
        var boxes=state.getCollisionShape(h.getLevel(),pos,CollisionContext.empty()).toAabbs();
        h.assertTrue(boxes.stream().noneMatch(b->b.contains(.5,.7,.5)),"Vessel opening is solid");
        h.assertTrue(boxes.stream().anyMatch(b->b.contains(.5,.25,.5)),"Vessel bottom has no collision");
        h.assertTrue(ModCreativeModeTabs.declaredIds(ModCreativeModeTabs.Category.RESEARCH).contains("rough_alchemy_cauldron"),"Cauldron missing from industry");
        h.succeed();
    }
}
