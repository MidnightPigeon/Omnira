package com.mcmagic.omnira.client.screen;

/** Pixel geometry shared by the live GUI and the offline art review. No inventory is drawn here. */
public final class MachinePanelArt {
    @FunctionalInterface public interface Pixels { void fill(int x,int y,int xx,int yy,int color); }
    public static final int RIM=0xFFA9CCD4,LIGHT=0xFFE0F3EF,BODY=0xE85A7C89,DARK=0xFF304D5D,
            MINT=0xFFB1E8D3,LILAC=0xFFCFBDE8,GOLD=0xFFE5D3A3,BLUE=0xFFACCFF0;
    private static final int[][] ASSEMBLY={{105,48},{122,78},{105,108},{71,108},{54,78},{71,48},{88,78}};
    private MachinePanelArt(){}
    public static void line(Pixels p,int x,int y,int xx,int yy,int color){
        int dx=Math.abs(xx-x),sx=x<xx?1:-1,dy=-Math.abs(yy-y),sy=y<yy?1:-1,error=dx+dy;
        while(true){p.fill(x,y,x+1,y+1,color);if(x==xx&&y==yy)break;int e=2*error;if(e>=dy){error+=dy;x+=sx;}if(e<=dx){error+=dx;y+=sy;}}
    }
    public static void polygon(Pixels p,int color,int... points){
        int min=9999,max=-9999;for(int i=1;i<points.length;i+=2){min=Math.min(min,points[i]);max=Math.max(max,points[i]);}
        for(int y=min;y<max;y++){
            double left=Double.POSITIVE_INFINITY,right=Double.NEGATIVE_INFINITY;
            for(int i=0;i<points.length;i+=2){int j=(i+2)%points.length;double ay=points[i+1],by=points[j+1];
                if((ay<=y+.5&&by>y+.5)||(by<=y+.5&&ay>y+.5)){double x=points[i]+(y+.5-ay)*(points[j]-points[i])/(by-ay);left=Math.min(left,x);right=Math.max(right,x);}}
            if(left<=right)p.fill((int)Math.ceil(left),y,(int)Math.ceil(right),y+1,color);
        }
    }
    public static void frame(Pixels p,int x,int y,int w,int h,int color){
        polygon(p,RIM,x+3,y,x+w-3,y,x+w,y+3,x+w,y+h-3,x+w-3,y+h,x+3,y+h,x,y+h-3,x,y+3);
        p.fill(x+2,y+2,x+w-2,y+h-2,color);p.fill(x+4,y+1,x+w-4,y+2,LIGHT);
        p.fill(x+4,y+h-2,x+w-4,y+h-1,0xFF7DA6B5);
    }
    public static void slot(Pixels p,int cx,int cy,int color){
        p.fill(cx-10,cy-10,cx+10,cy+10,color);p.fill(cx-9,cy-9,cx+9,cy+9,DARK);
        p.fill(cx-8,cy-8,cx+8,cy+8,0xFF547383);p.fill(cx-8,cy+8,cx+9,cy+9,LIGHT);
    }
    public static void ring(Pixels p,int cx,int cy,int rx,int ry,int color){
        int x=cx+rx,y=cy;
        for(int i=1;i<=64;i++){double a=i*Math.PI/32;int xx=cx+(int)Math.round(Math.cos(a)*rx),yy=cy+(int)Math.round(Math.sin(a)*ry);
            line(p,x,y,xx,yy,color);x=xx;y=yy;}
    }
    public static void orbit(Pixels p,int cx,int cy,int rx,int ry,float time,int color){
        for(int i=0;i<3;i++){double a=time*.04+i*Math.PI*2/3;int x=cx+(int)Math.round(Math.cos(a)*rx),y=cy+(int)Math.round(Math.sin(a)*ry);p.fill(x-1,y-1,x+2,y+2,color);}
    }
    public static void flow(Pixels p,int x,int y,int xx,int yy,float time,int color){
        line(p,x,y,xx,yy,0xFF789AA8);
        for(int i=0;i<2;i++){double f=((time*.018+i*.5)%1+1)%1;int a=(int)Math.round(x+(xx-x)*f),b=(int)Math.round(y+(yy-y)*f);p.fill(a-1,b-1,a+2,b+2,color);}
    }
    public static void button(Pixels p,int w,int h,boolean enabled,boolean hover){
        frame(p,0,0,w,h,enabled?(hover?0xFF88ADB5:0xFF4C7180):0xFF43545F);
        if(hover&&enabled)p.fill(4,h-3,w-4,h-2,MINT);
    }
    private static void table(Pixels p,boolean raised){
        if(raised){
            p.fill(78,110,98,130,RIM);p.fill(81,110,95,130,0xFF6A929D);
            polygon(p,RIM,54,126,122,126,138,136,126,140,50,140,38,136);
            polygon(p,BODY,56,128,120,128,132,135,124,137,52,137,44,135);
            line(p,53,138,123,138,MINT);
        }
        polygon(p,RIM,48,30,128,30,160,77,128,124,48,124,16,77);
        polygon(p,DARK,49,32,127,32,157,77,127,121,49,121,19,77);
        polygon(p,BODY,50,35,126,35,153,77,126,117,50,117,23,77);
        line(p,49,32,127,32,LIGHT);line(p,19,77,49,32,LIGHT);
    }
    public static void workstation(Pixels p,String kind,float time,float progress,boolean active){
        if(kind.equals("condensation")){
            polygon(p,RIM,43,100,133,100,148,116,135,123,41,123,28,116);
            polygon(p,BODY,44,102,132,102,143,114,132,119,44,119,33,114);
            ring(p,88,110,40,7,MINT);ring(p,88,76,41,26,0xFF81ACBA);ring(p,88,76,29,38,RIM);
            if(active){orbit(p,88,76,41,26,time,LIGHT);orbit(p,88,76,29,38,-time,MINT);}
            for(int side:new int[]{-1,1}){int x=88+side*55;polygon(p,BLUE,x,76,x+4,83,x,92,x-4,83);}
            slot(p,88,78,LIGHT);return;
        }
        table(p,kind.equals("forge"));
        switch(kind){
            case "analysis" -> {
                ring(p,88,56,15,15,GOLD);
                line(p,88,71,88,82,RIM);line(p,64,82,112,82,RIM);
                if(active){flow(p,88,82,64,98,time,MINT);flow(p,88,82,112,98,time,LILAC);}
                else{line(p,64,82,64,96,RIM);line(p,112,82,112,96,RIM);}
                slot(p,88,56,GOLD);slot(p,64,104,MINT);slot(p,112,104,LILAC);
                if(active){int y=44+(int)(progress*22);p.fill(77,y,99,y+1,LIGHT);}
            }
            case "processing" -> {
                int[][] nodes={{60,40},{116,40},{32,76},{60,112},{116,112},{144,76}};
                int[] colors={LIGHT,LIGHT,MINT,MINT,LILAC,LILAC};
                for(int i=0;i<nodes.length;i++){int[] n=nodes[i];line(p,n[0],n[1],88,76,0xFF8AA5B7);if(active)flow(p,n[0],n[1],88,96,time,colors[i]);}
                for(int i=0;i<nodes.length;i++)slot(p,nodes[i][0],nodes[i][1],colors[i]);
                slot(p,74,72,BLUE);slot(p,102,72,GOLD);slot(p,88,96,0xFFE7B6D4);
            }
            case "engine" -> {
                polygon(p,DARK,74,62,102,62,108,77,102,92,74,92,68,77);
                for(int[] n:new int[][]{{88,48},{124,98},{52,98}}){if(active)flow(p,n[0],n[1],88,77,time,MINT);else line(p,n[0],n[1],88,77,RIM);}
                ring(p,88,77,27,20,RIM);if(active)orbit(p,88,77,27,20,time,MINT);
                slot(p,88,77,MINT);slot(p,88,48,LILAC);slot(p,124,98,LILAC);slot(p,52,98,LILAC);
                frame(p,6,122,88,19,DARK);
            }
            default -> {
                for(int i=0;i<6;i++){var a=ASSEMBLY[i];var b=ASSEMBLY[(i+1)%6];line(p,a[0],a[1],b[0],b[1],RIM);
                    if(active)flow(p,a[0],a[1],88,78,time,MINT);else line(p,a[0],a[1],88,78,0xFF789AA8);}
                if(kind.equals("forge")){
                    // Four distinct tools echo the wand, tongs, engraving needle and hammer.
                    tool(p,26,46,0,BLUE);tool(p,149,46,1,MINT);tool(p,26,103,2,LILAC);tool(p,149,103,3,GOLD);
                    ring(p,88,78,16,16,active?LIGHT:LILAC);
                    if(active)orbit(p,88,78,16,16,time,GOLD);
                }
                for(int i=0;i<ASSEMBLY.length;i++)slot(p,ASSEMBLY[i][0],ASSEMBLY[i][1],i==6?GOLD:RIM);
            }
        }
    }
    private static void tool(Pixels p,int x,int y,int kind,int color){
        if(kind==1){line(p,x-4,y-5,x+1,y+6,color);line(p,x+4,y-5,x-1,y+6,color);p.fill(x-1,y-1,x+2,y+2,LIGHT);}
        else{p.fill(x-1,y-5,x+1,y+7,color);if(kind==3)p.fill(x-5,y-6,x+5,y-2,color);
            else if(kind==0)polygon(p,LIGHT,x,y-9,x+4,y-5,x,y-1,x-4,y-5);
            else{p.fill(x-3,y-2,x+3,y,color);p.fill(x,y-8,x+1,y-5,LIGHT);}}
    }
    public static void dome(Pixels p){
        polygon(p,RIM,68,17,108,17,129,25,143,43,143,109,33,109,33,43,47,25);
        polygon(p,0xC9527581,68,19,108,19,127,27,141,44,141,107,35,107,35,44,49,27);
        line(p,49,27,68,19,LIGHT);line(p,36,44,49,28,LIGHT);line(p,36,45,36,105,LIGHT);
        frame(p,22,113,132,33,0xFF526D79);
        p.fill(43,109,133,113,0xFF90B8C4);
    }
    public static void pool(Pixels p){
        frame(p,4,2,242,134,0xE8476773);
        // Keep the two working modules separate, with a stepped vessel around the fluid controls.
        polygon(p,RIM,15,23,145,23,150,28,150,100,142,109,20,109,12,100,12,28);
        polygon(p,0xFF3E606D,17,25,143,25,148,29,148,99,140,107,22,107,14,99,14,29);
        frame(p,160,20,68,68,0xFF476773);frame(p,150,97,88,32,0xFF476773);
        p.fill(154,27,155,90,0xFF92B4C0);
    }
    public static void grid(Pixels p,int count,int selected,float time){
        int color=count==3?LIGHT:count==5?MINT:count==8?LILAC:GOLD;
        ring(p,88,80,59,50,color);ring(p,88,80,44,32,0xFF7196A7);
        for(int i=0;i<count;i++){
            double a=i*Math.PI*2/count;int x=88+(int)Math.round(Math.sin(a)*52),y=80-(int)Math.round(Math.cos(a)*44);
            int xx=88+(int)Math.round(Math.sin((i+1)*Math.PI*2/count)*52),yy=80-(int)Math.round(Math.cos((i+1)*Math.PI*2/count)*44);
            line(p,x,y,xx,yy,color);
        }
        for(int i=0;i<count;i++){
            double a=i*Math.PI*2/count;int x=88+(int)Math.round(Math.sin(a)*52),y=80-(int)Math.round(Math.cos(a)*44);
            p.fill(x-9,y-9,x+9,y+9,i==selected?LIGHT:color);
            p.fill(x-8,y-8,x+8,y+8,0xFF547383);
            if(i==selected)p.fill(x-2,y-12,x+3,y-10,color);
        }
    }
    public static void aggregation(Pixels p,int[][] points,int[] colors,float time,float progress){
        for(int i=0;i<8;i++){
            var a=points[i];var b=points[(i+1)%8];int color=progress>0&&i<=Math.min(7,(int)(progress*8))?LIGHT:RIM;
            line(p,a[0]+8,a[1]+8,b[0]+8,b[1]+8,color);
            line(p,a[0]+8,a[1]+9,b[0]+8,b[1]+9,0xFF648FA1);
            if(progress>0&&i==(int)(progress*8))flow(p,a[0]+8,a[1]+8,b[0]+8,b[1]+8,time,LIGHT);
        }
        for(int i=0;i<points.length;i++){
            var a=points[i];int x=a[0]+8,y=a[1]+8,color=0xFF000000|colors[i];
            if(i<10){ring(p,x,y,13,13,color);p.fill(x-6,y-12,x-3,y-10,LIGHT);}
            slot(p,x,y,color);
        }
    }
    public static void engineControls(Pixels p,boolean kinetics,float time,int[] colors){
        frame(p,kinetics?0:76,18,kinetics?268:116,137,0xE84F707C);
        int[] hex={190,181,162,196,106,196,78,181,106,166,162,166};
        for(int i=0;i<6;i++){int j=(i+1)%6;line(p,hex[i*2],hex[i*2+1],hex[j*2],hex[j*2+1],RIM);
            if(i==(int)(time/12)%6)flow(p,hex[i*2],hex[i*2+1],hex[j*2],hex[j*2+1],time,MINT);}
        for(int i=0;i<4;i++){
            int y=43+i*28,color=colors[i];
            if(kinetics){frame(p,57,y-1,70,20,DARK);frame(p,137,y-1,43,20,DARK);
                p.fill(59,y+17,125,y+18,color);p.fill(139,y+17,178,y+18,color);}
        }
    }
    public static void reversalTrim(Pixels p,float time,boolean active){
        reversalTrim(p,time,active,false);
    }
    public static void reversalTrim(Pixels p,float time,boolean active,boolean infusion){
        frame(p,-29,34,26,101,0xE0355665);line(p,-3,68,38,68,RIM);line(p,-3,71,38,71,0xFF7196A7);
        for(int i=0;i<12;i++){double a=i*Math.PI/6;int x=88+(int)Math.round(Math.cos(a)*43),y=64+(int)Math.round(Math.sin(a)*43);p.fill(x,y,x+2,y+2,LIGHT);}
        if(active)orbit(p,88,64,47,47,-time,infusion?0xFFBD97EC:MINT);
    }
    public static void sundialTrim(Pixels p){
        for(int i=0;i<12;i++){double a=i*Math.PI/6;int x=100+(int)Math.round(Math.cos(a)*73),y=66+(int)Math.round(Math.sin(a)*40);p.fill(x,y,x+2,y+2,0xFFB6D6C4);}
    }
}
