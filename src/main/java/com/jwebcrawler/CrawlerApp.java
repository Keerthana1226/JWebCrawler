package com.jwebcrawler;

import java.io.IOException;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CrawlerApp {

    // These are now final and can be passed to workers
    private final Queue<String> frontier = new ConcurrentLinkedQueue<>();
    private final Set<String> visited = ConcurrentHashMap.newKeySet();
    private final AtomicInteger activeWorkers = new AtomicInteger(0);
    private final RobotsTxtManager robotsTxtManager = new RobotsTxtManager();
    private final Object lock = new Object(); // Shared lock for coordination

    private static final int NUM_THREADS = 10;
    public static final int MAX_PAGES_TO_CRAWL = 1300;

    public void startCrawling(String seedUrl) {
        DataWriter dataWriter = null;
        try {
            dataWriter = new DataWriter("results.csv");
            ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);

            frontier.add(seedUrl);
            visited.add(seedUrl);

            for (int i = 0; i < NUM_THREADS; i++) {
                // Pass the shared lock to each worker
                executor.submit(new CrawlerWorker(frontier, visited, activeWorkers, robotsTxtManager, dataWriter, lock));
            }

            // The main thread's job is now just to start and wait.
            executor.shutdown(); // Disable new tasks from being submitted

            // Wait until all threads have finished their work.
            if (!executor.awaitTermination(1, TimeUnit.HOURS)) {
                System.err.println("Threads did not terminate in an hour.");
                executor.shutdownNow(); // Force shutdown
            }

        } catch (IOException e) {
            System.err.println("Error initializing DataWriter: " + e.getMessage());
        } catch (InterruptedException e) {
            System.err.println("Main thread interrupted during awaitTermination.");
            Thread.currentThread().interrupt();
        } finally {
            if (dataWriter != null) {
                dataWriter.close();
            }
        }

        System.out.println("\nCrawl finished. Visited " + visited.size() + " pages.");
    }

    public static void main(String[] args) {
        CrawlerApp crawler = new CrawlerApp();
        crawler.startCrawling("https://jsoup.org/");
    }
}