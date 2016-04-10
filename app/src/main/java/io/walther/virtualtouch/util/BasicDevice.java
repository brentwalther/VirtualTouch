package io.walther.virtualtouch.util;

/**
 * Created by brentwalther on 4/10/2016.
 */

public class BasicDevice implements ReactionRecorder.ReactionDevice {

    private boolean isReacting;

    public void setIsReacting(boolean isReacting) {
        this.isReacting = isReacting;
    }

    @Override
    public boolean isReacting() {
        return isReacting;
    }
}
