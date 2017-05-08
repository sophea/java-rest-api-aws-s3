package com.sma.backend.service;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sma.backend.dao.CategoryDao;
import com.sma.backend.json.JCategory;

@Service
public class CategoryService {
    @Autowired
    private CategoryDao dao;
    
    
    public Collection<JCategory> getAll() {
        Collection<JCategory> dList= dao.getAll();
        
        return dList;
    }
    public JCategory getDetails(long id) {
        return dao.findById(id);
    }
    public void create(JCategory jCategory) {
        dao.add(jCategory);
    }
    
    public void update(Long id, JCategory jCategory) {
        
        dao.update(jCategory);
    }
}
