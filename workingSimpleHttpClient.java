package com.example;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class SimpleHttpClient {
    public static void main(String[] args) throws Exception {
        // Change localhost → server IP if running on different machine
        String serverUrl = "http://localhost:8000/chat";
        String message = "Hello world !";

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(serverUrl);
            post.setEntity(new StringEntity(message));
            post.setHeader("Content-Type", "text/plain");

            try (CloseableHttpResponse response = httpClient.execute(post)) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    String responseString = EntityUtils.toString(entity);
                    System.out.println("Response: " + responseString);
                }
            }
        }
    }
}
