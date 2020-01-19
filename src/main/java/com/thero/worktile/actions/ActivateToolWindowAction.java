package com.thero.worktile.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import org.jetbrains.annotations.NotNull;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class ActivateToolWindowAction extends DumbAwareAction {
    private final String myToolWindowId;

    public ActivateToolWindowAction(String myToolWindowId) {
        this.myToolWindowId = myToolWindowId;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = getEventProject(e);
        if (project == null) return;
        ToolWindowManager windowManager = ToolWindowManager.getInstance(project);
        final ToolWindow window = windowManager.getToolWindow(myToolWindowId);
        InputEvent event = e.getInputEvent();
        Runnable run = null;
        if (event instanceof KeyEvent && event.isShiftDown()) {
            final Content[] contents = window.getContentManager().getContents();
            if (contents.length > 0 && window.getContentManager().getSelectedContent() != contents[0]) {
                run = () -> window.getContentManager().setSelectedContent(contents[0], true, true);
            }
        }
        if (windowManager.isEditorComponentActive() || !myToolWindowId.equals(windowManager.getActiveToolWindowId()) || run != null) {
            if (run != null && window.isActive()) {
                run.run();
            } else {
                window.activate(run);
            }
        } else {
            windowManager.getToolWindow(myToolWindowId).hide(null);
        }
    }
}
