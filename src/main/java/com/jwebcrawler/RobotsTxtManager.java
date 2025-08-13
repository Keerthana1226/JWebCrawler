package com.jwebcrawler;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class RobotsTxtManager {

    // A thread-safe cache to store the rules for each domain.
    // Key: The domain host (e.g., "jsoup.org").
    // Value: A list of disallowed paths (e.g., ["/private/", "/admin/"]).
    private final Map<String, List<String>> cache = new ConcurrentHashMap<>();
    private final PageDownloader downloader = new PageDownloader();

    /**
     * Checks if a given URL is allowed to be crawled based on the site's robots.txt.
     * @param url The full URL to check.
     * @return true if the URL is allowed, false otherwise.
     */
    public boolean isAllowed(String url) {
        try {
            URL urlObject = new URL(url);
            String host = urlObject.getHost();
            String path = urlObject.getPath();

            // Get the disallowed paths for this host (from cache or by downloading).
            List<String> disallowedPaths = getDisallowedPaths(host);

            // Check if the URL's path starts with any of the disallowed paths.
            for (String disallowedPath : disallowedPaths) {
                if (path.startsWith(disallowedPath)) {
                    return false; // This path is disallowed.
                }
            }

        } catch (MalformedURLException e) {
            System.err.println("Malformed URL in isAllowed check: " + url);
            return false; // Don't crawl malformed URLs.
        }
        return true; // No matching disallow rule found, so it's allowed.
    }

    private List<String> getDisallowedPaths(String host) {
        // computeIfAbsent is a thread-safe way to populate the cache.
        // It guarantees the code inside the lambda is only executed once per host.
        return cache.computeIfAbsent(host, h -> {
            System.out.println("Fetching robots.txt for: " + h);
            String robotsUrl = "http://" + h + "/robots.txt";
            String content = downloader.download(robotsUrl);
            return parseRobotsTxt(content);
        });
    }

    /**
     * A simple parser for robots.txt files.
     * It looks for our specific user-agent or the wildcard '*' and collects Disallow rules.
     * @param content The content of the robots.txt file.
     * @return A list of disallowed paths.
     */
    private List<String> parseRobotsTxt(String content) {
        List<String> disallowedPaths = new ArrayList<>();
        if (content == null) {
            return disallowedPaths; // No content, so no rules.
        }

        Scanner scanner = new Scanner(content);
        boolean ourAgentBlock = false;
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();

            if (line.toLowerCase().startsWith("user-agent:")) {
                String agent = line.substring("user-agent:".length()).trim();
                // Check if the rules apply to us (either our specific agent or the wildcard).
                if (agent.equals("*") || agent.equals("JWebCrawler/1.0")) {
                    ourAgentBlock = true;
                } else {
                    ourAgentBlock = false;
                }
            } else if (ourAgentBlock && line.toLowerCase().startsWith("disallow:")) {
                String path = line.substring("disallow:".length()).trim();
                if (!path.isEmpty()) {
                    disallowedPaths.add(path);
                }
            } else if (line.isEmpty()) {
                // An empty line signifies the end of a rule block.
                ourAgentBlock = false;
            }
        }
        scanner.close();
        return disallowedPaths;
    }
}