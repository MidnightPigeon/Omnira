package com.mcmagic.omnira.gametest;

import com.google.gson.JsonParser;
import com.mcmagic.omnira.client.guide.*;
import java.net.URI;
import java.util.*;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_guide_contact")
@PrefixGameTestTemplate(false)
public final class GuideContactGameTests {
    @GameTest(template="spell_arena") public static void contactOrderAndLinks(GameTestHelper h) throws Exception {
        for(var language:List.of("zh_cn","en_us")){
            var book=GuideContents.fromJson(GuidanceGameTests.guide(language));var chapters=book.chapters();
            h.assertTrue(chapters.getLast().id().equals("cross_worlds")&&chapters.stream().noneMatch(c->c.id().equals("contact_author")),"Contact escaped the cross-world chapter");
            var contact=chapters.getLast().chapters().getLast();var intro=contact.introduction();
            h.assertTrue(contact.id().equals("contact_author")&&book.destination("contact_author").orElseThrow().path().size()==2,"Contact bookmark does not resolve to its nested chapter");
            h.assertTrue(contact.entries().isEmpty()&&contact.chapters().isEmpty()&&!intro.hasArtwork(),"Contact created a redundant directory or artwork placeholder");
            h.assertTrue(intro.externalLinks().stream().map(link->link.uri().toString()).toList().equals(List.of(
                    "https://github.com/MidnightPigeon/Omnira","https://midnightpigeon.github.io/")),"Incorrect contact URLs");
            h.assertTrue(intro.linesPerPage()==10,"Contact prose overlaps bottom links");
            for(int width:new int[]{88,124,160}){
                var pages=GuideTextLayout.paginate(intro.paragraphs(),width,intro.linesPerPage(),s->s.codePoints().map(c->Character.isIdeographic(c)?9:c==' '?4:6).sum());
                h.assertTrue(!pages.isEmpty()&&pages.stream().allMatch(page->page.size()<=10),"Contact layout overflow");
                h.assertTrue(GuideSpread.directoryTotal(0,pages.size())==pages.size(),"Contact created an empty directory page");
            }
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void rejectUnsafeResourcePackLinks(GameTestHelper h){
        for(var value:List.of("javascript:alert(1)","file:///C:/secret.txt","data:text/plain,test","http://example.com/",
                "//example.com/","https:/example.com","https://user:password@example.com/")){
            boolean rejected=false;try{new GuideExternalLink("Test",URI.create(value));}catch(IllegalArgumentException expected){rejected=true;}
            h.assertTrue(rejected,"Accepted unsafe external link: "+value);
        }
        h.assertTrue(new GuideExternalLink("Site",URI.create("https://example.com/path#contact")).uri().getScheme().equals("https"),"Valid HTTPS link rejected");
        boolean rejected=false;try{GuideExternalLink.read(JsonParser.parseString("[{},{},{}]").getAsJsonArray());}catch(IllegalArgumentException expected){rejected=true;}
        h.assertTrue(rejected,"More than two links can overlap the footer");
        var legacy=new GuideContents.Introduction("",112,88,"",List.of("Text"),List.of());
        h.assertTrue(legacy.externalLinks().isEmpty()&&legacy.linesPerPage()==12,"Old introductions changed pagination");h.succeed();
    }
    @GameTest(template="spell_arena") public static void sourceSeriesRemainConciseAndAttributed(GameTestHelper h) throws Exception {
        var expected=Map.of("omnira:phantom_thieves_aggregate","女神异闻录5","omnira:umbrella_aggregate","生化危机",
                "omnira:foundation_aggregate","SCP基金会","omnira:primordial_earth_aggregate","地球Online（2026版）");
        for(var language:List.of("zh_cn","en_us")){
            var book=GuideContents.fromJson(GuidanceGameTests.guide(language));
            var sources=book.destination("item_sources").orElseThrow().chapter().introduction().sources();
            h.assertTrue(sources.size()==20,"Series were not consolidated");
            for(var source:sources){
                h.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(net.minecraft.resources.ResourceLocation.parse(source.icon())),"Unregistered source icon: "+source.icon());
                if(expected.containsKey(source.icon())&&language.equals("zh_cn"))
                    h.assertTrue(source.origin().equals(expected.get(source.icon()))&&source.name().endsWith("聚合体及相关衍生物"),"Aggregate attribution or concise naming changed");
                if(source.icon().equals("omnira:crystallized_bone_nail")||source.icon().equals("omnira:arcane_needle"))
                    h.assertTrue(source.name().endsWith(language.equals("zh_cn")?"及其升级版":"and its upgrades"),"Weapon series lists every upgrade");
            }
            h.assertTrue(sources.stream().filter(s->expected.containsKey(s.icon())).count()==4,"Aggregate series disappeared");
        }
        h.succeed();
    }
    private GuideContactGameTests(){}
}
