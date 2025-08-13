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

    private final Queue<String> frontier = new ConcurrentLinkedQueue<>();
    private final Set<String> visited = ConcurrentHashMap.newKeySet();
    private final AtomicInteger activeWorkers = new AtomicInteger(0);
    private final RobotsTxtManager robotsTxtManager = new RobotsTxtManager();

    private static final int NUM_THREADS = 10;
    public static final int MAX_PAGES_TO_CRAWL = 500;

    public void startCrawling(String seedUrl) {
        DataWriter dataWriter = null;
        try {
            // Initialize the writer.
            dataWriter = new DataWriter("results.csv");

            ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);

            frontier.add(seedUrl);
            visited.add(seedUrl);

            for (int i = 0; i < NUM_THREADS; i++) {
                // Pass the writer instance to each worker.
                executor.submit(new CrawlerWorker(frontier, visited, activeWorkers, robotsTxtManager, dataWriter));
            }

            // ... (The smart manager shutdown logic remains the same)
            int quietChecks = 0;
            final int REQUIRED_QUIET_CHECKS = 3;

            while (true) {
                try {
                    Thread.sleep(1000);
                    boolean isFrontierEmpty = frontier.isEmpty();
                    boolean areWorkersIdle = activeWorkers.get() == 0;
                    boolean isBootstrapPhase = (visited.size() <= 1);

                    if (isFrontierEmpty && areWorkersIdle && !isBootstrapPhase) {
                        quietChecks++;
                        System.out.println("Manager: System appears idle. Quiet check #" + quietChecks);
                    } else {
                        quietChecks = 0;
                    }

                    if (quietChecks >= REQUIRED_QUIET_CHECKS) {
                        System.out.println("Manager: System has been idle. Shutting down.");
                        executor.shutdownNow();
                        break;
                    }

                    if (visited.size() >= MAX_PAGES_TO_CRAWL) {
                        System.out.println("Manager: Max page limit reached. Shutting down.");
                        executor.shutdownNow();
                        break;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            try {
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    System.err.println("Threads did not terminate gracefully.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

        } catch (IOException e) {
            System.err.println("Error initializing DataWriter: " + e.getMessage());
        } finally {
            // IMPORTANT: Ensure the writer is closed to save all data.
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