package org.matsim.contrib.accidents;

import org.matsim.api.core.v01.network.Link;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class AccidentRateCalculation {

    private AnalysisEventHandler analzyer;
    private AccidentsContext accidentsContext;
    private Map<String, Double> binaryLogitCoef;
    private Map<String, Double> poissonCoef;
    private Map<Integer, Double> timeOfDayCoef;
    private AccidentType accidentType;
    private AccidentSeverity accidentSeverity;

    public AccidentRateCalculation(AccidentsContext accidentsContext, AnalysisEventHandler analzyer, AccidentType accidentType, AccidentSeverity accidentSeverity) {
        this.accidentsContext = accidentsContext;
        this.analzyer = analzyer;
        this.accidentType = accidentType;
        this.accidentSeverity = accidentSeverity;
        this.binaryLogitCoef =
                new AccidentRateModelCoefficientReader(accidentType, accidentSeverity,"path").readData();
        this.poissonCoef =
                new AccidentRateModelCoefficientReader(accidentType, accidentSeverity,"path").readData();
        this.timeOfDayCoef = new AccidentRateModelCoefficientReader(accidentType, accidentSeverity,"path").readTimeOfDayData();
    }

    protected void run(Collection<? extends Link> links) {
        for ( Link link : links){
            computeLinkCrashRate(link);
        }
    }

    //TODO:UK data convert number of accidents to number of victims
    private void computeLinkCrashRate(Link link) {

        double probZeroCrash = getProbabilityZeroCrashBinaryLogit(link);
        double meanCrash = getMeanCrashPoisson(link);
        double finalCrashRate = meanCrash*(1-probZeroCrash);

        //convert 3-year crash rate to daily crash exposure
        String mode;
        switch (accidentType){
            case CAR:
                mode = "car";
                break;
            case PED:
                mode = "walk";
                break;
            case BIKECAR:
                mode = "bike";
                break;
            case BIKEBIKE:
                mode = "bike";
                break;
            default:
                mode = "null";
        }


        if(!"null".equals(mode)){
            double dailyCrashExposure = finalCrashRate/3/(analzyer.getDemand(link.getId(),mode)*365);
            Map<Integer, Double> timeOfDayCrash = new HashMap<>();
            for(int hour : timeOfDayCoef.keySet()){
                timeOfDayCrash.put(hour, dailyCrashExposure*timeOfDayCoef.get(hour));
            }

            switch (accidentSeverity){
                case LIGHT:
                    this.accidentsContext.getLinkId2info().get(link.getId()).getLightCrashRateByAccidentTypeByTime().put(accidentType, timeOfDayCrash);
                    break;
                case SEVEREFATAL:
                    this.accidentsContext.getLinkId2info().get(link.getId()).getSevereFatalCrashRateByAccidentTypeByTime().put(accidentType, timeOfDayCrash);
                    break;
                default:
                    throw new RuntimeException("Undefined accident severity " + accidentSeverity);
            }
        }else{
            throw new RuntimeException("Undefined accident type " + accidentType);
        }
    }


    private double getProbabilityZeroCrashBinaryLogit(Link link){
        double utilityZeroCrash = 0.;
        utilityZeroCrash += binaryLogitCoef.get("intercept");

        double carDailyDemand = analzyer.getDemand(link.getId(),"car");
        utilityZeroCrash += carDailyDemand * binaryLogitCoef.get("carAADTIn1000");

        double bikeDailyDemand = analzyer.getDemand(link.getId(),"bike");
        utilityZeroCrash += bikeDailyDemand * binaryLogitCoef.get("bikeAADTIn1000");

        double pedDailyDemand = analzyer.getDemand(link.getId(),"walk");
        utilityZeroCrash += pedDailyDemand * binaryLogitCoef.get("pedestrianAADTIn1000");


        String linkType = link.getAttributes().getAttribute("accidentLinkType").toString();
        switch (linkType) {
            case "motorway":
                utilityZeroCrash += carDailyDemand * binaryLogitCoef.get("carAADTIn1000_motorway");
                utilityZeroCrash += binaryLogitCoef.get("motorway");
                break;
            case "primary":
                utilityZeroCrash += carDailyDemand * binaryLogitCoef.get("carAADTIn1000_primary");
                utilityZeroCrash += binaryLogitCoef.get("primary");
                break;
            case "secondary":
                utilityZeroCrash += carDailyDemand * binaryLogitCoef.get("carAADTIn1000_secondary");
                utilityZeroCrash += binaryLogitCoef.get("secondary");
                break;
            case "tertiary":
                utilityZeroCrash += carDailyDemand * binaryLogitCoef.get("carAADTIn1000_tertiary");
                utilityZeroCrash += binaryLogitCoef.get("tertiary");
                break;
            default:
                utilityZeroCrash += carDailyDemand * binaryLogitCoef.get("carAADTIn1000_residential");
                utilityZeroCrash += binaryLogitCoef.get("residential");
        }

        double linkLength = link.getLength();
        utilityZeroCrash += Math.log(linkLength) * binaryLogitCoef.get("linkLength_log");

        double intersections = Double.parseDouble(link.getAttributes().getAttribute("intersections").toString());
        utilityZeroCrash += intersections * binaryLogitCoef.get("intersections");

        return  Math.exp(utilityZeroCrash) / (1. + Math.exp(utilityZeroCrash));
    }

    private double getMeanCrashPoisson(Link link){
        double meanCrashRate = 0.;
        meanCrashRate += poissonCoef.get("intercept");

        double carDailyDemand = analzyer.getDemand(link.getId(),"car");
        meanCrashRate += carDailyDemand * poissonCoef.get("carAADTIn1000");

        double bikeDailyDemand = analzyer.getDemand(link.getId(),"bike");
        meanCrashRate += bikeDailyDemand * poissonCoef.get("bikeAADTIn1000");

        double pedDailyDemand = analzyer.getDemand(link.getId(),"walk");
        meanCrashRate += pedDailyDemand * poissonCoef.get("pedestrianAADTIn1000");


        String linkType = link.getAttributes().getAttribute("accidentLinkType").toString();
        switch (linkType) {
            case "motorway":
                meanCrashRate += carDailyDemand * poissonCoef.get("carAADTIn1000_motorway");
                meanCrashRate += poissonCoef.get("motorway");
                meanCrashRate += poissonCoef.get("motorwayCalibration");
                break;
            case "primary":
                meanCrashRate += carDailyDemand * poissonCoef.get("carAADTIn1000_primary");
                meanCrashRate += poissonCoef.get("primary");
                meanCrashRate += poissonCoef.get("primaryCalibration");
                break;
            case "secondary":
                meanCrashRate += carDailyDemand * poissonCoef.get("carAADTIn1000_secondary");
                meanCrashRate += poissonCoef.get("secondary");
                meanCrashRate += poissonCoef.get("secondaryCalibration");
                break;
            case "tertiary":
                meanCrashRate += carDailyDemand * poissonCoef.get("carAADTIn1000_tertiary");
                meanCrashRate += poissonCoef.get("tertiary");
                meanCrashRate += poissonCoef.get("tertiaryCalibration");
                break;
            default:
                meanCrashRate += carDailyDemand * poissonCoef.get("carAADTIn1000_residential");
                meanCrashRate += poissonCoef.get("residential");
                meanCrashRate += poissonCoef.get("residentialCalibration");
        }

        double linkLength = link.getLength();
        meanCrashRate += Math.log(linkLength) * poissonCoef.get("linkLength_log");

        double intersections = Double.parseDouble(link.getAttributes().getAttribute("intersections").toString());
        meanCrashRate += intersections * poissonCoef.get("intersections");

        return Math.exp(meanCrashRate);
    }
}
