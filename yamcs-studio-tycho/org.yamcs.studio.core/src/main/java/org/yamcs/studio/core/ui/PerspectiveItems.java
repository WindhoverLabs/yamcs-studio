package org.yamcs.studio.core.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.csstudio.ui.util.CustomMediaFactory;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.handlers.RadioState;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.yamcs.studio.core.application.IDs;

public class PerspectiveItems extends CompoundContributionItem {

    public static final String OPEN_PERSPECTIVE_COMMAND = "org.yamcs.studio.core.ui.commands.openPerspectiveCommand";
    private static final List<String> SUPPORTED_PERSPECTIVES = Arrays.asList(IDs.OPI_EDITOR_PERSPECTIVE, IDs.OPI_RUNTIME_PERSPECTIVE);

    // The runtime perspective image is identical to the builder in our current cs-studio dependencies, so override.
    private static ImageDescriptor OPI_RUNTIME_IMAGE = CustomMediaFactory.getInstance()
            .getImageDescriptorFromPlugin(IDs.OPI_RUNTIME_PERSPECTIVE, "icons/OPIRunner.png"); //$NON-NLS-1$

    @Override
    public IContributionItem[] getContributionItems() {
        IWorkbench workbench = PlatformUI.getWorkbench();
        List<IContributionItem> items = new ArrayList<>();
        for (IPerspectiveDescriptor perspective : workbench.getPerspectiveRegistry().getPerspectives()) {
            if (SUPPORTED_PERSPECTIVES.contains(perspective.getId())) {
                CommandContributionItem item = createProfileItem(perspective);
                item.setVisible(true);
                items.add(item);
            }
        }
        updateSelection();
        return items.toArray(new IContributionItem[0]);
    }

    private CommandContributionItem createProfileItem(IPerspectiveDescriptor perspective) {
        CommandContributionItemParameter itemParameter = new CommandContributionItemParameter(
                PlatformUI.getWorkbench().getActiveWorkbenchWindow(),
                null,
                OPEN_PERSPECTIVE_COMMAND,
                CommandContributionItem.STYLE_RADIO);

        HashMap<String, String> params = new HashMap<String, String>();
        params.put(RadioState.PARAMETER_ID, perspective.getId());

        itemParameter.label = perspective.getLabel();
        if (perspective.getId().equals(IDs.OPI_RUNTIME_PERSPECTIVE))
            itemParameter.icon = OPI_RUNTIME_IMAGE;
        else
            itemParameter.icon = perspective.getImageDescriptor();
        itemParameter.tooltip = perspective.getDescription();
        itemParameter.parameters = params;

        return new CommandContributionItem(itemParameter);
    }

    private void updateSelection() {
        IWorkbench workbench = PlatformUI.getWorkbench();
        ICommandService commandService = (ICommandService) workbench.getService(ICommandService.class);
        Command command = commandService.getCommand(OPEN_PERSPECTIVE_COMMAND);
        try {
            IPerspectiveDescriptor perspective = getActivePerspective(workbench);
            HandlerUtil.updateRadioState(command, perspective.getId());
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    private IPerspectiveDescriptor getActivePerspective(IWorkbench workbench) {
        return workbench.getActiveWorkbenchWindow().getActivePage().getPerspective();
    }
}