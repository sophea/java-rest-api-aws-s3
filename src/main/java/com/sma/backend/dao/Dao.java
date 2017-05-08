package com.sma.backend.dao;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.sma.backend.json.JCategory;

public interface Dao <T extends Object>{
    
    T findById(@Param("id") long id);
    
    void add(T domain);
        
    void update(T domain);
    
   //Select("select * from category")
    List<JCategory> getAll();
    
    void remove(@Param("id") long id);
    
    List<T> getPage(@Param("limit") int limit, @Param("offset") int offset);
    
    List<T> getAllWithOffset(@Param("limit") int limit, @Param("offset") int offset);
    
    Integer count();
}
