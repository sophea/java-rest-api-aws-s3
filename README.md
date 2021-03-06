![Travis Build](https://travis-ci.org/sophea/sample-backend-template.svg?branch=master)
[![Codecov](https://codecov.io/github/sophea/sample-backend-template/coverage.svg?branch=master)](https://codecov.io/github/sophea/sample-backend-template?branch=master)
![Java 8 required](https://img.shields.io/badge/java-8-brightgreen.svg)
[![Sputnik](https://sputnik.ci/conf/badge)](https://sputnik.ci/app#/builds/sophea/sample-backend-template)

sample-backend-template
=======================

rest-api backend none-gae : tomcat | jetty + database mybatis frameworks


To run this backend project : git clone from git hub : https://github.com/sophea/sample-backend-template

1 : install Java JDK 1.8 or later version , maven 2 or 3 

2 : install MySQL/mariaDb database (https://downloads.mariadb.org/)

3 : import sql file in mysql console : src/main/sql/sample-mybatis.sql 

4 : go to this project location by console

5 : create class-path for eclipse :mvn eclipse:eclipse

6 : run command mvn clean jetty:run  or mvn clean tomcat7:run

7 : sample APIs :

 
 a) category api: http://localhost:8080/api/category/v1/all
 
 b) category api: http://localhost:8080/api/category/v1/2

 c) monitor api : http://localhost:8080/api/monitor/v10

If you want to change database type :  have a look on file persistence-db.xml

================================

Upload image with Amazon S3 and using Cloudfront Service 

```
  <!-- https://mvnrepository.com/artifact/com.amazonaws/aws-java-sdk-s3 -->
    <dependency>
        <groupId>com.amazonaws</groupId>
        <artifactId>aws-java-sdk-s3</artifactId>
        <version>1.11.126</version>
    </dependency>

```

 - See Upload controller
 
 - AmazonS3Manager 

AWS console 
```
We need to grant appropriate permissions on the bucket to the AIM user so that backend is able to do operations such as put, get and delete objects or images on the bucket.
To grant the permissions, we need to add bucket policy and below is the bucket policy we add for Test environment.
Note that we should use AWS Policy Generator to generate the policy configuration below to avoid syntax error.
{
 "Version": "2008-10-17",
 "Id": "Policy1418211126005",
 "Statement": [
 {
 "Sid": "Stmt1418211122222",
 "Effect": "Allow",
 "Principal": {
 "AWS": "arn:aws:iam::615371640498:user/backend-dev-test"
 },
 "Action": "s3:*",
 "Resource": "arn:aws:s3:::backend-test-ap-southeast-1/*"
 }
 ]
}
```
