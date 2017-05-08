package com.sma.backend.web;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.sma.backend.service.AmazonS3Manager;

/**
 * Display application status such as the version number of the application.
 * 
 * @author sm
 */
@Controller
@RequestMapping("upload")
public class UploadControllerController {
    private static final Logger LOGGER = LoggerFactory.getLogger(UploadControllerController.class);

    @Autowired
    private AmazonS3Manager amazonS3Client;
    
    /**
     * Upload profile picture to S3</br>
     *
     * @param file
     * @return
     * @throws IOException
     */
    
    @RequestMapping(value = "v1/photo", method = RequestMethod.POST)
    public ResponseEntity<Map<String,String>> uploadImage(HttpServletRequest request, @RequestParam("file") MultipartFile file) throws IOException {
        
        final Map<String, String> url = new HashMap<>();
        url.put("url", writeFile(request, file));
        return new ResponseEntity<>(url, HttpStatus.OK);
    }

    
    private String writeFile(HttpServletRequest request, MultipartFile file) throws IOException {
        String fileName = null;
        if (!file.isEmpty()) {

            // folder uglyforms
            String folderPath = "uglyforms/";
            String id = UUID.randomUUID().toString() + "." + FilenameUtils.getExtension(file.getOriginalFilename());

            final String key = AmazonS3Manager.getKey(folderPath, id);

            amazonS3Client.putObject(key, file.getBytes(), file.getContentType());

            String url = amazonS3Client.getCloudFrontUrlByKey(key);
            LOGGER.debug("url {}", url);
            return url;
        }
        return fileName;
    }
    
//    @RequestMapping(value = "v1/test", method = RequestMethod.GET)
//    public void getImage(HttpServletRequest request, HttpServletResponse response) throws Exception {
//
//        String imageUrl = "http://lh3.googleusercontent.com/sDuN3K40fsyWQAyOVsTaJ-3FfaiXAyQ-9lKcuJvJF630wl9IZUHSQS7fCIioOyiAwAjSI0u8bqeb0vthgt371WyXbxikwgcu=s500";
//        
//        //folder uglyforms
//        String folderPath ="uglyforms/";
//        String id = UUID.randomUUID().toString() + "-test.png";
//        
//        final String key = AmazonS3Manager.getKey(folderPath, id);;
//        
//        amazonS3Client.putObject(key, IOUtils.toByteArray(new URI(imageUrl)), "image/png");
//        
//        
//        //String mimeType = URLConnection.guessContentTypeFromStream(is);
////        S3Object s3Object = amazonS3Client.get(folderPath + id);
////        InputStream is = s3Object.getObjectContent();
////        response.setContentType("image/png");
////        IOUtils.copy(is, response.getOutputStream());
//        
//        response.sendRedirect(amazonS3Client.getCloudFrontUrlByKey(key));
//        
//    }
  
}
