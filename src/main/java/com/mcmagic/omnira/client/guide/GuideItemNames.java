package com.mcmagic.omnira.client.guide;

import java.util.*;
import net.minecraft.world.item.*;

/** Longest registered name wins. Only installed items can produce a guide icon. */
public final class GuideItemNames {
    record Name(String text, Item item, String before, String after) {
        boolean fits(String prose,int start,int end){
            return prose.regionMatches(true,start-before.length(),before,0,before.length())
                    && prose.regionMatches(true,end,after,0,after.length());
        }
    }
    private final Map<Character,List<Name>> byInitial=new HashMap<>();
    public GuideItemNames(Map<String,Item> names){
        names.forEach((text,item)->{
            if(item!=Items.AIR)byInitial.computeIfAbsent(Character.toLowerCase(text.charAt(0)),k->new ArrayList<>()).add(new Name(text,item,"",""));
        });
        byInitial.values().forEach(list->list.sort(Comparator.comparingInt((Name n)->n.text.length()).reversed()));
    }
    public void addContext(String expression,Item item){
        int start=expression.indexOf('['), end=expression.indexOf(']');
        if(start<0 || end<=start+1)throw new IllegalArgumentException("Invalid guide item context: "+expression);
        String label=expression.substring(start+1,end);
        var options=byInitial.computeIfAbsent(Character.toLowerCase(label.charAt(0)),k->new ArrayList<>());
        options.add(new Name(label,item,expression.substring(0,start),expression.substring(end+1)));
        options.sort(Comparator.comparingInt((Name n)->n.text.length()).reversed());
    }
    private static boolean word(char ch){return ch<128 && (Character.isLetterOrDigit(ch)||ch=='_');}
    Name match(String text,int i){
        var options=byInitial.get(Character.toLowerCase(text.charAt(i)));
        if(options==null)return null;
        for(var name:options){
            int end=i+name.text.length();
            if(!text.regionMatches(true,i,name.text,0,name.text.length()))continue;
            if(word(text.charAt(i)) && i>0 && word(text.charAt(i-1)))continue;
            if(word(text.charAt(end-1)) && end<text.length() && word(text.charAt(end)))continue;
            if(!name.fits(text,i,end))continue;
            return name;
        }
        return null;
    }
}
