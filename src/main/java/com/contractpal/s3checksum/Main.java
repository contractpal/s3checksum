package com.contractpal.s3checksum;
import software.amazon.awssdk.regions.Region;

public class Main {
    public static void main(String[] args) {
        S3Verifier verifier = new S3Verifier(System.getenv("inputFilePath"), System.getenv("bucketName"), Region.of(System.getenv("region")), System.getenv("accessKey"), System.getenv("secretKey"));
        verifier.verifyAll();
        if(!verifier.exportResultToFile("output")){
            System.out.println("ERROR exporting results!");
        }
    }
}