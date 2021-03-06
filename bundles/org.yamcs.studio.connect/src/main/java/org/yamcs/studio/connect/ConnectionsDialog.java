package org.yamcs.studio.connect;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.yamcs.client.ClientException;
import org.yamcs.client.YamcsClient;
import org.yamcs.studio.connect.YamcsConfiguration.AuthType;

/**
 * A modal dialog for managing connection to Yamcs servers. Extracted out of preferences, because these kind of settings
 * are a lot more variable and depending on the user configuration.
 */
public class ConnectionsDialog extends Dialog {

    private static final Logger log = Logger.getLogger(ConnectionsDialog.class.getName());
    private static final String ITEM_STANDARD = "Standard";
    private static final String ITEM_KERBEROS = "Kerberos";

    private TableViewer connViewer;
    private Composite detailPanel;

    private ToolItem addServerButton;
    private ToolItem removeServerButton;
    private Button testButton;

    private YamcsConfiguration selectedConfiguration;

    private Text yamcsInstanceText;
    private Text yamcsURLText;
    private Text commentText;

    private Label yamcsUserLabel;
    private Text yamcsUserText;
    private Label yamcsPasswordLabel;
    private Text yamcsPasswordText;

    private Button savePasswordButton;
    private Text caCertFileText;
    private Combo authTypeCombo;

    private YamcsConfiguration chosenConfiguration;

    public ConnectionsDialog(Shell parentShell) {
        super(parentShell);

        // Allow resize
        setShellStyle(getShellStyle() | SWT.RESIZE);
    }

    @Override
    public void create() {
        super.create();
        getShell().setSize(800, 500);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        parent.getShell().setText("Yamcs Server Connections");

        Composite contentArea = (Composite) super.createDialogArea(parent);
        GridLayout gl = new GridLayout();
        gl.marginHeight = 0;
        gl.verticalSpacing = 0;
        contentArea.setLayout(gl);

        ResourceManager resourceManager = new LocalResourceManager(JFaceResources.getResources(), contentArea);

        ToolBar editBar = new ToolBar(contentArea, SWT.NO_FOCUS);
        addServerButton = new ToolItem(editBar, SWT.NONE);
        addServerButton.setImage(resourceManager
                .createImage(getImageDescriptor(ConnectionsDialog.class, "icons/obj16/server_add.png")));
        addServerButton.setToolTipText("Add Connection");
        addServerButton.addListener(SWT.Selection, evt -> {
            addServer();
            updateState();
        });

        removeServerButton = new ToolItem(editBar, SWT.NONE);
        removeServerButton.setImage(resourceManager
                .createImage(getImageDescriptor(ConnectionsDialog.class, "icons/obj16/server_remove.png")));
        removeServerButton.setToolTipText("Delete Connection");
        removeServerButton.addListener(SWT.Selection, evt -> {
            removeSelectedServer();
            updateState();
        });
        editBar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        SashForm sash = new SashForm(contentArea, SWT.HORIZONTAL);
        sash.setLayoutData(new GridData(GridData.FILL_BOTH));
        sash.setLayout(new FillLayout());
        createServerPanel(sash, resourceManager);

        // Create right side, but wrap it in another composite to force
        // dimensions even when invisible
        Composite detailPanelWrapper = new Composite(sash, SWT.NONE);
        gl = new GridLayout();
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        detailPanelWrapper.setLayout(gl);
        createDetailPanel(detailPanelWrapper, resourceManager);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 200;
        detailPanel.setLayoutData(gd);

        sash.setWeights(new int[] { 70, 30 });

        ConnectionPreferences.getConnections().forEach(conf -> {
            connViewer.add(conf);
        });

        String lastId = ConnectionPreferences.getLastUsedConnection();
        if (lastId != null) {
            selectConnection(lastId);
        } else {
            selectFirstConnection();
        }
        updateState();

        return contentArea;
    }

    private void updateState() {
        IStructuredSelection sel = (IStructuredSelection) connViewer.getSelection();
        if (sel.isEmpty()) {
            selectedConfiguration = null;
            detailPanel.setVisible(false);
            removeServerButton.setEnabled(false);
            if (testButton != null) {
                testButton.setEnabled(false);
            }
        } else {
            selectedConfiguration = (YamcsConfiguration) sel.getFirstElement();
            detailPanel.setVisible(true);
            removeServerButton.setEnabled(true);
            if (testButton != null) {
                testButton.setEnabled(true);
            }

            if (selectedConfiguration.getAuthType() == null
                    || selectedConfiguration.getAuthType() == AuthType.STANDARD) {
                yamcsUserLabel.setVisible(true);
                yamcsUserText.setVisible(true);
                yamcsPasswordLabel.setVisible(true);
                yamcsPasswordText.setVisible(true);
                savePasswordButton.setVisible(true);
            } else {
                yamcsUserLabel.setVisible(false);
                yamcsUserText.setVisible(false);
                yamcsPasswordLabel.setVisible(false);
                yamcsPasswordText.setVisible(false);
                savePasswordButton.setVisible(false);
            }
        }
    }

    @Override
    protected void okPressed() {
        // TODO maybe only save changes when "save" button is used (and only for the active detail pane)
        saveChanges();
        super.okPressed();
    }

    private void saveChanges() {
        if (selectedConfiguration != null) {
            chosenConfiguration = selectedConfiguration;
        }
        List<YamcsConfiguration> confs = new ArrayList<>();
        Object el;
        int i = 0;
        while ((el = connViewer.getElementAt(i++)) != null) {
            YamcsConfiguration conf = (YamcsConfiguration) el;
            if (conf.isSavePassword()) {
                conf.savePassword();
            }
            confs.add(conf);
        }
        ConnectionPreferences.setConnections(confs);
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        parent.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Label filler = new Label(parent, SWT.NONE);
        filler.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        GridLayout layout = (GridLayout) parent.getLayout();
        layout.numColumns++;
        layout.makeColumnsEqualWidth = false;

        Button saveButton = createButton(parent, 123, "Save", false);
        saveButton.addListener(SWT.Selection, evt -> saveChanges());

        testButton = createButton(parent, 124, "Test", false);
        testButton.addListener(SWT.Selection, evt -> {
            if (selectedConfiguration != null) {
                testConnection(selectedConfiguration);
            }
        });
        testButton.setEnabled(!connViewer.getSelection().isEmpty());

        super.createButtonsForButtonBar(parent);

        Button ok = getButton(IDialogConstants.OK_ID);
        ok.setText("Connect");
        setButtonLayoutData(ok);
    }

    private void testConnection(YamcsConfiguration conf) {
        YamcsClient yamcsClient = null;
        try {
            YamcsClient.Builder clientBuilder = YamcsClient
                    .newBuilder(conf.getURL())
                    .withVerifyTls(false);

            if (conf.getCaCertFile() != null) {
                clientBuilder.withCaCertFile(Paths.get(conf.getCaCertFile()));
            }
            yamcsClient = clientBuilder.build();

            if (conf.getAuthType() == AuthType.KERBEROS) {
                yamcsClient.connectWithKerberos();
            } else if (conf.getUser() == null) {
                yamcsClient.connectAnonymously();
            } else {
                String password = conf.getTransientPassword();
                if (password != null && !password.isEmpty()) {
                    yamcsClient.connect(conf.getUser(), password.toCharArray());
                } else {
                    throw new ClientException("No password was provided");
                }
            }

            Display.getDefault().asyncExec(() -> {
                MessageDialog.openInformation(getShell(), "Connection OK", "Connection OK");
            });
        } catch (ClientException e) {
            Display.getDefault().asyncExec(() -> {
                MessageDialog.openError(getShell(), "Failed to connect", e.getMessage());
            });
        } finally {
            if (yamcsClient != null) {
                yamcsClient.close();
            }
        }
    }

    private void selectConnection(String id) {
        for (int i = 0; i < connViewer.getTable().getItemCount(); i++) {
            YamcsConfiguration configuration = (YamcsConfiguration) connViewer.getElementAt(i);
            if (configuration.getId().equals(id)) {
                connViewer.setSelection(new StructuredSelection(configuration), true);
                return;
            }
        }
    }

    private void selectFirstConnection() {
        if (connViewer.getTable().getItemCount() > 0) {
            connViewer.setSelection(new StructuredSelection(connViewer.getElementAt(0)), true);
        }
    }

    private void addServer() {
        YamcsConfiguration conf = new YamcsConfiguration();
        conf.setURL("http://localhost:8090");
        conf.setAuthType(AuthType.STANDARD);
        connViewer.add(conf);
        connViewer.setSelection(new StructuredSelection(conf), true);
        yamcsURLText.setFocus();
    }

    private void removeSelectedServer() {
        IStructuredSelection sel = (IStructuredSelection) connViewer.getSelection();
        YamcsConfiguration conf = (YamcsConfiguration) sel.getFirstElement();
        boolean confirmed = MessageDialog.openConfirm(connViewer.getTable().getShell(), "",
                "Do you really want to remove the server configuration '" + conf + "'?");
        if (confirmed) {
            connViewer.remove(conf);
            selectFirstConnection();
        }
    }

    private Composite createServerPanel(Composite parent, ResourceManager resourceManager) {
        Composite serverPanel = new Composite(parent, SWT.NONE);
        GridData gd = new GridData(GridData.FILL_BOTH);
        serverPanel.setLayoutData(gd);
        TableColumnLayout tcl = new TableColumnLayout();
        serverPanel.setLayout(tcl);

        Image serverImage = resourceManager
                .createImage(getImageDescriptor(ConnectionsDialog.class, "icons/obj16/server.gif"));

        connViewer = new TableViewer(serverPanel, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
        connViewer.getTable().setHeaderVisible(true);
        connViewer.getTable().setLinesVisible(true);

        TableViewerColumn urlColumn = new TableViewerColumn(connViewer, SWT.NONE);
        urlColumn.getColumn().setText("Server URL");
        urlColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public Image getImage(Object element) {
                return serverImage;
            }

            @Override
            public String getText(Object element) {
                YamcsConfiguration conf = (YamcsConfiguration) element;
                return conf.getURL();
            }
        });
        tcl.setColumnData(urlColumn.getColumn(), new ColumnPixelData(200));

        TableViewerColumn instanceColumn = new TableViewerColumn(connViewer, SWT.NONE);
        instanceColumn.getColumn().setText("Instance");
        instanceColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                YamcsConfiguration conf = (YamcsConfiguration) element;
                return conf.getInstance();
            }
        });
        tcl.setColumnData(instanceColumn.getColumn(), new ColumnPixelData(90));

        TableViewerColumn userColumn = new TableViewerColumn(connViewer, SWT.NONE);
        userColumn.getColumn().setText("User");
        userColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                YamcsConfiguration conf = (YamcsConfiguration) element;
                if (conf.getAuthType() != AuthType.KERBEROS) {
                    return conf.getUser();
                } else {
                    return null;
                }
            }
        });
        tcl.setColumnData(userColumn.getColumn(), new ColumnPixelData(90));

        TableViewerColumn nameColumn = new TableViewerColumn(connViewer, SWT.NONE);
        nameColumn.getColumn().setText("Comment");
        nameColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                YamcsConfiguration conf = (YamcsConfiguration) element;
                return conf.getComment();
            }
        });
        tcl.setColumnData(nameColumn.getColumn(), new ColumnPixelData(200));

        connViewer.setContentProvider(new ArrayContentProvider());
        connViewer.addSelectionChangedListener(evt -> {
            IStructuredSelection sel = (IStructuredSelection) evt.getSelection();
            if (sel.getFirstElement() != null) {
                YamcsConfiguration conf = (YamcsConfiguration) sel.getFirstElement();

                yamcsInstanceText.setText(forceString(conf.getInstance()));
                yamcsURLText.setText(forceString(conf.getURL()));
                /// caCertFileText.setText(forceString(conf.getCaCertFile()));
                commentText.setText(forceString(conf.getComment()));

                AuthType authType = (conf.getAuthType() == null) ? AuthType.STANDARD : conf.getAuthType();
                if (authType == AuthType.STANDARD) {
                    authTypeCombo.select(0);
                    yamcsUserText.setText(forceString(conf.getUser()));
                    yamcsPasswordText.setText(forceString(conf.getTransientPassword()));
                    savePasswordButton.setSelection(conf.isSavePassword());
                } else if (authType == AuthType.KERBEROS) {
                    authTypeCombo.select(1);
                    yamcsUserText.setText("");
                    yamcsPasswordText.setText("");
                    savePasswordButton.setSelection(false);
                } else {
                    throw new IllegalArgumentException("Unexpected auth type " + authType);
                }

                updateState();
            }
        });

        connViewer.setComparator(new ViewerComparator() {
            @Override
            public int compare(Viewer viewer, Object o1, Object o2) {
                YamcsConfiguration c1 = (YamcsConfiguration) o1;
                YamcsConfiguration c2 = (YamcsConfiguration) o2;
                if (c1.getURL() != null && c2.getURL() != null) {
                    return c1.getURL().compareTo(c2.getURL());
                } else {
                    return 0;
                }
            }
        });

        return serverPanel;
    }

    private Composite createDetailPanel(Composite parent, ResourceManager resourceManager) {
        detailPanel = new Composite(parent, SWT.NONE);
        GridLayout gl = new GridLayout(3, false);
        gl.marginWidth = 0;
        gl.horizontalSpacing = 0;
        detailPanel.setLayout(gl);

        Label lbl = new Label(detailPanel, SWT.NONE);
        lbl.setText("Server URL:");
        GridData gd = new GridData();
        gd.horizontalSpan = 3;
        lbl.setLayoutData(gd);
        yamcsURLText = new Text(detailPanel, SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 3;
        yamcsURLText.setLayoutData(gd);
        yamcsURLText.addListener(SWT.KeyUp, evt -> {
            if (!isBlank(yamcsURLText.getText()) && selectedConfiguration != null) {
                selectedConfiguration.setURL(yamcsURLText.getText());
            } else if (selectedConfiguration != null) {
                selectedConfiguration.setURL(null);
            }
        });

        lbl = new Label(detailPanel, SWT.NONE);
        lbl.setText("Instance:");
        gd = new GridData();
        gd.horizontalSpan = 3;
        lbl.setLayoutData(gd);
        yamcsInstanceText = new Text(detailPanel, SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 3;
        yamcsInstanceText.setLayoutData(gd);
        yamcsInstanceText.addListener(SWT.KeyUp, evt -> {
            if (!isBlank(yamcsInstanceText.getText()) && selectedConfiguration != null) {
                selectedConfiguration.setInstance(yamcsInstanceText.getText());
            } else if (selectedConfiguration != null) {
                selectedConfiguration.setInstance(null);
            }
        });

        /*lbl = new Label(detailPanel, SWT.NONE);
        lbl.setText("CA Certificate:");
        caCertFileText = new Text(detailPanel, SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        caCertFileText.setLayoutData(gd);
        caCertFileText.addListener(SWT.KeyUp, evt -> {
            if (!isBlank(caCertFileText.getText()) && selectedConfiguration != null) {
                // selectedConfiguration.setUser(caCertFileText.getText());
            } else if (selectedConfiguration != null) {
                // selectedConfiguration.setUser(null);
            }
        });*/

        // Spacer
        lbl = new Label(detailPanel, SWT.NONE);
        gd = new GridData();
        gd.horizontalSpan = 3;
        lbl.setLayoutData(gd);

        lbl = new Label(detailPanel, SWT.NONE);
        lbl.setText("Authentication:");
        gd = new GridData();
        gd.horizontalSpan = 3;
        lbl.setLayoutData(gd);

        lbl = new Label(detailPanel, SWT.NONE);
        lbl.setText("Type:");
        authTypeCombo = new Combo(detailPanel, SWT.READ_ONLY);
        authTypeCombo.setItems(ITEM_STANDARD, ITEM_KERBEROS);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        authTypeCombo.setLayoutData(gd);
        authTypeCombo.addListener(SWT.Selection, evt -> {
            if (authTypeCombo.getSelectionIndex() == 0) {
                selectedConfiguration.setAuthType(AuthType.STANDARD);
            } else if (authTypeCombo.getSelectionIndex() == 1) {
                selectedConfiguration.setAuthType(AuthType.KERBEROS);
                selectedConfiguration.setUser(null);
                yamcsUserText.setText("");
                selectedConfiguration.setTransientPassword(null);
                yamcsPasswordText.setText("");
                selectedConfiguration.setSavePassword(false);
                savePasswordButton.setSelection(false);
            } else {
                throw new IllegalArgumentException("Unexpected auth type " + authTypeCombo.getSelectionIndex());
            }
            updateState();
        });

        yamcsUserLabel = new Label(detailPanel, SWT.NONE);
        yamcsUserLabel.setText("User:");
        yamcsUserText = new Text(detailPanel, SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        yamcsUserText.setLayoutData(gd);
        yamcsUserText.addListener(SWT.KeyUp, evt -> {
            if (!isBlank(yamcsUserText.getText()) && selectedConfiguration != null) {
                selectedConfiguration.setUser(yamcsUserText.getText());
            } else if (selectedConfiguration != null) {
                selectedConfiguration.setUser(null);
            }
        });

        yamcsPasswordLabel = new Label(detailPanel, SWT.NONE);
        yamcsPasswordLabel.setText("Password:");
        yamcsPasswordText = new Text(detailPanel, SWT.BORDER | SWT.PASSWORD);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        yamcsPasswordText.setLayoutData(gd);
        yamcsPasswordText.addListener(SWT.KeyUp, evt -> {
            if (!isBlank(yamcsPasswordText.getText()) && selectedConfiguration != null) {
                selectedConfiguration.setTransientPassword(yamcsPasswordText.getText());
            } else if (selectedConfiguration != null) {
                selectedConfiguration.setTransientPassword(null);
            }
        });

        lbl = new Label(detailPanel, SWT.NONE);
        savePasswordButton = new Button(detailPanel, SWT.CHECK);
        savePasswordButton.setText("Save Password");
        gd = new GridData();
        gd.horizontalSpan = 2;
        savePasswordButton.setLayoutData(gd);
        savePasswordButton.addListener(SWT.Selection, evt -> {
            selectedConfiguration.setSavePassword(savePasswordButton.getSelection());
        });

        // Spacer
        lbl = new Label(detailPanel, SWT.NONE);
        gd = new GridData();
        gd.horizontalSpan = 3;
        lbl.setLayoutData(gd);

        lbl = new Label(detailPanel, SWT.NONE);
        lbl.setText("Comment:");
        gd = new GridData();
        gd.horizontalSpan = 3;
        lbl.setLayoutData(gd);

        commentText = new Text(detailPanel, SWT.BORDER | SWT.MULTI);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        gd.heightHint = 64;
        commentText.setLayoutData(gd);
        // Update the label in the left panel too
        commentText.addListener(SWT.KeyUp, evt -> {
            IStructuredSelection sel = (IStructuredSelection) connViewer.getSelection();
            YamcsConfiguration conf = (YamcsConfiguration) sel.getFirstElement();
            conf.setComment(commentText.getText());
            connViewer.update(conf, null);

            if (!isBlank(commentText.getText()) && selectedConfiguration != null) {
                log.fine("Storing name " + commentText.getText());
                selectedConfiguration.setComment(commentText.getText());
            } else if (selectedConfiguration != null) {
                selectedConfiguration.setComment(null);
            }
        });

        return detailPanel;
    }

    public YamcsConfiguration getChosenConfiguration() {
        return chosenConfiguration;
    }

    public static ImageDescriptor getImageDescriptor(Class<?> classFromBundle, String path) {
        Bundle bundle = FrameworkUtil.getBundle(classFromBundle);
        return ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path(path), null));
    }

    private static boolean isBlank(String string) {
        return string == null || "".equals(string);
    }

    private static String forceString(Object obj) {
        return (obj != null) ? obj.toString() : "";
    }
}
