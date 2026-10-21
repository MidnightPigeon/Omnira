package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.codex.*;
import com.mcmagic.omnira.codex.edit.*;
import com.mcmagic.omnira.item.MultiblockPlacementItem;
import com.mcmagic.omnira.myriad.MiniatureWorldContent;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("omnira_codex_materials") @PrefixGameTestTemplate(false)
public final class CodexMaterialGameTests {
    private static List<ItemStack> filled(){
        var chest=new ItemStack(Items.CHEST);chest.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND))));
        var machine=new ItemStack(ModItems.MANA_ENGINE.get());
        var data=new CompoundTag();var items=new ListTag();var item=new CompoundTag();item.putString("id","minecraft:diamond");item.putInt("count",1);items.add(item);data.put("Items",items);
        machine.set(DataComponents.BLOCK_ENTITY_DATA,CustomData.of(data));
        var liquid=new ItemStack(ModItems.LIQUID_CRYSTAL_BALL.get());
        var tank=new CompoundTag();tank.putString("id","minecraft:water");tank.putInt("amount",1000);
        var fluidData=new CompoundTag();fluidData.put("Fluid",tank);liquid.set(DataComponents.BLOCK_ENTITY_DATA,CustomData.of(fluidData));
        var upgraded=new ItemStack(ModItems.CRYSTAL_BALL.get());
        var upgradeData=new CompoundTag();upgradeData.put("Upgrades",data.copy());upgraded.set(DataComponents.BLOCK_ENTITY_DATA,CustomData.of(upgradeData));
        var frame=new ItemStack(MiniatureWorldContent.ITEM.get());frame.set(DataComponents.BLOCK_ENTITY_DATA,CustomData.of(new CompoundTag()));
        var bundle=new ItemStack(Items.BUNDLE);bundle.set(DataComponents.BUNDLE_CONTENTS,new BundleContents(List.of(new ItemStack(Items.DIAMOND))));
        var crossbow=new ItemStack(Items.CROSSBOW);crossbow.set(DataComponents.CHARGED_PROJECTILES,ChargedProjectiles.of(new ItemStack(Items.ARROW)));
        return List.of(chest,machine,liquid,upgraded,frame,bundle,crossbow,new ItemStack(Items.WATER_BUCKET),new ItemStack(Items.MILK_BUCKET));
    }
    private static BlockPos center(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(5,5,5));
        for(int x=(pos.getX()-4)>>4;x<=(pos.getX()+4)>>4;x++)for(int z=(pos.getZ()-4)>>4;z<=(pos.getZ()+4)>>4;z++)h.getLevel().getChunk(x,z);
        return pos;
    }
    @GameTest(template="spell_arena") public static void contentsAreRejectedButEmptyAndCosmeticDataAreAllowed(GameTestHelper h){
        for(var stack:filled())h.assertTrue(!CodexMaterials.allowed(stack),"Filled specimen accepted: "+stack);
        for(var item:List.of(Items.CHEST,Items.BUCKET,Items.BUNDLE,Items.CROSSBOW,MiniatureWorldContent.ITEM.get(),ModItems.MANA_ENGINE.get())){
            var stack=new ItemStack(item);stack.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Named specimen"));
            h.assertTrue(CodexMaterials.allowed(stack),"Empty or merely named item rejected: "+stack);
        }
        var emptyMachine=new ItemStack(ModItems.MANA_ENGINE.get());var data=new CompoundTag();data.put("Items",new ListTag());data.putInt("Facing",2);
        emptyMachine.set(DataComponents.BLOCK_ENTITY_DATA,CustomData.of(data));h.assertTrue(CodexMaterials.allowed(emptyMachine),"Empty machine state mistaken for contents");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void filledMaterialsCannotEnterByClickShiftSwapOrDirectPlan(GameTestHelper h){
        var pos=new BlockPos(1,4,1);h.setBlock(pos,MyriadCodexContent.BLOCK.get());
        var be=MyriadCodexEntity.from(h.getBlockEntity(pos));var player=AdvancedForgeGameTests.player(h,be.getBlockPos());
        var menu=new MyriadCodexMenu(7,player.getInventory(),be);player.containerMenu=menu;
        try{
            for(var stack:filled()){
                for(int slot=0;slot<2;slot++){
                    menu.setCarried(stack.copy());menu.clicked(slot,0,ClickType.PICKUP,player);
                    h.assertTrue(ItemStack.matches(menu.getCarried(),stack)&&be.materials.getStackInSlot(slot).isEmpty(),"Filled click bypass or item loss");
                    h.assertTrue(ItemStack.matches(be.materials.insertItem(slot,stack.copy(),false),stack),"Filled handler insertion bypass");
                }
                menu.setCarried(ItemStack.EMPTY);player.getInventory().setItem(9,stack.copy());menu.quickMoveStack(player,4);
                h.assertTrue(ItemStack.matches(player.getInventory().getItem(9),stack)&&be.materials.getStackInSlot(0).isEmpty(),"Filled shift bypass or loss");
                player.getInventory().setItem(0,stack.copy());menu.clicked(1,0,ClickType.SWAP,player);
                h.assertTrue(ItemStack.matches(player.getInventory().getItem(0),stack)&&be.materials.getStackInSlot(1).isEmpty(),"Filled hotbar swap bypass or loss");
                var result=CodexEditEngine.begin(h.getLevel(),center(h),7,Operation.MATTER,Direction.Axis.Y,false,new ItemStack(Items.STONE),stack,null,null);
                h.assertTrue(!result.accepted(),"Direct plan bypassed filled target rejection");
                result=CodexEditEngine.begin(h.getLevel(),center(h),7,Operation.MATTER,Direction.Axis.Y,false,stack,new ItemStack(Items.DIRT),null,null);
                h.assertTrue(!result.accepted(),"Direct plan bypassed filled source rejection");
                be.materials.setStackInSlot(0,new ItemStack(Items.STONE));be.materials.setStackInSlot(1,stack.copy());
                h.assertTrue(CodexExecution.start(be,player).equals("contents")&&be.job==null&&ItemStack.matches(be.materials.getStackInSlot(1),stack),"Legacy/direct-set formula consumed filled specimen");
                be.materials.setStackInSlot(0,ItemStack.EMPTY);be.materials.setStackInSlot(1,ItemStack.EMPTY);player.getInventory().clearContent();
            }
        }finally{menu.setCarried(ItemStack.EMPTY);player.containerMenu=player.inventoryMenu;player.getInventory().clearContent();}
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void multiblockOutputsStayItemsForBlocksAndDropsAfterReload(GameTestHelper h){
        var level=h.getLevel();var pos=center(h);var bounds=new AABB(pos).inflate(4);int checked=0;
        for(var item:BuiltInRegistries.ITEM){
            if(!(item instanceof MultiblockPlacementItem))continue;checked++;
            level.setBlock(pos,Blocks.STONE.defaultBlockState(),2);
            var result=CodexEditEngine.begin(level,pos,7,Operation.MATTER,Direction.Axis.Y,false,new ItemStack(Items.STONE),new ItemStack(item),null,null);
            h.assertTrue(result.accepted(),"Multiblock target was refused");
            var job=EditJob.load(result.job().save(level.registryAccess()),level.registryAccess());finish(h,job);
            var drops=level.getEntitiesOfClass(ItemEntity.class,bounds,e->e.getItem().is(item));
            h.assertTrue(level.getBlockState(pos).isAir()&&drops.stream().mapToInt(e->e.getItem().getCount()).sum()==1,"Block conversion placed an incomplete multiblock or lost output");
            drops.forEach(Entity::discard);
            var source=new ItemEntity(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,new ItemStack(Items.APPLE,3),0,0,0);level.addFreshEntity(source);
            result=CodexEditEngine.begin(level,pos,7,Operation.MATTER,Direction.Axis.Y,false,new ItemStack(Items.APPLE),new ItemStack(item),null,null);
            h.assertTrue(result.accepted(),"Item-to-multiblock plan refused");
            result.job().advance(level,1);job=EditJob.load(result.job().save(level.registryAccess()),level.registryAccess());finish(h,job);
            drops=level.getEntitiesOfClass(ItemEntity.class,bounds,e->e.getItem().is(item));
            h.assertTrue(level.getBlockState(pos).isAir()&&drops.stream().mapToInt(e->e.getItem().getCount()).sum()==3,"Item conversion treated multiblock as a block or lost split output");
            drops.forEach(Entity::discard);
        }
        h.assertTrue(checked>=7,"Multiblock placement coverage incomplete");h.succeed();
    }
    @GameTest(template="spell_arena") public static void savedOutputCannotReintroduceContents(GameTestHelper h){
        var pos=center(h);h.getLevel().setBlock(pos,Blocks.STONE.defaultBlockState(),2);
        var result=CodexEditEngine.begin(h.getLevel(),pos,7,Operation.MATTER,Direction.Axis.Y,false,new ItemStack(Items.STONE),new ItemStack(Items.APPLE),null,null);
        h.assertTrue(result.accepted(),"Unfilled baseline failed");
        var saved=result.job().save(h.getLevel().registryAccess());
        saved.getList("Changes",Tag.TAG_COMPOUND).getCompound(0).put("Item",filled().getFirst().save(h.getLevel().registryAccess()));
        boolean rejected=false;try{EditJob.load(saved,h.getLevel().registryAccess());}catch(IllegalArgumentException expected){rejected=true;}
        h.assertTrue(rejected,"Saved output containing inventory bypassed validation");h.succeed();
    }
    @GameTest(template="spell_arena") public static void emptyMatterTargetStartsAndClearsOnlyMatches(GameTestHelper h){
        var level=h.getLevel();var center=center(h);var codexPos=new BlockPos(1,4,1);h.setBlock(codexPos,MyriadCodexContent.BLOCK.get());
        var be=MyriadCodexEntity.from(h.getBlockEntity(codexPos));be.potential=8192;
        var player=AdvancedForgeGameTests.player(h,be.getBlockPos());
        be.takeHeart(player);h.assertTrue(be.placeHeart(player,center),"Air replacement marker could not be placed");
        level.setBlock(center.east(),Blocks.STONE.defaultBlockState(),2);level.setBlock(center.west(),Blocks.DIRT.defaultBlockState(),2);
        level.setBlock(center.east(4),Blocks.STONE.defaultBlockState(),2);
        var stone=new ItemEntity(level,center.getX()+.5,center.getY()+2.5,center.getZ()+.5,new ItemStack(Items.STONE,2),0,0,0);level.addFreshEntity(stone);
        be.materials.setStackInSlot(0,new ItemStack(Items.STONE));
        h.assertTrue(be.start(player).isEmpty()&&be.materials.getStackInSlot(0).isEmpty()&&be.materials.getStackInSlot(1).isEmpty()&&be.heartPos==null,"Empty right slot did not submit or consume only the source");
        var edits=CodexEdits.get(level);int ticks=0;
        while(edits.active(be.job)&&ticks++<100)CodexEdits.tick(new net.neoforged.neoforge.event.tick.LevelTickEvent.Post(()->true,level));
        h.assertTrue(!edits.active(be.job)&&level.getBlockState(center.east()).isAir()&&!stone.isAlive(),"Air target did not clear matching blocks/drops");
        h.assertTrue(level.getBlockState(center.west()).is(Blocks.DIRT)&&level.getBlockState(center.east(4)).is(Blocks.STONE),"Air replacement changed unmatched or out-of-range blocks");
        edits.forget(be.job);h.succeed();
    }
    @GameTest(template="spell_arena") public static void emptyMatterTargetSurvivesPlanReload(GameTestHelper h){
        var pos=center(h);var level=h.getLevel();level.setBlock(pos,Blocks.STONE.defaultBlockState(),2);
        var result=CodexEditEngine.begin(level,pos,7,Operation.MATTER,Direction.Axis.Y,false,new ItemStack(Items.STONE),ItemStack.EMPTY,null,null);
        h.assertTrue(result.accepted(),"Air target plan refused");finish(h,EditJob.load(result.job().save(level.registryAccess()),level.registryAccess()));
        h.assertTrue(level.getBlockState(pos).isAir()&&level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(3)).isEmpty(),"Reloaded air target placed or dropped an item");
        result=CodexEditEngine.begin(level,pos,7,Operation.MATTER,Direction.Axis.Y,false,ItemStack.EMPTY,ItemStack.EMPTY,null,null);
        h.assertTrue(!result.accepted(),"Air replacement accepted an empty source");h.succeed();
    }
    private static void finish(GameTestHelper h,EditJob job){int ticks=0;while(job.active()&&ticks++<1000)job.advance(h.getLevel(),256);h.assertTrue(job.status()==EditJob.Status.COMPLETE,"Output plan failed: "+job.error());}
    private CodexMaterialGameTests(){}
}
