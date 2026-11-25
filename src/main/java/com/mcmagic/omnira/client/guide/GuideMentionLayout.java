package com.mcmagic.omnira.client.guide;

import java.util.*;
import net.minecraft.world.item.Item;

/** Keep the icon with the start of a name; the rest follows ordinary prose wrapping. */
public final class GuideMentionLayout {
    private static final int FIRST=0xF0000;
    record Token(String spelling,String fullName,Item item){}
    private final List<Token> tokens=new ArrayList<>();
    private final Map<Token,Integer> ids=new HashMap<>();
    private final java.util.function.ToIntFunction<String> widthOf;
    private final GuideItemNames names;
    private final com.ibm.icu.text.BreakIterator breaks=com.ibm.icu.text.BreakIterator.getLineInstance(Locale.ROOT);
    public GuideMentionLayout(GuideItemNames names,java.util.function.ToIntFunction<String> widthOf){this.names=names;this.widthOf=widthOf;}
    Token token(int cp){int i=cp-FIRST;return i>=0 && i<tokens.size()?tokens.get(i):null;}
    public String encode(String text,int width){
        var out=new StringBuilder();
        for(int i=0;i<text.length();){
            if(i==0 && text.startsWith("{dream}")){out.append("{dream}");i+=7;continue;}
            if(text.charAt(i)=='§' && i+1<text.length()){out.append(text,i,i+2);i+=2;continue;}
            var name=names.match(text,i);
            if(name==null){int cp=text.codePointAt(i);out.appendCodePoint(cp);i+=Character.charCount(cp);continue;}
            String spelling=text.substring(i,i+name.text().length());
            breaks.setText(spelling);
            String prefix=spelling.substring(0,breaks.next()).stripTrailing();
            while(widthOf.applyAsInt("  "+prefix)>width && prefix.codePointCount(0,prefix.length())>1){
                prefix=prefix.substring(0,prefix.offsetByCodePoints(prefix.length(),-1));
            }
            var key=new Token(prefix,spelling,name.item());
            int cp=ids.computeIfAbsent(key,k->{tokens.add(k);return FIRST+tokens.size()-1;});
            out.appendCodePoint(cp).append(spelling,prefix.length(),spelling.length());
            i+=spelling.length();
        }
        return out.toString();
    }
    String expand(String encoded,boolean markers){
        var out=new StringBuilder();
        encoded.codePoints().forEach(cp->{
            var t=token(cp);
            if(t==null)out.appendCodePoint(cp);
            else{
                if(markers)out.appendCodePoint(cp);else out.append("  ");
                out.append(t.spelling);
            }
        });
        return out.toString();
    }
    public int measure(String encoded){return widthOf.applyAsInt(expand(encoded,false));}
    public String plain(String encoded){
        var out=new StringBuilder();
        encoded.codePoints().forEach(cp->{var t=token(cp);if(t==null)out.appendCodePoint(cp);else out.append(t.spelling);});
        return out.toString();
    }
}
