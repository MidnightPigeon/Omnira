package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.paradox.*;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.space.anchor.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("omnira_paradox")
@PrefixGameTestTemplate(false)
public final class ParadoxMechanicsGameTests {
    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h){
        var p=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"paradox-test")){
            @Override public boolean teleportTo(net.minecraft.server.level.ServerLevel target,double x,double y,double z,
                    Set<RelativeMovement> relative,float yaw,float pitch){setServerLevel(target);setPos(x,y,z);return true;}
        };
        GameType.SURVIVAL.updatePlayerAbilities(p.getAbilities());p.setPos(h.absoluteVec(new Vec3(3.5,2,3.5)));return p;
    }
    @GameTest(template="spell_arena") public static void blinkIsExactlyTwentyEvenInsideBlocks(GameTestHelper h){
        var p=player(h);p.setYRot(37);p.setXRot(-18);var start=p.position();var target=start.add(p.getLookAngle().normalize().scale(20));
        var pos=BlockPos.containing(target);h.getLevel().setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
        h.assertTrue(ParadoxDustItem.trigger(p,ParadoxDustItem.Paradox.BLINK),"Blink refused solid destination");
        h.assertTrue(p.position().distanceTo(target)<1e-6&&Math.abs(p.position().distanceTo(start)-20)<1e-6,"Blink adjusted length or direction");
        h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        var anchor=h.absolutePos(new BlockPos(3,1,3));h.getLevel().setBlockAndUpdate(anchor,AnchorContent.ACTIVE.get().defaultBlockState());StabilityField.update(h.getLevel(),anchor,true);
        start=p.position();h.assertTrue(!ParadoxDustItem.trigger(p,ParadoxDustItem.Paradox.BLINK)&&p.position().equals(start),"Blink bypassed stability field");
        h.getLevel().setBlockAndUpdate(anchor,Blocks.AIR.defaultBlockState());StabilityField.update(h.getLevel(),anchor,false);h.succeed();
    }
    @GameTest(template="spell_arena") public static void restoreDreamAndElementResults(GameTestHelper h){
        var p=player(h);p.setHealth(3);p.getFoodData().setFoodLevel(2);
        ParadoxDustItem.trigger(p,ParadoxDustItem.Paradox.RESTORE);
        h.assertTrue(p.getHealth()==p.getMaxHealth()&&p.getFoodData().getFoodLevel()==20,"Recovery incomplete");
        h.assertTrue(ParadoxDustItem.trigger(p,ParadoxDustItem.Paradox.DREAM_CREATURE),"Dream tag summon failed");
        var mobs=h.getLevel().getEntitiesOfClass(Mob.class,p.getBoundingBox().inflate(5),e->e.getType().is(ParadoxDustItem.DREAMS));
        h.assertTrue(!mobs.isEmpty(),"Summon not drawn from dream tag");mobs.forEach(Entity::discard);
        var allowed=Set.of(ModItems.EARTH_MICROCORE.get(),ModItems.WATER_MICROCORE.get(),ModItems.FIRE_MICROCORE.get(),ModItems.AIR_MICROCORE.get());
        p.getRandom().setSeed(652);for(int i=0;i<32;i++)ParadoxDustItem.trigger(p,ParadoxDustItem.Paradox.MICROCORE);
        int count=0;var seen=new HashSet<Item>();
        for(var stack:p.getInventory().items)if(!stack.isEmpty()){h.assertTrue(allowed.contains(stack.getItem()),"Unexpected core element");count+=stack.getCount();seen.add(stack.getItem());}
        h.assertTrue(count==32&&seen.size()==4,"Core reward count or available elements incorrect");h.succeed();
    }
    @GameTest(template="spell_arena") public static void consumptionAndNonPlaceableItems(GameTestHelper h){
        var p=player(h);var dust=new ItemStack(ModItems.PARADOX_DUST.get(),8);p.setItemInHand(InteractionHand.MAIN_HAND,dust);
        dust.getItem().use(h.getLevel(),p,InteractionHand.MAIN_HAND);
        h.assertTrue(dust.getCount()==7,"Dust not consumed exactly once");
        for(var item:List.of(ParadoxContent.MYSTERY.get(),ParadoxContent.ORE.get(),ParadoxContent.BOTTLED_FLOW.get()))h.assertTrue(!(item instanceof BlockItem),"Material can be placed");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void anvilWaitsFallsHurtsAndVanishes(GameTestHelper h){
        h.setBlock(3,1,3,Blocks.STONE);var pig=h.spawn(EntityType.PIG,new Vec3(3.5,2,3.5));pig.setNoAi(true);
        var anvil=ParadoxContent.ANVIL.get().create(h.getLevel());anvil.setPos(h.absoluteVec(new Vec3(3.5,8,3.5)));h.getLevel().addFreshEntity(anvil);
        double height=anvil.getY();for(int i=0;i<59;i++)anvil.tick();
        h.assertTrue(anvil.remainingWait()==1&&anvil.getY()==height,"Anvil fell before three seconds");
        anvil.tick();h.assertTrue(anvil.remainingWait()==0&&anvil.getY()==height,"Suspension timing incorrect");
        anvil.tick();h.assertTrue(anvil.getY()<height,"Anvil did not start falling");
        for(int i=0;i<150&&!anvil.isRemoved();i++)anvil.tick();
        h.assertTrue(anvil.isRemoved()&&pig.getHealth()<pig.getMaxHealth(),"Anvil did not damage/vanish on landing");
        h.assertTrue(h.getBlockState(new BlockPos(3,2,3)).isAir()&&h.getBlockState(new BlockPos(3,1,3)).is(Blocks.STONE),"Anvil left a block or changed floor");
        pig.discard();h.succeed();
    }
    @GameTest(template="spell_arena") public static void anvilWaitSurvivesReload(GameTestHelper h){
        var anvil=ParadoxContent.ANVIL.get().create(h.getLevel());anvil.setPos(h.absoluteVec(new Vec3(3,8,3)));
        for(int i=0;i<23;i++)anvil.tick();var data=new net.minecraft.nbt.CompoundTag();anvil.saveWithoutId(data);
        var restored=ParadoxContent.ANVIL.get().create(h.getLevel());restored.load(data);
        h.assertTrue(restored.remainingWait()==37&&!restored.dropItem&&restored.getBlockState().is(Blocks.ANVIL),"Anvil reload changed timer or drop rules");h.succeed();
    }
    @GameTest(template="spell_arena") public static void threeRecipesUseSpecifiedInputs(GameTestHelper h){
        var manager=h.getLevel().getRecipeManager();var grid=NonNullList.withSize(9,new ItemStack(ModItems.PARADOX_DUST.get()));
        var input=CraftingInput.of(3,3,grid);var recipe=manager.getRecipeFor(RecipeType.CRAFTING,input,h.getLevel()).orElseThrow();
        h.assertTrue(recipe.value().assemble(input,h.getLevel().registryAccess()).is(ParadoxContent.MYSTERY.get()),"Nine-dust craft missing");
        var assembly=manager.getAllRecipesFor(ModRecipes.ASSEMBLY_TYPE.get()).stream().map(RecipeHolder::value).filter(r->r.result().is(ParadoxContent.ORE.get())).findFirst().orElseThrow();
        h.assertTrue(assembly.slots().get(6).test(new ItemStack(ParadoxContent.MYSTERY.get())),"Mystery is not assembly center");
        for(int i=0;i<6;i++)h.assertTrue(assembly.slots().get(i).test(new ItemStack(ModItems.SPIRITUAL_CRYSTAL.get())),"Assembly crystal mismatch");
        var reaction=manager.getAllRecipesFor(ModRecipes.REACTION_TYPE.get()).stream().map(RecipeHolder::value).filter(r->r.results().stream().anyMatch(s->s.is(ParadoxContent.BOTTLED_FLOW.get()))).findFirst().orElseThrow();
        h.assertTrue(reaction.fluid().getFluid()==net.minecraft.world.level.material.Fluids.WATER&&reaction.fluid().getAmount()==1000&&reaction.items().size()==2
                &&reaction.support().isEmpty()&&reaction.duration()==2000,"Unspecified reaction requirement or quantity");
        h.succeed();
    }
    private ParadoxMechanicsGameTests(){}
}
