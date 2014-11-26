/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.roadpricing.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.roadpricing.RoadPricing;
import org.matsim.roadpricing.RoadPricingConfigGroup;

/**
 * Basic "script" to use roadpricing.
 * 
 * @author nagel
 *
 */
public class Main {

	public static void main(String[] args) {
		// load the config, telling it to "materialize" the road pricing section:
		Config config = ConfigUtils.loadConfig( args[0], new RoadPricingConfigGroup() ) ;
		
		// load the scenario:
		Scenario scenario = ScenarioUtils.loadScenario(config) ;

		// instantiate the controler:
		Controler controler = new Controler(scenario) ;

		// instantiate road pricing and add it as controler listener:
		RoadPricing roadPricing = new RoadPricing() ;
		controler.addControlerListener( roadPricing ) ;

		// run the controler:
		controler.run() ;
	}

}
