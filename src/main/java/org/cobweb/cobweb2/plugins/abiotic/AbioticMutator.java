package org.cobweb.cobweb2.plugins.abiotic;

import java.util.ArrayList;
import java.util.List;

import org.cobweb.cobweb2.core.Agent;
import org.cobweb.cobweb2.core.Location;
import org.cobweb.cobweb2.core.SimulationTimeSpace;
import org.cobweb.cobweb2.core.StateParameter;
import org.cobweb.cobweb2.core.StatePlugin;
import org.cobweb.cobweb2.plugins.EnvironmentMutator;
import org.cobweb.cobweb2.plugins.SpawnMutator;
import org.cobweb.cobweb2.plugins.StatefulMutatorBase;
import org.cobweb.cobweb2.plugins.StepMutator;

/**
 * AbioticMutator is an instance of Step and Spawn Mutator
 *
 * @author ???
 */
public class AbioticMutator extends StatefulMutatorBase<AbioticState> implements StepMutator, StatePlugin, EnvironmentMutator, SpawnMutator {

	public AbioticMutator() {
		super(AbioticState.class);
	}

	public AbioticParams params;

	private SimulationTimeSpace sim;

	/**
	 * @param loc location.
	 * @return The abiotic factor value at location
	 */
	public float getValue(int factor, Location loc) {
		float x = (float) loc.x / sim.getTopology().width;
		float y = (float) loc.y / sim.getTopology().height;

		AbioticFactor abioticFactor = params.factors.get(factor);
		float value = abioticFactor.getValue(x, y);
		return value;
	}

	/**
	 * @param l agent location.
	 * @param state Agent type specific parameters.
	 * @return The effect the abiotic factor has on the agent. 0 if agent is unaffected.
	 */
	private float effectAtLocation(int factor, Location l, AbioticState state) {
		float temp = getValue(factor, l);
		AbioticPreferenceParam agentFactorParams = state.factorStates[factor].agentParams.preference;

		return agentFactorParams.score(temp);
	}

	/**
	 * During a step
	 *
	 * @param agent The agent doing the step
	 * @param from Location the agent is moving from.
	 * @param to Location the agent is moving to.
	 */
	@Override
	public void onStep(Agent agent, Location from, Location to) {
		if (to == null) {
			return;
		}

		AbioticState state = getAgentState(agent);
		for (int i = 0; i < params.factors.size(); i++) {
			AbioticFactorState factorState = state.factorStates[i];
			float effect = effectAtLocation(i, to, state);
			float multiplier = 1 + effect;

			factorState.agentParams.parameter.modifyValue(causeKeys[i], agent, multiplier);
		}
	}

	@Override
	public void onSpawn(Agent agent) {
		AbioticAgentParams aPar = params.agentParams[agent.getType()].clone();
		setAgentState(agent, new AbioticState(aPar));
	}

	@Override
	public void onSpawn(Agent agent, Agent parent) {
		setAgentState(agent, getAgentState(parent).clone());
	}

	@Override
	public void onSpawn(Agent agent, Agent parent1, Agent parent2) {
		Agent parent = sim.getRandom().nextBoolean() ? parent1 : parent2;
		onSpawn(agent, parent);
	}

	@Override
	public void onDeath(Agent agent) {
		// nothing
	}

	/**
	 * Sets the parameters according to the simulation configuration.
	 * @param sim simulation sim
	 * @param params abiotic parameters from the simulation configuration.
	 */
	public void setParams(SimulationTimeSpace sim, AbioticParams params) {
		this.params = params;
		this.sim = sim;

		causeKeys = new CauseKey[params.factors.size()];
		for (int i = 0 ; i < causeKeys.length; i++) {
			causeKeys[i] = new CauseKey(i);
		}
	}

	private CauseKey[] causeKeys;

	private class CauseKey {
		private int index;
		public CauseKey(int index) {
			this.index = index;
		}

		@Override
		public String toString() {
			return AbioticMutator.this.toString() + ".factor[" + index + "]";
		}
	}


	@Override
	public List<StateParameter> getParameters() {
		List<StateParameter> res = new ArrayList<>(params.factors.size());
		for (int i = 0; i < params.factors.size(); i++) {
			res.add(new AbioticStatePenalty(i));
		}
		return res;
	}

	/**
	 * Magnitude of abiotic discomfort the agent is experiencing
	 */
	private class AbioticStatePenalty implements StateParameter {

		private int factor;

		public AbioticStatePenalty(int factor) {
			this.factor = factor;
		}

		@Override
		public String getName() {
			return String.format(AbioticParams.STATE_NAME_ABIOTIC_PENALTY, factor + 1);
		}

		@Override
		public double getValue(Agent agent) {
			double value = Math.abs(effectAtLocation(factor, agent.getPosition(), getAgentState(agent)));
			return value;
		}

	}

	@Override
	public void update() {
		for (AbioticFactor f : params.factors) {
			f.update(sim);
		}
	}

	@Override
	public void loadNew() {
		// nothing
	}

	@Override
	protected boolean validState(AbioticState value) {
		return value.factorStates.length == this.params.factors.size();
	}

}
