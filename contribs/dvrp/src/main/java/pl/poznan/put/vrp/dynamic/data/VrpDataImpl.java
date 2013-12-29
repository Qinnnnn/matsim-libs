/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package pl.poznan.put.vrp.dynamic.data;

import java.util.*;

import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.network.VrpGraph;


/**
 * @author michalm
 */
public class VrpDataImpl
    implements VrpData
{
    private final List<Depot> depots = new ArrayList<Depot>();
    private final List<Customer> customers = new ArrayList<Customer>();
    private final List<Vehicle> vehicles = new ArrayList<Vehicle>();
    private final List<Request> requests = new ArrayList<Request>();

    private int time;

    private VrpGraph vrpGraph;

    private VrpDataParameters parameters;


    @Override
    public List<Depot> getDepots()
    {
        return depots;
    }


    @Override
    public List<Customer> getCustomers()
    {
        return customers;
    }


    @Override
    public List<Vehicle> getVehicles()
    {
        return vehicles;
    }


    @Override
    public List<Request> getRequests()
    {
        return requests;
    }


    @Override
    public int getTime()
    {
        return time;
    }


    @Override
    public VrpGraph getVrpGraph()
    {
        return vrpGraph;
    }


    @Override
    public VrpDataParameters getParameters()
    {
        return parameters;
    }


    // SETTERS

    @Override
    public void addDepot(Depot depot)
    {
        depots.add(depot);
    }


    @Override
    public void addCustomer(Customer customer)
    {
        customers.add(customer);
    }


    @Override
    public void addVehicle(Vehicle vehicle)
    {
        vehicles.add(vehicle);
    }


    @Override
    public void addRequest(Request request)
    {
        requests.add(request);
    }


    @Override
    public void setTime(int time)
    {
        this.time = time;
    }


    @Override
    public void setVrpGraph(VrpGraph vrpGraph)
    {
        this.vrpGraph = vrpGraph;
    }


    @Override
    public void setParameters(VrpDataParameters parameters)
    {
        this.parameters = parameters;
    }
}
