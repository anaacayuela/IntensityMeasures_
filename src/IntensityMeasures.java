/*
 *
 * @author Carlos Oscar S. Sorzano 2009
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation (http://www.gnu.org/licenses/gpl.txt )
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.ImageWindow;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.gui.StackWindow;
import ij.gui.Toolbar;
import ij.process.ImageProcessor;
import ij.CompositeImage;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.*;
import java.util.ListIterator;
import java.util.Vector;
import java.util.Collections;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;

public class IntensityMeasures extends JFrame implements ActionListener,
		ItemListener {
	private static final String INITIALIZE = "1. Initialize";
	private static final String BACKGROUND = "2. Background";
	private static final String WHOLECELL = "3. Whole cell";
	private static final String INTEREST = "4. Interest region";

	private Vector typeVector;
	private IntensityMeasuresCellCntrMarkerVector markerVector;

	private JPanel statButtonPanel;
	private JButton initializeButton;
	private JButton backgroundButton;
	private JButton wholeCellButton;
	private JButton interestButton;

	private IntensityMeasuresCellCntrImageCanvas ic;

	private ImagePlus img;
	private ImagePlus counterImg;

	private GridLayout dynGrid;

	private double bg;
	private ROImeasurement whole;
	private ROImeasurement interest;

	private boolean isJava14;

	static IntensityMeasures instance;

	public IntensityMeasures() {
		super("Intensity Measures");
		isJava14 = IJ.isJava14();
		if (!isJava14) {
			IJ
					.showMessage("You are using a pre 1.4 version of java, exporting and loading marker data is disabled");
		}
		setResizable(false);
		typeVector = new Vector();
		initGUI();
		instance = this;
	}

	/** Show the GUI threadsafe */
	private static class GUIShower implements Runnable {
		final JFrame jFrame;

		public GUIShower(JFrame jFrame) {
			this.jFrame = jFrame;
		}

		public void run() {
			jFrame.pack();
			jFrame.setLocation(100, 200);
			jFrame.setVisible(true);
		}
	}

	private void initGUI() {
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		GridBagLayout gb = new GridBagLayout();
		getContentPane().setLayout(gb);

		// create a "static" panel to hold control buttons
		statButtonPanel = new JPanel();
		statButtonPanel.setBorder(BorderFactory.createTitledBorder("Actions"));
		statButtonPanel.setLayout(gb);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridx = 0;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		initializeButton = makeButton(INITIALIZE, "Initialize image to count");
		gb.setConstraints(initializeButton, gbc);
		statButtonPanel.add(initializeButton);

		gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridx = 0;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		backgroundButton = makeButton(BACKGROUND, "Measure background");
		gb.setConstraints(backgroundButton, gbc);
		statButtonPanel.add(backgroundButton);

		gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridx = 0;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		wholeCellButton = makeButton(WHOLECELL,
				"Measure intensity in the whole cell");
		gb.setConstraints(wholeCellButton, gbc);
		statButtonPanel.add(wholeCellButton);

		gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridx = 0;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		interestButton = makeButton(INTEREST,
				"Measure intensity in the interest region");
		gb.setConstraints(interestButton, gbc);
		statButtonPanel.add(interestButton);

		gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.ipadx = 5;
		gb.setConstraints(statButtonPanel, gbc);
		getContentPane().add(statButtonPanel);

		Runnable runner = new GUIShower(this);
		EventQueue.invokeLater(runner);
	}

	private JButton makeButton(String name, String tooltip) {
		JButton jButton = new JButton(name);
		jButton.setToolTipText(tooltip);
		jButton.addActionListener(this);
		return jButton;
	}

	private void initializeImage() {
		reset();
		if (markerVector==null)
		{
			markerVector = new IntensityMeasuresCellCntrMarkerVector(1);
			typeVector.add(markerVector);
		}
		else
			markerVector.clear();
		img = WindowManager.getCurrentImage();
		boolean v139t = IJ.getVersion().compareTo("1.39t") >= 0;
		if (img == null) {
			IJ.noImage();
		} else if (img.getStackSize() == 1) {
			ImageProcessor ip = img.getProcessor().convertToByte(false);
			ip.resetRoi();
			ip = ip.crop();
			counterImg = new ImagePlus("Counter Window - " + img.getTitle(), ip);
			Overlay overlay = counterImg.getOverlay();
			ic = new IntensityMeasuresCellCntrImageCanvas(counterImg, typeVector, this, overlay);
			new ImageWindow(counterImg, ic);
		} else if (img.getStackSize() > 1) {
			ImageStack stack = img.getStack();
			int size = stack.getSize();
			ImageStack counterStack = img.createEmptyStack();
			for (int i = 1; i <= size; i++) {
				ImageProcessor ip = stack.getProcessor(i);
				ip.resetRoi();
				ip = ip.crop();
				counterStack.addSlice(stack.getSliceLabel(i), ip);
			}
			counterImg = new ImagePlus("Counter Window - " + img.getTitle(),
					counterStack);
			counterImg.setDimensions(img.getNChannels(), img.getNSlices(), img
					.getNFrames());
			if (img.isComposite()) {
				counterImg = new CompositeImage(counterImg,
						((CompositeImage) img).getMode());
				((CompositeImage) counterImg).copyLuts(img);
			}
			counterImg.setOpenAsHyperStack(img.isHyperStack());
			Overlay displayList = v139t ? img.getCanvas().getOverlay()
					: null;
			ic = new IntensityMeasuresCellCntrImageCanvas(counterImg, typeVector, this,
					displayList);
			new StackWindow(counterImg, ic);
		}
		ic.setCurrentMarkerVector(markerVector);
		img.hide();
	}

	public void reset() {
		if (ic != null)
			ic.repaint();
	}

	void validateLayout() {
		statButtonPanel.validate();
		validate();
		pack();
	}

	public void actionPerformed(ActionEvent event) {
		String command = event.getActionCommand();

		if (command.compareTo(INITIALIZE) == 0) {
			initializeImage();
		} else if (command.startsWith("2.")) {
			bg = measureBg();
		} else if (command.startsWith("3.")) {
			whole = measureROI();
		} else if (command.startsWith("4.")) { // COUNT
			interest = measureROI();
			report();
		}
		if (ic != null)
			ic.repaint();
	}

	public double measureBg() {
		return ic.measure();
	}

	public ROImeasurement measureROI() {
		Rectangle roiRect = new Rectangle(0, 0, img.getWidth(), img.getHeight());
		ImageProcessor ip = img.getProcessor();
		ROImeasurement retval=new ROImeasurement();

		// If there is any ROI selected restricts the area to be duplicated to
		// it.
		Roi roi = counterImg.getRoi();
		if (roi != null && roi.isArea()) {
			roiRect = roi.getBounds();
		}

		retval.totalSum = 0;
		retval.area = 0;
		for (int y = 0; y < roiRect.height; y++) {
			int actualY = y + roiRect.y;
			for (int x = 0; x < roiRect.width; x++) {
				int actualX = x + roiRect.x;
				if (roi.contains(actualX, actualY)) {
					retval.totalSum += ip.getPixelValue(actualX, actualY);
					retval.area++;
				}
			}
		}
		retval.meanIntensity = retval.totalSum / retval.area;
		IJ.run("Select None");
		return retval;
	}

	public void itemStateChanged(ItemEvent e) {
	}

	public void report() {
		IJ.setColumnHeadings("Mean Bg\tCell Mean\tInterest Interest\tRatio Mean\tCell Area\tCell Total\tInterest Area\tInterest Total\tPercentage");
		double ratio = (interest.meanIntensity - bg) / (whole.meanIntensity - bg);
		double percentage = (interest.totalSum - bg*interest.area)/
			(whole.totalSum - bg*whole.area);
		String output = bg + "\t" + whole.meanIntensity + "\t" +
		    interest.meanIntensity + "\t"+
		    ratio+"\t"+
		    whole.area + "\t"+
		    (whole.totalSum - bg*whole.area) +"\t"+
		    interest.area + "\t"+
		    (interest.totalSum - bg*interest.area) +"\t"+
		    percentage;
		IJ.write(output);

		try {
			FileOutputStream rawData = new FileOutputStream(
					"IntensityMeasures.txt",
					(new File("IntensityMeasures.txt")).exists());
			PrintStream pRawData = new PrintStream(rawData);
			pRawData.println(output);
			rawData.close();
		} catch (FileNotFoundException e) {
			IJ.write("Cannot open raw data file for output");
			return;
		} catch (IOException e) {
			// Do nothing
		}
		IJ.setTool(Toolbar.RECTANGLE);
	}
}
