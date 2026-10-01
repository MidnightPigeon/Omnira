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

public final class GuideBookScreen extends Screen {
    private static final int BUTTON_SIZE=20,LEFT_BUTTON=18,RIGHT_BUTTON=282,HEADER_BUTTON_Y=18,FOOTER_BUTTON_Y=190;
    private static final ResourceLocation BOOK=ResourceLocation.fromNamespaceAndPath("omnira","textures/gui/guide_book.png");
    private static final ResourceLocation VANILLA=ResourceLocation.withDefaultNamespace("textures/gui/container/crafting_table.png");
    private final GuideContents contents=GuideContents.load();
    private GuideContents.Chapter chapter;
    private GuideContents.Entry entry;
    private List<List<FormattedCharSequence>> pages=List.of();
    private List<GuideRecipes.Diagram> recipes=List.of();
    private int left,top,listPage,introPage,page,ticks;
    private record View(GuideContents.Chapter chapter,GuideContents.Entry entry,int listPage,int introPage,int page) {}
    private final Deque<View> history=new ArrayDeque<>();
    private ItemStack hovered=ItemStack.EMPTY;
    public GuideBookScreen() {super(Component.translatable("item.omnira.guide_book"));}
    @Override protected void init() {
        left=(width-320)/2;top=(height-224)/2;buttons();
    }
    private void select(GuideContents.Entry next) {
        if(entry==null) history.push(view());
        loadEntry(next);
        buttons();
    }
    private View view() {return new View(chapter,entry,listPage,introPage,page);}
    private void loadEntry(GuideContents.Entry next) {
        entry=next;page=0;
        pages=textPages(entry.text(),entry.links().isEmpty()?12:10);
        if(pages.isEmpty()) pages=List.of(List.of());
        recipes=entry.recipes()?GuideRecipes.find(GuideContents.icon(entry.item()).getItem()):List.of();
    }
    private void jump(GuideContents.Destination target) {
        history.push(view());chapter=target.chapter();entry=null;listPage=0;introPage=0;page=0;
        if(target.entry()!=null) {
            listPage=Math.max(0,chapter.entries().indexOf(target.entry()))/6;
            loadEntry(target.entry());
        }
        buttons();
    }
    private void fit(GuiGraphics g,String text,int x,int y,int maxWidth,int color) {
        float scale=Math.min(1,maxWidth/(float)Math.max(1,font.width(text)));
        g.pose().pushPose();g.pose().translate(x,y,0);g.pose().scale(scale,scale,1);
        g.drawString(font,text,0,0,color,false);g.pose().popPose();
    }
    private void row(String name,String item,int index,Runnable action) {
        var icon=GuideContents.icon(item);
        var button=addRenderableWidget(new Button(left+(entry==null?174:18),top+45+index*23,132,20,Component.literal(name),b->action.run(),supplier->supplier.get()) {
            @Override protected void renderWidget(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) {
                if(isHoveredOrFocused()) graphics.fill(getX(),getY(),getX()+width,getY()+height,0x18745232);
                graphics.fill(getX()+23,getY()+height-1,getX()+width-3,getY()+height,0x307F6E50);
                graphics.renderItem(icon,getX()+3,getY()+2);
                fit(graphics,name,getX()+23,getY()+(getHeight()-font.lineHeight)/2,104,0xFF514333);
            }
        });
        button.setTooltip(Tooltip.create(Component.literal(name)));
    }
    private int directorySize() {return chapter==null?contents.chapters().size():chapter.entries().size()+chapter.chapters().size();}
    private void openChapter(GuideContents.Chapter target) {
        history.push(view());
        chapter=target;entry=null;listPage=0;introPage=0;buttons();
    }
    private int scenePages() {return entry!=null && (entry.scene().equals("dream_ritual") || entry.scene().equals("resonance_ritual") || entry.scene().equals("affinity_ritual") || entry.scene().equals("corridor_ritual") || entry.scene().equals("cleansing_ritual") || entry.scene().equals("sword_shaping") || entry.scene().equals("advanced_assembly_table"))?1:0;}
    private void buttons() {
        clearWidgets();
        bookButton(left+RIGHT_BUTTON,top+HEADER_BUTTON_Y,"X","gui.close",this::onClose);
        if(chapter!=null || !history.isEmpty()) {
            bookButton(left+LEFT_BUTTON,top+HEADER_BUTTON_Y,"<","guide.omnira.back",this::back);
        }
        int count=directorySize();
        listPage=Math.clamp(listPage,0,Math.max(0,(count-1)/6));
        for(int index=0;index<6 && listPage*6+index<count;index++) {
            int absolute=listPage*6+index;
            if(chapter==null) {
                var target=contents.chapters().get(absolute);
                row(target.title(),target.icon(),index,()->openChapter(target));
            } else if(absolute<chapter.entries().size()) {
                var target=chapter.entries().get(absolute);
                row(target.title(),target.item(),index,()->select(target));
            } else {
                var target=chapter.chapters().get(absolute-chapter.entries().size());
                row(target.title(),target.icon(),index,()->openChapter(target));
            }
        }
        int current=entry==null?introPage:page;
        int total=entry==null?Math.max((count+5)/6,introductionPages().size()+1):pages.size()+scenePages()+recipes.size()+linkPages();
        if(entry!=null && page<pages.size()) inlineLinks();
        if(entry!=null && page==pages.size()-1) {
            for(int i=0;i<Math.min(2,entry.links().size());i++) linkButton(entry.links().get(i),top+163+i*13,12);
        }
        if(entry!=null && page>=pages.size()+scenePages()+recipes.size()) {
            int first=(page-pages.size()-scenePages()-recipes.size())*6;
            for(int i=first;i<Math.min(entry.links().size(),first+6);i++) {
                linkButton(entry.links().get(i),top+49+(i-first)*21,18);
            }
        }
        turnButton(false,current>0);
        turnButton(true,current+1<total);
    }
    private void linkButton(GuideContents.Link link,int y,int height) {
        var target=contents.destination(link.target());
        if(target.isEmpty()) return;
        addRenderableWidget(new Button(left+179,y,124,height,Component.literal(link.label()),b->jump(target.get()),supplier->supplier.get()) {
            @Override protected void renderWidget(GuiGraphics g,int mx,int my,float pt) {
                fit(g,link.label(),getX()+2,getY()+(getHeight()-font.lineHeight)/2,120,isHoveredOrFocused()?0xFF345D85:0xFF467889);
                g.fill(getX()+2,getY()+height-1,getX()+122,getY()+height,0x70467889);
            }
        });
    }
    private void inlineLinks() {
        int y=top+47;
        for(var line:pages.get(page)) {
            var plain=new StringBuilder();
            line.accept((index,style,codePoint)->{plain.appendCodePoint(codePoint);return true;});
            String text=plain.toString();
            for(var link:entry.links()) {
                var target=contents.destination(link.target());
                if(target.isEmpty() || link.label().isEmpty()) continue;
                for(int start=text.indexOf(link.label());start>=0;start=text.indexOf(link.label(),start+link.label().length())) {
                    int x=left+179+font.width(text.substring(0,start));
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
                    int step=forward?1:-1;
                    if(entry==null) {
                        introPage+=step;
                        int count=directorySize();
                        listPage=Math.min(introPage,Math.max(0,(count-1)/6));
                    } else page+=step;
                    buttons();
                });
        button.active=enabled;
    }
    private Button bookButton(int x,int y,String symbol,String tooltip,Runnable action) {
        var button=addRenderableWidget(new Button(x,y,BUTTON_SIZE,BUTTON_SIZE,Component.literal(symbol),b->action.run(),supplier->supplier.get()) {
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
                int inkWidth=Math.max(1,font.width(getMessage())-1);
                g.drawString(font,getMessage(),x+(getWidth()-inkWidth)/2,y+(getHeight()-7)/2,ink,false);
            }
        });
        button.setTooltip(Tooltip.create(Component.translatable(tooltip)));
        return button;
    }
    @Override public void tick() {ticks++;}
    // This screen paints its own background; Screen.render must not blur the finished content.
    @Override public void renderBackground(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) {}

    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partialTick) {
        hovered=ItemStack.EMPTY;
        g.fill(0,0,width,height,0xAA101821);
        g.blit(BOOK,left,top,0,0,320,224,320,224);
        fit(g,chapter==null?contents.title():chapter.title(),left+(chapter==null?21:43),top+25,chapter==null?124:105,0xFF314A58);
        int directoryX=entry==null?175:19;
        int count=directorySize();
        if(entry==null) {
            fit(g,Component.translatable(chapter==null?"guide.omnira.contents":"guide.omnira.chapter").getString(),left+179,top+25,98,0xFF314A58);
            renderIntroduction(g);
            if(chapter!=null && count==0)
                fit(g,Component.translatable("guide.omnira.empty").getString(),left+179,top+65,124,0xFF405462);
        } else {
            g.renderItem(GuideContents.icon(entry.item()),left+178,top+21);
            boolean links=page>=pages.size()+scenePages()+recipes.size();
            fit(g,links?Component.translatable("guide.omnira.related").getString():page<pages.size()+scenePages()?entry.title():recipes.get(page-pages.size()-scenePages()).title().getString(),left+198,top+25,79,0xFF314A58);
            if(page<pages.size()) {
                int y=top+47;
                for(var line:pages.get(page)) {g.drawString(font,line,left+179,y,0xFF353F44,false);y+=11;}
            } else if(scenePages()>0 && page==pages.size()) {
                if(entry.scene().equals("sword_shaping"))com.mcmagic.omnira.client.guide.SwordShapingScene.render(g,left+175,top+44,ticks);
                else if(entry.scene().equals("advanced_assembly_table"))com.mcmagic.omnira.client.guide.AdvancedForgeScene.render(g,left+175,top+44,ticks);
                else DreamRitualScene.render(g,left+175,top+44,ticks,entry.scene().equals("resonance_ritual"),entry.scene().equals("affinity_ritual"),entry.scene().equals("corridor_ritual"),entry.scene().equals("cleansing_ritual"));
            } else if(!links) {
                var diagram=recipes.get(page-pages.size()-scenePages());
                if(diagram.slots().stream().anyMatch(slot->slot.output() && slot.x()>=90)
                        && diagram.slots().stream().filter(slot->!slot.output()).allMatch(slot->slot.x()<=60)) {
                    g.fill(left+247,top+88,left+266,top+90,0xFF70838A);
                    for(int i=0;i<5;i++) g.fill(left+263-i,top+85+i,left+265-i,top+93-i,0xFF70838A);
                }
                for(var slot:diagram.slots()) {
                    int x=left+175+slot.x()-8,y=top+slot.y()-8;
                    g.blit(VANILLA,x-1,y-1,7,83,18,18,256,256);
                    if(slot.output()) {g.fill(x-1,y+17,x+17,y+18,0xFF83B4BE);}
                    if(!slot.items().isEmpty()) {
                        var stack=slot.items().get((ticks/25)%slot.items().size());
                        g.renderItem(stack,x,y);g.renderItemDecorations(font,stack,x,y);
                        if(mouseX>=x && mouseX<x+16 && mouseY>=y && mouseY<y+16) hovered=stack;
                    }
                }
                int y=top+154;
                for(var note:diagram.notes()) {fit(g,note.getString(),left+179,y,124,0xFF405462);y+=12;}
            }
        }
        super.render(g,mouseX,mouseY,partialTick);
        if(!hovered.isEmpty()) {
            var tooltip=new ArrayList<>(Screen.getTooltipFromItem(minecraft,hovered));
            itemLink(hovered).ifPresent(target->tooltip.add(Component.translatable("guide.omnira.shift_link",target.title()).withStyle(net.minecraft.ChatFormatting.AQUA)));
            g.renderTooltip(font,tooltip,hovered.getTooltipImage(),mouseX,mouseY);
        }
    }
    private Optional<GuideContents.Destination> itemLink(ItemStack stack) {
        var id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.getNamespace().equals("omnira")?contents.itemDestination(id.toString()):Optional.empty();
    }
    @Override public boolean mouseClicked(double mouseX,double mouseY,int button) {
        int index=page-pages.size()-scenePages();
        if(button==0 && hasShiftDown() && entry!=null && index>=0 && index<recipes.size()) {
            for(var slot:recipes.get(index).slots()) {
                int x=left+175+slot.x()-8,y=top+slot.y()-8;
                if(!slot.items().isEmpty() && mouseX>=x && mouseX<x+16 && mouseY>=y && mouseY<y+16) {
                    var target=itemLink(slot.items().get((ticks/25)%slot.items().size()));
                    if(target.isPresent()) {jump(target.get());return true;}
                }
            }
        }
        return super.mouseClicked(mouseX,mouseY,button);
    }
    private void back() {
        if(!history.isEmpty()) {
            var previous=history.pop();chapter=previous.chapter();entry=null;
            if(previous.entry()!=null) loadEntry(previous.entry());
            listPage=previous.listPage();introPage=previous.introPage();page=previous.page();
        } else if(chapter!=null) {
            entry=null;chapter=null;page=0;listPage=0;introPage=0;
        }
        buttons();
    }
    private GuideContents.Introduction introduction() {return chapter==null?contents.introduction():chapter.introduction();}
    private List<List<FormattedCharSequence>> introductionPages() {
        return textPages(introduction().text());
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
    private void renderIntroduction(GuiGraphics g) {
        var intro=introduction();
        var extra=introductionPages();
        int visibleIntro=Math.clamp(introPage,0,extra.size());
        if(visibleIntro==0) {
            int x=left+26,y=top+52;
            var image=ResourceLocation.tryParse(intro.image());
            if(image!=null && minecraft.getResourceManager().getResource(image).isPresent()) {
                float scale=Math.min(112F/intro.imageWidth(),88F/intro.imageHeight());
                g.pose().pushPose();
                g.pose().translate(x+(112-intro.imageWidth()*scale)/2,y+(88-intro.imageHeight()*scale)/2,0);
                g.pose().scale(scale,scale,1);
                com.mojang.blaze3d.systems.RenderSystem.enableBlend();
                g.blit(image,0,0,0,0,intro.imageWidth(),intro.imageHeight(),intro.imageWidth(),intro.imageHeight());
                g.pose().popPose();
            } else {
                // Reserve the artwork area without inventing an illustration.
                for(int dx:new int[]{0,111}) for(int dy:new int[]{0,87}) {
                    g.fill(x+dx-(dx==0?0:4),y+dy,x+dx+(dx==0?5:1),y+dy+1,0xFFBCCAC1);
                    g.fill(x+dx,y+dy-(dy==0?0:4),x+dx+1,y+dy+(dy==0?5:1),0xFFBCCAC1);
                }
            }
            float scale=1;
            var lines=GuideTextLayout.wrap(intro.caption(),124,font::width);
            while(lines.size()*11*scale>42 && scale>.5F) {
                scale-=.05F;lines=GuideTextLayout.wrap(intro.caption(),(int)(124/scale),font::width);
            }
            g.pose().pushPose();g.pose().translate(left+82,top+150,0);g.pose().scale(scale,scale,1);
            for(int i=0;i<lines.size();i++) g.drawString(font,lines.get(i),-font.width(lines.get(i))/2,i*11,0xFF405462,false);
            g.pose().popPose();
        } else {
            int y=top+47;
            for(var line:extra.get(visibleIntro-1)) {g.drawString(font,line,left+21,y,0xFF353F44,false);y+=11;}
        }
    }
    @Override public boolean keyPressed(int key,int scanCode,int modifiers) {
        if(key==org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE && chapter!=null) {back();return true;}
        return super.keyPressed(key,scanCode,modifiers);
    }
    @Override public boolean isPauseScreen() {return false;}
}
