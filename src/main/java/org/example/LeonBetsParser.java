package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.models.markets.Market;
import org.example.models.markets.MarketResponse;
import org.example.models.markets.Runner;
import org.example.models.sports.League;
import org.example.models.matches.MatchEvent;
import org.example.models.matches.MatchResponse;
import org.example.models.sports.Sport;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

public class LeonBetsParser {
    private static final Set<String> ALLOWED_FAMILIES = Set.of("Soccer", "IceHockey", "Tennis", "Basketball");
    private static final String SPORTS_URL = "https://leonbets.com/api-2/betline/sports?ctag=en-US&flags=urlv2";
    private static final String MATCHES_URL = "https://leonbets.com/api-2/betline/events/all?ctag=en-US&league_id=%s&hideClosed=true&flags=reg,urlv2,mm2,rrc,nodup";
    private static final String MARKETS_URL = "https://leonbets.com/api-2/betline/event/all?ctag=en-US&eventId=%s&flags=reg,urlv2,mm2,rrc,nodup,smgv2,outv2";
    private static final String DATE_FORMATTER = "yyyy-MM-dd HH:mm:ss 'UTC'";

    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ExecutorService executor = Executors.newFixedThreadPool(3);
    private static final Map<League, Map<MatchEvent, List<Market>>> dataStore = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        processSportsData().join();
        displayData();
        executor.shutdown();
    }

    private static CompletableFuture<Void> processSportsData() {
        return fetchSportsData()
                .thenApplyAsync(LeonBetsParser::parseSportsJson, executor)
                .thenComposeAsync(LeonBetsParser::filterAndProcessSportsData, executor)
                .exceptionally(e -> { e.printStackTrace(); return null; });
    }

    private static CompletableFuture<String> fetchSportsData() {
        return sendRequest(SPORTS_URL);
    }

    private static List<Sport> parseSportsJson(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, mapper.getTypeFactory().constructCollectionType(List.class, Sport.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static CompletableFuture<Void> filterAndProcessSportsData(List<Sport> sports) {
        List<CompletableFuture<Void>> futures = sports.stream()
                .filter(sport -> ALLOWED_FAMILIES.contains(sport.getFamily()))
                .flatMap(sport -> sport.getRegions().stream()
                    .flatMap(region -> region.getLeagues().stream()
                            .filter(League::isTop)
                            .peek(league -> {
                                String updatedLeagueName = sport.getName() + ", " + region.getName() + " " + league.getName();
                                league.setName(updatedLeagueName);
                            })
                            .map(LeonBetsParser::fetchAndProcessMatchesForLeague)
                    )
                )
                .toList();
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    private static CompletableFuture<Void> fetchAndProcessMatchesForLeague(League league) {
        return fetchMatchesForLeague(league.getId())
                .thenApplyAsync(LeonBetsParser::parseMatchesJson, executor)
                .thenComposeAsync(matches -> processMatches(league, matches), executor);
    }

    private static CompletableFuture<String> fetchMatchesForLeague(long leagueId) {
        return sendRequest(MATCHES_URL.formatted(leagueId));
    }

    private static List<MatchEvent> parseMatchesJson(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            MatchResponse response = mapper.readValue(json, MatchResponse.class);
            return response.getEvents();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static CompletableFuture<Void> processMatches(League league, List<MatchEvent> matches) {
        dataStore.putIfAbsent(league, new ConcurrentHashMap<>());
        List<CompletableFuture<Void>> futures = matches.stream().limit(2)
                .map(event -> fetchAndProcessMarketsForEvent(league, event))
                .toList();
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    private static CompletableFuture<Void> fetchAndProcessMarketsForEvent(League league, MatchEvent event) {
        return fetchMarketsForEvent(event.getId())
                .thenApplyAsync(LeonBetsParser::parseMarketsJson, executor)
                .thenAcceptAsync(markets -> dataStore.get(league).put(event, markets), executor);
    }

    private static CompletableFuture<String> fetchMarketsForEvent(long eventId) {
        return sendRequest(MARKETS_URL.formatted(eventId));
    }

    private static List<Market> parseMarketsJson(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            MarketResponse response = mapper.readValue(json, MarketResponse.class);
            return response.getMarkets();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void displayData() {
        for (var entry : dataStore.entrySet()) {
            League league = entry.getKey();
            System.out.println(league.getName());
            for (var matchEntry : entry.getValue().entrySet()) {
                MatchEvent event = matchEntry.getKey();
                System.out.println("  " + event.getName() + ", " + formatTime(event.getKickoff()) + ", " + event.getId());
                for (Market market : matchEntry.getValue()) {
                    System.out.println("    " + market.getName());
                    for (Runner runner : market.getRunners()) {
                        System.out.println("      " + runner.getName() + ", " + runner.getPrice() + ", " + runner.getId());
                    }
                }
            }
        }
    }

    private static CompletableFuture<String> sendRequest(String url) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return response.body();
                } else {
                    throw new RuntimeException("HTTP Error: " + response.statusCode());
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }

    private static String formatTime(long timestamp) {
        Instant instant = Instant.ofEpochMilli(timestamp);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMATTER).withZone(ZoneOffset.UTC);
        return formatter.format(instant);
    }
}
