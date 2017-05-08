package com.sma.backend.json;

import java.io.Serializable;

public class JCategory implements Serializable {

    private static final long serialVersionUID = 1L;


    /** Unique id for this Entity in the database */
    private Long id;
    
    
    /** Possible states are DELETED (-1), PENDING (0), ACTIVE (1) and REDEEMED (2) */
    private Long state;
    
    /** a one-liner category name (default language) */
    private String              name;

    /** A longer description of this category (default language) */
    private String              description;

    /** key to the category asset generic field for each app specific*/
    private String              appArg0;


    /** If this is a sub-category, the id of its parent */
    private Long                parentId;
    
    /** logo Url */
    private String              logoUrl;
    
    /** type of Category, e.g. DBrand, DOffer , DStore */
    private String type;
    
    private Long levelOrder;

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAppArg0() {
        return appArg0;
    }

    public void setAppArg0(String appArg0) {
        this.appArg0 = appArg0;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public Long getLevelOrder() {
        return levelOrder;
    }

    public void setLevelOrder(Long levelOrder) {
        this.levelOrder = levelOrder;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getState() {
        return state;
    }

    public void setState(Long state) {
        this.state = state;
    }

   
}
