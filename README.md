# Colombo Search Engine

## Description
The **Colombo Search Engine** project was developed as part of the course to become a Java Developer. It aims to create a search engine capable of "parsing" websites, primarily static HTML, and extracting the content of the `body` of each page. The content is analyzed, and for each word, the corresponding lemma is saved. The search engine exclusively supports texts in the Russian language. Users can search for information through the available graphical interface.

## Tools
- **Java**: v17
- **Maven**: v4.0.0
- **Spring Boot**: v2.7.1
- **Jsoup**: v1.15.3
- **Lemmatizator**: v1.5

## Features
- Indexing of all pages present on the sites specified in the configuration file.
- Indexing of a specific site.
- Searching on a specific site or on all previously indexed sites.

## Usage
The Maven project can be launched via an IDE or from the command line. After starting, you can access the graphical interface by visiting `http://localhost:8080` in your browser.

### Dashboard
The dashboard provides information about the number of indexed sites, pages, and total lemmas. For each site, the indexing status is indicated:

- **INDEXING**
- **INDEXED**
- **FAILED** 

<img src="src\main\resources\img\dashboard.PNG" alt="Dashboard" width="500"/>

In case of an error, an error message will also be displayed. All errors are logged in the file located in the `/logs` folder.

## Management
You can start global indexing or specific indexing for a page. Global indexing may take time, as all records are removed from the database before starting a new indexing.

<img src="src\main\resources\img\start_indexing.PNG" alt="Dashboard" width="500"/>

After starting the indexing, the button will change status to "stopIndex." Only at this point will it be possible to stop the indexing. The complete shutdown of all threads may take a few minutes.

<img src="src\main\resources\img\stop_indexing.PNG" alt="Dashboard" width="500"/>

For indexing a single page, enter the URL and press the **ADD/UPDATE** button.

<img src="src\main\resources\img\index_single_page.PNG" alt="Dashboard" width="500"/>

### Search
Select whether to search on a specific site or on all sites, enter the search terms, and press **search**. The results are sorted by relevance, calculated based on the presence of the searched words across various sites. Some words may be excluded from the search if their occurrence is below a percentage specified in the configuration file.

<img src="src\main\resources\img\search.PNG" alt="Dashboard" width="500"/>

## Configuration File
The configuration file contains settings for:

- Database connection;
- Logging;
- Search parameters (relevance, snippet length, distance between lemmas);
- Indexing settings (wait time between requests, number of attempts);
- List of sites to index.


```yaml
server:
  port: 8080

spring:
  datasource:
    username: root
    password: toor
    url: jdbc:mysql://localhost:3306/search_engine?useSSL=false&requireSSL=false&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=utf-8
  jpa:
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
    hibernate:
      ddl-auto: update
      show-sql: true

# Logging
logging.level.org.hibernate.SQL: DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder: TRACE

search-settings:
  lower-filter-score: 0.18 # relevance value below which the word is discarded from search results
  upper-filter-score: 1.00 # relevance value above which the word is discarded from search results
  max-snippet-length: 300  # average length for the snippet
  max-lemma-distance: 150  # maximum number of characters between one lemma and another in the same sentence
  context-length: 20       # number of characters before and after a lemma

indexing-settings:
  thread-sleep-time: 1
  number-of-attempts: 3
  sites:                  
    - url: https://www.cossa.ru/
      name: cossa.ru
    - url: https://habr.com/
      name: habr.com
```

