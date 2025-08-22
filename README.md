# JWebCrawler

A lightweight multithreaded web crawler written in Java, built with Maven.  
It respects `robots.txt`, parses pages with jsoup, and saves results (URL, Title, Description) into a CSV file.

---

##  Features
- Starts crawling from a seed URL and explores links recursively.  
- Uses a thread pool to run multiple crawler workers concurrently.  
- Respects robots.txt rules (per domain).  
- Extracts page title** and meta description (or first paragraph as fallback).  
- Writes results to `results.csv` in proper CSV format.  
- Thread-safe data structures (concurrent queue, set, atomic counters).  
- Graceful shutdown once all workers are idle and the frontier is empty.  

---

## Tech Stack
- **Java 21**  
- **Maven** (build + dependency management)  
- **Jsoup** (HTML parsing)  
- **Concurrent Java utilities** (thread pool, concurrent collections)  

---

## Project Structure

src/
  -└── main/
  -└── java/
  -└── com/
  -└── jwebcrawler/
  ├── CrawlData.java # Record holding (URL, Title, Description)
  ├── CrawlerApp.java # Main entrypoint, sets up crawling
  ├── CrawlerWorker.java # Worker threads that download & parse pages
  ├── DataWriter.java # Thread-safe CSV writer
  ├── PageDownloader.java # Handles HTTP requests
  ├── PageParser.java # Extracts links from HTML
  └── RobotsTxtManager.java # Fetches & parses robots.txt rules

  
---

## Setup & Run

### 1. Clone the repo
```bash
git clone https://github.com/yourusername/JWebCrawler.git
cd JWebCrawler
```
### 2. Build with Maven
```bash
mvn clean install
```
### 3. Run the crawler
```bash
mvn exec:java -Dexec.mainClass="com.jwebcrawler.CrawlerApp"
```
By default, it starts crawling from https://jsoup.org/ and saves results to results.csv.

---

## Output
Results are saved in CSV format:
- "URL","Title","Description"
- "https://jsoup.org/","jsoup Java HTML Parser","jsoup is a Java library for working with real-world HTML."
...

---

## Configuration
You can tweak these values in CrawlerApp.java:

  - NUM_THREADS → Number of worker threads (default: 10)

  - MAX_PAGES_TO_CRAWL → Crawl limit (default: 1300)

  - Seed URL → Passed to startCrawling() in main()

---

## Robots.txt Compliance
Each domain’s robots.txt is fetched and cached.

The crawler follows rules for:

  - User-agent: *

  - User-agent: JWebCrawler/1.0

  - Disallowed paths are strictly avoided.


