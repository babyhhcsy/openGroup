package com.thero.worktile.model;

import com.intellij.openapi.util.io.win32.FileInfo;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.TagValue;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.util.List;

public class GroupModel  {
    private String groupName;
    private String groupDesc;

    private List<GroupFiles> groupFiles;

    public GroupModel() {
    }

    public GroupModel(String groupName, String groupDesc) {
        this.groupName = groupName;
        this.groupDesc = groupDesc;
    }
    @TagValue
    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
    @TagValue
    public String getGroupDesc() {
        return groupDesc;
    }

    public void setGroupDesc(String groupDesc) {
        this.groupDesc = groupDesc;
    }

    public List<GroupFiles> getGroupFiles() {
        return groupFiles;
    }

    public void setGroupFiles(List<GroupFiles> groupFiles) {
        this.groupFiles = groupFiles;
    }
}
