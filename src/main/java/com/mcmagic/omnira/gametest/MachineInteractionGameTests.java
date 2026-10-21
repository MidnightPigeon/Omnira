package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.alchemy.*;
import com.mcmagic.omnira.block.entity.*;
import com.mcmagic.omnira.codex.*;
import com.mcmagic.omnira.menu.*;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.workstation.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("omnira_menu_interactions") @PrefixGameTestTemplate(false)
public final class MachineInteractionGameTests {
    private static ItemStack sample(int count){var s=new ItemStack(Items.DIAMOND,count);s.set(DataComponents.CUSTOM_NAME,Component.literal("interaction"));return s;}
    private static int count(AbstractContainerMenu m,ItemStack sample){
        int n=0;for(var slot:m.slots)if(slot.isActive()&&ItemStack.isSameItemSameComponents(slot.getItem(),sample))n+=slot.getItem().getCount();
        if(ItemStack.isSameItemSameComponents(m.getCarried(),sample))n+=m.getCarried().getCount();return n;
    }
    private static void drag(AbstractContainerMenu m,Player p,int mode,int...slots){
        m.clicked(-999,AbstractContainerMenu.getQuickcraftMask(0,mode),ClickType.QUICK_CRAFT,p);
        for(int slot:slots)m.clicked(slot,AbstractContainerMenu.getQuickcraftMask(1,mode),ClickType.QUICK_CRAFT,p);
        m.clicked(-999,AbstractContainerMenu.getQuickcraftMask(2,mode),ClickType.QUICK_CRAFT,p);
    }
    private static void clear(AbstractContainerMenu m,Player p,int start){
        p.getInventory().clearContent();m.setCarried(ItemStack.EMPTY);for(int i=0;i<start;i++)m.getSlot(i).set(ItemStack.EMPTY);
    }
    private static void interactions(GameTestHelper h,AbstractContainerMenu m,Player p,int start){
        var s=sample(17);
        for(int button:new int[]{0,1}){
            clear(m,p,start);m.setCarried(s.copy());boolean allowed=m.getSlot(0).mayPlace(s);m.clicked(0,button,ClickType.PICKUP,p);
            h.assertTrue(count(m,s)==17,"Click lost/duplicated components: "+m.getClass());
            h.assertTrue(allowed||m.getSlot(0).getItem().isEmpty(),"Click bypassed slot restriction");
        }
        for(int mode:new int[]{0,1}){
            clear(m,p,start);m.setCarried(s.copy());boolean a=m.getSlot(0).mayPlace(s),b=start>1&&m.getSlot(1).mayPlace(s);
            drag(m,p,mode,0,start>1?1:0,start);
            h.assertTrue(count(m,s)==17,"Drag lost/duplicated components: "+m.getClass());
            h.assertTrue(a||m.getSlot(0).getItem().isEmpty(),"Drag bypassed first slot restriction");
            if(start>1)h.assertTrue(b||m.getSlot(1).getItem().isEmpty(),"Drag wrote output/locked slot");
        }
        for(int button:new int[]{0,40}){
            clear(m,p,start);p.getInventory().setItem(button,s.copy());boolean allowed=m.getSlot(0).mayPlace(s);
            m.clicked(0,button,ClickType.SWAP,p);
            int total=count(m,s)+(button==40&&ItemStack.isSameItemSameComponents(p.getOffhandItem(),s)?p.getOffhandItem().getCount():0);
            h.assertTrue(total==17,"Number/offhand swap lost items: "+m.getClass());
            h.assertTrue(allowed||m.getSlot(0).getItem().isEmpty(),"Swap bypassed slot restriction");
            p.getInventory().setItem(40,ItemStack.EMPTY);
        }
        clear(m,p,start);m.setCarried(sample(1));m.getSlot(start).set(sample(17));m.getSlot(start+1).set(sample(5));
        m.clicked(start+2,0,ClickType.PICKUP_ALL,p);h.assertTrue(count(m,s)==23&&m.getCarried().getCount()==23,"Double collect lost/duplicated items");
        clear(m,p,start);
    }
    @GameTest(template="spell_arena") public static void vanillaInteractionMatrix(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);var pos=new BlockPos(5,3,5);
        for(var id:List.of("crystal_processing_table","analysis_artisan_table","arcane_assembly_table","mana_engine","simple_condensation_table","advanced_condensation_table","spacetime_reverser","void_infuser","split_time_sundial",
                "crystal_furnace","crystal_grindstone","crystal_anvil","infused_crystal_furnace","infused_crystal_grindstone","infused_crystal_anvil")){
            h.setBlock(pos,BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("omnira",id)));var be=h.getBlockEntity(pos);
            MenuProvider provider=be instanceof AssemblyAccess a?a.assembly():be instanceof WorkstationAccess a?a.workstation():(MenuProvider)be;
            var m=provider.createMenu(1,p.getInventory(),p);interactions(h,m,p,m.slots.size()-36);
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void poolDragAndLargeStackBoundaries(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(5,3,5));h.assertTrue(ReactionPoolBlock.place(h.getLevel(),pos,Direction.NORTH),"Pool placement failed");
        var pool=ReactionPoolBlock.controller(h.getLevel(),pos);var p=h.makeMockPlayer(GameType.SURVIVAL);var m=new ReactionPoolMenu(1,p.getInventory(),pool);
        interactions(h,m,p,13);pool.upgrades.setStackInSlot(0,new ItemStack(ModItems.INFUSED_STACKING_UPGRADE.get()));
        for(int button:new int[]{0,1}){
            pool.items.setStackInSlot(0,sample(200));m.setCarried(ItemStack.EMPTY);m.clicked(0,button,ClickType.PICKUP,p);
            h.assertTrue(count(m,sample(1))==200&&m.getCarried().getCount()<=64,"Large stack click overflow/loss");m.setCarried(ItemStack.EMPTY);
        }
        pool.items.setStackInSlot(0,sample(200));p.getInventory().setItem(0,ItemStack.EMPTY);m.clicked(0,0,ClickType.SWAP,p);
        h.assertTrue(pool.items.getStackInSlot(0).getCount()==136&&p.getInventory().getItem(0).getCount()==64,"Large stack number swap overflow");
        var before=pool.upgrades.getStackInSlot(0).copy();m.setCarried(new ItemStack(ModItems.INFUSED_STACKING_UPGRADE.get()));
        m.clicked(13,0,ClickType.PICKUP_ALL,p);h.assertTrue(ItemStack.matches(before,pool.upgrades.getStackInSlot(0)),"Double collect removed required capacity upgrade");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void crystalStorageRejectsPackedItemsAcrossGestures(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);var pos=new BlockPos(5,3,5);
        var banned=new ItemStack(ModItems.POCKET_MAGIC_BOTTLE.get());banned.set(ModDataComponents.BOTTLE_CAPTURE,UUID.randomUUID());
        for(var id:List.of("crystal_ball","resonant_crystal_ball")){
            h.setBlock(pos,BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("omnira",id)));var ball=(CrystalBallBlockEntity)h.getBlockEntity(pos);
            var m=new CrystalBallMenu(1,p.getInventory(),ball);p.getInventory().clearContent();
            m.setCarried(banned.copy());m.clicked(0,0,ClickType.PICKUP,p);h.assertTrue(ball.isEmpty()&&ItemStack.matches(m.getCarried(),banned),"Click accepted packed bottle");
            drag(m,p,0,0,1);h.assertTrue(ball.isEmpty()&&ItemStack.matches(m.getCarried(),banned),"Drag accepted packed bottle");
            m.setCarried(ItemStack.EMPTY);p.getInventory().setItem(0,banned.copy());m.clicked(0,0,ClickType.SWAP,p);
            h.assertTrue(ball.isEmpty()&&ItemStack.matches(p.getInventory().getItem(0),banned),"Number key accepted packed bottle");
            clear(m,p,m.playerStart);ball.setItem(0,sample(20));m.setCarried(sample(1));m.clicked(m.playerStart,0,ClickType.PICKUP_ALL,p);
            h.assertTrue(ball.isEmpty()&&m.getCarried().getCount()==21,"Storage double collect failed");
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void codexHiddenSlotsCannotDragOrCollect(GameTestHelper h){
        var pos=new BlockPos(5,4,5);h.setBlock(pos,MyriadCodexContent.BLOCK.get());var be=MyriadCodexEntity.from(h.getBlockEntity(pos));
        var p=h.makeMockPlayer(GameType.SURVIVAL);p.setPos(net.minecraft.world.phys.Vec3.atCenterOf(be.getBlockPos()));var m=new MyriadCodexMenu(1,p.getInventory(),be);
        for(int mode=0;mode<3;mode++){
            be.mode=mode;be.materials.setStackInSlot(0,ItemStack.EMPTY);be.materials.setStackInSlot(1,ItemStack.EMPTY);m.setCarried(sample(4));
            drag(m,p,0,0,1,2,3);int physical=be.materials.getStackInSlot(0).getCount()+be.materials.getStackInSlot(1).getCount()+m.getCarried().getCount();
            h.assertTrue(physical==4,"Codex alias drag duplicated/lost items");
            if(mode!=0)h.assertTrue(be.materials.getStackInSlot(1).isEmpty(),"Drag filled hidden second material");
            be.materials.setStackInSlot(0,ItemStack.EMPTY);be.materials.setStackInSlot(1,sample(1));m.setCarried(sample(1));m.clicked(4,0,ClickType.PICKUP_ALL,p);
            h.assertTrue(mode==0?be.materials.getStackInSlot(1).isEmpty():be.materials.getStackInSlot(1).getCount()==1,"Double collect accessed hidden material");
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void frequencySamplesRemainGhostsForNonClickGestures(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);var pos=new BlockPos(5,3,5);
        for(var id:List.of("resonant_crystal_ball","resonant_liquid_crystal_ball","resonant_accumulator_orb")){
            h.setBlock(pos,BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("omnira",id)));var be=h.getBlockEntity(pos);
            AbstractContainerMenu m;int ghost;
            if(be instanceof CrystalBallBlockEntity ball){var menu=new CrystalBallMenu(1,p.getInventory(),ball);m=menu;ghost=menu.frequencyStart;}
            else {var orb=com.mcmagic.omnira.pulse.PulseOrbEntity.from(be);m=new com.mcmagic.omnira.pulse.PulseOrbMenu(1,p.getInventory(),orb);ghost=1;}
            p.getInventory().clearContent();p.setPos(net.minecraft.world.phys.Vec3.atCenterOf(be.getBlockPos()));m.setCarried(new ItemStack(Items.STONE));m.clicked(ghost,0,ClickType.PICKUP,p);
            h.assertTrue(m.getSlot(ghost).getItem().is(Items.STONE),"Frequency sample not set");
            m.setCarried(sample(1));p.getInventory().setItem(0,sample(1));
            for(var type:List.of(ClickType.SWAP,ClickType.THROW,ClickType.PICKUP_ALL,ClickType.QUICK_MOVE)){
                m.clicked(ghost,0,type,p);h.assertTrue(m.getSlot(ghost).getItem().is(Items.STONE)&&m.getCarried().getCount()==1&&p.getInventory().getItem(0).getCount()==1,"Ghost gesture changed frequency or moved sample: "+type);
            }
            drag(m,p,0,ghost,ghost+1);h.assertTrue(m.getSlot(ghost).getItem().is(Items.STONE)&&m.getSlot(ghost+1).getItem().isEmpty(),"Drag modified frequency samples");
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void sharedLargeStackGuardPreservesStorageAndVehicle(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);var pos=new BlockPos(5,3,5);h.setBlock(pos,ModBlocks.CRYSTAL_BALL.get());var ball=(CrystalBallBlockEntity)h.getBlockEntity(pos);
        ball.upgrades.setStackInSlot(0,new ItemStack(ModItems.INFUSED_STACKING_UPGRADE.get()));var m=new CrystalBallMenu(1,p.getInventory(),ball);
        for(int button:new int[]{0,1}){
            p.getInventory().clearContent();ball.setItem(0,sample(200));m.setCarried(ItemStack.EMPTY);m.clicked(0,button,ClickType.PICKUP,p);
            h.assertTrue(count(m,sample(1))==200&&m.getCarried().getCount()==(button==0?64:32),"Storage oversized click changed behavior");
        }
        ball.setItem(0,sample(128));m.setCarried(sample(12));m.clicked(0,0,ClickType.PICKUP,p);
        h.assertTrue(ball.getItem(0).getCount()==140&&m.getCarried().isEmpty(),"Vanilla insertion cannot merge into expanded stack");
        var orb=ModEntityTypes.CRUISE_ORB.get().create(h.getLevel());orb.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(8,20,8)));p.setPos(orb.position().add(0,com.mcmagic.omnira.vehicle.CruiseOrbPanel.RIDER_Y,.18));
        h.assertTrue(p.startRiding(orb),"Cannot mount test vehicle");var vehicle=new com.mcmagic.omnira.vehicle.CruiseOrbMenu(1,p.getInventory(),orb);
        try{
            p.getInventory().clearContent();orb.storage.upgrades.setStackInSlot(0,new ItemStack(ModItems.INFUSED_STACKING_UPGRADE.get()));orb.storage.items.setStackInSlot(0,sample(200));
            vehicle.setCarried(ItemStack.EMPTY);vehicle.clicked(0,0,ClickType.PICKUP,p);
            h.assertTrue(vehicle.getCarried().getCount()==64&&orb.storage.items.getStackInSlot(0).getCount()==136,"Vehicle cursor overflow");
            vehicle.setCarried(ItemStack.EMPTY);vehicle.clicked(0,40,ClickType.SWAP,p);
            h.assertTrue(p.getOffhandItem().getCount()==64&&orb.storage.items.getStackInSlot(0).getCount()==72,"Vehicle offhand overflow");
        }finally{p.stopRiding();orb.discard();}h.succeed();
    }
    @GameTest(template="spell_arena") public static void previewOutputNumberKeysSettleRecipe(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);
        for(int index:new int[]{1,2,4,5})for(int key:new int[]{0,40}){
            var pos=new BlockPos(5,3,5);h.setBlock(pos,CrystalWorkstations.BLOCKS.get(index).get());var work=CrystalWorkstations.work(h.getBlockEntity(pos));
            p.getInventory().clearContent();p.getInventory().setItem(40,ItemStack.EMPTY);p.experienceLevel=100;p.setData(ModAttachments.MANA,new com.mcmagic.omnira.mana.ManaState(10000,10000));
            var input=new ItemStack(Items.IRON_SWORD);input.setDamageValue(100);work.setItem(0,input);
            work.setItem(1,index%3==2?new ItemStack(Items.IRON_INGOT,3):input.copy());
            var expected=work.preview().copy();h.assertTrue(!expected.isEmpty()&&work.canTake(p),"Missing test preview");
            var m=new CrystalWorkstationMenu(1,p.getInventory(),work);int levels=p.experienceLevel;int durability=work.durability;
            m.clicked(2,key,ClickType.SWAP,p);
            h.assertTrue(ItemStack.matches(p.getInventory().getItem(key),expected)&&work.getItem(0).isEmpty()&&work.preview().isEmpty(),"Number key copied preview without consuming ingredients");
            if(index%3==2)h.assertTrue(p.experienceLevel<levels&&(index>=3||work.durability<durability),"Anvil number key bypassed XP/durability");
            m.clicked(2,key,ClickType.SWAP,p);h.assertTrue(ItemStack.matches(p.getInventory().getItem(key),expected),"Repeated swap duplicated output");
        }h.succeed();
    }
    private MachineInteractionGameTests(){}
}
