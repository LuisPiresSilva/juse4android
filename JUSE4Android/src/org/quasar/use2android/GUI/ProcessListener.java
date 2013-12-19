package org.quasar.use2android.GUI;

import java.util.EventListener;

public interface ProcessListener extends EventListener {
    void processFinished(Process process);
}