package com.thero.worktile.factory;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.thero.worktile.window.WorkTileWindow;
import org.jetbrains.annotations.NotNull;

public class WorkTileWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        WorkTileWindow workTileWindow = new WorkTileWindow(toolWindow);
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(workTileWindow.getContent(), "", false);
        toolWindow.getContentManager().addContent(content);

    }
}
