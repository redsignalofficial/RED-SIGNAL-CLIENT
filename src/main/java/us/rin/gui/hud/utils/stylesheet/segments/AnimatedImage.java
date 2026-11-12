package us.rin.gui.hud.utils.stylesheet.segments;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class AnimatedImage {
    private final List<ResourceLocation> frames;
    private final List<Integer> durations;
    private final int totalDuration;

    public AnimatedImage(List<ResourceLocation> frames, List<Integer> durations) {
        this.frames = frames;
        this.durations = durations;
        int total = 0;
        for (int duration : durations) {
            total += Math.max(20, duration);
        }
        this.totalDuration = Math.max(1, total);
    }

    public ResourceLocation getCurrentFrame() {
        if (frames.isEmpty()) {
            return null;
        }

        long now = System.currentTimeMillis() % totalDuration;
        int elapsed = 0;
        for (int i = 0; i < frames.size(); i++) {
            elapsed += Math.max(20, durations.get(i));
            if (now < elapsed) {
                return frames.get(i);
            }
        }
        return frames.get(frames.size() - 1);
    }
}
