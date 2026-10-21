package com.mcmagic.omnira.client.guide;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

/** Resource-pack diagrams use the same fixed content area as ordinary book text. */
public record GuideFigure(String title,List<Icon> icons,List<Stroke> strokes,List<Area> areas,List<Label> labels) {
    public static final int WIDTH=124,HEIGHT=132;
    public record Icon(String item,int x,int y,int count) {}
    public record Stroke(int x1,int y1,int x2,int y2,int color,boolean arrow) {}
    public record Area(int x,int y,int width,int height,int color) {}
    public record Label(String text,int x,int y,int width,int height,int color) {}
    private static int number(JsonObject o,String key){return o.get(key).getAsInt();}
    private static int color(JsonObject o){return o.has("color")?(int)(0xFF000000L|Long.parseLong(o.get("color").getAsString().replace("#",""),16)):0xFF405462;}
    private static void bounds(int x,int y,int w,int h) {
        if(x<0||y<0||w<1||h<1||x+w>WIDTH||y+h>HEIGHT)throw new IllegalArgumentException("Guide figure outside page: "+x+","+y+","+w+","+h);
    }
    public static List<GuideFigure> read(JsonArray array) {
        if(array==null)return List.of();
        var result=new ArrayList<GuideFigure>();
        for(var value:array) {
            var o=value.getAsJsonObject();
            var icons=new ArrayList<Icon>();var strokes=new ArrayList<Stroke>();var areas=new ArrayList<Area>();var labels=new ArrayList<Label>();
            if(o.has("icons"))for(var v:o.getAsJsonArray("icons")) {
                var a=v.getAsJsonObject();int x=number(a,"x"),y=number(a,"y"),count=a.has("count")?number(a,"count"):1;bounds(x,y,16,16);
                if(count<1||count>99)throw new IllegalArgumentException("Invalid guide icon count");
                icons.add(new Icon(a.get("item").getAsString(),x,y,count));
            }
            if(o.has("strokes"))for(var v:o.getAsJsonArray("strokes")) {
                var a=v.getAsJsonObject();int x=number(a,"x1"),y=number(a,"y1"),x2=number(a,"x2"),y2=number(a,"y2");bounds(x,y,1,1);bounds(x2,y2,1,1);
                strokes.add(new Stroke(x,y,x2,y2,color(a),a.has("arrow")&&a.get("arrow").getAsBoolean()));
            }
            if(o.has("areas"))for(var v:o.getAsJsonArray("areas")) {
                var a=v.getAsJsonObject();int x=number(a,"x"),y=number(a,"y"),w=number(a,"width"),h=number(a,"height");bounds(x,y,w,h);
                areas.add(new Area(x,y,w,h,color(a)));
            }
            if(o.has("labels"))for(var v:o.getAsJsonArray("labels")) {
                var a=v.getAsJsonObject();int x=number(a,"x"),y=number(a,"y"),w=number(a,"width"),h=number(a,"height");bounds(x,y,w,h);
                labels.add(new Label(a.get("text").getAsString(),x,y,w,h,color(a)));
            }
            result.add(new GuideFigure(o.get("title").getAsString(),List.copyOf(icons),List.copyOf(strokes),List.copyOf(areas),List.copyOf(labels)));
        }
        return List.copyOf(result);
    }
}
