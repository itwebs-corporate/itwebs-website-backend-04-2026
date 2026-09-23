package ru.itwebs.cms.service;

import ru.itwebs.cms.entity.MediaAsset;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import java.io.IOException;
import java.net.URI;

@Service
public class StorageService {
    private final S3Client s3;
    private final String bucket;

    public StorageService(@Value("${app.storage.endpoint}") String endpoint,
                          @Value("${app.storage.region}") String region,
                          @Value("${app.storage.bucket}") String bucket,
                          @Value("${app.storage.access-key}") String accessKey,
                          @Value("${app.storage.secret-key}") String secretKey) {
        if (accessKey.isBlank() || secretKey.isBlank()) throw new IllegalStateException("Set S3_ACCESS_KEY and S3_SECRET_KEY");
        this.bucket = bucket;
        this.s3 = S3Client.builder().endpointOverride(URI.create(endpoint)).region(Region.of(region))
                .forcePathStyle(true).credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey))).build();
    }

    @PostConstruct
    void ensureBucket() {
        try { s3.headBucket(HeadBucketRequest.builder().bucket(bucket).build()); }
        catch (NoSuchBucketException e) { s3.createBucket(CreateBucketRequest.builder().bucket(bucket).build()); }
        catch (S3Exception e) {
            if (e.statusCode() == 404) s3.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            else throw e;
        }
    }

    public void upload(MultipartFile file, String key, String type) throws IOException {
        s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key).contentType(type).build(),
                RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
    }

    public ResponseBytes<GetObjectResponse> download(MediaAsset asset) {
        return s3.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(asset.getObjectKey()).build());
    }

    public void delete(MediaAsset asset) {
        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(asset.getObjectKey()).build());
    }
}
