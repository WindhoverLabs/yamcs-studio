package org.yamcs.studio.ui.commanding.stack;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.yamcs.studio.ui.commanding.cmdhist.CommandHistoryRecord;

/*
 *        CommandClipboard
 *        Store command entries copied from the command history or the command stack
 *        Copy command sources to system clipboard
 *
 */
public class CommandClipboard {

    static private List<CommandHistoryRecord> copiedCommandHistoryRecords = new ArrayList<>();
    static private List<StackedCommand> copiedStackedCommands = new ArrayList<>();
    static private List<StackedCommand> cutStackedCommands = new ArrayList<>();

    static public void addCommandHistoryRecords(List<CommandHistoryRecord> chrs, Display display) {
        copiedStackedCommands.clear();
        cutStackedCommands.clear();
        copiedCommandHistoryRecords.clear();
        copiedCommandHistoryRecords.addAll(chrs);

        String source = "";
        for (CommandHistoryRecord chr : chrs) {
            source += chr.getSource() + "\n";
        }
        textToClipboard(source, display);
    }

    public static void addStackedCommands(List<StackedCommand> scs, boolean cut, Display display) {
        copiedStackedCommands.clear();
        cutStackedCommands.clear();
        copiedCommandHistoryRecords.clear();
        copiedStackedCommands.addAll(scs);
        if (cut) {
            cutStackedCommands.addAll(scs);
        }

        String source = "";
        for (StackedCommand sc : scs) {
            source += sc.getSource() + "\n";
        }
        textToClipboard(source, display);
    }

    public static List<StackedCommand> getCopiedCommands() throws Exception {
        List<StackedCommand> result = new ArrayList<StackedCommand>();

        // convert CommandHistoryRecord to new Stacked Command
        for (CommandHistoryRecord chr : copiedCommandHistoryRecords) {
            StackedCommand pastedCommand = StackedCommand.buildCommandFromSource(chr.getSource());
            pastedCommand.setComment(chr.getTextForColumn("Comment"));
            result.add(pastedCommand);
        }

        // copy stacked commands
        for (StackedCommand sc : copiedStackedCommands) {
            result.add(sc.copy());
        }

        return result;
    }

    public static List<StackedCommand> getCutCommands() {
        List<StackedCommand> result = new ArrayList<>(cutStackedCommands);
        return result;
    }

    public static boolean hasData() {
        return !(copiedCommandHistoryRecords.isEmpty() && copiedStackedCommands.isEmpty());
    }

    static private void textToClipboard(String text, Display display) {
        final Clipboard cb = new Clipboard(display);
        TextTransfer textTransfer = TextTransfer.getInstance();
        cb.setContents(new Object[] { text }, new Transfer[] { textTransfer });
    }

}
