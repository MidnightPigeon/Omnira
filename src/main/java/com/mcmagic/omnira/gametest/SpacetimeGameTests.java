package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.spacetime.*;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_spacetime")
@PrefixGameTestTemplate(false)
public final class SpacetimeGameTests {
    @GameTest(template="spell_arena") public static void corridorVoidRockKeepsRiftsAndShape(GameTestHelper h){
        int floorVoid=0,wallVoid=0,regular=0;
        for(int z=0;z<512;z++){
            for(int x=-2;x<=2;x++){
                var floor=CorridorLayout.block(x,CorridorLayout.FLOOR,z);
                if(floor.is(DreamContent.VOID_MIRROR_ROCK.get()))floorVoid++;
                else h.assertTrue(floor.is(DreamContent.MIRROR_ROCK.get()),"Corridor floor changed shape");
            }
            for(int x:new int[]{-2,2})for(int y=CorridorLayout.FLOOR+1;y<=CorridorLayout.FLOOR+4;y++){
                var block=CorridorLayout.block(x,y,z);
                if(block.is(ModBlocks.SPACETIME_RIFT.get()))continue;
                if(block.is(DreamContent.VOID_MIRROR_ROCK.get()) || block.is(DreamContent.VOID_ENGRAVED_MIRROR_ROCK.get()))wallVoid++;
                else if(block.is(DreamContent.MIRROR_ROCK.get()) || block.is(DreamContent.ENGRAVED_MIRROR_ROCK.get()))regular++;
                else h.fail("Unexpected corridor wall or ceiling material");
            }
            h.assertTrue(CorridorLayout.block(0,CorridorLayout.FLOOR+1,z).isAir(),"Void rock blocked the passage");
        }
        h.assertTrue(floorVoid>120 && wallVoid>90 && regular>1500,"Void rock is missing or dominates the corridor");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void corridorRiftsUseBothHeights(GameTestHelper h){
        int low=0,high=0,count=0;
        for(int z=0;z<24*160;z++)for(int x:new int[]{-2,2}){
            if(!CorridorLayout.corridorRift(x,z))continue;
            count++;
            int bottom=CorridorLayout.riftBottom(z);
            h.assertTrue(bottom==129 || bottom==130,"Rift bottom outside corridor");
            h.assertTrue(CorridorLayout.block(x,bottom,z).is(ModBlocks.SPACETIME_RIFT.get())
                    && CorridorLayout.block(x,bottom+1,z).is(ModBlocks.SPACETIME_RIFT.get()),"Rift halves did not follow height");
            if(bottom==129)low++;else high++;
        }
        h.assertTrue(low>20 && high>20 && count>80,"Rift frequency or vertical distribution is too low");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void heldExpiryAndThrownFlight(GameTestHelper h){
        var level=h.getLevel();var pos=Vec3.atCenterOf(h.absolutePos(new BlockPos(5,30,5)));
        var player=new net.neoforged.neoforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"aggregate-test"));player.setPos(pos);level.addFreshEntity(player);
        var held=new ItemStack(ModItems.UNSTABLE_SPACETIME_AGGREGATE.get());held.setDamageValue(127);
        held.set(ModDataComponents.AGGREGATE_TICKS,9);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,held);
        held.getItem().inventoryTick(held,level,player,0,true);
        h.assertTrue(held.isEmpty() && player.getHealth()==0,"Held expiry did not consume aggregate or cause true damage");
        var thrown=ModEntityTypes.UNSTABLE_AGGREGATE.get().create(level);thrown.setPos(pos.add(0,20,0));
        var item=new ItemStack(ModItems.UNSTABLE_SPACETIME_AGGREGATE.get());item.setDamageValue(127);thrown.setItem(item);thrown.setDeltaMovement(.2,.5,0);
        level.addFreshEntity(thrown);thrown.tick();
        h.assertTrue(!thrown.isRemoved() && thrown.getDeltaMovement().y<.5,"Throw detonated immediately or ignored gravity");
        for(int i=1;i<10;i++)thrown.tick();
        h.assertTrue(thrown.isRemoved(),"Thrown durability did not expire after ten ticks");
        var impact=ModEntityTypes.UNSTABLE_AGGREGATE.get().create(level);impact.setPos(pos.add(0,40,0));impact.setItem(new ItemStack(ModItems.UNSTABLE_SPACETIME_AGGREGATE.get()));
        level.setBlock(BlockPos.containing(impact.position()).east(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),2);
        impact.setDeltaMovement(1,0,0);level.addFreshEntity(impact);impact.tick();
        h.assertTrue(impact.isRemoved(),"Thrown collision did not detonate");h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=60) public static void closedChestRepairsAutomatically(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(5,2,5));h.getLevel().setBlock(pos,net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState(),2);
        var chest=(net.minecraft.world.level.block.entity.ChestBlockEntity)h.getLevel().getBlockEntity(pos);
        var item=new ItemStack(ModItems.UNSTABLE_SPACETIME_AGGREGATE.get());item.setDamageValue(10);chest.setItem(0,item);
        h.runAfterDelay(25,()->{int damage=chest.getItem(0).getDamageValue();h.assertTrue(damage>=7 && damage<=8,"Closed chest did not repair at ten tick intervals: "+damage);h.succeed();});
    }
    @GameTest(template="spell_arena") public static void createStructurePartialErasure(GameTestHelper h){
        if(!net.neoforged.fml.ModList.get().isLoaded("create")){h.succeed();return;}
        SpacetimeCompatibilityChecks.create(h);
    }
    @GameTest(template="spell_arena") public static void sableStructurePartialErasure(GameTestHelper h){
        if(!net.neoforged.fml.ModList.get().isLoaded("sable")){h.succeed();return;}
        SpacetimeCompatibilityChecks.sable(h);
    }
    @GameTest(template="spell_arena") public static void durabilityHandsBackpackAndContainers(GameTestHelper h){
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);var stack=new ItemStack(ModItems.UNSTABLE_SPACETIME_AGGREGATE.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,stack);
        for(int i=0;i<10;i++)stack.getItem().inventoryTick(stack,h.getLevel(),player,40,false);
        h.assertTrue(stack.getDamageValue()==1 && stack.getMaxDamage()==128,"Offhand countdown must cost one point per ten ticks");
        player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,ItemStack.EMPTY);
        player.getInventory().setItem(12,stack);
        for(int i=0;i<100;i++)stack.getItem().inventoryTick(stack,h.getLevel(),player,12,false);
        h.assertTrue(stack.getDamageValue()==11,"Backpack must keep draining durability");
        player.getInventory().setItem(12,ItemStack.EMPTY);
        var chest=new net.minecraft.world.SimpleContainer(3);chest.setItem(0,stack);
        AggregateStorage.container(chest,10);h.assertTrue(chest.getItem(0).getDamageValue()==10,"Container did not repair");
        chest.getItem(0).setDamageValue(10);AggregateStorage.container(chest,10);h.assertTrue(chest.getItem(0).getDamageValue()==10,"Capability aliases repaired twice in same tick");
        var handler=new net.neoforged.neoforge.items.ItemStackHandler(1);handler.setStackInSlot(0,chest.getItem(0).copy());
        AggregateStorage.handler(handler,20);h.assertTrue(handler.getStackInSlot(0).getDamageValue()==9,"Capability container did not repair");
        player.getInventory().setItem(1,stack.copy());AggregateStorage.container(player.getInventory(),30);
        h.assertTrue(player.getInventory().getItem(1).getDamageValue()==11,"Player inventory counted as recovery container");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void inspectedStorageIsStableUntilTaken(GameTestHelper h){
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var chest=new net.minecraft.world.SimpleContainer(27);
        var item=new ItemStack(ModItems.UNSTABLE_SPACETIME_AGGREGATE.get());item.setDamageValue(100);chest.setItem(0,item);
        player.containerMenu=net.minecraft.world.inventory.ChestMenu.threeRows(1,player.getInventory(),chest);
        for(int tick=1;tick<=20;tick++){
            // Merely inspecting slots, including a mod forwarding inventory ticks, cannot drain storage.
            item.getItem().inventoryTick(item,h.getLevel(),player,0,false);
            AggregateStorage.entity(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(player));
            if(tick%10==0)AggregateStorage.container(chest,tick);
        }
        h.assertTrue(chest.getItem(0).getDamageValue()==98,"Inspecting chest damaged aggregate");
        player.containerMenu.setCarried(chest.removeItemNoUpdate(0));
        for(int i=0;i<10;i++)AggregateStorage.entity(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(player));
        h.assertTrue(player.containerMenu.getCarried().getDamageValue()==98,"Cursor must pause without repairing");
        var carried=player.containerMenu.getCarried();player.containerMenu.setCarried(ItemStack.EMPTY);player.getInventory().setItem(15,carried);
        for(int i=0;i<10;i++)carried.getItem().inventoryTick(carried,h.getLevel(),player,15,false);
        h.assertTrue(carried.getDamageValue()==99,"Backpack did not drain after extraction");
        carried.setDamageValue(127);carried.set(ModDataComponents.AGGREGATE_TICKS,9);
        carried.getItem().inventoryTick(carried,h.getLevel(),player,15,false);
        h.assertTrue(carried.isEmpty(),"Backpack exhaustion did not consume aggregate");h.succeed();
    }
    @GameTest(template="spell_arena") public static void stormBypassesDefensesAndErasesStructures(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(6,4,6));var center=Vec3.atCenterOf(pos);
        var mob=EntityType.ZOMBIE.create(level);mob.setPos(center.add(0,0,2));mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);mob.setHealth(100);
        mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_ABSORPTION).setBaseValue(40);
        mob.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.NETHERITE_CHESTPLATE));mob.setAbsorptionAmount(40);mob.setInvulnerable(true);
        mob.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE,200,10));level.addFreshEntity(mob);
        var frame=new net.minecraft.world.entity.decoration.ArmorStand(level,center.x+1,center.y,center.z);level.addFreshEntity(frame);
        var orb=ModEntityTypes.CRUISE_ORB.get().create(level);orb.setPos(center.add(-2,0,0));level.addFreshEntity(orb);
        var outside=EntityType.PIG.create(level);outside.setPos(center.add(9,9,9));level.addFreshEntity(outside);
        var forge=pos.offset(3,0,0);com.mcmagic.omnira.forging.ForgeLayout.assemble(level,forge,net.minecraft.core.Direction.SOUTH);
        ((com.mcmagic.omnira.forging.AdvancedForgeBlockEntity)level.getBlockEntity(forge)).setItem(0,new ItemStack(Items.DIAMOND));
        SpacetimeStorm.trigger(level,center);
        h.assertTrue(mob.getHealth()==60 && mob.getAbsorptionAmount()==40,"Storm was reduced or consumed absorption instead of health");
        h.assertTrue(frame.isRemoved() && orb.isRemoved(),"Nonliving entities survived");
        h.assertTrue(outside.isAlive(),"Cube corner outside sphere was damaged");
        h.assertTrue(level.getBlockState(forge).isAir() && level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(10)).isEmpty(),"Structure was not erased without drops");h.succeed();
    }
    @GameTest(template="spell_arena") public static void discardedAggregateDetonatesAndRecipeIsOrdered(GameTestHelper h){
        var level=h.getLevel();var center=Vec3.atCenterOf(h.absolutePos(new BlockPos(5,4,5)));
        var frame=new net.minecraft.world.entity.decoration.ArmorStand(level,center.x+1,center.y,center.z);level.addFreshEntity(frame);
        var stack=new ItemStack(ModItems.UNSTABLE_SPACETIME_AGGREGATE.get());var drop=new net.minecraft.world.entity.item.ItemEntity(level,center.x,center.y,center.z,stack);
        stack.getItem().onEntityItemUpdate(stack,drop);h.assertTrue(drop.isRemoved() && stack.isEmpty() && frame.isRemoved(),"Dropped aggregate did not detonate");
        var recipe=level.getRecipeManager().getAllRecipesFor(ModRecipes.ADVANCED_FORGE_TYPE.get()).stream().map(net.minecraft.world.item.crafting.RecipeHolder::value).filter(r->r.result().is(ModItems.UNSTABLE_SPACETIME_AGGREGATE.get())).findFirst().orElseThrow();
        var input=new net.minecraft.world.SimpleContainer(9);for(int i=0;i<6;i++)input.setItem(i,new ItemStack(ModItems.SPACETIME_KNOT.get()));
        input.setItem(6,new ItemStack(ModItems.PARADOX_DUST.get()));input.setItem(7,new ItemStack(ModItems.RESONANCE_CRYSTAL.get()));input.setItem(8,new ItemStack(ModItems.PARADOX_DUST.get()));
        h.assertTrue(recipe.matches(new com.mcmagic.omnira.forging.AdvancedForgeRecipe.Input(input),level),"Left-middle-right recipe mismatch");h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=120) public static void corridorAndUniqueIslandEntrances(GameTestHelper h){
        var world=h.getLevel().getServer().getLevel(com.mcmagic.omnira.world.dimension.ModDimensions.SPACETIME_CORRIDOR);
        h.assertTrue(world!=null,"Corridor dimension missing");
        for(int z:new int[]{-10000,-1,0,10000}){
            world.getChunk(0,z>>4);
            for(int x=-1;x<=1;x++)for(int y=129;y<=131;y++)h.assertTrue(world.getBlockState(new BlockPos(x,y,z)).isAir(),"Corridor interior not 3x3");
            var floor=world.getBlockState(new BlockPos(0,128,z));
            h.assertTrue(floor.is(DreamContent.MIRROR_ROCK.get()) || floor.is(DreamContent.VOID_MIRROR_ROCK.get()),"Missing corridor floor");
        }
        for(boolean time:new boolean[]{true,false}){
            var entrance=CorridorLayout.entrance(time);world.getChunkAt(entrance);
            h.assertTrue(!CorridorLayout.block(entrance.getX(),entrance.getY(),entrance.getZ()).is(ModBlocks.SPACETIME_RIFT.get())
                    && !CorridorLayout.block(entrance.getX(),entrance.getY()+1,entrance.getZ()).is(ModBlocks.SPACETIME_RIFT.get()),"New continents still generate return rifts");
            h.assertTrue(!CorridorLayout.block(entrance.getX()+1000,entrance.getY()-1,0).isAir(),"Continent must extend beyond the old island");
            h.assertTrue(!CorridorLayout.block(entrance.getX()+1000,entrance.getY(),0).is(ModBlocks.SPACETIME_RIFT.get()),"Continent must not duplicate its entrance");
            var origin=new BlockPos(2,129,time?24:72);
            var rift=ModBlocks.SPACETIME_RIFT.get().defaultBlockState().setValue(SpacetimeRiftBlock.TIME,time);
            var pig=EntityType.PIG.create(world);pig.setPos(Vec3.atBottomCenterOf(origin));world.addFreshEntity(pig);
            rift.entityInside(world,origin,pig);
            h.assertTrue(!pig.isRemoved() && Math.abs(pig.getX())<=CorridorLayout.RIFT_RANGE+1 && Math.abs(pig.getZ())<=CorridorLayout.RIFT_RANGE+1
                    && (Math.abs(pig.getY()-CorridorLayout.TIME_ENTRANCE.getY())<1 || Math.abs(pig.getY()-CorridorLayout.SPACE_ENTRANCE.getY())<1),"Rift did not reach a random continent landing");
            pig.getPersistentData().putLong("OmniraRiftCooldown",0);
            rift.entityInside(world,entrance,pig);
            h.assertTrue(pig.getY()!=129 && !pig.getPersistentData().contains("OmniraRiftOrigin"),"Old return rift still works");pig.discard();
        }
        h.assertTrue(CorridorLayout.block(0,224,0).is(DreamContent.TEMPORAL_SILT.get())
                || CorridorLayout.block(0,224,0).is(DreamContent.LIVING_TEMPORAL_SILT.get()),"Time surface is not silt");
        h.assertTrue(CorridorLayout.block(0,40,0).is(DreamContent.SPATIAL_CRYSTAL.get())
                && CorridorLayout.block(0,80,0).is(DreamContent.SPATIAL_CRYSTAL.get()),"Space cave lacks floor or ceiling");
        int floorCones=0,ceilingCones=0,glowing=0;
        for(int x=0;x<64;x++)for(int z=0;z<64;z++){
            if(!CorridorLayout.block(x,48,z).isAir())floorCones++;
            if(!CorridorLayout.block(x,72,z).isAir())ceilingCones++;
            if(CorridorLayout.block(x,48,z).is(DreamContent.EXCITED_SPATIAL_CRYSTAL.get())
                    || CorridorLayout.block(x,72,z).is(DreamContent.EXCITED_SPATIAL_CRYSTAL.get()))glowing++;
        }
        h.assertTrue(floorCones>0 && ceilingCones>0 && glowing>0,"Cave cones or glowing surface are missing");
        h.assertTrue(DreamContent.EXCITED_SPATIAL_CRYSTAL.get().defaultBlockState().getLightEmission()==15,"Excited crystal light is not 15");
        h.assertTrue(DreamContent.SPATIAL_CRYSTAL.get().defaultBlockState().getLightEmission()==5,"Spatial crystal light is not 5");
        h.assertTrue(CorridorLayout.block(0,133,0).is(ModBlocks.PURE_SOLIDIFIED_SPACETIME.get()),"Corridor ceiling needs an opaque backing");
        h.assertTrue(CorridorLayout.block(10000,128,-10000).is(ModBlocks.PURE_SOLIDIFIED_SPACETIME.get()),"Middle separator must be continuous");
        var barrier=ModBlocks.PURE_SOLIDIFIED_SPACETIME.get().defaultBlockState();
        h.assertTrue(barrier.canOcclude() && barrier.getDestroySpeed(world,BlockPos.ZERO)<0
                && barrier.getPistonPushReaction()==net.minecraft.world.level.material.PushReaction.BLOCK,"Separator must be opaque, unbreakable and immovable");
        h.succeed();
    }
}
