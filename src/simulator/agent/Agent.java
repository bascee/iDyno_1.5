/**
 * \package agent
 * \brief Package of utilities that create and manage agents in the simulation
 * and their participation in relevant reactions.
 * 
 * This package is part of iDynoMiCS v1.2, governed by the CeCILL license
 * under French law and abides by the rules of distribution of free software.
 * You can use, modify and/ or redistribute iDynoMiCS under the terms of the
 * CeCILL license as circulated by CEA, CNRS and INRIA at 
 * the following URL  "http://www.cecill.info".
 */
package simulator.agent;

import java.math.BigInteger;

import idyno.SimTimer;
import simulator.Simulator;
import utils.LogFile;
import utils.XMLParser;

/**
 * \brief Major class of iDynoMiCS - defines the agents that are involved in
 * an iDynoMiCS simulation.
 * 
 * Extended by a number of agent types.
 * 
 * @author Andreas Dötsch (andreas.doetsch@helmholtz-hzi.de), Helmholtz Centre
 * for Infection Research (Germany).
 * @author Laurent Lardon (lardonl@supagro.inra.fr), INRA, France.
 */
public abstract class Agent implements Cloneable
{
	/* Parameters common to all agents of this class ________________________ */
	/* Temporary variables stored in static fields __________________________ */
	/* Parameters common (strict equality) to all agents of a Species _________ */
	/* Parameters mutated from species parameters ___________________________ */

	/**
	 * Integer noting the last simulation timestep when this agent was stepped
	 */
	protected int _lastStep;

	/**
	 * The number of generations between the progenitor and the current agent
	 */
	protected int _generation = 0;
	
	/**
	 * Integer for the binary reading of the 0 and 1 coding the lineage. When
	 * a cells divides, one daughter has the index value 1, the other the
	 * index value 0, then this index is added on the left of the lineage
	 * description.
	 */
	protected BigInteger _genealogy  = BigInteger.ZERO;
	
	/**
	 * Integer noting the family which this agent belongs to
	 */
	protected int        _family     = 0;
	
	/**
	 * Integer noting the next family that any newly created agent will belong
	 * to.
	 */
	protected static int nextFamily  = 0;
	
	/**
	 * Time at which this agent was created.
	 */
	protected Double _birthday;
	
	/**
	 * \brief Initialise an agent object, setting its time of creation and
	 * thus the time it was last stepped.
	 */
	public Agent()
	{
		_birthday = SimTimer.getCurrentTime();
		_lastStep = SimTimer.getCurrentIter()-1;
	}
	
	/**
	 * \brief Initialise the agent from the protocol file.
	 * 
	 * Implemented by classes that extend this class.
	 * 
	 * @param aSimulator	The simulation object used to simulate the
	 * conditions specified in the protocol file.
	 * @param aSpeciesRoot	A Species mark-up within the specified protocol
	 * file.
	 */
	public void initFromProtocolFile(Simulator aSimulator,
													XMLParser aSpeciesRoot)
	{
		
	}
	
	/**
	 * \brief Create an agent using information in a previous state or
	 * initialisation file.
	 * 
	 * @param aSim	The simulation object used to simulate the conditions
	 * specified in the protocol file.
	 * @param singleAgentData	Data from the result or initialisation file 
	 * that is used to recreate this agent.
	 */
	public void initFromResultFile(Simulator aSim, String[] singleAgentData) 
	{
		// read in info from the result file IN THE SAME ORDER AS IT WAS OUTPUT
		_family     = Integer.parseInt(singleAgentData[0]);
		_genealogy  = new BigInteger(singleAgentData[1]);
		_generation = Integer.parseInt(singleAgentData[2]);
		_birthday   = Double.parseDouble(singleAgentData[3]);
	}
	
	/**
	 * \brief Creates a new agent from an existing one, and registers this new
	 * agent in the simulation.
	 * 
	 * @throws CloneNotSupportedException	Exception should the class not
	 * implement Cloneable.
	 */
	public void makeKid() throws CloneNotSupportedException 
	{

		Agent anAgent = (Agent) this.clone();
		// Now register the agent in the appropriate container
		registerBirth();
	}

	/**
	 * \brief Clones this agent object, creating a new progeny of this agent.
	 * 
	 * @throws CloneNotSupportedException	Exception should the class not
	 * implement Cloneable.
	 */
	@Override
	public Object clone() throws CloneNotSupportedException
	{
		return super.clone();
	}

	/**
	 * \brief Registers a created agent into a respective container.
	 * 
	 * Each agent must be referenced by one such container. Implemented by
	 * classes that extend Agent.
	 */
	public abstract void registerBirth();
	
	/**
	 * \brief Perform the next timestep of the simulation for this agent.
	 * 
	 * _lastStep is implemented to note that the agent has been stepped.
	 * Implemented fully by agent types that extend Agent.
	 */
	public void step()
	{
		_lastStep = SimTimer.getCurrentIter();
		internalStep();
	}
	
	/**
	 * \brief Called at each time step (under the control of the method Step
	 * of the class Agent to avoid multiple calls).
	 * 
	 * Implemented by classes that extend Agent.
	 */
	protected abstract void internalStep();
	
	/**
	 * \brief Specifies the header of the columns of output information for
	 * this agent.
	 * 
	 * Used in creation of results files.
	 * 
	 * @return	String specifying the header of each column of results
	 * associated with this agent.
	 */
	public StringBuffer sendHeader()
	{
		return new StringBuffer("family,genealogy,generation,birthday");
	}
	
	/**
	 * \brief Creates an output string of information generated on this
	 * particular agent.
	 * 
	 * Used in creation of results files. Data written matches the headers in
	 * sendHeader().
	 * 
	 * @return	String containing results associated with this agent.
	 */
	public StringBuffer writeOutput()
	{
		return new StringBuffer(
						_family+","+_genealogy+","+_generation+","+_birthday);
	}
	
	/**
	 * \brief Called when creating an agent : updates _generation and
	 * _genealogy field.
	 * 
	 * @param baby The newly created agent that is the next generation of this
	 * agent.
	 */
	protected void recordGenealogy(Agent baby) 
	{
		// Rob 18/1/11: Shuffled around slightly to include odd numbers
		//baby._genealogy = _genealogy+ExtraMath.exp2long(this._generation);
		// Rob 11/2/15: Changed to BigInteger
		baby._genealogy = new BigInteger("2");
		baby._genealogy.pow(this._generation);
		baby._genealogy.add(this._genealogy);
		
		this._generation++;
		baby._generation = this._generation;
		
		// We want to know if the genealogy goes negative.
		if ( baby._genealogy.signum() < 0 )
		{
			LogFile.writeLog("Warning: baby's genealogy has gone negative:");
			LogFile.writeLog("family "+baby._family+", genealogy "+
							baby._genealogy+", generation "+baby._generation);
		}
		
		// Rob 21/1/11: changed so that only the baby is given a new birthday
		// this._birthday = SimTimer.getCurrentTime();
		// baby._birthday = this._birthday;
		baby._birthday = SimTimer.getCurrentTime();
	}

	/**
	 * \brief Returns a string containing the family name and genealogy of
	 * this agent.
	 * 
	 * @return	String containing the family name and genealogy of this agent.
	 */
	public String sendName()
	{
		return _family+"-"+_genealogy;
	}
	
	/**
	 * \brief Set the family for this agent, based on the next family.
	 */
	public void giveName() 
	{
		_family = ++nextFamily;
	}
	
	/**
	 * \brief Return the simulation time at which this agent was created.
	 * 
	 * @return	Double noting the simulation time at which this agent was
	 * created.
	 */
	public Double getBirthday()
	{
		return this._birthday;
	}
	
	public BigInteger getGenealogy()
	{
		return this._genealogy;
	}
}
