package com.contractpal.s3checksum;
import software.amazon.awssdk.regions.Region;

public class Main {
    public static void main(String[] args) {
        String[] required = {"bucketName", "region", "accessKey", "secretKey"};
        for (String var : required) {
            if (System.getenv(var) == null || System.getenv(var).isEmpty()) {
                System.out.println("ERROR: Missing required environment variable: " + var);
                System.exit(1);
            }
        }

        long start = System.currentTimeMillis();
        S3Verifier verifier = new S3Verifier(System.getenv("bucketName"), Region.of(System.getenv("region")), System.getenv("accessKey"), System.getenv("secretKey"));
        verifier.verifyAll();
        if(!verifier.exportResultToFile("output")){
            System.out.println("ERROR exporting results!");
            System.exit(1);
        }
        double minutes = (System.currentTimeMillis() - start) / 60000.0;
        System.out.printf("Completed in %.2f minutes%n", minutes);
        System.exit(0);
    }
}