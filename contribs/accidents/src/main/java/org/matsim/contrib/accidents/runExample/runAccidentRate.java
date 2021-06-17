package org.matsim.contrib.accidents.runExample;

import org.matsim.contrib.accidents.AccidentRateModel;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

public class runAccidentRate {
    public static void main(String[] args) {
        //final String outputDirectoryRoot = "F:/models/muc/scenOutput/accidentTest/matsim/2011/";
        final String outputDirectoryRoot = "F:\\models\\healthModel\\muc/scenOutput/healthModel_0.05/matsim/2011/weekday/";

        Config config = ConfigUtils.createConfig();
        config.controler().setOutputDirectory(outputDirectoryRoot);
        config.controler().setRunId(String.valueOf(2011));
        final MutableScenario scenario = ScenarioUtils.createMutableScenario(config);
        scenario.getConfig().travelTimeCalculator().setTraveltimeBinSize(3600);
        scenario.addScenarioElement("accidentModelFile","F:\\models\\healthModel\\muc/input/accident/");
        AccidentRateModel model = new AccidentRateModel(scenario,1.f/(0.05f));
        model.runModelOnline();
    }
}
