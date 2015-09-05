package org.yamcs.studio.core.model;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.yamcs.api.ws.WebSocketRequest;
import org.yamcs.protobuf.Alarms.Alarm;
import org.yamcs.studio.core.ConnectionManager;
import org.yamcs.studio.core.YamcsPlugin;
import org.yamcs.studio.core.web.WebSocketRegistrar;

public class AlarmCatalogue implements Catalogue {

    private Set<AlarmListener> alarmListeners = new CopyOnWriteArraySet<>();

    public static AlarmCatalogue getInstance() {
        return YamcsPlugin.getDefault().getCatalogue(AlarmCatalogue.class);
    }

    public void addAlarmListener(AlarmListener listener) {
        alarmListeners.add(listener);
    }

    @Override
    public void onStudioConnect() {
        WebSocketRegistrar webSocketClient = ConnectionManager.getInstance().getWebSocketClient();
        webSocketClient.sendMessage(new WebSocketRequest("alarms", "subscribe"));
    }

    public void processAlarm(Alarm alarm) {
        alarmListeners.forEach(l -> l.processAlarm(alarm));
    }

    @Override
    public void onStudioDisconnect() {
    }
}
