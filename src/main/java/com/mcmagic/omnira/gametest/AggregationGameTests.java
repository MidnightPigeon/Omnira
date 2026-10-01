package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.aggregation.*;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_aggregation")
@PrefixGameTestTemplate(false)
public final class AggregationGameTests {
    @GameTest(template="spell_arena") public static void visualLayoutAndAnimation(GameTestHelper h){
        h.assertTrue(AggregationLayout.COLORS[0]==AggregationLayout.COLORS[1],"Shape nodes have different colors");
        var center=new BlockPos(4,3,4);
        for(var facing:Direction.Plane.HORIZONTAL){
            for(int part=0;part<9;part++)h.assertTrue(
                    com.mcmagic.omnira.aggregation.AggregationCollision.shape(part,facing).isEmpty()==(part==4),
                    "Frame collision missing or central aperture blocked");
            double bob=AggregationLayout.bob(60,true);
            var hit=net.minecraft.world.phys.Vec3.atCenterOf(center).add(
                    facing.getStepX()*AggregationLayout.BUTTON_Z,AggregationLayout.BUTTON_Y+bob,
                    facing.getStepZ()*AggregationLayout.BUTTON_Z);
            h.assertTrue(AggregationLayout.button(AggregationLayout.local(hit,center,facing),bob),"Visible button does not match hit region");
            h.assertTrue(!AggregationLayout.button(new net.minecraft.world.phys.Vec3(0,-1.38,0),bob),"Old floor button remains active");
        }
        for(int slot=0;slot<8;slot++){
            var released=AggregationLayout.ingredient(slot*.085+.02,slot);
            var node=AggregationLayout.NODES[slot];
            h.assertTrue(Math.abs(released.x*node[1]-released.y*node[0])<.00001,"Ingredient does not leave toward ring center");
            h.assertTrue(Math.hypot(released.x,released.y)<Math.hypot(node[0],node[1]),"Ingredient moves outward");
            net.minecraft.world.phys.Vec3 last=null;
            for(int step=0;step<=1000;step++){
                var at=AggregationLayout.ingredient(step/1000.0,slot);if(at==null)continue;
                h.assertTrue(Double.isFinite(at.x)&&Double.isFinite(at.y)&&Double.isFinite(at.z),"Non-finite synthesis position");
                if(last!=null)h.assertTrue(at.distanceTo(last)<.06,"Synthesis path jumps between nodes");
                last=at;
            }
            h.assertTrue(Math.hypot(last.x,last.y)<.00001,"Ingredient failed to reach core");
        }
        for(int i=0;i<10;i++){
            var a=AggregationLayout.decoration(0,i);var b=AggregationLayout.decoration(40,i);
            h.assertTrue(a.distanceTo(b)>.05&&Math.hypot(a.x,a.y)>1,"Ring decoration is static or detached");
        }
        h.succeed();
    }
    private static void build(GameTestHelper h,BlockPos p,Direction facing,boolean alternate){
        for(int i=0;i<9;i++){
            var b=i==4?ModBlocks.CRYSTAL_PROCESSING_TABLE.get():i==1||i==7?(alternate?AggregationContent.SPATIAL_BLOCK.get():DreamContent.EXCITED_SPATIAL_CRYSTAL.get()):
                    i==3||i==5?(alternate?TimeNatureContent.LIVING_SILT.get():DreamContent.LIVING_TEMPORAL_SILT.get()):ModBlocks.INFUSED_CRYSTAL_CASING.get();
            h.getLevel().setBlockAndUpdate(AggregationLayout.part(p,facing,i),b.defaultBlockState());
        }
    }
    @GameTest(template="spell_arena") public static void assemblyAndDestruction(GameTestHelper h){
        var p=h.absolutePos(new BlockPos(5,3,5));var player=AdvancedForgeGameTests.player(h,p);
        for(var facing:Direction.Plane.HORIZONTAL){
            player.setYRot(facing.getOpposite().toYRot());build(h,p,facing,facing.getAxis()==Direction.Axis.X);
            var material=new ItemStack(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get(),2);
            var input=(com.mcmagic.omnira.block.entity.CrystalProcessingTableBlockEntity)h.getLevel().getBlockEntity(p);
            input.setItem(0,new ItemStack(Items.DIAMOND));
            h.assertTrue(!AggregationStructure.activate(h.getLevel(),p,player,material)&&material.getCount()==2,"Occupied source table was erased");
            input.clearContent();
            h.assertTrue(AggregationStructure.activate(h.getLevel(),p,player,material)&&material.getCount()==1,"Valid alternate assembly failed");
            h.assertTrue(AggregationStructure.intact(h.getLevel(),p,h.getLevel().getBlockState(p)),"Incomplete assembly");
            for(int i=0;i<9;i++){
                var at=AggregationLayout.part(p,facing,i);var state=h.getLevel().getBlockState(at);
                h.assertTrue(state.getDestroySpeed(h.getLevel(),at)<0,"Survival mining allowed");
            }
            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            var corner=AggregationLayout.part(p,facing,8);var state=h.getLevel().getBlockState(corner);state.getBlock().playerWillDestroy(h.getLevel(),corner,state,player);
            for(int i=0;i<9;i++)h.assertTrue(h.getLevel().getBlockState(AggregationLayout.part(p,facing,i)).isAir(),"Destruction left fragments");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void bottlePreservesInventory(GameTestHelper h){
        var p=h.absolutePos(new BlockPos(5,3,5));var player=AdvancedForgeGameTests.player(h,p);
        h.assertTrue(AggregationStructure.assemble(h.getLevel(),p,Direction.NORTH),"Assembly failed");
        var machine=(AggregationRingBlockEntity)h.getLevel().getBlockEntity(p);
        machine.setItem(10,new ItemStack(ModItems.TEST_SPELL_CORE.get()));machine.setItem(8,new ItemStack(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get(),12));
        var bottle=new ItemStack(ModItems.POCKET_MAGIC_BOTTLE.get());
        h.assertTrue(com.mcmagic.omnira.item.bottle.MultiblockBottleCapture.capture(player,p.above(),bottle),"Capture failed");
        for(int i=0;i<9;i++)h.assertTrue(h.getLevel().getBlockState(AggregationLayout.part(p,Direction.NORTH,i)).isAir(),"Capture left fragments");
        var projectile=new com.mcmagic.omnira.item.bottle.ThrownPocketBottle(ModEntityTypes.POCKET_BOTTLE.get(),h.getLevel());
        projectile.setOwner(player);projectile.setPos(player.position());h.getLevel().addFreshEntity(projectile);
        var storage=com.mcmagic.omnira.item.bottle.BottleStorage.get(h.getLevel());
        h.assertTrue(storage.claim(bottle.get(ModDataComponents.BOTTLE_CAPTURE),projectile.getUUID()),"Bottle claim failed");
        h.assertTrue(com.mcmagic.omnira.item.bottle.PocketBottleItem.release(h.getLevel(),bottle,net.minecraft.world.phys.Vec3.atBottomCenterOf(p),projectile.getUUID()),"Release failed");
        machine=(AggregationRingBlockEntity)h.getLevel().getBlockEntity(p);
        h.assertTrue(machine!=null&&machine.getItem(8).getCount()==12&&machine.powered(),"Bottle lost inventory or core");
        h.assertTrue(AggregationStructure.intact(h.getLevel(),p,machine.getBlockState()),"Release incomplete");projectile.discard();h.succeed();
    }
    @GameTest(template="spell_arena") public static void advancedSpellAndCosts(GameTestHelper h){
        var p=h.absolutePos(new BlockPos(5,3,5));AggregationStructure.assemble(h.getLevel(),p,Direction.NORTH);
        var machine=(AggregationRingBlockEntity)h.getLevel().getBlockEntity(p);
        machine.setItem(0,new ItemStack(ModItems.EARTH_MICROCORE.get()));
        machine.setItem(2,new ItemStack(ModItems.SHARP_BREATH.get()));machine.setItem(3,new ItemStack(ModItems.HEALING_DEW.get()));machine.setItem(4,new ItemStack(ModItems.CONSTRUCTION_MATRIX.get()));
        machine.setItem(5,new ItemStack(Items.IRON_BLOCK));machine.setItem(6,new ItemStack(Items.CLOCK));machine.setItem(7,new ItemStack(ModItems.SPIRITUAL_CRYSTAL.get()));
        machine.setItem(8,new ItemStack(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get()));machine.setItem(9,new ItemStack(ModItems.SPELL_INK.get()));machine.setItem(10,new ItemStack(ModItems.TEST_SPELL_CORE.get()));
        h.assertTrue(machine.recipeReady()&&machine.manaCost()==140,"Optional shape or crafting cost incorrect");
        var crystal=com.mcmagic.omnira.menu.CrystalProcessingTableMenu.previewCrystal(machine,5,8,2,5,AggregationContent.CRYSTAL.get());
        h.assertTrue(com.mcmagic.omnira.menu.CrystalGridMenu.isCrystal(crystal),"High-tier crystal rejected by lattice");
        h.assertTrue(crystal.get(ModDataComponents.SPELL_PATTERN).shapeElement()==null,"Optional shape lost");
        h.assertTrue(com.mcmagic.omnira.menu.CrystalProcessingTableMenu.composeEffects(machine,5,8,2,5).size()==3,"Third element ignored");
        machine.setItem(4,new ItemStack(ModItems.SHARP_BREATH.get()));
        h.assertTrue(com.mcmagic.omnira.menu.CrystalProcessingTableMenu.composeEffects(machine,5,8,2,5).size()==2,"Duplicate elements applied twice");
        h.succeed();
    }
}
