package com.jwebcrawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PageParser {

    /**
     * Parses HTML content to extract all valid hyperlinks.
     * @param html The HTML content of the page.
     * @param baseUrl The URL of the page, used to resolve relative links.
     * @return A list of absolute URL strings found on the page.
     */
    public List<String> parse(String html, String baseUrl) {
        if (html == null || html.isEmpty()) {
            return new ArrayList<>(); // Return an empty list if there's no HTML
        }

        // Use JSoup to parse the HTML string.
        Document doc = Jsoup.parse(html, baseUrl);

        // Select all anchor tags (<a href="...">)
        Elements linksOnPage = doc.select("a[href]");

        // Map the Element objects to their absolute URL strings.
        return linksOnPage.stream()
                .map(link -> link.attr("abs:href")) // "abs:href" is a JSoup feature to get the absolute URL
                .filter(link -> !link.isEmpty()) // Filter out any empty links
                .collect(Collectors.toList());
    }
}