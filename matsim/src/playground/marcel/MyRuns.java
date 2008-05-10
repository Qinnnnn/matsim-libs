/* *********************************************************************** *
 * project: org.matsim.*
 * MyRuns.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.marcel;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.matsim.analysis.CalcAverageTolledTripLength;
import org.matsim.analysis.CalcAverageTripLength;
import org.matsim.analysis.CalcLegTimes;
import org.matsim.analysis.CalcLinkStats;
import org.matsim.analysis.LegHistogram;
import org.matsim.analysis.StuckVehStats;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.config.Config;
import org.matsim.config.ConfigWriter;
import org.matsim.controler.ScenarioData;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.deqsim.EventsReaderDEQv1;
import org.matsim.events.Events;
import org.matsim.events.MatsimEventsReader;
import org.matsim.events.algorithms.CalcLegNumber;
import org.matsim.events.algorithms.CalcODMatrices;
import org.matsim.events.algorithms.EventWriterTXT;
import org.matsim.events.algorithms.GenerateRealPlans;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.gbl.Gbl;
import org.matsim.matrices.Matrices;
import org.matsim.matrices.MatricesWriter;
import org.matsim.matrices.Matrix;
import org.matsim.matrices.MatsimMatricesReader;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.network.Link;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkWriter;
import org.matsim.network.Node;
import org.matsim.network.algorithms.NetworkAlgorithm;
import org.matsim.network.algorithms.NetworkCalcLanes;
import org.matsim.network.algorithms.NetworkCleaner;
import org.matsim.network.algorithms.NetworkFalsifier;
import org.matsim.network.algorithms.NetworkSummary;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansReaderI;
import org.matsim.plans.PlansReaderKutter;
import org.matsim.plans.PlansWriter;
import org.matsim.plans.Route;
import org.matsim.plans.algorithms.ActLocationFalsifier;
import org.matsim.plans.algorithms.PersonAlgorithm;
import org.matsim.plans.algorithms.PersonFilterSelectedPlan;
import org.matsim.plans.algorithms.PersonRemoveCertainActs;
import org.matsim.plans.algorithms.PersonRemoveLinkAndRoute;
import org.matsim.plans.algorithms.PersonRemovePlansWithoutLegs;
import org.matsim.plans.algorithms.PlanAverageScore;
import org.matsim.plans.algorithms.PlanCalcType;
import org.matsim.plans.algorithms.PlanFilterActTypes;
import org.matsim.plans.algorithms.PlanSimplifyForDebug;
import org.matsim.plans.algorithms.PlanSummary;
import org.matsim.plans.algorithms.PlansCreateTripsFromODMatrix;
import org.matsim.plans.algorithms.PlansFilterArea;
import org.matsim.plans.algorithms.PlansFilterByLegMode;
import org.matsim.plans.algorithms.PlansFilterPersonHasPlans;
import org.matsim.plans.algorithms.XY2Links;
import org.matsim.plans.filters.PersonIntersectAreaFilter;
import org.matsim.replanning.modules.ReRouteLandmarks;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.router.PlansCalcRoute;
import org.matsim.router.PlansCalcRouteLandmarks;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.router.costcalculators.TravelTimeDistanceCostCalculator;
import org.matsim.router.util.PreProcessLandmarks;
import org.matsim.scoring.CharyparNagelScoringFunctionFactory;
import org.matsim.scoring.EventsToScore;
import org.matsim.trafficmonitoring.TravelTimeCalculatorArray;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.geometry.CoordinateTransformationI;
import org.matsim.utils.geometry.shared.Coord;
import org.matsim.utils.geometry.transformations.CH1903LV03toWGS84;
import org.matsim.utils.geometry.transformations.GK4toWGS84;
import org.matsim.utils.misc.Time;
import org.matsim.utils.vis.kml.ColorStyle;
import org.matsim.utils.vis.kml.Document;
import org.matsim.utils.vis.kml.Feature;
import org.matsim.utils.vis.kml.Folder;
import org.matsim.utils.vis.kml.Geometry;
import org.matsim.utils.vis.kml.KML;
import org.matsim.utils.vis.kml.KMLWriter;
import org.matsim.utils.vis.kml.KMZWriter;
import org.matsim.utils.vis.kml.LineString;
import org.matsim.utils.vis.kml.LineStyle;
import org.matsim.utils.vis.kml.MultiGeometry;
import org.matsim.utils.vis.kml.Placemark;
import org.matsim.utils.vis.kml.Point;
import org.matsim.utils.vis.kml.PolyStyle;
import org.matsim.utils.vis.kml.Style;
import org.matsim.utils.vis.kml.TimeSpan;
import org.matsim.utils.vis.kml.fields.Color;
import org.matsim.utils.vis.matsimkml.MatsimKMLLogo;
import org.matsim.visum.VisumAnbindungstabelleWriter;
import org.matsim.visum.VisumMatrixReader;
import org.matsim.visum.VisumMatrixWriter;
import org.matsim.visum.VisumWriteRoutes;
import org.matsim.world.Location;
import org.matsim.world.MatsimWorldReader;
import org.matsim.world.World;
import org.matsim.world.ZoneLayer;

import playground.marcel.ptnetwork.PtNetworkLayer;
import playground.marcel.ptnetwork.PtNode;

public class MyRuns {

	//////////////////////////////////////////////////////////////////////
	// createKutterPlans
	//////////////////////////////////////////////////////////////////////

	public static void createKutterPlans(final String[] args) {

		System.out.println("RUN: createKutterPlans");
		System.out.println("---   create plans from data from kutter-model");
		final Config config = Gbl.createConfig(args);

		final World world = Gbl.getWorld();

		System.out.println("  reading world xml file... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(world);
		worldReader.readFile(config.world().getInputFile());
		System.out.println("  done.");

		System.out.println("  setting up plans object... ");
		final Plans plans = new Plans(Plans.USE_STREAMING);
		plans.setRefLayer(world.getLayer(new IdImpl("tvz")));
		final PlansWriter plansWriter = new PlansWriter(plans);
		plans.setPlansWriter(plansWriter);
		System.out.println("  done.");

		System.out.println("  reading kutter data, creating plans... ");
		final PlansReaderKutter plansReader = new PlansReaderKutter(plans);
		plansReader.readFile(config.getParam("kutter", "inputDirectory"));
		System.out.println("  done.");

		System.out.println("  writing plans xml file... ");
		plansWriter.write();
		System.out.println("  done.");

		System.out.println("  writing config xml file... ");
		final ConfigWriter config_writer = new ConfigWriter(config);
		config_writer.write();
		System.out.println("  done.");

		System.out.println("RUN: createKutterPlans finished.");
		System.out.println();
	}


	//////////////////////////////////////////////////////////////////////
	// fmaToTrips
	//////////////////////////////////////////////////////////////////////

	public static void fmaToTrips(final String[] args) {

		System.out.println("RUN: fmaToTrips");

		final Config config = Gbl.createConfig(args);
		final World world = Gbl.getWorld();

		System.out.println("  reading world xml file... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(world);
		worldReader.readFile(config.world().getInputFile());
		System.out.println("  done.");

		System.out.println("  setting up plans objects...");
		final Plans plans = new Plans(Plans.USE_STREAMING);
		final PlansWriter plansWriter = new PlansWriter(plans);
		plans.setPlansWriter(plansWriter);
		System.out.println("  done.");

		System.out.println("  reading matrices file... ");
		final Matrix matrix = new VisumMatrixReader("test", world.getLayer(new IdImpl("tvz"))).readFile(config.matrices().getInputFile());
		System.out.println("  done.");

		final ArrayList<Double> timeDistro = new TimeDistributionReader().readFile(config.getParam("plans", "timeDistributionInputFile"));

		System.out.println("  writing plans (trips) based on matrix...");
		final PlansCreateTripsFromODMatrix tripsmaker = new PlansCreateTripsFromODMatrix(matrix, timeDistro);
		tripsmaker.run(plans);
		plans.printPlansCount();
		plansWriter.write();
		System.out.println("  done.");

		System.out.println("RUN: fmaToTrips finished.");
		System.out.println();
	}


	//////////////////////////////////////////////////////////////////////
	// convertPlans
	//////////////////////////////////////////////////////////////////////
	// reads and writes plans, without running any algorithms

	public static void convertPlans(final String[] args) {

		System.out.println("RUN: convertPlans");

		final Config config = Gbl.createConfig(args);
		final World world = Gbl.getWorld();

		System.out.println("  reading world xml file... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(world);
		worldReader.readFile(config.world().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading the network...");
		NetworkLayer network = null;
		network = (NetworkLayer)world.createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading facilities xml file... ");
		Facilities facilities = (Facilities)world.createLayer(Facilities.LAYER_TYPE, null);
		new MatsimFacilitiesReader(facilities).readFile(config.facilities().getInputFile());
		System.out.println("  done.");

		System.out.println("  setting up plans objects...");
		final Plans plans = new Plans(Plans.USE_STREAMING);
		final PlansWriter plansWriter = new PlansWriter(plans);
		plans.setPlansWriter(plansWriter);
		PlansReaderI plansReader = new MatsimPlansReader(plans);
		System.out.println("  done.");

		System.out.println("  reading and writing plans...");
		plansReader.readFile(config.plans().getInputFile());
		plans.printPlansCount();
		plansWriter.write();
		System.out.println("  done.");

		System.out.println("RUN: convertPlans finished.");
		System.out.println();
	}

	public static void readPlans(final String[] args) {

		System.out.println("RUN: readPlans");

		final Config config = Gbl.createConfig(args);
		final ScenarioData data = new ScenarioData(config);

		System.out.println("  reading world, facilities and network ... ");
		data.getWorld();
		data.getFacilities();
		data.getNetwork();
		System.out.println("  done.");

		System.out.println("  setting up plans objects...");
		final Plans plans = new Plans(Plans.NO_STREAMING);
		PlansReaderI plansReader = new MatsimPlansReader(plans);
		System.out.println("  done.");

		System.gc();System.gc();System.gc();
		System.out.println("  memory used: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())/1024.0);

		System.out.println("  reading plans...");
		System.out.flush();
		final long startTime = System.currentTimeMillis();
		plansReader.readFile(config.plans().getInputFile());
		final long stopTime = System.currentTimeMillis();
		System.out.println("  done.");
		plans.printPlansCount();

		System.gc();System.gc();System.gc();
		System.out.println("  memory used: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())/1024.0);
		System.out.println("  time used: " + (stopTime - startTime));

		System.out.println("RUN: readPlans finished.");
		System.out.println();
	}


	//////////////////////////////////////////////////////////////////////
	// filterSelectedPlans
	//////////////////////////////////////////////////////////////////////

	public static void filterSelectedPlans(final String[] args) {

		System.out.println("RUN: filterSelectedPlans");

		final Config config = Gbl.createConfig(args);
		final World world = Gbl.getWorld();

		System.out.println("  reading world xml file... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(world);
		worldReader.readFile(config.world().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading the network...");
		NetworkLayer network = null;
		network = (NetworkLayer)world.createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		System.out.println("  done.");

		System.out.println("  setting up plans objects...");
		final Plans plans = new Plans(Plans.USE_STREAMING);
		plans.addAlgorithm(new PersonFilterSelectedPlan());
		final PlansWriter plansWriter = new PlansWriter(plans);
		plans.setPlansWriter(plansWriter);
		PlansReaderI plansReader = new MatsimPlansReader(plans);
		System.out.println("  done.");

		System.out.println("  reading and writing plans...");
		plansReader.readFile(config.plans().getInputFile());
		plans.printPlansCount();
		plansWriter.write();
		System.out.println("  done.");

		System.out.println("RUN: filterSelectedPlans finished.");
		System.out.println();
	}


	//////////////////////////////////////////////////////////////////////
	// filterPlansInArea
	//////////////////////////////////////////////////////////////////////

	public static void filterPlansInArea(final String[] args, final double x1, final double y1, final double x2, final double y2) {

		System.out.println("RUN: filterPlansInArea");

		final Config config = Gbl.createConfig(args);
		final World world = Gbl.getWorld();

		System.out.println("  reading world xml file... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(world);
		worldReader.readFile(config.world().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading the network...");
		NetworkLayer network = null;
		network = (NetworkLayer)world.createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		System.out.println("  done.");

		final Plans plans = new Plans(Plans.NO_STREAMING);

		System.out.println("  reading plans...");
		PlansReaderI plansReader = new MatsimPlansReader(plans);
		plansReader.readFile(config.plans().getInputFile());
		plans.printPlansCount();
		System.out.println("  done.");

		final Coord minCoord = new Coord(Math.min(x1, x2), Math.min(y1, y2));
		final Coord maxCoord = new Coord(Math.max(x1, x2), Math.max(y1, y2));
		plans.addAlgorithm(new PlansFilterArea(minCoord, maxCoord)); // needs PLANS.NO_STREAMING
		plans.runAlgorithms();

		System.out.println("  writing plans...");
		final PlansWriter plansWriter = new PlansWriter(plans);
		plansWriter.write();
		System.out.println("  done.");

		System.out.println("RUN: filterPlansInArea finished.");
		System.out.println();
	}

	//////////////////////////////////////////////////////////////////////
	// filterPlansInNetworkArea
	//////////////////////////////////////////////////////////////////////
	public static void filterPlansWithRouteInArea(final String[] args, final double x, final double y, final double radius) {
		System.out.println("RUN: filterPlansWithRouteInArea");

		final Coord center = new Coord(x, y);
		final Map<Id, Link> areaOfInterest = new HashMap<Id, Link>();

		final Config config = Gbl.createConfig(args);
		final World world = Gbl.getWorld();

		System.out.println("  reading world xml file... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(world);
		worldReader.readFile(config.world().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading the network..." + (new Date()));
		NetworkLayer network = null;
		network = (NetworkLayer)world.createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		System.out.println("  done.");

		System.out.println("  extracting aoi... at " + (new Date()));
		for (Link link : network.getLinks().values()) {
			final Node from = link.getFromNode();
			final Node to = link.getToNode();
			if ((from.getCoord().calcDistance(center) <= radius) || (to.getCoord().calcDistance(center) <= radius)) {
				System.out.println("    link " + link.getId().toString());
				areaOfInterest.put(link.getId(),link);
			}
		}
		System.out.println("  done. ");
		System.out.println("  aoi contains: " + areaOfInterest.size() + " links.");

		System.out.println("  reading, filtering and writing population... at " + (new Date()));
		final Plans population = new Plans(Plans.USE_STREAMING);

		PlansReaderI plansReader = new MatsimPlansReader(population);
		final PlansWriter plansWriter = new PlansWriter(population);
		plansWriter.writeStartPlans();
		final PersonIntersectAreaFilter filter = new PersonIntersectAreaFilter(plansWriter, areaOfInterest);
		filter.setAlternativeAOI(center, radius);
		population.addAlgorithm(filter);

		plansReader.readFile(config.plans().getInputFile());

		plansWriter.writeEndPlans();
		population.printPlansCount();
		System.out.println("  done. " + (new Date()));
		System.out.println("  filtered persons: " + filter.getCount());

		System.out.println("RUN: filterPlansWithRouteInArea finished");
	}

	//////////////////////////////////////////////////////////////////////
	// filterPlansInNetworkArea
	//////////////////////////////////////////////////////////////////////

	public static void filterPlansInNetworkArea(final String[] args) {

		System.out.println("RUN: filterPlansInNetworkArea");

		final Config config = Gbl.createConfig(args);
		final World world = Gbl.getWorld();

		System.out.println("  reading world xml file... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(world);
		worldReader.readFile(config.world().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading the network...");
		NetworkLayer network = null;
		network = (NetworkLayer)world.createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		System.out.println("  done.");

		final NetworkSummary summary = new NetworkSummary();
		summary.run(network);

		final Plans plans = new Plans(Plans.NO_STREAMING);

		System.out.println("  reading plans...");
		PlansReaderI plansReader = new MatsimPlansReader(plans);
		plansReader.readFile(config.plans().getInputFile());
		plans.printPlansCount();
		System.out.println("  done.");

		final Coord minCoord = summary.getMinCoord();
		final Coord maxCoord = summary.getMaxCoord();
		plans.addAlgorithm(new PlansFilterArea(minCoord, maxCoord));
		plans.runAlgorithms();

		System.out.println("  writing plans...");
		final PlansWriter plansWriter = new PlansWriter(plans);
		plansWriter.write();
		System.out.println("  done.");

		System.out.println("RUN: filterPlansInNetworkArea finished.");
		System.out.println();
	}


	public static void filterPlansPassingLink(final String[] args, final int linkId) {

		System.out.println("RUN: filterPlansPassingLink");

		final Config config = Gbl.createConfig(args);
		final World world = Gbl.getWorld();

		System.out.println("  reading world xml file... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(world);
		worldReader.readFile(config.world().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading the network...");
		NetworkLayer network = null;
		network = (NetworkLayer)world.createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading plans...");
		final Plans plans = new Plans(Plans.NO_STREAMING);
		PlansReaderI plansReader = new MatsimPlansReader(plans);
		plansReader.readFile(config.plans().getInputFile());
		plans.printPlansCount();
		System.out.println("  done.");

		System.out.println("  searching link...");
		final Link link = network.getLinks().get(Integer.toString(linkId));
		final Node fromNode = link.getFromNode();
		final Node toNode = link.getToNode();
		System.out.println("  done.");
		System.out.println("  link " + link.getId()
				+ " starts at node " + fromNode.getId()
				+ " and ends at node " + toNode.getId());

		System.out.println("  filtering plans...");
		final ArrayList<Person> removeAfterwards = new ArrayList<Person>();
		for (final Person person : plans.getPersons().values()) {
			boolean passesLink = false;
			for (final Plan plan : person.getPlans()) {
				for (int i = 1, max = plan.getActsLegs().size(); i < max; i +=2) {
					final Leg leg = (Leg)plan.getActsLegs().get(i);
					final Route route = leg.getRoute();
					final ArrayList<Node> nodes = route.getRoute();
					final int fromNodeIdx = nodes.indexOf(fromNode);
					final int toNodeIdx = nodes.indexOf(toNode);
					if (toNodeIdx == fromNodeIdx + 1) {
						passesLink = true;
						break;
					}
				}
				if (passesLink) {
					break;
				}
			}
			if (!passesLink) {
				removeAfterwards.add(person);
			}
		}
		for (final Person person : removeAfterwards) {
			plans.getPersons().remove(person.getId());
		}
		System.out.println("  done.");
		System.out.println(removeAfterwards.size() + " persons removed.");

		System.out.println("  writing plans...");
		final PlansWriter plansWriter = new PlansWriter(plans);
		plansWriter.write();
		System.out.println("  done.");

		System.out.println("RUN: filterPlansPassingLink finished.");
		System.out.println();
	}

	//////////////////////////////////////////////////////////////////////
	// filterCars
	//////////////////////////////////////////////////////////////////////

	public static void filterCars(final String[] args) {

		System.out.println("RUN: filterCars");

		final Config config = Gbl.createConfig(args);
		final World world = Gbl.getWorld();

		System.out.println("  reading world xml file... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(world);
		worldReader.readFile(config.world().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading the network...");
		NetworkLayer network = null;
		network = (NetworkLayer)world.createLayer(NetworkLayer.LAYER_TYPE,null);
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		System.out.println("  done.");

		System.out.println("  setting up plans objects...");
		final Plans plans = new Plans(Plans.NO_STREAMING);
		PlansReaderI plansReader = new MatsimPlansReader(plans);
		System.out.println("  done.");

		System.out.println("  adding plans algorithm... ");
		plans.addAlgorithm(new PlansFilterByLegMode("car", false));
		System.out.println("  done.");

		System.out.println("  reading plans...");
		plansReader.readFile(config.plans().getInputFile());
		plans.printPlansCount();
		System.out.println("  done;");

		System.out.println("  processing plans...");
		plans.runAlgorithms();
		System.out.println("  done;");

		System.out.println("  writing plans...");
		final PlansWriter plansWriter = new PlansWriter(plans);
		plansWriter.write();
		System.out.println("  done.");

		System.out.println("RUN: filterCars finished.");
		System.out.println();
	}


	//////////////////////////////////////////////////////////////////////
	// filterPt
	//////////////////////////////////////////////////////////////////////

	public static void filterPt(final String[] args) {

		System.out.println("RUN: filterPt");

		final Config config = Gbl.createConfig(args);
		final World world = Gbl.getWorld();

		System.out.println("  reading world xml file... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(world);
		worldReader.readFile(config.world().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading the network...");
		NetworkLayer network = null;
		network = (NetworkLayer)world.createLayer(NetworkLayer.LAYER_TYPE,null);
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		System.out.println("  done.");

		System.out.println("  setting up plans objects...");
		final Plans plans = new Plans(Plans.NO_STREAMING);
		PlansReaderI plansReader = new MatsimPlansReader(plans);
		System.out.println("  done.");

		System.out.println("  adding plans algorithm... ");
		plans.addAlgorithm(new PlansFilterByLegMode("pt", true));
		System.out.println("  done.");

		System.out.println("  reading plans...");
		plansReader.readFile(config.plans().getInputFile());
		plans.printPlansCount();
		System.out.println("  done;");

		System.out.println("  processing plans...");
		plans.runAlgorithms();
		System.out.println("  done;");

		System.out.println("  writing plans...");
		final PlansWriter plansWriter = new PlansWriter(plans);
		plansWriter.write();
		System.out.println("  done.");

		System.out.println("RUN: filterPt finished.");
		System.out.println();
	}


	//////////////////////////////////////////////////////////////////////
	// filterWork
	//////////////////////////////////////////////////////////////////////

	public static void filterWork(final String[] args) {

		System.out.println("RUN: filterWork");

		final Config config = Gbl.createConfig(args);
		final World world = Gbl.getWorld();

		System.out.println("  reading world xml file... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(world);
		worldReader.readFile(config.world().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading the network...");
		NetworkLayer network = null;
		network = (NetworkLayer)world.createLayer(NetworkLayer.LAYER_TYPE,null);
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		System.out.println("  done.");

		System.out.println("  setting up plans objects...");
		final Plans plans = new Plans(Plans.NO_STREAMING);
		PlansReaderI plansReader = new MatsimPlansReader(plans);
		System.out.println("  done.");

		System.out.println("  adding plans algorithm... ");
		plans.addAlgorithm(new PersonRemoveCertainActs());
		plans.addAlgorithm(new PersonRemovePlansWithoutLegs());
		plans.addAlgorithm(new PlansFilterPersonHasPlans());
		System.out.println("  done.");

		System.out.println("  reading, processing, writing plans...");
		plansReader.readFile(config.plans().getInputFile());
		plans.printPlansCount();
		plans.runAlgorithms();
		final PlansWriter plansWriter = new PlansWriter(plans);
		plansWriter.write();
		System.out.println("  done.");

		System.out.println("RUN: filterWork finished.");
		System.out.println();
	}

	public static void filterWorkEdu(final String[] args) {
		System.out.println("RUN: filterWorkEdu");

		final Config config = Gbl.createConfig(args);
		final World world = Gbl.getWorld();

		System.out.println("  reading world xml file... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(world);
		worldReader.readFile(config.world().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading the network...");
		NetworkLayer network = null;
		network = (NetworkLayer)world.createLayer(NetworkLayer.LAYER_TYPE,null);
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		System.out.println("  done.");

		System.out.println("  setting up plans objects...");
		final Plans plans = new Plans(Plans.NO_STREAMING);
		PlansReaderI plansReader = new MatsimPlansReader(plans);
		System.out.println("  done.");

		System.out.println("  adding plans algorithm... ");
		plans.addAlgorithm(new PlanFilterActTypes(new String[] {"work1", "work2", "work3", "edu", "uni"}));
		plans.addAlgorithm(new PlansFilterPersonHasPlans());
		System.out.println("  done.");

		System.out.println("  reading, processing, writing plans...");
		plansReader.readFile(config.plans().getInputFile());
		plans.printPlansCount();
		plans.runAlgorithms();
		final PlansWriter plansWriter = new PlansWriter(plans);
		plansWriter.write();
		System.out.println("  done.");

		System.out.println("RUN: filterWorkEdu finished.");
		System.out.println();
	}

	/**
	 * reads in a population, removes all non-work and non-edu/non-uni activities
	 * ensures that at most one work or edu activity exists in a plan, and removes
	 * all plans from a person not having one work or edu activity. All person
	 * with at least one plan remaining are written out again. This means: in the
	 * output population, all plans are simple hwh or heh plans, no hwwh or
	 * similar; all plans will have exactly three activities, and every activity
	 * takes exactly 8 hours. All output plans have valid routes.
	 */
	public static void createDebugPlans(final String[] args) {
		System.out.println("RUN: createDebugPlans");

		final Config config = Gbl.createConfig(args);
		final World world = Gbl.getWorld();

		System.out.println("  reading world xml file... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(world);
		worldReader.readFile(config.world().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading the network...");
		NetworkLayer network = null;
		network = (NetworkLayer)world.createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		System.out.println("  done.");

		System.out.println("  setting up plans objects...");
		final Plans population = new Plans(Plans.NO_STREAMING);
		final PlansReaderI plansReader = new MatsimPlansReader(population);
		System.out.println("  done.");

		System.out.println("  adding plans algorithm... ");
		population.addAlgorithm(new PlanSimplifyForDebug(network));
		population.addAlgorithm(new PlansFilterPersonHasPlans());
		System.out.println("  done.");

		System.out.println("  reading plans...");
		plansReader.readFile(config.plans().getInputFile());
		population.printPlansCount();

		System.out.println("  processing plans...");
		population.runAlgorithms();

		System.out.println("  writing plans...");
		final PlansWriter plansWriter = new PlansWriter(population);
		plansWriter.write();
		System.out.println("  done.");

		System.out.println("RUN: createDebugPlans finished.");
		System.out.println();
	}


	//////////////////////////////////////////////////////////////////////
	// removeLinkAndRoute
	//////////////////////////////////////////////////////////////////////

	public static void removeLinkAndRoute(final String[] args) {

		System.out.println("RUN: removeLinkAndRoute");

		final Config config = Gbl.createConfig(args);
		final World world = Gbl.getWorld();

		System.out.println("  reading world xml file... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(world);
		worldReader.readFile(config.world().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading the network...");
		NetworkLayer network = null;
		network = (NetworkLayer)world.createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		System.out.println("  done.");

		System.out.println("  setting up plans objects...");
		final Plans plans = new Plans(Plans.USE_STREAMING);
		final PlansWriter plansWriter = new PlansWriter(plans);
		plans.setPlansWriter(plansWriter);
		final PlansReaderI plansReader = new MatsimPlansReader(plans);
		System.out.println("  done.");

		System.out.println("  adding plans algorithm... ");
		plans.addAlgorithm(new PersonRemoveLinkAndRoute());
		System.out.println("  done.");

		System.out.println("  reading, processing, writing plans...");
		plansReader.readFile(config.plans().getInputFile());
		plansWriter.write();
		System.out.println("  done.");

		System.out.println("RUN: removeLinkAndRoute finished.");
		System.out.println();
	}

	//////////////////////////////////////////////////////////////////////
	// calcRoute
	//////////////////////////////////////////////////////////////////////

	public static void calcRoute(final String[] args) {

		System.out.println("RUN: calcRoute");

		final Config config = Gbl.createConfig(args);
		final World world = Gbl.getWorld();

		System.out.println("  reading world xml file... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(world);
		worldReader.readFile(config.world().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading the network...");
		NetworkLayer network = null;
		network = (NetworkLayer)world.createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading facilities xml file... ");
		Facilities facilities = (Facilities)world.createLayer(Facilities.LAYER_TYPE, null);
		new MatsimFacilitiesReader(facilities).readFile(config.facilities().getInputFile());
		System.out.println("  done.");

		System.out.println("  setting up plans objects...");
		final Plans plans = new Plans(Plans.NO_STREAMING);
		final PlansWriter plansWriter = new PlansWriter(plans);
		plans.setPlansWriter(plansWriter);
		final PlansReaderI plansReader = new MatsimPlansReader(plans);
		System.out.println("  done.");

		System.out.println("  adding plans algorithm... ");
		final FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost();
		PreProcessLandmarks preprocess = new PreProcessLandmarks(timeCostCalc);
		preprocess.run(network);
		plans.addAlgorithm(new PlansCalcRouteLandmarks(network, preprocess, timeCostCalc, timeCostCalc));
		System.out.println("  done.");

		System.out.println("  reading plans...");
		plansReader.readFile(config.plans().getInputFile());
		plans.printPlansCount();
		System.out.println("  done.");

		System.out.println("  processing plans..." + (new Date()));
		plans.runAlgorithms();
		System.out.println("  done. " + (new Date()));

		System.out.println("  writing plans...");
		plansWriter.write();
		System.out.println("  done.");

		System.out.println("RUN: calcRoute finished.");
		System.out.println();
	}

	/**
	 * Calculate routes, multithreaded! This requires all plans to be read into
	 * memory, so beware!
	 *
	 * @param args arguments for the program, mainly config-file and dtd
	 */
	public static void calcRouteMTwithTimes(final String[] args) {

		System.out.println("RUN: calcRouteMTwithTimes");

		Config config = Gbl.createConfig(args);
		World world = Gbl.getWorld();

		System.out.println("  reading world... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(world);
		worldReader.readFile(config.world().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading the network...");
		NetworkLayer network = null;
		network = (NetworkLayer)world.createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading facilities... ");
		Facilities facilities = (Facilities)world.createLayer(Facilities.LAYER_TYPE, null);
		new MatsimFacilitiesReader(facilities).readFile(config.facilities().getInputFile());
		System.out.println("  done.");

		System.out.println("  setting up plans objects...");
		final Plans population = new Plans(Plans.NO_STREAMING);
		System.out.println("  done.");

		System.out.println("  reading plans...");
		final PlansReaderI plansReader = new MatsimPlansReader(population);
		plansReader.readFile(config.plans().getInputFile());
		population.printPlansCount();
		System.out.println("  done.");

		Gbl.startMeasurement();
		System.out.println("  reading events, calculating travel times...");
		final Events events = new Events();
		final TravelTimeCalculatorArray ttime = new TravelTimeCalculatorArray(network, 15*60);
		events.addHandler(ttime);
		new MatsimEventsReader(events).readFile(config.events().getInputFile());
		events.printEventsCount();
		System.out.println("  done.");
		Gbl.printElapsedTime();

		Gbl.startMeasurement();
		System.out.println("  processing plans, calculating routes...");
		PreProcessLandmarks preProcessRoutingData = new PreProcessLandmarks(new FreespeedTravelTimeCost());
		preProcessRoutingData.run(network);
		final ReRouteLandmarks reroute = new ReRouteLandmarks(network, new TravelTimeDistanceCostCalculator(ttime), ttime, preProcessRoutingData);
		reroute.init();
		for (final Person person : population.getPersons().values()) {
			for (final Plan plan : person.getPlans()) {
				reroute.handlePlan(plan);
			}
		}
		reroute.finish();
		System.out.println("  done.");
		Gbl.printElapsedTime();

		System.out.println("  writing plans...");
		final PlansWriter plansWriter = new PlansWriter(population);
		plansWriter.write();
		System.out.println("  done.");

		System.out.println("RUN: calcRouteMTwithTimes finished.");
		System.out.println();
	}

	//////////////////////////////////////////////////////////////////////
	// calcRoutePt -- Public Transport
	//////////////////////////////////////////////////////////////////////

	public static void calcRoutePt(final String[] args) {

		System.out.println("RUN: calcRoutePt");

		final Config config = Gbl.createConfig(args);
		final World world = Gbl.getWorld();

		System.out.println("  reading world xml file... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(world);
		worldReader.readFile(config.world().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading the network...");
		final PtNetworkLayer ptNetwork = new PtNetworkLayer();
		ptNetwork.buildfromBERTA(config.getParam("network", "inputBvgDataDir"),
				config.getParam("network", "inputBvgDataCoords"), true,
				config.getParam("network", "inputSBahnNet"));
		ptNetwork.handleDeadEnds(ptNetwork.getDeadEnds());


//		PtInternalNetwork intNetwork = new PtInternalNetwork();
//		intNetwork.buildInternalNetwork(Gbl.getConfig().getParam("network", "inputBvgDataDir"));
//		intNetwork.readCoordFileByCoord(Gbl.getConfig().getParam("network", "inputBvgDataCoords"));
//		intNetwork.createHbs();
//		intNetwork.writeToNetworkLayer(ptNetwork);

//		intNetwork.writeToVISUM("testnet.net");
//		PtInternalNetwork int2 = new PtInternalNetwork();
//		int2.parseVISUMNetwork("testnet.net");
//		int2.writeToNetworkLayer(ptNetwork);

		System.out.println("  done.");

		final String filename = config.findParam("network", "outputPtNetworkFile");
		if (filename != null) {
			System.out.println("  writing network xml file to " + filename + "... ");
			final NetworkWriter network_writer = new NetworkWriter(ptNetwork, filename);
			network_writer.write();
			System.out.println("  done");
		}

		System.out.println("  reading plans...");
		final Plans plans = new Plans(Plans.NO_STREAMING);
		final PlansWriter plansWriter = new PlansWriter(plans);
		plans.setPlansWriter(plansWriter);
		final PlansReaderI plansReader = new MatsimPlansReader(plans);
		plansReader.readFile(config.plans().getInputFile());
		plans.printPlansCount();
		System.out.println("  done.");

		System.out.println("  processing plans... at " + (new Date()));
		int counter = 0;
		int nextCount = 1;
		final int radius = 250;
		for (final Person person : plans.getPersons().values()) {
			counter++;
			if (counter == nextCount) {
				nextCount *= 2;
				System.out.println("plan # " + counter);
			}
			final Plan plan = person.getPlans().get(0);
			CoordI depCoord = ((Act)plan.getActsLegs().get(0)).getCoord();
			for (int i = 2, maxi = plan.getActsLegs().size(); i < maxi; i = i + 2) {
				final Leg leg = ((Leg)plan.getActsLegs().get(i-1));
				final CoordI arrCoord = ((Act)plan.getActsLegs().get(i)).getCoord();
				final double depTime = leg.getDepTime();
				try {
					final Route route = ptNetwork.dijkstraGetCheapestRoute(depCoord, arrCoord, depTime, radius);
					leg.setRoute(route);
				} catch (final RuntimeException e) {
					System.err.println("error while handling plan " + 0 + " of person " + person.getId());
					e.printStackTrace();
				}
				depCoord = arrCoord;
			}
		}
		System.out.println("  done. at " + (new Date()));

		System.out.println("  writing plans...");
		plansWriter.write();
		System.out.println("  done.");

		System.out.println("RUN: calcRoutePt finished.");
		System.out.println();
	}



	//////////////////////////////////////////////////////////////////////
	// testPt -- Public Transport
	//////////////////////////////////////////////////////////////////////

	public static void testPt(final String[] args) {

		System.out.println("RUN: testPt");

		final Config config = Gbl.createConfig(args);
		final World world = Gbl.getWorld();

		System.out.println("  reading world xml file... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(world);
		worldReader.readFile(config.world().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading the network...");
		final PtNetworkLayer ptNetwork = new PtNetworkLayer();
		ptNetwork.buildfromBERTA(config.getParam("network", "inputBvgDataDir"),
				config.getParam("network", "inputBvgDataCoords"), true,
				config.getParam("network", "inputSBahnNet"));
//		ptNetwork.handleDeadEnds(ptNetwork.getDeadEnds());
//		PtInternalNetwork intNetwork = new PtInternalNetwork();
//		intNetwork.buildInternalNetwork(Gbl.getConfig().getParam("network", "inputBvgDataDir"));
//		intNetwork.readCoordFileByCoord(Gbl.getConfig().getParam("network", "inputBvgDataCoords"));
//		intNetwork.createHbs();
//		intNetwork.writeToNetworkLayer(ptNetwork);

//		intNetwork.writeToVISUM("testnet.net");
//		PtInternalNetwork int2 = new PtInternalNetwork();
//		int2.parseVISUMNetwork("testnet.net");
//		int2.writeToNetworkLayer(ptNetwork);

		System.out.println("  done.");
		final String pedNodeType = "P";
		for (final Iterator<? extends Node> iter = ptNetwork.getNodes().values().iterator(); iter.hasNext(); ) {
			final PtNode node = (PtNode) iter.next();
			if (pedNodeType.equals(node.getType())) {
				if (node.getIncidentLinks().size() == 2) {
					final PtNode node2 = (PtNode)node.getOutNodes().values().iterator().next();
					if (node2.getIncidentLinks().size() == 3) {
						System.out.println("HP " + node2.getId() + " (part of HB " + node.getId() + ") is a dead end.");
					}
				}
			}
		}

		final String filename = config.findParam("network", "outputPtNetworkFile");
		if (filename != null) {
			System.out.println("  writing network xml file to " + filename + "... ");
			final NetworkWriter network_writer = new NetworkWriter(ptNetwork, filename);
			network_writer.write();
			System.out.println("  done");
		}

		System.out.println("RUN: testPt finished.");
		System.out.println();
	}

	static public void calcRealPlans(final String[] args) {

		System.out.println("RUN: calcRealPlans");

		final Config config = Gbl.createConfig(args);
		final World world = Gbl.getWorld();

		System.out.println("  reading world... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(world);
		worldReader.readFile(config.world().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading the network...");
		NetworkLayer network = null;
		network = (NetworkLayer)world.createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		System.out.println("  done.");

		System.out.println("  setting up events objects... ");
		final Events events = new Events();
		final GenerateRealPlans algo = new GenerateRealPlans();
		events.addHandler(algo);
		System.out.println("  done.\n");

		System.out.println("  reading events file and (probably) running events algos");
		new MatsimEventsReader(events).readFile(config.events().getInputFile());
		System.out.println("  done.\n");

		System.out.println("  writing plans...");
		final PlansWriter plansWriter = new PlansWriter(algo.getPlans());
		plansWriter.write();
		System.out.println("  done.");

		System.out.println("RUN: calcRealPlans finished.");
		System.out.println();
	}


	//////////////////////////////////////////////////////////////////////
	// calcScoreFromEvents
	//////////////////////////////////////////////////////////////////////

	public static void calcScoreFromEvents_old(final String[] args) {

		System.out.println("RUN: calcScoreFromEvents_old");

		final Config config = Gbl.createConfig(args);
		final World world = Gbl.getWorld();

		System.out.println("  reading world xml file... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(world);
		worldReader.readFile(config.world().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading the network...");
		NetworkLayer network = null;
		network = (NetworkLayer)world.createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading plans...");
		final Plans population = new Plans(Plans.NO_STREAMING);
		final PlansReaderI plansReader = new MatsimPlansReader(population);
		plansReader.readFile(config.plans().getInputFile());
		population.printPlansCount();
		System.out.println("  done.");

		System.out.println("  creating events object... ");
		final Events events = new Events();
		final EventsToScore scoring = new EventsToScore(population, new CharyparNagelScoringFunctionFactory());
		events.addHandler(scoring);
		System.out.println("  done.");

		System.out.println("  reading events..." + (new Date()));
		new MatsimEventsReader(events).readFile(config.events().getInputFile());
		events.printEventsCount();
		System.out.println("  done.");

		System.out.println("  calculating score... " + (new Date()));
		scoring.finish();
		final PlanAverageScore average = new PlanAverageScore();
		average.run(population);
		System.out.println("    average score = " + average.getAverage());
		System.out.println("  done. " + (new Date()));

		System.out.println("  writing plans...");
		population.runAlgorithms();
		final PlansWriter plansWriter = new PlansWriter(population);
		plansWriter.write();
		System.out.println("  done.");

		System.out.println("RUN: calcScoreFromEvents_old finished.");
		System.out.println();
	}

	public static void calcScoreFromEvents_new(final String[] args) {

		System.out.println("RUN: calcScoreFromEvents_new");

		final Config config = Gbl.createConfig(args);
		final World world = Gbl.getWorld();

		System.out.println("  reading world xml file... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(world);
		worldReader.readFile(config.world().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading the network...");
		NetworkLayer network = null;
		network = (NetworkLayer)world.createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading plans...");
		final Plans population = new Plans(Plans.NO_STREAMING);
		final PlansReaderI plansReader = new MatsimPlansReader(population);
		plansReader.readFile(config.plans().getInputFile());
		population.printPlansCount();
		System.out.println("  done.");

		System.out.println("  creating events object... ");
		final EventsToScore calcscore = new EventsToScore(population, new CharyparNagelScoringFunctionFactory());
		final Events events = new Events();
		events.addHandler(calcscore);
		System.out.println("  done.");

		System.out.println("  reading events..." + (new Date()));
		new MatsimEventsReader(events).readFile(config.events().getInputFile());
		events.printEventsCount();
		System.out.println("  done.");

		System.out.println("  calculating score... " + (new Date()));
		final PlanAverageScore average = new PlanAverageScore();
		calcscore.finish();
		population.addAlgorithm(average);
		population.runAlgorithms();
		System.out.println("    average score = " + average.getAverage());
		System.out.println("  done."  + (new Date()));

		System.out.println("  writing plans...");
		final PlansWriter plansWriter = new PlansWriter(population);
		plansWriter.setUseCompression(true);
		plansWriter.write();
		System.out.println("  done.");

		System.out.println("RUN: calcScoreFromEvents_new finished.");
		System.out.println();
	}



	//////////////////////////////////////////////////////////////////////
	// convertNetwork
	//////////////////////////////////////////////////////////////////////

	public static void convertNetwork(final String[] args) {

		System.out.println("RUN: convertNetwork");

		Config config = Gbl.createConfig(args);

		System.out.println("  reading the network...");
		NetworkLayer network = null;
		network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		System.out.println("  done.");

//		NetworkCleaner cleaner = new NetworkCleaner(true);
//		cleaner.run(network);

		System.out.println("  writing the network...");
		final NetworkWriter network_writer = new NetworkWriter(network);
		network_writer.write();
		System.out.println("  done.");

		System.out.println("RUN: convertNetwork finished.");
		System.out.println();
	}

	//////////////////////////////////////////////////////////////////////
	// cleanNetwork
	//////////////////////////////////////////////////////////////////////

	public static void cleanNetwork(final String[] args) {

		System.out.println("RUN: cleanNetwork");

		Config config = Gbl.createConfig(args);

		System.out.println("  reading the network...");
		NetworkLayer network = null;
		network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		System.out.println("  done.");

		System.out.println("  running NetworkCleaner... ");
		new NetworkCleaner().run(network);
		System.out.println("  done.");

		System.out.println("  writing the network...");
		final NetworkWriter network_writer = new NetworkWriter(network);
		network_writer.write();
		System.out.println("  done.");

		System.out.println("RUN: cleanNetwork finished.");
		System.out.println();
	}

	//////////////////////////////////////////////////////////////////////
	// calcNofLanes
	//////////////////////////////////////////////////////////////////////

	public static void calcNofLanes(final String[] args) {

		System.out.println("RUN: calcNofLanes");

		Config config = Gbl.createConfig(args);

		System.out.println("  reading the network...");
		NetworkLayer network = null;
		network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		System.out.println("  done.");

		System.out.println("  calculating number of lanes... ");
		new NetworkCalcLanes().run(network);
		System.out.println("  done.");

		System.out.println("  writing the network...");
		final NetworkWriter network_writer = new NetworkWriter(network);
		network_writer.write();
		System.out.println("  done.");

		System.out.println("RUN: calcNofLanes finished.");
		System.out.println();
	}


	public static void falsifyNetwork(final String[] args) {

		System.out.println("RUN: falsifyNetwork");

		Config config = Gbl.createConfig(args);

		System.out.println("  reading the network...");
		NetworkLayer network = null;
		network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		System.out.println("  done.");

		(new NetworkFalsifier(50)).run(network);

		System.out.println("  writing the network...");
		final NetworkWriter network_writer = new NetworkWriter(network);
		network_writer.write();
		System.out.println("  done.");

		System.out.println("RUN: falsifyNetwork finished.");
		System.out.println();
	}
	
	//////////////////////////////////////////////////////////////////////
	// calcODMatrices
	//////////////////////////////////////////////////////////////////////

	public static void calcODMatrix(final String[] args) {

		System.out.println("RUN: calcODMatrix");

		final Config config = Gbl.createConfig(args);
		final World world = Gbl.getWorld();

		System.out.println("  reading world xml file... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(world);
		worldReader.readFile(config.world().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading the network...");
		NetworkLayer network = null;
		network = (NetworkLayer)world.createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		System.out.println("  done.");

		System.out.println("  setting up plans objects...");
		final Plans plans = new Plans(Plans.USE_STREAMING);
		final PlansReaderI plansReader = new MatsimPlansReader(plans);
		final ODMatrix calcOD = new ODMatrix("od_0-24");
		plans.addAlgorithm(calcOD);
		System.out.println("  done.");

		System.out.println("  reading, processing plans...");
		plansReader.readFile(config.plans().getInputFile());
		plans.printPlansCount();
		System.out.println("  done.");

		System.out.println("  writing matrices... ");
		final MatricesWriter matrices_writer = new MatricesWriter(Matrices.getSingleton());
		matrices_writer.write();
		System.out.println("  done.");

		System.out.println("  writing VISUM file... ");
		final ZoneLayer tvz = (ZoneLayer)world.getLayer(new IdImpl("tvz"));
		final Set<Id> ids = new TreeSet<Id>();
		for (final Iterator<? extends Location> iter = tvz.getLocations().values().iterator(); iter.hasNext(); ) {
			final Location loc = iter.next();
			ids.add(loc.getId());
		}
		final VisumMatrixWriter writer = new VisumMatrixWriter(calcOD.getMatrix());
		writer.setIds(ids);
		writer.writeFile(calcOD.getMatrix().getId() + ".fma");
		System.out.println("  done.");

		System.out.println("RUN: calcODMatrix finished.");
		System.out.println();
	}

	//////////////////////////////////////////////////////////////////////
	// calcODMatrices
	//////////////////////////////////////////////////////////////////////

	public static void calc1hODMatrices(final String[] args) {

		System.out.println("RUN: calc1hODMatrices");

		Config config = Gbl.createConfig(args);
		final World world = Gbl.getWorld();

		System.out.println("  reading world xml file... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(world);
		worldReader.readFile(config.world().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading the network...");
		NetworkLayer network = null;
		network = (NetworkLayer)world.createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		System.out.println("  done.");

		Plans population = null;

		System.out.println("  reading plans...");
		population = new Plans(Plans.NO_STREAMING);
		final PlansReaderI plansReader = new MatsimPlansReader(population);
		plansReader.readFile(config.plans().getInputFile());
		population.printPlansCount();
		System.out.println("  done.");

		// events
		System.out.println("  creating events object... ");
		final Events events = new Events();
		System.out.println("  done.");

		System.out.println("  adding events algorithms...");
		final ZoneLayer tvz = (ZoneLayer)world.getLayer(new IdImpl("tvz"));
		final ArrayList<CalcODMatrices> odcalcs = new ArrayList<CalcODMatrices>();
		for (int i = 0; i < 30; i++) {
			final CalcODMatrices odcalc = new CalcODMatrices(network, tvz, population, "od_" + i + "-" + (i + 1));
			odcalc.setTimeRange(i*3600, (i+1)*3600);
			events.addHandler(odcalc);
			odcalcs.add(odcalc);
		}
		System.out.println("  done");

		// read file, run algorithms if streaming is on
		System.out.println("  reading events file and analyzing volumes...");
		new MatsimEventsReader(events).readFile(config.events().getInputFile());
		System.out.println("  done.");

		System.out.println("  writing matrices files... ");
		final MatricesWriter matrices_writer = new MatricesWriter(Matrices.getSingleton());
		matrices_writer.write();
		final Set<Id> ids = new TreeSet<Id>();
		for (final Iterator<? extends Location> iter = tvz.getLocations().values().iterator(); iter.hasNext(); ) {
			final Location loc = iter.next();
			if (Integer.parseInt(loc.getId().toString()) < 24) {
				ids.add(loc.getId());
			}
		}
		for (final CalcODMatrices odcalc : odcalcs) {
			final String filename = odcalc.getMatrix().getId() + ".fma";
			System.out.println("    writing file: " + filename + "   (" + odcalc.counter + ")");
			final VisumMatrixWriter writer = new VisumMatrixWriter(odcalc.getMatrix());
			writer.setIds(ids);
			writer.writeFile(filename);
		}
		System.out.println("  done.");

		System.out.println("RUN: calc1hODMatrices finished.");
		System.out.println();
	}


	//////////////////////////////////////////////////////////////////////
	// calcODMatrixBezirke
	//////////////////////////////////////////////////////////////////////

	public static void calcODMatrixBezirke(final String[] args) {

		System.out.println("RUN: calcODMatrixBezirke");

		final Config config = Gbl.createConfig(args);
		final World world = Gbl.getWorld();

		System.out.println("  reading world xml file... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(world);
		worldReader.readFile(config.world().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading the network...");
		NetworkLayer network = null;
		network = (NetworkLayer)world.createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		System.out.println("  done.");

		System.out.println("  setting up plans objects...");
		final Plans plans = new Plans(Plans.USE_STREAMING);
		final PlansReaderI plansReader = new MatsimPlansReader(plans);
		final ODMatrixBezirke calcOD = new ODMatrixBezirke("od_0-24");
		plans.addAlgorithm(calcOD);
		System.out.println("  done.");

		System.out.println("  reading, processing plans...");
		plansReader.readFile(config.plans().getInputFile());
		plans.printPlansCount();
		System.out.println("  done.");

		System.out.println("  writing matrices... ");
		final MatricesWriter matrices_writer = new MatricesWriter(Matrices.getSingleton());
		matrices_writer.write();
		System.out.println("  done.");

		System.out.println("  writing VISUM file... ");
		final ZoneLayer tvz = (ZoneLayer)world.getLayer("bezirke");
		final Set<Id> ids = new TreeSet<Id>();
		for (final Iterator<? extends Location> iter = tvz.getLocations().values().iterator(); iter.hasNext(); ) {
			final Location loc = iter.next();
			if (Integer.parseInt(loc.getId().toString()) < 24) {
				ids.add(loc.getId());
			}
		}
		final VisumMatrixWriter writer = new VisumMatrixWriter(calcOD.getMatrix());
		writer.setIds(ids);
		writer.writeFile(calcOD.getMatrix().getId() + ".fma");
		System.out.println("  done.");

		System.out.println("RUN: calcODMatrixBezirke finished.");
		System.out.println();
	}

	//////////////////////////////////////////////////////////////////////
	// calc1hODMatrixBezirke
	//////////////////////////////////////////////////////////////////////

	public static void calc1hODMatricesBezirke(final String[] args) {

		System.out.println("RUN: calc1hODMatricesBezirke");

		final Config config = Gbl.createConfig(args);
		final World world = Gbl.getWorld();

		System.out.println("  reading world xml file... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(world);
		worldReader.readFile(config.world().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading the network...");
		NetworkLayer network = null;
		network = (NetworkLayer)world.createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		System.out.println("  done.");

		Plans population = null;

		System.out.println("  reading plans...");
		population = new Plans(Plans.NO_STREAMING);
		final PlansReaderI plansReader = new MatsimPlansReader(population);
		plansReader.readFile(config.plans().getInputFile());
		population.printPlansCount();
		System.out.println("  done.");

		// events
		System.out.println("  creating events object... ");
		final Events events = new Events();
		System.out.println("  done.");

		System.out.println("  adding events algorithms...");
		final ZoneLayer tvz = (ZoneLayer)world.getLayer(new IdImpl("tvz"));
		final ArrayList<CalcODMatricesBezirke> odcalcs = new ArrayList<CalcODMatricesBezirke>();
		for (int i = 0; i < 30; i++) {
			final CalcODMatricesBezirke odcalc = new CalcODMatricesBezirke(network, tvz, population, "od_" + i + "-" + (i + 1));
			odcalc.setTimeRange(i*3600, (i+1)*3600);
			events.addHandler(odcalc);
			odcalcs.add(odcalc);
		}
		System.out.println("  done");

		// read file, run algos if streaming is on
		System.out.println("  reading events file and analyzing volumes...");
		new MatsimEventsReader(events).readFile(config.events().getInputFile());
		System.out.println("  done.");

		System.out.println("  writing matrices files... ");
		final MatricesWriter matrices_writer = new MatricesWriter(Matrices.getSingleton());
		matrices_writer.write();
		final ZoneLayer bezirke = (ZoneLayer)world.getLayer(new IdImpl("bezirke"));
		final Set<Id> ids = new TreeSet<Id>();
		for (final Iterator<? extends Location> iter = bezirke.getLocations().values().iterator(); iter.hasNext(); ) {
			final Location loc = iter.next();
			if (Integer.parseInt(loc.getId().toString()) < 24) {
				ids.add(loc.getId());
			}
		}
		for (final CalcODMatricesBezirke odcalc : odcalcs) {
			final String filename = odcalc.getMatrix().getId() + ".fma";
			System.out.println("    writing file: " + filename + "   (" + odcalc.counter + ")");
			final VisumMatrixWriter writer = new VisumMatrixWriter(odcalc.getMatrix());
			writer.setIds(ids);
			writer.writeFile(filename);
		}
		System.out.println("  done.");

		System.out.println("RUN: calc1hODMatricesBezirke finished.");
		System.out.println();
	}


	//////////////////////////////////////////////////////////////////////
	// convertMatrices
	//////////////////////////////////////////////////////////////////////

	public static void convertMatrices(final String[] args) {
		System.out.println("RUN: convertMatrices");

		final Config config = Gbl.createConfig(args);
		final World world = Gbl.getWorld();

		System.out.println("  reading world xml file... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(world);
		worldReader.readFile(config.world().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading matrices xml file... ");
		MatsimMatricesReader reader = new MatsimMatricesReader(Matrices.getSingleton());
		reader.readFile(config.matrices().getInputFile());
		System.out.println("  done.");

		System.out.println("  writing matrices file... ");
		MatricesWriter matrices_writer = new MatricesWriter(Matrices.getSingleton());
		matrices_writer.write();

		final TreeSet<Id> ids = new TreeSet<Id>();
		for (int i = 1; i < 882; i++) {
			ids.add(new IdImpl(i));
		}
		for (final String name : Matrices.getSingleton().getMatrices().keySet()) {
			final Matrix matrix = Matrices.getSingleton().getMatrices().get(name);
			final VisumMatrixWriter writer = new VisumMatrixWriter(matrix);
			writer.setIds(ids);
			writer.writeFile(name + ".fma");
		}
		System.out.println("  done.");

		System.out.println("RUN: convertMatrices finished.");
		System.out.println();
	}

	//////////////////////////////////////////////////////////////////////
	// calcPlanStatistics
	//////////////////////////////////////////////////////////////////////

	public static void calcPlanStatistics(final String[] args) {

		System.out.println("RUN: calcPlanStatistics");

		final Config config = Gbl.createConfig(args);
		final World world = Gbl.getWorld();
		System.out.println("  reading world xml file... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(world);
		worldReader.readFile(config.world().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading the network...");
		NetworkLayer network = null;
		network = (NetworkLayer)world.createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		System.out.println("  done.");

		System.out.println("  setting up plans objects...");
		final Plans plans = new Plans(Plans.USE_STREAMING);
		final PlansReaderI plansReader = new MatsimPlansReader(plans);
		System.out.println("  done.");

		System.out.println("  adding plans algorithm... ");

		plans.addAlgorithm(new PlanCalcType());
		final String[] activities = {null, "home", "edu", "uni", "work1", "work2", "work3", "shop1", "shop2", "home2", "leisure1", "leisure2"};
		final String[] legModes = {null, "walk", "bike", "car", "pt", "ride"};
		final PlanSummary summary = new PlanSummary(activities, legModes);
		plans.addAlgorithm(summary);
		System.out.println("  done.");

		System.out.println("  reading, analyzing plans...");
		plansReader.readFile(config.plans().getInputFile());
		plans.printPlansCount();
		System.out.println("  done.");

		summary.print();

		System.out.println("RUN: calcPlanStatistics finished.");
		System.out.println();
	}

	public static void analyzeLegTimes_events(final String[] args, final int binSize) {
		System.out.println("RUN: analyzeLegTimes");
		final Config config = Gbl.createConfig(args);

/*		System.out.println("  reading world xml file... ");
		final WorldParser world_parser = new WorldParser(Gbl.getWorld());
		world_parser.parse();
		System.out.println("  done.");

		System.out.println("  reading the network...");
		NetworkLayer network = null;
		network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE,null);
		final NetworkParser network_parser = new NetworkParser(network);
		network_parser.parse();
		System.out.println("  done.");

		System.out.println("  reading plans...");
		final Plans plans = new Plans(Plans.NO_STREAMING);
		final PlansReaderI plansReader = new MatsimPlansReader(plans);
		plansReader.readfile(Gbl.getConfig().plans().getInputFile());
		plans.printPlansCount();
		System.out.println("  done.");
*/
		System.out.println("  reading events and analyzing departure times... ");
		final Events events = new Events();
		final LegHistogram analysis = new LegHistogram(binSize);
		events.addHandler(analysis);
		new MatsimEventsReader(events).readFile(config.events().getInputFile());
		System.out.println("  done.");
		analysis.write(System.out);
		System.out.println("RUN: analyzeLegTimes finished.");
	}

	//////////////////////////////////////////////////////////////////////
	// calcTripLength
	//////////////////////////////////////////////////////////////////////

	public static void calcTripLength(final String[] args) {
		System.out.println("RUN: calcTripLength");

		final Config config = Gbl.createConfig(args);
		final World world = Gbl.getWorld();

		System.out.println("  reading world xml file... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(world);
		worldReader.readFile(config.world().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading the network...");
		NetworkLayer network = null;
		network = (NetworkLayer)world.createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading plans...");
		final Plans plans = new Plans(Plans.USE_STREAMING);
		final PlansReaderI plansReader = new MatsimPlansReader(plans);
		final CalcAverageTripLength catl = new CalcAverageTripLength();
		plans.addAlgorithm(catl);
		plansReader.readFile(config.plans().getInputFile());
		plans.printPlansCount();
		System.out.println("  done.");

		System.out.println("Average trip length: " + catl.getAverageTripLength());

		System.out.println("RUN: calcTripLength finished.");
		System.out.println();
	}

	//////////////////////////////////////////////////////////////////////
	// calcTolledTripLength
	//////////////////////////////////////////////////////////////////////

	public static void calcTolledTripLength(final String[] args) {
		System.out.println("RUN: calcTolledTripLength");

		final Config config = Gbl.createConfig(args);
		final World world = Gbl.getWorld();

		System.out.println("  reading world xml file... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(world);
		worldReader.readFile(config.world().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading the network...");
		NetworkLayer network = null;
		network = (NetworkLayer)world.createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading toll links...");
//		CalcVehicleToll tollCalc = new CalcVehicleToll(network);
//		new LinkTollReaderTXTv1(tollCalc).readfile(Gbl.getConfig().getParam("roadpricing", "tollLinksFile"));
		final RoadPricingReaderXMLv1 reader1 = new RoadPricingReaderXMLv1(network);
		try {
			reader1.parse(config.roadpricing().getTollLinksFile());
		} catch (final Exception e) {
			e.printStackTrace();
		}
		final RoadPricingScheme scheme = reader1.getScheme();
		System.out.println("  done.");

		System.out.println("  reading events... ");
		final Events events = new Events();
		final CalcAverageTolledTripLength catl = new CalcAverageTolledTripLength(network, scheme);
		events.addHandler(catl);
		new MatsimEventsReader(events).readFile(config.events().getInputFile());
		events.printEventsCount();
		System.out.println("  done.");

		System.out.println("Average tolled trip length: " + catl.getAverageTripLength());

		System.out.println("RUN: calcTolledTripLength finished.");
		System.out.println();
	}


	//////////////////////////////////////////////////////////////////////
	// generateRealPlans
	//////////////////////////////////////////////////////////////////////

	public static void generateRealPlans(final String[] args) {
		System.out.println("RUN: generateRealPlans");

		final Config config = Gbl.createConfig(args);
		final World world = Gbl.getWorld();

		System.out.println("  reading world xml file... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(world);
		worldReader.readFile(config.world().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading the network...");
		NetworkLayer network = null;
		network = (NetworkLayer)world.createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		System.out.println("  done.");

		// events
		System.out.println("  creating events object... ");
		final Events events = new Events();
		System.out.println("  done.");

		System.out.println("  adding events algorithms...");
		final GenerateRealPlans generator = new GenerateRealPlans();
		events.addHandler(generator);
		System.out.println("  done");

		System.out.println("  reading events file and generating real plans...");
		new MatsimEventsReader(events).readFile(config.events().getInputFile());
		generator.finish();
		System.out.println("  done.");

		System.out.println("  writing real plans...");
		final String outfile = config.plans().getOutputFile();
		final String outversion = config.plans().getOutputVersion();
		final PlansWriter plansWriter = new PlansWriter(generator.getPlans(), outfile, outversion);
		plansWriter.write();
		System.out.println("  done.");

		System.out.println("RUN: generateRealPlans finished.");
		System.out.println();
	}

	//////////////////////////////////////////////////////////////////////
	// planPlotActLocations
	//////////////////////////////////////////////////////////////////////

	public static void planPlotActLocations(final String[] args) {
		System.out.println("RUN: planPlotActLocations");
		final Config config = Gbl.createConfig(args);

		System.out.println("  reading the network...");
		NetworkLayer network = null;
		network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		System.out.println("  done.");

		System.out.println("  setting up plans objects...");
		final Plans plans = new Plans(Plans.USE_STREAMING);
		final PlanPlotActLocations plotter = new PlanPlotActLocations("locations.T.veh", new String[] {"home", "edu", "uni", "work1", "work2", "work3", "shop1", "shop2", "leisure1", "leisure2", "home2"});
		plans.addAlgorithm(plotter);
		final PlansReaderI plansReader = new MatsimPlansReader(plans);
		System.out.println("  done.");

		System.out.println("  reading plans and running algorithms...");
		plansReader.readFile(config.plans().getInputFile());
		plans.printPlansCount();
		System.out.println("  done.");

		plotter.close();

		System.out.println("RUN: planPlotActLocations finished.");
	}

	//////////////////////////////////////////////////////////////////////
	// writeVisumRouten
	//////////////////////////////////////////////////////////////////////

	public static void writeVisumRouten_plans(final String[] args) {
		System.out.println("RUN: writeVisumRouten_plans");

		final Config config = Gbl.createConfig(args);
		final World world = Gbl.getWorld();

		System.out.println("  reading world xml file... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(world);
		worldReader.readFile(config.world().getInputFile());
		ZoneLayer tvz = (ZoneLayer)world.getLayer(new IdImpl("tvz"));
		System.out.println("  done.");

		System.out.println("  reading the network...");
		NetworkLayer network = null;
		network = (NetworkLayer)world.createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		System.out.println("  done.");

		System.out.println("  create Anbindungs-tabelle for VISUM...");
		final VisumAnbindungstabelleWriter anbindung = new VisumAnbindungstabelleWriter();
		anbindung.write("anbindungen.net", network, tvz);
		System.out.println("  done.");

		System.out.println("  setting up plans objects...");
		final Plans plans = new Plans(Plans.USE_STREAMING);
		final VisumWriteRoutes writer = new VisumWriteRoutes("routen.rim", tvz);
		plans.addAlgorithm(writer);
		final PlansReaderI plansReader = new MatsimPlansReader(plans);
		System.out.println("  done.");

		System.out.println("  reading plans and running algorithms...");
		plansReader.readFile(config.plans().getInputFile());
		plans.printPlansCount();
		System.out.println("  done.");

		writer.close();

		System.out.println("RUN: writeVisumRouten_plans finished.");
	}

	//////////////////////////////////////////////////////////////////////
	// calcLegTimes
	//////////////////////////////////////////////////////////////////////

	public static void calcLegTimes(final String[] args) {
		System.out.println("RUN: calcLegTimes");
		final Config config = Gbl.createConfig(args);

		System.out.println("  reading the network...");
		final NetworkLayer network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading facilities xml file... ");
		Facilities facilities = (Facilities)Gbl.getWorld().createLayer(Facilities.LAYER_TYPE, null);
		new MatsimFacilitiesReader(facilities).readFile(config.facilities().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading plans...");
		final Plans population = new Plans(Plans.NO_STREAMING);
		final PlansReaderI plansReader = new MatsimPlansReader(population);
		plansReader.readFile(config.plans().getInputFile());
		population.printPlansCount();
		System.out.println("  done.");

		// events
		System.out.println("  creating events object... ");
		final Events events = new Events();
		System.out.println("  done.");

		System.out.println("  adding events algorithms...");
		final CalcLegTimes calcLegTimes = new CalcLegTimes(population);
		events.addHandler(calcLegTimes);
		System.out.println("  done");

		System.out.println("  reading events...");
		new MatsimEventsReader(events).readFile(config.events().getInputFile());
		System.out.println("  done.");

		System.out.println("  writing stats...");
		System.out.println("---------------------------------------------------");
		final TreeMap<String, int[]> legStats = calcLegTimes.getLegStats();
		for (final String key : legStats.keySet()) {
			final int[] counts = legStats.get(key);
			System.out.print(key);
			for (int i = 0; i < counts.length; i++) {
				System.out.print("\t" + counts[i]);
			}
			System.out.println();
		}
		System.out.println("---------------------------------------------------");
		System.out.println("  done.");

		System.out.println("RUN: calcLegTimes finished.");
		System.out.println();
	}

	//////////////////////////////////////////////////////////////////////
	// calcStuckVehStats
	//////////////////////////////////////////////////////////////////////

	public static void calcStuckVehStats(final String[] args) {

		System.out.println("RUN: calcStuckVehStats");

		final Config config = Gbl.createConfig(args);
		final World world = Gbl.getWorld();

		System.out.println("  reading world xml file... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(world);
		worldReader.readFile(config.world().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading the network...");
		NetworkLayer network = null;
		network = (NetworkLayer)world.createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		System.out.println("  done.");

		// events
		System.out.println("  creating events object... ");
		final Events events = new Events();
		System.out.println("  done.");

		System.out.println("  adding events algorithms...");
		final StuckVehStats stuckStats = new StuckVehStats(network);
		events.addHandler(stuckStats);
		System.out.println("  done");

		System.out.println("  reading events...");
		new MatsimEventsReader(events).readFile(config.events().getInputFile());
		System.out.println("  done.");

		System.out.println("  writing stats...");
		System.out.println("---------------------------------------------------");
		stuckStats.printResults();
		System.out.println("---------------------------------------------------");

		System.out.println("RUN: calcStuckVehStats finished.");
		System.out.println();
	}


	public static void falsifyNetAndPlans(final String[] args) {
		System.out.println("RUN: falsifyNetAndPlans");

		final Config config = Gbl.createConfig(args);

		System.out.println("  reading the network...");
		NetworkLayer network = null;
		network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());

		System.out.println("  falsifying the network...");
		(new NetworkFalsifier(50)).run(network);

		System.out.println("  writing the falsified network...");
		final NetworkWriter network_writer = new NetworkWriter(network);
		network_writer.write();
		System.out.println("  done.");

		System.out.println("  processing plans...");
		final Plans population = new Plans(Plans.USE_STREAMING);
		final PlansWriter plansWriter = new PlansWriter(population);
		final PlansReaderI plansReader = new MatsimPlansReader(population);
		population.addAlgorithm(new ActLocationFalsifier(200));
		population.addAlgorithm(new XY2Links(network));
		final FreespeedTravelTimeCost timeCostFunction = new FreespeedTravelTimeCost();
		population.addAlgorithm(new PlansCalcRoute(network, timeCostFunction, timeCostFunction));
		population.setPlansWriter(plansWriter);
		plansReader.readFile(config.plans().getInputFile());
		population.printPlansCount();
		plansWriter.write();
		System.out.println("  done.");

		System.out.println("RUN: falsifyNetAndPlans finished.");
		System.out.println();
	}

	//////////////////////////////////////////////////////////////////////
	// visumMatrixTest
	//////////////////////////////////////////////////////////////////////

	public static void visumMatrixTest(final String[] args) {

		System.out.println("RUN: visumMatrixTest");

		final Config config = Gbl.createConfig(args);
		final World world = Gbl.getWorld();

		Runtime.getRuntime().gc();
		Gbl.printMemoryUsage();

		System.out.println("  reading world xml file... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(world);
		worldReader.readFile(config.world().getInputFile());
		System.out.println("  done.");

		Runtime.getRuntime().gc();
		Gbl.printMemoryUsage();
		System.out.println("  reading matrices file... " + (new Date()));
		new VisumMatrixReader("oev_reisezeiten", world.getLayer(new IdImpl("municipality"))).readFile(config.matrices().getInputFile());
		System.out.println("  done." + (new Date()));

		Runtime.getRuntime().gc();
		Gbl.printMemoryUsage();
		System.out.println("  writing matrices file... " + (new Date()));
		new MatricesWriter(Matrices.getSingleton()).writeFile(config.matrices().getOutputFile());
		System.out.println("  done." + (new Date()));

		Runtime.getRuntime().gc();
		Gbl.printMemoryUsage();
		System.out.println("RUN: visumMatrixTest finished.");
		System.out.println();
	}

	//////////////////////////////////////////////////////////////////////
	// buildKML2
	//////////////////////////////////////////////////////////////////////

	private static Geometry getNetworkAsKml(final NetworkLayer network, final CoordinateTransformationI coordTransform) {
		return getNetworkAsKml(network, new TreeMap<Id, Integer>(), coordTransform);
	}

	private static Geometry getNetworkAsKml(final NetworkLayer network, final TreeMap<Id, Integer> linkVolumes, final CoordinateTransformationI coordTransform) {
		final MultiGeometry networkGeom = new MultiGeometry();

		for (Link link : network.getLinks().values()) {
			Integer volume = linkVolumes.get(link.getId());
			if (volume == null) volume = 0;
			final CoordI fromCoord = coordTransform.transform(link.getFromNode().getCoord());
			final CoordI toCoord = coordTransform.transform(link.getToNode().getCoord());
			final Point fromPoint = new Point(fromCoord.getX(), fromCoord.getY(), volume);
			final Point toPoint = new Point(toCoord.getX(), toCoord.getY(), volume);
			final LineString lineString = new LineString(fromPoint, toPoint, true, true, Geometry.AltitudeMode.RELATIVE_TO_GROUND);
			networkGeom.addGeometry(lineString);
		}

		return networkGeom;
	}

	public static void buildKML2(final String[] args, final boolean useVolumes) {

		System.out.println("RUN: buildKML2");

		final TreeMap<Integer, TreeMap<Id, Integer>> linkValues = new TreeMap<Integer, TreeMap<Id, Integer>>();

		Config config = Gbl.createConfig(args);

		System.out.println("  reading the network...");
		NetworkLayer network = null;
		network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		System.out.println("  done.");

		if (useVolumes) {

			System.out.println("  reading world xml file... " + (new Date()));
			final MatsimWorldReader worldReader = new MatsimWorldReader(Gbl.getWorld());
			worldReader.readFile(config.world().getInputFile());
			System.out.println("  done." + (new Date()));

			System.out.println("  reading plans...");
			final Plans plans = new Plans(Plans.USE_STREAMING);
			final PlansReaderI plansReader = new MatsimPlansReader(plans);
			plans.addAlgorithm(
					new PersonAlgorithm() {

						@Override
						public void run(final Person person) {
							Plan plan = person.getSelectedPlan();
							if (plan == null) {
								if (person.getPlans().size() == 0) return;
								plan = person.getPlans().get(0);
							}
							run(plan);
						}

						public void run(final Plan plan) {
							final List actslegs = plan.getActsLegs();
							for (int i = 1, max = actslegs.size(); i < max; i+=2) {
								final Leg leg = (Leg)actslegs.get(i);
								run(leg.getRoute(), leg.getDepTime());
							}
						}

						public void run(final Route route, final double time) {
							if (route == null) return;

							final int hour = (int)time / 3600;
							if ((hour > 30) || (hour < 0)) return;

							for (final Link link : route.getLinkRoute()) {
								final Id id = link.getId();
								TreeMap<Id, Integer> hourValues = linkValues.get(hour);
								if (hourValues == null) {
									hourValues = new TreeMap<Id, Integer>();
									linkValues.put(hour, hourValues);
								}
								Integer counter = hourValues.get(id);
								if (counter == null) counter = 0;
								counter++;
								hourValues.put(id, counter);
							}
						}
					}
			);
			plansReader.readFile(config.plans().getInputFile());
			plans.printPlansCount();
			System.out.println("  done.");
		}

		System.out.println("  writing the network...");

		final KML kml = new KML();

		final Document kmlDoc = new Document("the root document");
		kml.setFeature(kmlDoc);

		final Style style = new Style("redWallStyle");
		kmlDoc.addStyle(style);
		style.setLineStyle(new LineStyle(new Color("af", "00", "00", "ff"), ColorStyle.DEFAULT_COLOR_MODE, 3));
		style.setPolyStyle(new PolyStyle(new Color("7f", "00", "00", "ff"), ColorStyle.DEFAULT_COLOR_MODE, true, true));

		final Folder networksFolder = new Folder("networks");
		kmlDoc.addFeature(networksFolder);

		if (useVolumes) {
			for (int hour = 4; hour < 23; hour++) {
				System.out.println("adding network hour = " + hour);
				final Placemark placemark = new Placemark(
						"network " + hour, // id
						"Network at " + hour, // name
						"the road network at " + hour,	// description
						Placemark.DEFAULT_ADDRESS,
						Feature.DEFAULT_LOOK_AT,
						style.getStyleUrl(),
						Feature.DEFAULT_VISIBILITY,
						Feature.DEFAULT_REGION,
						new TimeSpan(new GregorianCalendar(1970, 0, 1, hour, 0, 0),
								new GregorianCalendar(1970, 0, 1, hour, 59, 59)));
				networksFolder.addFeature(placemark);
				TreeMap<Id, Integer> hourValues = linkValues.get(hour);
				if (hourValues == null) {
					hourValues = new TreeMap<Id, Integer>();
				}
				placemark.setGeometry(getNetworkAsKml(network, hourValues, new CH1903LV03toWGS84()));
			}
		} else {
			final Placemark placemark = new Placemark(
					"network", // id
					"Network", // name
					"the road network",	// description
					Placemark.DEFAULT_ADDRESS,
					Feature.DEFAULT_LOOK_AT,
					style.getStyleUrl(),
					Feature.DEFAULT_VISIBILITY,
					Feature.DEFAULT_REGION,
					Feature.DEFAULT_TIME_PRIMITIVE);
			networksFolder.addFeature(placemark);
			placemark.setGeometry(getNetworkAsKml(network, new CH1903LV03toWGS84()));
		}

		final KMLWriter kmlWriter = new KMLWriter(kml, "test.kml", KMLWriter.DEFAULT_XMLNS, true);
		kmlWriter.write();

		System.out.println("  done.");

		System.out.println("RUN: buildKML2 finished.");
		System.out.println();
	}

	public static void network2kml(final String[] args) {

		System.out.println("RUN: network2kml");

		Config config = Gbl.createConfig(args);

		System.out.println("  reading the network...");
		NetworkLayer network = null;
		network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		System.out.println("  done.");


		System.out.println("  writing the network...");

		final KML kml = new KML();

		final Document kmlDoc = new Document("the root document");
		kml.setFeature(kmlDoc);

		final Style style = new Style("redWallStyle");
		kmlDoc.addStyle(style);
		style.setLineStyle(new LineStyle(new Color("af", "00", "00", "ff"), ColorStyle.DEFAULT_COLOR_MODE, 3));
		style.setPolyStyle(new PolyStyle(new Color("7f", "00", "00", "ff"), ColorStyle.DEFAULT_COLOR_MODE, true, true));

		final Folder networksFolder = new Folder("networks");
		kmlDoc.addFeature(networksFolder);

		final Placemark placemark = new Placemark(
				"network", // id
				"Network", // name
				"the road network",	// description
				Placemark.DEFAULT_ADDRESS,
				Feature.DEFAULT_LOOK_AT,
				style.getStyleUrl(),
				Feature.DEFAULT_VISIBILITY,
				Feature.DEFAULT_REGION,
				Feature.DEFAULT_TIME_PRIMITIVE);
		networksFolder.addFeature(placemark);
		placemark.setGeometry(getNetworkAsKml(network, new GK4toWGS84()));

		final KMLWriter kmlWriter = new KMLWriter(kml, "test.kml", KMLWriter.DEFAULT_XMLNS, true);
		kmlWriter.write();

		System.out.println("  done.");

		System.out.println("RUN: network2kml finished.");
		System.out.println();
	}

	public static void readPlansDat(final String[] args) {
		Gbl.createConfig(null);
		try {
			final DataInputStream in = new DataInputStream(new FileInputStream(args[0]));
			final int nofAgents = in.readInt();
			System.out.println("# agents: " + nofAgents);
			for (int i = 0; i < nofAgents; i++) {
				final int agentId = in.readInt();
				System.out.println("  agent: " + agentId);
				final int nofLegs = in.readInt();
				System.out.println("    # legs: " + nofLegs);
				for (int l = 0; l < nofLegs; l++) {
					final double time = in.readDouble();
					System.out.print("      " + Time.writeTime(time));
					final int nofLinks = in.readInt();
					for (int n = 0; n < nofLinks; n++) {
						System.out.print(" " + in.readInt());
					}
					System.out.println();
				}
			}
			in.close();
		}
		catch (final Exception e) {
			e.printStackTrace();
		}
	}

	public static void readEventsDat(final String[] args) {
		Gbl.createConfig(args);
		try {
			final DataInputStream in = new DataInputStream(new FileInputStream("output/equil1/ITERS/it.0/deq_events.deq"));
			while (in.available() > 0) {
				final double time = in.readDouble();
				final int agentId = in.readInt();
				final int linkId = in.readInt();
				final int eventType = in.readInt();
				System.out.println(time + "\t" + agentId + "\t" + linkId + "\t" + eventType);
			}
			in.close();
		}
		catch (final IOException e) {
			e.printStackTrace();
		}
	}

	public static void speedEventsDat(final String[] args) {
		Gbl.createConfig(args);
		Events events = new Events();
		Gbl.startMeasurement();
		new EventsReaderDEQv1(events).readFile("../mystudies/deqsimtest/10.deq_events.dat");
		events.printEventsCount();
		Gbl.printElapsedTime();
	}


	public static void readMatrices(final String[] args) {
		/* only used for performance testing */
		System.out.println("RUN: readMatrices");

		final Config config = Gbl.createConfig(args);
		final World world = Gbl.getWorld();

		System.out.println("  reading world xml file... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(world);
		worldReader.readFile(config.world().getInputFile());
		System.out.println("  done.");
		Gbl.printRoundTime();

		System.out.println("  reading matrices xml file... ");
		MatsimMatricesReader reader = new MatsimMatricesReader(Matrices.getSingleton());
		reader.readFile(config.matrices().getInputFile());
		System.out.println("  done.");
		Gbl.printRoundTime();

		System.out.println("RUN: readMatrices finished.");
		System.out.println();
	}

	public static void readWriteLinkStats(final String[] args) {
		System.out.println("RUN: readWriteLinkStats");

		Config config = Gbl.createConfig(args);

		System.out.println("  reading the network...");
		NetworkLayer network = null;
		network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());

		System.out.println("  reading linkstats...");
		CalcLinkStats linkStats = new CalcLinkStats(network);
		linkStats.readFile("210.linkstats.att");

		System.out.println("  writing linkstats...");
		linkStats.writeFile("210.out.linkstats.att");

		System.out.println("RUN: readWriteLinkStats finished.");
		System.out.println();
	}

	public static void someTests(final String[] args) {
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile("/Volumes/Data/ETH/cvs/ivt/studies/switzerland/networks/ivtch/network_r1.1.xml");
		Events events = new Events();
		TravelTimeCalculatorArray ttimeCalc = new TravelTimeCalculatorArray(network, 15*60, 30*3600);
		VolumesAnalyzer vol = new VolumesAnalyzer(3600, 30*3600, network);
		CalcLinkStats linkStats = new CalcLinkStats(network);
		int[] volumes;
		events.addHandler(ttimeCalc);
		events.addHandler(vol);

		new MatsimEventsReader(events).readFile("/Volumes/Data/VSP/runs/run252e/16.events.txt.gz");
		events.printEventsCount();
		linkStats.addData(vol, ttimeCalc);
		volumes = vol.getVolumesForLink("101885");
		if (volumes != null) {
			System.out.println("16 volume 7-8: " + volumes[7]);
		}

		events.resetHandlers(17);
		new MatsimEventsReader(events).readFile("/Volumes/Data/VSP/runs/run252e/17.events.txt.gz");
		events.printEventsCount();
		linkStats.addData(vol, ttimeCalc);
		volumes = vol.getVolumesForLink("101885");
		if (volumes != null) {
			System.out.println("17 volume 7-8: " + volumes[7]);
		}

		events.resetHandlers(18);
		new MatsimEventsReader(events).readFile("/Volumes/Data/VSP/runs/run252e/18.events.txt.gz");
		events.printEventsCount();
		linkStats.addData(vol, ttimeCalc);
		volumes = vol.getVolumesForLink("101885");
		if (volumes != null) {
			System.out.println("18 volume 7-8: " + volumes[7]);
		}

		events.resetHandlers(19);
		new MatsimEventsReader(events).readFile("/Volumes/Data/VSP/runs/run252e/19.events.txt.gz");
		events.printEventsCount();
		linkStats.addData(vol, ttimeCalc);
		volumes = vol.getVolumesForLink("101885");
		if (volumes != null) {
			System.out.println("19 volume 7-8: " + volumes[7]);
		}

		events.resetHandlers(20);
		new MatsimEventsReader(events).readFile("/Volumes/Data/VSP/runs/run252e/20.events.txt.gz");
		events.printEventsCount();
		linkStats.addData(vol, ttimeCalc);
		volumes = vol.getVolumesForLink("101885");
		if (volumes != null) {
			System.out.println("20 volume 7-8: " + volumes[7]);
		}

		linkStats.writeFile("./output/testStats.txt");
	}

	public static void randomWalk(final int steps) {
		long sum = 0;
		final Random rand = new Random(4710);
		for (int i = 0; i <= steps; i++) {
			if (rand.nextDouble() < 0.5) {
				sum--;
			} else {
				sum++;
			}
			System.out.println(i + "\t" + sum);
		}
	}

	public static void readCounts(final String[] args) {
		System.out.println("RUN: readCounts");
		final Config config = Gbl.createConfig(args);
		final Counts counts = new Counts();

		NetworkLayer network = null;
		network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());

		new MatsimCountsReader(counts).readFile(config.counts().getCountsFileName());

		new CountsWriter(counts).writeFile("test_counts.xml");

		System.out.println("RUN: readCounts finished.");
		System.out.println();
	}

	public static void readEvents(final String[] args) {
		System.out.println("RUN: readEvents");

		final Events events = new Events();
		events.addHandler(new CalcLegNumber());
		EventWriterTXT writer = new EventWriterTXT("/Volumes/Data/VSP/cvs/vsp-cvs/runs/run212/events_fixed.txt.gz");
		events.addHandler(writer);
		new MatsimEventsReader(events).readFile("/Volumes/Data/VSP/cvs/vsp-cvs/runs/run212/events.txt");
		writer.closefile();

		System.out.println("RUN: readEvents finished.");
		System.out.println();
	}

	public static void writeKml() {
		KML mainKml = new KML();
		Document mainDoc = new Document("test.kmz");
		mainKml.setFeature(mainDoc);

		KMZWriter writer = new KMZWriter("test.kmz", KMLWriter.DEFAULT_XMLNS);

		MatsimKMLLogo logo;
		try {
			logo = new MatsimKMLLogo(writer);
			mainDoc.addFeature(logo);
		} catch (IOException e) {
			e.printStackTrace();
		}
		writer.writeMainKml(mainKml);
		writer.close();
	}

	public static void createQVDiagramm(final String[] args) {

		String[] links = { "101207", "101208", "105683", "105684", "105651", "106505", "106506", "106427", "106428", "110579", "110580", "111609" };
//		String[] links = { "106505" };
		QVDiagramm qvds[] = new QVDiagramm[links.length];

		Config config = Gbl.createConfig(args);
		ScenarioData data = new ScenarioData(config);

		Events events = new Events();
		NetworkLayer network = data.getNetwork();
		for (int i = 0; i < links.length; i++) {
			qvds[i] = new QVDiagramm(network, links[i]);
			events.addHandler(qvds[i]);
		}

		new MatsimEventsReader(events).readFile(config.events().getInputFile());

		for (int i = 0; i < links.length; i++) {
			qvds[i].writeGraph("link" + links[i] + "_qv.png");
		}
	}

	public static void someTest(final String[] args) {
		Config config = Gbl.createConfig(args);
		NetworkLayer network = new NetworkLayer();
		Gbl.startMeasurement();
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		Gbl.printRoundTime();
		QueueNetworkLayer qnet = new QueueNetworkLayer(network);
		Gbl.printRoundTime();
	}

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(final String[] args) {


		System.out.println("start at " + (new Date()));

/* ***   P L A N S   *** */

//		createKutterPlans(args);
//		fmaToTrips(args);

//		convertPlans(args);
//		readPlans(args);
//		removeLinkAndRoute(args);

		/* ***   DEMAND MODELING   *** */

//		filterSelectedPlans(args);
//		filterPlansInArea(args, 4582000, 5939000, 4653000, 5850000);  // uckermark
//		filterPlansInArea(args, 4580000, 5807000, 4617000, 5835000);  // berlin
//		filterPlansWithRouteInArea(args, 683518.0, 246836.0, 30000.0); // Bellevue Zrh, 30km
//		filterPlansWithRouteInArea(args, 4595406.5, 5821171.5, 1000.0); // Berlin Mitte, 1km
//		filterPlansInNetworkArea(args);
//		filterPlansPassingLink(args, 4022);
//		filterCars(args);
//		filterPt(args);
//		filterWork(args);
//		filterWorkEdu(args);
//		createDebugPlans(args);
//		calcRoute(args);
//		calcRouteMTwithTimes(args); // multithreaded, use traveltimes from events
//		calcRoutePt(args);  // public transport

//		testPt(args);  // public transport
//		calcRealPlans(args);
//		calcScoreFromEvents_old(args);
//		calcScoreFromEvents_new(args);

/* ***   N E T W O R K S   *** */
//		convertNetwork(args);
//		cleanNetwork(args);
//		calcNofLanes(args);
//		falsifyNetwork(args);

/* ***   M A T R I C E S   *** */
//		visumMatrixTest(args);
//		convertMatrices(args);


/* ***   A N A L Y S I S   *** */

		/* ***   VOLUMES   *** */
//		calcODMatrix(args); // calcs OD matrix 0-24h from Plans, using 1004 tvz from Berlin
//		calc1hODMatrices(args);		// calcs 1h OD matrices from plans and events, using 1004 tvz from Berlin
//		calcODMatrixBezirke(args);	// calcs OD matrix 0-24h from Plans, using Bezirke 1-23 instead of tvz within Berlin
//		calc1hODMatricesBezirke(args);	// calcs OD matrix for single hours from Plans, using Bezirke 1-23 instead of tvz within Berlin

		/* ***   PLANS   *** */
//		calcPlanStatistics(args);
//		analyzeLegTimes_events(args, 300); // # departure, # arrival, # stuck per time bin
//		calcTripLength(args); // from plans
//		calcTolledTripLength(args); // calc avg trip length on tolled links
//		generateRealPlans(args);
//		planPlotActLocations(args);
//		writeVisumRouten_plans(args);

		/* ***   EVENTS   *** */
//		readEvents(args);
//		calcLegTimes(args);  // leg durations
//		calcStuckVehStats(args);

/* ***   C O M P L E X   S T U F F   *** */

//		falsifyNetAndPlans(args);

		/* ***   P L A Y G R O U N D   *** */

//		buildKML2(args, false);
//		network2kml(args);
//		readPlansDat(args);
//		readEventsDat(args);
//		speedEventsDat(args);
//		readMatrices(args);
//		readWriteLinkStats(args);
//		randomWalk(10000);
//		readCounts(args);
//		writeKml();
//		createQVDiagramm(args);
//		someTest(args);

//		Gbl.printSystemInfo();

		System.out.println("stop at " + (new Date()));
		System.exit(0); // currently only used for calcRouteMT();
	}
}
