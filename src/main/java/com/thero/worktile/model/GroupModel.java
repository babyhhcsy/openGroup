package com.thero.worktile.model;

import com.intellij.openapi.util.io.win32.FileInfo;

import java.util.List;

public class GroupModel {
    private String groupName;
    private String groupDesc;

    private List<FileInfo> files;

    public GroupModel() {
    }

    public GroupModel(String groupName, String groupDesc) {
        this.groupName = groupName;
        this.groupDesc = groupDesc;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getGroupDesc() {
        return groupDesc;
    }

    public void setGroupDesc(String groupDesc) {
        this.groupDesc = groupDesc;
    }

    public List<FileInfo> getFiles() {
        return files;
    }

    public void setFiles(List<FileInfo> files) {
        this.files = files;
    }
}
