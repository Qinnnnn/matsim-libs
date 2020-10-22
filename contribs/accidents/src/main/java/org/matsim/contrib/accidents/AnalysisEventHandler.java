/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.contrib.accidents;

import com.google.inject.Inject;
import org.apache.commons.collections.MultiMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.vehicles.Vehicle;

import java.util.*;

/**
* @author ikaddoura
*/

public final class AnalysisEventHandler implements EventHandler, LinkLeaveEventHandler, PersonEntersVehicleEventHandler, PersonDepartureEventHandler {

	private final Map<Id<Link>, Map<Integer, Integer>> linkId2time2leavingAgents = new HashMap<>();
	private final Map<Id<Link>, Map<Integer, List<Id<Person>>>> linkId2time2personIds = new HashMap<>();
	private final Map<Id<Vehicle>, Id<Person>> vehicleId2personId = new HashMap<>();

	private final Map<Id<Link>, Map<String, Integer>> linkId2mode2leavingAgents = new HashMap<>();
	private final Map<Id<Person>, String> personId2legMode = new HashMap<>();
	private final Map<Id<Person>, Map<Id<Link>, Set<Integer>>> personId2linkId2time = new HashMap<>();


	@Inject AnalysisEventHandler(){}
	
	@Inject
	private Scenario scenario;

	@Inject
	private AccidentsContext accidentsContext;
	
	@Override
	public void reset(int arg0) {
		// reset temporary information at the beginning of each iteration
		
		linkId2time2leavingAgents.clear();
		linkId2time2personIds.clear();
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		
		double timeBinSize = this.scenario.getConfig().travelTimeCalculator().getTraveltimeBinSize(); 
		int timeBinNr = (int) (event.getTime() / timeBinSize);
		
		Id<Link> linkId = event.getLinkId();
		
		if (linkId2time2leavingAgents.get(linkId) != null) {
			
			if (linkId2time2leavingAgents.get(linkId).get(timeBinNr) != null) {
				int leavingAgents = linkId2time2leavingAgents.get(linkId).get(timeBinNr) + 1;
				linkId2time2leavingAgents.get(linkId).put(timeBinNr, leavingAgents);
				
			} else {
				linkId2time2leavingAgents.get(linkId).put(timeBinNr, 1);
			}
			
		} else {
			Map<Integer, Integer> time2leavingAgents = new HashMap<>();
			time2leavingAgents.put(timeBinNr, 1);
			linkId2time2leavingAgents.put(linkId, time2leavingAgents);
		}
		
		if (linkId2time2personIds.get(linkId) != null) {
			
			if (linkId2time2personIds.get(linkId).get(timeBinNr) != null) {
				linkId2time2personIds.get(linkId).get(timeBinNr).add(getDriverId(event.getVehicleId()));
				
			} else {
				List<Id<Person>> personIds = new ArrayList<>();
				personIds.add(getDriverId(event.getVehicleId()));
				linkId2time2personIds.get(linkId).put(timeBinNr, personIds);
			}
			
		} else {
			Map<Integer, List<Id<Person>>> time2leavingAgents = new HashMap<>();
			List<Id<Person>> personIds = new ArrayList<>();
			personIds.add(getDriverId(event.getVehicleId()));
			time2leavingAgents.put(timeBinNr, personIds);
			linkId2time2personIds.put(linkId, time2leavingAgents);
		}

		String legMode = personId2legMode.get(vehicleId2personId.get(event.getVehicleId()));
		if (linkId2mode2leavingAgents.get(linkId) != null) {
			if (linkId2mode2leavingAgents.get(linkId).get(legMode) != null) {
				int leavingAgents = linkId2mode2leavingAgents.get(linkId).get(legMode) + 1;
				linkId2mode2leavingAgents.get(linkId).put(legMode, leavingAgents);
			} else {
				linkId2mode2leavingAgents.get(linkId).put(legMode, 1);
			}
		} else {
			Map<String, Integer> mode2leavingAgents = new HashMap<>();
			mode2leavingAgents.put(legMode, 1);

			linkId2mode2leavingAgents.put(linkId, mode2leavingAgents);
		}

		AccidentAgentInfo personInfo = accidentsContext.getPersonId2info().get(getDriverId(event.getVehicleId()));
		int hour = (int) (event.getTime()/3600);
		if(personInfo.getLinkId2time2mode().get(linkId)!=null){
			personInfo.getLinkId2time2mode().get(linkId).put(hour, legMode);
		}else{
			Map<Integer, String> time2Mode = new HashMap<>();
			time2Mode.put(hour, legMode);
			personInfo.getLinkId2time2mode().put(linkId, time2Mode);
		}
	}

	private Id<Person> getDriverId(Id<Vehicle> vehicleId) {
		return this.vehicleId2personId.get(vehicleId);
	}

	public double getDemand(Id<Link> linkId, int intervalNr) {
		double demand = 0.;
		if (this.linkId2time2leavingAgents.get(linkId) != null && this.linkId2time2leavingAgents.get(linkId).get(intervalNr) != null) {
			demand = this.linkId2time2leavingAgents.get(linkId).get(intervalNr);
		}
		return demand;
	}

	public double getDemand(Id<Link> linkId, String mode) {
		double demand = 0.;
		if (this.linkId2mode2leavingAgents.get(linkId) != null) {
			if(this.linkId2mode2leavingAgents.get(linkId).get(mode) != null){
				demand = this.linkId2mode2leavingAgents.get(linkId).get(mode);
			}
		}
		return demand;
	}

	public Map<Id<Link>, Map<Integer, List<Id<Person>>>> getLinkId2time2personIds() {
		return linkId2time2personIds;
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		vehicleId2personId.put(event.getVehicleId(), event.getPersonId());
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		personId2legMode.put(event.getPersonId(), event.getLegMode());
	}
	
}

