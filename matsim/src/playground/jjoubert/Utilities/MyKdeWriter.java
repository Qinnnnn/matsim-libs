/* *********************************************************************** *
 * project: org.matsim.*
 * MyKdeWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.jjoubert.Utilities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import playground.jjoubert.CommercialDemand.MyGrid;
import playground.jjoubert.CommercialDemand.MyGridCell;

/**
 * A class to write the kernel density estimation results to a text file that is readable
 * by the ET GeoWizards tool into ArcGIS. 
 * 
 * @author jwjoubert
 */
public class MyKdeWriter {

	public MyKdeWriter(){
		
	}
	/**
	 * Writes the given cells (boxes) to a file.
	 * @param grid a <code>QuadTree</code> containing <code>MyGridCell</code>s. The cells are
	 * 		  extensions of type <code>Envelope</code>.
	 * @param outputFilename the absolute path of the file where the cells are to be written to.
	 * @see {@link http://www.ian-ko.com/ET_GeoWizards/UserGuide/et_geowizards_userguide.htm} 
	 * 		for the required file format.
	 */
	public void writeKdeToFile(MyGrid grid, String outputFilename){

		
		
		
		try {
			BufferedWriter output = new BufferedWriter(new FileWriter(new File(outputFilename)));
			try{
				output.write("ID,XMIN,YMIN,XMAX,YMAX,ACTIVITY_COUNT");
				output.newLine();
				
				for (MyGridCell cell : grid.getGrid().values()) {
					output.write(String.valueOf(cell.getId()));
					output.write(",");
					output.write(String.valueOf(cell.getMinX()));
					output.write(",");
					output.write(String.valueOf(cell.getMinY()));
					output.write(",");
					output.write(String.valueOf(cell.getMaxX()));
					output.write(",");
					output.write(String.valueOf(cell.getMaxY()));
					output.write(",");
					output.write(String.valueOf(cell.getCount()));
					output.newLine();
				}
				
				output.write("END");
			} finally{
				output.close();
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
}
