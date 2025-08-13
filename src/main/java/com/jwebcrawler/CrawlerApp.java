package com.jwebcrawler;

import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CrawlerApp {

    private final Queue<String> frontier = new ConcurrentLinkedQueue<>();
    private final Set<String> visited = ConcurrentHashMap.newKeySet();
    private final AtomicInteger activeWorkers = new AtomicInteger(0);
    private final RobotsTxtManager robotsTxtManager = new RobotsTxtManager();

    private static final int NUM_THREADS = 10;
    public static final int MAX_PAGES_TO_CRAWL = 500; // Now public for workers to see

    public void startCrawling(String seedUrl) {
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);

        frontier.add(seedUrl);
        visited.add(seedUrl);

        for (int i = 0; i < NUM_THREADS; i++) {
            executor.submit(new CrawlerWorker(frontier, visited, activeWorkers, robotsTxtManager));
        }

        try {
            executor.shutdown();
            if (!executor.awaitTermination(60, TimeUnit.MINUTES)) {
                System.err.println("Crawl timed out!");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println("\nCrawl finished. Visited " + visited.size() + " pages.");
    }

    public static void main(String[] args) {
        CrawlerApp crawler = new CrawlerApp();
        crawler.startCrawling("https://jsoup.org/");
    }
}