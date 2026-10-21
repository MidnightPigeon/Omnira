package com.mcmagic.omnira.client.guide;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/** Keeps catalogue icons and wrapped attribution together within the existing page. */
public final class GuideSourceLayout {
    public record Row(GuideContents.Source source,List<String> lines,int height) {}
    public static List<List<Row>> pages(List<GuideContents.Source> sources,ToIntFunction<String> measure) {
        var pages=new ArrayList<List<Row>>();
        var page=new ArrayList<Row>();
        int used=0;
        for(var source:sources) {
            int width=source.icon().isBlank()?124:104;
            var lines=GuideTextLayout.wrap(source.name()+"\u2014\u2014"+source.origin(),width,measure);
            // Long resource-pack rows may continue on the next leaf without losing text.
            for(int start=0;start<lines.size();start+=11) {
                var part=List.copyOf(lines.subList(start,Math.min(lines.size(),start+11)));
                int height=Math.max(18,part.size()*11)+6;
                if(used+height>132&&!page.isEmpty()) {
                    pages.add(List.copyOf(page));page.clear();used=0;
                }
                page.add(new Row(source,part,height));used+=height;
            }
        }
        if(!page.isEmpty())pages.add(List.copyOf(page));
        return List.copyOf(pages);
    }
    private GuideSourceLayout() {}
}
