package com.thero.worktile.view;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

public class WorktileIcons {
    public static final Icon WORKTILE_TOOL_ICON;
    public static final Icon OPEN_GROUP;
    static {
        WORKTILE_TOOL_ICON = IconLoader.findIcon("/icons/worktile.png");
        OPEN_GROUP  = IconLoader.findIcon("/icons/open.svg");
    }
}
