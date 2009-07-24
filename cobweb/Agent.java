package cobweb;


/**
 * The Agent class represents the physical notion of an Agent in a simulation. Instances of the Agent class are not
 * responsible for implementing the intelligence of the Agent, this is deferred to the Agent.Controller class.
 */
public abstract class Agent {
	private boolean alive = true;

	private static int nextID = 0;

	private static int makeID() {
		if ((nextID + 1) == Integer.MAX_VALUE) {
			nextID = Integer.MIN_VALUE;
		}
		return nextID++;
	}

	protected Environment.Location position;

	protected Controller controller;

	protected long id;

	protected Agent(Environment.Location pos, Controller ai) {
		position = pos;
		controller = ai;
		controller.addClientAgent(this); // this currently does absolutely
		// nothing for both simple and
		// complex implementations of
		// controller
		position.setAgent(this);
		position.getEnvironment().getScheduler().addSchedulerClient(this);

		id = makeID();
	}

	public long birthday() {
		return 0L;
	}

	/**
	 * Removes this Agent from the Environment.
	 */
	public void die() {
		if (!isAlive())
			throw new IllegalStateException("Already dead!");

		position.setAgent(null);
		alive = false;
		controller.removeClientAgent(this);
		position.getEnvironment().getScheduler().removeSchedulerClient(this);
	}

	/**
	 * @return int AgentPDAction
	 */
	public abstract int getAgentPDAction();

	/**
	 * @return int AgentPDStrategy
	 */
	public abstract int getAgentPDStrategy();

	public abstract java.awt.Color getColor();

	public Controller getController() {
		return controller;
	}

	public abstract void getDrawInfo(UIInterface theUI);

	public int getEnergy() {
		return -1;
	}

	/**
	 * @return the location this Agent occupies.
	 */
	public Environment.Location getPosition() {
		return position;
	}

	public boolean isAlive() {
		return alive;
	}

	/**
	 * Sets the position of the Agent.
	 */
	public void move(Environment.Location newPos) {
		newPos.setAgent(this);
		position.setAgent(null);
		position = newPos;
	}

	public abstract void setColor(java.awt.Color c);

	public abstract double similarity(Agent other);

	public abstract double similarity(int other);

	public int type() {
		return -1;
	}
}
