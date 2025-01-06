package example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import example.SubscriberOuterClass.Subscriber; // Protobuf ile üretilen sınıfı import edin

public class SharedResources {
    private static final List<Subscriber> globalList = Collections.synchronizedList(new ArrayList<>());

    public static List<Subscriber> getGlobalList() {
            return globalList;
    }
    private static final List<Subscriber> level1List = Collections.synchronizedList(new ArrayList<>());

    public static List<Subscriber> getLevel1List() {
        return level1List;
    }
    public static final List<Subscriber> sharedSubscribers =
            Collections.synchronizedList(new ArrayList<Subscriber>());
    public static final int fault_tolerance_level = 2;
}
