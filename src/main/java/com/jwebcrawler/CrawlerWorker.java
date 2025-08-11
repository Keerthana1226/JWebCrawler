package com.jwebcrawler;

import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class CrawlerWorker implements Runnable {

    private final Queue<String> frontier;
    private final Set<String> visited;
    private final AtomicInteger activeWorkers; // Shared counter

    private final PageDownloader downloader = new PageDownloader();
    private final PageParser parser = new PageParser();

    // Update the constructor to accept the shared counter
    public CrawlerWorker(Queue<String> frontier, Set<String> visited, AtomicInteger activeWorkers) {
        this.frontier = frontier;
        this.visited = visited;
        this.activeWorkers = activeWorkers;
    }

    @Override
    public void run() {
        // The main loop now continues as long as this thread hasn't been interrupted.
        while (!Thread.currentThread().isInterrupted()) {
            String currentUrl = frontier.poll();

            if (currentUrl == null) {
                // Frontier is empty. If no other workers are busy, we might be done.
                // A worker can exit its loop if the frontier is empty and no other threads are working.
                if (activeWorkers.get() == 0) {
                    break; // Exit the loop
                }
                // Otherwise, wait a bit for other threads to maybe populate the frontier.
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Preserve the interrupted status
                }
                continue; // Try polling again
            }

            // If we got a URL, we are now "active".
            activeWorkers.incrementAndGet();

            System.out.println(Thread.currentThread().getName() + " is crawling: " + currentUrl);
            String html = downloader.download(currentUrl);

            if (html != null) {
                List<String> newLinks = parser.parse(html, currentUrl);
                for (String link : newLinks) {
                    if (visited.add(link)) {
                        frontier.add(link);
                    }
                }
            }

            // We are done with this URL, so we are no longer "active".
            activeWorkers.decrementAndGet();
        }
        System.out.println(Thread.currentThread().getName() + " finished.");
    }
}