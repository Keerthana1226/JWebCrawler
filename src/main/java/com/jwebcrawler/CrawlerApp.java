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
    private final AtomicInteger activeWorkers = new AtomicInteger(0); // The shared counter

    private static final int NUM_THREADS = 10;
    // Let's increase the limit to see more parallelism
    private static final int MAX_PAGES_TO_CRAWL = 500;

    public void startCrawling(String seedUrl) {
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);

        frontier.add(seedUrl);
        visited.add(seedUrl);

        // Submit tasks for all threads
        for (int i = 0; i < NUM_THREADS; i++) {
            // Pass the shared counter to each worker
            executor.submit(new CrawlerWorker(frontier, visited, activeWorkers));
        }

        // The shutdown logic can now be simplified slightly because workers are smarter
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