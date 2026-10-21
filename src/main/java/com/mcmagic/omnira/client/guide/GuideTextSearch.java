package com.mcmagic.omnira.client.guide;

import java.text.Normalizer;
import java.util.*;

/** Visible authored text is indexed once per open book, independently of its current page. */
public final class GuideTextSearch {
    public record Hit(String id,String title,String icon,String path,GuideContents.Destination destination) {}
    private record Document(Hit hit,String text) {}
    private final List<Document> documents=new ArrayList<>();
    public GuideTextSearch(GuideContents contents){
        add(new Hit("",contents.title(),"omnira:guide_book",contents.title(),null),contents.title()+"\n"+introduction(contents.introduction()));
        for(var chapter:contents.chapters())index(chapter,List.of());
    }
    private void index(GuideContents.Chapter chapter,List<GuideContents.Chapter> parents){
        var path=new ArrayList<>(parents);path.add(chapter);
        String breadcrumb=String.join(" / ",path.stream().map(GuideContents.Chapter::title).toList());
        add(new Hit(chapter.id(),chapter.title(),chapter.icon(),breadcrumb,new GuideContents.Destination(List.copyOf(path),null)),chapter.title()+"\n"+introduction(chapter.introduction()));
        for(var row:chapter.directory()){
            if(row.chapter()!=null)index(row.chapter(),path);
            else {
                var entry=row.entry();var text=new StringBuilder(entry.title());
                entry.text().forEach(p->text.append('\n').append(prose(p)));
                for(var figure:entry.figures()){
                    text.append('\n').append(figure.title());
                    figure.labels().forEach(label->text.append('\n').append(label.text()));
                }
                entry.links().forEach(link->text.append('\n').append(link.label()));
                add(new Hit(entry.id(),entry.title(),entry.icon(),breadcrumb+" / "+entry.title(),new GuideContents.Destination(List.copyOf(path),entry)),text.toString());
            }
        }
    }
    private static String introduction(GuideContents.Introduction intro){
        var text=new StringBuilder(intro.hasArtwork()?intro.caption():prose(intro.caption()));intro.text().forEach(p->text.append('\n').append(prose(p)));
        intro.sources().forEach(source->text.append('\n').append(source.name()).append("\u2014\u2014").append(source.origin()));
        intro.externalLinks().forEach(link->text.append('\n').append(link.label()));
        return text.toString();
    }
    private void add(Hit hit,String text){documents.add(new Document(hit,normalize(text)));}
    private static String prose(String text){
        String marker=com.mcmagic.omnira.mana.DreamText.MARKER;
        return text.startsWith(marker)?text.substring(marker.length()):text;
    }
    public static String normalize(String text){
        return Normalizer.normalize(text,Normalizer.Form.NFKC).toLowerCase(Locale.ROOT).replaceAll("(?U)\\s+"," ").strip();
    }
    public List<Hit> find(String query){
        String needle=normalize(query);if(needle.isEmpty())return List.of();
        return documents.stream().filter(document->document.text().contains(needle)).map(Document::hit).toList();
    }
}
