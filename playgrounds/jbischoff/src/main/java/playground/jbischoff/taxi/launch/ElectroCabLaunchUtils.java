/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.jbischoff.taxi.launch;

import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.contrib.dvrp.VrpSimEngine;
import org.matsim.contrib.dvrp.data.MatsimVrpData;
import org.matsim.contrib.dvrp.data.network.MatsimVertex;
import org.matsim.contrib.transEnergySim.controllers.EventHandlerGroup;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.*;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.ricardoFaria2012.EnergyConsumptionModelRicardoFaria2012;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.mobsim.qsim.QSim;

import pl.poznan.put.vrp.dynamic.data.model.*;
import playground.jbischoff.energy.charging.DepotArrivalDepartureCharger;
import playground.jbischoff.taxi.evaluation.*;
import playground.jbischoff.taxi.optimizer.rank.NOSRankTaxiOptimizer;
import playground.jbischoff.taxi.sim.ElectricTaxiSimEngine;
import playground.michalm.taxi.model.*;


/**
 * @author jbischoff
 */

public class ElectroCabLaunchUtils
{

    private EnergyConsumptionTracker energyConsumptionTracker;
    // private ChargeUponDepotArrival chargeUponDepotArrival;
    private DepotArrivalDepartureCharger depotArrivalDepartureCharger;
    private TravelDistanceTimeEvaluator travelDistanceEvaluator;
    private TaxiCustomerWaitTimeAnalyser taxiCustomerWaitTimeAnalyser;


    /**
     * Mandatory
     */
    public VrpSimEngine initVrpSimEngine(QSim qSim, MatsimVrpData data,
            NOSRankTaxiOptimizer optimizer)
    {
        Scenario scenario = data.getScenario();

        optimizer.setRankMode(false);
        optimizer.setIdleRankMode(true);
        boolean ALLCARSELECTRIC = true;

        EventsManager events = qSim.getEventsManager();

        EventHandlerGroup handlerGroup = new EventHandlerGroup();

        HashMap<Id, org.matsim.contrib.transEnergySim.vehicles.api.Vehicle> elvehicles = new HashMap<Id, org.matsim.contrib.transEnergySim.vehicles.api.Vehicle>();

        travelDistanceEvaluator = new TravelDistanceTimeEvaluator(scenario.getNetwork());

        if (ALLCARSELECTRIC) {

            for (Vehicle v : data.getVrpData().getVehicles()) {
                Id aid = new IdImpl(v.getName());
                elvehicles.put(aid, ((VrpAgentElectricTaxi)v).getElectricVehicle());
                travelDistanceEvaluator.addAgent(aid);
            }
        }

        for (Vehicle v : data.getVrpData().getVehicles()) {
            Id aid = new IdImpl(v.getName());
            travelDistanceEvaluator.addAgent(aid);
        }

        energyConsumptionTracker = new EnergyConsumptionTracker(elvehicles, scenario.getNetwork());
        depotArrivalDepartureCharger = new DepotArrivalDepartureCharger(elvehicles);
        taxiCustomerWaitTimeAnalyser = new TaxiCustomerWaitTimeAnalyser(scenario);

        handlerGroup.addHandler(travelDistanceEvaluator);
        handlerGroup.addHandler(energyConsumptionTracker);
        handlerGroup.addHandler(depotArrivalDepartureCharger);
        handlerGroup.addHandler(taxiCustomerWaitTimeAnalyser);

        List<Id> depotLinkIds = new ArrayList<Id>();
        for (Depot d : data.getVrpData().getDepots()) {
            depotLinkIds.add( ((MatsimVertex)d.getVertex()).getLink().getId());
        }

        depotArrivalDepartureCharger.setDepotLocations(depotLinkIds);
        events.addHandler(handlerGroup);

        optimizer.setDepotArrivalCharger(depotArrivalDepartureCharger);

        // chargeUponDepotArrival = new ChargeUponDepotArrival(elvehicles);
        // chargeUponDepotArrival.setDepotLocations(this.depotReader.getDepotLinks());

        // handlerGroup.addHandler(chargeUponDepotArrival);

        VrpSimEngine taxiSimEngine = new ElectricTaxiSimEngine(qSim, data, optimizer,
                depotArrivalDepartureCharger);
        qSim.addMobsimEngine(taxiSimEngine);

        return taxiSimEngine;
    }


    public void printStatisticsToConsole()
    {
        System.out.println("energy consumption stats");
        depotArrivalDepartureCharger.getSoCLog().printToConsole();
        System.out.println("===");

    }


    public String writeStatisticsToFiles(String dirname)
    {
        System.out.println("writing energy consumption stats directory to " + dirname);
        depotArrivalDepartureCharger.getSoCLog().writeToFiles(dirname);
        depotArrivalDepartureCharger.getChargeLog().writeToFiles(dirname);
        String dist = travelDistanceEvaluator.writeTravelDistanceStatsToFiles(dirname
                + "travelDistanceStats.txt");
        String wait = taxiCustomerWaitTimeAnalyser.writeCustomerWaitStats(dirname
                + "customerWaitStatistic.txt");
        System.out.println("...done");
        travelDistanceEvaluator.printTravelDistanceStatistics();
        taxiCustomerWaitTimeAnalyser.printTaxiCustomerWaitStatistics();

        return wait + "\t" + dist;
    }

}
