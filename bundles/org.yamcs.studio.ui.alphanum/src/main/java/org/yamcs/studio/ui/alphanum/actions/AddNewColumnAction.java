package org.yamcs.studio.ui.alphanum.actions;

import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.yamcs.protobuf.Mdb.ParameterInfo;
import org.yamcs.studio.core.model.ParameterCatalogue;
import org.yamcs.studio.ui.alphanum.AddParameterWizard;
import org.yamcs.studio.ui.alphanum.ParameterTableViewer;
import org.yamcs.studio.ui.alphanum.ScrollParameterTableViewer;

public class AddNewColumnAction extends AlphaNumericAction {

    
    private Listener listener;
        
    public AddNewColumnAction(ScrollParameterTableViewer viewer) {
        super( "icons/elcl16/add.png", viewer);
        setToolTipText("Add Column");
        

        
    }

    @Override
    public void run() {
        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        
        AddParameterWizard wizard = new AddParameterWizard();
        WizardDialog dialog = new WizardDialog(shell, wizard);
        if (dialog.open() == Window.OK)
            for(ParameterInfo info : wizard.getParameter())
                getScrollViewer().addParameter(info);

    }
    
    @Override
    public void setScrollViewer(ScrollParameterTableViewer viewer) {
        if(viewer != null)
            viewer.addDataChangedListener(listener);
        super.setScrollViewer(viewer);
    }

}
