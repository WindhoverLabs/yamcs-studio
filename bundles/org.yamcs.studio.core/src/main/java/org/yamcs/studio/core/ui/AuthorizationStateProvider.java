package org.yamcs.studio.core.ui;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.AbstractSourceProvider;
import org.eclipse.ui.ISources;
import org.yamcs.studio.core.Privileges;
import org.yamcs.studio.core.YamcsAware;
import org.yamcs.studio.core.YamcsPlugin;

/**
 * Used in plugin.xml core-expressions to keep track of connection state
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class AuthorizationStateProvider extends AbstractSourceProvider implements YamcsAware {

    private static final Logger log = Logger.getLogger(AuthorizationStateProvider.class.getName());

    public static final String STATE_KEY_MAY_COMMAND_PAYLOAD = "org.yamcs.studio.ui.authorization.mayCommandPayload";
    private static final String[] SOURCE_NAMES = { STATE_KEY_MAY_COMMAND_PAYLOAD };

    public AuthorizationStateProvider() {
        YamcsPlugin.addListener(this);
    }

    @Override
    public Map getCurrentState() {
        Map map = new HashMap(1);
        map.put(STATE_KEY_MAY_COMMAND_PAYLOAD,
                YamcsPlugin.hasSystemPrivilege(Privileges.Command));
        return map;
    }

    @Override
    public String[] getProvidedSourceNames() {
        return SOURCE_NAMES;
    }

    @Override
    public void onYamcsConnected() {
        Display.getDefault().asyncExec(() -> {
            Map newState = getCurrentState();
            log.fine(String.format("Fire new authz state %s", newState));
            fireSourceChanged(ISources.WORKBENCH, newState);
        });
    }

    @Override
    public void onYamcsDisconnected() {
        Display.getDefault().asyncExec(() -> {
            Map newState = getCurrentState();
            log.fine(String.format("Fire new authz state %s", newState));
            fireSourceChanged(ISources.WORKBENCH, newState);
        });
    }

    @Override
    public void dispose() {
        YamcsPlugin.removeListener(this);
    }
}
