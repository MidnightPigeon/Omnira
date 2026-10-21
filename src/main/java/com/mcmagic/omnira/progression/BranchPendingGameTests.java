package com.mcmagic.omnira.progression;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_branches") @PrefixGameTestTemplate(false)
public final class BranchPendingGameTests {
    @GameTest(template="spell_arena") public static void offlineObjectivesDeduplicatePersistAndDeliver(GameTestHelper h){
        var profile=new GameProfile(UUID.randomUUID(),"offline-branch");
        var player=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),profile,ClientInformation.createDefault());
        player.connection=new FakePlayer(h.getLevel(),profile).connection;
        var data=new BranchPending();for(int i=0;i<100;i++)data.add(player.getUUID(),"first_reaction","performed");
        var saved=data.save(new CompoundTag(),h.getLevel().registryAccess());
        h.assertTrue(saved.getList(player.getUUID().toString(),8).size()==1,"Repeated automation grew the offline ledger");
        var restored=BranchPending.load(saved,h.getLevel().registryAccess());restored.deliver(player);
        var advancement=player.server.getAdvancements().get(ResourceLocation.fromNamespaceAndPath("omnira","branches/first_reaction"));
        var progress=player.getAdvancements().getOrStartProgress(advancement);
        h.assertTrue(progress.getCriterion("performed").isDone()&&!progress.isDone(),"Offline objective failed or bypassed its parent");
        h.assertTrue(restored.save(new CompoundTag(),h.getLevel().registryAccess()).isEmpty(),"Delivered offline records were retained");h.succeed();
    }
}
