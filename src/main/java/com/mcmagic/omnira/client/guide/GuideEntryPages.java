package com.mcmagic.omnira.client.guide;

/** One shared order for drawing, navigation and item hit-testing. */
public record GuideEntryPages(int text,int scenes,int figures,int recipes) {
    public GuideEntryPages {
        if(text<0||scenes<0||figures<0||recipes<0)throw new IllegalArgumentException("Negative guide page count");
    }
    public int figureStart(){return text+scenes;}
    public int recipeStart(){return figureStart()+figures;}
    public int linkStart(){return recipeStart()+recipes;}
}
