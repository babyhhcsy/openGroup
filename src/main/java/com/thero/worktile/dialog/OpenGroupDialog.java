package com.thero.worktile.dialog;

import com.github.hypfvieh.util.StringUtil;
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.MultiColumnListModel;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.ui.speedSearch.NameFilteringListModel;
import com.intellij.util.Function;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OpenGroupDialog extends JPanel implements ListSelectionListener {
    private JLabel picture;
    private JBList list;
    private JSplitPane splitPane;
    private String[] imageNames = { "Bird", "Cat", "Dog", "Rabbit", "Pig", "dukeWaveRed",
            "kathyCosmo", "lainesTongue", "left", "middle", "right", "stickerface"};
    public OpenGroupDialog(Project project) {

        java.util.List<String> items = Arrays.asList(imageNames);
        //获取最近打开的文件列表
        //List<VirtualFile> virtualFiles = Arrays.asList(IdeDocumentHistory.getInstance(project).getChangedFiles());
        list = new JBList(createModel(items));

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedIndex(0);
        list.addListSelectionListener(this);


        JScrollPane listScrollPane = new JScrollPane(list);
        picture = new JLabel();
        picture.setFont(picture.getFont().deriveFont(Font.ITALIC));
        picture.setHorizontalAlignment(JLabel.CENTER);

        JScrollPane pictureScrollPane = new JScrollPane(picture);

        //Create a split pane with the two scroll panes in it.
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                listScrollPane, pictureScrollPane);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation(150);

        //Provide minimum sizes for the two components in the split pane.
        Dimension minimumSize = new Dimension(100, 400);
        listScrollPane.setMinimumSize(minimumSize);
        pictureScrollPane.setMinimumSize(minimumSize);

        //Provide a preferred size for the split pane.
        splitPane.setPreferredSize(new Dimension(500, 400));

      /*  splitPane.setBorder(JBUI.Borders.empty());
        splitPane.setBorder(JBUI.CurrentTheme.Advertiser.border());
        splitPane.setForeground(JBUI.CurrentTheme.Advertiser.foreground());
        splitPane.setBackground(JBUI.CurrentTheme.Advertiser.background());
        setLocationRelativeTo(null);*/
        this.setBorder(JBUI.Borders.empty());
        this.add(splitPane);
        Window window = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
        if (window == null) {
            window = WindowManager.getInstance().getFrame(project);
        }
        JBPopup vpop = JBPopupFactory.getInstance().createComponentPopupBuilder(this, splitPane)
                .setTitle("分组打开")
                .setModalContext(false)
                .setFocusable(true)
                .setCancelOnWindowDeactivation(true)
                .setCancelOnOtherWindowOpen(true)
                .setMovable(true)
                .setMinSize(new Dimension(JBUIScale.scale(500), JBUIScale.scale(400))).createPopup();
        vpop.showInCenterOf(window);

    }
    private ListModel createModel(java.util.List<String> items){
        DefaultListModel m = new DefaultListModel();
        for(String item:items){
            m.addElement(item);
        }
        return m;
    }
    //Listens to the list
    public void valueChanged(ListSelectionEvent e) {
        JList list = (JList)e.getSource();
        updateLabel(imageNames[list.getSelectedIndex()]);
    }

    //Renders the selected image
    protected void updateLabel (String name) {
        ImageIcon icon = createImageIcon("images/" + name + ".gif");
        picture.setIcon(icon);
        if  (icon != null) {
            picture.setText(null);
        } else {
            picture.setText("Image not found");
        }
    }

    //Used by SplitPaneDemo2
    public JList getImageList() {
        return list;
    }

    public JSplitPane getSplitPane() {
        return splitPane;
    }


    /** Returns an ImageIcon, or null if the path was invalid. */
    protected static ImageIcon createImageIcon(String path) {
        java.net.URL imgURL = OpenGroupDialog.class.getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }

}
