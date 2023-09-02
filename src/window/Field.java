package window;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;

import javax.swing.JOptionPane;

import math.Transform;
import math.Vec2d;

/**
 * 
 * @author Mario Velez
 *
 */
public class Field extends Canvas
		implements KeyListener, MouseMotionListener, MouseListener, MouseWheelListener,
				   Runnable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -796167392411348854L;
	Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	private Graphics bufferGraphics; // graphics for backbuffer
	private BufferStrategy bufferStrategy;
	
	public static int mousex = 0; // mouse values
	public static int mousey = 0;

	public static ArrayList<Integer> keysDown; // holds all the keys being held down
	boolean leftClick;

	private Thread thread;

	private boolean running;
	private int refreshTime;
	
	public static int[] anchor = new  int[2];
	public static boolean dragging;
	
	ElectricField electric_field;
	Transform transform;
	Transform inverse;
	
	public Field(Dimension size) throws Exception {
		this.setPreferredSize(size);
		this.addKeyListener(this);
		this.addMouseListener(this);
		this.addMouseMotionListener(this);
		this.addMouseWheelListener(this);

		this.thread = new Thread(this);
		running = true;
		refreshTime = (int) (1f/50 * 1000);

		keysDown = new ArrayList<Integer>();
		
		electric_field = new ElectricField(50, 50);
		
		transform = new Transform(3, true);
		transform.data[2] = size.width / 2.0f;
		transform.data[5] = size.height / 2.0f;
		transform.data[0] = 30.0f;
		transform.data[4] = -30.0f;
		
		inverse = new Transform(3, false);
		
//		electric_field.addParticle(2.5f, 2.5f, -1.0f);
//		electric_field.addParticle(-2.5f, -2.5f, 1.0f);
		
		this.addComponentListener(new ComponentListener() {
			public void componentShown(ComponentEvent e) {
			}
			
			public void componentResized(ComponentEvent e)
			{
				transform.data[2] = e.getComponent().getWidth() / 2;
				transform.data[5] = e.getComponent().getHeight() / 2;
			}
			
			@Override
			public void componentMoved(ComponentEvent arg0) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void componentHidden(ComponentEvent arg0) {
				// TODO Auto-generated method stub
				
			}
		});
		
		this.setBackground(Color.BLACK);
		
	}

	public void paint(Graphics g) {


		if (bufferStrategy == null) {
			this.createBufferStrategy(2);
			bufferStrategy = this.getBufferStrategy();
			bufferGraphics = bufferStrategy.getDrawGraphics();

			this.thread.start();
		}
	}
	@Override
	public void run() {
		// what runs when editor is running
		
		while (running) {
			long t1 = System.currentTimeMillis();
			
			DoLogic();
			Draw();

			DrawBackbufferToScreen();

			Thread.currentThread();
			try {
				Thread.sleep(refreshTime);
			} catch (Exception e) {
				e.printStackTrace();
			}
			long t2 = System.currentTimeMillis();
			
			if(t2 - t1 > 16)
			{
				if(refreshTime > 0)
					refreshTime --;
			}
			else
				refreshTime ++;
		}
	}

	public void DrawBackbufferToScreen() {
		bufferStrategy.show();

		Toolkit.getDefaultToolkit().sync();
	}

	public void DoLogic() {
		
		electric_field.updateVectorField();
		
		electric_field.updateLine();
	}

	public void Draw() // titleScreen
	{
		// clears the backbuffer
		bufferGraphics = bufferStrategy.getDrawGraphics();
		try {
			bufferGraphics.clearRect(0, 0, this.getSize().width, this.getSize().height);
			// where everything will be drawn to the backbuffer
			Graphics2D g2 = (Graphics2D) bufferGraphics;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			
			electric_field.draw(transform, g2);
			

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			bufferGraphics.dispose();
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (!keysDown.contains(e.getKeyCode()) && e.getKeyCode() != 86)
			keysDown.add(new Integer(e.getKeyCode()));
		
		if(e.getKeyCode() == KeyEvent.VK_Q)
		{
			String q = JOptionPane.showInputDialog("Enter a q value (micrcoulombs uC)");
			
			try
			{
				float charge = Float.parseFloat(q) / 1_000_000.0f;
				Vec2d pos = new Vec2d(mousex, mousey);
				inverse.project2D(pos);
				
				electric_field.addParticle(pos.x, pos.y, charge);
			}
			catch(Exception e1){}
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		keysDown.remove(new Integer(e.getKeyCode()));
		
	}

	@Override
	public void keyTyped(KeyEvent e) {

	}

	@Override
	public void mouseClicked(MouseEvent e) {
		
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {

	}
	
	Particle selected = null;
	@Override
	public void mousePressed(MouseEvent e) {
		if(e.getButton() == 1)
		{
			leftClick = true;
			Vec2d mouse_pos = new Vec2d(mousex, mousey);
			transform.invert3x3(inverse);
			inverse.project2D(mouse_pos);
			selected = electric_field.getSelectedParticle(mouse_pos);
		}
		else if(e.getButton() == 2)
		{
			
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if(e.getButton() == 1)
		{
			leftClick = false;
			selected = null;
		}
		if(e.getButton() == 2)
			dragging = false;
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if(leftClick)
			leftClick = true;
		
		if(selected != null)
		{
			float dx = e.getX() - mousex;
			float dy = e.getY()- mousey;
			Vec2d vec = new Vec2d(dx, dy);
			inverse.projectVector(vec);
			selected.pos.add(vec);
		}
		
		mousex = e.getX();
		mousey = e.getY();
	}
	
	@Override
	public void mouseMoved(MouseEvent e) {
		mousex = e.getX();
		mousey = e.getY();
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e)
	{
		float dr = (float) e.getPreciseWheelRotation() * 0.1f;
		transform.data[0] *= 1 + dr;
		transform.data[4] *= 1 + dr;
	}

}