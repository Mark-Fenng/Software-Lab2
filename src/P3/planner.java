package P3;

import java.util.*;
import java.util.regex.*;

import P1.graph.*;

public class planner implements RoutePlanner {
    private Graph<StopEvent> graph;
    private Set<Stop> stops;
    private store data = new store();
    private int maxWaitLimit;
    private Set<StopEvent> visited = new HashSet<>();
    private Map<StopEvent, StopEvent> trace = new HashMap<>();
    private Map<StopEvent, Integer> distance = new HashMap<>();
    private StopEvent startEvent;
    final int infinite = 0x00ffffff;

    planner(Graph<StopEvent> graph, Set<Stop> stops, store data, int maxWaitLimit) {
        this.graph = graph;
        this.stops = stops;
        this.data = data;
        this.maxWaitLimit = maxWaitLimit;
    }

    @Override
    public List<Stop> findStopsBySubstring(String search) {
        List<Stop> foundStops = new ArrayList<>();
        Pattern rule = Pattern.compile("*" + search + "*");
        Matcher matcher;
        for (Stop item : stops) {
            matcher = rule.matcher(item.getName());
            if (matcher.find())
                foundStops.add(item);
        }
        return foundStops;
    }

    @Override
    public Itinerary computeRoute(Stop src, Stop dest, int time) {
        StopEvent startEvent = data.getBuses(src).stream().filter(item -> item.getTime() - time <= maxWaitLimit && item.getTime() - time >= 0).min(Comparator.comparingInt(StopEvent::getTime)).orElse(null);
        if (startEvent == null) {
            System.out.println("the ride can't ride a bus in max wait time (" + maxWaitLimit + "s)");
        }
        dijkstra();
        Map.Entry<StopEvent, Integer> min = distance.entrySet().stream().filter(item -> item.getKey().getLocation().equals(dest)).min(Comparator.comparing(Map.Entry::getValue)).orElse(null);
        Itinerary trip = new Itinerary();
        if (min != null && min.getValue() != infinite) {
            StopEvent traceNode, buffer = null;
            while (!(traceNode = trace.get(min.getKey())).equals(startEvent)) {
                if (buffer != null) {
                    TripSegment segment;
                    if (buffer.getLocation().equals(traceNode.getLocation()))
                        segment = new WaitSegment(buffer, traceNode, buffer.getTime() - traceNode.getTime());
                    else
                        segment = new BusSegment(buffer, traceNode, buffer.getTime() - traceNode.getTime());
                    trip.add(segment);
                }
                buffer = traceNode;
            }
        }
        return trip;
    }

    private void dijkstra() {
        int sum;
        visited.add(startEvent);
        Map<StopEvent, Integer> targets = graph.targets(startEvent);
        Set<StopEvent> stops = graph.vertices();
        for (StopEvent stopEvent : stops) {
            distance.put(stopEvent, targets.keySet().contains(stopEvent) ? targets.get(stopEvent) : infinite);
            trace.put(stopEvent, stopEvent);
        }
        distance.put(startEvent, 0);
        for (int i = 1; i <= stops.size(); i++) {
            Map.Entry<StopEvent, Integer> min = distance.entrySet().stream().min(Comparator.comparing(Map.Entry::getValue)).orElse(null);
            if (min != null) {
                visited.add(min.getKey());
                targets = graph.targets(min.getKey());
                for (StopEvent stop : stops) {
                    if (!visited.contains(stop)) {
                        if (targets.keySet().contains(stop)) {
                            sum = distance.get(min.getKey()) + targets.get(stop);
                            if (sum < distance.get(stop)) {
                                distance.put(stop, sum);
                                trace.put(stop, min.getKey());
                            }
                        }
                    }
                }
            }
        }
    }
}
