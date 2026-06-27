package com.finshield.backend.aml.matching;

import com.finshield.backend.aml.domain.AmlMatchType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Service
public class AmlNameMatchingService {

    private final double fuzzyThreshold;

    public AmlNameMatchingService(
            @Value("${finshield.aml.screening.fuzzy-threshold:0.88}") double fuzzyThreshold
    ) {
        if (fuzzyThreshold <= 0 || fuzzyThreshold > 1) {
            throw new IllegalArgumentException("AML fuzzy threshold must be greater than 0 and at most 1");
        }
        this.fuzzyThreshold = fuzzyThreshold;
    }

    public NameMatch match(String subjectName, String watchlistName) {
        String subject = NameNormalizer.normalize(subjectName);
        String watchlist = NameNormalizer.normalize(watchlistName);
        if (subject.isBlank() || watchlist.isBlank()) {
            return noMatch();
        }
        if (subject.equals(watchlist)) {
            return new NameMatch(true, AmlMatchType.EXACT, new BigDecimal("100.00"));
        }

        double similarity = Math.max(
                jaroWinkler(subject, watchlist),
                tokenJaccard(subject, watchlist)
        );
        double effectiveThreshold = Math.min(subject.length(), watchlist.length()) < 5
                ? Math.max(fuzzyThreshold, 0.95)
                : fuzzyThreshold;
        BigDecimal score = BigDecimal.valueOf(similarity * 100)
                .setScale(2, RoundingMode.HALF_UP);
        return similarity >= effectiveThreshold
                ? new NameMatch(true, AmlMatchType.FUZZY, score)
                : new NameMatch(false, AmlMatchType.NONE, score);
    }

    private NameMatch noMatch() {
        return new NameMatch(false, AmlMatchType.NONE, BigDecimal.ZERO.setScale(2));
    }

    private double tokenJaccard(String first, String second) {
        Set<String> firstTokens = new HashSet<>(Arrays.asList(first.split(" ")));
        Set<String> secondTokens = new HashSet<>(Arrays.asList(second.split(" ")));
        Set<String> intersection = new HashSet<>(firstTokens);
        intersection.retainAll(secondTokens);
        Set<String> union = new HashSet<>(firstTokens);
        union.addAll(secondTokens);
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    private double jaroWinkler(String first, String second) {
        if (first.equals(second)) {
            return 1.0;
        }
        int matchDistance = Math.max(0, Math.max(first.length(), second.length()) / 2 - 1);
        boolean[] firstMatches = new boolean[first.length()];
        boolean[] secondMatches = new boolean[second.length()];
        int matches = 0;

        for (int firstIndex = 0; firstIndex < first.length(); firstIndex++) {
            int start = Math.max(0, firstIndex - matchDistance);
            int end = Math.min(firstIndex + matchDistance + 1, second.length());
            for (int secondIndex = start; secondIndex < end; secondIndex++) {
                if (!secondMatches[secondIndex]
                        && first.charAt(firstIndex) == second.charAt(secondIndex)) {
                    firstMatches[firstIndex] = true;
                    secondMatches[secondIndex] = true;
                    matches++;
                    break;
                }
            }
        }
        if (matches == 0) {
            return 0.0;
        }

        int transpositions = 0;
        int secondIndex = 0;
        for (int firstIndex = 0; firstIndex < first.length(); firstIndex++) {
            if (!firstMatches[firstIndex]) {
                continue;
            }
            while (!secondMatches[secondIndex]) {
                secondIndex++;
            }
            if (first.charAt(firstIndex) != second.charAt(secondIndex)) {
                transpositions++;
            }
            secondIndex++;
        }

        double matchCount = matches;
        double jaro = (matchCount / first.length()
                + matchCount / second.length()
                + (matchCount - transpositions / 2.0) / matchCount) / 3.0;
        int prefixLength = 0;
        int maxPrefix = Math.min(4, Math.min(first.length(), second.length()));
        while (prefixLength < maxPrefix
                && first.charAt(prefixLength) == second.charAt(prefixLength)) {
            prefixLength++;
        }
        return jaro + prefixLength * 0.1 * (1 - jaro);
    }
}
