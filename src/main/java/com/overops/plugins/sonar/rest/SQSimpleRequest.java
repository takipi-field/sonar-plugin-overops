package com.overops.plugins.sonar.rest;

import com.takipi.api.core.consts.ApiConstants;
import com.takipi.api.core.request.intf.ApiPostRequest;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class SQSimpleRequest implements ApiPostRequest<SQIssuesResponse> {
    private final String url;
    private final List<String> queryParams;

    public SQSimpleRequest(String url, List<String> queryParams) {
        this.url = url;
        this.queryParams = queryParams;
    }

    @Override
    public String contentType() {
        return ApiConstants.CONTENT_TYPE_JSON;
    }

    @Override
    public String urlPath() {
        return url;
    }

    @Override
    public String[] queryParams() throws UnsupportedEncodingException {
        return queryParams.toArray(new String[0]);
    }

    @Override
    public String postData() {
        return null;
    }

    @Override
    public Class<SQIssuesResponse> resultClass() {
        return SQIssuesResponse.class;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private String url = "";
        private List<String> queryParams = new ArrayList<>();

        public Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        public Builder setQueryParams(List<String> queryParams) {
            this.queryParams = queryParams;
            return this;
        }

        public Builder addQueryParam(String key, String value) {
            this.queryParams.add(key + "=" + value);
            return this;
        }

        public SQSimpleRequest build() {
            return new SQSimpleRequest(url, queryParams);
        }
    }
}
