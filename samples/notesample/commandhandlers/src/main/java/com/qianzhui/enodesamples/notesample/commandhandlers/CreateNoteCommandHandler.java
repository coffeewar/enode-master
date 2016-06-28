package com.qianzhui.enodesamples.notesample.commandhandlers;

import com.qianzhui.enode.commanding.ICommandContext;
import com.qianzhui.enode.commanding.ICommandHandler;
import com.qianzhui.enodesamples.notesample.commands.CreateNoteCommand;
import com.qianzhui.enodesamples.notesample.domain.Note;

/**
 * Created by junbo_xu on 2016/4/15.
 */
public class CreateNoteCommandHandler implements ICommandHandler {
    public void handle(ICommandContext context, CreateNoteCommand command) {
        throw new TestException("fuck");
//        context.add(new Note(command.getAggregateRootId(), command.getTitle()));
    }
}
