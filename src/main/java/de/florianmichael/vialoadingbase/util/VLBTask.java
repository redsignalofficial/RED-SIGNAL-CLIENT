
/*
 * RED SIGNAL CLIENT
 * Copyright (C) 2026 Rin. All rights reserved.
 */
package de.florianmichael.vialoadingbase.util;

import com.viaversion.viaversion.api.platform.PlatformTask;
import com.viaversion.viaversion.api.scheduler.Task;
import com.viaversion.viaversion.api.scheduler.TaskStatus;

public class VLBTask implements PlatformTask<Task> {

    private final Task object;

    public VLBTask(Task object) {
        this.object = object;
    }

    @Deprecated
    public Task getObject() {
        return object;
    }

    @Override
    public void cancel() {
        object.cancel();
    }

    @Deprecated
    public TaskStatus getStatus() {
        return this.getObject().status();
    }
}