package com.thero.worktile.util;

import com.thero.worktile.model.Groups;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import java.io.*;

/**
 * 输出xml和解析xml的工具类
 *
 * @ClassName:XmlUtil
 * @author: chenyoulong  Email: chen.youlong@payeco.com
 * @date :2012-9-29 上午9:51:28
 * @Description:TODO
 */
public class XmlUtil {
    /**
     *  将传入xml文本转换成Java对象
     *  调用的方法实例：PersonBean person=XmlUtil.toBean(xmlStr, PersonBean.class);
     * @param xmlStr 传入的xml字符串
     * @param cls  xml对应的class类
     * @return T   xml对应的class类的实例对象
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public static <T> T  toBean(String xmlStr,Class<T> cls) throws InstantiationException, IllegalAccessException{
        //注意：不是new Xstream(); 否则报错：java.lang.NoClassDefFoundError: org/xmlpull/v1/XmlPullParserFactory
        XStream xstream=new XStream(new DomDriver("utf-8"));
        xstream.processAnnotations(cls);
        T obj = cls.newInstance();
        xstream.fromXML(xmlStr, new Groups());
        return obj;
    }

    /**
     *  将传入xml文本转换成Java对象
     *  调用的方法实例：PersonBean person=XmlUtil.toBean(xmlStr, PersonBean.class);
     * @param xmlStr 传入的xml字符串
     * @param cls  xml对应的class类
     * @return T   xml对应的class类的实例对象
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public static <T> T  toBean(String xmlStr,Class<T> cls,String charset) throws InstantiationException, IllegalAccessException{
        //注意：不是new Xstream(); 否则报错：java.lang.NoClassDefFoundError: org/xmlpull/v1/XmlPullParserFactory
        XStream xstream=new XStream(new DomDriver(charset));
        xstream.processAnnotations(cls);
        T obj = cls.newInstance();
        xstream.fromXML(xmlStr, obj);
        return obj;
    }

}