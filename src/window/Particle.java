package window;

import math.Vec2d;

public class Particle {
	
	public Vec2d pos;
	public float charge;
	
	public Particle(float x, float y, float q)
	{
		pos = new Vec2d(x, y);
		charge = q;
	}
	public boolean intersects(Vec2d pos, float r)
	{
		return Vec2d.dist(pos, this.pos) <= r;
	}

}
