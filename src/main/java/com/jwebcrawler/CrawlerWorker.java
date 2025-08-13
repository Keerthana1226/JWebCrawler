package com.jwebcrawler;

import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class CrawlerWorker implements Runnable {

    private final Queue<String> frontier;
    private final Set<String> visited;
    private final AtomicInteger activeWorkers;
    private final RobotsTxtManager robotsTxtManager;

    private final PageDownloader downloader = new PageDownloader();
    private final PageParser parser = new PageParser();

    public CrawlerWorker(Queue<String> frontier, Set<String> visited, AtomicInteger activeWorkers, RobotsTxtManager robotsTxtManager) {
        this.frontier = frontier;
        this.visited = visited;
        this.activeWorkers = activeWorkers;
        this.robotsTxtManager = robotsTxtManager;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            if (visited.size() >= CrawlerApp.MAX_PAGES_TO_CRAWL) {
                break; // Stop if the global limit is reached
            }

            String currentUrl = frontier.poll();

            if (currentUrl == null) {
                if (activeWorkers.get() == 0) {
                    break; // The crawl is finished, exit the loop
                }
                try {
                    Thread.sleep(100); // Wait for more URLs
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                continue;
            }

            if (!robotsTxtManager.isAllowed(currentUrl)) {
                System.out.println("Disallowed by robots.txt: " + currentUrl);
                continue; // Skip this URL and get the next one
            }

            activeWorkers.incrementAndGet();

            System.out.println(Thread.currentThread().getName() + " is crawling: " + currentUrl);
            String html = downloader.download(currentUrl);

            if (html != null) {
                List<String> newLinks = parser.parse(html, currentUrl);
                for (String link : newLinks) {
                    if (visited.size() < CrawlerApp.MAX_PAGES_TO_CRAWL && visited.add(link)) {
                        frontier.add(link);
                    }
                }
            }
            activeWorkers.decrementAndGet();
        }
        System.out.println(Thread.currentThread().getName() + " finished.");
    }
}