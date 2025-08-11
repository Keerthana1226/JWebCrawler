package com.jwebcrawler;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class PageDownloader {

    // A single, reusable HttpClient instance. It's efficient to reuse it.
    private final HttpClient httpClient;

    public PageDownloader() {
        // Initialize the HttpClient.
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2) // Use HTTP/2 if available
                .followRedirects(HttpClient.Redirect.ALWAYS) // Follow redirects automatically
                .build();
    }

    /**
     * Downloads the HTML content from a given URL.
     * @param url The URL of the webpage to download.
     * @return The HTML content as a String, or null if an error occurs.
     */
    public String download(String url) {
        try {
            // Create a request for the given URL.
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "JWebCrawler/1.0") // Be a good bot and identify yourself
                    .build();

            // Send the request and get the response. We ask for the body as a String.
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Check if the request was successful (HTTP status code 200 OK)
            if (response.statusCode() == 200) {
                return response.body();
            } else {
                System.err.println("Failed to download " + url + ". Status code: " + response.statusCode());
                return null;
            }
        } catch (IOException | InterruptedException | IllegalArgumentException e) {
            // Handle exceptions like network errors or invalid URLs
            System.err.println("Error downloading " + url + ": " + e.getMessage());
            return null;
        }
    }
}