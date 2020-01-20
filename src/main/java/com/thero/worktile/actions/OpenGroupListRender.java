package com.thero.worktile.actions;

import com.intellij.openapi.ui.GraphicsConfig;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.SpeedSearchBase;
import com.intellij.util.ui.JBUI;
import com.thero.worktile.model.GroupModel;
import com.thero.worktile.view.WorktileIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class OpenGroupListRender extends ColoredListCellRenderer<Object> {


    @Override
    protected void customizeCellRenderer(@NotNull JList<?> list, Object value, int index, boolean selected, boolean hasFocus) {
        setBorder(JBUI.Borders.empty());
        if(value instanceof GroupModel){
            GroupModel groupModel = (GroupModel) value;
            setPaintFocusBorder(false);
            setIcon(WorktileIcons.OPEN_GROUP);
            String name = groupModel.getGroupName();
            append(name);
        }
    }

    /**
     * 在list面板中产生了一个面板，设置了透明度，遮住list
     * @param g
     */
    @Override
    protected void doPaint(Graphics2D g) {
       /* GraphicsConfig config = new GraphicsConfig(g);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,0.15f));*/
        super.doPaint(g);
        //config.restore();
    }
}
