package com.mcmagic.omnira.client.hud;

import com.mcmagic.omnira.Omnira;
import com.mcmagic.omnira.config.ManaHudConfig;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ScreenEvent;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class ManaHudPosition {
    static final int WIDTH=81;
    private static final long HOLD_MS=400;
    record Point(int x,int y){}
    private static UUID playerId;
    private static Point saved, preview;
    private static Screen pressed;
    private static long pressedAt;
    private static double grabX,grabY,pressX,pressY;

    private static Path profile(){
        return FMLPaths.CONFIGDIR.get().resolve("omnira/hud").resolve(playerId+".txt");
    }
    private static void loadPlayer(){
        var id=Minecraft.getInstance().player.getUUID();
        if(id.equals(playerId))return;
        playerId=id;saved=null;cancel();
        var file=profile();
        if(Files.notExists(file))return;
        try{
            var parts=Files.readString(file).trim().split(",",-1);
            if(parts.length!=2)throw new NumberFormatException("Expected x,y");
            saved=new Point(Integer.parseInt(parts[0]),Integer.parseInt(parts[1]));
        }catch(IOException|NumberFormatException e){Omnira.LOGGER.warn("Cannot read mana HUD position {}",file,e);}
    }
    static Point position(int w,int h){
        loadPlayer();
        var offset=preview!=null?preview:saved;
        int x=offset==null?ManaHudConfig.OFFSET_X.get():offset.x;
        int y=offset==null?ManaHudConfig.OFFSET_Y.get():offset.y;
        long left=(long)(w/2-91)+x,top=(long)(h-22)+y;
        // On narrow displays the stock left-side placement would cover the hotbar.
        if(offset==null && x==-111 && y==4 && left<2){
            left=w/2-91;top=h-Minecraft.getInstance().gui.leftHeight-20;
        }
        return new Point(Math.clamp(left,2,Math.max(2,w-WIDTH-2)),Math.clamp(top,2,Math.max(2,h-15)));
    }
    static boolean editable(Screen screen){
        return screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen;
    }
    static boolean holding(){return pressed!=null;}
    private static void cancel(){pressed=null;preview=null;}
    @SubscribeEvent public static void press(ScreenEvent.MouseButtonPressed.Pre event){
        var screen=event.getScreen();var player=Minecraft.getInstance().player;
        if(event.getButton()!=0 || !editable(screen) || player==null || player.isSpectator()
                || !player.containerMenu.getCarried().isEmpty())return;
        var p=position(screen.width,screen.height);
        grabX=event.getMouseX()-p.x;grabY=event.getMouseY()-p.y;
        if(grabX< -1 || grabX>WIDTH || grabY< -1 || grabY>14)return;
        pressX=event.getMouseX();pressY=event.getMouseY();
        pressed=screen;pressedAt=Util.getMillis();
        event.setCanceled(true);
    }
    private static void move(Screen screen,double x,double y){
        if(Util.getMillis()-pressedAt<HOLD_MS || (preview==null && Math.hypot(x-pressX,y-pressY)<1))return;
        int left=Math.clamp(Math.round(x-grabX),2,Math.max(2,screen.width-WIDTH-2));
        int top=Math.clamp(Math.round(y-grabY),2,Math.max(2,screen.height-15));
        preview=new Point(left-(screen.width/2-91),top-(screen.height-22));
    }
    @SubscribeEvent public static void drag(ScreenEvent.MouseDragged.Pre event){
        if(pressed!=event.getScreen() || event.getMouseButton()!=0)return;
        move(pressed,event.getMouseX(),event.getMouseY());event.setCanceled(true);
    }
    @SubscribeEvent public static void release(ScreenEvent.MouseButtonReleased.Pre event){
        if(pressed!=event.getScreen() || event.getButton()!=0)return;
        move(pressed,event.getMouseX(),event.getMouseY());
        if(preview==null && Util.getMillis()-pressedAt<HOLD_MS){
            // A tap on a bar over a slot still performs the ordinary inventory click.
            var screen=pressed;
            cancel();
            screen.mouseClicked(pressX,pressY,0);
            screen.mouseReleased(event.getMouseX(),event.getMouseY(),0);
            event.setCanceled(true);
            return;
        }
        if(preview!=null){
            var file=profile();var temp=file.resolveSibling(file.getFileName()+".tmp");
            try{
                Files.createDirectories(file.getParent());
                Files.writeString(temp,preview.x+","+preview.y);
                try{Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
                catch(AtomicMoveNotSupportedException e){Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);}
                saved=preview;
            }catch(IOException e){Omnira.LOGGER.warn("Cannot save mana HUD position {}",file,e);}
        }
        cancel();event.setCanceled(true);
    }
    @SubscribeEvent public static void close(ScreenEvent.Closing event){if(pressed==event.getScreen())cancel();}
    private ManaHudPosition(){}
}
