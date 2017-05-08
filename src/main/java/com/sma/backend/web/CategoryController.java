package com.sma.backend.web;

import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;

import com.amazonaws.services.s3.model.S3Object;
import com.sma.backend.json.JCategory;
import com.sma.backend.service.AmazonS3Manager;

/**
 * @author sophea <a href='mailto:sopheamak@gmail.com> sophea </a>
 * @version $id$ - $Revision$
 * @date 2012
 */
@Controller
@RequestMapping(value = {"category", "categories" })
public class CategoryController {

    private static final Logger LOG = LoggerFactory.getLogger(CategoryController.class);

    @Autowired
    private com.sma.backend.service.CategoryService service;
    
    @Autowired
    private AmazonS3Manager amazonS3Client;

    /**
     * return all Categories support Header If-Modified-Since is optional, timestamp of last update; use
     * "Sat, 29 Oct 1994 19:43:31 GMT"
     * 
     * @return Iterable<JCategory>
     */
    @RequestMapping(value = "v1/all", method = RequestMethod.GET)
    @ResponseBody
    public Collection<JCategory> getAll(HttpServletRequest request, WebRequest webRequest,
            @RequestHeader(required = false, value = "If-Modified-Since") Date since) {

        LOG.debug(" ============== If-Modified-Since {} ", since);

        return service.getAll();
    }

    @RequestMapping(value = "v1/{id}", method = RequestMethod.GET)
    @ResponseBody
    public JCategory getDetails(HttpServletRequest request, @PathVariable Long id) {

        return service.getDetails(id);
    }

    @RequestMapping(value = "v1", method = RequestMethod.POST)
    @ResponseBody
    public JCategory create(HttpServletRequest request, @ModelAttribute JCategory domain) {

        service.create(domain);
        return domain;
    }
    
}
