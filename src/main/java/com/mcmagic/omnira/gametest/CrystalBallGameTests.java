package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.block.entity.CrystalBallBlockEntity;
import com.mcmagic.omnira.menu.CrystalBallMenu;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_crystal_ball")
@PrefixGameTestTemplate(false)
public final class CrystalBallGameTests {
    @GameTest(template="spell_arena")
    public static void itemFormDoesNotCarryInventory(GameTestHelper h) {
        var ball=ball(h);
        ball.setItem(0,new ItemStack(Items.DIAMOND,12));
        var item=new ItemStack(ModItems.CRYSTAL_BALL.get());
        ball.saveToItem(item,h.getLevel().registryAccess());
        h.assertTrue(!item.has(DataComponents.CONTAINER) && !item.has(DataComponents.CONTAINER_LOOT),"Portable ball contains inventory");
        h.assertTrue(ball.getItem(0).getCount()==12,"Export removed placed inventory");
        ball.setLootTable(ResourceKey.create(Registries.LOOT_TABLE,ResourceLocation.fromNamespaceAndPath("omnira","chests/crystal_workshop")));
        var unopened=new ItemStack(ModItems.CRYSTAL_BALL.get());
        ball.saveToItem(unopened,h.getLevel().registryAccess());
        h.assertTrue(!unopened.has(DataComponents.CONTAINER_LOOT),"Portable ball carries pending loot");
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void displaySyncDoesNotUnpackLoot(GameTestHelper h) {
        var ball=ball(h);
        var table=ResourceKey.create(Registries.LOOT_TABLE,ResourceLocation.fromNamespaceAndPath("omnira","chests/crystal_workshop"));
        ball.setLootTable(table);ball.setLootTableSeed(42);
        for(int i=0;i<12;i++)h.assertTrue(ball.displayItem(i).isEmpty(),"Pending loot exposed through display");
        var hidden=ball.getUpdateTag(h.getLevel().registryAccess());
        h.assertTrue(hidden.getBoolean("PendingLoot"),"Missing fog marker");
        var unopened=new CrystalBallBlockEntity(ball.getBlockPos(),ball.getBlockState());
        unopened.loadWithComponents(hidden,h.getLevel().registryAccess());
        h.assertTrue(unopened.hasPendingLoot() && unopened.getLootTable()==null,"Fog should not require sending the loot table");
        h.assertTrue(!hidden.contains("Items") && !hidden.contains("LootTable") && table.equals(ball.getLootTable()),"Display sync generated or leaked loot");
        ball.setLootTable(null);
        ball.setItem(7,new ItemStack(Items.DIAMOND,32));
        unopened.loadWithComponents(ball.getUpdateTag(h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(!unopened.hasPendingLoot(),"Opened ball kept fog marker");
        var clientCopy=new CrystalBallBlockEntity(ball.getBlockPos(),ball.getBlockState());
        clientCopy.loadWithComponents(ball.getUpdateTag(h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(clientCopy.displayItem(7).is(Items.DIAMOND) && clientCopy.displayItem(7).getCount()==32,"Display lost stored item or count");
        h.assertTrue(ball.getUpdatePacket()!=null,"Missing live block entity update packet");
        ball.removeItem(7,32);
        clientCopy.loadWithComponents(ball.getUpdateTag(h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(clientCopy.displayItem(7).isEmpty(),"Empty slot left a stale display");
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void unopenedLootResistsInventoryReads(GameTestHelper h) {
        var ball=ball(h);
        var table=ResourceKey.create(Registries.LOOT_TABLE,ResourceLocation.fromNamespaceAndPath("omnira","chests/crystal_workshop"));
        ball.setLootTable(table);ball.setLootTableSeed(42);
        h.assertTrue(ball.getItem(0).isEmpty() && ball.removeItem(0,1).isEmpty(),"Container read unpacked loot");
        h.assertTrue(ball.itemHandler.insertItem(0,new ItemStack(Items.DIAMOND),false).is(Items.DIAMOND),
                "Automation inserted into unopened loot ball");
        h.assertTrue(ball.itemHandler.extractItem(0,1,false).isEmpty(),"Automation extracted unopened loot");
        h.assertTrue(ball.isEmpty(),"Checking whether unopened loot is empty unpacked it");
        h.assertTrue(AbstractContainerMenu.getRedstoneSignalFromBlockEntity(ball)==0,"Comparator unpacked loot");
        h.assertTrue(table.equals(ball.getLootTable()) && ball.hasPendingLoot(),"Unopened loot lost its fog state");
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        h.assertTrue(ball.createMenu(1,player.getInventory(),player)!=null && !ball.hasPendingLoot() && !ball.isEmpty(),
                "Opening did not reveal the loot");
        h.succeed();
    }
    private static CrystalBallBlockEntity ball(GameTestHelper h) {
        var p=h.absolutePos(new BlockPos(5,2,5));
        h.getLevel().setBlockAndUpdate(p,ModBlocks.CRYSTAL_BALL.get().defaultBlockState());
        return (CrystalBallBlockEntity)h.getLevel().getBlockEntity(p);
    }
    private static void land(CrystalBallBlockEntity ball,net.minecraft.world.entity.Entity entity,float distance) {
        ball.getBlockState().getBlock().fallOn(ball.getLevel(),ball.getBlockState(),ball.getBlockPos(),entity,distance);
    }
    @GameTest(template="spell_arena")
    public static void nineLandingsShatterWithoutBallDrop(GameTestHelper h) {
        var ball=ball(h);var pos=ball.getBlockPos();var player=h.makeMockPlayer(GameType.SURVIVAL);
        ball.setItem(0,new ItemStack(Items.DIAMOND,37));ball.setItem(11,new ItemStack(Items.APPLE,12));
        land(ball,player,0);
        var dropped=new ItemEntity(h.getLevel(),pos.getX(),pos.getY()+2,pos.getZ(),new ItemStack(Items.STONE));
        land(ball,dropped,1);
        h.assertTrue(ball.impactCount()==0,"Standing or dropped items damaged the globe");
        for(int i=1;i<=8;i++) {
            land(ball,player,1);
            h.assertTrue(ball.impactCount()==i && ball.crackStage()==i/3,"Incorrect three-jump crack progression");
        }
        land(ball,player,1);
        h.assertTrue(h.getLevel().getBlockState(pos).isAir(),"Ninth landing did not shatter globe");
        var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2));
        h.assertTrue(drops.stream().filter(e->e.getItem().is(Items.DIAMOND)).mapToInt(e->e.getItem().getCount()).sum()==37,"Stored diamonds lost or duplicated");
        h.assertTrue(drops.stream().filter(e->e.getItem().is(Items.APPLE)).mapToInt(e->e.getItem().getCount()).sum()==12,"Last inventory slot lost");
        h.assertTrue(drops.stream().filter(e->e.getItem().is(Items.GLASS)).mapToInt(e->e.getItem().getCount()).sum()==1,"Shattering must yield one glass block");
        h.assertTrue(drops.stream().noneMatch(e->e.getItem().is(ModItems.CRYSTAL_BALL.get())),"Shattered globe dropped itself");
        h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=430)
    public static void cracksPersistSyncAndRecover(GameTestHelper h) {
        var ball=ball(h);var player=h.makeMockPlayer(GameType.SURVIVAL);
        for(int i=0;i<6;i++) land(ball,player,1);
        var copy=new CrystalBallBlockEntity(ball.getBlockPos(),ball.getBlockState());
        copy.loadWithComponents(ball.saveWithFullMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(copy.impactCount()==6 && copy.crackStage()==2,"Cracks lost on save");
        copy.loadWithComponents(ball.getUpdateTag(h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(copy.crackStage()==2,"Cracks not synced to client");
        h.runAfterDelay(150,()->land(ball,player,1));
        h.runAfterDelay(205,()->h.assertTrue(ball.impactCount()==7,"Old scheduled tick healed a freshly struck globe"));
        h.runAfterDelay(355,()->{
            h.assertTrue(ball.impactCount()==6,"Quiet globe did not recover one impact");h.succeed();
        });
    }
    @GameTest(template="spell_arena",timeoutTicks=80)
    public static void livingMobLandingCountsOnce(GameTestHelper h) {
        var ball=ball(h);
        var cow=h.spawn(net.minecraft.world.entity.EntityType.COW,new BlockPos(5,5,5));cow.setNoAi(true);
        // NoAI mobs do not run normal travel; exercise the real collision/landing path directly.
        for(int i=0;i<40;i++) cow.move(net.minecraft.world.entity.MoverType.SELF,new net.minecraft.world.phys.Vec3(0,-.15,0));
        h.assertTrue(ball.impactCount()==1,"Mob landing did not count exactly once: "+ball.impactCount());h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void shatteringUnopenedLootReleasesContents(GameTestHelper h) {
        var ball=ball(h);var pos=ball.getBlockPos();
        ball.setLootTable(ResourceKey.create(Registries.LOOT_TABLE,ResourceLocation.fromNamespaceAndPath("omnira","chests/wizard_tower_research")));
        ball.setLootTableSeed(42);
        var cow=net.minecraft.world.entity.EntityType.COW.create(h.getLevel());
        for(int i=0;i<9;i++) land(ball,cow,1);
        var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2));
        int crystals=drops.stream().filter(e->e.getItem().is(ModItems.SPIRITUAL_CRYSTAL.get())).mapToInt(e->e.getItem().getCount()).sum();
        h.assertTrue(crystals>=16 && crystals<=24,"Pending loot lost or generated twice");
        h.assertTrue(drops.stream().noneMatch(e->e.getItem().is(ModItems.CRYSTAL_BALL.get())),"Unopened globe dropped itself");
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void savesAllSlots(GameTestHelper h) {
        var ball=ball(h);
        for(int i=0;i<12;i++) {
            var item=new ItemStack(Items.DIAMOND,i+1);item.set(DataComponents.CUSTOM_NAME,Component.literal("slot-"+i));ball.setItem(i,item);
        }
        var restored=new CrystalBallBlockEntity(ball.getBlockPos(),ball.getBlockState());
        restored.loadWithComponents(ball.saveWithFullMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(restored.getContainerSize()==12,"Wrong capacity");
        for(int i=0;i<12;i++) h.assertTrue(restored.getItem(i).getCount()==i+1&&ItemStack.isSameItemSameComponents(restored.getItem(i),ball.getItem(i)),"Lost slot "+i);
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void lootTableSurvivesTemplateSave(GameTestHelper h) {
        var ball=ball(h);
        for(String name:new String[]{"apprentice_hut","crystal_workshop","wizard_tower_materials","wizard_tower_research"}) {
            var table=ResourceKey.create(Registries.LOOT_TABLE,ResourceLocation.fromNamespaceAndPath("omnira","chests/"+name));
            ball.clearContent();ball.setLootTable(table);ball.setLootTableSeed(42);
            var tag=ball.saveWithFullMetadata(h.getLevel().registryAccess());
            h.assertTrue(tag.getString("LootTable").equals(table.location().toString())&&!tag.contains("Items"),"Pending loot unpacked during save");
            var restored=new CrystalBallBlockEntity(ball.getBlockPos(),ball.getBlockState());restored.setLevel(h.getLevel());
            restored.loadWithComponents(tag,h.getLevel().registryAccess());
            h.assertTrue(table.equals(restored.getLootTable())&&restored.getLootTableSeed()==42,"Template lost loot metadata");
            restored.unpackLootTable(null);
            h.assertTrue(restored.getLootTable()==null&&!restored.isEmpty(),"Loot table did not generate: "+name);
            var generated=restored.saveWithFullMetadata(h.getLevel().registryAccess());
            restored.unpackLootTable(null);
            h.assertTrue(generated.equals(restored.saveWithFullMetadata(h.getLevel().registryAccess())),"Loot rerolled");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void shiftTransferAndLayout(GameTestHelper h) {
        var ball=ball(h);var player=h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(ball.getBlockPos().getX()+.5,ball.getBlockPos().getY(),ball.getBlockPos().getZ()+1.5);
        var menu=new CrystalBallMenu(1,player.getInventory(),ball);
        h.assertTrue(menu.slots.size()==51&&menu.stillValid(player),"Invalid menu slots or access");
        int index=0;
        for(int row=0;row<4;row++)for(int col=0,n=row==0||row==3?2:4;col<n;col++) {
            var slot=menu.slots.get(index++);
            h.assertTrue(slot.x==88-n*9+18*col&&slot.y==29+row*18,"Incorrect 2/4/4/2 layout");
            for(int dx:new int[]{0,16})for(int dy:new int[]{0,16})
                h.assertTrue(Math.hypot(slot.x+dx-88,slot.y+dy-64)<44,"Slot crosses circle outline");
        }
        player.getInventory().setItem(9,new ItemStack(Items.DIAMOND,40));
        menu.quickMoveStack(player,menu.playerStart);
        h.assertTrue(player.getInventory().getItem(9).isEmpty()&&ball.getItem(0).getCount()==40,"Transfer into ball failed");
        menu.quickMoveStack(player,0);
        h.assertTrue(ball.isEmpty()&&player.getInventory().countItem(Items.DIAMOND)==40,"Transfer out duplicated or lost items");
        for(int i=0;i<12;i++)ball.setItem(i,new ItemStack(Items.STONE,64));
        player.getInventory().setItem(9,new ItemStack(Items.DIAMOND,12));
        h.assertTrue(menu.quickMoveStack(player,menu.playerStart).isEmpty()&&player.getInventory().getItem(9).getCount()==12,"Full container consumed input");
        player.setPos(player.position().add(20,0,0));h.assertTrue(!menu.stillValid(player),"Remote container stays open");
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void automationAndDestruction(GameTestHelper h) {
        var ball=ball(h);var p=ball.getBlockPos();
        var handler=h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,p,Direction.UP);
        h.assertTrue(handler!=null&&handler.getSlots()==12,"Automation capability missing");
        h.assertTrue(handler.insertItem(11,new ItemStack(Items.DIAMOND,32),false).isEmpty(),"Automation insertion failed");
        h.assertTrue(handler.extractItem(11,5,true).getCount()==5&&ball.getItem(11).getCount()==32,"Simulation modified inventory");
        h.assertTrue(handler.extractItem(11,5,false).getCount()==5&&ball.getItem(11).getCount()==27,"Automation extraction failed");
        h.assertTrue(AbstractContainerMenu.getRedstoneSignalFromContainer(ball)>0,"Comparator remains empty");
        h.getLevel().destroyBlock(p,false);
        int count=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(p).inflate(2)).stream().filter(e->e.getItem().is(Items.DIAMOND)).mapToInt(e->e.getItem().getCount()).sum();
        h.assertTrue(count==27,"Destroyed contents lost or duplicated: "+count);
        h.assertTrue(h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,p,Direction.UP)==null,"Removed capability still exposed");
        var state=ModBlocks.CRYSTAL_BALL.get().defaultBlockState();
        h.assertTrue(new ItemStack(Items.IRON_PICKAXE).isCorrectToolForDrops(state)&&!new ItemStack(Items.STONE_PICKAXE).isCorrectToolForDrops(state),"Incorrect mining tier");
        h.succeed();
    }
}
