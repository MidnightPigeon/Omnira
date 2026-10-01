package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.block.ResonanceCoreBlock;
import com.mcmagic.omnira.block.entity.*;
import com.mcmagic.omnira.item.*;
import com.mcmagic.omnira.menu.*;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.shop.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;
import java.util.UUID;

@GameTestHolder("omnira_enhanced_resonance") @PrefixGameTestTemplate(false)
public final class EnhancedResonanceGameTests {
    private static Player player(GameTestHelper h,boolean enhanced){
        var p=h.makeMockPlayer(GameType.SURVIVAL);p.setUUID(UUID.randomUUID());
        var stack=new ItemStack((enhanced?ModItems.ENHANCED_RESONANCE_TERMINAL:ModItems.RESONANCE_TERMINAL).get());
        p.getInventory().setItem(0,stack);ResonanceTerminalItem.authorize(stack,p);return p;
    }
    private static ResonanceCoreBlockEntity core(GameTestHelper h,Player p,int x,boolean shop){
        var pos=h.absolutePos(new BlockPos(x,3,6));
        h.getLevel().setBlockAndUpdate(pos,(shop?KirisameContent.ORB.get():ModBlocks.CRYSTAL_BALL.get()).defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos.above(),ModBlocks.RESONANCE_CORE.get().defaultBlockState());
        var core=(ResonanceCoreBlockEntity)h.getLevel().getBlockEntity(pos.above());core.owner=p.getUUID();return core;
    }
    private static void bind(Player p,ResonanceCoreBlockEntity core){
        p.setShiftKeyDown(true);
        p.getMainHandItem().getItem().useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,
                new BlockHitResult(core.getBlockPos().getCenter(),Direction.UP,core.getBlockPos(),false)));
        p.setShiftKeyDown(false);
    }
    @GameTest(template="spell_arena") public static void destroyedNodesFreeOpenAndStoredLinks(GameTestHelper h){
        var p=player(h,false);var first=core(h,p,3,false);var second=core(h,p,6,false);bind(p,first);bind(p,second);
        var stack=p.getMainHandItem();var storedCopy=stack.copy();var menu=new ResonanceMenu(1,p.getInventory(),0);
        h.assertTrue(menu.clickMenuButton(p,0),"Initial link failed");
        first.refresh();h.assertTrue(!ResonanceLinks.get(h.getLevel()).isDestroyed(first.identity),"State refresh marked node destroyed");
        h.getLevel().removeBlock(first.getBlockPos(),false);h.getLevel().removeBlock(second.getBlockPos(),false);menu.broadcastChanges();
        h.assertTrue(menu.selected()==-1&&menu.status(0)==0&&menu.status(1)==0,"Destroyed selected/unselected nodes remain in panel");
        h.assertTrue(!ResonanceTerminalItem.data(stack).contains("Link0")&&!ResonanceTerminalItem.data(stack).contains("Link1"),"Destroyed nodes occupy terminal capacity");
        var restored=ResonanceLinks.load(ResonanceLinks.get(h.getLevel()).save(new CompoundTag(),h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(restored.isDestroyed(first.identity),"Destruction not persisted for offline owner");
        ResonanceLinks.prune(storedCopy,h.getLevel(),false);
        h.assertTrue(!ResonanceTerminalItem.data(storedCopy).contains("Link0"),"Stored terminal retains destroyed node");
        var replacement=core(h,p,3,false);bind(p,replacement);
        h.assertTrue(ResonanceTerminalItem.data(stack).getCompound("Link0").getUUID("Identity").equals(replacement.identity),"Vacated position cannot be rebound");
        menu.removed(p);h.succeed();
    }
    @GameTest(template="spell_arena") public static void unloadedIsNotDestroyed(GameTestHelper h){
        var p=player(h,false);var stack=p.getMainHandItem();var tag=ResonanceTerminalItem.data(stack);
        var pos=new BlockPos(21345123,60,21345678);
        h.assertTrue(!h.getLevel().hasChunkAt(pos),"Fixture must be unloaded");
        var link=new CompoundTag();link.putUUID("Identity",UUID.randomUUID());link.putLong("Pos",pos.asLong());
        link.putString("Dimension",h.getLevel().dimension().location().toString());link.putInt("Kind",2);tag.put("Link0",link);ResonanceTerminalItem.save(stack,tag);
        ResonanceLinks.prune(stack,h.getLevel(),false);
        h.assertTrue(ResonanceTerminalItem.data(stack).contains("Link0")&&!h.getLevel().hasChunkAt(pos),"Unloaded link erased or force-loaded during background scan");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void sixLinksAndSixthShop(GameTestHelper h){
        var p=player(h,true);ResonanceCoreBlockEntity sixth=null;
        for(int i=0;i<7;i++){
            var node=core(h,p,2+i,i==5);bind(p,node);
            h.assertTrue((node.terminal!=null)==(i<6),"Enhanced binding limit is not six");if(i==5)sixth=node;
        }
        var menu=new ResonanceMenu(1,p.getInventory(),0);
        h.assertTrue(menu.capacity()==6&&menu.clickMenuButton(p,4)&&menu.selected()==4,"Fifth link conflicts with paging");
        h.assertTrue(menu.clickMenuButton(p,5)&&menu.kind()==4&&menu.selected()==5,"Sixth shop link cannot be selected");
        var access=new MarisaRemoteAccess(sixth,p,p.getMainHandItem(),0);
        h.assertTrue(access.valid(),"Remote merchant rejects sixth enhanced link");access.close();
        h.assertTrue(menu.clickMenuButton(p,ResonanceMenu.UNLINK)&&sixth.terminal==null
                &&sixth.getBlockState().getValue(ResonanceCoreBlock.MODE)==1,"Sixth shop unlink failed");
        menu.removed(p);h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=240) public static void condensationRetainsExistingBindings(GameTestHelper h){
        var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"resonance-upgrade"));
        var stack=new ItemStack(ModItems.RESONANCE_TERMINAL.get());p.getInventory().setItem(0,stack);ResonanceTerminalItem.authorize(stack,p);
        var node=core(h,p,9,false);bind(p,node);var original=ResonanceTerminalItem.data(stack);
        var pos=h.absolutePos(new BlockPos(5,2,5));h.getLevel().setBlockAndUpdate(pos,ModBlocks.ADVANCED_CONDENSATION_TABLE.get().defaultBlockState());
        var table=(AdvancedCondensationTableBlockEntity)h.getLevel().getBlockEntity(pos);
        p.setPos(pos.getCenter());p.setData(ModAttachments.MANA,new com.mcmagic.omnira.mana.ManaState(500,500));
        p.getInventory().setItem(0,ItemStack.EMPTY);table.setItem(0,stack);
        var menu=new SimpleCondensationTableMenu(1,p.getInventory(),table);
        h.assertTrue(menu.recipe().advanced()&&menu.recipe().manaCost()==300&&menu.clickMenuButton(p,0),"Upgrade recipe did not start");
        h.runAfterDelay(200,()->{
            menu.broadcastChanges();var output=table.getItem(0);
            h.assertTrue(output.is(ModItems.ENHANCED_RESONANCE_TERMINAL.get())&&ResonanceTerminalItem.capacity(output)==6,"Condensation did not produce enhanced terminal");
            h.assertTrue(ResonanceTerminalItem.data(output).equals(original)&&p.getData(ModAttachments.MANA).current()==200,"Upgrade lost identity/links or charged wrong mana");
            table.setItem(0,ItemStack.EMPTY);p.getInventory().setItem(0,output);
            var terminalMenu=new ResonanceMenu(2,p.getInventory(),0);
            h.assertTrue(terminalMenu.clickMenuButton(p,0)&&terminalMenu.kind()==2,"Preserved link cannot access original node");
            terminalMenu.removed(p);menu.removed(p);h.succeed();
        });
    }
}
