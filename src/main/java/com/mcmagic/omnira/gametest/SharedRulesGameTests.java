package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.item.OrbUpgradeItem.Kind;
import com.mcmagic.omnira.item.staff.StaffUseClick;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("omnira_shared_rules")
@PrefixGameTestTemplate(false)
public final class SharedRulesGameTests {
    @GameTest(template="spell_arena") public static void physicalClicksDoNotRepeat(GameTestHelper h) {
        var click=new StaffUseClick();
        h.assertTrue(!click.consume(),"Unpressed use cast");
        click.begin();h.assertTrue(click.consume() && !click.consume(),"One click must cast exactly once");
        for(int i=0;i<100;i++){click.begin();h.assertTrue(!click.consume(),"Held use repeated casting");}
        click.release();click.begin();h.assertTrue(click.consume(),"New click did not cast");
        click.release();click.begin(); // A block or book takes this click instead.
        click.begin();h.assertTrue(!click.consume(),"Consumed interaction leaked into held use");
        click.release();h.assertTrue(!click.consume(),"Release left stale pending use");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void upgradeFamiliesAreConsistent(GameTestHelper h) {
        for(var a:Kind.values())for(var b:Kind.values()) {
            h.assertTrue(a.sameFamily(b)==b.sameFamily(a),"Asymmetric upgrade family");
            h.assertTrue(a.sameFamily(b)==(a==b || a.capacityMultiplier>1 && b.capacityMultiplier>1),"Unexpected family conflict");
        }
        h.assertTrue(Kind.STACKING.capacityMultiplier==2 && Kind.INFUSED_STACKING.capacityMultiplier==4,"Capacity rules changed");
        h.succeed();
    }
}
