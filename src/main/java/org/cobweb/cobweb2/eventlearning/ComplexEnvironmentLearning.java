package org.cobweb.cobweb2.eventlearning;

import org.cobweb.cobweb2.SimulationConfig;
import org.cobweb.cobweb2.core.ComplexEnvironment;
import org.cobweb.cobweb2.core.LocationDirection;
import org.cobweb.cobweb2.core.SimulationInterface;
import org.cobweb.cobweb2.core.params.ComplexAgentParams;
import org.cobweb.cobweb2.production.ProductionParams;


public class ComplexEnvironmentLearning extends ComplexEnvironment {

	public ComplexEnvironmentLearning(SimulationInterface simulation) {
		super(simulation);
	}

	private LearningAgentParams learningData[];

	@Override
	protected void copyParamsFromParser(SimulationConfig p) {
		super.copyParamsFromParser(p);
		learningData = p.getLearningParams().getLearningAgentParams();
	}

	@Override
	public void setDefaultMutableAgentParam() {
		ComplexAgentLearning.setDefaultMutableParams(agentData, learningData, prodData);
	}

	@Override
	protected void spawnAgent(LocationDirection location, int agentType) {
		ComplexAgentLearning child = (ComplexAgentLearning)simulation.newAgent();
		child.init(this, agentType, location, (ComplexAgentParams) agentData[agentType].clone(),
				(ProductionParams)prodData[agentType].clone(),
				(LearningAgentParams)learningData[agentType].clone()); // Default
	}
}
