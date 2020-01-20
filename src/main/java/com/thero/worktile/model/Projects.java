package com.thero.worktile.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Projects extends ColumnBase implements Serializable {
    /**
     * 自定义参数
     */

    private String pid;
    private String name;
    private String team_id;
    private String desc;
    private Integer archived;
    private String pic;
    private String bg;
    private Integer visibility;
    private Integer is_star;
    private String pos;
    private Double member_count;
    private Integer curr_role;
    private Integer permission;



    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTeam_id() {
        return team_id;
    }

    public void setTeam_id(String team_id) {
        this.team_id = team_id;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public Integer getArchived() {
        return archived;
    }

    public void setArchived(Integer archived) {
        this.archived = archived;
    }

    public String getPic() {
        return pic;
    }

    public void setPic(String pic) {
        this.pic = pic;
    }

    public String getBg() {
        return bg;
    }

    public void setBg(String bg) {
        this.bg = bg;
    }

    public Integer getVisibility() {
        return visibility;
    }

    public void setVisibility(Integer visibility) {
        this.visibility = visibility;
    }

    public Integer getIs_star() {
        return is_star;
    }

    public void setIs_star(Integer is_star) {
        this.is_star = is_star;
    }

    public String getPos() {
        return pos;
    }

    public void setPos(String pos) {
        this.pos = pos;
    }

    public Double getMember_count() {
        return member_count;
    }

    public void setMember_count(Double member_count) {
        this.member_count = member_count;
    }

    public Integer getCurr_role() {
        return curr_role;
    }

    public void setCurr_role(Integer curr_role) {
        this.curr_role = curr_role;
    }

    public Integer getPermission() {
        return permission;
    }

    public void setPermission(Integer permission) {
        this.permission = permission;
    }
}
