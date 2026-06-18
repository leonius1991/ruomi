package fi.newdoska.doska.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class EstonianBorderQueueParser {

    public record LaneReading(String checkpoint, String lane, String elementId) {}

    public record ParsedQueueData(Map<String, Integer> liveByLane, int koidulaTotal, int luhamaaTotal) {}

    public static final List<LaneReading> LANES = List.of(
            new LaneReading("KOIDULA", "A/B", "nvl-4"),
            new LaneReading("KOIDULA", "BC", "nvl-12"),
            new LaneReading("KOIDULA", "C", "nvl-5"),
            new LaneReading("KOIDULA", "CE", "nvl-14"),
            new LaneReading("KOIDULA", "D", "nvl-6"),
            new LaneReading("LUHAMAA", "A/B", "nvl-7"),
            new LaneReading("LUHAMAA", "BC", "nvl-11"),
            new LaneReading("LUHAMAA", "C", "nvl-8"),
            new LaneReading("LUHAMAA", "CE", "nvl-15"),
            new LaneReading("LUHAMAA", "D", "nvl-9")
    );

    @Value("${border.queue.source-url:https://www.estonianborder.eu/yphis/borderQueueInfo.action?request_locale=en}")
    private String sourceUrl;

    public String getSourceUrl() {
        return sourceUrl;
    }

    public ParsedQueueData parse(String html) {
        Map<String, Integer> liveByLane = new LinkedHashMap<>();
        int koidulaTotal = 0;
        int luhamaaTotal = 0;

        for (LaneReading lane : LANES) {
            Integer count = extractCount(html, lane.elementId());
            if (count != null) {
                String key = lane.checkpoint() + ":" + lane.lane();
                liveByLane.put(key, count);
                if ("KOIDULA".equals(lane.checkpoint())) {
                    koidulaTotal += count;
                } else {
                    luhamaaTotal += count;
                }
            }
        }

        return new ParsedQueueData(liveByLane, koidulaTotal, luhamaaTotal);
    }

    private Integer extractCount(String html, String elementId) {
        Pattern pattern = Pattern.compile(
                "id=\"" + Pattern.quote(elementId) + "\"[^>]*>\\s*(\\d+|N/A)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(html);
        if (!matcher.find()) {
            log.warn("Не найден элемент очереди: {}", elementId);
            return null;
        }
        String value = matcher.group(1).trim();
        if ("N/A".equalsIgnoreCase(value)) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("Некорректное значение очереди для {}: {}", elementId, value);
            return null;
        }
    }
}
