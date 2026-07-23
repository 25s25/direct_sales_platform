package com.ds.oss.core;

import java.io.InputStream;

public interface OssClient {

    UploadResult upload(InputStream inputStream, String originalName, long size, String module);

    void delete(String filePath);

    String getUrl(String filePath);

    boolean supports(OssType type);
}
