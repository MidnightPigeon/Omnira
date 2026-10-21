package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.spell.world.ReloadEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;
import java.nio.file.*;
import java.util.*;

/** Registry-based audit uses the same default-state eligibility as the actual reload pool. */
@GameTestHolder("omnira_reload_audit") @PrefixGameTestTemplate(false)
public final class ReloadBlockAuditGameTests {
    @GameTest(template="spell_arena") public static void exportCompleteBlockCandidates(GameTestHelper h){
        var rows=new ArrayList<String>();
        var defaults=com.mcmagic.omnira.config.OmniraConfig.RELOAD_BLOCK_BLACKLIST.getDefault();
        rows.add("id\tconfiguredBlocked\tdefaultBlocked\titem\titemClass\tblockClass\trender\tmatchingBlockItem\tstate");
        int candidates=0,excluded=0;var unsafe=new ArrayList<String>();
        for(var block:BuiltInRegistries.BLOCK){
            var state=block.defaultBlockState();if(!ReloadEffect.completeBlock(state,h.getLevel(),BlockPos.ZERO))continue;
            candidates++;var item=block.asItem();boolean matching=item instanceof BlockItem placed&&placed.getBlock()==block;
            var id=BuiltInRegistries.BLOCK.getKey(block).toString();boolean denied=defaults.contains(id);if(denied)excluded++;
            boolean special=!matching||item.getClass().getName().equals("com.simibubi.create.content.decoration.bracket.BracketBlockItem");
            if(special&&!denied)unsafe.add(id);
            var row=id+"\t"+ReloadEffect.blocked(block)+"\t"+denied+"\t"+BuiltInRegistries.ITEM.getKey(item)
                    +"\t"+item.getClass().getName()+"\t"+block.getClass().getName()+"\t"+state.getRenderShape()+"\t"+matching+"\t"+state;
            rows.add(row);
            if(special)com.mcmagic.omnira.Omnira.LOGGER.info("Reload audit special item: {}",row);
        }
        try{
            var path=Path.of("../../build/reload-block-audit.tsv").toAbsolutePath().normalize();
            Files.createDirectories(path.getParent());Files.write(path,rows);
            com.mcmagic.omnira.Omnira.LOGGER.info("Reload audit: {} registry blocks, {} default-state candidates, {} default-blacklisted, {} remaining; {}",BuiltInRegistries.BLOCK.size(),candidates,excluded,candidates-excluded,path);
        }catch(java.io.IOException exception){throw new IllegalStateException("Cannot save reload audit",exception);}
        h.assertTrue(candidates>0,"Reload registry scan is empty");
        h.assertTrue(unsafe.isEmpty(),"Unreviewed internal/special-placement candidates: "+unsafe);h.succeed();
    }
    private ReloadBlockAuditGameTests(){}
}
