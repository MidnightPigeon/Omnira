package com.mcmagic.omnira.client.screen;

/** The book retains its original 320x224 pages; the detached search row occupies the space below. */
public record GuideBookLayout(int left,int top) {
    public static final int WIDTH=320,BOOK_HEIGHT=224,HEIGHT=256;
    public static final int SEARCH_Y=230,INPUT_X=18,INPUT_WIDTH=198,FIND_X=222,FIND_WIDTH=80,CONTROL_HEIGHT=20;
    public static GuideBookLayout centered(int width,int height){return new GuideBookLayout((width-WIDTH)/2,(height-HEIGHT)/2);}
}
