package com.contractpal.s3checksum;

import java.util.Objects;

public class S3Object {

    private final String folderId;
    private final String fileName;
    private final String md5;
    private final int size;
    private String calculatedHash;
    private Boolean verified;
    private String comments;


    public S3Object(String folderId, String fileName, String checksum, int size) {
        this.folderId = folderId;
        this.fileName = fileName.isEmpty() ? null : fileName;
        this.md5 = checksum;
        this.size = size;
        this.verified = false;
    }

    public String getFileName() {
        return fileName;
    }

    public String getMd5() {
        return md5;
    }

    public int getSize() {
        return size;
    }

    public Boolean getVerified() {
        return verified;
    }

    public String getComments() {
        return comments;
    }

    public Boolean verify() {
        verified = Objects.equals(md5, calculatedHash);
        return verified;
    }

    public void unverify() {
        verified = false;
    }

    public void setComments(String comments) {
        this.comments = comments.replaceAll(",", "");
    }

    public String getFolderId() {
        return folderId;
    }

    public String getCalculatedHash() {
        return calculatedHash;
    }

    public void setCalculatedHash(String calculatedHash) {
        this.calculatedHash = calculatedHash;
    }
}
