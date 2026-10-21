package com.mcmagic.omnira.client.guide;

import com.google.gson.JsonArray;
import java.net.URI;
import java.util.*;

/** Resource-pack links are data only; opening them always requires the vanilla confirmation. */
public record GuideExternalLink(String label,URI uri) {
    public GuideExternalLink {
        if(label==null||label.isBlank()||uri==null||!"https".equalsIgnoreCase(uri.getScheme())
                ||uri.getHost()==null||uri.getUserInfo()!=null)
            throw new IllegalArgumentException("Guide links require a label and an absolute HTTPS URL without credentials");
    }
    public static List<GuideExternalLink> read(JsonArray values){
        if(values==null)return List.of();
        if(values.size()>2)throw new IllegalArgumentException("A guide introduction supports at most two external links");
        var links=new ArrayList<GuideExternalLink>();
        for(var value:values){var link=value.getAsJsonObject();links.add(new GuideExternalLink(link.get("label").getAsString(),URI.create(link.get("url").getAsString())));}
        return List.copyOf(links);
    }
}
