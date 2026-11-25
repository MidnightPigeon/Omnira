package com.mcmagic.omnira.client.guide;

/** One shared order for drawing, navigation and item hit-testing. */
public record GuideEntryPages(int text,int scenes,int figures,int recipes) {
    public GuideEntryPages {
        if(text<0||scenes<0||figures<0||recipes<0)throw new IllegalArgumentException("Negative guide page count");
    }
    public int figureStart(){return text+scenes;}
    public int recipeStart(){return figureStart()+figures;}
    public int linkStart(){return recipeStart()+recipes;}
    // Two links fit below the last text page; only the remainder needs dedicated pages.
    public static final int FOOTER_LINKS=2, LINKS_PER_PAGE=6;
    public static int extraLinkPages(int links){return Math.ceilDiv(Math.max(0,links-FOOTER_LINKS),LINKS_PER_PAGE);}
    public static int firstExtraLink(int page){return FOOTER_LINKS+page*LINKS_PER_PAGE;}
}
