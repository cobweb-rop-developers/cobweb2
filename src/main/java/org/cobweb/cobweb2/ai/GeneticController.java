/**
 *
 */
package org.cobweb.cobweb2.ai;

import org.cobweb.cobweb2.core.Agent;
import org.cobweb.cobweb2.core.ComplexAgent;
import org.cobweb.cobweb2.core.SeeInfo;
import org.cobweb.cobweb2.core.SimulationInternals;
import org.cobweb.cobweb2.core.Topology;
import org.cobweb.cobweb2.interconnect.StateParameter;
import org.cobweb.cobweb2.io.CobwebParam;
import org.cobweb.util.BitField;

/**
 * This class contains methods that set up the parameters for agents
 * that are used to influence the actions of the agents.
 *
 * @author ???
 *
 */
public class GeneticController implements Controller {

	private final static int TURN_LEFT = 0;
	private final static int TURN_RIGHT = 1;
	private final static int MOVE_STRAIGHT = 2;
	private final static int NONE = 3;

	private BehaviorArray ga;
	private int memorySize;
	private int commSize;

	private static final int INPUT_BITS = 8;

	private static final int OUTPUT_BITS = 2;

	private static final int ENERGY_THRESHOLD = 160;

	private GeneticControllerParams params;

	private SimulationInternals simulation;

	public GeneticController(SimulationInternals sim) { // NO_UCD (unused code) called through reflection
		simulation = sim;
	}

	/**
	 * Given the agent's energy, get the amount of energy to add to the array.
	 * @param energy The agent's energy.
	 */
	private int getEnergy(int energy) {
		final int maxEnergy = 3;

		if(energy > ENERGY_THRESHOLD) {
			return maxEnergy;
		} else {
			return (int) ((double) energy / (ENERGY_THRESHOLD) * 4.0);
		}
	}


	/**
	 * Converts the parameters of the agent into a behavior (turn left or right,
	 * step).
	 *
	 * @see BehaviorArray
	 * @see ComplexAgent#turnLeft()
	 * @see ComplexAgent#turnRight()
	 * @see ComplexAgent#step()
	 */
	@Override
	public void controlAgent(Agent baseAgent) {
		ComplexAgent theAgent = (ComplexAgent) baseAgent;

		BitField inputCode = getInputArray(theAgent);

		int[] outputArray = ga.getOutput(inputCode.intValue());

		int actionCode = outputArray[0];
		theAgent.setMemoryBuffer(outputArray[1]);
		theAgent.setCommOutbox(outputArray[2]);
		//whether to breed
		theAgent.setShouldReproduceAsex(outputArray[3] != 0);

		theAgent.setCommInbox(0);

		switch (actionCode) {
			case TURN_LEFT:
				theAgent.turnLeft();
				break;
			case TURN_RIGHT:
				theAgent.turnRight();
				break;
			case MOVE_STRAIGHT:
			case NONE:
				theAgent.step();
				break;
		}
	}

	/**
	 * Given an agent, read all information for it and compound it into an array.
	 * @param theAgent The agent.
	 * @return The input array.
	 */
	private BitField getInputArray(ComplexAgent theAgent) {
		BitField inputCode = new BitField();

		//add the energy info to the array
		inputCode.add(getEnergy(theAgent.getEnergy()), 2);

		//add the direction the agent is facing to the array
		inputCode.add(simulation.getTopology()
				.getRotationBetween(Topology.NORTH, theAgent.getPosition().direction)
				.ordinal(), 2);

		//add the viewing info to the array
		SeeInfo get = theAgent.distanceLook();
		inputCode.add(get.getType(), 2);
		inputCode.add(get.getDist(), 2);

		//add the memory buffer to the array
		inputCode.add(theAgent.getMemoryBuffer(), memorySize);

		//add the communications to the array
		inputCode.add(theAgent.getCommInbox(), commSize);

		for (StateSize ss : params.agentParams.agentParams[theAgent.getType()].stateSizes) {
			StateParameter sp = simulation.getStateParameter(ss.name);
			double value = sp.getValue(theAgent);
			int val = (int) Math.round(value * ((1 << ss.size) - 1));
			inputCode.add(val, ss.size);
		}

		return inputCode;
	}

	@Override
	public CobwebParam getParams() {
		return params;
	}

	@Override
	public void setupFromEnvironment(int memory, int comm, CobwebParam params, int type) {
		memorySize = memory;
		commSize = comm;
		this.params = (GeneticControllerParams) params;
		int[] outputArray = { OUTPUT_BITS, memorySize, commSize, 1 };

		int inputSize = INPUT_BITS + memorySize + commSize;
		for (StateSize ss : this.params.agentParams.agentParams[type].stateSizes) {
			inputSize += ss.size;
		}

		ga = new BehaviorArray(inputSize, outputArray);
		ga.randomInit(this.params.randomSeed);
	}

	@Override
	public GeneticController createChildAsexual(float mutationRate) {
		GeneticController child = new GeneticController(simulation);

		child.ga = this.ga.copy(mutationRate);
		child.memorySize = this.memorySize;
		child.params = this.params;

		return child;
	}

	@Override
	public GeneticController createChildSexual(Controller parent2, float mutationRate) {
		if (!(parent2 instanceof GeneticController)) {
			throw new RuntimeException("Parent's controller type must match the child's");
		}
		GeneticController p2 = (GeneticController) parent2;

		GeneticController child = new GeneticController(simulation);

		child.ga = BehaviorArray.splice(this.ga, p2.ga, simulation.getRandom()).copy(mutationRate);
		child.memorySize = this.memorySize;
		child.params = this.params;

		return child;
	}

	/** return the measure of similiarity between this agent and the 'other'
	 ranging from 0.0 to 1.0 (identical)

	 */
	public double similarity(GeneticController other) {
		return ga.similarity(other.ga);
	}

}