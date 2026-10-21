package com.mcmagic.omnira.compat.jei;

/** Recipe-local bounds shared by vanilla buttons, tooltips and input hit testing. */
public final class EvolutionNavigation {
    public static final int WIDTH=174,HEIGHT=156;
    public record Bounds(int x,int y,int width,int height){
        public boolean contains(double mx,double my){return mx>=x&&mx<x+width&&my>=y&&my<y+height;}
    }
    public static Bounds button(boolean stages,int direction){return new Bounds(direction<0?6:148,stages?94:133,20,18);}
    private EvolutionNavigation(){}
}
