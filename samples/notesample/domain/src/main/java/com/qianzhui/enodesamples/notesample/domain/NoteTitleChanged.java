package com.qianzhui.enodesamples.notesample.domain;

import com.qianzhui.enode.eventing.DomainEvent;

/**
 * Created by junbo_xu on 2016/4/15.
 */
public class NoteTitleChanged extends DomainEvent<String> {
    private String title;

    public NoteTitleChanged(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
