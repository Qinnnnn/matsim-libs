package org.matsim.contrib.accidents;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.events.EventsManagerModule;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioByInstanceModule;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class AccidentRateModel {
    private static final Logger log = Logger.getLogger( AccidentRateModel.class );
    private Scenario scenario;
    private final double SCALEFACTOR;
    private AccidentsContext accidentsContext = new AccidentsContext();
    private AnalysisEventHandler analysisEventHandler = new AnalysisEventHandler();
    private AnalysisEventHandlerOnline analysisEventHandlerOnline = new AnalysisEventHandlerOnline();

    private static final double CAR_LIGHT_LIGHT = 1.23;
    private static final double CAR_SEVERE_LIGHT = 0.29;
    private static final double CAR_SEVERE_SEVERE = 1.01;
    private static final double BIKECAR_LIGHT_LIGHT = 0.99;
    private static final double BIKECAR_SEVERE_LIGHT = 0.01;
    private static final double BIKECAR_SEVERE_SEVERE = 0.99;
    private static final double BIKEBIKE_LIGHT_LIGHT = 1.05;
    private static final double BIKEBIKE_SEVERE_LIGHT = 0.00;
    private static final double BIKEBIKE_SEVERE_SEVERE = 1.00;
    private static final double PED_LIGHT_LIGHT = 1.03;
    private static final double PED_SEVERE_LIGHT = 0.05;
    private static final double PED_SEVERE_SEVERE = 1.00;
    private int count;
    private int counterCar;
    private int counterBikePed;

    public AccidentRateModel(Scenario scenario, double scalefactor) {
        this.scenario = scenario;
        SCALEFACTOR = scalefactor;
    }

    public void runModelOnline() {
        com.google.inject.Injector injector = Injector.createInjector( scenario.getConfig() , new AbstractModule(){
            @Override public void install(){
                install( new ScenarioByInstanceModule( scenario ) );
                install( new AccidentsModule()) ;
                install( new EventsManagerModule());
            }
        }) ;

        log.info("Reading network file...");
        String networkFile;
        if (this.scenario.getConfig().controler().getRunId() == null || this.scenario.getConfig().controler().getRunId().equals("")) {
            networkFile = this.scenario.getConfig().controler().getOutputDirectory() + "car/" + "output_network.xml.gz";
        } else {
            networkFile = this.scenario.getConfig().controler().getOutputDirectory() + "car/" + this.scenario.getConfig().controler().getRunId() + ".output_network.xml.gz";
        }
        new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);
        log.info("Reading network file... Done.");

        log.info("Reading car plans file...");
        PopulationReader popReader = new PopulationReader(scenario);
        String plansFile;
        if (this.scenario.getConfig().controler().getRunId() == null || this.scenario.getConfig().controler().getRunId().equals("")) {
            plansFile = this.scenario.getConfig().controler().getOutputDirectory() + "car/" + "output_plans.xml.gz";
        } else {
            plansFile = this.scenario.getConfig().controler().getOutputDirectory() + "car/" + this.scenario.getConfig().controler().getRunId() + ".output_plans.xml.gz";
        }
        popReader.readFile(plansFile);
        log.info("Reading car plans file... Done.");

        log.warn("Total population:" + scenario.getPopulation().getPersons().size());

        log.info("Reading bikePed plans file...");
        String plansFileBikePed;
        if (this.scenario.getConfig().controler().getRunId() == null || this.scenario.getConfig().controler().getRunId().equals("")) {
            plansFileBikePed = this.scenario.getConfig().controler().getOutputDirectory() + "bikePed/" + "output_plans.xml.gz";
        } else {
            plansFileBikePed = this.scenario.getConfig().controler().getOutputDirectory() + "bikePed/" + this.scenario.getConfig().controler().getRunId() + ".output_plans.xml.gz";
        }
        popReader.readFile(plansFileBikePed);
        log.info("Reading bikePed plans file... Done.");
        log.warn("Total population:" + scenario.getPopulation().getPersons().size());



        analysisEventHandlerOnline.setScenario(scenario);
        analysisEventHandlerOnline.setAccidentsContext(accidentsContext);
        log.info("Reading car events file...");
        EventsManager events = injector.getInstance( EventsManager.class ) ;
        MatsimEventsReader eventsReader = new MatsimEventsReader(events);
        String eventsFile;
        if (this.scenario.getConfig().controler().getRunId() == null || this.scenario.getConfig().controler().getRunId().equals("")) {
            eventsFile = this.scenario.getConfig().controler().getOutputDirectory() + "car/" + "output_events.xml.gz";
        } else {
            eventsFile = this.scenario.getConfig().controler().getOutputDirectory() + "car/" + this.scenario.getConfig().controler().getRunId() + ".output_events.xml.gz";
        }
        events.addHandler(analysisEventHandlerOnline);
        eventsReader.readFile(eventsFile); //car AADT are calculated by eventHandler
        log.info("Reading car events file... Done.");

        log.info("Reading bike&ped events file...");
        String eventsFileBikePed;
        if (this.scenario.getConfig().controler().getRunId() == null || this.scenario.getConfig().controler().getRunId().equals("")) {
            eventsFileBikePed = this.scenario.getConfig().controler().getOutputDirectory() + "bikePed/" + "output_events.xml.gz";
        } else {
            eventsFileBikePed = this.scenario.getConfig().controler().getOutputDirectory() + "bikePed/" + this.scenario.getConfig().controler().getRunId() + ".output_events.xml.gz";
        }
        eventsReader.readFile(eventsFileBikePed); //car, bike, ped AADT are calculated by eventHandler
        log.info("Reading bike&ped events file... Done.");


        //Preparation
        for (Link link : this.scenario.getNetwork().getLinks().values()) {
            AccidentLinkInfo info = new AccidentLinkInfo(link.getId());
            this.accidentsContext.getLinkId2info().put(link.getId(), info);
        }
        log.info("Initializing all link-specific information... Done.");

//        for (Person person : this.scenario.getPopulation().getPersons().values()) {
//            AccidentAgentInfo info = new AccidentAgentInfo(person.getId());
//            this.accidentsContext.getPersonId2info().put(person.getId(), info);
//        }
//        log.info("Initializing all agent-specific information... Done.");


        log.info("Link accident frequency calculation (by type by time of day) start.");
        for (AccidentType accidentType : AccidentType.values()){
            for (AccidentSeverity accidentSeverity : AccidentSeverity.values()){
                String basePath = scenario.getScenarioElement("accidentModelFile").toString();
                AccidentRateCalculation calculator = new AccidentRateCalculation(SCALEFACTOR, accidentsContext, analysisEventHandlerOnline, accidentType, accidentSeverity, basePath);
                calculator.run(this.scenario.getNetwork().getLinks().values());
                log.info("Calculating " + accidentType + "_" + accidentSeverity + " crash rate done.");
            }
        }
        log.info("Link accident frequency calculation completed.");

        log.info("Link casualty frequency conversion (by type by time of day) start.");
        for (Link link : this.scenario.getNetwork().getLinks().values()) {
            casualtyRateCalculation(link);
        }
        log.info("Link casualty frequency conversion completed.");

        log.info("Link casualty exposure calculation start.");
        for (Link link : this.scenario.getNetwork().getLinks().values()) {
            computeLinkCasualtyExposure(link);
        }
        log.info(counterCar + "car links have no hourly traffic volume");
        log.info(counterBikePed + "bikeped links have no hourly traffic volume");
        log.info("Link casualty exposure calculation completed.");
    }

    public void runModelOffline() {
        com.google.inject.Injector injector = Injector.createInjector( scenario.getConfig() , new AbstractModule(){
            @Override public void install(){
                install( new ScenarioByInstanceModule( scenario ) );
                install( new AccidentsModule()) ;
                install( new EventsManagerModule());
            }
        }) ;

        log.info("Reading network file...");
        String networkFile;
        if (this.scenario.getConfig().controler().getRunId() == null || this.scenario.getConfig().controler().getRunId().equals("")) {
            networkFile = this.scenario.getConfig().controler().getOutputDirectory() + "car/" + "output_network.xml.gz";
        } else {
            networkFile = this.scenario.getConfig().controler().getOutputDirectory() + "car/" + this.scenario.getConfig().controler().getRunId() + ".output_network.xml.gz";
        }
        new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);
        log.info("Reading network file... Done.");

        log.info("Reading car plans file...");
        PopulationReader popReader = new PopulationReader(scenario);
        String plansFile;
        if (this.scenario.getConfig().controler().getRunId() == null || this.scenario.getConfig().controler().getRunId().equals("")) {
            plansFile = this.scenario.getConfig().controler().getOutputDirectory() + "car/" + "output_plans.xml.gz";
        } else {
            plansFile = this.scenario.getConfig().controler().getOutputDirectory() + "car/" + this.scenario.getConfig().controler().getRunId() + ".output_plans.xml.gz";
        }
        popReader.readFile(plansFile);
        log.info("Reading car plans file... Done.");

        log.warn("Total population:" + scenario.getPopulation().getPersons().size());

        log.info("Reading bikePed plans file...");
        String plansFileBikePed;
        if (this.scenario.getConfig().controler().getRunId() == null || this.scenario.getConfig().controler().getRunId().equals("")) {
            plansFileBikePed = this.scenario.getConfig().controler().getOutputDirectory() + "bikePed/" + "output_plans.xml.gz";
        } else {
            plansFileBikePed = this.scenario.getConfig().controler().getOutputDirectory() + "bikePed/" + this.scenario.getConfig().controler().getRunId() + ".output_plans.xml.gz";
        }
        popReader.readFile(plansFileBikePed);
        log.info("Reading bikePed plans file... Done.");
        log.warn("Total population:" + scenario.getPopulation().getPersons().size());



        analysisEventHandler.setScenario(scenario);
        analysisEventHandler.setAccidentsContext(accidentsContext);
        log.info("Reading car events file...");
        EventsManager events = injector.getInstance( EventsManager.class ) ;
        MatsimEventsReader eventsReader = new MatsimEventsReader(events);
        String eventsFile;
        if (this.scenario.getConfig().controler().getRunId() == null || this.scenario.getConfig().controler().getRunId().equals("")) {
            eventsFile = this.scenario.getConfig().controler().getOutputDirectory() + "car/" + "output_events.xml.gz";
        } else {
            eventsFile = this.scenario.getConfig().controler().getOutputDirectory() + "car/" + this.scenario.getConfig().controler().getRunId() + ".output_events.xml.gz";
        }
        events.addHandler(analysisEventHandler);
        eventsReader.readFile(eventsFile); //car AADT are calculated by eventHandler
        log.info("Reading car events file... Done.");

        log.info("Reading bike&ped events file...");
        String eventsFileBikePed;
        if (this.scenario.getConfig().controler().getRunId() == null || this.scenario.getConfig().controler().getRunId().equals("")) {
            eventsFileBikePed = this.scenario.getConfig().controler().getOutputDirectory() + "bikePed/" + "output_events.xml.gz";
        } else {
            eventsFileBikePed = this.scenario.getConfig().controler().getOutputDirectory() + "bikePed/" + this.scenario.getConfig().controler().getRunId() + ".output_events.xml.gz";
        }
        eventsReader.readFile(eventsFileBikePed); //car, bike, ped AADT are calculated by eventHandler
        log.info("Reading bike&ped events file... Done.");


        //Preparation
        for (Link link : this.scenario.getNetwork().getLinks().values()) {
            AccidentLinkInfo info = new AccidentLinkInfo(link.getId());
            this.accidentsContext.getLinkId2info().put(link.getId(), info);
        }
        log.info("Initializing all link-specific information... Done.");

//        for (Person person : this.scenario.getPopulation().getPersons().values()) {
//            AccidentAgentInfo info = new AccidentAgentInfo(person.getId());
//            this.accidentsContext.getPersonId2info().put(person.getId(), info);
//        }
//        log.info("Initializing all agent-specific information... Done.");


        log.info("Link accident frequency calculation (by type by time of day) start.");
        for (AccidentType accidentType : AccidentType.values()){
            for (AccidentSeverity accidentSeverity : AccidentSeverity.values()){
                String basePath = scenario.getScenarioElement("accidentModelFile").toString();
                AccidentRateCalculation calculator = new AccidentRateCalculation(SCALEFACTOR, accidentsContext, analysisEventHandler, accidentType, accidentSeverity,basePath);
                calculator.run(this.scenario.getNetwork().getLinks().values());
                log.info("Calculating " + accidentType + "_" + accidentSeverity + " crash rate done.");
            }
        }
        log.info("Link accident frequency calculation completed.");

        log.info("Link casualty frequency conversion (by type by time of day) start.");
        for (Link link : this.scenario.getNetwork().getLinks().values()) {
            casualtyRateCalculation(link);
        }
        log.info("Link casualty frequency conversion completed.");

        log.info("Link casualty exposure calculation start.");
        for (Link link : this.scenario.getNetwork().getLinks().values()) {
            computeLinkCasualtyExposure(link);
        }
        log.info(counterCar + "car links have no hourly traffic volume");
        log.info(counterBikePed + "bikeped links have no hourly traffic volume");
        log.info("Link casualty exposure calculation completed.");

        //only for offline
        log.info("Agent crash risk calculation start.");
        for (Person pp : this.scenario.getPopulation().getPersons().values()){
            AccidentAgentInfo personInfo = this.accidentsContext.getPersonId2info().get(pp.getId());
            if(personInfo==null){
                //log.warn("Person Id: " + pp.getId() + "is not analyzed in the handler!");
                count++;
                continue;
            }
            computeAgentCrashRisk(personInfo);
        }
        log.info(count + " agents are not analyzed in the handler!");
        log.info("Agent crash risk calculation completed.");

        try {
            writeOut();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void computeAgentCrashRisk(AccidentAgentInfo personInfo) {

        double lightInjuryRisk = .0;
        double severeInjuryRisk = .0;
        for(Id<Link> linkId : personInfo.getLinkId2time2mode().keySet()){
            AccidentLinkInfo linkInfo = this.accidentsContext.getLinkId2info().get(linkId);
            for(int hour : personInfo.getLinkId2time2mode().get(linkId).keySet()){
                String mode = personInfo.getLinkId2time2mode().get(linkId).get(hour);
                switch (mode){
                    case "car":
                        if(linkInfo.getLightCasualityExposureByAccidentTypeByTime().get(AccidentType.CAR)==null){
                            log.warn("link Id: " + linkId);
                        }

                        if(linkInfo.getLightCasualityExposureByAccidentTypeByTime().get(AccidentType.CAR).get(hour)==null){
                            log.warn("link Id: " + linkId + "| hour: " + hour);
                        }
                        lightInjuryRisk += linkInfo.getLightCasualityExposureByAccidentTypeByTime().get(AccidentType.CAR).get(hour);
                        severeInjuryRisk += linkInfo.getSevereFatalCasualityExposureByAccidentTypeByTime().get(AccidentType.CAR).get(hour);
                        break;
                    case "bike":
                        lightInjuryRisk += linkInfo.getLightCasualityExposureByAccidentTypeByTime().get(AccidentType.BIKECAR).get(hour);
                        severeInjuryRisk += linkInfo.getSevereFatalCasualityExposureByAccidentTypeByTime().get(AccidentType.BIKECAR).get(hour);
                        lightInjuryRisk += linkInfo.getLightCasualityExposureByAccidentTypeByTime().get(AccidentType.BIKEBIKE).get(hour);
                        severeInjuryRisk += linkInfo.getSevereFatalCasualityExposureByAccidentTypeByTime().get(AccidentType.BIKEBIKE).get(hour);
                        break;
                    case "walk":
                        lightInjuryRisk += linkInfo.getLightCasualityExposureByAccidentTypeByTime().get(AccidentType.PED).get(hour);
                        severeInjuryRisk += linkInfo.getSevereFatalCasualityExposureByAccidentTypeByTime().get(AccidentType.PED).get(hour);
                        break;
                    default:
                        throw new RuntimeException("Undefined mode " + mode);
                }
            }
        }
        personInfo.setLightInjuryRisk(lightInjuryRisk);
        personInfo.setSevereInjuryRisk(severeInjuryRisk);
    }

    private void casualtyRateCalculation(Link link) {
        AccidentLinkInfo linkInfo = this.accidentsContext.getLinkId2info().get(link.getId());
        for (AccidentType accidentType : AccidentType.values()){
            Map<Integer, Double> lightCasualtyByTime = new HashMap<>();
            Map<Integer, Double> severeCasualtyByTime = new HashMap<>();
            for(int hour = 0; hour<=24; hour++) {
                double lightCasualty = 0.0;
                double severeCasualty = 0.0;
                double lightCrash = 0.0;
                double severeCrash = 0.0;

                if(linkInfo.getLightCrashRateByAccidentTypeByTime().get(accidentType)!=null){
                    if(linkInfo.getLightCrashRateByAccidentTypeByTime().get(accidentType).get(hour)!=null){
                        lightCrash = linkInfo.getLightCrashRateByAccidentTypeByTime().get(accidentType).get(hour);
                    }
                }

                if(linkInfo.getSevereFatalCrashRateByAccidentTypeByTime().get(accidentType)!=null){
                    if(linkInfo.getSevereFatalCrashRateByAccidentTypeByTime().get(accidentType).get(hour)!=null){
                        severeCrash = linkInfo.getSevereFatalCrashRateByAccidentTypeByTime().get(accidentType).get(hour);
                    }
                }

                 switch (accidentType){
                    case CAR:
                        lightCasualty += lightCrash * CAR_LIGHT_LIGHT;
                        lightCasualty += severeCrash * CAR_SEVERE_LIGHT;
                        severeCasualty += severeCrash * CAR_SEVERE_SEVERE;
                        break;
                    case PED:
                        lightCasualty += lightCrash * PED_LIGHT_LIGHT;
                        lightCasualty += severeCrash * PED_SEVERE_LIGHT;
                        severeCasualty += severeCrash * PED_SEVERE_SEVERE;
                        break;
                    case BIKECAR:
                        lightCasualty += lightCrash * BIKECAR_LIGHT_LIGHT;
                        lightCasualty += severeCrash * BIKECAR_SEVERE_LIGHT;
                        severeCasualty += severeCrash * BIKECAR_SEVERE_SEVERE;
                        break;
                    case BIKEBIKE:
                        lightCasualty += lightCrash * BIKEBIKE_LIGHT_LIGHT;
                        lightCasualty += severeCrash * BIKEBIKE_SEVERE_LIGHT;
                        severeCasualty += severeCrash * BIKEBIKE_SEVERE_SEVERE;
                        break;
                    default:
                        throw new RuntimeException("Undefined accident type " + accidentType);
                }
                lightCasualtyByTime.put(hour,lightCasualty);
                severeCasualtyByTime.put(hour,severeCasualty);
            }
            linkInfo.getLightCasualityRateByAccidentTypeByTime().put(accidentType,lightCasualtyByTime);
            linkInfo.getSevereFatalCasualityRateByAccidentTypeByTime().put(accidentType,severeCasualtyByTime);
        }
    }

    private void computeLinkCasualtyExposure(Link link) {
        for (AccidentType accidentType : AccidentType.values()){
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

            if("null".equals(mode)){
                throw new RuntimeException("Undefined accident type " + accidentType);
            }

            Map<Integer, Double> lightCasualtyExposureByTime = new HashMap<>();
            Map<Integer, Double> severeCasualtyExposureByTime = new HashMap<>();
            for(int hour = 0; hour < 24; hour++) {
                double lightCasualty = this.accidentsContext.getLinkId2info().get(link.getId()).getLightCasualityRateByAccidentTypeByTime().get(accidentType).get(hour);
                double severeCasualty = this.accidentsContext.getLinkId2info().get(link.getId()).getSevereFatalCasualityRateByAccidentTypeByTime().get(accidentType).get(hour);
                double lightCasualtyExposure =0.;
                double severeCasualtyExposure = 0.;
                if(mode.equals("car")){
                    if(analysisEventHandler.getDemand(link.getId(),mode,hour)!=0){
                        lightCasualtyExposure = lightCasualty/((analysisEventHandler.getDemand(link.getId(),mode,hour))*SCALEFACTOR*1.5);
                        severeCasualtyExposure = severeCasualty/((analysisEventHandler.getDemand(link.getId(),mode,hour))*SCALEFACTOR*1.5);
                    }else{
                        //log.warn(link.getId()+mode+hour);
                        counterCar++;
                    }
                }else{
                    if(analysisEventHandler.getDemand(link.getId(),mode,hour)!=0){
                        lightCasualtyExposure = lightCasualty/(analysisEventHandler.getDemand(link.getId(),mode,hour)*SCALEFACTOR);
                        severeCasualtyExposure = severeCasualty/(analysisEventHandler.getDemand(link.getId(),mode,hour)*SCALEFACTOR);
                    }else{
                        counterBikePed++;
                    }
                }

                lightCasualtyExposureByTime.put(hour,lightCasualtyExposure);
                severeCasualtyExposureByTime.put(hour,severeCasualtyExposure);
            }
            this.accidentsContext.getLinkId2info().get(link.getId()).getLightCasualityExposureByAccidentTypeByTime().put(accidentType,lightCasualtyExposureByTime);
            this.accidentsContext.getLinkId2info().get(link.getId()).getSevereFatalCasualityExposureByAccidentTypeByTime().put(accidentType,severeCasualtyExposureByTime);

        }
    }

    public void writeOut () throws FileNotFoundException {
        String outputRisk = scenario.getConfig().controler().getOutputDirectory() + "injuryRisk.csv";
        StringBuilder risk = new StringBuilder();

        //write header
        risk.append("personId,lightInjury,severeFatalInjury");
        risk.append('\n');

        //write data
        for (Id<Person> person : accidentsContext.getPersonId2info().keySet()){

            risk.append(person.toString());
            risk.append(',');
            risk.append(accidentsContext.getPersonId2info().get(person).getLightInjuryRisk());
            risk.append(',');
            risk.append(accidentsContext.getPersonId2info().get(person).getSevereInjuryRisk());
            risk.append('\n');

        }
        writeToFile(outputRisk,risk.toString());
    }

    public static void writeToFile(String path, String building) throws FileNotFoundException {
        PrintWriter bd = new PrintWriter(new FileOutputStream(path, true));
        bd.write(building);
        bd.close();
    }

    public AccidentsContext getAccidentsContext() {
        return accidentsContext;
    }
}
