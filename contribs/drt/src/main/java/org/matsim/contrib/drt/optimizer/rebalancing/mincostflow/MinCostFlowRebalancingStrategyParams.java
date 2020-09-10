/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.optimizer.rebalancing.mincostflow;

import static com.google.common.base.Preconditions.checkState;

import java.util.Map;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;

import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingParams;
import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author michalm
 */
public final class MinCostFlowRebalancingStrategyParams extends ReflectiveConfigGroup
		implements RebalancingParams.RebalancingStrategyParams {
	public static final String SET_NAME = "minCostFlowRebalancingStrategy";

	public enum RebalancingTargetCalculatorType {
		EstimatedDemand, EqualRebalancableVehicleDistribution, EqualVehicleDensity, EqualVehiclesToPopulationRatio
	}

	public enum ZonalDemandEstimatorType {
		PreviousIterationDemand
	}
	
	//TODO move this to the params for the target calculator
	public static final String TARGET_ALPHA = "targetAlpha";
	static final String TARGET_ALPHA_EXP = "alpha coefficient in linear target calculation."
			+ " In general, should be lower than 1.0 to prevent over-reacting and high empty mileage."
			+ " Used only for LinearRebalancingTarget";

	public static final String TARGET_BETA = "targetBeta";
	static final String TARGET_BETA_EXP = "beta constant in linear target calculation."
			+ " In general, should be lower than 1.0 to prevent over-reacting and high empty mileage."
			+ " Used only for LinearRebalancingTarget";

	public static final String REBALANCING_TARGET_CALCULATOR_TYPE = "rebalancingTargetCalculatorType";
	static final String REBALANCING_TARGET_CALCULATOR_TYPE_EXP =
			"Defines the calculator used for computing rebalancing targets per each zone"
					+ " (i.e. number of the desired vehicles)."
					+ " Can be one of [LinearRebalancingTarget, EqualRebalancableVehicleDistribution,"
					+ " EqualVehicleDensity, EqualVehiclesToPopulationRatio]."
					+ " Current default is LinearRebalancingTarget";

	public static final String ZONAL_DEMAND_AGGREGATOR_TYPE = "zonalDemandEstimatorType";
	static final String ZONAL_DEMAND_AGGREGATOR_TYPE_EXP = "Defines the methodology for demand estimation."
			+ " Can be one of [PreviousIterationDemand]. Current default is PreviousIterationDemand";

	@NotNull
	private RebalancingTargetCalculatorType rebalancingTargetCalculatorType = RebalancingTargetCalculatorType.EstimatedDemand;

	@Nullable
	@PositiveOrZero
	private Double targetAlpha = null;

	@Nullable
	@PositiveOrZero
	private Double targetBeta = null;

	@Nullable //required only if rebalancingTargetCalculatorType == EstimatedDemand
	private ZonalDemandEstimatorType zonalDemandEstimatorType = ZonalDemandEstimatorType.PreviousIterationDemand;

	public MinCostFlowRebalancingStrategyParams() {
		super(SET_NAME);
	}

	@Override
	protected void checkConsistency(Config config) {
		super.checkConsistency(config);

		if (rebalancingTargetCalculatorType == RebalancingTargetCalculatorType.EstimatedDemand) {
			checkState(zonalDemandEstimatorType != null,
					"zonalDemandEstimatorType is required if EstimatedDemand is used as rebalancing target");
			checkState(targetAlpha != null, "targetAlpha is required if EstimatedDemand is used as rebalancing target");
			checkState(targetBeta != null, "targetBeta is required if EstimatedDemand is used as rebalancing target");
		} else {
			checkState(zonalDemandEstimatorType == null,
					"zonalDemandEstimatorType should be null if the rebalancing target is not set to EstimatedDemand");
		}
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> map = super.getComments();
		map.put(TARGET_ALPHA, TARGET_ALPHA_EXP);
		map.put(TARGET_BETA, TARGET_BETA_EXP);
		map.put(ZONAL_DEMAND_AGGREGATOR_TYPE, ZONAL_DEMAND_AGGREGATOR_TYPE_EXP);
		return map;
	}

	/**
	 * @return -- {@value #TARGET_ALPHA_EXP}
	 */
	@StringGetter(TARGET_ALPHA)
	public Double getTargetAlpha() {
		return targetAlpha;
	}

	/**
	 * @param targetAlpha -- {@value #TARGET_ALPHA_EXP}
	 */
	@StringSetter(TARGET_ALPHA)
	public void setTargetAlpha(double targetAlpha) {
		this.targetAlpha = targetAlpha;
	}

	/**
	 * @return -- {@value #TARGET_BETA_EXP}
	 */
	@StringGetter(TARGET_BETA)
	public Double getTargetBeta() {
		return targetBeta;
	}

	/**
	 * @param targetBeta -- {@value #TARGET_BETA_EXP}
	 */
	@StringSetter(TARGET_BETA)
	public void setTargetBeta(double targetBeta) {
		this.targetBeta = targetBeta;
	}

	/**
	 * @return -- {@value #REBALANCING_TARGET_CALCULATOR_TYPE_EXP}
	 */
	@StringGetter(REBALANCING_TARGET_CALCULATOR_TYPE)
	public RebalancingTargetCalculatorType getRebalancingTargetCalculatorType() {
		return rebalancingTargetCalculatorType;
	}

	/**
	 * @param calculatorType -- {@value #REBALANCING_TARGET_CALCULATOR_TYPE_EXP}
	 */
	@StringSetter(REBALANCING_TARGET_CALCULATOR_TYPE)
	public void setRebalancingTargetCalculatorType(RebalancingTargetCalculatorType calculatorType) {
		this.rebalancingTargetCalculatorType = calculatorType;
	}

	/**
	 * @return -- {@value #ZONAL_DEMAND_AGGREGATOR_TYPE_EXP}
	 */
	@StringGetter(ZONAL_DEMAND_AGGREGATOR_TYPE)
	public ZonalDemandEstimatorType getZonalDemandEstimatorType() {
		return zonalDemandEstimatorType;
	}

	/**
	 * @param estimatorType -- {@value #ZONAL_DEMAND_AGGREGATOR_TYPE_EXP}
	 */
	@StringSetter(ZONAL_DEMAND_AGGREGATOR_TYPE)
	public void setZonalDemandEstimatorType(ZonalDemandEstimatorType estimatorType) {
		this.zonalDemandEstimatorType = estimatorType;
	}
}
