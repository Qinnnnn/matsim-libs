/* *********************************************************************** *
 * project: org.matsim.*
 * DeliberateAgent.java
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

/**
 * 
 */
package playground.johannes.mobsim;

import org.matsim.network.Link;
import org.matsim.plans.Plan;

/**
 * @author illenberger
 * 
 */
public class DeliberateAgent extends MobsimAgentDecorator<PlanAgent> {

	private IntradayStrategy strategy;
	
	/*
	 * TODO: Discuss the name of this field!
	 */
	private int replanCoolDownTime = 1;

	private double lastReplanTime;
	
	public DeliberateAgent(PlanAgent agent, IntradayStrategy strategy) {
		super(agent);
		this.strategy = strategy;
	}

	public void setReplanCoolDownTime(int time) {
		this.replanCoolDownTime = time;
	}
	
	public int getReplanCoolDownTime() {
		return replanCoolDownTime;
	}
	
	@Override
	public Link getNextLink(double time) {
		replan(time);
		return super.getNextLink(time);
	}

	public void replan(double time) {
		if (time - lastReplanTime > replanCoolDownTime) {
			lastReplanTime = time;
			
			Plan newPlan = strategy.replan(time);
			if (newPlan != null) {
				/*
				 * TODO: Do some plan validation, e.g., if size of new plan
				 * equals size of old plan.
				 */
				agent.getPerson().exchangeSelectedPlan(newPlan, false);
			}
		}
	}
}
