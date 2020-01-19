package com.thero.worktile.actions;

import com.intellij.ide.actions.RecentLocationsAction;
import com.intellij.openapi.actionSystem.ShortcutSet;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.ui.GraphicsConfig;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.*;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.IconUtil;
import com.intellij.util.PlatformIcons;
import com.intellij.util.ui.JBUI;
import com.twelvemonkeys.lang.StringUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

import java.awt.*;
import java.util.Map;
import java.util.function.Supplier;

import static com.thero.worktile.actions.Switcher.SwitcherPanel.RECENT_LOCATIONS;

/**
 * toolWindow被选择后颜色发生变化
 */
public class SwitcherToolWindowsListRenderer extends ColoredListCellRenderer<Object> {
    private final Map<ToolWindow, String> shortcuts;
    private final boolean myPinned;
    private final SpeedSearchBase mySpeedSearch;
    private boolean hide = false;
    //每次get的时候都会得到一个新的对象
    private Supplier<Boolean> myShowEdited;

    public SwitcherToolWindowsListRenderer(SpeedSearchBase speedSearch,
                                           Map<ToolWindow, String> shortcuts,
                                           boolean pinned,
                                           @NotNull Supplier<Boolean> showEdited) {
        mySpeedSearch = speedSearch;
        this.shortcuts = shortcuts;
        myPinned = pinned;
        myShowEdited = showEdited;
    }

    @Override
    protected void customizeCellRenderer(@NotNull JList<?> jList, Object value,
                                         int index, boolean selected, boolean hasFocus) {
        //应用外部类中的常量
        setBorder(value==RECENT_LOCATIONS?
                JBUI.Borders.customLine(selected?getBackground():new JBColor(Gray._220, Gray._80),1,0,0,0):JBUI.Borders.empty());
        String nameToMath = "";
        if(value instanceof ToolWindow){
            ToolWindow tw = (ToolWindow) value;
            hide = false;
            setPaintFocusBorder(false);
            setIcon(getIcon(tw));
            nameToMath = tw.getStripeTitle();
            String shortCut = shortcuts.get(tw);
            String name;
            if(myPinned || shortCut == null){
                name = nameToMath;
            }else{
                append(shortCut,new SimpleTextAttributes(SimpleTextAttributes.STYLE_UNDERLINE,null));
                name = ": " + nameToMath;
            }
            append(name);
        }else if(value == RECENT_LOCATIONS){
            String label = Switcher.SwitcherPanel.getRecentLocationsLabel(myShowEdited);
            nameToMath = label;
            ShortcutSet shortcuts = KeymapUtil.getActiveKeymapShortcuts(RecentLocationsAction.RECENT_LOCATIONS_ACTION_ID);
            append(label);
            if(!myShowEdited.get()){
                append(" ").append(KeymapUtil.getShortcutsText(shortcuts.getShortcuts()),SimpleTextAttributes.GRAY_ATTRIBUTES);
            }
        }
        if(mySpeedSearch !=null && mySpeedSearch.isPopupActive()){
            hide = mySpeedSearch.matchingFragments(nameToMath) == null && !StringUtil.isEmpty(mySpeedSearch.getEnteredPrefix());
        }
    }

    /**
     * 设置透明度
     * @param g
     */
    @Override
    protected void doPaint(Graphics2D g) {
        GraphicsConfig config = new GraphicsConfig(g);
        if(hide){
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,0.15f));
        }
        super.doPaint(g);
        config.restore();
    }

    /**
     * 获得toolWindow上的icon图标
     * @param toolWindow
     * @return
     */
    private static Icon getIcon(ToolWindow toolWindow){
        Icon icon = toolWindow.getIcon();
        if (icon == null) {
            return PlatformIcons.UI_FORM_ICON;
        }
        return IconUtil.toSize(icon, JBUIScale.scale(16),JBUIScale.scale(16));
    }
}
