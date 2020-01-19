package com.thero.worktile.model;

import com.thero.worktile.util.ReflectUtil;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ColumnBase {
    private Map<String, Object> params = new HashMap<>();

    public Field[] getFields(){
        return ReflectUtil.getAllFields(this);
    }


    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public String[] columnNames() {
        Field[] fields = getFields();
        List<String> columns = new ArrayList<>(fields.length);
        for (Field field : fields) {
            columns.add(field.getName());
        }
        String[] strings = new String[columns.size()];
        return columns.toArray(strings);
    }
}
