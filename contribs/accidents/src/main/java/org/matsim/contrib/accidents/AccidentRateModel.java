package org.matsim.contrib.accidents;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.events.EventsManagerModule;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.scenario.ScenarioByInstanceModule;

public class AccidentRateModel {
    private static final Logger log = Logger.getLogger( AccidentRateModel.class );

    private String outputDirectory;
    private Scenario scenario;

    private AccidentsContext accidentsContext = null;

    public AccidentRateModel(Scenario scenario, String analysisOutputDirectory ) {
        this.scenario = scenario;
        this.outputDirectory = analysisOutputDirectory;

        if (!outputDirectory.endsWith("/")) {
            outputDirectory = outputDirectory + "/";
        }
    }

    public void run() {

        com.google.inject.Injector injector = Injector.createInjector( scenario.getConfig() , new AbstractModule(){
            @Override public void install(){
                install( new ScenarioByInstanceModule( scenario ) );
                install( new AccidentsModule() ) ;
                install( new EventsManagerModule() ) ;
            }
        }) ;

        log.info("Reading network file...");
        String networkFile;
        if (this.scenario.getConfig().controler().getRunId() == null || this.scenario.getConfig().controler().getRunId().equals("")) {
            networkFile = this.scenario.getConfig().controler().getOutputDirectory() + "output_plans.xml.gz";
        } else {
            networkFile = this.scenario.getConfig().controler().getOutputDirectory() + this.scenario.getConfig().controler().getRunId() + ".output_plans.xml.gz";
        }
        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(networkFile);
        log.info("Reading network file... Done.");


        log.info("Reading events file...");
        EventsManager events = injector.getInstance( EventsManager.class ) ;
        MatsimEventsReader eventsReader = new MatsimEventsReader(events);
        String eventsFile;
        if (this.scenario.getConfig().controler().getRunId() == null || this.scenario.getConfig().controler().getRunId().equals("")) {
            eventsFile = this.scenario.getConfig().controler().getOutputDirectory() + "output_events.xml.gz";
        } else {
            eventsFile = this.scenario.getConfig().controler().getOutputDirectory() + this.scenario.getConfig().controler().getRunId() + ".output_events.xml.gz";
        }
        AnalysisEventHandler analysisEventHandler = new AnalysisEventHandler();
        events.addHandler(analysisEventHandler);
        eventsReader.readFile(eventsFile); //car, bike, ped AADT are calculated by eventHandler
        log.info("Reading events file... Done.");


       /* log.info("Reading plans file...");
        StreamingPopulationReader plansReader = new StreamingPopulationReader(scenario);
        String plansFile;
        if (this.scenario.getConfig().controler().getRunId() == null || this.scenario.getConfig().controler().getRunId().equals("")) {
            plansFile = this.scenario.getConfig().controler().getOutputDirectory() + "output_plans.xml.gz";
        } else {
            plansFile = this.scenario.getConfig().controler().getOutputDirectory() + this.scenario.getConfig().controler().getRunId() + ".output_plans.xml.gz";
        }
        plansReader.readFile(plansFile);
        log.info("Reading plans file... Done.");*/


        //Preparation
        for (Link link : this.scenario.getNetwork().getLinks().values()) {
            AccidentLinkInfo info = new AccidentLinkInfo(link.getId());
            this.accidentsContext.getLinkId2info().put(link.getId(), info);
        }

        for (Person person : this.scenario.getPopulation().getPersons().values()) {
            AccidentAgentInfo info = new AccidentAgentInfo(person.getId());
            this.accidentsContext.getPersonId2info().put(person.getId(), info);
        }

        log.info("Link accident rate calculation start.");
        for (AccidentType accidentType : AccidentType.values()){
            for (AccidentSeverity accidentSeverity : AccidentSeverity.values()){
                AccidentRateCalculation calculator = new AccidentRateCalculation(accidentsContext, analysisEventHandler, accidentType, accidentSeverity);
                calculator.run(this.scenario.getNetwork().getLinks().values());
            }
        }
        log.info("Link accident rate calculation completed.");



        log.info("Agent crash risk calculation start.");
        //calculateAgentCrashRisk(scenario);
        for (Person pp : this.scenario.getPopulation().getPersons().values()){
            AccidentAgentInfo personInfo = this.accidentsContext.getPersonId2info().get(pp.getId());
            double lightInjuryRisk = .0;
            double severeInjuryRisk = .0;
            for(Id<Link> linkId : personInfo.getLinkId2time2mode().keySet()){
                AccidentLinkInfo linkInfo = this.accidentsContext.getLinkId2info().get(linkId);
                for(int hour : personInfo.getLinkId2time2mode().get(linkId).keySet()){
                    String mode = personInfo.getLinkId2time2mode().get(linkId).get(hour);
                    switch (mode){
                        case "car":
                            lightInjuryRisk += linkInfo.getLightCrashRateByAccidentTypeByTime().get(AccidentType.CAR).get(hour);
                            severeInjuryRisk += linkInfo.getSevereFatalCrashRateByAccidentTypeByTime().get(AccidentType.CAR).get(hour);
                            break;
                        case "bike":
                            lightInjuryRisk += linkInfo.getLightCrashRateByAccidentTypeByTime().get(AccidentType.BIKECAR).get(hour);
                            severeInjuryRisk += linkInfo.getSevereFatalCrashRateByAccidentTypeByTime().get(AccidentType.BIKECAR).get(hour);
                            lightInjuryRisk += linkInfo.getLightCrashRateByAccidentTypeByTime().get(AccidentType.BIKEBIKE).get(hour);
                            severeInjuryRisk += linkInfo.getSevereFatalCrashRateByAccidentTypeByTime().get(AccidentType.BIKEBIKE).get(hour);
                            break;
                        case "walk":
                            lightInjuryRisk += linkInfo.getLightCrashRateByAccidentTypeByTime().get(AccidentType.PED).get(hour);
                            severeInjuryRisk += linkInfo.getSevereFatalCrashRateByAccidentTypeByTime().get(AccidentType.PED).get(hour);
                            break;
                        default:
                            throw new RuntimeException("Undefined mode " + mode);
                    }
                }
            }
            personInfo.setLightInjuryRisk(lightInjuryRisk);
            personInfo.setSevereInjuryRisk(severeInjuryRisk);
        }
        log.info("Agent crash risk calculation completed.");
    }




}
