package com.zuovil.haChallenges.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.*;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.ssl.SSLContexts;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class HttpConnector {

    private static CloseableHttpClient client;

    private final static ObjectMapper mapper = new ObjectMapper();

    static {
        client = getHttpClient();
    }

    public String doGet(String url) {
        if(url == null || url.isEmpty()) {
            return null;
        }
        try {
            HttpGet request  = new HttpGet(new URI(url));
            TypeReference<String> ref = new TypeReference<String>() { };
            Response<String> response = httpRequest(ref, request);
            if (response == null) {
                return null;
            }
            if (response.getCode() == 200) {
                return response.getData();
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    public String doPost(String url, Map<String, Object> params) {
        if(url == null || url.isEmpty()) {
            return null;
        }
        try {
            String jsonStr = params != null ? mapper.writer().writeValueAsString(params) : null;
            HttpPost request = new HttpPost(new URI(url));
            request.addHeader("Content-Type", "application/json");
            if(jsonStr != null) {
                request.setEntity(new StringEntity(jsonStr));
            }

            TypeReference<String> ref = new TypeReference<String>() { };
            Response<String> response = httpRequest(ref, request);
            if (response == null) {
                return null;
            }
            if (response.getCode() == 200) {
                return response.getData();
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    private static <T> Response<T> httpRequest(TypeReference<T> tp, HttpUriRequest httpRequest) {
        if(client == null) {
            client = getHttpClient();
        }
        Response<T> resp = null;
        try {
            resp = client.execute(httpRequest, response -> {
                if (response.getEntity() != null) {
                    String body = EntityUtils.toString(response.getEntity());
                    if (tp.getType() == String.class) {
                        return new Response<>(response.getCode(), body, (T)body);
                    }
                    // 当需要区分更多类型时可以增加定义
                    else {
                        T t = mapper.readValue(body, tp);
                        return new Response<>(response.getCode(), body, t);
                    }
                } else {
                    return new Response<>(response.getCode(), null, null);
                }
            });
            return resp;
        } catch (Exception e) {
            // 当异常也需要返回 Response 类型对象时可以在catch中封装
            System.out.println(e.getMessage());
        }
        return null;
    }

    private static CloseableHttpClient getHttpClient() {
        // 设置超时
        final RequestConfig defaultRequestConfig = RequestConfig.custom()
                                                                .setConnectionRequestTimeout(60, TimeUnit.SECONDS)
                                                                .setResponseTimeout(60, TimeUnit.SECONDS)
                                                                .build();
        // 设置 SSL TrustAllStrategy
        final BasicCookieStore defaultCookieStore = new BasicCookieStore();
        SSLContext sslContext = null;
        // 在httpclient5:5.4.4中，用于禁止SSL/TLS证书验证的方法setSSLSocketFactory、构造器SSLConnectionSocketFactoryBuilder已被弃用，现在也不推荐使用被弃用的方法
        try {
            sslContext = SSLContextBuilder.create()
                                          .loadTrustMaterial(TrustAllStrategy.INSTANCE)
                                          .build();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return HttpClients.custom()
                          .setDefaultCookieStore(defaultCookieStore)
                          .setDefaultRequestConfig(defaultRequestConfig)
                          .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
                                                                                         .setTlsSocketStrategy(new DefaultClientTlsStrategy(sslContext, NoopHostnameVerifier.INSTANCE))
                                                                                         .build())
                          .evictExpiredConnections()
                          .build();
    }
}
