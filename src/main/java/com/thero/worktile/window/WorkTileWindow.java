package com.thero.worktile.window;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.source.xml.XmlFileImpl;
import com.intellij.psi.xml.XmlFile;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomManager;
import com.intellij.util.xml.DomUtil;
import com.thero.worktile.actions.OpenGrouper;
import com.thero.worktile.actions.Switcher;
import com.thero.worktile.model.ColumnBase;
import com.thero.worktile.model.GroupModel;
import com.thero.worktile.model.Groups;
import com.thero.worktile.model.Projects;
import com.thero.worktile.util.FastJsonUtil;
import com.thero.worktile.util.XmlUtil;
import com.thero.worktile.view.WorktileIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.io.File;
import java.util.List;

public class WorkTileWindow {
    private JPanel mainPanel;
    private JPanel workTileWindowContent;
    JScrollPane scrollPane ;
    private JBTable varTable;
    private ToolbarDecorator toolbarDecorator;
    JPanel toolPan;
    private JSplitPane jSplitPane;
    public WorkTileWindow(ToolWindow toolWindow) {
        createTableComponents(toolWindow);
    }


    public void createTableComponents(ToolWindow toolWindow){
        workTileWindowContent = new JPanel();
        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        workTileWindowContent.setLayout(new BorderLayout());
        varTable = new JBTable();
        scrollPane = new JScrollPane(varTable);
        varTable.getEmptyText().setText("暂无信息");

        varTable.setFillsViewportHeight(true);
        //不可整列移动
        ToolbarDecorator.createDecorator(varTable);
        String s = "[" +
                "   {" +
                "      \"pid\":\"eda54f766b7f457ea7cdb5a93c6d892e\"," +
                "      \"name\":\"Worktile 开发\"," +
                "      \"team_id\":\"cds4ls3f5c4ee8\"," +
                "      \"desc\":\"\"," +
                "      \"archived\":0," +
                "      \"pic\":\"icon-github\"," +
                "      \"bg\":\"#5d8d0b\"," +
                "      \"visibility\":2," +
                "      \"is_star\":0," +
                "      \"pos\":65536.5," +
                "      \"member_count\":20," +
                "      \"curr_role\":1," +
                "      \"permission\":31" +
                "   }" +
                "]";
        List<Projects> projects3 = FastJsonUtil.parseArray(s, Projects.class);
        setVariables(new Projects(), projects3);
        setToolBtn();
        mainPanel.add(toolPan,BorderLayout.NORTH);
        mainPanel.add(workTileWindowContent,BorderLayout.CENTER);
        workTileWindowContent.add(varTable.getTableHeader(),BorderLayout.NORTH);
        workTileWindowContent.add(varTable,BorderLayout.CENTER);
    }
    public void setToolBtn(){
        toolbarDecorator = ToolbarDecorator.createDecorator(varTable)
                .setToolbarPosition(ActionToolbarPosition.TOP)
        .addExtraAction(new AnActionButton("Import", AllIcons.Actions.AddMulticaret) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
                int selectedRow = varTable.getSelectedRow();
                TableColumn column = varTable.getColumn(selectedRow);
                Messages.showInfoMessage("请选择模版压缩文件(.zip)", "ERROR");
                return ;
            }
        })
        .addExtraAction(new AnActionButton("openGroup", WorktileIcons.OPEN_GROUP) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
               // OpenGroupDialog openGroupDialog = new OpenGroupDialog(anActionEvent.getProject());
                Switcher.createAndShowSwitcher(e, "Recent Files", "RecentFiles",false, true);
            }
        }).addExtraAction(new AnActionButton("openGroups",AllIcons.Actions.GroupByFile) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        OpenGrouper.createAndShowOpenGrouper(e.getProject(),"open Grouper","open-gouper");
                    }
        }).addExtraAction(new AnActionButton("createFile",AllIcons.Actions.New) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        String path = e.getProject().getBasePath();
                        DomManager domManager = DomManager.getDomManager(e.getProject());
                        File file = new File(path+ File.separator + ".idea"+ File.separator+"open-group.xml");
                        VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByIoFile(file);

                        PsiFile xmlFile = PsiManager.getInstance(e.getProject()).findFile(virtualFile);
                        String text = xmlFile.getText();
                        try {
                            Groups groups = XmlUtil.toBean(text, Groups.class);
                        } catch (InstantiationException ex) {
                            ex.printStackTrace();
                        } catch (IllegalAccessException ex) {
                            ex.printStackTrace();
                        }
                        XmlFileImpl temp = (XmlFileImpl) xmlFile;
                        DomManager manager = DomManager.getDomManager(e.getProject());
                        DomFileElement<DomElement> fileElement = manager.getFileElement(temp);
                        Editor data = e.getData(PlatformDataKeys.EDITOR);

                    }
                });
        toolPan = toolbarDecorator.createPanel();

    }
    private void setVariables(ColumnBase columnBase, List<Projects> projects){
        String[] columnNames = columnBase.columnNames();
        Object[][] tableVales = new String[projects.size()][columnNames.length];
        for (int row = 0; row < tableVales.length; row++) {
            tableVales[row][0] = projects.get(row).getPid();
            tableVales[row][1] = projects.get(row).getName();
            tableVales[row][2] = projects.get(row).getTeam_id();
            tableVales[row][3] = projects.get(row).getDesc();
            tableVales[row][4] = projects.get(row).getArchived()+"";
            tableVales[row][5] = projects.get(row).getPic();
            tableVales[row][6] = projects.get(row).getBg();
            tableVales[row][7] = projects.get(row).getVisibility()+"";
            tableVales[row][8] = projects.get(row).getIs_star()+"";
            tableVales[row][9] = projects.get(row).getPos();
            tableVales[row][10] = projects.get(row).getMember_count()+"";
            tableVales[row][11] = projects.get(row).getCurr_role()+"";
            tableVales[row][12] = projects.get(row).getPermission()+"";
        }
        DefaultTableModel tableModel = new DefaultTableModel(tableVales, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        varTable.setModel(tableModel);
    }
    public void initSplitPanel(){

    }
    public JPanel getContent() {
        return mainPanel;
    }
}
