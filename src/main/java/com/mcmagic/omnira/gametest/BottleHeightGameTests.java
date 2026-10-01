package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.item.bottle.StructureSections;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_bottle_height")
@PrefixGameTestTemplate(false)
public final class BottleHeightGameTests {
    @GameTest(template="spell_arena")
    public static void roundTrip(GameTestHelper h) {
        var ship=new CompoundTag();var plot=new CompoundTag();var chunks=new CompoundTag();
        var chunk=new CompoundTag();var sections=new CompoundTag();var section=new CompoundTag();
        section.putString("payload","blocks-and-light");sections.put("8",section);chunk.put("sections",sections);
        var entities=new ListTag();var chest=new CompoundTag();chest.putInt("y",65);entities.add(chest);chunk.put("block_entities",entities);
        chunks.put("0",chunk);plot.put("chunks",chunks);ship.put("plot",plot);
        var pose=new CompoundTag();var pivot=new CompoundTag();pivot.putDouble("y",65);pose.put("rotation_point",pivot);ship.put("pose",pose);
        var original=ship.copy();
        h.assertTrue(StructureSections.containsPivot(ship,-4),"Legacy overworld height not recognized");
        h.assertTrue(!StructureSections.containsPivot(ship,0),"Legacy height inference is ambiguous");
        StructureSections.remap(ship,-4,0,16);
        h.assertTrue(chunk.getCompound("sections").getCompound("4").equals(section),"Overworld to dream section shifted incorrectly");
        h.assertTrue(chest.getInt("y")==65,"Block entity coordinates changed");
        StructureSections.remap(ship,0,-4,24);
        h.assertTrue(ship.equals(original),"Round trip lost structure data");
        boolean rejected=false;
        try {StructureSections.remap(ship,-4,8,1);} catch(IllegalArgumentException expected) {rejected=true;}
        h.assertTrue(rejected,"Out-of-range structure was accepted");
        h.succeed();
    }
}
