package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.block.entity.CrystalPedestalBlockEntity;
import com.mcmagic.omnira.item.PortableStorageRules;
import com.mcmagic.omnira.item.bottle.BottleStorageHandler;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

@GameTestHolder("omnira_portable_rules")
@PrefixGameTestTemplate(false)
public final class PortableOrbRulesGameTests {
    @GameTest(template="spell_arena") public static void onlyInventoryAndPedestal(GameTestHelper h){
        var upgrades=new ItemStackHandler(3);
        upgrades.setStackInSlot(2,new ItemStack(ModItems.SPACETIME_STABILIZATION_UPGRADE.get()));
        var data=new CompoundTag();data.put("Upgrades",upgrades.serializeNBT(h.getLevel().registryAccess()));
        var pedestal=new CrystalPedestalBlockEntity(BlockPos.ZERO,ModBlocks.CRYSTAL_PEDESTAL.get().defaultBlockState());
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        for(var item:new Item[]{ModItems.CRYSTAL_BALL.get(),ModItems.LIQUID_CRYSTAL_BALL.get(),ModItems.CRUISE_ORB.get(),ModItems.POCKET_MAGIC_BOTTLE.get()}){
            var stack=new ItemStack(item);
            h.assertTrue(!PortableStorageRules.stabilized(stack),"Empty orb is restricted");
            if(item==ModItems.POCKET_MAGIC_BOTTLE.get())stack.set(ModDataComponents.BOTTLE_CAPTURE,java.util.UUID.randomUUID());
            else stack.set(item==ModItems.CRUISE_ORB.get()?DataComponents.CUSTOM_DATA:DataComponents.BLOCK_ENTITY_DATA,CustomData.of(data));
            h.assertTrue(!stack.canFitInsideContainerItems(),"Portable item can nest in container items");
            var box=new SimpleContainer(27);var menu=ChestMenu.threeRows(1,player.getInventory(),box);
            menu.setCarried(stack.copy());menu.clicked(0,0,ClickType.PICKUP,player);
            h.assertTrue(box.isEmpty() && ItemStack.matches(menu.getCarried(),stack),"Rejected click loses or stores item");
            menu.setCarried(ItemStack.EMPTY);player.getInventory().setItem(0,stack.copy());
            menu.clicked(0,0,ClickType.SWAP,player);menu.clicked(54,0,ClickType.QUICK_MOVE,player);
            h.assertTrue(box.isEmpty() && ItemStack.matches(player.getInventory().getItem(0),stack),"Swap/shift bypasses restriction");
            h.assertTrue(new Slot(player.getInventory(),1,0,0).mayPlace(stack),"Player inventory rejects portable item");
            h.assertTrue(new Slot(pedestal,0,0,0).mayPlace(stack),"Pedestal slot rejects portable item");
            var handler=new ItemStackHandler(1);
            h.assertTrue(ItemStack.matches(handler.insertItem(0,stack.copy(),true),stack)
                    && ItemStack.matches(handler.insertItem(0,stack.copy(),false),stack),"Handler accepts portable item");
            h.assertTrue(ItemStack.matches(HopperBlockEntity.addItem(null,box,stack.copy(),Direction.UP),stack),"Hopper accepts portable item");
            var allowed=BottleStorageHandler.wrap(new InvWrapper(pedestal));
            h.assertTrue(allowed.insertItem(0,stack.copy(),false).isEmpty(),"Pedestal capability rejects portable item");
            h.assertTrue(ItemStack.matches(allowed.extractItem(0,1,false),stack),"Pedestal loses portable data");
            h.assertTrue(HopperBlockEntity.addItem(null,pedestal,stack.copy(),Direction.UP).isEmpty(),"Hopper cannot insert into pedestal");
            pedestal.clearContent();player.getInventory().clearContent();
        }
        h.succeed();
    }
}
