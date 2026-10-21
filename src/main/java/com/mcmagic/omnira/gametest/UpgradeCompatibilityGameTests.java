package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.item.*;
import com.mcmagic.omnira.registry.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.EntityBlock;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("omnira_upgrade_compatibility")
@PrefixGameTestTemplate(false)
public final class UpgradeCompatibilityGameTests {
    @GameTest(template="spell_arena") public static void everyListedHostMatchesRealSlots(GameTestHelper h){
        var upgrades=BuiltInRegistries.ITEM.stream().filter(item->item instanceof OrbUpgradeItem).toList();
        h.assertTrue(upgrades.size()==OrbUpgradeItem.Kind.values().length,"Upgrade types are not completely covered");
        var cruise=ModEntityTypes.CRUISE_ORB.get().create(h.getLevel());
        for(var host:UpgradeCompatibility.Host.values()){
            var block=BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("omnira",host.id));
            var entity=host==UpgradeCompatibility.Host.CRUISE_ORB?null:((EntityBlock)block).newBlockEntity(BlockPos.ZERO,block.defaultBlockState());
            if(entity instanceof com.mcmagic.omnira.block.entity.CrystalBallBlockEntity ball)ball.virtualStorage=true;
            for(var item:upgrades){
                var stack=new ItemStack(item);boolean accepted=false;
                net.neoforged.neoforge.items.IItemHandler slots=null;
                if(host==UpgradeCompatibility.Host.CRUISE_ORB)slots=cruise.storage.upgrades;
                else if(entity instanceof com.mcmagic.omnira.block.entity.CrystalBallBlockEntity ball)slots=ball.upgrades;
                else if(entity instanceof com.mcmagic.omnira.alchemy.ReactionPoolBlockEntity pool)slots=pool.upgrades;
                else if(entity instanceof com.mcmagic.omnira.alchemy.catalysis.CatalysisBlockEntity chamber)slots=chamber.upgrades;
                else {var pulse=com.mcmagic.omnira.pulse.PulseOrbEntity.from(entity);h.assertTrue(pulse!=null,"Unknown upgrade host "+host);accepted=pulse.accepts(stack);}
                if(slots!=null)for(int i=0;i<slots.getSlots();i++)accepted|=slots.isItemValid(i,stack);
                h.assertTrue(accepted==host.accepts(((OrbUpgradeItem)item).kind),"Tooltip disagrees with actual slots: "+host+" / "+item);
            }
            h.assertTrue(!host.accepts(null),"Non-upgrade type accepted");
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void shiftShowsOnlyCompatibleHosts(GameTestHelper h){
        for(var kind:OrbUpgradeItem.Kind.values()){
            var collapsed=new ArrayList<Component>();UpgradeCompatibility.appendTooltip(kind,false,collapsed);
            h.assertTrue(collapsed.size()==1&&collapsed.getFirst().getString().equals(Component.translatable("tooltip.omnira.upgrade.hold_shift").getString()),"Collapsed tooltip did not show only the Shift hint");
            var expanded=new ArrayList<Component>();UpgradeCompatibility.appendTooltip(kind,true,expanded);
            var hosts=UpgradeCompatibility.hosts(kind);
            h.assertTrue(!hosts.isEmpty()&&expanded.size()==hosts.size()+1,"Expanded tooltip has missing or extra entries");
            for(int i=0;i<hosts.size();i++)h.assertTrue(expanded.get(i+1).getString().equals("  "+Component.translatable(hosts.get(i).translationKey()).getString()),"Tooltip contains an incompatible or duplicate host");
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void allHostNamesAndHintsAreTranslated(GameTestHelper h){
        for(var language:List.of("zh_cn","en_us"))try(var input=UpgradeCompatibilityGameTests.class.getResourceAsStream("/assets/omnira/lang/"+language+".json")){
            var json=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(input,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            for(var host:UpgradeCompatibility.Host.values())h.assertTrue(json.has(host.translationKey())&&!json.get(host.translationKey()).getAsString().isBlank(),"Missing host translation: "+language+" / "+host);
            for(var key:List.of("tooltip.omnira.upgrade.hold_shift","tooltip.omnira.upgrade.compatible"))h.assertTrue(json.has(key)&&!json.get(key).getAsString().isBlank(),"Missing tooltip translation: "+language+" / "+key);
        }catch(java.io.IOException error){throw new IllegalStateException(error);}h.succeed();
    }
}
