package com.jwebcrawler;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration; // Import this

public class PageDownloader {

    private final HttpClient httpClient;

    public PageDownloader() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.ALWAYS)
                // =======================================================
                // NEW: ADD A CONNECTION TIMEOUT
                // =======================================================
                .connectTimeout(Duration.ofSeconds(10)) // Give up after 10 seconds
                // =======================================================
                .build();
    }

    public String download(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "JWebCrawler/1.0")
                    // =======================================================
                    // NEW: ADD A REQUEST TIMEOUT
                    // =======================================================
                    .timeout(Duration.ofSeconds(15)) // The entire request must finish in 15 seconds
                    // =======================================================
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return response.body();
            } else {
                System.err.println("Failed to download " + url + ". Status code: " + response.statusCode());
                return null;
            }
        } catch (IOException | InterruptedException | IllegalArgumentException e) {
            System.err.println("Error downloading " + url + ": " + e.getMessage());
            return null;
        }
    }
}