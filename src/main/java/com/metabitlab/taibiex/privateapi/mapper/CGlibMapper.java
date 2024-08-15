package com.metabitlab.taibiex.privateapi.mapper;

import org.springframework.cglib.beans.BeanCopier;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;



public class CGlibMapper {

    //使用缓存提高效率
    private static final ConcurrentHashMap<String, BeanCopier> mapCaches = new ConcurrentHashMap<>();

    public static <O, T> T mapper(O source, Class<T> target) {
        T instance = baseMapper(source, target);
        return instance;
    }

    public static <O, T> T mapperObject(O source, T target) {
        String baseKey = generateKey(source.getClass(), target.getClass());
        BeanCopier copier;
        if (!mapCaches.containsKey(baseKey)) {
            copier = BeanCopier.create(source.getClass(), target.getClass(), false);
            mapCaches.put(baseKey, copier);
        } else {
            copier = mapCaches.get(baseKey);
        }
        copier.copy(source, target, null);
        return target;
    }

    private static <O, T> T baseMapper(O source, Class<T> target) {
        if(source == null){
            return null;
        }
        String baseKey = generateKey(source.getClass(), target);
        BeanCopier copier;
        if (!mapCaches.containsKey(baseKey)) {
            copier = BeanCopier.create(source.getClass(), target, false);
            mapCaches.put(baseKey, copier);
        } else {
            copier = mapCaches.get(baseKey);
        }
        T instance = null;
        try {
            instance = target.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            System.out.println("mapper 创建对象异常" + e.getMessage());
        }
        copier.copy(source, instance, null);
        return instance;
    }

    private static String generateKey(Class<?> class1, Class<?> class2) {
        return class1.toString() + class2.toString();
    }

    public static <O, T> List<T> listMapper(List<O> source, Class<T> target){
        if(source.isEmpty()){
            return null;
        }
        List<T> list = new ArrayList<T>();
        for (O o:source) {
            list.add(baseMapper(o,target));
        }
        return list;
    }
}
