package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.registry.*;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.items.*;

@GameTestHolder("omnira_bottle_containers")
@PrefixGameTestTemplate(false)
public final class BottleContainerGameTests {
    @GameTest(template="spell_arena")
    public static void rejectsStorageWithoutLosingBottle(GameTestHelper h) {
        var bottle=new ItemStack(ModItems.POCKET_MAGIC_BOTTLE.get());
        bottle.set(ModDataComponents.BOTTLE_CAPTURE,java.util.UUID.randomUUID());
        var empty=new ItemStack(ModItems.POCKET_MAGIC_BOTTLE.get());
        h.assertTrue(!bottle.canFitInsideContainerItems() && empty.canFitInsideContainerItems(),"Nested-container rule depends on contents");
        var container=new SimpleContainer(27);
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        var menu=ChestMenu.threeRows(1,player.getInventory(),container);
        menu.setCarried(bottle.copy());
        menu.clicked(0,0,ClickType.PICKUP,player);
        h.assertTrue(container.isEmpty() && menu.getCarried().getCount()==1,"Click must reject without deletion");
        menu.setCarried(ItemStack.EMPTY);
        player.getInventory().setItem(0,bottle.copy());
        menu.clicked(0,0,ClickType.SWAP,player);
        h.assertTrue(container.isEmpty() && player.getInventory().getItem(0).getCount()==1,"Hotbar swap must reject");
        menu.clicked(54,0,ClickType.QUICK_MOVE,player);
        h.assertTrue(container.isEmpty() && player.getInventory().getItem(0).getCount()==1,"Shift transfer must reject");
        h.assertTrue(new Slot(player.getInventory(),1,0,0).mayPlace(bottle),"Player inventory must remain usable");
        var remaining=HopperBlockEntity.addItem(null,container,bottle.copy(),Direction.UP);
        h.assertTrue(remaining.getCount()==1 && container.isEmpty(),"Hopper must reject without loss");
        var handler=new ItemStackHandler(1);
        h.assertTrue(handler.insertItem(0,bottle.copy(),true).getCount()==1,"Simulation must reject");
        h.assertTrue(handler.insertItem(0,bottle.copy(),false).getCount()==1 && handler.getStackInSlot(0).isEmpty(),"Automation must reject");
        var ballPos=h.absolutePos(new net.minecraft.core.BlockPos(2,2,2));
        h.getLevel().setBlockAndUpdate(ballPos,ModBlocks.CRYSTAL_BALL.get().defaultBlockState());
        var capability=h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,ballPos,Direction.UP);
        h.assertTrue(capability instanceof com.mcmagic.omnira.item.bottle.BottleStorageHandler,"Block storage capability must be guarded");
        h.assertTrue(capability.insertItem(0,bottle.copy(),false).getCount()==1,"Crystal ball must reject filled bottle");
        h.assertTrue(capability.insertItem(0,empty.copy(),false).isEmpty(),"Crystal ball must accept empty bottle");
        var minecart=net.minecraft.world.entity.EntityType.CHEST_MINECART.create(h.getLevel());
        var cartHandler=minecart.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.ENTITY_AUTOMATION,Direction.UP);
        h.assertTrue(cartHandler instanceof com.mcmagic.omnira.item.bottle.BottleStorageHandler
                && cartHandler.insertItem(0,bottle.copy(),false).getCount()==1,"Entity storage automation must reject filled bottles");
        menu.setCarried(empty);
        menu.clicked(0,0,ClickType.PICKUP,player);
        h.assertTrue(!container.isEmpty() && menu.getCarried().isEmpty(),"Empty bottles remain storable");
        h.succeed();
    }
}
