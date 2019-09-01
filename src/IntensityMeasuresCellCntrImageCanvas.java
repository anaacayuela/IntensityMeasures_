/*
 * CellCntrImageCanvas.java
 *
 * Created on November 22, 2005, 5:58 PM
 *
 */
/*
 *
 * @author Kurt De Vos � 2005
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
import ij.gui.ImageCanvas;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.gui.Toolbar;
import ij.measure.Calibration;
import ij.plugin.filter.RGBStackSplitter;
import ij.process.ImageProcessor;
import java.awt.BasicStroke;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.util.ListIterator;
import java.util.Vector;
import java.io.*;

/**
 * 
 * @author Kurt De Vos
 */
public class IntensityMeasuresCellCntrImageCanvas extends ImageCanvas {
	private Vector typeVector;
	private IntensityMeasuresCellCntrMarkerVector currentMarkerVector;
	private IntensityMeasures cc;
	private ImagePlus img;
	private boolean delmode = false;
	private boolean showNumbers = false;
	private boolean showAll = false;
	private Font font = new Font("SansSerif", Font.PLAIN, 10);
	private int radius = 10;

	/** Creates a new instance of CellCntrImageCanvas */
	public IntensityMeasuresCellCntrImageCanvas(ImagePlus img, Vector typeVector, IntensityMeasures cc, Overlay overlay) {
		super(img);
		this.img = img;
		this.typeVector = typeVector;
		this.cc = cc;
		if (overlay != null)
			this.setOverlay(overlay);
	}

	public void mousePressed(MouseEvent e) {
		if (IJ.spaceBarDown() || Toolbar.getToolId() == Toolbar.MAGNIFIER
				|| Toolbar.getToolId() == Toolbar.HAND
				|| Toolbar.getToolId() == Toolbar.FREEROI) {
			super.mousePressed(e);
			return;
		}

		if (currentMarkerVector == null) {
			IJ.error("Select a counter type first!");
			return;
		}

		int x = super.offScreenX(e.getX());
		int y = super.offScreenY(e.getY());
		if (!delmode) {
			IntensityMeasuresCellCntrMarker m = new IntensityMeasuresCellCntrMarker(x, y, img.getCurrentSlice());
			currentMarkerVector.addMarker(m);
		} else {
			IntensityMeasuresCellCntrMarker m = currentMarkerVector.getMarkerFromPosition(
					new Point(x, y), img.getCurrentSlice());
			currentMarkerVector.remove(m);
		}
		repaint();
	}

	public void mouseReleased(MouseEvent e) {
		super.mouseReleased(e);
	}

	public void mouseMoved(MouseEvent e) {
		super.mouseMoved(e);
	}

	public void mouseExited(MouseEvent e) {
		super.mouseExited(e);
	}

	public void mouseEntered(MouseEvent e) {
		super.mouseEntered(e);
		if (!IJ.spaceBarDown() | Toolbar.getToolId() != Toolbar.MAGNIFIER
				| Toolbar.getToolId() != Toolbar.HAND)
			setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
	}

	public void mouseDragged(MouseEvent e) {
		super.mouseDragged(e);
	}

	public void mouseClicked(MouseEvent e) {
		super.mouseClicked(e);
	}

	private Point point;
	private Rectangle srcRect = new Rectangle(0, 0, 0, 0);

	public void paint(Graphics g) {
		super.paint(g);
		srcRect = getSrcRect();
		Roi roi = img.getRoi();
		double xM = 0;
		double yM = 0;

		/*
		 * double magnification = super.getMagnification();
		 * 
		 * try { if (imageUpdated) { imageUpdated = false; img.updateImage(); }
		 * Image image = img.getImage(); if (image!=null) g.drawImage(image, 0,
		 * 0, (int)(srcRect.width*magnification),
		 * (int)(srcRect.height*magnification), srcRect.x, srcRect.y,
		 * srcRect.x+srcRect.width, srcRect.y+srcRect.height, null); if (roi !=
		 * null) roi.draw(g); } catch(OutOfMemoryError e) {
		 * IJ.outOfMemory("Paint "+e.getMessage()); }
		 */

		Graphics2D g2 = (Graphics2D) g;
		g2.setStroke(new BasicStroke(1f));
		g2.setFont(font);

		ListIterator it = typeVector.listIterator();
		while (it.hasNext()) {
			IntensityMeasuresCellCntrMarkerVector mv = (IntensityMeasuresCellCntrMarkerVector) it.next();
			int typeID = mv.getType();
			g2.setColor(mv.getColor());
			ListIterator mit = mv.listIterator();
			while (mit.hasNext()) {
				IntensityMeasuresCellCntrMarker m = (IntensityMeasuresCellCntrMarker) mit.next();
				boolean sameSlice = m.getZ() == img.getCurrentSlice();
				if (sameSlice || showAll) {
					xM = ((m.getX() - srcRect.x) * magnification);
					yM = ((m.getY() - srcRect.y) * magnification);
					g2.drawOval((int) (xM - radius * magnification),
							(int) (yM - radius * magnification),
							(int) (2 * radius * magnification),
							(int) (2 * radius * magnification));
					if (showNumbers)
						g2.drawString(Integer.toString(typeID), (int) xM + 3,
								(int) yM - 3);
				}
			}
		}
	}

	public double measure() {
		// Produce raw data
		ImageProcessor ip = img.getProcessor();
		ip.getWidth();
		ip.getHeight();
		int radius2 = radius * radius;

		ListIterator it = typeVector.listIterator();
		double value = 0;
		double A=0;
		while (it.hasNext()) {
			IntensityMeasuresCellCntrMarkerVector mv = (IntensityMeasuresCellCntrMarkerVector) it.next();
			int typeID = mv.getType();
			ListIterator mit = mv.listIterator();
			while (mit.hasNext()) {
				IntensityMeasuresCellCntrMarker m = (IntensityMeasuresCellCntrMarker) mit.next();
				int xM = m.getX();
				int yM = m.getY();

				for (int sx = -radius; sx <= radius; sx++) {
					for (int sy = -radius; sy <= radius; sy++) {
						value += ip.getPixelValue(xM + sx, yM + sy);
						A++;
					}
				}
			}
		}
		double avg=value/A;
		IJ.setTool(Toolbar.FREEROI);
		System.out.println(avg);
		return avg;
	}

	public Vector getTypeVector() {
		return typeVector;
	}

	public void setTypeVector(Vector typeVector) {
		this.typeVector = typeVector;
	}

	public IntensityMeasuresCellCntrMarkerVector getCurrentMarkerVector() {
		return currentMarkerVector;
	}

	public void setCurrentMarkerVector(IntensityMeasuresCellCntrMarkerVector currentMarkerVector) {
		this.currentMarkerVector = currentMarkerVector;
	}

	public boolean isDelmode() {
		return delmode;
	}

	public void setDelmode(boolean delmode) {
		this.delmode = delmode;
	}

	public boolean isShowNumbers() {
		return showNumbers;
	}

	public void setShowNumbers(boolean showNumbers) {
		this.showNumbers = showNumbers;
	}

	public void setShowAll(boolean showAll) {
		this.showAll = showAll;
	}

}
