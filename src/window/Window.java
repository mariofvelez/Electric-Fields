package window;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * 
 * @author Mario Velez
 * 
 *
 */
public class Window extends JFrame
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -3494605708565768482L;
	public static final int WIDTH = 900;
	public static final int HEIGHT = 600;
	
	public static Window window;
	
	public Window(String name)
	{
		super(name);
		
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		Dimension windowSize = new Dimension(WIDTH, HEIGHT);
		this.setSize(windowSize);
	}
	public static void main(String[] args) throws Exception
	{
		window = new Window("Electric Field Simulation");
		
		Container contentPane = window.getContentPane();
		contentPane.setLayout(new BorderLayout());
		
		Field field = new Field(window.getSize());
		contentPane.add(field, BorderLayout.CENTER);
		
		JPanel control_panel = new JPanel();
		
		/*
		 * Controls:
		 * - toggle show grid
		 * - toggle show vectors
		 * - toggle show lines
		 * - line angle slider
		 */
		GridBagLayout layout = new GridBagLayout();
		control_panel.setLayout(layout);
		GridBagConstraints gc = new GridBagConstraints();
		gc.gridx = 0;
		gc.gridy = 0;
		gc.weightx = 0;
		gc.weighty = 0;
		gc.anchor = GridBagConstraints.NORTH;
		gc.insets = new Insets(4, 4, 4, 4);
		
		JCheckBox show_grid_check = new JCheckBox();
		show_grid_check.setSelected(true);
		control_panel.add(show_grid_check, gc);
		
		JCheckBox show_vector_field_check = new JCheckBox();
		show_vector_field_check.setSelected(true);
		gc.gridy = 1;
		control_panel.add(show_vector_field_check, gc);
		
		JCheckBox show_line_check = new JCheckBox();
		show_line_check.setSelected(false);
		gc.gridy = 2;
		control_panel.add(show_line_check, gc);
		
		
		JLabel show_grid_label = new JLabel("Show Grid");
		gc.gridx = 1;
		gc.gridy = 0;
		gc.weightx = 1;
		gc.anchor = GridBagConstraints.NORTHWEST;
		control_panel.add(show_grid_label, gc);
		
		JLabel show_vector_field_label = new JLabel("Show Vector Field");
		gc.gridy = 1;
		control_panel.add(show_vector_field_label, gc);
		
		JLabel show_line_label = new JLabel("Show Line");
		gc.gridy = 2;
		control_panel.add(show_line_label, gc);
		
		JSlider line_slider = new JSlider(0, 100, 0);
		line_slider.setMaximumSize(new Dimension(100, 50));
		gc.gridx = 0;
		gc.gridy = 3;
		gc.gridwidth = 2;
		control_panel.add(line_slider, gc);
		line_slider.setEnabled(false);
		
		JLabel line_slider_label = new JLabel("Line Angle");
		gc.gridy = 4;
		gc.anchor = GridBagConstraints.NORTH;
		control_panel.add(line_slider_label, gc);
		line_slider_label.setEnabled(false);
		
		show_grid_check.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				boolean selected = show_grid_check.isSelected();
				field.electric_field.draw_graph = selected;
			}
		});
		show_vector_field_check.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				boolean selected = show_vector_field_check.isSelected();
				field.electric_field.draw_vector_field = selected;
			}
		});
		show_line_check.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				boolean selected = show_line_check.isSelected();
				line_slider.setEnabled(selected);
				line_slider_label.setEnabled(selected);
				field.electric_field.draw_line = selected;
			}
		});
		line_slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e)
			{
				float val = line_slider.getValue();
				field.electric_field.angle = val * 0.02f * (float) Math.PI;
			}
		});
		
		JTextArea controls_description = new JTextArea("Controls\n"
														+ "q: add a charge\n"
														+ "scroll: zoom\n"
														+ "drag a charge to move it\n"
														+ "+ Positive charges are red\n"
														+ "- Negative charges are blue");
		controls_description.setEditable(false);
		controls_description.setHighlighter(null);
		gc.gridy = 5;
		gc.weighty = 1;
		gc.anchor = GridBagConstraints.NORTHWEST;
		control_panel.add(controls_description, gc);
		
		window.add(control_panel, BorderLayout.EAST);
		
		window.setVisible(true);
		window.setLocation(200, 100);
		
		window.requestFocus();
		field.requestFocusInWindow();
	}
	public static void close()
	{
		window.dispose();
	}
}
