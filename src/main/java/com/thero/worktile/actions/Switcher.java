package com.thero.worktile.actions;

import com.intellij.ide.DataManager;
import com.intellij.ide.IdeEventQueue;
import com.intellij.ide.actions.ActivateToolWindowAction;
import com.intellij.ide.actions.ToolWindowsGroup;
import com.intellij.ide.ui.UISettings;
import com.intellij.ide.util.gotoByName.QuickSearchComponent;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.Experiments;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory;
import com.intellij.openapi.fileEditor.impl.EditorTabPresentationUtil;
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.fileEditor.impl.*;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.impl.ToolWindowImpl;
import com.intellij.problems.WolfTheProblemSolver;
import com.intellij.ui.*;
import com.intellij.ui.border.CustomLineBorder;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.ui.popup.PopupUpdateProcessorBase;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.ui.speedSearch.NameFilteringListModel;
import com.intellij.ui.speedSearch.SpeedSearchUtil;
import com.intellij.util.Alarm;
import com.intellij.util.Function;
import com.intellij.util.IconUtil;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import com.intellij.ide.actions.Switcher.SwitcherPanel;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.TextAttribute;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;

public class Switcher extends AnAction implements DumbAware {
    private static final Color ON_MOUSE_OVER_BG_COLOR = new JBColor(new Color(231, 242, 249), new Color(77, 80, 84));
    private static final int MINIMUM_HEIGHT = JBUIScale.scale(400);
    private static final int MINIMUM_WIDTH = JBUIScale.scale(500);
    private static final Color SHORTCUT_FOREGROUND_COLOR = UIUtil.getContextHelpForeground();
    private static final Key<SwitcherPanel> SWITCHER_KEY = Key.create("SWITCHER_KEY");
    private static final String TOGGLE_CHECK_BOX_ACTION_ID = "SwitcherRecentEditedChangedToggleCheckBox";

    public static final String SHORTCUT_HEX_COLOR = String.format("#%02x%02x%02x",
            SHORTCUT_FOREGROUND_COLOR.getRed(),
            SHORTCUT_FOREGROUND_COLOR.getGreen(),
            SHORTCUT_FOREGROUND_COLOR.getBlue());
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Project project = e.getProject();
        if (project == null) return;
        createAndShowSwitcher(e, "Switcher", "Switcher", false, false);
    }
    public static SwitcherPanel createAndShowSwitcher(@NotNull AnActionEvent e,@NotNull String title
     ,@NotNull String actionId,boolean onlyEdited,boolean pinned){
        Project project = e.getProject();
        //TODO: 处理创建Switcher面板的创建 254 line
        return new SwitcherPanel(project, title, actionId, onlyEdited, pinned);
    }

    public static class SwitcherPanel extends JPanel implements KeyListener, MouseListener, MouseMotionListener,
            QuickSearchComponent, DataProvider{
        //最近打开文件所在的位置
        static final Object RECENT_LOCATIONS = new Object();
        final JBPopup myPopup;
        final JBList<Object> toolWindows;
        final JBList<FileInfo> files;
        final ToolWindowManager twManager;
        //最近打开的文件夹，标题头“show change only 复选框”
        JBCheckBox myShowOnlyEditedFilesCheckBox;
        final JLabel pathLabel = new JLabel(" ");
        final JPanel myTopPanel;
        //描述信息
        final JPanel descriptions;
        final Project project;
        private final boolean myPinned;
        final Map<String, ToolWindow> twShortcuts;
        final Alarm myAlarm;
        final SwitcherSpeedSearch mySpeedSearch;
        final String myTitle;
        private JBPopup myHint;

        final ClickListener myClickListener = new ClickListener() {
            @Override
            public boolean onClick(@NotNull MouseEvent e, int i) {
                if(myPinned && (e.isControlDown() || e.isMetaDown() || e.isShiftDown())) return false;
                final Object source = e.getSource();
                if (source instanceof JList){
                    JList jList = (JList) source;
                    if(jList.getSelectedIndex() == -1 && jList.getAnchorSelectionIndex() !=-1){
                        //定位选择索引
                        jList.setSelectedIndex(jList.getAnchorSelectionIndex());
                    }
                    if(jList.getSelectedIndex()!=-1){
                        //TODO: navigate 方法
                    }
                }
                return true;
            }
        };
        /**
         * @param project
         * @param title
         * @param actionId
         * @param onlyEdited
         * @param pinned 是否支持多选
         */
        SwitcherPanel(@NotNull final Project project,@NotNull String title,@NotNull String actionId,
                      boolean onlyEdited,boolean pinned){
            setLayout(new BorderLayout());
            this.project= project;
            myTitle = title;
            myPinned = pinned;
            mySpeedSearch = pinned?new SwitcherSpeedSearch(this):null;

            //对面板进行布局
            setBorder(JBUI.Borders.empty());
            setBackground(JBColor.background());
            //设置文本左对齐
            pathLabel.setHorizontalAlignment(SwingConstants.LEFT);
            final Font font = pathLabel.getFont();
            //通过复制当前 Font 对象并应用新的大小
            pathLabel.setFont(font.deriveFont(Math.max(10f,font.getSize()-4f)));
            //描述信息是一个面板
            descriptions = new JPanel(new BorderLayout());
            //设置边框，设置前景色和背景色
            pathLabel.setBorder(JBUI.CurrentTheme.Advertiser.border());
            pathLabel.setForeground(JBUI.CurrentTheme.Advertiser.foreground());
            pathLabel.setBackground(JBUI.CurrentTheme.Advertiser.background());
            //是否置顶
            pathLabel.setOpaque(true);
            descriptions.setBorder(new CustomLineBorder(JBUI.CurrentTheme.Advertiser.borderColor(), JBUI.insetsTop(1)));
            descriptions.add(pathLabel, BorderLayout.CENTER);
            //获得toolwindow管理者
            twManager = ToolWindowManager.getInstance(project);
            CollectionListModel<Object> twModel = new CollectionListModel<>();
            List<ActivateToolWindowAction> actions = ToolWindowsGroup.getToolWindowActions(project, true);
            List<ToolWindow> windows = new ArrayList<>();
            //获得当前已激活的toolWindows列表，并把他放到集合当中，以便在页面中展示
            for (ActivateToolWindowAction action : actions) {
                ToolWindow tw = twManager.getToolWindow(action.getToolWindowId());
                //toolwindow是否处于激活的状态
                if (tw.isAvailable()) {
                    windows.add(tw);
                }
            }
            twShortcuts = createShortcuts(windows);
            final Map<ToolWindow, String> map = ContainerUtil.reverseMap(twShortcuts);
            Collections.sort(windows, (o1, o2) -> StringUtil.compare(map.get(o1), map.get(o2), false));
            for (ToolWindow window : windows) {
                twModel.add(window);
            }
            twModel.add(RECENT_LOCATIONS);
            toolWindows = createList(twModel, getNamer(), mySpeedSearch, pinned);
            toolWindows.addFocusListener(new MyToolWindowsListFocusListener());
            toolWindows.setPreferredSize(new Dimension(JBUI.scale(200), toolWindows.getPreferredSize().height));
            toolWindows.setBorder(JBUI.Borders.empty(5, 5, 5, 20));
            //设置选择的模式
            toolWindows.setSelectionMode(pinned ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION : ListSelectionModel.SINGLE_SELECTION);
            //设置选中的颜色，和已经选择了的颜色
            toolWindows.setCellRenderer(new SwitcherToolWindowsListRenderer(mySpeedSearch,map,myPinned,showEdited()){
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean hasFocus) {
                    Component renderer = super.getListCellRendererComponent(list, value, index, selected, hasFocus);
                    if(selected){
                        return renderer;
                    }
                    //判断list的格式和索引的下标
                    final Color bgColor = list == mouseMoveSrc && index == mouseMoveListIndex ? ON_MOUSE_OVER_BG_COLOR : list.getBackground();
                    UIUtil.changeBackGround(renderer,bgColor);
                    return renderer;
                }
            });
            toolWindows.addKeyListener(this);
            //注册上下滚动所需要使用的快捷键
            ScrollingUtil.installActions(toolWindows);
            toolWindows.addMouseListener(this);
            toolWindows.addMouseMotionListener(this);
            //已选择的列表
            ScrollingUtil.ensureSelectionExists(toolWindows);
            myClickListener.installOn(toolWindows);
            toolWindows.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    if(!toolWindows.isSelectionEmpty() && files.isEmpty()){
                        files.clearSelection();
                    }
                }
            });

            final Pair<List<FileInfo>, Integer> filesAndSelection = getFilesToShowAndSelectionIndex(
                    project, collectFiles(project, onlyEdited), toolWindows.getModel().getSize(), isPinnedMode());
            final int selectionIndex = filesAndSelection.getSecond();
            final CollectionListModel<FileInfo> filesModel = new CollectionListModel<FileInfo>();
            for(FileInfo editor:filesAndSelection.getFirst()){
                filesModel.add(editor);
            }
            final VirtualFilesRenderer filesRenderer = new VirtualFilesRenderer(this) {
                //创建一个透明的面板
                final JPanel mypanel = new NonOpaquePanel(new BorderLayout());
                {
                    mypanel.setBackground(UIUtil.getListBackground());
                }
                @NotNull
                @Override
                public Component getListCellRendererComponent(JList<? extends FileInfo> list, FileInfo value, int index, boolean selected, boolean hasFocus) {
                    Component c = super.getListCellRendererComponent(list, value, index, selected, hasFocus);
                    mypanel.removeAll();
                    mypanel.add(c,BorderLayout.CENTER);
                    mypanel.getAccessibleContext().setAccessibleName(c.getAccessibleContext().getAccessibleName());
                    VirtualFile file = value.first;
                    //在文件夹的底部显示文件的绝对位置
                    String presentableUrl = ObjectUtils.notNull(file.getParent(), file).getPresentableUrl();
                    String location = FileUtil.getLocationRelativeToUserHome(presentableUrl);
                    mypanel.getAccessibleContext().setAccessibleDescription(location);
                    if(!selected && list==mouseMoveSrc && index ==mouseMoveListIndex){
                        setBackground(ON_MOUSE_OVER_BG_COLOR);
                    }
                    return mypanel;
                }

                @Override
                protected void customizeCellRenderer(@NotNull JList<? extends FileInfo> list, FileInfo value, int index, boolean selected, boolean hasFoucs) {
                    setPaintFocusBorder(false);
                    super.customizeCellRenderer(list, value, index, selected, hasFoucs);
                }
            };
            //列表变更时监听工具
            final ListSelectionListener listSelectionListener = new ListSelectionListener() {
                @Nullable
                private String getTitle2Text(@Nullable String fullText) {
                    int labelWidth = pathLabel.getWidth();
                    if (fullText == null || fullText.length() == 0) return " ";
                    //如果文件的路径超过宽度，那么从第4个/开始截取，中间的用省略号来代替
                    while (pathLabel.getFontMetrics(pathLabel.getFont()).stringWidth(fullText) > labelWidth) {
                        int sep = fullText.indexOf(File.separatorChar, 4);
                        if (sep < 0) return fullText;
                        fullText = "..." + fullText.substring(sep);
                    }
                    return fullText;
                }

                @Override
                public void valueChanged(@NotNull final ListSelectionEvent e) {
                    if (e.getValueIsAdjusting()) return;
                    updatePathLabel();
                    PopupUpdateProcessorBase popupUpdater = myHint == null || !myHint.isVisible() ? null : myHint.getUserData(PopupUpdateProcessorBase.class);
                    if (popupUpdater != null) {
                        popupUpdater.updatePopup(CommonDataKeys.PSI_ELEMENT.getData(
                                DataManager.getInstance().getDataContext(SwitcherPanel.this)
                        ));
                    }
                }

                private void updatePathLabel() {
                    List<FileInfo> values = files.getSelectedValuesList();
                    if (values != null && values.size() == 1) {
                        VirtualFile file = values.get(0).first;
                        String presentableUrl = ObjectUtils.notNull(file.getParent(), file).getPresentableUrl();
                        pathLabel.setText(getTitle2Text(FileUtil.getLocationRelativeToUserHome(presentableUrl)));
                    } else {
                        pathLabel.setText(" ");
                        ;
                    }
                }
            };

            files = createList(filesModel, FileInfo::getNameForRendering, mySpeedSearch, pinned);
            files.setSelectionMode(pinned?ListSelectionModel.MULTIPLE_INTERVAL_SELECTION:ListSelectionModel.SINGLE_INTERVAL_SELECTION);
            files.getSelectionModel().addListSelectionListener(e->{
                if(!files.isSelectionEmpty() && !toolWindows.isSelectionEmpty()){
                    toolWindows.getSelectionModel().clearSelection();
                }
            });
            files.getSelectionModel().addListSelectionListener(listSelectionListener);
            files.setCellRenderer(filesRenderer);
            files.setBorder(JBUI.Borders.empty(5));
            files.addKeyListener(this);
            ScrollingUtil.installActions(files);
            files.addMouseListener(this);
            files.addMouseMotionListener(this);
            files.addFocusListener(new MyFilesListFocusListener());
            myClickListener.installOn(files);
            ScrollingUtil.ensureSelectionExists(files);

            myShowOnlyEditedFilesCheckBox = new MyCheckBox(ToggleCheckBoxAction.isEnabled()?TOGGLE_CHECK_BOX_ACTION_ID:actionId,onlyEdited);
            myTopPanel = createTopPanel(myShowOnlyEditedFilesCheckBox,"Recent Files",pinned);
            if (isCheckboxMode()) {
                myShowOnlyEditedFilesCheckBox.addActionListener(e -> setShowOnlyEditedFiles(myShowOnlyEditedFilesCheckBox.isSelected()));
                myShowOnlyEditedFilesCheckBox.addActionListener(e -> toolWindows.repaint());
            }
            else {
                myShowOnlyEditedFilesCheckBox.setEnabled(false);
                myShowOnlyEditedFilesCheckBox.setVisible(false);
            }
            this.add(myTopPanel,BorderLayout.NORTH);
            this.add(toolWindows,BorderLayout.WEST);
            this.add(descriptions,BorderLayout.SOUTH);

            myPopup = JBPopupFactory.getInstance().createComponentPopupBuilder(this,toolWindows)
            .setResizable(pinned)
            .setModalContext(false)
            .setFocusable(true)
            .setRequestFocus(true)
            .setCancelOnWindowDeactivation(true)
            .setCancelOnOtherWindowOpen(true)
            .setMovable(pinned)
            .setMinSize(new Dimension(MINIMUM_WIDTH,MINIMUM_HEIGHT)).createPopup();
            myAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD,myPopup);
            IdeEventQueue.getInstance().getPopupManager().closeAllPopups(false);
            Window window = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
            if (window == null) {
                window = WindowManager.getInstance().getFrame(project);
            }
            myPopup.showInCenterOf(window);
            fromListToList(toolWindows, files);
            fromListToList(files, toolWindows);
        }
        boolean isCheckboxMode() {
            return isPinnedMode() && Experiments.getInstance().isFeatureEnabled("recent.and.edited.files.together");
        }
        void setShowOnlyEditedFiles(boolean onlyEdited) {
            if (myShowOnlyEditedFilesCheckBox.isSelected() != onlyEdited) {
                myShowOnlyEditedFilesCheckBox.setSelected(onlyEdited);
            }

            final boolean listWasSelected = files.getSelectedIndex() != -1;

            final Pair<List<FileInfo>, Integer> filesAndSelection = getFilesToShowAndSelectionIndex(
                    project, collectFiles(project, onlyEdited), toolWindows.getModel().getSize(), isPinnedMode());
            final int selectionIndex = filesAndSelection.getSecond();

            ListModel<FileInfo> model = files.getModel();
            ListUtil.removeAllItems(model);
            ListUtil.addAllItems(model, filesAndSelection.getFirst());

            if (selectionIndex > -1 && listWasSelected) {
                files.setSelectedIndex(selectionIndex);
            }
            files.revalidate();
            files.repaint();
        }
        private static void fromListToList(JBList from, JBList to) {
            AbstractAction action = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    to.requestFocus();
                }
            };
            ActionMap map = from.getActionMap();
            map.put(ListActions.Left.ID, action);
            map.put(ListActions.Right.ID, action);
        }
        public boolean isPinnedMode() {
            return myPinned;
        }
        @NotNull
        static List<VirtualFile> collectFiles(@NotNull Project project, boolean onlyEdited) {
            return onlyEdited ? Arrays.asList(IdeDocumentHistory.getInstance(project).getChangedFiles())
                    : getRecentFiles(project);
        }
        @NotNull
        public static List<VirtualFile> getRecentFiles(@NotNull Project project){
            List<VirtualFile> recentFiles = EditorHistoryManager.getInstance(project).getFileList();
            //获得打开的所有文件
            VirtualFile[] openFiles = FileEditorManager.getInstance(project).getOpenFiles();
            HashSet<VirtualFile> recentFilesSet = new HashSet<>(recentFiles);
            HashSet<VirtualFile> openFilesSet = ContainerUtil.newHashSet(openFiles);
            int index = 0;
            for (int i = 0; i < recentFiles.size(); i++) {
                if (openFilesSet.contains(recentFiles.get(i))) {
                    index = i;
                    break;
                }
            }
            List<VirtualFile> result = new ArrayList<>(recentFiles);
            result.addAll(index, ContainerUtil.filter(openFiles, it -> !recentFilesSet.contains(it)));
            return result;


        }
        @NotNull
        static Pair<List<FileInfo>,Integer> getFilesToShowAndSelectionIndex(@NotNull Project project,
                                                                            @NotNull List<VirtualFile> filesForInit,
                                                                            int toolWindowsCount,boolean pinned){
            int selectionIndex = -1;
            final FileEditorManagerImpl editorManager = (FileEditorManagerImpl) FileEditorManager.getInstance(project);
            final ArrayList<FileInfo> filesData = new ArrayList<>();
            final ArrayList<FileInfo> editors = new ArrayList<>();
            if(!pinned){
                for (Pair<VirtualFile, EditorWindow> pair : editorManager.getSelectionHistory()) {
                    editors.add(new FileInfo(pair.first, pair.second, project));
                }
            }
            if(editors.size()<2 || pinned){
                if(pinned && editors.size()>1){
                    filesData.addAll(editors);
                }
                int maxFiles = Math.max(editors.size(), filesForInit.size());
                int minIndex = pinned?0:(filesForInit.size() - Math.min(toolWindowsCount,maxFiles));
                boolean firstRecentMarked = false;
                List<VirtualFile> selectedFiles = Arrays.asList(editorManager.getSelectedFiles());
                //获得当前编辑器对象
                EditorWindow currentWindow = editorManager.getCurrentWindow();
                VirtualFile currentFile = currentWindow!=null ?currentWindow.getSelectedFile():null;
                for(int i = filesForInit.size() -1 ; i >= minIndex;i--){
                    if(pinned && UISettings.getInstance().getEditorTabPlacement()!=UISettings.TABS_NONE
                        && selectedFiles.contains(filesForInit.get(i))){
                        continue;
                    }
                    FileInfo info = new FileInfo(filesForInit.get(i), null, project);
                    boolean add = true;
                    if (pinned) {
                        for (FileInfo fileInfo : filesData) {
                            if (fileInfo.first.equals(info.first)) {
                                add = false;
                                break;
                            }
                        }
                    }
                    if (add) {
                        filesData.add(info);
                        if (!firstRecentMarked && !info.first.equals(currentFile)) {
                            selectionIndex = filesData.size() - 1;
                            firstRecentMarked = true;
                        }
                    }
                }

                if (editors.size() == 1 && (filesData.isEmpty() || !editors.get(0).getFirst().equals(filesData.get(0).getFirst()))) {
                    filesData.add(0, editors.get(0));
                }
            }else {
                for (int i = 0; i < Math.min(30, editors.size()); i++) {
                    filesData.add(editors.get(i));
                }
            }
            return Pair.create(filesData, selectionIndex);
        }
        @NotNull
        private static Map<String,ToolWindow> createShortcuts(@NotNull List<ToolWindow> windows){
            final Map<String, ToolWindow> keymap = new HashMap<>(windows.size());
            final List<ToolWindow> otherTW = new ArrayList<>();
            for (ToolWindow window : windows) {
                //Mnemonic助记符
                int index = ActivateToolWindowAction.getMnemonicForToolWindow(((ToolWindowImpl)window).getId());
                if (index >= '0' && index <= '9') {
                    keymap.put(getIndexShortcut(index - '0'), window);
                }
                else {
                    otherTW.add(window);
                }
            }
            int i = 0;
            for (ToolWindow window : otherTW) {
                String bestShortcut = getSmartShortcut(window, keymap);
                if (bestShortcut != null) {
                    keymap.put(bestShortcut, window);
                    continue;
                }

                while (keymap.get(getIndexShortcut(i)) != null) {
                    i++;
                }
                keymap.put(getIndexShortcut(i), window);
                i++;
            }
            return keymap;
        }

        /**
         * 获得当前显示的名字
         * @return
         */
        @NotNull
        private Function<? super Object, String> getNamer() {
            return value -> {
                if (value instanceof ToolWindow) {
                    return ((ToolWindow)value).getStripeTitle();
                }
                //如果是最近打开的文件
                if (value == RECENT_LOCATIONS) {
                    return getRecentLocationsLabel(showEdited());
                }

                throw new IllegalStateException();
            };
        }

        static String getRecentLocationsLabel(@NotNull Supplier<Boolean> showEdited) {
            return showEdited.get() ? "Recent Changed Locations" : "Recent Locations";
        }
        //在标题头显示的是修改的文件
        @NotNull
        private Supplier<Boolean> showEdited() {
            return () -> myShowOnlyEditedFilesCheckBox != null && myShowOnlyEditedFilesCheckBox.isSelected();
        }
        @NotNull
        private static <T> JBList<T> createList(CollectionListModel<T> baseModel,
                                                Function<? super T, String> namer,
                                                SwitcherSpeedSearch speedSearch,
                                                boolean pinned) {
            ListModel<T> listModel;
            if (pinned) {
                listModel = new NameFilteringListModel<>(baseModel, namer, s -> !speedSearch.isPopupActive()
                        || StringUtil.isEmpty(speedSearch.getEnteredPrefix())
                        || speedSearch.getComparator().matchingFragments(speedSearch.getEnteredPrefix(), s) != null, () -> StringUtil.notNullize(
                        speedSearch.getEnteredPrefix()));
            }
            else {
                listModel = baseModel;
            }
            return new JBList<>(listModel);
        }
        @Nullable
        private static String getSmartShortcut(ToolWindow window, Map<String, ToolWindow> keymap) {
            String title = window.getStripeTitle();
            if (StringUtil.isEmpty(title))
                return null;
            for (int i = 0; i < title.length(); i++) {
                char c = title.charAt(i);
                if (Character.isUpperCase(c)) {
                    String shortcut = String.valueOf(c);
                    if (keymap.get(shortcut) == null)
                        return shortcut;
                }
            }
            return null;
        }
        private static class MyCheckBox extends JBCheckBox{
            public MyCheckBox(@NotNull String actionId,boolean selected) {
                super(layoutText(actionId),selected);
                setOpaque(false);
                setFocusable(false);
            }
            private static String layoutText(@NotNull String actionId){
                ShortcutSet shortcuts = KeymapUtil.getActiveKeymapShortcuts(actionId);
                return "<html>"
                        + "Show changed only"
                        + "<font color=\""+SHORTCUT_HEX_COLOR+"\">"
                        + KeymapUtil.getShortcutsText(shortcuts.getShortcuts())+"</font>"
                        +"<html>";
            }
        }
        private class MyFilesListFocusListener extends FocusAdapter {
            //focusGained 获得焦点
            @Override
            public void focusGained(FocusEvent e) {
                exchangeSelectionState(toolWindows, files);
            }
        }
        /**
         * 处理最近打开的文件渲染工具
         */
        private static class VirtualFilesRenderer extends ColoredListCellRenderer<FileInfo>{
            private final SwitcherPanel mySwitcherPanel;
            boolean open;

            public VirtualFilesRenderer(@NotNull SwitcherPanel mySwitcherPanel) {
                this.mySwitcherPanel = mySwitcherPanel;
            }

            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends FileInfo> list,
                                                 FileInfo value, int index, boolean selected, boolean hasFoucs) {
                Project project = mySwitcherPanel.project;
                VirtualFile virtualFile = value.getFirst();
                String renderedName = value.getNameForRendering();
                setIcon(IconUtil.getIcon(virtualFile, Iconable.ICON_FLAG_READ_STATUS,project));
                FileStatus fileStatus = FileStatusManager.getInstance(project).getStatus(virtualFile);
                open = FileEditorManager.getInstance(project).isFileOpen(virtualFile);
                //文件列表中是否有错误的文件，或未编译通过的文件
                boolean hasProblem = WolfTheProblemSolver.getInstance(project).isProblemFile(virtualFile);
                //设置他们的文本属性
                TextAttributes attributes = new TextAttributes(fileStatus.getColor(),null
                        ,hasProblem?JBColor.red:null, EffectType.WAVE_UNDERSCORE,Font.PLAIN);
                //对文字进行设置文字属性
                append(renderedName,SimpleTextAttributes.fromTextAttributes(attributes));
                //获取当前文件的背景颜色
                Color color = EditorTabPresentationUtil.getFileBackgroundColor(project, virtualFile);
                if(!selected && color !=null){
                    setBackground(color);
                }
                SpeedSearchUtil.applySpeedSearchHighlighting(mySwitcherPanel,this,false,selected);
            }
        }
        private static class ToggleCheckBoxAction extends DumbAwareAction implements DumbAware{

            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                Project project = e.getProject();
                SwitcherPanel switcherPanel = SWITCHER_KEY.get(project);
                if (switcherPanel != null) {
                   // switcherPanel.toggleShowEditedFiles();
                }
            }
            @Override
            public void update(@NotNull AnActionEvent e) {
                Project project = e.getProject();
                e.getPresentation().setEnabledAndVisible(SWITCHER_KEY.get(project) != null);
            }

            static boolean isEnabled() {
               return KeymapUtil.getActiveKeymapShortcuts(TOGGLE_CHECK_BOX_ACTION_ID).getShortcuts().length > 0;
            }
        }
        @NotNull
        private static JPanel createTopPanel(@NotNull JBCheckBox showOnlyEditedFilesCheckBox,
                                             @NotNull String title,
                                             boolean isMovable) {
            JPanel topPanel = new CaptionPanel();
            JBLabel titleLabel = new JBLabel(title);
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
            topPanel.add(titleLabel, BorderLayout.WEST);
            topPanel.add(showOnlyEditedFilesCheckBox, BorderLayout.EAST);

            Dimension size = topPanel.getPreferredSize();
            size.height = JBUIScale.scale(29);
            size.width = titleLabel.getPreferredSize().width + showOnlyEditedFilesCheckBox.getPreferredSize().width + JBUIScale.scale(50);
            topPanel.setPreferredSize(size);
            topPanel.setMinimumSize(size);
            topPanel.setBorder(JBUI.Borders.empty(5, 8));

            if (isMovable) {
                WindowMoveListener moveListener = new WindowMoveListener(topPanel);
                topPanel.addMouseListener(moveListener);
                topPanel.addMouseMotionListener(moveListener);
            }

            return topPanel;
        }
        private static String getIndexShortcut(int index) {
            return StringUtil.toUpperCase(Integer.toString(index, index + 1));
        }
        @Override
        public void registerHint(@NotNull JBPopup jbPopup) {

        }

        @Override
        public void unregisterHint() {

        }

        @Nullable
        @Override
        public Object getData(@NotNull String s) {
            return null;
        }

        @Override
        public void keyTyped(KeyEvent e) {

        }

        @Override
        public void keyPressed(KeyEvent e) {

        }

        @Override
        public void keyReleased(KeyEvent e) {

        }

        @Override
        public void mouseClicked(MouseEvent e) {

        }

        @Override
        public void mousePressed(MouseEvent e) {

        }

        @Override
        public void mouseReleased(MouseEvent e) {

        }

        @Override
        public void mouseEntered(MouseEvent e) {

        }

        @Override
        public void mouseExited(MouseEvent e) {

        }

        @Override
        public void mouseDragged(MouseEvent e) {

        }
        private boolean mouseMovedFirstTime = true;
        private JList mouseMoveSrc = null;
        private int mouseMoveListIndex = -1;
        @Override
        public void mouseMoved(MouseEvent e) {

        }
        //SwitcherPanel中匿名内部类
        private static void exchangeSelectionState(JBList toClear,JBList toSelect){
            if(toSelect.getModel().getSize()>0){
                int index = Math.min(toClear.getSelectedIndex(),toSelect.getModel().getSize()-1);
                toSelect.setSelectedIndex(index);
                toSelect.ensureIndexIsVisible(index);
                toSelect.clearSelection();
            }
        }
        //SwitcherPanel中匿名内部类 MyToolWindowsListFocusLister
        private class MyToolWindowsListFocusListener extends FocusAdapter{
            @Override
            public void focusGained(FocusEvent e) {
                exchangeSelectionState(files,toolWindows);
            }

        }
    private static class SwitcherSpeedSearch extends SpeedSearchBase<SwitcherPanel> implements PropertyChangeListener{

        SwitcherSpeedSearch(@NotNull SwitcherPanel switcher){
            super(switcher);
            addChangeListener(this);
            setComparator(new SpeedSearchComparator(false,true));
        }
        @Override
        protected int getSelectedIndex() {
            return 0;
        }

        @NotNull
        @Override
        protected Object[] getAllElements() {
            return new Object[0];
        }

        @Nullable
        @Override
        protected String getElementText(Object o) {
            return null;
        }

        @Override
        protected void selectElement(Object o, String s) {

        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {

        }
    }
 }
 static class FileInfo extends Pair<VirtualFile,EditorWindow>{
     private final Project myProject;
     private String myNameForRendering;
     public FileInfo(VirtualFile first, EditorWindow second,Project project) {
         super(first, second);
         myProject = project;
     }
     String getNameForRendering(){
         if(myNameForRendering==null){
             myNameForRendering = EditorTabPresentationUtil.getUniqueEditorTabTitle(myProject,first,second);
         }
         return myNameForRendering;
     }
 }
}
