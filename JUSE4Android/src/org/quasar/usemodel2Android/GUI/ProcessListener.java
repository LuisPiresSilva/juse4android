package org.quasar.usemodel2Android.GUI;

import java.util.EventListener;

public interface ProcessListener extends EventListener {
    void processFinished(Process process);
}