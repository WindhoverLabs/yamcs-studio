package org.yamcs.studio.core.commanding.cmdstack;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.yamcs.protobuf.Commanding.CommandHistoryEntry;
import org.yamcs.protobuf.Commanding.CommandId;
import org.yamcs.studio.core.commanding.cmdhist.CommandHistoryRecord;

public class CommandStackViewerContentProvider implements IStructuredContentProvider {

    private Map<CommandId, CommandHistoryRecord> recordsByCommandId = new LinkedHashMap<>();
    private TableViewer tableViewer;

    public CommandStackViewerContentProvider(TableViewer tableViewer) {
        this.tableViewer = tableViewer;
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        // TODO ?
    }

    @Override
    public void dispose() {
    }

    @Override
    public Object[] getElements(Object inputElement) {
        return recordsByCommandId.values().toArray();
    }

    public void processCommandHistoryEntry(CommandHistoryEntry entry) {
        CommandId commandId = entry.getCommandId();
        CommandHistoryRecord rec;
        boolean create;
        if (recordsByCommandId.containsKey(commandId)) {
            rec = recordsByCommandId.get(commandId);
            create = false;
        } else {
            rec = new CommandHistoryRecord(commandId);
            recordsByCommandId.put(commandId, rec);
            create = true;
        }

        if (create)
            tableViewer.add(rec);
        else
            tableViewer.update(rec, null); // Null, means all properties
    }
}