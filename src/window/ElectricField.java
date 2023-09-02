package window;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.function.BiFunction;

import geometry.Circle;
import geometry.LineSegment;
import integration.RK4Integrator;
import math.Transform;
import math.Vec2d;

public class ElectricField {
	
	public static final float inv_4pi_e = 8987000000.0f;
	public float Er = 1.0f; // 1.0 for vacuum
	
	private final int LINE_SIZE = 200;
	
	public int width; // number of vectors displayed width * height
	public int height;
	public float spacing = 0.5f; // spacing between vectors
	
	/**
	 * used for integration. Same as calculateCharge()
	 */
	public BiFunction<Float, Vec2d, Vec2d> f;
	public Vec2d[] vector_field;
	
	public ArrayList<Particle> particles;
	
	public boolean draw_graph = true;
	public boolean draw_vector_field = true;
	public boolean draw_line = false;
	public float angle = 0.0f;
	
//	private Vec3d min_col = new Vec3d(0.0f, 1.0f, 1.0f);
//	private Vec3d max_col = new Vec3d(1.0f, 0.0f, 0.0f);
	
	/**
	 * Used for marching along the electric field to create the field lines
	 */
	private RK4Integrator integrator;
	private int line_len = LINE_SIZE;
	private Vec2d[] line;
	private Vec2d[] line_proj;
	
	public ElectricField(int width, int height)
	{
		this.width = width;
		this.height = height;
		
		f = (t, x) -> {
			Vec2d v = new Vec2d();
			
			for(int i = 0; i < particles.size(); ++i)
			{
				Particle p = particles.get(i);
				float d_sq = Vec2d.dist2(x, p.pos);
				Vec2d dir = Vec2d.normalize(Vec2d.subtract(x, p.pos));
				float Ei = (inv_4pi_e / Er) * (p.charge / d_sq);
				v.add(dir.x * Ei, dir.y * Ei);
			}
			
			return v;
		};
		
		f = (t, y) -> {
			Vec2d charge = new Vec2d();
			calculateCharge(y, charge);
			return charge;
		};
		
		vector_field = new Vec2d[width * height];
		for(int i = 0; i < width * height; ++i)
			vector_field[i] = new Vec2d(0.5f, 0.5f);
		
		particles = new ArrayList<>();
		
		integrator = new RK4Integrator();
		line = new Vec2d[LINE_SIZE];
		line_proj = new Vec2d[LINE_SIZE];
		for(int i = 0; i < LINE_SIZE; ++i)
		{
			line[i] = new Vec2d(0, 0);
			line_proj[i] = new Vec2d(0, 0);
		}
	}
	public void addParticle(float x, float y, float charge)
	{
		particles.add(new Particle(x, y, charge));
	}
	/**
	 * Calculates the charge of the electric field at a point
	 * @param pos - the position to check
	 * @param charge - stores the vector of the charge
	 */
	public void calculateCharge(Vec2d pos, Vec2d charge)
	{
		charge.setZero();
		
		for(int i = 0; i < particles.size(); ++i)
		{
			Particle p = particles.get(i);
			float d_sq = Vec2d.dist2(pos, p.pos);
			Vec2d dir = Vec2d.normalize(Vec2d.subtract(pos, p.pos));
			float Ei = (inv_4pi_e / Er) * (p.charge / d_sq);
			charge.add(dir.x * Ei, dir.y * Ei);
		}
	}
	public void updateVectorField()
	{
		float w = width * spacing - spacing;
		float h = height * spacing - spacing;
		
		for(int y = 0; y < height; ++y)
		{
			for(int x = 0; x < width; ++x)
			{
				Vec2d pos = new Vec2d(-w / 2.0f + x * spacing, -h / 2.0f + y * spacing);
				
				calculateCharge(pos, vector_field[y*width + x]);
			}
		}
	}
	public void updateLine()
	{
		// first positive charge location
		Vec2d positive_pos = null;
		for(int i = 0; i < particles.size(); ++i)
		{
			if(particles.get(i).charge > 0)
			{
				positive_pos = new Vec2d(particles.get(i).pos);
				break;
			}
		}
		
		// could not find a positive charge
		if(positive_pos == null)
		{
			line_len = 0;
			return;
		}
		
		line[0].set(positive_pos);
		line[0].add(Vec2d.fromPolar(angle, 0.1f));
		line_len = 1;
		
		float dt = 0.2f;
		
		for(int i = 1; i < LINE_SIZE; ++i)
		{
			float len = f.apply(0.0f, line[i-1]).length();
			line[i] = integrator.y(dt * i, dt / len, line[i-1], f);
			
			// check if value is too large
			if(Vec2d.dist2(line[i], line[i-1]) > 4.0f)
			{
				return;
			}
			// check if the line intersects a point
			LineSegment ls = new LineSegment(line[i], line[i-1]);
			for(int j = 0; j < particles.size(); ++j)
			{
				Particle p = particles.get(j);
				Circle c = new Circle(p.pos, 0.09f);
				if(c.intersects(ls))
				{
					line[i].set(c.pos);
					return;
				}
			}
			line_len = i+1;
		}
	}
	/**
	 * Queries for the first particle within a distance 0.3 of a point
	 * @param pos - the position to query
	 * @returns the Particle queried, null if no Particle found
	 */
	public Particle getSelectedParticle(Vec2d pos)
	{
		for(int i = 0; i < particles.size(); ++i)
		{
			if(particles.get(i).intersects(pos, 0.3f))
				return particles.get(i);
		}
		return null;
	}
	public void draw(Transform transform, Graphics2D g2)
	{
		if(draw_graph)
			drawPlane(g2, transform);
		
		float w = width * spacing - spacing;
		float h = height * spacing - spacing;
		
		// vector field
		if(draw_vector_field)
		{	
			for(int y = 0; y < height; ++y)
			{
				for(int x = 0; x < width; ++x)
				{
					Vec2d pos = new Vec2d(-w / 2.0f + x * spacing, -h / 2.0f + y * spacing);
					Vec2d v = vector_field[y * width + x];
					drawVector(transform, g2, v, pos);
				}
			}
		}
		
		// particles
		for(int i = 0; i < particles.size(); ++i)
		{
			Particle p = particles.get(i);
			
			if(p.charge > 0)
				g2.setColor(Color.RED);
			else
				g2.setColor(Color.BLUE);
			
			Vec2d pos = new Vec2d(p.pos);
			transform.project2D(pos);
			
			g2.fillOval((int) (pos.x - 4), (int) (pos.y - 4), 8, 8);
		}
		
		// line
		if(draw_line)
			drawLine(g2, transform);
	}
	// shape of the arrow
	private Vec2d[] arrow = {
			new Vec2d(0.3f, 0.0f),
			new Vec2d(-0.2f, -0.1f),
			new Vec2d(-0.2f, 0.1f)
	};
	public void drawVector(Transform transform, Graphics2D g2, Vec2d vec, Vec2d offs)
	{	
//		vec.normalize();
		vec.mult(0.00008f); // scaling the vectors to a manageable size
		
		Vec2d temp = new Vec2d(vec);
		
		float len = vec.length();
		len = Math.max(Math.min(1.5f, len), 0.8f);
		vec.normalize();
		vec.mult(len);
		
		Vec2d normal = vec.leftNormal();
		
		Transform tf = new Transform(3, false);
		tf.data[0] = vec.x;
		tf.data[1] = vec.y;
		tf.data[3] = normal.x;
		tf.data[4] = normal.y;
		
		Vec2d[] proj = new Vec2d[3];
		for(int i = 0; i < 3; ++i)
		{
			proj[i] = new Vec2d(arrow[i]);
			tf.project2D(proj[i]);
			proj[i].add(offs);
			transform.project2D(proj[i]);
		}
		
		// coloring
		float clamped = temp.length();
		clamped = Math.max(Math.min(2.0f, clamped), 0.1f);
		
		float lerp = (clamped - 0.1f) / (10.0f - 0.1f);
//		Vec3d col = Vec3d.lerp(min_col, max_col, lerp);
		g2.setColor(Color.getHSBColor(lerp, 1.0f, 1.0f));
		g2.fillPolygon(Vec2d.xPoints(proj), Vec2d.yPoints(proj), 3);
	}
	public void drawPlane(Graphics2D g2, Transform transform)
	{
		g2.setColor(Color.DARK_GRAY);
		
		float w = width * spacing - spacing;
		float h = height * spacing - spacing;
		
		// x axis
		for(int i = 0; i < height; ++i)
		{
			Vec2d left = new Vec2d(-w/2 - 2, -h / 2.0f + i * spacing);
			Vec2d right = new Vec2d(w/2 + 2,-h / 2.0f + i * spacing);
			
			transform.project2D(left);
			transform.project2D(right);
			
			g2.drawLine((int) left.x, (int) left.y, (int) right.x, (int) right.y);
		}
		// y axis
		for(int i = 0; i < height; ++i)
		{
			Vec2d top = new Vec2d(-w / 2.0f + i * spacing, -h/2 - 2);
			Vec2d bottom = new Vec2d(-w / 2.0f + i * spacing, h/2 + 2);
			
			transform.project2D(top);
			transform.project2D(bottom);
			
			g2.drawLine((int) top.x, (int) top.y, (int) bottom.x, (int) bottom.y);
		}
	}
	public void drawLine(Graphics2D g2, Transform transform)
	{
		g2.setColor(Color.WHITE);
		
		for(int i = 0; i < line_len; ++i)
		{
			line_proj[i].set(line[i]);
			transform.project2D(line_proj[i]);
		}
		
		g2.drawPolyline(Vec2d.xPoints(line_proj), Vec2d.yPoints(line_proj), line_len);
	}

}
