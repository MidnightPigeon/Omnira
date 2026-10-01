package com.mcmagic.omnira.gametest;
import com.mcmagic.omnira.forging.*;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.common.util.FakePlayer;
import java.util.*;

@GameTestHolder("omnira_advanced_assembly_table")
@PrefixGameTestTemplate(false)
public final class AdvancedForgeGameTests {
    @GameTest(template="spell_arena") public static void correctedRegistryIdsAndSaveAliases(GameTestHelper h){
        var blocks=net.minecraft.core.registries.BuiltInRegistries.BLOCK;
        var items=net.minecraft.core.registries.BuiltInRegistries.ITEM;
        h.assertTrue(blocks.get(id("advanced_forge"))==ModBlocks.ADVANCED_FORGE.get(),"Old assembly block ID not aliased");
        h.assertTrue(items.get(id("advanced_forge"))==ModItems.ADVANCED_FORGE.get(),"Old assembly item ID not aliased");
        h.assertTrue(blocks.get(id("test_spell_core"))==ModBlocks.TEST_SPELL_CORE.get(),"Old core block ID not aliased");
        h.assertTrue(items.get(id("test_spell_core"))==ModItems.TEST_SPELL_CORE.get(),"Old core item ID not aliased");
        h.assertTrue(items.get(id("crystallized_nail"))==ModItems.CRYSTALLIZED_NAIL.get(),"Old nail ID not aliased");
        h.assertTrue(blocks.get(id("spatial_crystal"))==DreamContent.SPATIAL_CRYSTAL.get(),"Old rough crystal block ID not aliased");
        h.assertTrue(items.get(id("spatial_crystal"))==net.minecraft.world.item.Item.byBlock(DreamContent.SPATIAL_CRYSTAL.get()),"Old rough crystal item ID not aliased");
        h.assertTrue(blocks.get(id("excited_spatial_crystal"))==DreamContent.EXCITED_SPATIAL_CRYSTAL.get(),"Old excited crystal block ID not aliased");
        h.assertTrue(items.get(id("excited_spatial_crystal"))==net.minecraft.world.item.Item.byBlock(DreamContent.EXCITED_SPATIAL_CRYSTAL.get()),"Old excited crystal item ID not aliased");
        h.succeed();
    }
    private static net.minecraft.resources.ResourceLocation id(String path){
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("omnira",path);
    }
    @GameTest(template="spell_arena") public static void creativeWholeMachineItem(GameTestHelper h){
        var center=h.absolutePos(new BlockPos(5,4,5));var p=player(h,center);p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        var item=new ItemStack(ModItems.ADVANCED_FORGE.get());p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,item);
        var floor=center.below();h.getLevel().setBlockAndUpdate(floor,Blocks.STONE.defaultBlockState());
        var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atBottomCenterOf(center),Direction.UP,floor,false);
        for(var facing:Direction.Plane.HORIZONTAL){
            p.setYRot(facing.toYRot());
            var context=new net.minecraft.world.item.context.BlockPlaceContext(p,net.minecraft.world.InteractionHand.MAIN_HAND,item,hit);
            var blocked=center.above().east();h.getLevel().setBlockAndUpdate(blocked,Blocks.STONE.defaultBlockState());
            h.assertTrue(!((BlockItem)item.getItem()).place(context).consumesAction() && h.getLevel().getBlockState(center).isAir(),"Placement overwrites obstacle or leaves partial machine");
            h.getLevel().removeBlock(blocked,false);
            h.assertTrue(((BlockItem)item.getItem()).place(context).consumesAction(),"Creative machine placement failed");
            var state=h.getLevel().getBlockState(center);
            h.assertTrue(ForgeLayout.formedParts(h.getLevel(),center,state).size()==18 && state.getValue(AdvancedForgeBlock.FACING)==facing,"Incomplete or incorrectly oriented machine");
            var part=center.offset(ForgeLayout.offset(facing,17));var partState=h.getLevel().getBlockState(part);
            partState.getBlock().playerWillDestroy(h.getLevel(),part,partState,p);
            for(int i=0;i<18;i++)h.assertTrue(h.getLevel().getBlockState(center.offset(ForgeLayout.offset(facing,i))).isAir(),"Creative destruction left a part");
        }
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        h.assertTrue(!((BlockItem)item.getItem()).place(new net.minecraft.world.item.context.BlockPlaceContext(p,net.minecraft.world.InteractionHand.MAIN_HAND,item,hit)).consumesAction(),"Creative item bypasses survival assembly");
        h.succeed();
    }
    static FakePlayer player(GameTestHelper h,BlockPos center){
        var p=new FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"forge-test"));p.setPos(center.getX()+.5,center.getY(),center.getZ()+3);return p;
    }
    static void build(GameTestHelper h,BlockPos center,Direction facing){
        for(int part=0;part<18;part++){
            int x=part%3-1,z=part%9/3-1;var block=ForgeLayout.required(x,part/9,z);var state=block.defaultBlockState();
            if(state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)){
                state=state.setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.from2DDataValue(part%4));
            }
            h.getLevel().setBlockAndUpdate(center.offset(ForgeLayout.offset(facing,part)),state);
        }
    }
    @GameTest(template="spell_arena") public static void formationIgnoresFacingAndProtectsAllParts(GameTestHelper h){
        var center=h.absolutePos(new BlockPos(5,2,5));var p=player(h,center);
        for(var facing:Direction.Plane.HORIZONTAL){
            h.assertTrue(ForgeLayout.required(0,1,-1)==Blocks.AIR && ForgeLayout.required(0,1,1)==ModBlocks.MANA_ENGINE.get(),"Entrance must be clear and node at rear");
            build(h,center,facing);var crystal=new ItemStack(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get(),2);
            h.assertTrue(ForgeLayout.form(h.getLevel(),center,p,crystal) && crystal.getCount()==1,"Valid rotated structure rejected");
            var entrance=center.offset(ForgeLayout.offset(facing,0,1,-1));
            var rear=center.offset(ForgeLayout.offset(facing,0,1,1));
            h.assertTrue(h.getLevel().getBlockState(entrance).getShape(h.getLevel(),entrance).isEmpty(),"Entrance has invisible obstruction");
            h.assertTrue(!h.getLevel().getBlockState(rear).getShape(h.getLevel(),rear).isEmpty(),"Rear node cannot be targeted");
            var forge=(AdvancedForgeBlockEntity)h.getLevel().getBlockEntity(center);
            var panelPos=center.offset(ForgeLayout.offset(facing,1));
            var panelState=h.getLevel().getBlockState(panelPos);
            var front=facing.getOpposite();
            var target=new net.minecraft.world.phys.Vec3(center.getX()+.5,center.getY()+.625,center.getZ()+.5);
            var direction=new net.minecraft.world.phys.Vec3(front.getStepX(),0,front.getStepZ());
            var hit=panelState.getShape(h.getLevel(),panelPos).clip(target.add(direction.scale(2)),target,panelPos);
            h.assertTrue(hit!=null && hit.getLocation().y>center.getY()+.4,"Front panel cannot be targeted: "+facing);
            panelState.useWithoutItem(h.getLevel(),p,hit);
            h.assertTrue(forge.buttonTicks()>0,"Panel did not press start button: "+facing);
            var tabletop=center.above();
            var tableState=h.getLevel().getBlockState(tabletop);
            h.assertTrue(tableState.getShape(h.getLevel(),tabletop).clip(
                    target.add(0,2,0),target,tabletop)!=null,"Raised tabletop cannot be targeted");
            h.assertTrue(forge!=null && !forge.start(p),"Empty forge processed");
            h.assertTrue(forge.getBlockState().getValue(AdvancedForgeBlock.FACING)==facing,"Layout orientation inferred from block facing");
            for(int part=0;part<18;part++){
                var at=center.offset(ForgeLayout.offset(facing,part));var state=h.getLevel().getBlockState(at);
                h.assertTrue(state.getDestroySpeed(h.getLevel(),at)<0 && state.getPistonPushReaction()==net.minecraft.world.level.material.PushReaction.BLOCK,"Unprotected component");
                h.getLevel().setBlock(at,Blocks.AIR.defaultBlockState(),2);
            }
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void nineInputsCoreTimingPersistenceAndLocks(GameTestHelper h){
        var center=h.absolutePos(new BlockPos(5,2,5));var p=player(h,center);build(h,center,Direction.SOUTH);
        h.assertTrue(ForgeLayout.form(h.getLevel(),center,p,new ItemStack(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get())),"Cannot form fixture");
        var forge=(AdvancedForgeBlockEntity)h.getLevel().getBlockEntity(center);
        var manager=h.getLevel().getRecipeManager();var previous=List.copyOf(manager.getRecipes());
        var recipes=new ArrayList<RecipeHolder<?>>(previous);
        var ingredients=new ArrayList<Ingredient>();for(int i=0;i<9;i++)ingredients.add(Ingredient.of(Items.STONE));
        recipes.add(new RecipeHolder<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("omnira","forge_test_only"),new AdvancedForgeRecipe(ingredients,new ItemStack(Items.DIAMOND))));
        manager.replaceRecipes(recipes);
        var victim=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        try{
            for(var core:List.of(ModItems.TEST_SPELL_CORE.get(),ModItems.DREAM_SPELL_CORE.get(),ModItems.LIGHT_DARK_SPELL_CORE.get())){
                for(int i=0;i<9;i++)forge.setItem(i,new ItemStack(Items.STONE));
                forge.setItem(9,ItemStack.EMPTY);h.assertTrue(!forge.start(p),"Missing core accepted");forge.setItem(9,new ItemStack(core));
                forge.setItem(8,ItemStack.EMPTY);h.assertTrue(!forge.start(p),"Physical node ingredient ignored");forge.setItem(8,new ItemStack(Items.STONE));
                int duration=core==ModItems.TEST_SPELL_CORE.get()?1200:600;h.assertTrue(AdvancedForgeBlockEntity.duration(forge.getItem(9))==duration && forge.start(p),"Core time incorrect");
                var menu=new AdvancedForgeMenu(1,p.getInventory(),forge);
                h.assertTrue(menu.slots.size()==43 && menu.slots.get(6).getContainerSlot()==9 && !menu.slots.get(6).mayPickup(p),"Core lock or UI mapping incorrect");
                for(int i=0;i<duration/5-1;i++)forge.tick();h.assertTrue(!forge.hot() && !forge.getItem(0).isEmpty(),"Materials consumed during preparation");
                if(core==ModItems.TEST_SPELL_CORE.get()){
                    victim.setPos(center.getX()+.5,center.getY()+.2,center.getZ()+.5);h.getLevel().addFreshEntity(victim);
                }
                forge.tick();h.assertTrue(forge.hot() && forge.getItem(0).isEmpty(),"Glow phase did not consume materials");
                if(core==ModItems.TEST_SPELL_CORE.get()){
                    h.assertTrue(victim.getHealth()==18 && victim.isOnFire(),"Active glow must deal two damage and ignite players");
                    victim.setPos(center.getX()+.5,center.getY(),center.getZ()+3);victim.clearFire();
                }
                var saved=forge.saveWithFullMetadata(h.getLevel().registryAccess());forge.loadWithComponents(saved,h.getLevel().registryAccess());
                for(int i=duration/5;i<duration;i++)forge.tick();
                h.assertTrue(!forge.working() && forge.output().is(Items.DIAMOND) && forge.getItem(9).is(core),"Completion, persistence or core retention failed");
                forge.collect(p);h.assertTrue(!forge.collect(p),"Output collected twice");
            }
            for(int i=0;i<9;i++)forge.setItem(i,new ItemStack(Items.STONE));
            h.assertTrue(forge.start(p),"Cannot start bottle fixture");
            for(int i=0;i<120;i++)forge.tick();
            var bottle=new ItemStack(ModItems.POCKET_MAGIC_BOTTLE.get());
            h.assertTrue(com.mcmagic.omnira.item.bottle.MultiblockBottleCapture.capture(p,center.above().east(),bottle),"Whole machine capture failed");
            for(int part=0;part<18;part++)h.assertTrue(h.getLevel().getBlockState(center.offset(ForgeLayout.offset(Direction.SOUTH,part))).isAir(),"Capture left part behind");
            var projectile=new com.mcmagic.omnira.item.bottle.ThrownPocketBottle(ModEntityTypes.POCKET_BOTTLE.get(),h.getLevel());
            projectile.setOwner(p);projectile.setPos(p.position());h.getLevel().addFreshEntity(projectile);
            var storage=com.mcmagic.omnira.item.bottle.BottleStorage.get(h.getLevel());var id=bottle.get(ModDataComponents.BOTTLE_CAPTURE);
            h.assertTrue(storage.claim(id,projectile.getUUID()),"Cannot claim bottle");
            h.getLevel().setBlockAndUpdate(center,Blocks.STONE.defaultBlockState());
            var impact=net.minecraft.world.phys.Vec3.atBottomCenterOf(center);
            h.assertTrue(!com.mcmagic.omnira.item.bottle.PocketBottleItem.release(h.getLevel(),bottle,impact,projectile.getUUID()) && storage.get(id)!=null,"Blocked release lost contents");
            h.getLevel().setBlockAndUpdate(center,Blocks.AIR.defaultBlockState());
            java.util.function.Consumer<net.neoforged.neoforge.event.level.BlockEvent.EntityMultiPlaceEvent> deny=e->e.setCanceled(true);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(deny);
            try{
                h.assertTrue(!com.mcmagic.omnira.item.bottle.PocketBottleItem.release(h.getLevel(),bottle,impact,projectile.getUUID()),"Bottle bypassed placement protection");
                h.assertTrue(storage.get(id)!=null && h.getLevel().getBlockState(center).isAir(),"Denied placement did not roll back");
            }finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(deny);}
            h.assertTrue(com.mcmagic.omnira.item.bottle.PocketBottleItem.release(h.getLevel(),bottle,impact,projectile.getUUID()),"Bottle release failed");
            h.assertTrue(!com.mcmagic.omnira.item.bottle.PocketBottleItem.release(h.getLevel(),bottle,impact,projectile.getUUID()),"Duplicate bottle release");
            forge=(AdvancedForgeBlockEntity)h.getLevel().getBlockEntity(center);
            h.assertTrue(forge.working() && forge.getItem(9).is(ModItems.LIGHT_DARK_SPELL_CORE.get()),"Bottle lost core or processing state");
            for(int i=120;i<600;i++)forge.tick();
            h.assertTrue(forge.output().is(Items.DIAMOND),"Captured hot process did not resume");projectile.discard();
        }finally{manager.replaceRecipes(previous);victim.discard();}
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void rejectsOccupiedComponents(GameTestHelper h){
        var center=h.absolutePos(new BlockPos(5,2,5));build(h,center,Direction.SOUTH);var p=player(h,center);
        var be=(com.mcmagic.omnira.block.entity.ManaEngineAccess)h.getLevel().getBlockEntity(center.offset(ForgeLayout.offset(Direction.SOUTH,0,1,1)));
        be.engineState().inventory.setItem(0,new ItemStack(ModItems.TEST_SPELL_CORE.get()));var crystal=new ItemStack(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get());
        h.assertTrue(!ForgeLayout.form(h.getLevel(),center,p,crystal) && crystal.getCount()==1 && h.getLevel().getBlockState(center).is(ModBlocks.ARCANE_ASSEMBLY_TABLE.get()),"Formation erased occupied component");h.succeed();
    }
    @GameTest(template="spell_arena") public static void damagedAssemblyRecoversAndCanBeBottled(GameTestHelper h){
        var center=h.absolutePos(new BlockPos(5,2,5));var p=player(h,center);
        for(var facing:Direction.Plane.HORIZONTAL){
            build(h,center,facing);
            h.assertTrue(ForgeLayout.form(h.getLevel(),center,p,new ItemStack(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get())),"Cannot form recovery fixture");
            var forge=(AdvancedForgeBlockEntity)h.getLevel().getBlockEntity(center);
            forge.setItem(0,new ItemStack(Items.DIAMOND));
            if(facing==Direction.SOUTH)forge.setItem(9,new ItemStack(ModItems.LIGHT_DARK_SPELL_CORE.get()));
            for(int part:new int[]{1,13,16,17})h.getLevel().setBlock(center.offset(ForgeLayout.offset(facing,part)),Blocks.AIR.defaultBlockState(),2);
            h.assertTrue(ForgeLayout.formedParts(h.getLevel(),center,forge.getBlockState()).isEmpty(),"Incomplete fixture was accepted");
            var occupied=center.offset(ForgeLayout.offset(facing,17));
            h.getLevel().setBlock(occupied,Blocks.STONE.defaultBlockState(),2);
            h.assertTrue(!ForgeLayout.restoreMissingParts(h.getLevel(),center,forge.getBlockState()),"Recovery replaced foreign block");
            h.assertTrue(h.getLevel().getBlockState(center.above()).isAir(),"Failed recovery partially changed structure");
            h.getLevel().setBlock(occupied,Blocks.AIR.defaultBlockState(),2);
            forge.tick();
            h.assertTrue(ForgeLayout.formedParts(h.getLevel(),center,forge.getBlockState()).size()==18,"Missing parts not restored");
            h.assertTrue(h.getLevel().getBlockEntity(center)==forge && forge.getItem(0).is(Items.DIAMOND),"Recovery replaced inventory");
            p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            var click=new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock(p,center,Direction.UP,
                    net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action.START);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(click);
            h.assertTrue(click.isCanceled(),"Survival mining can remove a part");
            var bottle=new ItemStack(ModItems.POCKET_MAGIC_BOTTLE.get());
            java.util.function.Consumer<net.neoforged.neoforge.event.level.BlockEvent.BreakEvent> deny=e->e.setCanceled(true);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(deny);
            try{h.assertTrue(!com.mcmagic.omnira.item.bottle.MultiblockBottleCapture.capture(p,center,bottle),"Capture bypassed protection");}
            finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(deny);}
            h.assertTrue(com.mcmagic.omnira.item.bottle.MultiblockBottleCapture.capture(p,center.above(),bottle),"Recovered assembly cannot be bottled");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void creativeBreakRemovesWholeAssembly(GameTestHelper h){
        var center=h.absolutePos(new BlockPos(5,2,5));var p=player(h,center);
        p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        for(var facing:Direction.Plane.HORIZONTAL)for(int clicked=0;clicked<18;clicked++){
            build(h,center,facing);
            h.assertTrue(ForgeLayout.form(h.getLevel(),center,p,new ItemStack(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get())),"Formation failed");
            h.assertTrue(ForgeLayout.formedParts(h.getLevel(),center,h.getLevel().getBlockState(center)).size()==18,"Formation reported success with missing parts");
            var at=center.offset(ForgeLayout.offset(facing,clicked));
            h.assertTrue(p.gameMode.destroyBlock(at),"Creative destruction rejected");
            for(int i=0;i<18;i++)h.assertTrue(h.getLevel().getBlockState(center.offset(ForgeLayout.offset(facing,i))).isAir(),"Creative destruction left a part");
            h.assertTrue(h.getLevel().getBlockEntity(center)==null,"Destroyed controller remained");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void incompleteFormationRollsBack(GameTestHelper h){
        var center=h.absolutePos(new BlockPos(5,2,5));var p=player(h,center);build(h,center,Direction.SOUTH);
        var before=new java.util.HashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        for(int i=0;i<18;i++){var pos=center.offset(ForgeLayout.offset(Direction.SOUTH,i));before.put(pos,h.getLevel().getBlockState(pos));}
        var interrupted=new java.util.concurrent.atomic.AtomicBoolean();
        java.util.function.Consumer<net.neoforged.neoforge.event.level.BlockEvent.NeighborNotifyEvent> interrupt=e->{
            if(e.getState().is(ModBlocks.ADVANCED_FORGE.get()) && interrupted.compareAndSet(false,true))
                h.getLevel().setBlock(center.above(),Blocks.AIR.defaultBlockState(),18);
        };
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(interrupt);
        var crystal=new ItemStack(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get());
        try{h.assertTrue(!ForgeLayout.form(h.getLevel(),center,p,crystal),"Incomplete formation reported success");}
        finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(interrupt);}
        h.assertTrue(interrupted.get() && crystal.getCount()==1,"Failed formation consumed crystal");
        for(var entry:before.entrySet())h.assertTrue(h.getLevel().getBlockState(entry.getKey()).equals(entry.getValue()),"Formation rollback lost original state");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void wholeGlueAndPhysicsSelection(GameTestHelper h) throws Exception{
        if(net.neoforged.fml.ModList.get().isLoaded("create"))AdvancedForgeCompatibilityChecks.run(h);else h.succeed();
    }
    @GameTest(template="spell_arena") public static void productionRecipesMatchShuffledMaterials(GameTestHelper h){
        var center=h.absolutePos(new BlockPos(5,2,5));var p=player(h,center);build(h,center,Direction.SOUTH);
        h.assertTrue(ForgeLayout.form(h.getLevel(),center,p,new ItemStack(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get())),"Cannot form recipe fixture");
        var forge=(AdvancedForgeBlockEntity)h.getLevel().getBlockEntity(center);
        for(var output:List.of(ModItems.LIGHT_DARK_SPELL_CORE.get(),ModItems.CRUISE_ORB.get(),ModItems.ADVANCED_RITUAL_ENERGY_CORE.get())){
            var recipe=h.getLevel().getRecipeManager().getAllRecipesFor(ModRecipes.ADVANCED_FORGE_TYPE.get()).stream()
                    .map(RecipeHolder::value).filter(r->r.result().is(output)).findFirst().orElseThrow();
            for(int i=0;i<9;i++)forge.setItem(i,recipe.slots().get(i<6?5-i:i).getItems()[0].copy());
            boolean orb=recipe.result().is(ModItems.CRUISE_ORB.get());
            boolean ritual=recipe.result().is(ModItems.ADVANCED_RITUAL_ENERGY_CORE.get());
            // Actual player arrangement, viewed from the entrance: left / rear / right.
            forge.setItem(6,new ItemStack(ritual?ModItems.PARADOX_DUST.get():orb?ModItems.CRYSTAL_BALL.get():ModItems.LIGHT_MICROCORE.get()));
            forge.setItem(7,new ItemStack(orb||ritual?ModItems.LIGHT_DARK_SPELL_CORE.get():ModItems.TEST_SPELL_CORE.get()));
            forge.setItem(8,new ItemStack(ritual?ModItems.PARADOX_DUST.get():orb?ModItems.LIQUID_CRYSTAL_BALL.get():ModItems.DARK_MICROCORE.get()));
            var input=new AdvancedForgeRecipe.Input(forge);
            h.assertTrue(recipe.matches(input,h.getLevel()),"Shuffled central materials rejected");
            var first=forge.getItem(0).copy();forge.setItem(0,new ItemStack(Items.DIRT));
            h.assertTrue(!recipe.matches(input,h.getLevel()),"Incorrect ingredient accepted");forge.setItem(0,first);
            var middle=forge.getItem(7).copy();var right=forge.getItem(8).copy();forge.setItem(7,right);forge.setItem(8,middle);
            h.assertTrue(!recipe.matches(input,h.getLevel()),"Physical node positions were ignored");forge.setItem(7,middle);forge.setItem(8,right);
            forge.setItem(9,ItemStack.EMPTY);h.assertTrue(!forge.start(p),"Recipe node core replaced power core requirement");
            forge.setItem(9,new ItemStack(ModItems.TEST_SPELL_CORE.get()));h.assertTrue(forge.start(p),"Production recipe cannot start");
            for(int i=0;i<1200;i++)forge.tick();
            h.assertTrue(forge.output().is(output) && forge.output().getCount()==1 && forge.getItem(9).is(ModItems.TEST_SPELL_CORE.get()),"Wrong result or consumed power core");
            for(int i=0;i<9;i++)h.assertTrue(forge.getItem(i).isEmpty(),"Recipe material was not consumed");
            forge.collect(p);
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void legacyNodeOrderMigratesOnce(GameTestHelper h){
        var center=h.absolutePos(new BlockPos(5,2,5));
        ForgeLayout.assemble(h.getLevel(),center,Direction.SOUTH);
        var forge=(AdvancedForgeBlockEntity)h.getLevel().getBlockEntity(center);
        var registries=h.getLevel().registryAccess();
        var old=forge.saveWithFullMetadata(registries);old.remove("NodeOrderVersion");
        var items=NonNullList.withSize(10,ItemStack.EMPTY);
        items.set(6,new ItemStack(Items.GOLD_INGOT));items.set(7,new ItemStack(Items.IRON_INGOT));items.set(8,new ItemStack(Items.DIAMOND));
        net.minecraft.world.ContainerHelper.saveAllItems(old,items,registries);
        var reserved=new net.minecraft.nbt.CompoundTag();net.minecraft.world.ContainerHelper.saveAllItems(reserved,items,registries);
        old.put("Reserved",reserved.copy());old.put("Remainders",reserved.copy());
        forge.loadWithComponents(old,registries);
        for(int pass=0;pass<2;pass++){
            h.assertTrue(forge.getItem(6).is(Items.DIAMOND) && forge.getItem(7).is(Items.GOLD_INGOT) && forge.getItem(8).is(Items.IRON_INGOT),"Legacy node positions moved or migrated twice");
            var saved=forge.saveWithFullMetadata(registries);
            for(String key:List.of("Reserved","Remainders")){
                var restored=NonNullList.withSize(9,ItemStack.EMPTY);net.minecraft.world.ContainerHelper.loadAllItems(saved.getCompound(key),restored,registries);
                h.assertTrue(restored.get(6).is(Items.DIAMOND) && restored.get(7).is(Items.GOLD_INGOT) && restored.get(8).is(Items.IRON_INGOT),"Working inventory not migrated");
            }
            forge.loadWithComponents(saved,registries);
        }
        h.assertTrue(ForgeLayout.NODES[0][0]==1 && ForgeLayout.NODES[1][1]==1 && ForgeLayout.NODES[2][0]==-1,"Nodes must follow entrance left/middle/right");
        h.succeed();
    }
}
