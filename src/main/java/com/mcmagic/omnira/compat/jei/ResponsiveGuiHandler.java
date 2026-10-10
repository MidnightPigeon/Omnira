package com.mcmagic.omnira.compat.jei;

import com.mcmagic.omnira.client.screen.*;
import mezz.jei.api.gui.handlers.*;
import mezz.jei.api.gui.builder.IClickableIngredientFactory;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.runtime.IClickableIngredient;
import net.minecraft.client.gui.screens.Screen;
import java.util.List;
import java.util.Optional;

final class ResponsiveGuiHandler {
    static <T extends ResponsiveContainerScreen<?>> void register(IGuiHandlerRegistration registration,Class<T> type){
        registration.addGuiScreenHandler(type,new IScreenHandler<T>(){
            @Override public IGuiProperties apply(T screen){
                // JEI queries screens before Minecraft has initialized their viewport.
                if(screen.physicalWidth()<=1||screen.physicalHeight()<=1)return null;
                var bounds=screen.physicalBounds();
                return new IGuiProperties(){
                    public Class<? extends Screen> screenClass(){return type;}
                    public int guiLeft(){return bounds.getX();}
                    public int guiTop(){return bounds.getY();}
                    public int guiXSize(){return bounds.getWidth();}
                    public int guiYSize(){return bounds.getHeight();}
                    public int screenWidth(){return screen.physicalWidth();}
                    public int screenHeight(){return screen.physicalHeight();}
                };
            }
            @Override public Optional<? extends IClickableIngredient<?>> getClickableIngredientUnderMouse(IClickableIngredientFactory factory,T screen,double x,double y){
                for(var slot:screen.getMenu().slots){
                    var area=screen.physicalRect(screen.getGuiLeft()+slot.x,screen.getGuiTop()+slot.y,16,16);
                    if(slot.hasItem()&&area.contains((int)x,(int)y))return factory.createBuilder(slot.getItem()).buildWithArea(area);
                }
                return Optional.empty();
            }
        });
        registration.addGuiContainerHandler(type,new IGuiContainerHandler<T>(){
            @Override public List<net.minecraft.client.renderer.Rect2i> getGuiExtraAreas(T screen){return List.of(screen.physicalBounds());}
        });
    }
    static <T extends ResponsiveContainerScreen<?>> void clickArea(IGuiHandlerRegistration registration,Class<T> type,mezz.jei.api.recipe.RecipeType<?>... recipes){
        registration.addGuiContainerHandler(type,new IGuiContainerHandler<T>(){
            @Override public java.util.Collection<IGuiClickableArea> getGuiClickableAreas(T screen,double x,double y){
                var area=screen.physicalRect(screen.getGuiLeft()+28,screen.getGuiTop()+7,120,16);
                return List.of(IGuiClickableArea.createBasic(area.getX()-screen.getGuiLeft(),area.getY()-screen.getGuiTop(),area.getWidth(),area.getHeight(),recipes));
            }
        });
    }
    private ResponsiveGuiHandler(){}
}
