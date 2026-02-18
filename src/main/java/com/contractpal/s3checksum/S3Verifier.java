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
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import java.io.FileWriter;
import java.io.IOException;

public class S3Verifier {
    private final List<S3Object> items = new ArrayList<>();
    private final String csvPath;
    private final String bucket;
    private final S3Client client;
    private Boolean verified = false;

    public S3Verifier(String csvPath, String bucket, Region region, String accessKey, String secretKey) {
        this.bucket = bucket;
        this.csvPath = csvPath;
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
            writer.write("folderId,filename,size,md5,calculatedMd5,verified,comments");
            items.forEach(object -> {
                try {
                    writer.write(object.getFolderId() + "," + object.getFileName() + "," + object.getSize() + "," + object.getMd5() + "," + object.getCalculatedHash() + "," + object.getVerified() + "," + object.getComments());
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
        for(int i = 0; i < items.size(); i++) {
            verifyOne(i);
        }
        verified = true;
    }


    public Boolean verifyOne(int index) {
        S3Object row = items.get(index);
        try{

            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(this.bucket)
                    .key(row.getFolderId() + "/" + row.getFileName())
                    .build();
            System.out.println("Fetching object: " + row.getFolderId() + "/" + row.getFileName());

            ResponseBytes<GetObjectResponse> response = client.getObjectAsBytes(request);
            byte[] data = response.asByteArray();
            row.setCalculatedHash(getMd5(data));
            row.verify();
            if(!row.getVerified()) {
                row.setComments("Hash does not match");
            }
            System.out.println("Object verification: " + row.getVerified());
            return row.getVerified();
        }catch(NoSuchKeyException e) {
            System.out.println("ERROR: object not found " + row.getFolderId() + "/" + row.getFileName());
            row.unverify();
            row.setComments("Object not found");
            return false;
        }catch(NoSuchAlgorithmException e) {
            row.unverify();
            row.setComments("Check code for invalid hash algorithm");
            return false;
        }
    }

    private String getMd5(byte[] bytes) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(bytes);

        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }



    private void readFile() {
        File csv = new File(csvPath);

        try (Scanner myReader = new Scanner(csv)) {
            boolean colsRead = false;
            while (myReader.hasNextLine()) {
                if (!colsRead) {
                    myReader.nextLine();
                    colsRead = true;
                    continue;
                }
                String data = myReader.nextLine();
                String[] row = data.split(",");
                items.add(new S3Object(row[0], row[1], row[2], Integer.parseInt(row[3])));
            }
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }


}
