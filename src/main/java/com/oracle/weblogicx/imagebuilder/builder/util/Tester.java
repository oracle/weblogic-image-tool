package com.oracle.weblogicx.imagebuilder.builder.util;

import org.apache.http.*;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.HttpClientParamConfig;
import org.apache.http.impl.client.*;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tester {

    public static void main(String[] args) throws IOException {
        Path DEFAULT_META_PATH = Paths.get(System.getProperty("user.home") + File.separator + "cache" +
                File.separator + ".metadata");
        System.out.println(DEFAULT_META_PATH.toString());
        System.out.println(DEFAULT_META_PATH.toAbsolutePath());
//        CookieStore cookieStore = new BasicCookieStore();
//        BasicClientCookie acookie = new BasicClientCookie("oraclelicense", "a");
//        acookie.setDomain("edelivery.oracle.com");
//        cookieStore.addCookie(acookie);
//
//        RequestConfig requestConfig = RequestConfig.custom().setCircularRedirectsAllowed(true).build();
//
//        CloseableHttpClient client = HttpClientBuilder.create()
//                .setRedirectStrategy(new LaxRedirectStrategy())
//                .useSystemProperties()
//                .setDefaultRequestConfig(requestConfig)
//                .setDefaultCookieStore(cookieStore)
//                .build();
//
//        Executor httpExecutor = Executor.newInstance(client).auth("gopi.suryadevara@oracle.com", "omSAI@123");
//
//        httpExecutor.execute(Request.Get("https://download.oracle.com/otn-pub/java/jdk/8u191-b12/2787e4a523244c269598db4e85c51e0c/jdk-8u191-linux-x64.tar.gz")
//                .connectTimeout(1000)
//                .socketTimeout(10000))
//                .saveContent(new File("/Users/gsuryade/Downloads/tmp/jdk8/jdk.zip"));

        System.out.println("============");
    }
}
