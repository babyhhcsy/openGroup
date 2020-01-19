package com.thero.worktile.actions;

import com.intellij.ide.DataManager;
import com.intellij.ide.IdeEventQueue;
import com.intellij.ide.ui.UISettings;
import com.intellij.ide.util.gotoByName.QuickSearchComponent;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.impl.EditorHistoryManager;
import com.intellij.openapi.fileEditor.impl.EditorTabPresentationUtil;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.problems.WolfTheProblemSolver;
import com.intellij.ui.*;
import com.intellij.ui.border.CustomLineBorder;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.ui.popup.PopupUpdateProcessorBase;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.ui.speedSearch.NameFilteringListModel;
import com.intellij.ui.speedSearch.SpeedSearchUtil;
import com.intellij.util.*;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.StatusText;
import com.intellij.util.ui.UIUtil;
import com.thero.worktile.model.GroupModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.security.acl.Group;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class OpenGrouper extends AnAction implements DumbAware {

    private static final int MINIMUM_HEIGHT = JBUIScale.scale(400);
    private static final int MINIMUM_WIDTH = JBUIScale.scale(500);
    private static final Color ON_MOUSE_OVER_BG_COLOR = new JBColor(new Color(231, 242, 249), new Color(77, 80, 84));
    private static final Color SEPARATOR_COLOR = JBColor.namedColor("Popup.separatorColor", new JBColor(Gray.xC0, Gray.x4B));
    private static int CTRL_KEY;
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if(project==null){
            return ;
        }
       // new OpenGroupPanel(project,"分组打开","open-group");
    }
    @Nullable
    public static OpenGroupPanel createAndShowOpenGrouper(@NotNull Project project,@NotNull String title,
                                                           @NotNull String actionId){
        synchronized (OpenGroupPanel.class){
            return new OpenGroupPanel(project,title,actionId);
        }
    }
    public static class OpenGroupPanel extends JPanel implements KeyListener, MouseMotionListener,MouseListener, QuickSearchComponent, DataProvider{

        final JBPopup myPopup;
        final JBList<Object> JBGroupList;
        final JBList<FileInfo> files;
        final JPanel myTopPanel;
        final OpenGroupSpeedSearch mySpeedSearch;
        final JLabel descriptionsLabel = new JLabel(" ");
        final JPanel descripationsPanel ;
        final String myTitle;
        final Alarm myAlarm;
        final Project project;
        private JBPopup myHint;
        final ClickListener myClickListener = new ClickListener() {
            @Override
            public boolean onClick(@NotNull MouseEvent event, int clickCount) {
                if(event.isControlDown() || event.isMetaDown() || event.isShiftDown())return false;
                Object source = event.getSource();
                if(source instanceof JList){
                    JList jList = (JList) source;
                    if(jList.getSelectedIndex() == -1 && jList.getAnchorSelectionIndex() !=-1){
                        jList.setSelectedIndex(jList.getAnchorSelectionIndex());
                    }
                    if(jList.getSelectedIndex()!=-1){
                        //TODO: navigate 方法
                    }
                }
                return true;
            }
        };
        OpenGroupPanel(@NotNull final Project project, @NotNull String title, @NotNull String actionId){
            setLayout(new BorderLayout());
            myTitle = title;
            mySpeedSearch = new OpenGroupSpeedSearch(this);
            this.project = project;
            setBorder(JBUI.Borders.empty());
            setBackground(JBColor.background());
            final Font font = descriptionsLabel.getFont();
            descriptionsLabel.setHorizontalAlignment(SwingConstants.LEFT);
            descriptionsLabel.setFont(font.deriveFont(Math.max(10f,font.getSize()-4f)));
            descriptionsLabel.setBorder(JBUI.CurrentTheme.Advertiser.border());
            descriptionsLabel.setForeground(JBUI.CurrentTheme.Advertiser.foreground());
            descriptionsLabel.setBackground(JBUI.CurrentTheme.Advertiser.background());
            descriptionsLabel.setOpaque(true);

            descripationsPanel = new JPanel(new BorderLayout());
            descripationsPanel.setBorder(new CustomLineBorder(JBUI.CurrentTheme.Advertiser.borderColor(),JBUI.insetsTop(1)));;
            descripationsPanel.add(descriptionsLabel,BorderLayout.CENTER);

            CollectionListModel<Object> groupModelListModel = new CollectionListModel<Object>();
            java.util.List<GroupModel> groupModelList = new ArrayList<GroupModel>();
            groupModelList.add(new GroupModel("banOff","封禁管理"));
            groupModelList.add(new GroupModel("gameUser","游戏用户"));
            groupModelList.add(new GroupModel("activityManager","活动管理"));
            groupModelList.sort((GroupModel g1, GroupModel g2) ->
                StringUtil.compare(g1.getGroupName(), g2.getGroupName(), false)
            );
            for(GroupModel groupModel : groupModelList){
                groupModelListModel.add(groupModel);
            }
            JBGroupList = createList(groupModelListModel,getNamer(), mySpeedSearch);
            JBGroupList.addFocusListener(new MyToolWindowsListFocusListener());
            JBGroupList.setBorder(JBUI.Borders.empty(5,5,5,20));
            JBGroupList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            JBGroupList.addKeyListener(this);
            ScrollingUtil.installActions(JBGroupList);
            JBGroupList.addMouseListener(this);
            JBGroupList.addMouseMotionListener(this);
            JBGroupList.setCellRenderer(new OpenGroupListRender(){
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean hasFocus) {
                    Component render = super.getListCellRendererComponent(list, value, index, selected, hasFocus);
                    final Color bgColor = list == mouseMoveSrc && index == mouseMoveListIndex ? ON_MOUSE_OVER_BG_COLOR : list.getBackground();
                    setBackground(ON_MOUSE_OVER_BG_COLOR);
                    return render;
                }
            });

            ScrollingUtil.ensureSelectionExists(JBGroupList);
            ListSelectionListener listSelectionListener = new ListSelectionListener() {
                @Nullable
                private String getTitle2Text(@Nullable String fullText) {
                    int labelWidth = descriptionsLabel.getWidth();
                    if (fullText == null || fullText.length() == 0) return " ";
                    //如果文件的路径超过宽度，那么从第4个/开始截取，中间的用省略号来代替
                    while (descriptionsLabel.getFontMetrics(descriptionsLabel.getFont()).stringWidth(fullText) > labelWidth) {
                        int sep = fullText.indexOf(File.separatorChar, 4);
                        if (sep < 0) return fullText;
                        fullText = "..." + fullText.substring(sep);
                    }
                    return fullText;
                }

                @Override
                public void valueChanged(ListSelectionEvent e) {
                    if (e.getValueIsAdjusting()) return;
                    updateDescLabel();
                    PopupUpdateProcessorBase popupUpdater = myHint==null||!myHint.isVisible()?null:myHint.getUserData(PopupUpdateProcessorBase.class);
                    if(popupUpdater!=null){
                        popupUpdater.updatePopup(CommonDataKeys.PSI_ELEMENT.getData(
                                DataManager.getInstance().getDataContext(OpenGroupPanel.this)
                        ));
                    }
                }

                private void updateDescLabel() {
                    List<Object> valuesList = JBGroupList.getSelectedValuesList();
                    if (valuesList != null && valuesList.size() == 1) {
                        GroupModel groupModel = (GroupModel) valuesList.get(0);
                        descriptionsLabel.setText(getTitle2Text(groupModel.getGroupDesc()));
                    } else {
                        descriptionsLabel.setText(" ");
                    }
                }
            };
            JBGroupList.addListSelectionListener(listSelectionListener);


            myTopPanel = createTopPanel("OpenGroup");







           //处理最近打开的文件
            Pair<List<FileInfo>, Integer> filesAndSelection = getFilesToShowAndSelectionIndex(project, collectFiles(project));
            final int selectionIndex = filesAndSelection.getSecond();
            final CollectionListModel<FileInfo> filesModel = new CollectionListModel<FileInfo>();
            for(FileInfo editor:filesAndSelection.getFirst()){
                filesModel.add(editor);
            }
            final VirtualFilesRender filesRenderer = new VirtualFilesRender(this) {
                final JPanel mypanel = new NonOpaquePanel(new BorderLayout());

                {
                    mypanel.setBackground(UIUtil.getListBackground());
                }

                @Override
                public Component getListCellRendererComponent(JList<? extends FileInfo> list, FileInfo value, int index, boolean selected, boolean hasFocus) {
                    Component c = super.getListCellRendererComponent(list, value, index, selected, hasFocus);
                    mypanel.removeAll();
                    ;
                    mypanel.add(c, BorderLayout.CENTER);
                    mypanel.getAccessibleContext().setAccessibleName(c.getAccessibleContext().getAccessibleName());
                    VirtualFile file = value.first;
                    String presentableUrl = ObjectUtils.notNull(file.getParent(), file).getPresentableUrl();
                    String location = FileUtil.getLocationRelativeToUserHome(presentableUrl);
                    mypanel.getAccessibleContext().setAccessibleDescription(location);
                    if (!selected && list == mouseMoveSrc && index == mouseMoveListIndex) {
                        setBackground(ON_MOUSE_OVER_BG_COLOR);
                    }
                    return mypanel;
                }

                @Override
                protected void customizeCellRenderer(@NotNull JList<? extends FileInfo> list, FileInfo value, int index, boolean selected, boolean hasFocus) {
                    setPaintFocusBorder(false);
                    super.customizeCellRenderer(list, value, index, selected, hasFocus);
                }

                //列表变更时监听工具
                final ListSelectionListener listSelectionListener = new ListSelectionListener() {
                    @Nullable
                    private String getTitle2Text(@Nullable String fullText) {
                        int labelWidth = descriptionsLabel.getWidth();
                        if (fullText == null || fullText.length() == 0) return " ";
                        //如果文件的路径超过宽度，那么从第4个/开始截取，中间的用省略号来代替
                        while (descriptionsLabel.getFontMetrics(descriptionsLabel.getFont()).stringWidth(fullText) > labelWidth) {
                            int sep = fullText.indexOf(File.separatorChar, 4);
                            if (sep < 0) return fullText;
                            fullText = "..." + fullText.substring(sep);
                        }
                        return fullText;
                    }

                    @Override
                    public void valueChanged(@NotNull final ListSelectionEvent e) {
                        if (e.getValueIsAdjusting()) return;
                        updateDescLabel();
                        PopupUpdateProcessorBase popupUpdater = myHint == null || !myHint.isVisible() ? null : myHint.getUserData(PopupUpdateProcessorBase.class);
                        if (popupUpdater != null) {
                            popupUpdater.updatePopup(CommonDataKeys.PSI_ELEMENT.getData(
                                    DataManager.getInstance().getDataContext(OpenGroupPanel.this)
                            ));
                        }
                    }

                    private void updateDescLabel() {
                        List<FileInfo> values = files.getSelectedValuesList();
                        if (values != null && values.size() == 1) {
                            VirtualFile file = values.get(0).first;
                            String presentableUrl = ObjectUtils.notNull(file.getParent(), file).getPresentableUrl();
                            descriptionsLabel.setText(getTitle2Text(FileUtil.getLocationRelativeToUserHome(presentableUrl)));
                        } else {
                            descriptionsLabel.setText(" ");
                        }
                    }
                };
            };
            files = createList(filesModel,FileInfo::getMyNameForRendering,mySpeedSearch);
            files.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
            files.getSelectionModel().addListSelectionListener(e -> {
                if(!files.isSelectionEmpty()){
                    JBGroupList.getSelectionModel().clearSelection();
                }
            });
            files.getSelectionModel().addListSelectionListener(listSelectionListener);
            files.setCellRenderer(filesRenderer);
            files.setBorder(JBUI.Borders.empty(5));
            files.addKeyListener(this);
            ScrollingUtil.installActions(files);
            files.addMouseMotionListener(this);
            files.addMouseMotionListener(this);
            files.addFocusListener(new MyFilesListFocusListener());
            myClickListener.installOn(files);
            ScrollingUtil.ensureSelectionExists(files);

            this.add(myTopPanel,BorderLayout.NORTH);
            this.add(JBGroupList,BorderLayout.WEST);
            //向面板添加最近打开的文件
            if(filesModel.getSize() > 0){
                files.setAlignmentY(1f);
                final JScrollPane pane = ScrollPaneFactory.createScrollPane(files,true);
                pane.setPreferredSize(new Dimension(Math.max(myTopPanel.getPreferredSize().width - JBGroupList.getPreferredSize().width,
                        files.getPreferredSize().width),
                        20 * 20));
                Border border = JBUI.Borders.merge(
                        JBUI.Borders.emptyLeft(9),
                        new CustomLineBorder(SEPARATOR_COLOR, JBUI.insetsLeft(1)),
                        true
                );
                pane.setBorder(border);
                this.add(pane, BorderLayout.CENTER);
                if (selectionIndex > -1) {
                    files.setSelectedIndex(selectionIndex);
                }
            }

            //激活搜索的功能
            this.add(descripationsPanel,BorderLayout.SOUTH);
            final ShortcutSet shortcutSet = ActionManager.getInstance().getAction("ActivateworktileToolWindow").getShortcutSet();
            final int modifiers = getModifiers(shortcutSet);
            final boolean isAlt = (modifiers & Event.ALT_MASK)!=0;
            CTRL_KEY = isAlt ? KeyEvent.VK_ALT : KeyEvent.VK_CONTROL;
            KeymapUtil.reassignAction(files, KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.CTRL_DOWN_MASK), WHEN_FOCUSED, false);
            KeymapUtil.reassignAction(files, KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.CTRL_DOWN_MASK), WHEN_FOCUSED, false);
            files.addKeyListener(ArrayUtil.getLastElement(getKeyListeners()));
            JBGroupList.addKeyListener(ArrayUtil.getLastElement(getKeyListeners()));


            myPopup = JBPopupFactory.getInstance().createComponentPopupBuilder(this,JBGroupList)
                    .setResizable(true).setModalContext(false).setFocusable(true).setRequestFocus(true)
                    .setCancelOnWindowDeactivation(true).setCancelOnOtherWindowOpen(true).setMovable(true)
                    .setMinSize(new Dimension(MINIMUM_WIDTH,MINIMUM_HEIGHT)).createPopup();

            myAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD,myPopup);
            IdeEventQueue.getInstance().getPopupManager().closeAllPopups(false);
            Window window = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
            if (window == null) {
                window = WindowManager.getInstance().getFrame(project);
            }
            myPopup.showInCenterOf(window);
            fromListToList(JBGroupList,files);
            fromListToList(files,JBGroupList);
        }
        private static int getModifiers(@Nullable ShortcutSet shortcutSet){
            if(shortcutSet == null || shortcutSet.getShortcuts().length == 0
            || !(shortcutSet.getShortcuts()[0] instanceof KeyboardShortcut)){
                return Event.CTRL_MASK;
            }
            return ((KeyboardShortcut)shortcutSet.getShortcuts()[0]).getFirstKeyStroke().getModifiers();
        }
        private class MyFilesListFocusListener extends FocusAdapter{
            @Override
            public void focusGained(FocusEvent e) {
                exchangeSelectionState(JBGroupList,files);
            }
        }
        @NotNull
        static List<VirtualFile> collectFiles(@NotNull Project project) {
            return getRecentFiles(project);
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
        static Pair<List<FileInfo>,Integer> getFilesToShowAndSelectionIndex(@NotNull Project project,
                                                                            @NotNull List<VirtualFile> filesForInit){
            int selectionIndex = -1;
            FileEditorManagerImpl editorManager = (FileEditorManagerImpl) FileEditorManager.getInstance(project);
            final ArrayList<FileInfo> filesData = new ArrayList<>();
            final ArrayList<FileInfo> editors = new ArrayList<>();
            for (Pair<VirtualFile, EditorWindow> pair : editorManager.getSelectionHistory()) {
                editors.add(new FileInfo(pair.first, pair.second, project));
            }
            if(editors.size()<2){
                if(editors.size()>1){
                    filesData.addAll(editors);
                }
                int maxFiles = Math.max(editors.size(), filesForInit.size());
                int minIndex = filesForInit.size() - maxFiles;
                boolean firstRecentMarked = false;
                List<VirtualFile> selectedFiles = Arrays.asList(editorManager.getSelectedFiles());
                EditorWindow currentWindow = editorManager.getCurrentWindow();
                VirtualFile currentFile = currentWindow!=null?currentWindow.getSelectedFile():null;
                for(int i = filesForInit.size() -1 ;i >= minIndex ; i--){
                    if(UISettings.getInstance().getEditorTabPlacement()!=UISettings.TABS_NONE && selectedFiles.contains(filesForInit.get(i))){
                        continue;
                    }
                    FileInfo info = new FileInfo(filesForInit.get(i),null,project);
                    boolean add = true;
                    for(FileInfo fileInfo:filesData){
                        if(fileInfo.first.equals(info.first)){
                            add = false;
                            break;
                        }
                    }
                    if(add){
                        filesData.add(info);
                        if(!firstRecentMarked && !info.first.equals(currentFile)){
                            selectionIndex=filesData.size() - 1;
                            firstRecentMarked = true;
                        }
                    }
                }
                if(editors.size()==1 && (filesData.isEmpty() || !editors.get(0).getFirst().equals(filesData.get(0).getFirst()))){
                    filesData.add(0,editors.get(0));
                }
            }else{
                for(int i = 0 ;i < Math.min(30,editors.size());i++){
                    filesData.add(editors.get(i));
                }
            }
            return Pair.create(filesData,selectionIndex);
        }
        private static void fromListToList(JBList from,JBList to){
            AbstractAction action = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    to.requestFocus();
                }
            };
            ActionMap map = from.getActionMap();
            map.put(ListActions.Left.ID,action);
            map.put(ListActions.Right.ID,action);
        }
        @NotNull
        private static <T> JBList<T> createList(CollectionListModel<T> baseModel,Function<? super T,String> namer,
                                                OpenGroupSpeedSearch speedSearch){
            ListModel<T> listModel = new NameFilteringListModel<T>(baseModel,namer, s->!speedSearch.isPopupActive()
                ||StringUtil.isEmpty(speedSearch.getEnteredPrefix())
                    || speedSearch.getComparator().matchingFragments(speedSearch.getEnteredPrefix(),s)!=null ,()->StringUtil.notNullize(speedSearch.getEnteredPrefix()));
            return new JBList<>(listModel);
        }
        public Function<? super Object,String> getNamer(){
            return value -> {
                if(value instanceof GroupModel){
                    return ((GroupModel) value).getGroupName();
                }
                throw new IllegalStateException();
            };
        }
        @Override
        public void registerHint(@NotNull JBPopup h) {
            if(myHint !=null && myHint.isVisible() && myHint!=h){
                myHint.cancel();
            }
            myHint = h;
        }

        @Override
        public void unregisterHint() {
            myHint = null;
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
        public void mouseDragged(MouseEvent e) {

        }
        private boolean mouseMovedFirstTime = true;
        private JList mouseMoveSrc = null;
        private int mouseMoveListIndex = -1;
        /**
         * 处理鼠标移动时，划过的列表显示背景色
         * */
        @Override
        public void mouseMoved(MouseEvent e) {
            if(mouseMovedFirstTime){
                mouseMovedFirstTime = false;
                return ;
            }
            final Object source = e.getSource();
            boolean changed = false;
            if(source instanceof JList){
                JList list = (JList) source;
                int index = list.locationToIndex(e.getPoint());
                if(0 <=  index && index < list.getModel().getSize()){
                    mouseMoveSrc = list;
                    mouseMoveListIndex = index;
                    changed = true;
                }
            }
            if(!changed){
                mouseMoveSrc = null;
                mouseMoveListIndex = -1;
            }
            JBGroupList.repaint();
            files.repaint();
        }

        @Override
        public void mouseClicked(MouseEvent e) {

        }

        @Override
        public void mousePressed(MouseEvent e) {

        }

        @Override
        public void mouseReleased(MouseEvent e) {
           final Object source =  e.getSource();
           if(source instanceof JList){
               JList list = (JList) source;
               int index = list.locationToIndex(e.getPoint());
               Object item = list.getModel().getElementAt(index);
               System.out.println(item.getClass());
               myPopup.cancel();
               final FileEditorManagerImpl manager = (FileEditorManagerImpl)FileEditorManager.getInstance(project);
               ListModel<FileInfo> fileInfos = files.getModel();
               for(int i = 0 ;i < fileInfos.getSize();i++){
                   FileInfo fileInfo = fileInfos.getElementAt(i);
                   manager.openFile(fileInfo.first,true,true);
               }

           }
        }
        @Nullable
        private static EditorWindow findAppropriateWindow(@NotNull FileInfo info){
            if(info.second==null)return null;
            final EditorWindow[] windows = info.second.getOwner().getWindows();
            return ArrayUtil.contains(info.second,windows)?info.second:windows.length>0?windows[0]:null;
        }
        @Override
        public void mouseEntered(MouseEvent e) {

        }
        /**
         * 鼠标移开时，移除所有的背景色
         * */
        @Override
        public void mouseExited(MouseEvent e) {
            mouseMoveSrc = null;
            mouseMoveListIndex = -1;
            JBGroupList.repaint();
            files.repaint();
        }

        private class MyToolWindowsListFocusListener extends FocusAdapter{
            @Override
            public void focusGained(FocusEvent e) {
                exchangeSelectionState(JBGroupList,files);
            }

        }
        private static void exchangeSelectionState(JBList toclear,JBList toSelect){
            if(toSelect.getModel().getSize()>0){
                int index = Math.min(toclear.getSelectedIndex(),toSelect.getModel().getSize()-1);
                toSelect.setSelectedIndex(index);
                toSelect.ensureIndexIsVisible(index);
                toSelect.clearSelection();
            }
        }
        private static JPanel createTopPanel(@NotNull String title){
            JPanel topPanel = new CaptionPanel();
            JBLabel titleLabel = new JBLabel(title);
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
            topPanel.add(titleLabel,BorderLayout.WEST);
            Dimension size = topPanel.getPreferredSize();
            size.height = JBUIScale.scale(29);
            size.width = titleLabel.getPreferredSize().width + JBUIScale.scale(50);
            topPanel.setPreferredSize(size);
            topPanel.setMinimumSize(size);
            topPanel.setBorder(JBUI.Borders.empty(5,8));
            WindowMoveListener moveListener = new WindowMoveListener(topPanel);
            topPanel.addMouseListener(moveListener);
            topPanel.addMouseMotionListener(moveListener);
            return topPanel;
        }
        private static class VirtualFilesRender extends ColoredListCellRenderer<FileInfo>{
            private final OpenGroupPanel openGroupPanel;
            boolean open;

            public VirtualFilesRender(OpenGroupPanel openGroupPanel) {
                this.openGroupPanel = openGroupPanel;
            }

            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends FileInfo> list,
                                                 FileInfo value, int index, boolean selected, boolean hasFocus) {
                Project project = openGroupPanel.project;
                VirtualFile virtualFile = value.getFirst();
                String renderedName = value.getMyNameForRendering();
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
                SpeedSearchUtil.applySpeedSearchHighlighting(openGroupPanel,this,false,selected);
            }

        }
        private boolean isFilesSelected(){
            return getSelectedList(files) ==files ;
        }
        @Nullable
        public JBList<?> getSelectedList(@Nullable JBList preferable){
            return files.hasFocus() ? files:JBGroupList.hasFocus()?JBGroupList:preferable;
        }

    }

    private static class OpenGroupSpeedSearch extends SpeedSearchBase<OpenGroupPanel> implements PropertyChangeListener{
        public OpenGroupSpeedSearch(OpenGroupPanel component) {
            super(component);
            addChangeListener(this);
            setComparator(new SpeedSearchComparator(false,true));
        }

        @Override
        protected int getSelectedIndex() {
            return myComponent.isFilesSelected()
                    ?myComponent.files.getSelectedIndex():myComponent.files.getModel().getSize() + myComponent.JBGroupList.getSelectedIndex();
        }

        @Override
        protected void processKeyEvent(KeyEvent e) {
            int keyCode = e.getKeyCode();
            if(keyCode==KeyEvent.VK_ENTER){
                //TODO: 处理回车键按下时，进行文件跳转
                e.consume();
                return ;
            }
            if(keyCode==KeyEvent.VK_LEFT || keyCode==KeyEvent.VK_RIGHT){
                return ;
            }
            super.processKeyEvent(e);
        }


        @NotNull
        @Override
        protected Object[] getAllElements() {
            ListModel<FileInfo> filesModel = myComponent.files.getModel();
            Object[] files = new Object[filesModel.getSize()];
            for(int i = 0 ;i <files.length;i++){
                files[i] = filesModel.getElementAt(i);
            }
            ListModel<Object> groupModel = myComponent.JBGroupList.getModel();
            Object[] groupModelClone = new Object[groupModel.getSize()];
            for(int i = 0;i < groupModelClone.length;i++){
                groupModelClone[i] = groupModel.getElementAt(i);
            }
            Object[] elements = new Object[files.length+groupModelClone.length];
            System.arraycopy(files,0,elements,0,files.length);
            System.arraycopy(groupModelClone,0,elements,files.length,groupModelClone.length);
            return elements;
        }

        /**
         * 处理search选择框，当输入内容时，列表中若没有该字符，显示红色
         * @param element
         * @return
         */
        @Nullable
        @Override
        protected String getElementText(Object element) {
            if(element instanceof GroupModel){
                return ((GroupModel)element).getGroupName();
            }else if(element instanceof FileInfo){
                ((FileInfo)element).getMyNameForRendering();
            }
            return "";
        }
        /**
         * 搜索功能，输入字符时触发
         * */
        @Override
        protected void selectElement(Object element, String selectedText) {
            if(element instanceof FileInfo){
                if(!myComponent.JBGroupList.isSelectionEmpty()){
                    myComponent.JBGroupList.clearSelection();
                }
                myComponent.files.clearSelection();
                myComponent.files.setSelectedValue(element,true);
                myComponent.files.requestFocusInWindow();
            }else{
                if(!myComponent.files.isSelectionEmpty()){
                    myComponent.files.clearSelection();
                }
                myComponent.JBGroupList.clearSelection();
                myComponent.JBGroupList.setSelectedValue(element,true);
                myComponent.JBGroupList.requestFocusInWindow();
            }
        }
        /**
         * 过滤查询，触发文字输入时，刷新列表
         * */
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if(myComponent.project.isDisposed()){
                myComponent.myPopup.cancel();
                return;
            }
            ((NameFilteringListModel)myComponent.files.getModel()).refilter();
            ((NameFilteringListModel)myComponent.JBGroupList.getModel()).refilter();
            if(myComponent.files.getModel().getSize() + myComponent.JBGroupList.getModel().getSize()==0){
                myComponent.JBGroupList.getEmptyText().setText(" ");
                myComponent.files.getEmptyText().setText("Press 'Enter' to search in Project");
            }else{
                myComponent.files.getEmptyText().setText(StatusText.DEFAULT_EMPTY_TEXT);
                myComponent.JBGroupList.getEmptyText().setText(StatusText.DEFAULT_EMPTY_TEXT);
            }
            refreshSelection();
        }

        @Nullable
        @Override
        protected Object findElement(String s) {
            final List<SpeedSearchObjectWithWeight> elements = SpeedSearchObjectWithWeight.findElement(s, this);
            return elements.isEmpty() ? null : elements.get(0).node;
        }
    }
    static class FileInfo extends Pair<VirtualFile,EditorWindow>{
        private final Project myProject;
        private String myNameForRendering;
        /**
         * @param first
         * @param second
         * @see #create(Object, Object)
         */
        public FileInfo(VirtualFile first, EditorWindow second,Project project) {
            super(first, second);
            myProject = project;
        }
        String getMyNameForRendering(){
            if(myNameForRendering==null){
                myNameForRendering = EditorTabPresentationUtil.getUniqueEditorTabTitle(myProject, first, second);
            }
            return myNameForRendering;
        }
    }
}
