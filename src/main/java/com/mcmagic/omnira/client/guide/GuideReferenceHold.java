package com.mcmagic.omnira.client.guide;

import java.util.Objects;

/** A single uninterrupted hover, followed by a mandatory key release. */
public final class GuideReferenceHold {
    public static final int TICKS=20;
    private Object target;
    private int ticks;
    private boolean waitRelease;
    public boolean tick(Object next,boolean down){
        if(!down){waitRelease=false;target=null;ticks=0;return false;}
        if(waitRelease)return false;
        if(next==null){target=null;ticks=0;return false;}
        if(!Objects.equals(target,next)){target=next;ticks=0;}
        if(++ticks<TICKS)return false;
        ticks=0;waitRelease=true;return true;
    }
    public int percent(Object hovered){return Objects.equals(target,hovered)?ticks*100/TICKS:0;}
}
