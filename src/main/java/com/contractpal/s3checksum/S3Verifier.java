package com.contractpal.s3checksum;

import java.io.File;
import java.io.FileNotFoundException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.security.MessageDigest;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import java.io.InputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class S3Verifier {
    private final List<S3Object> items = new ArrayList<>();
    private final String bucket;
    private final S3Client client;
    private Boolean verified = false;

    public S3Verifier(String bucket, Region region, String accessKey, String secretKey) {
        this.bucket = bucket;
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        client = S3Client.builder()
                .region(region)
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
        readFile();
    }

    public Boolean exportResultToFile(String filePath) {
        if(!verified) {
            verifyAll();
        }
        try{
            File file = new File(filePath + "/s3-checksum.csv");
            file.getParentFile().mkdirs();
            FileWriter writer = new FileWriter(file);
            writer.write("folderId,filename,size,md5,calculatedMd5,verified,comments\n");
            items.forEach(object -> {
                try {
                    writer.write(object.getFolderId() + "," + object.getFileName() + "," + object.getSize() + "," + object.getMd5() + "," + object.getCalculatedHash() + "," + object.getVerified() + "," + object.getComments() + '\n');
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            writer.close();
            System.out.println("Results written to: " + filePath + "/s3-checksum.csv");
            return true;
        }catch(IOException t) {
            t.printStackTrace();
            return false;
        }
    }


    public void verifyAll() {
        ExecutorService executor = Executors.newFixedThreadPool(100);
        for(int i = 0; i < items.size(); i++) {
            final int index = i;
            executor.submit(() -> verifyOne(index));
        }
        executor.shutdown();
        try {
            executor.awaitTermination(15, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Verification interrupted");
        }
        verified = true;
    }


    public Boolean verifyOne(int index) {
        S3Object row = items.get(index);
        try{

            String key = row.getFolderId() != null && !row.getFolderId().isEmpty() ? row.getFolderId() + "/" + row.getFileName() : row.getFileName();
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(this.bucket)
                    .key(key)
                    .build();
            System.out.println("Fetching object: " + row.getFolderId() + "/" + row.getFileName());

            try (InputStream stream = client.getObject(request)) {
                row.setCalculatedHash(getMd5(stream));
            }
            if(!row.verify()) {
                row.setComments("Hash does not match");
            }
            System.out.println("Object verification: " + row.getVerified());
            return row.getVerified();
        }catch(NoSuchKeyException e) {
            System.out.println("ERROR: object not found " + row.getFolderId() + "/" + row.getFileName());
            row.unverify();
            row.setComments("Object not found");
            return false;
        }catch(IOException e) {
            row.unverify();
            row.setComments("Error reading object: " + e.getMessage());
            return false;
        }catch(NoSuchAlgorithmException e) {
            row.unverify();
            row.setComments("Check code for invalid hash algorithm");
            return false;
        }
    }

    private String getMd5(InputStream input) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            md.update(buffer, 0, bytesRead);
        }
        return java.util.Base64.getEncoder().encodeToString(md.digest());
    }



    private void readFile() {

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(this.bucket)
                .key("index.csv")
                .build();

        try (Scanner reader = new Scanner(client.getObject(request))) {
            boolean colsRead = false;
            while (reader.hasNextLine()) {
                if (!colsRead) {
                    reader.nextLine();
                    colsRead = true;
                    continue;
                }
                String data = reader.nextLine();
                String[] row = data.split(",");
                for (int i = 0; i < row.length; i++) {
                    row[i] = row[i].replace("\"", "").trim();
                }
                items.add(new S3Object(row[0], row[1], row[2], Integer.parseInt(row[3])));
            }
        }
    }


}
