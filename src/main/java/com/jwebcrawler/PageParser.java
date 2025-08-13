package com.jwebcrawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PageParser {

    public List<String> parse(String html, String baseUrl) {
        if (html == null || html.isEmpty()) {
            return new ArrayList<>();
        }

        Document doc = Jsoup.parse(html, baseUrl);
        Elements linksOnPage = doc.select("a[href]");

        return linksOnPage.stream()
                .map(link -> link.attr("abs:href"))
                .filter(link -> !link.isEmpty())
                .collect(Collectors.toList());
    }
}