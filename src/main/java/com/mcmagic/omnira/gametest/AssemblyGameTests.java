package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.block.entity.ArcaneAssemblyTableBlockEntity;
import com.mcmagic.omnira.mana.ManaState;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.common.util.FakePlayer;
import com.mojang.authlib.GameProfile;
import java.util.UUID;

@GameTestHolder("omnira")
@PrefixGameTestTemplate(false)
public final class AssemblyGameTests {
    @GameTest(template="spell_arena",timeoutTicks=240)
    public static void assemblyChargesPerStrikeAndCreatesOneStaff(GameTestHelper h) {
        h.setBlock(4,2,4,ModBlocks.ARCANE_ASSEMBLY_TABLE.get());
        var table=((com.mcmagic.omnira.block.entity.AssemblyAccess)h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(4,2,4)))).assembly();
        var player=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"assembly-test"));
        player.setPos(h.absoluteVec(new Vec3(4.5,2,6.5)));
        player.setData(ModAttachments.MANA,new ManaState(2000,2000));
        h.assertTrue(!table.strike(player),"Empty recipe accepted");
        table.setItem(3,new ItemStack(ModItems.WOODEN_STAFF_SHAFT.get()));
        table.setItem(6,new ItemStack(ModItems.IRON_REINFORCEMENT.get()));
        table.setItem(0,new ItemStack(ModItems.SPIRITUAL_CRYSTAL_TIP.get()));
        h.assertTrue(table.recipe()!=null,"Diagonal recipe not loaded");
        player.setData(ModAttachments.MANA,new ManaState(49,2000));
        h.assertTrue(!table.strike(player) && table.progress()==0,"Unaffordable strike mutated work");
        player.setData(ModAttachments.MANA,new ManaState(2000,2000));
        h.assertTrue(table.strike(player),"Valid strike rejected");
        h.assertTrue(table.progress()==5 || table.progress()==10,"Invalid random increment");
        h.assertTrue(player.getData(ModAttachments.MANA).current()==1950,"Wrong strike cost");
        h.assertTrue(!table.strike(player),"Duplicate same-tick strike accepted");
        var saved=table.saveWithFullMetadata(h.getLevel().registryAccess());
        table.loadWithComponents(saved,h.getLevel().registryAccess());
        h.assertTrue(table.progress()>0,"Progress did not persist");
        table.removeItem(3,1);
        h.assertTrue(table.progress()==0,"Removing component kept progress");
        table.setItem(3,new ItemStack(ModItems.WOODEN_STAFF_SHAFT.get()));
        for(int i=1;i<=20;i++) h.runAtTickTime(i*ArcaneAssemblyTableBlockEntity.STRIKE_COOLDOWN,()->{
            if(!table.finished()) h.assertTrue(table.strike(player),"Valid continued strike rejected");
        });
        h.runAtTickTime(205,()->{
            h.assertTrue(table.finished() && table.progress()==100,"Did not complete within twenty strikes");
            h.assertTrue(table.getItem(6).is(ModItems.MODULAR_STAFF.get()) && table.getItem(6).getCount()==1,"Wrong output");
            for(int i=0;i<6;i++) h.assertTrue(table.getItem(i).isEmpty(),"Unconsumed input");
            h.assertTrue(!table.strike(player),"Completed recipe charged again");
            table.removeItem(6,1);
            h.assertTrue(!table.finished() && table.progress()==0,"Output extraction did not reset");
            h.succeed();
        });
    }
}
