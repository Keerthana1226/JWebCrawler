package com.jwebcrawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class CrawlerWorker implements Runnable {

    private final Queue<String> frontier;
    private final Set<String> visited;
    private final AtomicInteger activeWorkers;
    private final RobotsTxtManager robotsTxtManager;
    private final DataWriter dataWriter; // Add this

    private final PageDownloader downloader = new PageDownloader();
    private final PageParser parser = new PageParser();

    // Update the constructor
    public CrawlerWorker(Queue<String> frontier, Set<String> visited, AtomicInteger activeWorkers, RobotsTxtManager robotsTxtManager, DataWriter dataWriter) {
        this.frontier = frontier;
        this.visited = visited;
        this.activeWorkers = activeWorkers;
        this.robotsTxtManager = robotsTxtManager;
        this.dataWriter = dataWriter; // Initialize it
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            String currentUrl = frontier.poll();

            if (currentUrl == null) {
                if (activeWorkers.get() == 0) {
                    break;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                continue;
            }

            if (visited.size() >= CrawlerApp.MAX_PAGES_TO_CRAWL) {
                break;
            }

            if (!robotsTxtManager.isAllowed(currentUrl)) {
                System.out.println("Disallowed by robots.txt: " + currentUrl);
                continue;
            }

            activeWorkers.incrementAndGet();

            System.out.println(Thread.currentThread().getName() + " is crawling: " + currentUrl);
            String html = downloader.download(currentUrl);

            if (html != null) {
                // =======================================================
                // NEW: EXTRACT AND SAVE DATA
                // =======================================================
                Document doc = Jsoup.parse(html, currentUrl);
                String title = doc.title();
                String description = doc.select("meta[name=description]").attr("content");
                if (description.isEmpty()) {
                    Element firstParagraph = doc.select("p").first();
                    if (firstParagraph != null) {
                        description = firstParagraph.text();
                    }
                }
                dataWriter.writeData(new CrawlData(currentUrl, title, description));
                // =======================================================


                List<String> newLinks = parser.parse(html, currentUrl);
                for (String link : newLinks) {
                    if (visited.size() < CrawlerApp.MAX_PAGES_TO_CRAWL && visited.add(link)) {
                        frontier.add(link);
                    }
                }
            }
            activeWorkers.decrementAndGet();
        }
        System.out.println(Thread.currentThread().getName() + " shutting down.");
    }
}