package com.frozenbloo;

import lombok.RequiredArgsConstructor;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.particle.ParticleCreator;

@RequiredArgsConstructor
public class ParticleBuilder {

    private final Particle partcile;
    private final Pos pos;
    private final Boolean distance;
    private final int count;

    private float particleSpeed;
    private final float[] offset = new float[] {0,0,0};

    public ParticleBuilder setOffset(float x, float y, float z) {
        this.offset[0] = x;
        this.offset[1] = y;
        this.offset[2] = z;
        return this;
    }

    public ParticleBuilder setSpeed(float speed) {
        particleSpeed = speed;
        return this;
    }

    public ParticlePacket build() {
        return ParticleCreator.createParticlePacket(partcile, distance, pos.x(), pos.y(), pos.z(), offset[0], offset[1], offset[2], particleSpeed, count, null);
    }
}
