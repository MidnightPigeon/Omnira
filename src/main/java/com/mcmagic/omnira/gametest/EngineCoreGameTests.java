package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.block.entity.*;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_engine_core")
@PrefixGameTestTemplate(false)
public final class EngineCoreGameTests {
    @GameTest(template="spell_arena") public static void hexPanelSlots(GameTestHelper h) throws Exception {
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var menu=new com.mcmagic.omnira.menu.ManaEngineMenu(1,player.getInventory(),engine(h));
        try(var stream=EngineCoreGameTests.class.getResourceAsStream("/assets/omnira/textures/gui/container/workstation_engine.png")){
            var image=javax.imageio.ImageIO.read(java.util.Objects.requireNonNull(stream));
            for(int i=0;i<4;i++){
                var slot=menu.getSlot(i);int color=image.getRGB(slot.x,slot.y);
                h.assertTrue(slot.getContainerSlot()==i,"Engine material slot changed meaning");
                for(int x=0;x<16;x++)for(int y=0;y<16;y++)
                    h.assertTrue(image.getRGB(slot.x+x,slot.y+y)==color,"Engine slot and texture disagree");
                h.assertTrue(image.getRGB(slot.x-1,slot.y)!=color && image.getRGB(slot.x+16,slot.y)!=color,"Engine slot border offset");
                h.assertTrue(slot.y>=28 && slot.y+16<122,"Engine slot overlaps scroll or controls");
            }
        }
        for(int i=0;i<36;i++){
            var slot=menu.getSlot(4+i);
            h.assertTrue(slot.x==8+(i%9)*18 && slot.y==(i<27?158+i/9*18:216),"Engine player inventory moved");
        }
        h.succeed();
    }
    private static ManaEngineState engine(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(5,2,5));h.getLevel().setBlockAndUpdate(pos,ModBlocks.MANA_ENGINE.get().defaultBlockState());
        var state=((ManaEngineAccess)h.getLevel().getBlockEntity(pos)).engineState();state.redstoneControl=false;return state;
    }
    @GameTest(template="spell_arena") public static void coresDriveBaseAndLatticeOutputs(GameTestHelper h) {
        var e=engine(h);
        for(var item:java.util.List.of(ModItems.TEST_SPELL_CORE.get(),ModItems.DREAM_SPELL_CORE.get(),ModItems.LIGHT_DARK_SPELL_CORE.get(),ModItems.SPACETIME_SPELL_CORE.get())) {
            int base=item==ModItems.TEST_SPELL_CORE.get()?512:1024;
            for(int slot=1;slot<4;slot++)e.inventory.setItem(slot,ItemStack.EMPTY);
            var core=new ItemStack(item);h.assertTrue(ManaEngineState.accepts(0,core),"Core rejected");e.inventory.setItem(0,core);
            h.assertTrue(e.ratedCapacity()==base && e.ratedEnergy()==base && e.running(),"Wrong core base output");
            for(int slot=1;slot<4;slot++)e.inventory.setItem(slot,new ItemStack(ModItems.OMNI_CRYSTAL_GRID.get()));
            h.assertTrue(e.ratedCapacity()==base*64 && e.ratedEnergy()==base*64,"Lattice multipliers use wrong core base");
            if(e.host instanceof ManaEngineBlockEntity kinetic)
                h.assertTrue(kinetic.calculateAddedStressCapacity()*16==base*64,"Actual Create stress differs from rating");
            for(int slot=1;slot<4;slot++)e.inventory.setItem(slot,new ItemStack(ModItems.BASIC_CRYSTAL_GRID.get()));
            h.assertTrue(e.ratedCapacity()==Math.round(base*1.2*1.2*1.2),"Basic lattices must boost output, rounding only once");
            e.inventory.setItem(2,new ItemStack(ModItems.ELEMENTAL_CRYSTAL_GRID.get()));
            e.inventory.setItem(3,new ItemStack(ModItems.ARCANE_CRYSTAL_GRID.get()));
            h.assertTrue(e.ratedCapacity()==Math.round(base*1.2*1.5*2) && e.ratedEnergy()==e.ratedCapacity(),"Mixed fractional lattice output wrong");
        }
        h.assertTrue(!ManaEngineState.accepts(0,new ItemStack(Items.DIAMOND)),"Non-core accepted");
        e.inventory.setItem(0,ItemStack.EMPTY);
        h.assertTrue(e.ratedCapacity()==0 && e.ratedEnergy()==0 && !e.running(),"Core removal did not stop engine");h.succeed();
    }
    @GameTest(template="spell_arena") public static void energyAndSavedCore(GameTestHelper h) {
        var e=engine(h);e.inventory.setItem(0,new ItemStack(ModItems.DREAM_SPELL_CORE.get()));
        var tag=new net.minecraft.nbt.CompoundTag();e.write(tag,h.getLevel().registryAccess());
        e.inventory.setItem(0,ItemStack.EMPTY);e.read(tag,h.getLevel().registryAccess());
        h.assertTrue(e.ratedEnergy()==1024,"Dream core lost on reload");
        if(com.mcmagic.omnira.energy.EnergyIntegration.available()) {
            var port=e.energyPort(e.host.getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING));
            h.assertTrue(port!=null && port.getMaxEnergyStored()==1024,"Wrong electrical capacity");
            int amount=port.extractEnergy(Integer.MAX_VALUE,false);
            h.assertTrue(amount==51 || amount==52,"Wrong electrical tick rate");
            h.assertTrue(port.extractEnergy(Integer.MAX_VALUE,false)==0,"Duplicated same-tick power");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void menuPreservesIndependent32BitOutputs(GameTestHelper h) {
        var p=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"engine-menu"));
        var buf=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());
        try {
            buf.writeBlockPos(h.absolutePos(new BlockPos(5,2,5)));
            var menu=new com.mcmagic.omnira.menu.ManaEngineMenu(1,p.getInventory(),buf);
            int stress=128000,fe=192000;
            menu.setData(3,(short)stress);menu.setData(7,(short)(stress>>>16));
            menu.setData(8,(short)fe);menu.setData(9,(short)(fe>>>16));
            h.assertTrue(menu.capacity()==stress && menu.energyPerSecond()==fe,"Menu truncated or conflated independent outputs");
        } finally {buf.release();}
        h.succeed();
    }
}
