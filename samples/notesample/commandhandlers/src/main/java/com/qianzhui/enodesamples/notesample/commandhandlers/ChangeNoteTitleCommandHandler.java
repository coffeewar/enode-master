package com.qianzhui.enodesamples.notesample.commandhandlers;

import com.qianzhui.enode.commanding.ICommandContext;
import com.qianzhui.enode.commanding.ICommandHandler;
import com.qianzhui.enodesamples.notesample.commands.ChangeNoteTitleCommand;
import com.qianzhui.enodesamples.notesample.domain.Note;

/**
 * Created by junbo_xu on 2016/4/15.
 */
public class ChangeNoteTitleCommandHandler implements ICommandHandler {
    public void handle(ICommandContext context, ChangeNoteTitleCommand command) {
        context.get(Note.class, command.getAggregateRootId()).changeTitle(command.getTitle());
    }
}
