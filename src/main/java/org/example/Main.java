package org.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;

public class Main {
    private static final String BASE_URL = "https://leonbets.com";
    private static final List<String> SPORTS = List.of("football", "tennis", "hockey", "basketball");
    private static final ExecutorService executor = Executors.newFixedThreadPool(3);

    public static void main(String[] args) {
        List<CompletableFuture<Void>> futures = SPORTS.stream()
                .map(sport -> CompletableFuture.runAsync(() -> parseSport(sport), executor))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();
    }

    private static void parseSport(String sport) {
        try {
            String url = BASE_URL + "/category/sport/" + sport;
            Document doc = Jsoup.connect(url).get();
            Elements topLeagues = doc.select(".top-leagues a");

            for (Element league : topLeagues) {
                parseLeague(league.absUrl("href"), sport, league.text());
            }
        } catch (IOException e) {
            System.err.println("Ошибка загрузки спорта: " + sport);
        }
    }

    private static void parseLeague(String leagueUrl, String sport, String leagueName) {
        try {
            Document doc = Jsoup.connect(leagueUrl).get();
            Elements matches = doc.select(".match-card");

            for (int i = 0; i < Math.min(2, matches.size()); i++) {
                Element match = matches.get(i);
                String matchName = match.select(".match-name").text();
                String matchTime = match.select(".match-time").text();
                String matchId = match.attr("data-match-id");

                System.out.println(sport + ", " + leagueName);
                System.out.println(matchName + ", " + matchTime + ", " + matchId);
                parseMarkets(matchId);
            }
        } catch (IOException e) {
            System.err.println("Ошибка загрузки лиги: " + leagueName);
        }
    }

    private static void parseMarkets(String matchId) {
        try {
            String url = BASE_URL + "/api/match/" + matchId + "/markets";
            Document doc = Jsoup.connect(url).ignoreContentType(true).get();

            // Здесь нужно распарсить JSON (используйте Jackson или Gson)
            // Вывести рынки и исходы
        } catch (IOException e) {
            System.err.println("Ошибка загрузки рынков для матча: " + matchId);
        }
    }
}
