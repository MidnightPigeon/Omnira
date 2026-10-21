package com.mcmagic.omnira.client.guide;

import java.util.List;

/** Only the directly opened result returns to search; descendants keep normal directory navigation. */
public final class GuideSearchSession {
    public record Position(String destination,int page) {}
    private final GuideContents contents;
    private GuideTextSearch index;
    private Position origin;
    private String query="",root="";
    private List<GuideTextSearch.Hit> hits=List.of();
    private boolean results;
    private int page;
    public GuideSearchSession(GuideContents contents){this.contents=contents;}
    public boolean active(){return origin!=null;}
    public boolean results(){return active()&&results;}
    public String query(){return query;}
    public List<GuideTextSearch.Hit> hits(){return hits;}
    public int page(){return page;}
    public int totalPages(){return Math.max(2,(hits.size()+5)/6);}
    public boolean start(String text,Position before){
        if(active()||GuideTextSearch.normalize(text).isEmpty())return false;
        if(index==null)index=new GuideTextSearch(contents);
        origin=before;query=text;hits=index.find(text);results=true;page=0;root="";return true;
    }
    public void turn(boolean forward){
        if(results()&&(forward?GuideSpread.hasNext(page,totalPages()):page>0))page=GuideSpread.turn(page,forward);
    }
    public void open(GuideTextSearch.Hit hit){
        if(!active()||!hits.contains(hit))throw new IllegalArgumentException("Not a current search result");
        root=hit.id();results=false;
    }
    public boolean returnsToResults(String destination){return active()&&!results&&root.equals(destination);}
    public boolean back(String destination){
        if(!returnsToResults(destination))return false;
        results=true;return true;
    }
    public Position stop(){
        var before=origin;origin=null;query="";root="";hits=List.of();results=false;page=0;return before;
    }
}
