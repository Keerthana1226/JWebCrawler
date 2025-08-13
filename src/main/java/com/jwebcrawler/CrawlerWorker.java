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
    private final DataWriter dataWriter;

    private final PageDownloader downloader = new PageDownloader();
    private final PageParser parser = new PageParser();

    public CrawlerWorker(Queue<String> frontier, Set<String> visited, AtomicInteger activeWorkers, RobotsTxtManager robotsTxtManager, DataWriter dataWriter) {
        this.frontier = frontier;
        this.visited = visited;
        this.activeWorkers = activeWorkers;
        this.robotsTxtManager = robotsTxtManager;
        this.dataWriter = dataWriter;
    }

    @Override
    public void run() {
        // The worker's only job is to run until the main thread interrupts it.
        while (!Thread.currentThread().isInterrupted()) {
            String currentUrl = frontier.poll();

            if (currentUrl == null) {
                // The frontier is temporarily empty. Let's wait a moment and try again.
                // The manager in CrawlerApp will tell us when to stop for good.
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Preserve the interrupt status and exit the loop.
                }
                continue; // Go back to the start of the loop to check for more work.
            }

            // If we get here, we have a URL to process.
            if (!robotsTxtManager.isAllowed(currentUrl)) {
                System.out.println("Disallowed by robots.txt: " + currentUrl);
                continue; // Skip this URL.
            }

            activeWorkers.incrementAndGet();

            System.out.println(Thread.currentThread().getName() + " is crawling: " + currentUrl);
            String html = downloader.download(currentUrl);

            if (html != null) {
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

                List<String> newLinks = parser.parse(html, currentUrl);
                for (String link : newLinks) {
                    // The check to stop adding links is now the only place we need this
                    if (visited.size() < CrawlerApp.MAX_PAGES_TO_CRAWL) {
                        if(visited.add(link)) {
                            frontier.add(link);
                        }
                    }
                }
            }
            activeWorkers.decrementAndGet();
        }
        System.out.println(Thread.currentThread().getName() + " shutting down.");
    }
}