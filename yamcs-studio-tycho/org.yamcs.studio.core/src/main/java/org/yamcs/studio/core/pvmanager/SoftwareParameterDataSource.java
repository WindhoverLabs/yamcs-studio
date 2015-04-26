package org.yamcs.studio.core.pvmanager;

import org.epics.pvmanager.ChannelHandler;
import org.epics.pvmanager.DataSource;
import org.yamcs.studio.core.WebSocketRegistrar;
import org.yamcs.studio.core.YamcsPlugin;

/**
 * When running the OPIbuilder this is instantiated for every parameter separately.
 */
public class SoftwareParameterDataSource extends DataSource {

    private static WebSocketRegistrar webSocketClient;

    public SoftwareParameterDataSource() {
        super(true /* writable */);
        webSocketClient = YamcsPlugin.getDefault().getWebSocketClient();
    }

    @Override
    protected ChannelHandler createChannel(String channelName) {
        return new SoftwareParameterChannelHandler(channelName, webSocketClient);
    }
}