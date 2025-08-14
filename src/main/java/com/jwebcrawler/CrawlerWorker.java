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
    private final Object lock; // The shared lock object

    private final PageDownloader downloader = new PageDownloader();
    private final PageParser parser = new PageParser();

    public CrawlerWorker(Queue<String> frontier, Set<String> visited, AtomicInteger activeWorkers, RobotsTxtManager robotsTxtManager, DataWriter dataWriter, Object lock) {
        this.frontier = frontier;
        this.visited = visited;
        this.activeWorkers = activeWorkers;
        this.robotsTxtManager = robotsTxtManager;
        this.dataWriter = dataWriter;
        this.lock = lock;
    }

    @Override
    public void run() {
        while (true) {
            String currentUrl = null;

            // This synchronized block is the key to the solution.
            // It ensures no other thread can interfere between checking the frontier
            // and updating the active worker count.
            synchronized (lock) {
                if (!frontier.isEmpty()) {
                    currentUrl = frontier.poll();
                    activeWorkers.incrementAndGet();
                } else if (activeWorkers.get() == 0) {
                    // Frontier is empty AND no other workers are busy. The crawl is done.
                    // Notify any waiting threads just in case, then exit.
                    lock.notifyAll();
                    break; // Exit the while(true) loop
                } else {
                    // Frontier is empty, but other workers are busy.
                    // Wait for them to potentially add more URLs.
                    try {
                        // wait() releases the lock and waits efficiently
                        lock.wait(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break; // Exit if interrupted
                    }
                    continue; // Re-check the conditions in the next loop iteration
                }
            }

            // If we are here, it means we have a URL to process.
            // This processing happens OUTSIDE the synchronized block for performance.
            try {
                if (!robotsTxtManager.isAllowed(currentUrl)) {
                    System.out.println("Disallowed by robots.txt: " + currentUrl);
                    continue;
                }

                System.out.println(Thread.currentThread().getName() + " is crawling: " + currentUrl);
                String html = downloader.download(currentUrl);

                if (html != null) {
                    Document doc = Jsoup.parse(html, currentUrl);
                    String title = doc.title();
                    String description = doc.select("meta[name=description]").attr("content");
                    if (description.isEmpty()) {
                        Element firstParagraph = doc.select("p").first();
                        description = (firstParagraph != null) ? firstParagraph.text() : "";
                    }
                    dataWriter.writeData(new CrawlData(currentUrl, title, description));

                    List<String> newLinks = parser.parse(html, currentUrl);
                    int linksAdded = 0;
                    for (String link : newLinks) {
                        if (visited.size() < CrawlerApp.MAX_PAGES_TO_CRAWL) {
                            if (visited.add(link)) {
                                frontier.add(link);
                                linksAdded++;
                            }
                        } else {
                            break; // Stop adding links if max pages reached
                        }
                    }

                    // If we added new links, we must notify waiting threads
                    if (linksAdded > 0) {
                        synchronized (lock) {
                            lock.notifyAll();
                        }
                    }
                }
            } finally {
                // This MUST be in a finally block to ensure it's always called.
                synchronized (lock) {
                    activeWorkers.decrementAndGet();
                    // If this was the last active worker, we need to wake up other
                    // threads so they can see the crawl is over.
                    if (activeWorkers.get() == 0) {
                        lock.notifyAll();
                    }
                }
            }
        }
        System.out.println(Thread.currentThread().getName() + " shutting down.");
    }
}