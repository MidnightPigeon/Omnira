package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.codex.*;
import com.mcmagic.omnira.pulse.*;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_codex_core")
@PrefixGameTestTemplate(false)
public final class CodexCoreGameTests {
    @GameTest(template="spell_arena") public static void matterTargetAcceptsModBlocksAndMachines(GameTestHelper h){
        var be=codex(h);var p=AdvancedForgeGameTests.player(h,be.getBlockPos());
        var menu=new MyriadCodexMenu(7,p.getInventory(),be);p.containerMenu=menu;
        int checked=0;
        try{
            for(var block:net.minecraft.core.registries.BuiltInRegistries.BLOCK){
                if(!net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals("omnira"))continue;
                var item=block.asItem();if(item==Items.AIR||item==MyriadCodexContent.ITEM.get()||item==MyriadCodexContent.HEART_ITEM.get())continue;
                var stack=new ItemStack(item);be.materials.setStackInSlot(1,ItemStack.EMPTY);menu.setCarried(stack.copy());
                h.assertTrue(menu.slots.get(1).mayPlace(stack),"Matter target slot refuses mod block/machine: "+stack);
                menu.clicked(1,0,net.minecraft.world.inventory.ClickType.PICKUP,p);
                h.assertTrue(menu.getCarried().isEmpty()&&ItemStack.matches(stack,be.materials.getStackInSlot(1)),"Actual right-slot insertion failed: "+stack);
                menu.clicked(1,0,net.minecraft.world.inventory.ClickType.PICKUP,p);
                h.assertTrue(ItemStack.matches(stack,menu.getCarried())&&be.materials.getStackInSlot(1).isEmpty(),"Right-slot extraction failed: "+stack);
                checked++;
            }
            h.assertTrue(checked>100,"Machine/block registry audit was incomplete");
            var frame=new ItemStack(com.mcmagic.omnira.myriad.MiniatureWorldContent.ITEM.get());
            frame.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.CustomData.of(new net.minecraft.nbt.CompoundTag()));
            h.assertTrue(com.mcmagic.omnira.item.PortableStorageRules.packedFrame(frame),"Packed frame fixture is not recognized");
            menu.setCarried(frame.copy());menu.clicked(1,0,net.minecraft.world.inventory.ClickType.PICKUP,p);
            h.assertTrue(ItemStack.matches(frame,menu.getCarried())&&be.materials.getStackInSlot(1).isEmpty(),"Packed world bypassed portable-container protection");
            menu.setCarried(new ItemStack(MyriadCodexContent.ITEM.get()));menu.clicked(1,0,net.minecraft.world.inventory.ClickType.PICKUP,p);
            h.assertTrue(menu.getCarried().is(MyriadCodexContent.ITEM.get())&&be.materials.getStackInSlot(1).isEmpty(),"Codex could designate itself as output");
            com.mcmagic.omnira.Omnira.LOGGER.info("Verified codex matter target accepts {} Omnira block/machine items",checked);
        }finally{menu.setCarried(ItemStack.EMPTY);be.materials.setStackInSlot(1,ItemStack.EMPTY);p.containerMenu=p.inventoryMenu;}
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void emptyFormStartsWithoutConsumingMaterials(GameTestHelper h){
        var be=codex(h);be.potential=8192;be.mode=1;
        var player=AdvancedForgeGameTests.player(h,be.getBlockPos());
        be.takeHeart(player);be.placeHeart(player,be.getBlockPos().east(3));
        h.assertTrue(be.start(player).isEmpty(),"Empty form did not start");
        h.assertTrue(be.job!=null&&be.heartPos==null&&be.materials.getStackInSlot(0).isEmpty(),"Empty form did not recall heart and retain empty slot");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void diameterUsesWholeOddStepsAndHardCap(GameTestHelper h){
        double[] input={0,8191,8192,10239.5,10240,12287.5,12288,14335.5,14336,16383.5,16384,32768};
        int[] output={0,0,7,7,9,9,11,11,13,13,15,15};
        for(int i=0;i<input.length;i++)h.assertTrue(MyriadCodexEntity.diameter(input[i])==output[i],"Wrong diameter at "+input[i]);
        h.succeed();
    }
    private static MyriadCodexEntity codex(GameTestHelper h){var p=new BlockPos(5,4,5);h.setBlock(p,MyriadCodexContent.BLOCK.get());return MyriadCodexEntity.from(h.getBlockEntity(p));}
    @GameTest(template="spell_arena") public static void onlyRearReceivesAndDemandCapsAt16384(GameTestHelper h){
        var pos=new BlockPos(5,4,5);
        for(var face:Direction.Plane.HORIZONTAL){
            var rear=face.getOpposite();h.setBlock(pos,MyriadCodexContent.BLOCK.get().defaultBlockState().setValue(MyriadCodexBlock.FACING,face));
            var source=pos.relative(rear);h.setBlock(source,ModBlocks.MANA_ENGINE.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,face));
            var engine=((com.mcmagic.omnira.block.entity.ManaEngineAccess)h.getBlockEntity(source)).engineState();
            engine.inventory.setItem(0,new ItemStack(ModItems.TEST_SPELL_CORE.get()));
            engine.inventory.setItem(1,new ItemStack(ModItems.OMNI_CRYSTAL_GRID.get()));engine.inventory.setItem(2,new ItemStack(ModItems.OMNI_CRYSTAL_GRID.get()));
            var power=PulseNetworks.power(h.getLevel(),h.absolutePos(pos));
            h.assertTrue(power.running()&&Math.abs(power.allocated()-8192)<.001,"Rear input failed for "+face);
            engine.inventory.setItem(3,new ItemStack(ModItems.OMNI_CRYSTAL_GRID.get()));PulseNetworks.changed(h.getLevel(),h.absolutePos(pos));
            h.assertTrue(Math.abs(PulseNetworks.power(h.getLevel(),h.absolutePos(pos)).allocated()-16384)<.001,"Codex exceeded cap");
            h.getLevel().removeBlock(h.absolutePos(source),false);
            var front=pos.relative(face);h.setBlock(front,ModBlocks.MANA_ENGINE.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,rear));
            h.assertTrue(!PulseNetworks.power(h.getLevel(),h.absolutePos(pos)).running(),"Front is a second input");
            h.getLevel().removeBlock(h.absolutePos(front),false);h.getLevel().removeBlock(h.absolutePos(pos),false);
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void tokenPlacesOnceAndBrokenHeartReturns(GameTestHelper h){
        var be=codex(h);be.potential=8192;var p=AdvancedForgeGameTests.player(h,be.getBlockPos());
        be.takeHeart(p);var token=p.getMainHandItem();var old=HeartLink.of(token);
        h.assertTrue(old!=null&&be.validHolder(p),"Empty main hand did not take linked heart");
        var at=be.getBlockPos().east(3);h.assertTrue(be.placeHeart(p,at)&&p.getMainHandItem().isEmpty(),"Heart did not place");
        h.assertTrue(!be.placeHeart(p,at.above()),"Repeated placement created a second heart");
        h.getLevel().removeBlock(at,false);
        h.assertTrue(be.heartPos==null&&be.holder==null&&!old.equals(be.link()),"Broken heart did not recall or invalidate lease");
        be.takeHeart(p);h.assertTrue(!old.equals(HeartLink.of(p.getMainHandItem())),"Old token lease was reused");
        var stack=p.getMainHandItem();stack.getItem().onDroppedByPlayer(stack,p);
        h.assertTrue(be.holder==null&&stack.isEmpty(),"Dropping created a portable heart");h.succeed();
    }
    @GameTest(template="spell_arena") public static void emptyHandBlockInteractionTakesAndRetrievesHeart(GameTestHelper h){
        var be=codex(h);be.potential=8192;
        // FakePlayer normally suppresses menu opening; retain a local menu to observe this path.
        var p=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"codex-use")){
            @Override public java.util.OptionalInt openMenu(net.minecraft.world.MenuProvider provider,java.util.function.Consumer<net.minecraft.network.RegistryFriendlyByteBuf> writer){
                if(provider==null)return java.util.OptionalInt.empty();
                containerMenu=provider.createMenu(91,getInventory(),this);return java.util.OptionalInt.of(91);
            }
        };
        p.setPos(be.getBlockPos().getX()+.5,be.getBlockPos().getY(),be.getBlockPos().getZ()+3);
        p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,new ItemStack(Items.TORCH));
        var pos=be.getBlockPos();var hit=new net.minecraft.world.phys.BlockHitResult(pos.getCenter(),Direction.UP,pos,false);
        var state=be.getBlockState();
        h.assertTrue(state.useItemOn(ItemStack.EMPTY,h.getLevel(),p,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION,"Empty hand opened UI instead of default pickup");
        p.gameMode.useItemOn(p,h.getLevel(),ItemStack.EMPTY,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
        h.assertTrue(be.validHolder(p)&&p.containerMenu==p.inventoryMenu,"Real block interaction did not take heart");
        var at=pos.east(3);h.assertTrue(be.placeHeart(p,at),"Heart did not place");
        var placedLink=be.link();be.materials.setStackInSlot(0,new ItemStack(Items.STONE));
        p.gameMode.useItemOn(p,h.getLevel(),ItemStack.EMPTY,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
        h.assertTrue(p.containerMenu instanceof MyriadCodexMenu&&at.equals(be.heartPos)&&placedLink.equals(be.link())
                &&be.holder==null&&p.getMainHandItem().isEmpty()&&be.materials.getStackInSlot(0).is(Items.STONE),"Empty-hand configuration recalled marker or reset formula");
        p.closeContainer();
        var placed=new net.minecraft.world.phys.BlockHitResult(at.getCenter(),Direction.UP,at,false);
        p.gameMode.useItemOn(p,h.getLevel(),ItemStack.EMPTY,net.minecraft.world.InteractionHand.MAIN_HAND,placed);
        h.assertTrue(be.validHolder(p)&&be.heartPos==null&&h.getLevel().getBlockState(at).isAir(),"Placed heart could not be retrieved empty-handed");
        h.assertTrue(p.getOffhandItem().is(Items.TORCH),"Pickup modified offhand");
        be.recall();p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));
        p.gameMode.useItemOn(p,h.getLevel(),p.getMainHandItem(),net.minecraft.world.InteractionHand.MAIN_HAND,hit);
        h.assertTrue(p.containerMenu instanceof MyriadCodexMenu&&p.getMainHandItem().is(Items.STICK),"Held item no longer opens configuration");
        p.closeContainer();h.succeed();
    }
    @GameTest(template="spell_arena") public static void invalidStartPreservesMarkerAndIngredients(GameTestHelper h){
        var be=codex(h);be.potential=8192;var p=AdvancedForgeGameTests.player(h,be.getBlockPos());
        be.materials.setStackInSlot(0,new ItemStack(Items.STONE));be.materials.setStackInSlot(1,new ItemStack(Items.DIRT));
        h.assertTrue(be.start(p).equals("heart")&&be.materials.getStackInSlot(0).getCount()==1,"Missing marker consumed materials");
        be.takeHeart(p);var at=be.getBlockPos().east(3);h.assertTrue(be.placeHeart(p,at),"No test marker");
        be.materials.setStackInSlot(0,ItemStack.EMPTY);
        h.assertTrue(be.start(p).equals("material")&&be.heartPos.equals(at)&&!be.materials.getStackInSlot(1).isEmpty(),"Missing source recalled marker or ate material");
        h.assertTrue(!be.materials.isItemValid(0,new ItemStack(MyriadCodexContent.ITEM.get()))&&!be.materials.isItemValid(0,new ItemStack(MyriadCodexContent.HEART_ITEM.get())),"Recursive target accepted");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void configurationPersistsAndSuccessfulStartConsumesExactlyOnce(GameTestHelper h){
        var be=codex(h);be.potential=8192;be.mode=1;be.shape=2;be.axis=0;be.hollow=true;
        be.materials.setStackInSlot(0,new ItemStack(Items.STONE));
        var saved=be.saveWithoutMetadata(h.getLevel().registryAccess());var identity=be.identity;
        be.loadWithComponents(saved,h.getLevel().registryAccess());
        h.assertTrue(be.identity.equals(identity)&&be.mode==1&&be.shape==2&&be.axis==0&&be.hollow,"Codex save lost configuration");
        var p=AdvancedForgeGameTests.player(h,be.getBlockPos());be.takeHeart(p);
        h.assertTrue(be.placeHeart(p,be.getBlockPos().east(3)),"No marker");
        h.assertTrue(be.start(p).isEmpty()&&be.busy()&&be.heartPos==null&&be.materials.getStackInSlot(0).isEmpty(),"Accepted start did not consume once/recall");
        h.assertTrue(be.start(p).equals("busy"),"Repeated click queued a duplicate job");
        h.assertTrue(!be.materials.isItemValid(0,new ItemStack(Items.DIAMOND)),"Busy codex accepts new formula");h.succeed();
    }
    @GameTest(template="spell_arena") public static void bookSlotsStayInsideSpreadAndSwitchWithoutConsuming(GameTestHelper h){
        var be=codex(h);var p=AdvancedForgeGameTests.player(h,be.getBlockPos());
        var menu=new MyriadCodexMenu(7,p.getInventory(),be);
        be.materials.setStackInSlot(0,new ItemStack(Items.STONE));be.materials.setStackInSlot(1,new ItemStack(Items.DIRT));
        for(var slot:menu.slots)h.assertTrue(slot.x>=0&&slot.y>=0&&slot.x+16<=CodexLayout.WIDTH&&slot.y+16<=CodexLayout.HEIGHT,"Slot escapes book/inventory panel");
        for(int mode=0;mode<3;mode++){
            h.assertTrue(menu.clickMenuButton(p,mode),"Mode switch rejected");
            int visible=0;for(int i=0;i<4;i++)if(menu.slots.get(i).isActive())visible++;
            h.assertTrue(visible==(mode==0?2:1),"Duplicated formula ports");
            h.assertTrue(be.materials.getStackInSlot(0).is(Items.STONE),"Mode switching consumed first specimen");
            for(int i=4;i<menu.slots.size();i++)h.assertTrue(menu.slots.get(i).isActive()&&menu.slots.get(i).y>CodexLayout.BOOK_HEIGHT,"Inventory overlaps book or is inactive");
        }
        h.assertTrue(p.getInventory().countItem(Items.DIRT)==1&&be.materials.getStackInSlot(1).isEmpty(),"Hidden second specimen lost or copied");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void inventoryAndFormulaAcceptPickupAndQuickMoveTogether(GameTestHelper h){
        var be=codex(h);var p=AdvancedForgeGameTests.player(h,be.getBlockPos());var menu=new MyriadCodexMenu(7,p.getInventory(),be);p.containerMenu=menu;
        for(int mode=0;mode<3;mode++){
            h.assertTrue(menu.clickMenuButton(p,mode),"Mode unavailable");int input=mode==0?0:mode+1;
            p.getInventory().setItem(9,new ItemStack(Items.STONE,2));
            menu.clicked(4,0,net.minecraft.world.inventory.ClickType.PICKUP,p);
            h.assertTrue(menu.getCarried().getCount()==2,"Inventory cannot pick up material");
            menu.clicked(input,0,net.minecraft.world.inventory.ClickType.PICKUP,p);
            h.assertTrue(be.materials.getStackInSlot(0).is(Items.STONE)&&menu.getCarried().getCount()==1,"Carried material cannot enter active formula slot");
            menu.clicked(4,0,net.minecraft.world.inventory.ClickType.PICKUP,p);
            menu.clicked(input,0,net.minecraft.world.inventory.ClickType.PICKUP,p);
            menu.clicked(4,0,net.minecraft.world.inventory.ClickType.PICKUP,p);
            h.assertTrue(menu.getCarried().isEmpty()&&p.getInventory().getItem(9).getCount()==2&&be.materials.getStackInSlot(0).isEmpty(),"Returning specimen lost/duplicated items");
            h.assertTrue(!menu.quickMoveStack(p,4).isEmpty()&&be.materials.getStackInSlot(0).is(Items.STONE),"Inventory quick move cannot insert formula");
            menu.quickMoveStack(p,input);if(mode==0)menu.quickMoveStack(p,1);
            h.assertTrue(p.getInventory().countItem(Items.STONE)==2&&be.materials.getStackInSlot(0).isEmpty()&&be.materials.getStackInSlot(1).isEmpty(),"Formula quick move lost/duplicated items");
            p.getInventory().clearContent();
        }
        p.closeContainer();h.succeed();
    }
    @GameTest(template="spell_arena") public static void outsideMarkerFadesForTenSecondsAndReturns(GameTestHelper h){
        var be=codex(h);var pos=be.getBlockPos();var source=pos.south();
        h.getLevel().setBlock(source,ModBlocks.MANA_ENGINE.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.NORTH),3);
        var engine=((com.mcmagic.omnira.block.entity.ManaEngineAccess)h.getLevel().getBlockEntity(source)).engineState();
        engine.inventory.setItem(0,new ItemStack(ModItems.TEST_SPELL_CORE.get()));
        engine.inventory.setItem(1,new ItemStack(ModItems.OMNI_CRYSTAL_GRID.get()));engine.inventory.setItem(2,new ItemStack(ModItems.OMNI_CRYSTAL_GRID.get()));
        be.potential=8192;var p=AdvancedForgeGameTests.player(h,pos);be.takeHeart(p);var target=pos.above(33);
        h.assertTrue(be.placeHeart(p,target),"No distant marker");
        for(int i=0;i<199;i++)be.tick();
        h.assertTrue(be.fade==9&&target.equals(be.heartPos),"Marker vanished before ten seconds");
        be.tick();h.assertTrue(be.fade==0&&be.heartPos==null&&h.getLevel().getBlockState(target).isAir(),"Marker did not return at ten seconds");
        be.takeHeart(p);var token=p.getMainHandItem();p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        token.getItem().inventoryTick(token,h.getLevel(),p,1,false);
        h.assertTrue(be.holder==null&&token.isEmpty(),"Switching hands did not recall marker");h.succeed();
    }
}
