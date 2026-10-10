package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.alchemy.*;
import com.mcmagic.omnira.block.entity.LiquidCrystalBallBlockEntity;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.*;
import java.util.List;
import static net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.*;

@GameTestHolder("omnira_alchemy")
@PrefixGameTestTemplate(false)
public final class AlchemyMaterialsGameTests {
    private static ReactionRecipe recipe(GameTestHelper h,String name){return h.getLevel().getRecipeManager().getAllRecipesFor(ModRecipes.REACTION_TYPE.get()).stream()
            .filter(r->r.id().equals(ResourceLocation.parse("omnira:reaction/"+name))).findFirst().orElseThrow().value();}
    private static ReactionPoolBlockEntity pool(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(5,3,5));
        h.assertTrue(ReactionPoolBlock.place(h.getLevel(),pos,Direction.NORTH),"Pool placement failed");
        return ReactionPoolBlock.controller(h.getLevel(),pos);
    }
    private static void fill(ReactionPoolBlockEntity p,ReactionRecipe r){
        p.items.setStackInSlot(0,new ItemStack(ModItems.ROUGH_ALCHEMICAL_ENERGY_CORE.get()));
        for(var fluid:r.fluidInputs())p.manualFluid.fill(fluid,EXECUTE);
    }
    private static int waste(ReactionPoolBlockEntity p){return java.util.stream.IntStream.range(0,9)
            .map(i->p.items.getStackInSlot(i).is(AlchemyMaterials.WASTE.get())?p.items.getStackInSlot(i).getCount():0).sum();}

    @GameTest(template="spell_arena") public static void dualFluidBatchesAndReload(GameTestHelper h){
        var r=recipe(h,"dilute_spacetime");var p=pool(h);fill(p,r);
        h.assertTrue(r.totalFluidInput()==20000&&r.fluidInputs().size()==2&&r.support().isEmpty()&&r.duration()==2000,"Mixed recipe contract changed");
        h.assertTrue(p.start(r)&&p.items.getStackInSlot(0).isEmpty(),"Mixed job did not consume core once");
        p.advance(199);h.assertTrue(p.fluids.total()==20000,"Early liquid consumption");
        p.advance(1);
        for(var input:r.fluidInputs())h.assertTrue(p.fluids.amount(input)==9000,"First batch did not debit both reagents equally");
        h.assertTrue(p.fluids.amount(r.fluidResult())==100&&waste(p)==0,"Mixed output timing incorrect");
        var copy=new ReactionPoolBlockEntity(p.getBlockPos(),p.getBlockState());copy.setLevel(h.getLevel());
        copy.loadWithComponents(p.getUpdateTag(h.getLevel().registryAccess()),h.getLevel().registryAccess());
        copy.advance(1800);
        h.assertTrue(!copy.reacting()&&copy.fluids.total()==1000&&copy.fluids.amount(r.fluidResult())==1000&&waste(copy)==1,"Reload lost reagents, progress or mixed output");
        copy.advance(2000);h.assertTrue(waste(copy)==1&&copy.fluids.total()==1000,"Completed recipe duplicated outputs");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void missingSecondFluidIsAtomicAndFailureIsDeferred(GameTestHelper h){
        var r=recipe(h,"dilute_spacetime");var p=pool(h);fill(p,r);h.assertTrue(p.start(r),"Start failed");
        p.advance(1000);var missing=r.fluidInputs().get(1);p.manualFluid.drain(missing,EXECUTE);
        p.advance(199);h.assertTrue(p.reacting(),"Shortage failed before its debit node");
        int untouched=p.fluids.amount(r.fluid());p.advance(1);
        h.assertTrue(!p.reacting()&&p.fluids.amount(r.fluid())==untouched&&waste(p)==0,"Partial installment debited the surviving reagent");
        h.assertTrue(p.fluids.amount(r.fluidResult())==500,"Failure fabricated or removed already produced liquid");h.succeed();
    }
    @GameTest(template="spell_arena") public static void mixedFailureExplosionAndOtherJobsSurvive(GameTestHelper h){
        var r=recipe(h,"dilute_spacetime");var p=pool(h);fill(p,r);p.start(r);p.advance(1200);
        var cow=new net.minecraft.world.entity.animal.Cow(EntityType.COW,h.getLevel());
        cow.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);cow.setHealth(100);
        cow.setPos(Vec3.atCenterOf(p.getBlockPos().offset(3,0,0)));h.getLevel().addFreshEntity(cow);
        p.manualFluid.drain(r.fluidInputs().get(1),EXECUTE);
        p.items.setStackInSlot(1,new ItemStack(Items.STICK));
        var other=new ReactionRecipe(List.of(new ReactionRecipe.InputItem(Ingredient.of(Items.STICK),1)),FluidStack.EMPTY,Ingredient.EMPTY,
                List.of(new ItemStack(Items.EMERALD)),FluidStack.EMPTY,2000);
        h.assertTrue(p.start(other),"Parallel item reaction failed");
        p.advance(200);
        h.assertTrue(p.reactionCount()==1&&cow.getHealth()==70&&p.fluids.amount(r.fluid())==4000&&waste(p)==0,"Mixed shortage explosion, atomicity or parallel isolation failed");
        p.advance(1800);h.assertTrue(!p.reacting()&&cow.getHealth()==70,"Failure repeated or stopped independent job");h.succeed();
    }
    @GameTest(template="spell_arena") public static void overlappingFluidAdmissionAndOutputSpace(GameTestHelper h){
        var r=recipe(h,"dilute_spacetime");var p=pool(h);fill(p,r);
        p.manualFluid.drain(r.fluidInputs().get(1).copyWithAmount(1),EXECUTE);
        h.assertTrue(!p.start(r)&&!p.items.getStackInSlot(0).isEmpty(),"Missing second reagent consumed core");
        p.manualFluid.fill(r.fluidInputs().get(1).copyWithAmount(1),EXECUTE);p.items.setStackInSlot(1,new ItemStack(AlchemyMaterials.WASTE.get(),64));
        h.assertTrue(!p.start(r),"Full waste output allowed reaction");p.items.setStackInSlot(1,ItemStack.EMPTY);
        h.assertTrue(p.start(r),"Valid recipe rejected");
        var competing=new ReactionRecipe(List.of(),r.fluidInputs().get(1).copyWithAmount(1000),Ingredient.of(Blocks.STONE),List.of(new ItemStack(Items.DIAMOND)),FluidStack.EMPTY,2000);
        h.getLevel().setBlockAndUpdate(p.getBlockPos().below(),Blocks.STONE.defaultBlockState());
        h.assertTrue(p.start(competing),"10 B mixed and 1 B catalytic reactions could not coexist with 10 B stored");
        p.manualFluid.fill(r.fluidInputs().get(1).copyWithAmount(1000),EXECUTE);
        p.manualFluid.fill(new FluidStack(Fluids.WATER,1000),EXECUTE);
        h.assertTrue(p.manualFluid.fill(new FluidStack(Fluids.LAVA,1000),EXECUTE)==0,"Fourth input kind stole pending product kind");
        h.assertTrue(!p.items.insertItem(0,new ItemStack(AlchemyMaterials.WASTE.get(),64),false).isEmpty(),"Inserted items stole reserved waste output");
        p.advance(2000);h.assertTrue(waste(p)==1&&p.fluids.amount(r.fluidResult())==1000,"Reserved mixed outputs failed");h.succeed();
    }
    @GameTest(template="spell_arena") public static void bothLivingSiltCatalystsAndPrimitiveLimits(GameTestHelper h){
        var r=recipe(h,"light_dark_microcore");
        h.assertTrue(r.results().size()==2&&r.results().get(0).is(ModItems.LIGHT_MICROCORE.get())&&r.results().get(1).is(ModItems.DARK_MICROCORE.get())
                &&r.results().stream().allMatch(s->s.getCount()==1)&&r.fluid().getAmount()==4000,"Equal light/dark pool incorrect");
        var pos=h.absolutePos(new BlockPos(5,3,5));
        h.getLevel().setBlockAndUpdate(pos,ModBlocks.PRIMITIVE_REACTION_CAULDRON.get().defaultBlockState());
        var v=(RoughAlchemyCauldronBlockEntity)h.getLevel().getBlockEntity(pos);
        v.tank.setFluid(r.fluid().copy());h.assertTrue(!v.start(r),"Missing catalyst accepted");
        for(var block:List.of(DreamContent.LIVING_TEMPORAL_SILT.get(),TimeNatureContent.LIVING_SILT.get())){
            h.getLevel().setBlockAndUpdate(pos.below(),block.defaultBlockState());
            for(int i=0;i<4;i++)v.items.setStackInSlot(i,ItemStack.EMPTY);v.tank.setFluid(r.fluid().copy());
            h.assertTrue(v.start(r),"Living silt variant not accepted");
            for(int i=0;i<200;i++)RoughAlchemyCauldronBlockEntity.tick(h.getLevel(),pos,v.getBlockState(),v);
            h.assertTrue(v.tank.getFluidAmount()==3600,"Wrong 400 mB installment");
            for(int i=0;i<1800;i++)RoughAlchemyCauldronBlockEntity.tick(h.getLevel(),pos,v.getBlockState(),v);
            h.assertTrue(!v.working()&&v.tank.isEmpty()&&(v.items.getStackInSlot(0).is(ModItems.LIGHT_MICROCORE.get())||v.items.getStackInSlot(0).is(ModItems.DARK_MICROCORE.get())),"Primitive result invalid");
        }
        v.items.setStackInSlot(0,new ItemStack(ModItems.ROUGH_ALCHEMICAL_ENERGY_CORE.get()));
        h.assertTrue(!v.start(recipe(h,"dilute_spacetime"))&&!v.items.getStackInSlot(0).isEmpty(),"Primitive vessel accepted dual-fluid reaction");h.succeed();
    }
    @GameTest(template="spell_arena") public static void legacyCodecAndNewRecipeRoundTrip(GameTestHelper h){
        var ops=h.getLevel().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
        var old=recipe(h,"dream_water");var tag=(net.minecraft.nbt.CompoundTag)ReactionRecipe.CODEC.codec().encodeStart(ops,old).getOrThrow();
        tag.remove("additional_fluids");
        h.assertTrue(ReactionRecipe.CODEC.codec().parse(ops,tag).getOrThrow().additionalFluids().isEmpty(),"Old save rejected");
        var mixed=recipe(h,"dilute_spacetime");var encoded=ReactionRecipe.CODEC.codec().encodeStart(ops,mixed).getOrThrow();
        var decoded=ReactionRecipe.CODEC.codec().parse(ops,encoded).getOrThrow();
        h.assertTrue(decoded.fluidInputs().size()==2&&!decoded.results().isEmpty()&&!decoded.fluidResult().isEmpty(),"Codec lost mixed reaction");
        boolean rejected=false;
        try{new ReactionRecipe(List.of(),mixed.fluid(),Ingredient.EMPTY,mixed.results(),mixed.fluidResult(),2000,List.of(mixed.fluid()));}
        catch(IllegalArgumentException expected){rejected=true;}
        h.assertTrue(rejected,"Duplicate input can overdraw liquid");h.succeed();
    }
    @GameTest(template="spell_arena") public static void infiniteOnlyForDiluteAndExternalExtraction(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(4,3,4));
        h.getLevel().setBlockAndUpdate(pos,ModBlocks.LIQUID_CRYSTAL_BALL.get().defaultBlockState());
        var orb=(LiquidCrystalBallBlockEntity)h.getLevel().getBlockEntity(pos);
        // External throughput accrues with ticks; a newly placed orb starts with no transfer credit.
        h.runAfterDelay(10,()->{
        for(var liquid:List.of(AlchemyMaterials.DILUTE,AlchemyMaterials.REFINED)){
            boolean dilute=liquid==AlchemyMaterials.DILUTE;
            h.assertTrue(liquid.source.get().defaultFluidState().is(net.minecraft.tags.FluidTags.WATER)==dilute,"Wrong water tag");
            h.assertTrue(liquid.source.get().canConvertToSource(liquid.source.get().defaultFluidState(),h.getLevel(),pos)==dilute,"Wrong source conversion policy");
            orb.tank.setFluid(new FluidStack(liquid.source.get(),1000));
            h.assertTrue(orb.infinite()==dilute,"Wrong orb infinite status");
            int amount=orb.fluidHandler.drain(100,EXECUTE).getAmount();
            h.assertTrue(amount>0&&orb.tank.getFluidAmount()==(dilute?1000:1000-amount),"External drain policy incorrect");
            int before=orb.tank.getFluidAmount();var bucket=net.neoforged.neoforge.fluids.FluidUtil.tryFillContainer(new ItemStack(Items.BUCKET),orb.tank,1000,null,true);
            h.assertTrue(dilute?bucket.isSuccess()&&orb.tank.isEmpty():!bucket.isSuccess()&&orb.tank.getFluidAmount()==before,"Manual container extraction treated fluid as infinite");
            h.assertTrue(liquid.type.get().getLightLevel()==5,"Fluid luminosity differs from starflow");
        }h.succeed();
        });
    }
    @GameTest(template="spell_arena",timeoutTicks=100) public static void sourceMultiplicationRespectsFluidKind(GameTestHelper h){
        for(int i=0;i<2;i++){
            var liquid=i==0?AlchemyMaterials.DILUTE:AlchemyMaterials.REFINED;
            var center=h.absolutePos(new BlockPos(4+i*6,3,5));
            for(int x=-2;x<=2;x++)for(int z=-1;z<=1;z++){
                h.getLevel().setBlockAndUpdate(center.offset(x,-1,z),Blocks.STONE.defaultBlockState());
                h.getLevel().setBlockAndUpdate(center.offset(x,0,z),Math.abs(x)==2||Math.abs(z)==1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState());
            }
            h.getLevel().setBlockAndUpdate(center.west(),liquid.block.get().defaultBlockState());
            h.getLevel().setBlockAndUpdate(center.east(),liquid.block.get().defaultBlockState());
        }
        h.runAfterDelay(30,()->{
            h.assertTrue(h.getLevel().getFluidState(h.absolutePos(new BlockPos(4,3,5))).isSource(),"Dilute two-source pool failed");
            var refined=h.getLevel().getFluidState(h.absolutePos(new BlockPos(10,3,5)));
            h.assertTrue(!refined.isEmpty()&&!refined.isSource(),"Refined fluid created an infinite source");h.succeed();
        });
    }
    @GameTest(template="spell_arena",timeoutTicks=40) public static void wasteProjectileAndCoal(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(5,3,8));
        var cow=new net.minecraft.world.entity.animal.Cow(EntityType.COW,level);cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(Vec3.atCenterOf(pos));level.addFreshEntity(cow);
        var projectile=new ThrownAlchemicalWaste(AlchemyMaterials.THROWN_WASTE.get(),level);
        projectile.setPos(pos.getX()+.5,pos.getY()+1,pos.getZ()-3);projectile.shoot(0,0,1,1.2F,0);level.addFreshEntity(projectile);
        var gravity=new ThrownAlchemicalWaste(AlchemyMaterials.THROWN_WASTE.get(),level);gravity.setPos(Vec3.atCenterOf(pos.above(8)));gravity.setDeltaMovement(.2,0,0);gravity.tick();
        h.assertTrue(gravity.getDeltaMovement().y<0,"Waste projectile has no gravity");gravity.discard();
        var crafting=CraftingInput.of(1,1,List.of(new ItemStack(AlchemyMaterials.WASTE.get())));
        var result=level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,crafting,level).orElseThrow().value().assemble(crafting,level.registryAccess());
        h.assertTrue(result.is(Items.COAL)&&result.getCount()==9,"Waste does not unpack to nine coal");
        // Advance only the projectile: inspect impact before Wither II's first damage tick.
        for(int tick=0;tick<6&&!projectile.isRemoved();tick++)projectile.tick();
        var effect=cow.getEffect(MobEffects.WITHER);
        h.assertTrue(projectile.isRemoved()&&effect!=null&&effect.getAmplifier()==1&&effect.getDuration()==100,"Waste did not hit with five-second Wither II");
        h.assertTrue(cow.getHealth()==cow.getMaxHealth(),"Waste added unrequested direct damage");h.succeed();
    }
    private AlchemyMaterialsGameTests(){}
}
