package com.thero.worktile.model;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

import java.util.List;
@XStreamAlias("openGroup")
public class Groups {
    @XStreamAlias("groupName")
    private String groupName;
    @XStreamAlias("groupDesc")
    private String groupDesc;
    @XStreamImplicit(itemFieldName="files")
    private List<GroupFiles> groupFilesList;

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

    public List<GroupFiles> getGroupFilesList() {
        return groupFilesList;
    }

    public void setGroupFilesList(List<GroupFiles> groupFilesList) {
        this.groupFilesList = groupFilesList;
    }
}
