package org.yamcs.studio.editor.actions;

import org.csstudio.platform.workspace.WorkspaceInfo;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.yamcs.studio.editor.SelectWorkspaceDialog;
import org.yamcs.studio.editor.YamcsStudioWorkspace;

//See org.eclipse.ui.internal.ide.actions.OpenWorkspaceAction
public class SwitchWorkspaceAction extends Action implements IWorkbenchAction {

    private IWorkbenchWindow window;

    public SwitchWorkspaceAction(IWorkbenchWindow window) {
        super("Switch &Workspace...");

        this.window = window;
    }

    @Override
    public void run() {
        String path = promptForWorkspace();
        if (path == null) {
            return;
        }

        restart(path);
    }

    private void restart(String path) {
        String commandline = buildCommandLine(path);
        System.setProperty("eclipse.exitcode", IApplication.EXIT_RELAUNCH.toString());
        System.setProperty("eclipse.exitdata", commandline);
        window.getWorkbench().restart();
    }

    private String promptForWorkspace() {
        WorkspaceInfo workspaceInfo = new WorkspaceInfo(Platform.getInstanceLocation().getURL(), false);
        String[] recentWorkspaces = YamcsStudioWorkspace.getRecentWorkspaces(workspaceInfo);
        SelectWorkspaceDialog dialog = new SelectWorkspaceDialog(recentWorkspaces);
        if (dialog.open() == SelectWorkspaceDialog.OK) {
            String selectedWorkspace = dialog.getSelectedWorkspace();
            workspaceInfo.setSelectedWorkspace(selectedWorkspace);
            workspaceInfo.writePersistedData();
            return workspaceInfo.getSelectedWorkspace();
        } else {
            return null;
        }
    }

    /**
     * Create a command line that will launch a new workbench that is the same as the currently running one, but using
     * the argument directory as its workspace.
     *
     * @param workspace
     *            Directory to use as the new workspace
     * @return New command line or <code>null</code> on error
     */
    private String buildCommandLine(String workspace) {
        String property = System.getProperty("eclipse.vm");
        if (property == null) {
            MessageDialog.openError(null,
                    "Error",
                    "Cannot determine virtual machine, need 'eclipse.vm ...' command-line argument\n"
                            + "Workspace switch does not work when started from within IDE!");
            return null;
        }

        final StringBuffer buf = new StringBuffer(512);
        buf.append(property);
        buf.append("\n");

        // append the vmargs and commands. Assume that these already end in \n
        final String vmargs = System.getProperty("eclipse.vmargs");
        if (vmargs != null)
            buf.append(vmargs);

        // append the rest of the args, replacing or adding -data as required
        property = System.getProperty("eclipse.commands");
        if (property == null) {
            buf.append("-data");
            buf.append("\n");
            buf.append(workspace);
            buf.append("\n");
        } else {
            // find the index of the arg to replace its value
            int cmd_data_pos = property.lastIndexOf("-data");
            if (cmd_data_pos != -1) {
                cmd_data_pos += "-data".length() + 1;
                buf.append(property.substring(0, cmd_data_pos));
                buf.append(workspace);
                buf.append(property.substring(property.indexOf('\n',
                        cmd_data_pos)));
            } else {
                buf.append("-data");
                buf.append("\n");
                buf.append(workspace);
                buf.append("\n");
                buf.append(property);
            }
        }

        // put the vmargs back at the very end (the eclipse.commands property
        // already contains the -vm arg)
        if (vmargs != null) {
            buf.append("-vmargs");
            buf.append("\n");
            buf.append(vmargs);
        }

        return buf.toString();
    }

    @Override
    public void dispose() {
        window = null;
    }
}