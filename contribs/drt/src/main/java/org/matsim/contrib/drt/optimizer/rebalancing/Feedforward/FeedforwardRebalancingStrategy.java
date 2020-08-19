package org.matsim.contrib.drt.optimizer.rebalancing.Feedforward;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Triple;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.optimizer.rebalancing.toolbox.VehicleInfoCollector;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.core.network.NetworkUtils;

/**
 * @author Chengqi Lu This strategy is created based on the Feedforward Fluidic
 *         rebalancing algorithm in AMoDeus. The algorithm send rebalancing
 *         vehicles based on the DRT demand flow of previous iteration. This
 *         strategy is comparable to the MinCostFlowRebalancing Strategy with
 *         Previous Iteration Zonal DRT Demand Aggregator. But, instead of
 *         setting a rebalance target for each zone, the concept of flow is
 *         used. Important: At least 2 iterations are needed in order to make
 *         this strategy function properly.
 */
public class FeedforwardRebalancingStrategy implements RebalancingStrategy {
	private static final Logger log = Logger.getLogger(FeedforwardRebalancingStrategy.class);

	private final DrtZonalSystem zonalSystem;
	private final FeedforwardRebalancingParams params;
	private final Network network;
	private final VehicleInfoCollector vehicleInfoCollector;

	private final int timeBinSize;
	private final double rebalanceInterval;
	private final double scale;
	private final Random rnd = new Random(1234);
	private final int feedforwardSignalLead;

	private final Map<Double, List<Triple<String, String, Integer>>> rebalancePlanCore;

	// TODO Potential further extension
	// 1. Add feedback mechanism to maintain minimum number of vehicles (including
	// soon arrival vehicles) for each zone

	public FeedforwardRebalancingStrategy(DrtZonalSystem zonalSystem, Fleet fleet, Network network,
			FeedforwardRebalancingParams params, FeedforwardSignalHandler feedforwardSignalHandler) {
		this.network = network;
		this.zonalSystem = zonalSystem;
		this.params = params;
		timeBinSize = params.getTimeBinSize();

		rebalanceInterval = params.getInterval();
		vehicleInfoCollector = new VehicleInfoCollector(fleet, zonalSystem);

		scale = params.getFeedforwardSignalStrength() * rebalanceInterval / timeBinSize;
		log.info("The feedforward signal strength is: " + Double.toString(params.getFeedforwardSignalStrength()));

		rebalancePlanCore = feedforwardSignalHandler.getRebalancePlanCore();
		feedforwardSignalLead = params.getFeedforwardSignalLead();

	}

	@Override
	public List<Relocation> calcRelocations(Stream<? extends DvrpVehicle> rebalancableVehicles, double time) {
		// assign rebalnace vehicles based on the rebalance plan
		log.info("Rebalance fleet now: Feedforward Rebalancing Strategy is used");

		double timeBin = Math.floor((time + feedforwardSignalLead) / timeBinSize);
		List<Relocation> relocationList = new ArrayList<>();

		if (rebalancePlanCore.containsKey(timeBin)) {
			Map<String, List<DvrpVehicle>> rebalancableVehiclesPerZone = vehicleInfoCollector
					.groupRebalancableVehicles(rebalancableVehicles, timeBin, params.getMinServiceTime());
			// Generate relocations based on the "rebalancePlanCore"
			for (Triple<String, String, Integer> rebalanceInfo : rebalancePlanCore.get(timeBin)) {
				String departureZoneId = rebalanceInfo.getLeft();
				String arrivalZoneId = rebalanceInfo.getMiddle();
				int vehicleToSend = (int) Math.floor(scale * rebalanceInfo.getRight() + rnd.nextDouble());
				// Note: we use probability to solve the problem of non-integer value of
				// vehileToSend after scaling.
				int numVehiclesInZone = 0;
				if (rebalancableVehiclesPerZone.get(departureZoneId) != null) {
					numVehiclesInZone = rebalancableVehiclesPerZone.get(departureZoneId).size();
				}

				if (vehicleToSend > numVehiclesInZone) {
					vehicleToSend = numVehiclesInZone;
				}

				if (vehicleToSend > 0) {
					for (int i = 0; i < vehicleToSend; i++) {
						// TODO change to "send to random link in a node"
						Link destinationLink = NetworkUtils.getNearestLink(network,
								zonalSystem.getZoneCentroid(arrivalZoneId));
						relocationList.add(new Relocation(rebalancableVehiclesPerZone.get(departureZoneId).get(0),
								destinationLink));
						rebalancableVehiclesPerZone.get(departureZoneId).remove(0);
					}
				}
			}
		}
		return relocationList;
	}

}
