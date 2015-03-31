package com.mediatek.gallery3d.ext;

public class MovieStrategy implements IMovieStrategy {
    @Override
   //M: shouldEnableNMP() should not be used any more after Anroid FWK notification done.
   //but OP01 is still using this interface, so retain it here.
    public boolean shouldEnableNMP(IMovieItem item) {
        return false;
    }

    @Override
    public boolean shouldEnableCheckLongSleep() {
        return true;
    }

    @Override
    public boolean shouldEnableServerTimeout() {
        return false;
    }
    
    @Override
    public boolean shouldEnableRewindAndForward() {
        return false;
    }
}
