package com.mcmagic.omnira.client.screen;

import com.mcmagic.omnira.client.guide.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Pannable alternate index; all reading remains in the existing book renderer. */
public final class GuideTreeScreen extends ResponsiveScreen {
    private static final net.minecraft.resources.ResourceLocation REALMS =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("omnira","textures/gui/guide_tree_realms.png");
    private static final net.minecraft.resources.ResourceLocation STARFIELD =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("omnira","textures/gui/guide_tree_starfield.png");
    private static final net.minecraft.resources.ResourceLocation STAR_TRAILS =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("omnira","textures/gui/guide_tree_star_trails.png");
    private final long skyStart=net.minecraft.Util.getMillis();
    // Crop each star together with its curved tail. The atlas positions already lie on the orbit.
    private record Trail(int x,int y,int w,int h,int atlasAngle){}
    private static final Trail[] TRAILS={
            new Trail(540,262,5,57,4), new Trail(385,486,66,39,69), new Trail(104,451,45,44,142),
            new Trail(59,221,15,71,199), new Trail(212,56,52,15,261), new Trail(420,92,55,43,316)
    };
    private record Orbit(int trail,int startAngle,long periodMillis){}
    private static final Orbit[] ORBITS={
            new Orbit(0,12,413000),new Orbit(2,31,627000),new Orbit(5,58,517000),
            new Orbit(1,116,459000),new Orbit(3,134,693000),new Orbit(4,185,559000),
            new Orbit(0,199,378000),new Orbit(5,215,483000),new Orbit(2,283,586000),
            new Orbit(1,328,441000)
    };
    private final GuideBookScreen book;
    private final GuideContents contents;
    private final GuideTree tree;
    private static final GuideTreeView view=new GuideTreeView();
    private final Map<String,GuideTree.Node> nodes=new HashMap<>();
    private final Map<String,ItemStack> icons=new HashMap<>();
    private final List<Edge> edges=new ArrayList<>();
    private final List<double[]> circle=new ArrayList<>();
    private record Edge(GuideTree.Node from,GuideTree.Node to,boolean extra,boolean association,List<double[]> points){}
    private int mapX,mapY,mapW,mapH;
    private static int stage=-1;
    static boolean preferred;
    private final Set<String> highlighted=new HashSet<>();
    private static boolean initialized;
    private boolean dragging,moved;
    private double downX,downY;
    private String selected;
    private GuideTree.Node pressed;
    private static final int INK=0xFFE6DDBA,GOLD=0xFFC9AD6D,LINE=0xFF557982,PAPER=0xFF152C36;
    public GuideTreeScreen(GuideBookScreen book,GuideContents contents){
        super(Component.translatable("guide.omnira.tree"));this.book=book;this.contents=contents;tree=GuideTree.load(contents);
        for(var n:tree.nodes()){nodes.put(n.id(),n);icons.put(n.id(),GuideContents.icon(n.icon()));}
        for(var n:tree.nodes()){
            for(int i=0;i<n.parents().size();i++){
                var parent=nodes.get(n.parents().get(i));
                addEdge(parent,n,i>0 || parent.stage()!=n.stage() && parent.x()!=n.x(),false);
            }
            for(String related:n.related())addEdge(n,nodes.get(related),true,true);
        }
        for(int i=0;i<=1440;i++){double a=i*Math.PI/720;circle.add(new double[]{Math.cos(a)*tree.radius(),Math.sin(a)*tree.radius()});}
    }
    private void addEdge(GuideTree.Node p,GuideTree.Node n,boolean extra,boolean association){
        var route=new ArrayList<double[]>();route.add(new double[]{p.x(),p.y()});
        if(p.x()==n.x()&&tree.nodes().stream().anyMatch(t->t.x()==p.x()&&t.y()>Math.min(p.y(),n.y())&&t.y()<Math.max(p.y(),n.y()))){
            double lane=p.x()+(p.x()<0?-73:73);route.add(new double[]{lane,p.y()});route.add(new double[]{lane,n.y()});
        }else if(p.ring()!=n.ring()){
            // Join the circular track through a free row, even if its height nearly matches a node.
            var inner=p.ring()?n:p;
            var outer=p.ring()?p:n;
            double gutter=inner.y()+Math.copySign(GuideTree.ROW_HEIGHT*.5,outer.y()-inner.y());
            route.add(new double[]{p.x(),gutter});route.add(new double[]{n.x(),gutter});
        }else if(!p.ring()&&Math.abs(p.x()-n.x())>GuideTree.COLUMN_WIDTH){
            // Cross intermediate columns through the gutter between rows.
            double lane=p.x()+Math.copySign(GuideTree.COLUMN_WIDTH*.5,n.x()-p.x());
            double gutter=n.y()+Math.copySign(GuideTree.ROW_HEIGHT*.5,p.y()-n.y());
            route.add(new double[]{lane,p.y()});route.add(new double[]{lane,gutter});route.add(new double[]{n.x(),gutter});
        }else if(p.x()!=n.x()){
            double lane=(p.x()+n.x())/2;route.add(new double[]{lane,p.y()});route.add(new double[]{lane,n.y()});
        }
        route.add(new double[]{n.x(),n.y()});edges.add(new Edge(p,n,extra,association,List.copyOf(route)));
    }
    @Override protected int minimumWidth(){return 360;}
    @Override protected int minimumHeight(){return 256;}
    @Override protected void init(){
        super.init();mapX=10;mapY=39;mapW=width-20;mapH=height-76;
        if(!initialized){view.fit(mapW,mapH,tree.radius());initialized=true;}
        int w=Math.min(62,(width-48)/5);
        control(10,10,w,"guide.omnira.tree_all",()->{stage=-1;view.fit(mapW,mapH,tree.radius());});
        for(int i=0;i<4;i++){final int s=i;control(10+(i+1)*w,10,w,"guide.omnira.tree_stage_"+i,()->{stage=s;view.focus(s);});}
        control(width-30,10,20,"codex.omnira.close",Component.literal("X"),()->onClose());
        control(10,height-28,72,"guide.omnira.book_mode",this::returnBook);
        control(86,height-28,72,"guide.omnira.tree_center",()->view.center(stage));
        control(width-94,height-28,20,"guide.omnira.tree_zoom_out",Component.literal("−"),()->zoom(1/1.25,cx(),cy()));
        control(width-30,height-28,20,"guide.omnira.tree_zoom_in",Component.literal("+"),()->zoom(1.25,cx(),cy()));
    }
    private void control(int x,int y,int w,String key,Runnable action){
        control(x,y,w,key,Component.translatable(key),action);
    }
    private void control(int x,int y,int w,String key,Component label,Runnable action){
        addRenderableWidget(new Button(x,y,w,20,label,b->action.run(),s->s.get()){
            @Override protected void renderWidget(GuiGraphics g,int mx,int my,float partial){
                g.fill(getX(),getY(),getX()+getWidth(),getY()+20,isHoveredOrFocused()?GOLD:LINE);
                g.fill(getX()+1,getY()+1,getX()+getWidth()-1,getY()+19,PAPER);
                CrystalUi.title(g,font,getMessage(),getX()+getWidth()/2,getY()+6,getWidth()-4,INK);
            }
        }).setTooltip(Tooltip.create(Component.translatable(key)));
    }
    private double cx(){return mapX+mapW/2.0;}private double cy(){return mapY+mapH/2.0;}
    private int sx(double x){return (int)Math.round(view.screenX(x,cx()));}private int sy(double y){return (int)Math.round(view.screenY(y,cy()));}
    private boolean inMap(double x,double y){return x>=mapX&&x<mapX+mapW&&y>=mapY&&y<mapY+mapH;}
    private GuideTree.Node hit(double x,double y){
        if(!inMap(x,y))return null;double distance=Double.MAX_VALUE;GuideTree.Node best=null;
        double radius=view.zoom()>=.55?13:5;
        for(var n:tree.nodes()){double d=Math.hypot(x-sx(n.x()),y-sy(n.y()));if(d<=radius&&d<distance){best=n;distance=d;}}
        return best;
    }
    private void zoom(double factor,double x,double y){view.zoomAt(factor,x,y,cx(),cy());}
    public void returnBook(){preferred=false;minecraft.setScreen(book);}
    public void homeBook(){returnBook();book.treeHome();}
    @Override public void onClose(){book.closeGuide();}
    private void open(GuideTree.Node node){selected=node.id();minecraft.setScreen(GuideBookScreen.fromTree(contents,this,node));}
    @Override public void renderBackground(GuiGraphics g,int x,int y,float partial){}
    @Override protected void renderContent(GuiGraphics g,int mx,int my,float partial){
        g.fill(0,0,width,height,0xEB101F29);g.fill(mapX-1,mapY-1,mapX+mapW+1,mapY+mapH+1,LINE);g.fill(mapX,mapY,mapX+mapW,mapY+mapH,0xFF0C1424);
        var hover=hit(mx,my);UiScissor.enable(g,mapX,mapY,mapX+mapW,mapY+mapH);
        // A repeating sky behind the translucent rim covers even a viewport entirely off the map.
        int period=(int)Math.round(300+110*Math.sqrt(view.zoom()));
        long elapsed=net.minecraft.Util.getMillis()-skyStart;
        float u=(float)(view.x()*period/1800-mapW*.5+period*(elapsed%360000L)/360000.0);
        float v=(float)(view.y()*period/1800-mapH*.5+period*(elapsed%840000L)/840000.0);
        g.blit(STARFIELD,mapX,mapY,mapW,mapH,u,v,mapW,mapH,period,period);
        // The node orbit occupies 225 of the 300 pixels from the center to the texture edge.
        double extent=tree.radius()*4/3;
        int side=(int)Math.round(2*extent*view.zoom());
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        g.blit(REALMS,sx(-extent),sy(-extent),side,side,0F,0F,600,600,600,600);
        // Shared center and scale keep the moving arcs registered to the fixed node orbit.
        g.pose().pushPose();
        g.pose().translate(sx(-extent)+side*.5,sy(-extent)+side*.5,0);
        g.pose().scale(side/600F,side/600F,1);
        for(var orbit:ORBITS){
            var trail=TRAILS[orbit.trail()];
            float angle=orbit.startAngle()-trail.atlasAngle()+360F*(elapsed%orbit.periodMillis())/orbit.periodMillis();
            g.pose().pushPose();
            g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(angle));
            g.blit(STAR_TRAILS,trail.x()-300,trail.y()-300,trail.x(),trail.y(),trail.w(),trail.h(),600,600);
            g.pose().popPose();
        }
        g.pose().popPose();
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        for(int i=1;i<circle.size();i++){var a=circle.get(i-1);var b=circle.get(i);line(g,sx(a[0]),sy(a[1]),sx(b[0]),sy(b[1]),LINE,false);}
        highlighted.clear();
        String active=hover!=null?hover.id():selected;
        for(var edge:edges){if(edge.extra()&&!edge.from().id().equals(active)&&!edge.to().id().equals(active))continue;
            if(edge.extra()){highlighted.add(edge.from().id());highlighted.add(edge.to().id());}
            int color=edge.extra()?0xFFB5A67F:edge.from().x()==0&&edge.to().x()==0?GOLD:LINE;
            for(int i=1;i<edge.points().size();i++){var a=edge.points().get(i-1);var b=edge.points().get(i);line(g,sx(a[0]),sy(a[1]),sx(b[0]),sy(b[1]),color,edge.extra());}
            if(view.zoom()<.55||edge.association())continue;
            var p=edge.points().get(edge.points().size()-2);double dx=edge.to().x()-p[0],dy=edge.to().y()-p[1],length=Math.hypot(dx,dy);
            if(length>0){dx/=length;dy/=length;double inset=13;int x=(int)(sx(edge.to().x())-dx*inset),y=(int)(sy(edge.to().y())-dy*inset);
                line(g,x,y,(int)(x-dx*3-dy*3),(int)(y-dy*3+dx*3),color,false);line(g,x,y,(int)(x-dx*3+dy*3),(int)(y-dy*3-dx*3),color,false);}
        }
        for(var n:tree.nodes()){
            int x=sx(n.x()),y=sy(n.y());if(x<mapX-50||x>mapX+mapW+50||y<mapY-35||y>mapY+mapH+35)continue;
            boolean detail=view.zoom()>=.55,focused=n.id().equals(active)||highlighted.contains(n.id());int r=detail?10:3,color=focused?0xFFFFE6A1:n.kind().equals("main")?GOLD:n.ring()?0xFFB59ECD:0xFF8EBCC6;
            g.fill(x-r,y-r,x+r+1,y+r+1,color);g.fill(x-r+1,y-r+1,x+r,y+r,PAPER);
            if(detail){var icon=icons.get(n.id());if(!icon.isEmpty())g.renderItem(icon,x-8,y-8);else g.fill(x-2,y-2,x+3,y+3,color);}
            if(view.zoom()>=.9||view.zoom()>=.35&&n.overview()){
                String name=font.plainSubstrByWidth(n.title(),view.zoom()>=.9?90:70);if(!name.equals(n.title()))name=font.plainSubstrByWidth(name,view.zoom()>=.9?82:62)+"…";
                int half=font.width(name)/2;g.fill(x-half-2,y+r+2,x+half+3,y+r+13,0xE6152C36);g.drawString(font,name,x-half,y+r+3,INK,false);
            }
        }
        if(tree.nodes().isEmpty())g.drawCenteredString(font,Component.translatable("guide.omnira.tree_empty"),(int)cx(),(int)cy(),INK);
        g.disableScissor();g.drawCenteredString(font,Math.round(view.zoom()*100)+"%",width-52,height-22,INK);
        super.renderContent(g,mx,my,partial);
        if(hover!=null&&!dragging)g.renderTooltip(font,Component.literal(hover.title()),mx,my);
    }
    private void line(GuiGraphics g,int x,int y,int x2,int y2,int color,boolean dashed){
        // Clip axis-aligned channels before drawing. Circle segments have only a few screen pixels.
        if(Math.max(x,x2)<mapX||Math.min(x,x2)>mapX+mapW||Math.max(y,y2)<mapY||Math.min(y,y2)>mapY+mapH)return;
        if(x==x2){int lo=Math.max(mapY,Math.min(y,y2)),hi=Math.min(mapY+mapH,Math.max(y,y2));if(!dashed){g.fill(x,lo,x+1,hi+1,color);return;}for(int a=lo;a<=hi;a+=6)g.fill(x,a,x+1,Math.min(a+3,hi+1),color);return;}
        if(y==y2){int lo=Math.max(mapX,Math.min(x,x2)),hi=Math.min(mapX+mapW,Math.max(x,x2));if(!dashed){g.fill(lo,y,hi+1,y+1,color);return;}for(int a=lo;a<=hi;a+=6)g.fill(a,y,Math.min(a+3,hi+1),y+1,color);return;}
        int dx=Math.abs(x2-x),dy=-Math.abs(y2-y),sx=x<x2?1:-1,sy=y<y2?1:-1,error=dx+dy;
        while(true){if(inMap(x,y))g.fill(x,y,x+1,y+1,color);if(x==x2&&y==y2)break;int e=error*2;if(e>=dy){error+=dy;x+=sx;}if(e<=dx){error+=dx;y+=sy;}}
    }
    @Override protected boolean mouseClickedContent(double x,double y,int button){
        if(button==0&&inMap(x,y)){dragging=true;moved=false;downX=x;downY=y;pressed=hit(x,y);setFocused(null);return true;}return super.mouseClickedContent(x,y,button);
    }
    @Override protected boolean mouseDraggedContent(double x,double y,int button,double dx,double dy){
        if(dragging&&button==0){if(!moved&&Math.hypot(x-downX,y-downY)>3){moved=true;view.pan(x-downX,y-downY);}else if(moved)view.pan(dx,dy);return true;}return super.mouseDraggedContent(x,y,button,dx,dy);
    }
    @Override protected boolean mouseReleasedContent(double x,double y,int button){
        if(button==0&&dragging){dragging=false;if(!moved&&pressed!=null&&pressed==hit(x,y))open(pressed);pressed=null;return true;}return super.mouseReleasedContent(x,y,button);
    }
    @Override protected boolean mouseScrolledContent(double x,double y,double dx,double dy){if(inMap(x,y)){zoom(Math.exp(dy*.16),x,y);return true;}return super.mouseScrolledContent(x,y,dx,dy);}
    @Override public boolean keyPressed(int key,int scan,int mods){
        if(key==org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER&&selected!=null){open(nodes.get(selected));return true;}
        if(key>=org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT&&key<=org.lwjgl.glfw.GLFW.GLFW_KEY_UP){
            var current=nodes.get(selected);double x=current==null?view.x():current.x(),y=current==null?view.y():current.y();
            int dx=key==262?1:key==263?-1:0,dy=key==264?1:key==265?-1:0;GuideTree.Node best=null;double score=Double.MAX_VALUE;
            for(var n:tree.nodes()){double vx=n.x()-x,vy=n.y()-y;if(vx*dx+vy*dy<=0)continue;double value=Math.hypot(vx,vy)+Math.abs(vx*dy-vy*dx)*2;if(value<score){score=value;best=n;}}
            if(best!=null){selected=best.id();view.pan((view.x()-best.x())*view.zoom(),(view.y()-best.y())*view.zoom());}return true;
        }
        return super.keyPressed(key,scan,mods);
    }
    @Override public boolean isPauseScreen(){return false;}
}
