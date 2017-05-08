package com.sma.backend.service;

import java.awt.Dimension;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.CopyObjectResult;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

@Service("amazonS3Manager")
public class AmazonS3Manager {

    private Logger LOG = LoggerFactory.getLogger(AmazonS3Manager.class);
    public static final Integer DEFAULT_CACHE_DAYS = 180; // 6 months
    public static final String GZIP_EXTENSION = "gzip";
    public static final String VIRTUALDIRECTORY_MESSAGE_MEDIA = "message/media/";
    
    public static final long ONE_HOUR_MILLIS = 60 * 60 * 1000;
    public static final long ONE_DAY_MILLIS = ONE_HOUR_MILLIS * 24;
    public static final long ONE_WEEK_MILLIS = ONE_DAY_MILLIS * 7;

    public static final long ONE_HOUR_SECONDS = 60 * 60;
    public static final long ONE_DAY_SECONDS = ONE_HOUR_SECONDS * 24;
    public static final long ONE_WEEK_SECONDS = ONE_DAY_SECONDS * 7;
    public static final long SIX_MONTHS_SECONDS = ONE_DAY_SECONDS * 6 * 30;

    
    public static final String RULEID_DELETE = "Delete rule";
    @Value("${amazon.s3.baseDownloadUrl}")
    private String baseDownloadUrl;
    @Value("${aws.s3.accessKeyId}")
    private String accessKey;
    @Value("${aws.s3.secretKey}")
    private String secretKey;
    @Value("${aws.s3.defaultBucketName}")
    private String bucketName;
    @Value("${aws.s3.defaultBucketRegion}")
    private String bucketRegion;
    @Value("${amazon.s3.objectTimeTiLiveInDays}")
    private int objectTimeTiLiveInDays;
    @Value("${cloudfront.asset.path.enabled}")
    private boolean isCloudFrontAssetPathEnabled;
    @Value("${isCloudFrontGzipAssetPathEnabled}")
    private boolean isCloudFrontGzipAssetPathEnabled;
    
    private Map<String, String> cache = new ConcurrentHashMap<>();
    
    //amazon S3 client
    private AmazonS3 amazonS3Client;

    public static String getKey(String folderPath, String objectId) {
        if (folderPath == null) {
            return objectId;
        }
        return folderPath + objectId;
    }

    public static ObjectMetadata createObjectMetadata(int contentLength, String contentType, String contentDeposition) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(contentLength);
        metadata.setContentType(contentType);
        metadata.setContentDisposition(contentDeposition);
        return metadata;
    }

    /**
     * initialize Amazon S3 client
     *
     * @throws FileNotFoundException
     * @throws IOException
     */
    @PostConstruct
    public void initialize() throws FileNotFoundException, IOException {
        LOG.info(">> Initializing Amazon S3... {}", bucketRegion);
        // New scheme of authentication required for the region central Europe
        //System.setProperty(SDKGlobalConfiguration.ENABLE_S3_SIGV4_SYSTEM_PROPERTY, "true");

        final AWSCredentials creds = new BasicAWSCredentials(accessKey, secretKey);

        amazonS3Client = AmazonS3ClientBuilder.standard()
                         .withCredentials(new AWSStaticCredentialsProvider(creds)).withRegion(bucketRegion)
                         .build();
        
        LOG.debug(">> Constructed AmazonS3Client object ");

        try {
            boolean exists = false;
            for (Bucket bucket : amazonS3Client.listBuckets()) {
                LOG.debug("S3 bucket: " + bucket.getName());

                if (bucket.getName().equals(bucketName)) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                amazonS3Client.createBucket(bucketName);

                LOG.info(">> Created bucket " + bucketName + " on Amazon S3");

                amazonS3Client.setBucketLifecycleConfiguration(bucketName, createBucketLifecycleConfiguration(RULEID_DELETE,
                    VIRTUALDIRECTORY_MESSAGE_MEDIA, objectTimeTiLiveInDays));

                LOG.info(">> Set default life cycle configuration for the bucket " + bucketName);
            }
        } catch (Throwable e) {
            LOG.warn("Unable to initialize S3", e);
        }
    }
    /**
     * list object
     * @param prefix
     * @return
     */
    public ObjectListing listObject(String prefix) {
        return this.amazonS3Client.listObjects(bucketName, prefix);
    }

    public ObjectListing listObjects(ListObjectsRequest listObjectsRequest) {
        return this.amazonS3Client.listObjects(listObjectsRequest);
    }

    public String createObjectDownloadUrl(String folderPath, String objectId) {
        final String key = getKey(folderPath, objectId);
        return baseDownloadUrl + key;
    }

    public String getKey(String downloadUrl) {
        return downloadUrl.substring(baseDownloadUrl.length());
    }

    public void putObject(String objId, MultipartFile file) throws IOException {
        putObject(objId, IOUtils.toByteArray(file.getInputStream()), file.getContentType(), DEFAULT_CACHE_DAYS);
    }

    public String getBucketLocation() {
        return amazonS3Client.getBucketLocation(bucketName);
    }

    public void putObject(String id, byte[] bytes) throws IOException {
        putObject(id, bytes, "application/octet-stream", DEFAULT_CACHE_DAYS);
    }

    public void putObject(String id, byte[] bytes, String contentType, Integer cachePeriod) throws IOException {
        putObject(id, bytes, contentType, true, cachePeriod);
    }

    public void putObject(String id, byte[] content, String contentType) throws IOException {
        putObject(id, new AmazonS3Object(contentType, content), true, DEFAULT_CACHE_DAYS);
    }

    public void putObject(String id, byte[] bytes, String contentType, boolean publicAccess, Integer cachePeriod)
        throws IOException {
        putObject(id, new AmazonS3Object(contentType, bytes), publicAccess, cachePeriod);
    }

    public void putObject(String id, String folderPath, byte[] bytes, String contentType, boolean publicAccess)
            throws IOException {
        ObjectMetadata om = new ObjectMetadata();
        om.setContentType(contentType);
        om.setCacheControl(getCacheControl(DEFAULT_CACHE_DAYS));
        om.setHttpExpiresDate(getHttpExpiresDate(DEFAULT_CACHE_DAYS));
        
        putObject(id, folderPath, bytes, publicAccess, om);
    }
    
    public void putObject(String id, InputStream is, String contentType, long contentLength, boolean publicAccess,
            Integer cachePeriod) {
        ObjectMetadata om = new ObjectMetadata();
        om.setContentType(contentType);
        om.setCacheControl(getCacheControl(cachePeriod));
        om.setHttpExpiresDate(getHttpExpiresDate(cachePeriod));
        om.setContentLength(contentLength);

        PutObjectRequest request = new PutObjectRequest(bucketName, id, is, om);

        if (publicAccess) {
            request.setCannedAcl(CannedAccessControlList.PublicRead);
        }
        amazonS3Client.putObject(request);

        LOG.debug(String.format("Put code object data for id %s", id));

    }

    public static String getCacheControl(Integer days) {
        long cacheSeconds = days == null ? 0 : ONE_DAY_SECONDS * days;
        return new StringBuilder("max-age=").append(cacheSeconds).toString();
    }
    
    public static Date getHttpExpiresDate(Integer days) {
        Calendar expires = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        expires.setTime(new Date());
        expires.add(Calendar.DATE, days == null ? 0 : days);
        return expires.getTime();
    }
    
    public void delete(String id) {
        amazonS3Client.deleteObject(bucketName, id);
    }

    public void putObject(String imageId, AmazonS3Object imageObject) {
        putObject(imageId, imageObject, true, DEFAULT_CACHE_DAYS);
    }

    /**
     * Put object
     *
     * @param id
     * @param object
     * @param publicAccess
     */
    public void putObject(String id, AmazonS3Object object, boolean publicAccess, Integer cacheDurationAsDays) {
        final ObjectMetadata om = new ObjectMetadata();
        
        byte[] bytes = object.getData();
        om.setContentLength(bytes.length);
        om.setContentType(object.getContentType());
        
        om.setCacheControl(getCacheControl(cacheDurationAsDays));
        om.setHttpExpiresDate(getHttpExpiresDate(cacheDurationAsDays));
        
        final ByteArrayInputStream bais = new ByteArrayInputStream(bytes);

        final PutObjectRequest request = new PutObjectRequest(bucketName, id, bais, om);
        if (publicAccess) {
            request.setCannedAcl(CannedAccessControlList.PublicRead);
        }
        amazonS3Client.putObject(request);

        LOG.debug(String.format("Put code object data for id %s", id));
    }

    public void putGzipObject(String id, byte[] content, String contentType, boolean publicAccess, Integer cacheDurationAsDays) {
        final ObjectMetadata om = new ObjectMetadata();
        om.setContentLength(content.length);
        om.setContentType(contentType);
        om.setContentEncoding(GZIP_EXTENSION);
        om.setCacheControl(getCacheControl(cacheDurationAsDays));
        om.setHttpExpiresDate(getHttpExpiresDate(cacheDurationAsDays));
        ByteArrayInputStream bais = new ByteArrayInputStream(content);

        PutObjectRequest request = new PutObjectRequest(bucketName, id, bais, om);
        if (publicAccess) {
            request.setCannedAcl(CannedAccessControlList.PublicRead);
        }
        amazonS3Client.putObject(request);

        LOG.debug(String.format("Put code object data for id %s", id));
    }

   
    /**
     * Put object
     *
     * @param id
     * @param folderPath   for example, message-media
     * @param object
     * @param contentType
     * @param publicAccess
     */
    public void putObject(String id, String folderPath, byte[] object, boolean publicAccess, ObjectMetadata metadata) {
        final String key = getKey(folderPath, id);

        ByteArrayInputStream bais = new ByteArrayInputStream(object);

        PutObjectRequest request = new PutObjectRequest(bucketName, key, bais, metadata);
        if (publicAccess) {
            request.setCannedAcl(CannedAccessControlList.PublicRead);
        }
        //put object into S3 bucket
        amazonS3Client.putObject(request);

        LOG.info(String.format(">> Put object (key=%s) in Amazon S3", key));
    }

    public CopyObjectResult copyObject(CopyObjectRequest copyObjectRequest) {
        return amazonS3Client.copyObject(copyObjectRequest);
    }

    public ObjectMetadata getObjectMetadata(String key) {
        return amazonS3Client.getObjectMetadata(bucketName, key);
    }

    public BucketLifecycleConfiguration createBucketLifecycleConfiguration(String ruleId, String objectPrefix,
                                                                           int objectTimeTiLiveInDays) {
        BucketLifecycleConfiguration.Rule ruleArchiveAndExpire = new BucketLifecycleConfiguration.Rule().withId(ruleId)
            .withPrefix(objectPrefix).withExpirationInDays(objectTimeTiLiveInDays)
            .withStatus(BucketLifecycleConfiguration.ENABLED);

        List<BucketLifecycleConfiguration.Rule> rules = new ArrayList<>();
        rules.add(ruleArchiveAndExpire);

        return new BucketLifecycleConfiguration().withRules(rules);
    }

    /**
     * Has object
     *
     * @param id
     * @return byte[]
     */
    public boolean hasObject(String id) {
        try {
            ObjectMetadata meta = amazonS3Client.getObjectMetadata(bucketName, id);
            return null != meta;
        } catch (Exception e) {
            return false;
        }
    }

    public Date getExpirationTime(String bucketName, String key) {
        try {
            return amazonS3Client.getObjectMetadata(bucketName, key).getExpirationTime();
        } catch (AmazonS3Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * @param key : path of the file without bucket name.
     * @return
     */
    public S3Object get(String key) {
        return amazonS3Client.getObject(bucketName, key);
    }

    public String getCloudFrontUrlByKey(String s3KeyId) {
        return new StringBuilder(getAssetServerUrl()).append("/").append(s3KeyId).toString(); 
    }
    /**
     * This method assumes the give S3 object URL has bucket name as context path. if not, it won't work correctly. For example,
     * the object URL should look like this
     * https://s3-ap-southeast-1.amazonaws.com/gg.backend-test-ap-southeast-1/portal-194-174151-products-1413897524.2187_6_o.jpg
     * and the result URL won't have bucket name, for example
     * http://d1tpr58s7zs699.cloudfront.net/portal-194-174151-products-1413897524.2187_6_o.jpg
     *
     * @param s3ObjectUrl
     * @return cloud front url to download the S3 object
     */
    public String getCloudFrontUrl(String s3ObjectUrl) {
        if (s3ObjectUrl == null) {
            return s3ObjectUrl;
        }

        String objUrlPath = getPath(s3ObjectUrl);

        LOG.debug(String.format(">> S3 object URL path: %s", objUrlPath));

        String urlPathPattern = "\\A(.[^/]*)/(.+)";
        Matcher matcher = Pattern.compile(urlPathPattern).matcher(objUrlPath);
        String s3ObjId = null;
        if (matcher.find()) {
            s3ObjId = matcher.group(2);
        }

        return new StringBuilder(getAssetServerUrl()).append("/").append(s3ObjId).toString();
    }


    public static String getPath(String url) {
        final String GET_URL_PATH_PATTERN = "\\A(http://|https://)(.[^/]*)/(.+)";
        Matcher matcher = Pattern.compile(GET_URL_PATH_PATTERN).matcher(url);
        if (matcher.find()) {
            return matcher.group(3);
        }
        return null;
    }
    
    public static String getAssetServerUrl() {
        return "https://d33ipftjqrd91.cloudfront.net";
    }
//    #asset.server.url.prefix=http://localhost:8080/cmsstatic
//        asset.server.url.prefix=http://dv9vy7p6qfoa3.cloudfront.net
//
//        # If left blank, the system will use the non secure url and replace the http with
//        # https.
//        asset.server.url.prefix.secure=https://dv9vy7p6qfoa3.cloudfront.net
            

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getBaseDownloadUrl() {
        return baseDownloadUrl;
    }

    public void setBaseDownloadUrl(String baseDownloadUrl) {
        this.baseDownloadUrl = baseDownloadUrl;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public void setObjectTimeTiLiveInDays(int objectTimeTiLiveInDays) {
        this.objectTimeTiLiveInDays = objectTimeTiLiveInDays;
    }

    public String getBucketRegion() {
        return bucketRegion;
    }

    public void setBucketRegion(String bucketRegion) {
        this.bucketRegion = bucketRegion;
    }

    public static byte[] compressWithGzip(byte[] content) throws IOException {

        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        final GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);
        gzipOutputStream.write(content);
        gzipOutputStream.close();

        return byteArrayOutputStream.toByteArray();
    }

   
    public String generateAssetVersionPath(String assetPath, String assetVersion) {
        return new StringBuilder(FilenameUtils.removeExtension(assetPath)).append(".").append(assetVersion).append(".")
            .append(FilenameUtils.getExtension(assetPath)).toString();
    }

    public String generateAssetGzipVersionPath(String assetPath, String assetVersion) {
        return new StringBuilder(FilenameUtils.removeExtension(assetPath)).append(".").append(assetVersion).append(".")
            .append(GZIP_EXTENSION).append(".").append(FilenameUtils.getExtension(assetPath)).toString();
    }

    public String getAssetVersion(byte[] content) {
        return String.valueOf(Arrays.hashCode(content));
    }

    public boolean existInCache(String assetPath) {
        return cache.get(assetPath) != null;
    }

    public boolean existVersionInCache(String assetPath, byte[] content) {
        String assetVersion = getAssetVersion(content);
        return cache.get(assetPath) != null && cache.get(assetPath).equals(assetVersion);
    }

    public boolean existGzipVersionInCache(String assetPath, byte[] content) {
        String assetVersion = getAssetVersion(content);
        String path = new StringBuilder(assetPath).append(".").append(GZIP_EXTENSION).toString();
        return cache.get(path) != null && cache.get(path).equals(assetVersion);
    }

    public boolean existInCloudFront(String assetPath) {
        boolean hasObject = hasObject(assetPath.substring(1));
        if (hasObject) {
            cache.put(assetPath, "1");
        }
        return hasObject;
    }

    public boolean existVersionInCloudFront(String assetPath, byte[] content) {
        String assetVersion = getAssetVersion(content);
        String assetVersionPath = generateAssetVersionPath(assetPath, assetVersion);

        boolean hasObject = hasObject(assetVersionPath.substring(1));
        if (hasObject) {
            cache.put(assetPath, assetVersion);
        }
        return hasObject;
    }

    public boolean existGzipVersionInCloudFront(String assetPath, byte[] content) {
        String assetVersion = getAssetVersion(content);
        String assetGzipVersionPath = generateAssetGzipVersionPath(assetPath, assetVersion);

        boolean hasObject = hasObject(assetGzipVersionPath.substring(1));
        if (hasObject) {
            String path = new StringBuilder(assetPath).append(".").append(GZIP_EXTENSION).toString();
            cache.put(path, assetVersion);
        }
        return hasObject;
    }


    public boolean isCloudFrontAssetPathEnabled() {
        return isCloudFrontAssetPathEnabled;
    }

    public boolean isGzipEnabled() {
//        BroadleafRequestContext instance = BroadleafRequestContext.getBroadleafRequestContext();
//        if (instance != null && instance.getRequest() != null) {
//            String acceptEncodingHeader = instance.getRequest().getHeader("Accept-Encoding");
//            boolean supportGzip = acceptEncodingHeader != null && acceptEncodingHeader.contains("gzip");
//            if (supportGzip && isCloudFrontGzipAssetPathEnabled) {
//                return true;
//            }
//        }
        return true;
    }

    public Dimension getImageDimension(String imageUrl) throws IOException {
        if (StringUtils.isEmpty(imageUrl)) {
            return null;
        }

        String encodedUrl = HttpUtils.urlEncode(imageUrl, true);
        if (imageUrl.toLowerCase().startsWith("http://") || imageUrl.toLowerCase().startsWith("https://")) {
            encodedUrl = encodedUrl.replaceFirst("%3A", ":");
        }

        URL url = new URL(encodedUrl);

        try (ImageInputStream in = ImageIO.createImageInputStream(url.openStream())) {
            final Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                try {
                    reader.setInput(in);

                    return new Dimension(reader.getWidth(0), reader.getHeight(0));
                } finally {
                    reader.dispose();
                }
            }
        }

        return null;
    }

    public static class AmazonS3Object {
        private String contentType;
        private byte[] data;
        private double maxSize;

        public AmazonS3Object() {
        }

        public AmazonS3Object(String contentType, byte[] data) {
            this.contentType = contentType;
            this.data = data;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public byte[] getData() {
            return data;
        }

        public void setData(byte[] data) {
            this.data = data;
        }

        public double getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(double maxSize) {
            this.maxSize = maxSize;
        }
    }
}
