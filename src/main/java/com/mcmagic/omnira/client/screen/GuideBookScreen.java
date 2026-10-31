package com.mcmagic.omnira.client.screen;

import com.mcmagic.omnira.client.guide.*;
import com.mcmagic.omnira.registry.ModItems;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import java.util.*;

public final class GuideBookScreen extends ResponsiveScreen {
    @Override protected int minimumWidth(){return GuideBookLayout.WIDTH;}
    @Override protected int minimumHeight(){return GuideBookLayout.HEIGHT;}
    private static final int BUTTON_SIZE=20,LEFT_BUTTON=18,RIGHT_BUTTON=282,HEADER_BUTTON_Y=18,FOOTER_BUTTON_Y=190;
    private static final ResourceLocation BOOK=ResourceLocation.fromNamespaceAndPath("omnira","textures/gui/guide_book.png");
    private static final ResourceLocation VANILLA=ResourceLocation.withDefaultNamespace("textures/gui/container/crafting_table.png");
    private final GuideContents contents=GuideContents.load();
    private final GuideSearchSession search=new GuideSearchSession(contents);
    private EditBox searchBox;
    private Button findButton;
    private String searchText="";
    private GuideContents.Chapter chapter;
    private GuideContents.Entry entry;
    private List<List<FormattedCharSequence>> pages=List.of();
    private List<GuideRecipes.Diagram> recipes=List.of();
    private int left,top,introPage,page,ticks;
    private float sceneYaw,scenePitch;
    private boolean rotatingScene;
    private boolean constructionScene=true;
    private ItemStack hovered=ItemStack.EMPTY;
    private Component recipeTooltip=Component.empty();
    private String figureTooltip="";
    private GuideContents.Introduction cachedSourceIntroduction;
    private final GuideFigureRenderer figureRenderer=new GuideFigureRenderer();
    private List<List<GuideSourceLayout.Row>> cachedSourcePages=List.of();
    public GuideBookScreen() {super(Component.translatable("item.omnira.guide_book"));}
    private String initialDestination;
    private int initialPage;
    private boolean restored;
    private Screen returnScreen;
    private net.minecraft.world.inventory.AbstractContainerMenu returnMenu;
    private java.util.function.Consumer<com.mcmagic.omnira.guidance.GuideBookmark> remember;
    private com.mcmagic.omnira.guidance.GuideBookmark lastBookmark;
    public GuideBookScreen(String destination) {this();initialDestination=destination;}
    public GuideBookScreen(String destination,Screen parent){
        this(destination);returnScreen=parent;
        var player=net.minecraft.client.Minecraft.getInstance().player;
        returnMenu=player==null?null:player.containerMenu;
    }
    @Override public void onClose(){
        if(returnScreen!=null&&minecraft.player!=null&&minecraft.player.containerMenu==returnMenu)minecraft.setScreen(returnScreen);
        else super.onClose();
    }
    public GuideBookScreen(String destination,int page,java.util.function.Consumer<com.mcmagic.omnira.guidance.GuideBookmark> remember){
        this(destination);this.initialPage=page;this.remember=remember;
    }
    @Override protected void init() {
        super.init();
        cachedSourceIntroduction=null;
        figureRenderer.clear();
        var layout=GuideBookLayout.centered(width,height);left=layout.left();top=layout.top();
        searchBox=new EditBox(font,left+GuideBookLayout.INPUT_X+4,top+GuideBookLayout.SEARCH_Y+6,GuideBookLayout.INPUT_WIDTH-8,12,Component.translatable("guide.omnira.search_text"));
        searchBox.setMaxLength(256);searchBox.setBordered(false);searchBox.setTextColor(0xFF514333);searchBox.setTextColorUneditable(0xFF706247);
        searchBox.setHint(Component.translatable("guide.omnira.search_text"));searchBox.setValue(searchText);
        searchBox.setResponder(value->{searchText=value;if(findButton!=null)findButton.active=search.active()||!GuideTextSearch.normalize(value).isEmpty();});
        buttons();
        if(!restored){
            if(initialDestination!=null){
                String target=initialDestination;initialDestination=null;var found=contents.destination(target);
                if(found.isPresent())jump(found.get());else if(!target.isEmpty())initialPage=0;
            }
            int saved=Math.min(initialPage,Math.max(0,totalPages()-1))&~1;
            if(entry==null)introPage=saved;else page=saved;
            restored=true;buttons();
        }
    }
    private void select(GuideContents.Entry next) {
        loadEntry(next);
        buttons();
    }
    private void loadEntry(GuideContents.Entry next) {
        entry=next;page=0;sceneYaw=0;scenePitch=0;rotatingScene=false;constructionScene=true;
        pages=textPages(entry.text(),entry.links().isEmpty()?12:10);
        if(pages.isEmpty()) pages=List.of(List.of());
        recipes=GuideEnergy.supports(entry.id())?GuideEnergy.pages(entry.id()):entry.recipes()?GuideRecipes.find(minecraft.level,GuideContents.icon(entry.item()).getItem()):List.of();
    }
    private void jump(GuideContents.Destination target) {
        chapter=target.chapter();entry=null;introPage=0;page=0;
        if(target.entry()!=null) {
            loadEntry(target.entry());
        }
        buttons();
    }
    private void fit(GuiGraphics g,String text,int x,int y,int maxWidth,int color) {
        CrystalUi.label(g,font,Component.literal(text),x,y,maxWidth,1,color);
    }
    private Button row(String name,String item,int index,int pageX,Runnable action) {
        var icon=GuideContents.icon(item);
        var button=addRenderableWidget(new Button(left+pageX-1,top+45+index*23,132,20,Component.literal(name),b->action.run(),supplier->supplier.get()) {
            @Override protected void renderWidget(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) {
                if(isHoveredOrFocused()) graphics.fill(getX(),getY(),getX()+width,getY()+height,0x18745232);
                graphics.fill(getX()+23,getY()+height-1,getX()+width-3,getY()+height,0x307F6E50);
                graphics.renderItem(icon,getX()+3,getY()+2);
                fit(graphics,name,getX()+23,getY()+(getHeight()-font.lineHeight)/2,104,0xFF514333);
            }
        });
        button.setTooltip(Tooltip.create(Component.literal(name)));
        return button;
    }
    private int directorySize() {return chapter==null?contents.chapters().size():chapter.directory().size();}
    private void openChapter(GuideContents.Chapter target) {
        chapter=target;entry=null;introPage=0;page=0;buttons();
    }
    private int scenePages() {return entry!=null && (CodexExampleScene.supports(entry.scene()) || MachineConstructionScene.supports(entry.scene()) || entry.scene().equals("world_forming_ritual") || entry.scene().equals("genesis_ritual") || entry.scene().equals("dream_ritual") || entry.scene().equals("resonance_ritual") || entry.scene().equals("affinity_ritual") || entry.scene().equals("corridor_ritual") || entry.scene().equals("cleansing_ritual") || entry.scene().equals("sword_shaping") || entry.scene().equals("advanced_assembly_table"))?1:0;}
    private void buttons() {
        clearWidgets();
        bookButton(left+RIGHT_BUTTON,top+HEADER_BUTTON_Y,"X","gui.close",this::onClose);
        bookButton(left+150,top+HEADER_BUTTON_Y,"home","guide.omnira.home",this::home);
        if(!search.results()&&(chapter!=null||search.returnsToResults(destination()))) {
            bookButton(left+LEFT_BUTTON,top+HEADER_BUTTON_Y,"<",search.returnsToResults(destination())?"guide.omnira.back_search":"guide.omnira.back",this::back);
        }
        for(int side=0;side<2;side++) {
            if(search.results()){
                int first=(search.page()+side)*6,pageX=side==0?19:175;
                for(int i=0;i<6&&first+i<search.hits().size();i++){
                    var hit=search.hits().get(first+i);
                    row(hit.title(),hit.icon(),i,pageX,()->openSearchResult(hit)).setTooltip(Tooltip.create(Component.literal(hit.path())));
                }
                continue;
            }
            int leaf=GuideSpread.leaf(entry==null?introPage:page,side),pageX=side==0?19:175;
            if(entry!=null){entryButtons(leaf,pageX);continue;}
            externalLinkButtons(leaf,pageX);
            int directoryLeaf=GuideSpread.directoryLeaf(leaf,introductionLeaves());
            if(directoryLeaf<0)continue;
            int count=directorySize();
        for(int index=0;index<6 && directoryLeaf*6+index<count;index++) {
            int absolute=directoryLeaf*6+index;
            if(chapter==null) {
                var target=contents.chapters().get(absolute);
                row(target.title(),target.icon(),index,pageX,()->openChapter(target));
            } else {
                var target=chapter.directory().get(absolute);
                row(target.title(),target.icon(),index,pageX,()->{
                    if(target.entry()!=null)select(target.entry());else openChapter(target.chapter());
                });
            }
        }
        }
        int current=currentPage();
        int total=totalPages();
        turnButton(false,current>0);
        turnButton(true,GuideSpread.hasNext(current,total));
        addRenderableWidget(searchBox);searchBox.setEditable(!search.active());
        String action=search.active()?"guide.omnira.stop_search":"guide.omnira.search";
        findButton=bookButton(left+GuideBookLayout.FIND_X,top+GuideBookLayout.SEARCH_Y,GuideBookLayout.FIND_WIDTH,Component.translatable(action).getString(),action,this::toggleSearch);
        findButton.active=search.active()||!GuideTextSearch.normalize(searchText).isEmpty();
        rememberPage();
    }
    private void rememberPage(){
        if(!restored||remember==null||search.active())return;
        var position=new com.mcmagic.omnira.guidance.GuideBookmark(entry!=null?entry.id():chapter!=null?chapter.id():"",entry!=null?page:introPage);
        if(!position.equals(lastBookmark)){lastBookmark=position;remember.accept(position);}
    }
    @Override public void removed(){rememberPage();super.removed();}
    private boolean introductionArt(){return introduction().hasArtwork();}
    private int introductionLeaves(){return introductionPages().size()+sourcePages().size()+(introductionArt()?1:0);}
    private GuideEntryPages entryPages(){return new GuideEntryPages(pages.size(),scenePages(),entry==null?0:entry.figures().size(),recipes.size());}
    private int figureStart(){return entryPages().figureStart();}
    private int recipeStart(){return entryPages().recipeStart();}
    private int linkStart(){return entryPages().linkStart();}
    private int totalPages(){return search.results()?search.totalPages():entry==null?GuideSpread.directoryTotal(directorySize(),introductionLeaves()):linkStart()+linkPages();}
    private int currentPage(){return search.results()?search.page():entry==null?introPage:page;}
    private String destination(){return entry!=null?entry.id():chapter!=null?chapter.id():"";}
    private void openSearchResult(GuideTextSearch.Hit hit){
        search.open(hit);rotatingScene=false;
        if(hit.destination()!=null)jump(hit.destination());
        else {chapter=null;entry=null;introPage=0;page=0;buttons();}
    }
    private void toggleSearch(){
        if(!search.active()){
            if(search.start(searchText,new GuideSearchSession.Position(destination(),currentPage()))){rotatingScene=false;setFocused(null);buttons();}
            return;
        }
        var before=search.stop();searchBox.setValue("");chapter=null;entry=null;page=0;introPage=0;rotatingScene=false;
        if(before!=null&&!before.destination().isEmpty())contents.destination(before.destination()).ifPresent(target->{
            chapter=target.chapter();if(target.entry()!=null)loadEntry(target.entry());
        });
        int restoredPage=before==null?0:Math.min(before.page(),Math.max(0,totalPages()-1))&~1;
        if(entry==null)introPage=restoredPage;else page=restoredPage;
        buttons();
    }
    private void entryButtons(int page,int pageX){
        if(page>=totalPages())return;
        if(page==pages.size()&&entry!=null&&(MachineConstructionScene.supports(entry.scene())||entry.scene().equals("advanced_assembly_table"))){
            bookButton(left+pageX+52,top+180,"\u21c4",constructionScene?"guide.omnira.show_finished":"guide.omnira.show_construction",()->{
                constructionScene=!constructionScene;rotatingScene=false;buttons();
            });
        }
        if(page<pages.size()) inlineLinks(page,pageX);
        if(entry!=null && page==pages.size()-1) {
            for(int i=0;i<Math.min(2,entry.links().size());i++) linkButton(entry.links().get(i),pageX,top+163+i*13,12);
        }
        if(entry!=null && page>=linkStart()) {
            int first=(page-linkStart())*6;
            for(int i=first;i<Math.min(entry.links().size(),first+6);i++) {
                linkButton(entry.links().get(i),pageX,top+49+(i-first)*21,18);
            }
        }
    }
    private void linkButton(GuideContents.Link link,int pageX,int y,int height) {
        var target=contents.destination(link.target());
        if(target.isEmpty()) return;
        navigationButton(link.label(),pageX,y,height,()->jump(target.get()));
    }
    private void externalLinkButtons(int leaf,int pageX){
        var links=introduction().externalLinks();if(links.isEmpty())return;
        int lastText=(introductionArt()?1:0)+introductionPages().size()-1;
        if(leaf!=lastText)return;
        for(int i=0;i<links.size();i++){
            var link=links.get(i);
            navigationButton(link.label(),pageX,top+163+i*13,12,()->
                    net.minecraft.client.gui.screens.ConfirmLinkScreen.confirmLinkNow(this,link.uri(),false))
                    .setTooltip(Tooltip.create(Component.literal(link.uri().toString())));
        }
    }
    private Button navigationButton(String label,int pageX,int y,int height,Runnable action){
        return addRenderableWidget(new Button(left+pageX+4,y,124,height,Component.literal(label),b->action.run(),supplier->supplier.get()) {
            @Override protected void renderWidget(GuiGraphics g,int mx,int my,float pt) {
                fit(g,label,getX()+2,getY()+(getHeight()-font.lineHeight)/2,120,isHoveredOrFocused()?0xFF345D85:0xFF467889);
                g.fill(getX()+2,getY()+height-1,getX()+122,getY()+height,0x70467889);
            }
        });
    }
    private void inlineLinks(int page,int pageX) {
        int y=top+47;
        for(var line:pages.get(page)) {
            var plain=new StringBuilder();
            line.accept((index,style,codePoint)->{plain.appendCodePoint(codePoint);return true;});
            String text=plain.toString();
            for(var link:entry.links()) {
                var target=contents.destination(link.target());
                if(target.isEmpty() || link.label().isEmpty()) continue;
                for(int start=text.indexOf(link.label());start>=0;start=text.indexOf(link.label(),start+link.label().length())) {
                    int x=left+pageX+4+font.width(text.substring(0,start));
                    addRenderableWidget(new Button(x,y,font.width(link.label()),font.lineHeight,
                            Component.literal(link.label()),b->jump(target.get()),supplier->supplier.get()) {
                        @Override protected void renderWidget(GuiGraphics g,int mx,int my,float pt) {
                            g.drawString(font,getMessage(),getX(),getY(),isHoveredOrFocused()?0xFF345D85:0xFF467889,false);
                            g.fill(getX(),getY()+font.lineHeight-1,getX()+getWidth(),getY()+font.lineHeight,0xFF467889);
                        }
                    });
                }
            }
            y+=11;
        }
    }
    private int linkPages() {return entry==null || entry.links().size()<=2?0:(entry.links().size()+5)/6;}
    private void turnButton(boolean forward,boolean enabled) {
        var button=bookButton(left+(forward?RIGHT_BUTTON:LEFT_BUTTON),top+FOOTER_BUTTON_Y,forward?">":"<",
                forward?"guide.omnira.next":"guide.omnira.previous",()->{
                    if(search.results())search.turn(forward);
                    else if(entry==null) {
                        introPage=GuideSpread.turn(introPage,forward);
                    } else page=GuideSpread.turn(page,forward);
                    buttons();
                });
        button.active=enabled;
    }
    private Button bookButton(int x,int y,String symbol,String tooltip,Runnable action) {
        return bookButton(x,y,BUTTON_SIZE,symbol,tooltip,action);
    }
    private Button bookButton(int x,int y,int buttonWidth,String symbol,String tooltip,Runnable action) {
        var button=addRenderableWidget(new Button(x,y,buttonWidth,BUTTON_SIZE,symbol.equals("home")?Component.translatable(tooltip):Component.literal(symbol),b->action.run(),supplier->supplier.get()) {
            @Override protected void renderWidget(GuiGraphics g,int mouseX,int mouseY,float partialTick) {
                int x=getX(),y=getY();
                boolean highlight=active && isHoveredOrFocused();
                int ink=active?0xFF62432A:0xFFA89978;
                g.fill(x,y,x+getWidth(),y+getHeight(),active?0xFFAA8850:0xFFC5B58E);
                g.fill(x+1,y+1,x+getWidth()-1,y+getHeight()-1,highlight?0xFFFFE4A6:0xFFF2DCA9);
                if(isFocused()) {
                    g.renderOutline(x+2,y+2,getWidth()-4,getHeight()-4,0xFF99753E);
                }
                // Single ASCII glyphs have a trailing advance pixel and seven visible rows.
                // Center the ink rather than the full nine-pixel text line (including its gap).
                if(buttonWidth==BUTTON_SIZE&&PixelUiIcons.draw(g::fill,symbol,x+getWidth()/2,y+getHeight()/2,ink)) {
                    // Shared pixel glyphs do not depend on the selected language font.
                } else if(symbol.equals("home")) {
                    // A pixel path travels left, then turns upward toward the home page.
                    g.fill(x+6,y+6,x+8,y+14,ink);
                    g.fill(x+6,y+12,x+15,y+14,ink);
                    g.fill(x+5,y+6,x+9,y+8,ink);
                    g.fill(x+4,y+7,x+10,y+9,ink);
                    g.fill(x+6,y+5,x+8,y+7,ink);
                } else if(buttonWidth>BUTTON_SIZE){
                    CrystalUi.title(g,font,getMessage(),x+getWidth()/2,y+(getHeight()-font.lineHeight)/2,getWidth()-8,ink);
                } else {
                    int inkWidth=Math.max(1,font.width(getMessage())-1);
                    g.drawString(font,getMessage(),x+(getWidth()-inkWidth)/2,y+(getHeight()-7)/2,ink,false);
                }
            }
        });
        button.setTooltip(Tooltip.create(Component.translatable(tooltip)));
        return button;
    }
    @Override public void tick() {ticks++;}
    // This screen paints its own background; Screen.render must not blur the finished content.
    @Override public void renderBackground(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) {}

    @Override protected void renderContent(GuiGraphics g,int mouseX,int mouseY,float partialTick) {
        hovered=ItemStack.EMPTY;
        recipeTooltip=Component.empty();
        figureTooltip="";
        g.fill(0,0,width,height,0xAA101821);
        g.blit(BOOK,left,top,0,0,320,224,320,224);
        int searchX=left+GuideBookLayout.INPUT_X,searchY=top+GuideBookLayout.SEARCH_Y;
        g.fill(searchX,searchY,searchX+GuideBookLayout.INPUT_WIDTH,searchY+GuideBookLayout.CONTROL_HEIGHT,0xFFAA8850);
        g.fill(searchX+1,searchY+1,searchX+GuideBookLayout.INPUT_WIDTH-1,searchY+GuideBookLayout.CONTROL_HEIGHT-1,0xFFECE3C8);
        for(int side=0;side<2;side++){
            if(search.results()){
                int x=left+(side==0?19:175);
                fit(g,Component.translatable(side==0?"guide.omnira.search_results":"guide.omnira.search_count",search.hits().size()).getString(),x+(side==0?24:4),top+25,98,0xFF314A58);
                if(side==0&&search.hits().isEmpty()){
                    int y=top+65;
                    for(var line:font.split(Component.translatable("guide.omnira.no_search_results"),124)){g.drawString(font,line,x+4,y,0xFF405462,false);y+=11;}
                }
                continue;
            }
            int leaf=GuideSpread.leaf(entry==null?introPage:page,side),pageX=side==0?19:175;
            if(leaf>=totalPages())continue;
        if(entry==null) {
            if(leaf<introductionLeaves()){
                fit(g,chapter==null?contents.title():chapter.title(),left+pageX+(side==0&&chapter!=null?24:2),top+25,side==0?104:102,0xFF314A58);
                renderIntroduction(g,leaf+(introductionArt()?0:1),pageX,mouseX,mouseY);
            }else if(GuideSpread.directoryLeaf(leaf,introductionLeaves())>=0){
                fit(g,Component.translatable(chapter==null?"guide.omnira.contents":"guide.omnira.chapter").getString(),left+pageX+(side==0?24:4),top+25,98,0xFF314A58);
                if(directorySize()==0)fit(g,Component.translatable("guide.omnira.empty").getString(),left+pageX+4,top+65,124,0xFF405462);
            }
        }else renderEntryPage(g,leaf,pageX,mouseX,mouseY);
        }
        super.renderContent(g,mouseX,mouseY,partialTick);
        if(!hovered.isEmpty()) {
            var tooltip=new ArrayList<>(Screen.getTooltipFromItem(minecraft,hovered));
            if(entry!=null&&GuideEnergy.supports(entry.id()))tooltip.add(GuideEnergy.tooltip(entry.id(),hovered));
            if(!recipeTooltip.getString().isEmpty())tooltip.add(recipeTooltip);
            itemLink(hovered).ifPresent(target->tooltip.add(Component.translatable("guide.omnira.shift_link",target.title()).withStyle(net.minecraft.ChatFormatting.AQUA)));
            g.renderTooltip(font,tooltip,hovered.getTooltipImage(),mouseX,mouseY);
        }else if(!figureTooltip.isEmpty())g.renderTooltip(font,Component.literal(figureTooltip),mouseX,mouseY);
    }
    private void renderEntryPage(GuiGraphics g,int page,int pageX,int mouseX,int mouseY){
            int left=this.left+pageX-175;
            g.renderItem(GuideContents.icon(entry.icon()),left+(pageX==19?198:178),top+21);
            boolean links=page>=linkStart(),figure=page>=figureStart()&&page<recipeStart();
            String title=links?Component.translatable("guide.omnira.related").getString():figure?entry.figures().get(page-figureStart()).title():page<figureStart()?entry.title():recipes.get(page-recipeStart()).title().getString();
            fit(g,title,left+198+(pageX==19?20:0),top+25,pageX==19?75:79,0xFF314A58);
            if(page<pages.size()) {
                int y=top+47;
                for(var line:pages.get(page)) {g.drawString(font,line,left+179,y,0xFF353F44,false);y+=11;}
            } else if(scenePages()>0 && page==pages.size()) {
                if(CodexExampleScene.supports(entry.scene()))CodexExampleScene.render(g,left+175,top+44,ticks,entry.scene(),sceneYaw,scenePitch);
                else if(MachineConstructionScene.supports(entry.scene()))MachineConstructionScene.render(g,left+175,top+44,ticks,entry.scene(),sceneYaw,scenePitch,constructionScene);
                else if(entry.scene().equals("sword_shaping"))SwordShapingScene.render(g,left+175,top+44,ticks,sceneYaw,scenePitch);
                else if(entry.scene().equals("advanced_assembly_table"))AdvancedForgeScene.render(g,left+175,top+44,ticks,sceneYaw,scenePitch,constructionScene);
                else DreamRitualScene.render(g,left+175,top+44,ticks,entry.scene().equals("resonance_ritual"),entry.scene().equals("affinity_ritual"),entry.scene().equals("corridor_ritual"),entry.scene().equals("cleansing_ritual"),entry.scene().equals("genesis_ritual"),entry.scene().equals("world_forming_ritual"),sceneYaw,scenePitch);
            } else if(figure) {
                var hover=figureRenderer.render(g,font,entry.figures().get(page-figureStart()),left+179,top+47,mouseX,mouseY);
                if(!hover.item().isEmpty())hovered=hover.item();
                if(!hover.text().isEmpty())figureTooltip=hover.text();
            } else if(!links) {
                var diagram=recipes.get(page-recipeStart());
                if(diagram.slots().stream().anyMatch(slot->slot.output() && slot.x()>=90)
                        && diagram.slots().stream().filter(slot->!slot.output()).allMatch(slot->slot.x()<=60)) {
                    g.fill(left+247,top+88,left+266,top+90,0xFF70838A);
                    for(int i=0;i<5;i++) g.fill(left+263-i,top+85+i,left+265-i,top+93-i,0xFF70838A);
                }
                for(var slot:diagram.slots()) {
                    int x=left+175+slot.x()-8,y=top+slot.y()-8;
                    g.blit(VANILLA,x-1,y-1,7,83,18,18,256,256);
                    if(slot.output()) {g.fill(x-1,y+17,x+17,y+18,0xFF83B4BE);}
                    else if(slot.retained()) {g.fill(x-1,y+17,x+17,y+18,0xFFD0B36A);}
                    if(!slot.items().isEmpty()) {
                        var stack=slot.items().get((ticks/25)%slot.items().size());
                        g.renderItem(stack,x,y);g.renderItemDecorations(font,stack,x,y);
                        if(mouseX>=x && mouseX<x+16 && mouseY>=y && mouseY<y+16) {
                            hovered=stack;recipeTooltip=slot.hint();
                        }
                    }
                }
                int y=top+154;
                for(var note:diagram.notes()) {fit(g,note.getString(),left+179,y,124,0xFF405462);y+=12;}
            }
    }
    private Optional<GuideContents.Destination> itemLink(ItemStack stack) {
        var id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.getNamespace().equals("omnira")?contents.itemDestination(id.toString()):Optional.empty();
    }
    @Override protected boolean mouseClickedContent(double mouseX,double mouseY,int button) {
        if(search.results())return super.mouseClickedContent(mouseX,mouseY,button);
        if(button==0&&scenePages()>0)for(int side=0;side<2;side++){
            if(GuideSpread.leaf(page,side)!=pages.size())continue;
            int x=left+(side==0?19:175);
            if(mouseX>=x&&mouseX<x+124&&mouseY>=top+44&&mouseY<top+178){rotatingScene=true;return true;}
        }
        if(button==0 && hasShiftDown() && entry!=null)for(int side=0;side<2;side++) {
            int leaf=GuideSpread.leaf(page,side),figure=leaf-figureStart();
            if(figure>=0&&figure<entry.figures().size())for(var icon:entry.figures().get(figure).icons()) {
                int x=left+(side==0?19:175)+4+icon.x(),y=top+47+icon.y();
                if(mouseX>=x&&mouseX<x+16&&mouseY>=y&&mouseY<y+16) {
                    var target=itemLink(GuideContents.icon(icon.item()));
                    if(target.isPresent()){jump(target.get());return true;}
                }
            }
            int index=leaf-recipeStart();
            if(index<0||index>=recipes.size())continue;
            for(var slot:recipes.get(index).slots()) {
                int x=left+(side==0?19:175)+slot.x()-8,y=top+slot.y()-8;
                if(!slot.items().isEmpty() && mouseX>=x && mouseX<x+16 && mouseY>=y && mouseY<y+16) {
                    var target=itemLink(slot.items().get((ticks/25)%slot.items().size()));
                    if(target.isPresent()) {jump(target.get());return true;}
                }
            }
        }
        return super.mouseClickedContent(mouseX,mouseY,button);
    }
    @Override protected boolean mouseDraggedContent(double x,double y,int button,double dx,double dy){
        if(rotatingScene&&button==0){sceneYaw+=(float)dx*1.5F;scenePitch=Math.clamp(scenePitch+(float)dy,-65,60);return true;}
        return super.mouseDraggedContent(x,y,button,dx,dy);
    }
    @Override protected boolean mouseReleasedContent(double x,double y,int button){
        if(rotatingScene&&button==0){rotatingScene=false;return true;}
        return super.mouseReleasedContent(x,y,button);
    }
    private void back() {
        if(search.back(destination())){rotatingScene=false;buttons();return;}
        if(chapter==null)return;
        var parent=contents.parentDirectory(entry==null?chapter.id():entry.id());
        chapter=parent.chapter();
        entry=null;page=0;
        introPage=GuideSpread.directorySpread(parent.row(),introductionLeaves());
        buttons();
    }
    private void home() {
        search.stop();searchBox.setValue("");
        chapter=null;entry=null;page=0;introPage=0;rotatingScene=false;
        buttons();
    }
    private GuideContents.Introduction introduction() {return chapter==null?contents.introduction():chapter.introduction();}
    private List<List<FormattedCharSequence>> introductionPages() {
        var intro=introduction();var pages=textPages(intro.paragraphs(),intro.linesPerPage());
        return pages.isEmpty()&&!intro.externalLinks().isEmpty()?List.of(List.of()):pages;
    }
    private List<List<FormattedCharSequence>> textPages(List<String> paragraphs) {
        return textPages(paragraphs,12);
    }
    private List<List<FormattedCharSequence>> textPages(List<String> paragraphs,int linesPerPage) {
        return GuideTextLayout.paginate(paragraphs,124,linesPerPage,font::width).stream()
                .map(lines->lines.stream().map(line->(line.startsWith(com.mcmagic.omnira.mana.DreamText.MARKER)
                        ?com.mcmagic.omnira.mana.DreamText.colored(line.substring(com.mcmagic.omnira.mana.DreamText.MARKER.length()))
                        :Component.literal(line)).getVisualOrderText()).toList()).toList();
    }
    private List<List<GuideSourceLayout.Row>> sourcePages() {
        if(cachedSourceIntroduction!=introduction()) {
            cachedSourceIntroduction=introduction();
            cachedSourcePages=GuideSourceLayout.pages(cachedSourceIntroduction.sources(),font::width);
        }
        return cachedSourcePages;
    }
    private void renderIntroduction(GuiGraphics g,int visibleIntro,int pageX,int mouseX,int mouseY) {
        var intro=introduction();
        var extra=introductionPages();
        if(visibleIntro==0) {
            int x=left+pageX+7,y=top+52;
            var image=ResourceLocation.tryParse(intro.image());
            if(image!=null && minecraft.getResourceManager().getResource(image).isPresent()) {
                float scale=Math.min(112F/intro.imageWidth(),88F/intro.imageHeight());
                g.pose().pushPose();
                g.pose().translate(x+(112-intro.imageWidth()*scale)/2,y+(88-intro.imageHeight()*scale)/2,0);
                g.pose().scale(scale,scale,1);
                com.mojang.blaze3d.systems.RenderSystem.enableBlend();
                g.blit(image,0,0,0,0,intro.imageWidth(),intro.imageHeight(),intro.imageWidth(),intro.imageHeight());
                g.pose().popPose();
            }
            float scale=1;
            var lines=GuideTextLayout.wrap(intro.caption(),124,font::width);
            while(lines.size()*11*scale>42 && scale>.5F) {
                scale-=.05F;lines=GuideTextLayout.wrap(intro.caption(),(int)(124/scale),font::width);
            }
            g.pose().pushPose();g.pose().translate(left+pageX+63,top+150,0);g.pose().scale(scale,scale,1);
            for(int i=0;i<lines.size();i++) g.drawString(font,lines.get(i),-font.width(lines.get(i))/2,i*11,0xFF405462,false);
            g.pose().popPose();
        } else if(visibleIntro-1<extra.size()) {
            int y=top+47;
            for(var line:extra.get(visibleIntro-1)) {g.drawString(font,line,left+pageX+4,y,0xFF353F44,false);y+=11;}
        } else {
            int y=top+47,x=left+pageX+4;
            for(var row:sourcePages().get(visibleIntro-1-extra.size())) {
                var stack=GuideContents.icon(row.source().icon());
                int inset=row.source().icon().isBlank()?0:20;
                if(!stack.isEmpty()) {
                    g.renderItem(stack,x,y);
                    if(mouseX>=x&&mouseX<x+16&&mouseY>=y&&mouseY<y+16)hovered=stack;
                }
                for(int i=0;i<row.lines().size();i++)
                    g.drawString(font,row.lines().get(i),x+inset,y+i*11,0xFF353F44,false);
                y+=row.height();
            }
        }
    }
    @Override public boolean keyPressed(int key,int scanCode,int modifiers) {
        if(searchBox!=null&&searchBox.isFocused()){
            if(!search.active()&&(key==org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER||key==org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER)){toggleSearch();return true;}
            return super.keyPressed(key,scanCode,modifiers);
        }
        if(key==org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE && !search.results()&&(chapter!=null||search.returnsToResults(destination()))) {back();return true;}
        return super.keyPressed(key,scanCode,modifiers);
    }
    @Override public boolean isPauseScreen() {return false;}
}
