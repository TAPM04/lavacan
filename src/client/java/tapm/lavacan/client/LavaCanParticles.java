package tapm.lavacan.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;


public final class LavaCanParticles {

    public enum Type { FIRE, SMOKE }

    private static final List<Particle> PARTICLES = new ArrayList<>();
    private static final RandomSource RANDOM = RandomSource.create();

    private static float tickAccumulator = 0f;

    private static TextureAtlasSprite flameSprite;
    private static TextureAtlasSprite[] smokeSprites;

    private LavaCanParticles() {}


    public static void spawn(float screenX, float screenY, Type type, int count) {
        for (int i = 0; i < count; i++) {
            float vx = (RANDOM.nextFloat() - 0.5f) * 0.7f;
            float vy = -(RANDOM.nextFloat() * 0.8f + 0.4f);
            int maxAge = 25 + RANDOM.nextInt(15);  // 25–39 ticks - 1.25–2 s
            float size = type == Type.FIRE
                    ? 4 + RANDOM.nextFloat() * 3    // 4–7 px
                    : 5 + RANDOM.nextFloat() * 4;   // 5–9 px (smoke is puffier)
            float ox = (RANDOM.nextFloat() - 0.5f) * 6;
            float oy = (RANDOM.nextFloat() - 0.5f) * 6;
            PARTICLES.add(new Particle(screenX + ox, screenY + oy, vx, vy, maxAge, size, type));
        }
    }

    public static void updateAndRender(GuiGraphicsExtractor graphics) {
        if (PARTICLES.isEmpty()) return;
        ensureSprites();

        float deltaTicks = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaTicks();
        tickAccumulator += deltaTicks;
        while (tickAccumulator >= 1.0f) {
            tickAccumulator -= 1.0f;
            tickAll();
        }

        float partialTick = tickAccumulator;
        for (Particle p : PARTICLES) {
            renderOne(graphics, p, partialTick);
        }
    }

    public static void clear() {
        PARTICLES.clear();
        tickAccumulator = 0f;
        flameSprite = null;
        smokeSprites = null;
    }

    private static void tickAll() {
        Iterator<Particle> it = PARTICLES.iterator();
        while (it.hasNext()) {
            Particle particle = it.next();
            particle.age++;
            if (particle.age > particle.maxAge) {
                it.remove();
                continue;
            }
            // Save previous position for lerped rendering.
            particle.lastX = particle.x;
            particle.lastY = particle.y;

            // Sine wobble + base velocity.
            particle.x += particle.vx + (float) Math.sin(particle.age * 0.5) * 0.3f;
            particle.y += particle.vy;
            particle.vy *= 0.96f;  // drag — particles slow down as they rise
        }
    }

    private static void renderOne(GuiGraphicsExtractor graphics, Particle particle, float partialTick) {
        // Interpolated position for smooth sub-tick rendering.
        float rx = Mth.lerp(partialTick, particle.lastX, particle.x);
        float ry = Mth.lerp(partialTick, particle.lastY, particle.y);

        // Alpha: quick fade-in, then gradual fade-out.
        float life = (float) particle.age / particle.maxAge;
        float alpha;
        if (life < 0.1f) {
            alpha = life / 0.1f;
        } else {
            alpha = 1.0f - Mth.clamp((life - 0.5f) / 0.5f, 0, 1);
        }

        TextureAtlasSprite sprite;
        int color;
        if (particle.type == Type.FIRE) {
            sprite = flameSprite;
            color = ARGB.colorFromFloat(alpha, 1.0f, 0.6f + RANDOM.nextFloat() * 0.15f, 0.2f);
        } else {
            // Pick a smoke frame based on particle age (cycles through like vanilla).
            int frame = Math.min((int) (life * smokeSprites.length), smokeSprites.length - 1);
            sprite = smokeSprites[frame];
            // Vanilla LARGE_SMOKE tints each RGB channel to 0–0.3 (dark grey).
            float grey = RANDOM.nextFloat() * 0.3f;
            color = ARGB.colorFromFloat(alpha * 0.7f, grey, grey, grey);
        }

        int sz = (int) particle.size;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite,
                (int) rx - sz / 2, (int) ry - sz / 2, sz, sz, color);
    }

    private static void ensureSprites() {
        if (flameSprite == null) {
            TextureAtlas atlas = Minecraft.getInstance()
                    .getAtlasManager()
                    .getAtlasOrThrow(AtlasIds.PARTICLES);
            flameSprite = atlas.getSprite(Identifier.withDefaultNamespace("flame"));

            // Vanilla LARGE_SMOKE uses generic_7 -> generic_0 (reversed order,
            // so the animation goes from "thick" to "thin").
            smokeSprites = new TextureAtlasSprite[8];
            for (int i = 0; i < 8; i++) {
                smokeSprites[i] = atlas.getSprite(
                        Identifier.withDefaultNamespace("generic_" + (7 - i)));
            }
        }
    }

    /**
     * Minimal mutable particle.  Holds current + previous position so the
     * renderer can lerp between game ticks for smooth motion.
     */
    private static final class Particle {
        float x, y;           // current position (updated each game tick)
        float lastX, lastY;   // position at previous tick (for lerp)
        float vx, vy;         // velocity per game tick
        float size;            // side length in pixels
        int age, maxAge;       // current age and maximum before removal
        Type type;

        Particle(float x, float y, float vx, float vy, int maxAge, float size, Type type) {
            this.x = x;        this.y = y;
            this.lastX = x;    this.lastY = y;
            this.vx = vx;      this.vy = vy;
            this.maxAge = maxAge;
            this.size = size;
            this.type = type;
        }
    }
}
