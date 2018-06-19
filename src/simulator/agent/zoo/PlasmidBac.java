package simulator.agent.zoo;

import idyno.SimTimer;

import java.awt.Color;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.PrimitiveIterator.OfDouble;

import org.omg.CORBA.PRIVATE_MEMBER;

import simulator.Simulator;
import simulator.agent.LocatedAgent;
import simulator.agent.SpecialisedAgent;
import simulator.agent.Species;
import simulator.geometry.ContinuousVector;
import utils.ExtraMath;
import utils.LogFile;
import utils.XMLParser;


/**
 * \brief Bacterium class that can host a number of Plasmids of differing
 * species.
 * 
 * <p>
 * Since this extends BactEPS, instances of this class may also produce and
 * excrete EPS.
 * </p>
 * 
 * <p>
 * This class is an amalgamation of the <b>EpiBac</b> class, written by Brian
 * Merkey, and the <b>MultiEpiBac</b> class, written by Sonia Martins.
 * </p>
 * 
 * @author Robert Clegg (r.j.clegg@bham.ac.uk)
 */
public class PlasmidBac extends BactEPS {
	/**
	 * Plasmids hosted by this bacterium.
	 */
	private LinkedList<Plasmid> _plasmidHosted = new LinkedList<Plasmid>();

	private LinkedList<PlasmidMemory> _plasmidMemories = new LinkedList<PlasmidMemory>();
	
	private Simulator simulator;

	private static int _now;
	// number of total attempted transfers in population
	public static int _numTotTry;
	// number of total transfers in population
	public static int _numTotTrans;
	protected double _tLost;

	/*************************************************************************
	 * CONSTRUCTORS
	 ************************************************************************/

	public PlasmidBac() {
		super();
		_speciesParam = new PlasmidBacParam();
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		PlasmidBac out = (PlasmidBac) super.clone();
		out._plasmidHosted = new LinkedList<Plasmid>();
		Plasmid newPlasmid;
		for (Plasmid aPlasmid : _plasmidHosted) {
			newPlasmid = (Plasmid) aPlasmid.clone();
			out._plasmidHosted.add(newPlasmid);
		}
		return out;
	}

	/**
	 * Called during species creation to build the progenitor.
	 */
	@Override
	public void initFromProtocolFile(Simulator aSimulator, XMLParser aSpeciesRoot) {
		boolean isCreatedByDivision = false;
		simulator=aSimulator;
			
		/*
		 * Initialisation of the BactEPS, and its superclasses.
		 */
		super.initFromProtocolFile(aSimulator, aSpeciesRoot);
		// Qian 10.2016: add following part. Otherwise, there will be no plasmid
		// since the beginning of simulation.
		/*
		 * Create hosted plasmids.
		 */
		
		//need to check birthday
		if (!aSimulator.getUseAgentFile()) {
			for (String aSpeciesName : aSpeciesRoot.getChildrenNames("plasmid")) {
				initPlasmid(aSpeciesName, isCreatedByDivision);
			}
		}
		/*
		 * Genealogy and size management.
		 */
		init();
		/*
		 * Finally, grab all the plasmid species names for reporting.
		 */
		this.collectPlasmidSpeciesNames(aSimulator);
	}

	@Override
	public void initFromResultFile(Simulator aSim, String[] singleAgentData, boolean createdByDivision) {
		boolean isCreatedByDivision = false;
		//Simulator yetAnotherSim = this._agentGrid.mySim;
		simulator=aSim;
		/*
		 * First, grab all the plasmid species names for reporting.
		 */
		this.collectPlasmidSpeciesNames(aSim);
		
		/*
		 * Find the position to start at by using length and number of values
		 * read.
		 */
		int nValsRead = 5 * aSim.numberOfDifferentPlasmids();
		int iDataStart = singleAgentData.length - nValsRead;
		/*
		 * Read in info from the result file IN THE SAME ORDER AS IT WAS OUTPUT.
		 * HGT parameters:
		 */
		
		// r - time last received
		// d - time last donated -> which is not currently written to result file
		// copy number is also not written to result file
		//	in result file we have:  plasmidID(but not name),tEntry,numHT,numVT
		
		int plasmidID, numHT, numVT;
		double tEntry;
		StringBuffer genealogy;
			
		for (String plasmidName : getPotentialPlasmidNames()) {
			
			plasmidID = Integer.parseInt(singleAgentData[iDataStart]);
			if (plasmidID!=-2){
				plasmidName = PlasmidParam.getPlasmidName(plasmidID);
				// tEntry is same as tReceived
				tEntry = Double.parseDouble(singleAgentData[iDataStart + 1]);
				String tmpString = singleAgentData[iDataStart + 2];
				genealogy  = new StringBuffer(tmpString);
				numHT= Integer.parseInt(singleAgentData[iDataStart + 3]);
				numVT= Integer.parseInt(singleAgentData[iDataStart + 4]);
			Plasmid aPlasmid = this.initPlasmid(plasmidName, isCreatedByDivision);
			aPlasmid.setInitFromResultFileDetails(tEntry, genealogy, numHT,numVT);
			}
		}
		/*
		 * Now go up the hierarchy with the rest of the data.
		 */
		String[] remainingSingleAgentData = new String[iDataStart];
		
		for (int i = 0; i < iDataStart; i++)
			remainingSingleAgentData[i] = singleAgentData[i];
		super.initFromResultFile(aSim, remainingSingleAgentData, createdByDivision);
	}

	@Override
	public PlasmidBac sendNewAgent() throws CloneNotSupportedException {
		PlasmidBac baby = (PlasmidBac) this.clone();
		updateSize();
		return baby;
	}

	@Override
	public void makeKid(boolean isCreatedByDivision) throws CloneNotSupportedException {
		/*
		 * Create the new instance and update the lineage.
		 */
		//LogFile.writeLog("growth " + getNetGrowth());
		
		/*	double deltaBiomass = getParticleMass(0);
		double growthRate = getNetGrowth();
		double specificGrowthRate = growthRate/deltaBiomass;
		*/
		//LogFile.writeLog("SpecificGrowthRate " + specificGrowthRate);
		
		double specificGrowthRate = getNetGrowth()/getParticleMass(0);
		
		PlasmidBac baby = sendNewAgent();
		
		this._myDivRadius = getDivRadius();
		baby._myDivRadius = getDivRadius();
		baby._myDeathRadius = getDeathRadius();
		recordGenealogy(this, baby);

		/*
		 * Share mass of all compounds between two daughter cells and compute
		 * new size.
		 */
		divideCompounds(baby, getBabyMassFrac());
		/*
		 * Compute movement to apply to both cells.
		 */
		if ( ! Simulator.isChemostat )
		{
			setDivisionDirection(getInteractDistance(baby)/2);
			baby._movement.subtract(_divisionDirection);
			_movement.add(_divisionDirection);
		}
		
		//changed because this is important only in biofilm
		/*setDivisionDirection(getInteractDistance(baby) / 2);
		baby._movement.subtract(_divisionDirection);
		_movement.add(_divisionDirection);*/
		/*
		 * Now register the agent inside the guilds and the agent grid.
		 */
		baby.registerBirth(isCreatedByDivision);
		/*
		 * Both daughter cells have an identical list of plasmids hosted.
		 * Loss-at-division could happen to either but not both, so first
		 * determine which daughter it could happen to, then determine if it
		 * does actually happen.
		 */
		for (int i = 0; i < this._plasmidHosted.size(); i++){
			if (ExtraMath.getUniRandDbl() < 0.5)
				this._plasmidHosted.get(i).applySegregation(specificGrowthRate);
			else
				baby._plasmidHosted.get(i).applySegregation(specificGrowthRate);
			recordGenealogy(this._plasmidHosted.get(i), baby._plasmidHosted.get(i));
		}
		baby.checkMissingPlasmid();
		this.checkMissingPlasmid();
	}

	/**
	 * \brief Create a new PlasmidBac agent (who a priori is registered in at
	 * least one container).
	 * 
	 * <p>
	 * This agent is located on the relevant grid, and may host plasmids if
	 * stated in the protocol file.
	 * </p>
	 * 
	 * @param position
	 *            Where to put this new agent.
	 * @param root
	 *            XMLParser used in determining if the cell should contain a
	 *            plasmid(s).
	 * @see {@link #createNewAgent(ContinuousVector)}
	 */
	public void createNewAgent(ContinuousVector position, XMLParser root, boolean isCreatedByDivision) {
		try {
			// Get a clone of the progenitor.
			PlasmidBac baby = (PlasmidBac) sendNewAgent();			
			baby.setFamily();
			baby.updateMass();

			/* If no mass defined, use the division radius to find the mass */
			// Note this should have been done already in initFromProtocolFile
			if (this._totalMass == 0.0) {
				setMassToHalfDivMass();
				LogFile.writeLogAlways("Warning: Bacterium.createNewAgent sets mass to half division mass");
			}
			// randomise its mass
			baby.randomiseMass();
			// System.out.println("RADIUS AT THIS POINT: "+this._totalRadius);
			baby.updateSize();

			this._myDivRadius = getDivRadius();
			baby._myDivRadius = getDivRadius();
			baby._myDeathRadius = getDeathRadius();

			baby.setLocation(position);
			baby.registerBirth(isCreatedByDivision);

			/*
			 * This is the part specific to PlasmidBac!
			 */

			Plasmid plasmid;
			for (String plName : root.getChildrenNames("plasmid")) {
				plasmid = baby.initPlasmid(plName, isCreatedByDivision);
				plasmid.setDetails(1, SimTimer.getCurrentTime(), -Double.MAX_VALUE);
			}
			baby.sendName();
		} catch (CloneNotSupportedException e) {
			LogFile.writeError(e, "PlasmidBac.createNewAgent()");
		}
	}

	/*************************************************************************
	 * BASIC GETTERS & SETTERS
	 ************************************************************************/

	@Override
	public PlasmidBacParam getSpeciesParam() {
		return (PlasmidBacParam) _speciesParam;
	}

	/**
	 * \brief Provides a list of all Plasmids hosted by this PlasmidBac host.
	 * 
	 * @return LinkedList of Plasmid objects hosted by this PlasmidBac.
	 */
	public LinkedList<Plasmid> getPlasmidsHosted() {
		return _plasmidHosted;
	}

	/*************************************************************************
	 * 
	 ************************************************************************/

	@Override
	public void internalStep() {
		/*
		 * First the plasmid stuff
		 */
		// Now checking of lost plasmids occurs in makeKid straigth after division
		//checkMissingPlasmid();
		if (Simulator.isChemostat) {
			chemostatConjugation();
		} else {
			biofilmConjugation();
		}

		/*
		 * Now the inherited BactEPS internalStep methods
		 */
		grow();
		updateSize();
		manageEPS();
		if (willDivide())
			divide();
		
		if (willDie())
			die(true);
	}

	/**
	 * \brief Remove any plasmids with copy number < 1
	 */
	protected void checkMissingPlasmid() {
		int nIter = this._plasmidHosted.size();
		Plasmid aPlasmid;
		for (int i = 0; i < nIter; i++) {
			aPlasmid = this._plasmidHosted.removeFirst();
			if (aPlasmid.getCopyNumber() < 1)
				this.killPlasmid(aPlasmid);
			else
				this._plasmidHosted.addLast(aPlasmid);
		}
		/*
		 * If any plasmids have been lost, refresh the reactions encoded by
		 * remaining plasmids: we should only remove those reactions uniquely
		 * provided by the plasmid that was lost.
		 */
		if (this._plasmidHosted.size() < nIter)
			this.refreshPlasmidReactions();
	}

	/**
	 * \brief Initialise a Plasmid to be hosted by this PlasmidBac.
	 * 
	 * <p>
	 * Note that the new Plasmid will have default copy number for the species
	 * it belongs to.
	 * </p>
	 * 
	 * @param plasmidName
	 *            Species name of the new Plasmid.
	 */
	private Plasmid initPlasmid(String plasmidName, boolean isCreatedByDivision) {
		Plasmid aPlasmid = null;
		try {
			aPlasmid = (Plasmid) _species.getSpecies(plasmidName).sendNewAgent();
			aPlasmid.init();
			this.welcomePlasmid(aPlasmid);
			aPlasmid.registerBirth(isCreatedByDivision);

		} catch (CloneNotSupportedException e) {
			LogFile.writeError(e, "PlasmidBac.initPlasmid(" + plasmidName + ")");
		}
		return aPlasmid;
	}

	/**
	 * \brief Receive a Plasmid into this host.
	 * 
	 * <p>
	 * <b>[Rob 31July2015]</b> Removed updates to conjugation time, etc: This is
	 * now handled by the donor plasmid.
	 * </p>
	 * 
	 * @param aPlasmid
	 *            Plasmid to be hosted by this PlasmidBac.
	 */
	public void welcomePlasmid(Plasmid aPlasmid) {
		this._plasmidHosted.add(aPlasmid);
		this.addPlasmidReactions(aPlasmid);
		boolean checkOldPlasmid = false;
		for (PlasmidMemory memory : _plasmidMemories) {
			if (memory.getPlasmidID() == aPlasmid.getPlasmidID()) {
				memory.addTPlasmidReceived(aPlasmid.getTimeReceived());
				checkOldPlasmid = true;
				break;
			}
		}
		if (!checkOldPlasmid) {
			PlasmidMemory memory = new PlasmidMemory(aPlasmid.getPlasmidID());
			memory.addTPlasmidReceived(aPlasmid.getTimeReceived());
			_plasmidMemories.add(memory);
		}
	}

	/**
	 * \brief Tell a Plasmid that it has been lost, and so should die.
	 * 
	 * <p>
	 * <b>[Rob 31July2015]</b> Changed the part about losing reactions so that
	 * we now refresh all plasmid-encoded reactions. This avoids the possibility
	 * of a reaction being lost when another hosted plasmid still encodes for
	 * it.
	 * </p>
	 * 
	 * @param plasmid
	 *            Plasmid that was hosted by this PlasmidBac, but has now been
	 *            lost.
	 */
	private void killPlasmid(Plasmid aPlasmid) {
		aPlasmid.die();
		//LogFile.writeLog("Plasmid " + aPlasmid.sendName() + " lost from " + this.sendName());
		
		String hostName = this.getHostName();
		String[] plasNames = hostName.split("_");
		String newName = new String();
		LinkedList<String> names = new LinkedList(Arrays.asList(plasNames));
		
		for ( int i=1; i<names.size(); i++){
			if(aPlasmid.getName().equals(names.get(i))){
				names.remove(i);
			}
		}
		newName+=names.get(0);
		for (int i=1;i<names.size();i++){
			newName +="_"+names.get(i); 
		}
		//LogFile.writeLog("new name is: " + newName);
		setNewSpecies(this,newName);
		this._tLost = SimTimer.getCurrentTime();
		for (PlasmidMemory memory : _plasmidMemories) {
			if (memory.getPlasmidID() == aPlasmid.getPlasmidID()) {
				memory.addTPlasmidLost(_tLost);
				break;
			}
		}
		
		//LogFile.writeLog("Plasmid " + aPlasmid.sendName() + " lost from " + hostName+" Now: " + this.getHostName());
	}

	/**
	 * \brief Add all the reactions encoded by a given Plasmid.
	 * 
	 * @param aPlasmid
	 *            Plasmid whose reactions should be conferred to this PlasmidBac
	 *            host.
	 */
	private void addPlasmidReactions(Plasmid aPlasmid) {
		for (int reacIndex : aPlasmid.getReactionsEncoded())
			this.addActiveReaction(allReactions[reacIndex], true);
	}

	/**
	 * \brief Lose all the reactions encoded by a given Plasmid.
	 * 
	 * <p>
	 * <b>[Rob 31July2015]</b> Beware of losing reactions that are also encoded
	 * by other plasmids: don't assume that each encoded reaction is unique to a
	 * particular plasmid species!
	 * </p>
	 * 
	 * @param aPlasmid
	 *            Plasmid whose reactions were conferred to this PlasmidBac
	 *            host.
	 */
	private void losePlasmidReactions(Plasmid aPlasmid) {
		for (int reacIndex : aPlasmid.getReactionsEncoded())
			this.removeReaction(allReactions[reacIndex]);
	}

	/**
	 * \brief Refresh all the reactions conferred to this PlasmidBac host by its
	 * hosted Plasmids.
	 */
	private void refreshPlasmidReactions() {
		for (Plasmid aPlasmid : this._plasmidHosted)
			this.losePlasmidReactions(aPlasmid);
		for (Plasmid aPlasmid : this._plasmidHosted)
			this.addPlasmidReactions(aPlasmid);
	}

	/**
	 * This function is only called for chemostat simulations where collisions
	 * are random and we use a kind of population level method - bad, but the
	 * only other option is to fit the IBM to the ODE model, which is also bad
	 */
	protected void doTheChemostatConjugation() {

		for (Plasmid aPlasmid : simulator.getDifferentPlasmids()) {
			// TODO: add checks for lag times (_tLastDonated and _tReceived):
			// aPlasmid.isReadyToConjugate()
			// and consider scaled tone
			// probToScreen *= this.getScaledTone();

			double chemostatVol = _species.domain.length_X * _species.domain.length_Y * _species.domain.length_Z;
			int numOfRec = simulator.getNumRecipients(aPlasmid.getName());
			double densityOfRecipients = numOfRec / chemostatVol;
			double dt = SimTimer.getCurrentTimeStep();
			double numOfDonors = simulator.getNumDonors(aPlasmid.getName());
			double collisionCoeficient = aPlasmid.getSpeciesParam().collisionCoeff;
			double cellsScreen = collisionCoeficient * numOfDonors * densityOfRecipients * dt + aPlasmid._testTally;
			// need to save remainder
			aPlasmid._testTally = (cellsScreen - Math.floor(cellsScreen));

			LinkedList<PlasmidBac> listOfAllRecipients = simulator.getRecList(aPlasmid);
			LinkedList<PlasmidBac> listPotentialRecipients = new LinkedList<PlasmidBac>();
			PlasmidBac potentialRecipient;
			int randyInt;
			for (int i = 1; i <= cellsScreen; i++) {
				randyInt = ExtraMath.getUniRandInt(numOfRec);
				potentialRecipient = listOfAllRecipients.get(randyInt);
				listPotentialRecipients.add(potentialRecipient);
			}

			for(int i=0;i<listPotentialRecipients.size();i++){

				PlasmidBac transconjugant = listPotentialRecipients.get(i);

				if (!sendPlasmid(aPlasmid, transconjugant))
					continue;

				listOfAllRecipients.remove(transconjugant);

				int instances = 0;
				for (int j = i + 1; j < listPotentialRecipients.size(); j++) {
					if (listPotentialRecipients.get(j).equals(transconjugant)) {
						instances++;
						listPotentialRecipients.remove(j);
						j--;
					}
				}

				for (int j = 0; j < instances; j++) {
					randyInt = ExtraMath.getUniRandInt(listOfAllRecipients.size());
					potentialRecipient = listOfAllRecipients.get(randyInt);
					listPotentialRecipients.add(potentialRecipient);
				}

			}
		}
	}

	
	protected void biofilmConjugation(){
		
		if (this._plasmidHosted.isEmpty())
			return;
		
		/* * Build a neighbourhood including only non-self Bacteria. The methods
		 * for this differ between chemostat and biofilm simulations: - In the
		 * biofilm, all non-self Bacteria within reach of the Plasmid's pilus
		 * should be included. Need to do this for each plasmid as parameters
		 * may differ
		 * 
		 * [Rob 31July2016] No need to shuffle this list:
		 * LocatedAgent.pickNeighbour() uses a randomly generated value to pick
		 * a random Bacterium.
		 */
		//LinkedList<Integer> numberOfDonors = new LinkedList<Integer>();

		for (Plasmid aPlasmid : this._plasmidHosted)
			if (aPlasmid.isReadyToConjugate()) {					
					
					// biofilm branch
					HashMap<Bacterium, Double> potentialRecps = new HashMap<Bacterium, Double>();
					potentialRecps = buildNbh(aPlasmid, aPlasmid.getPilusRange());
					// searchConjugation calls
					// Plasmid.updateTestTallyScaleScanRate() then calls
					// tryToSendPlasmid() while it canScan()
					this.searchConjugation(aPlasmid, potentialRecps);
				}
		}
	

	/**
	 * \brief Ensure chemostat conjugation occurs only once per timestep
	 */
	void chemostatConjugation() {

		if (_now != SimTimer.getCurrentIter()) {
			_now = SimTimer.getCurrentIter();
			_numTotTry = 0;
			_numTotTrans = 0;
			doTheChemostatConjugation();
			//LogFile.writeLog("_agentGrid.agentList.size() " + _agentGrid.agentList.size());
		} else
			return;
	}

	// need to update comment. Method screenAllPartners() does not exsist anymore, 
	/**
	 * \brief This is used only for biofilms, whereas in chemostats,
	 * screenAllPartners() is used Add all non-self Bacteria within reach of
	 * this to a HashMap of potential recipients.
	 * 
	 * <p>
	 * The double values in the HashMap correspond to the Bacterium's
	 * probability of being selected at random. If the species parameter
	 * <i>scaleScanProb</i> is false (default) then these probabilities are all
	 * equal, but if it is true then they are scaled by the distance from the
	 * host (this cell).
	 * </p>
	 * 
	 * <p>
	 * Parameter <b>nbhRadius</b> is typically the pilus length.
	 * </p>
	 * 
	 * @param nbhRadius
	 *            double length (in um) of the maximum cell surface-surface
	 *            distance for another Bacterium to be considered a neighbor.
	 */
	public HashMap<Bacterium, Double> buildNbh(Plasmid aPlasmid, double nbhRadius) {
		HashMap<Bacterium, Double> out = new HashMap<Bacterium, Double>();
		/*
		 * nbhRadius gives the distance OUTSIDE the donor agent that touches a
		 * recipient agent, and so we need to subtract the radii from
		 * getDistance() or add the radii to nbhRadius.
		 * 
		 * getDistance(aLocAgent) gets distance between cell centres.
		 */
		double donorRadius = this.getRadius(false);
		/*
		 * Find all potential neighbors in the neighboring grid elements by
		 * updating _myNeighbors
		 */
		this.getPotentialShovers(nbhRadius + donorRadius);
		/*
		 * Now remove agents that are too far away in Euclidean space rather
		 * than the grid
		 */
		double distance;
		double recipRadius;
		double probVar = 1.0;
		double cumulativeProb = 0.0;
		for (LocatedAgent recip : _myNeighbors) {
			/*
			 * First check that the potential recipient is not the current host,
			 * and that it is a Bacterium (or subclass, e.g. PlasmidBac)
			 */
			if (recip == this || !(recip instanceof Bacterium))
				continue;
			/*
			 * Now filter by the Euclidean distance between cell surfaces.
			 */
			recipRadius = recip.getRadius(false);
			distance = getDistance(recip) - donorRadius - recipRadius;
			if (distance > nbhRadius)
				continue;
			/*
			 * Finally, add the cell, together with a probability variable. By
			 * default, all potential recipients are treated equally. If
			 * scaleScanProb is true, scale the probability by the distance from
			 * the donor (reasoning is similar to the intensity of sunlight as a
			 * function of distance from the Sun's surface).
			 */
			if (getSpeciesParam().scaleScanProb)
				probVar = ExtraMath.sq(donorRadius / (donorRadius + distance));
			out.put((Bacterium) recip, probVar);
			cumulativeProb += probVar;
		}
		this._myNeighbors.clear();
		/*
		 * Now scale all probabilities so that they sum to one.
		 */
		scaleProbabilities(out, cumulativeProb);
		return out;
	}

	/**
	 * \brief Scale all probability variables in a given HashMap by 1/sum.
	 * 
	 * <p>
	 * Accepting <b>sum</b> as input, rather than calculating it directly, is a
	 * shortcut but depends on an accurate value of <b>sum</b>.
	 * 
	 * @param hm
	 *            HashMap<Bacterium, Double> linking each Bacterium with a
	 *            probability variable.
	 * @param sum
	 *            The sum of all these probability variables.
	 */
	private void scaleProbabilities(HashMap<Bacterium, Double> hm, double sum) {
		final double scaler = 1.0 / sum;
		hm.replaceAll((b, p) -> {
			return p * scaler;
		});
	}

	/**
	 * \brief Randomly select a Bacterium from the list of potential recipients
	 * generated in buildNbh() (used in biofilms only)
	 * 
	 * <p>
	 * Assumes the sum total of probability variables to be one.
	 * </p>
	 */
	protected Bacterium pickPotentialRecipient(HashMap<Bacterium, Double> hm) {
		double rand = ExtraMath.getUniRandDbl();
		double counter = 0.0;
		Iterator<Bacterium> it = hm.keySet().iterator();
		Bacterium bac;
		while (it.hasNext()) {
			bac = it.next();
			counter += hm.get(bac);
			if (counter > rand)
				return bac;
		}
		return null;
	}

	/**
	 * \brief Search for partners and try to send them a plasmid. Only used in
	 * biofilms.
	 * 
	 * @param aPlasmid
	 *            A Plasmid, hosted by this PlasmidBac, that should try to
	 *            conjugate with neighboring bacteria.
	 */
	public void searchConjugation(Plasmid aPlasmid, HashMap<Bacterium, Double> potentials) {
		/*
		 * If there is nobody to conjugate with, there is nothing more to do.
		 */
		if (potentials.isEmpty())
			return;
		/*
		 * First scales the plasmid's scan rate from its host's growth tone. The
		 * plasmid will calculate the number of neighbours it can look at per
		 * timestep. Then add this to the _testTally (there may be some overflow
		 * from the previous timestep).
		 * 
		 * This is not used in chemostat simulations, which require different
		 * treatment of collisions
		 */
		aPlasmid.updateTestTallyScaleScanRate(this.getScaledTone());
		/*
		 * Find a recipient(s) and try to send them a plasmid.
		 */
		while (aPlasmid.canScan()) {
			LogFile.writeLog("try to search conjugation");
			sendPlasmid(aPlasmid, pickPotentialRecipient(potentials));
		}
	}

	/**
	 * \brief Growth tone as a linear interpolation between the two cutoffs
	 * specified in the protocol file. If no cutoffs specified, growth tone will
	 * be 1.0
	 * 
	 * <p>
	 * See Merkey <i>et al</i> (2011) p.5 for more details: <a href=
	 * "http://onlinelibrary.wiley.com/doi/10.1111/j.1462-2920.2011.02535.x/full#ss18"
	 * >link</a>.
	 * </p>
	 * 
	 * @return double value in the range [0.0, 1.0]
	 */
	public double getScaledTone() {
		// Default is no growth rate dependence of plasmid transfer rate
		// If low and high tonus cutoffs are not set in the protocol file
		// they are set to -Double.MAX_VALUE by default
		// In this case this function should return 1
		Double lowTonus = getSpeciesParam().lowTonusCutoff;
		Double highTonus = getSpeciesParam().highTonusCutoff;
		Double theTonus = growthTone();
		Double scaledTone = 1.0; // default

		/*
		 * Too low, so return zero.
		 */
		if (theTonus < lowTonus)
			scaledTone = 0.0;
		/*
		 * Middle case, so do linear interpolation.
		 */
		else if (theTonus < highTonus)
			scaledTone = (theTonus - lowTonus) / (highTonus - lowTonus);
		/*
		 * If neither of these is called we have a high tonus, so just return
		 * 1.0 (same effect as no growth dependence).
		 */
		return scaledTone;
	}

	/**
	 * \brief Net growth rate as a fraction of the maximum rate.
	 * 
	 * <p>
	 * See Merkey <i>et al</i> (2011) p.5 for more details: <a href=
	 * "http://onlinelibrary.wiley.com/doi/10.1111/j.1462-2920.2011.02535.x/full#ss18"
	 * >link</a>.
	 * </p>
	 * 
	 * @return Net growth rate divided by maximum growth rate.
	 */
	public Double growthTone() {
		return _netGrowthRate / getSpeciesParam().maxGrowthRate;
	}

	/*************************************************************************
	 * REPORTING
	 ************************************************************************/

	/**
	 * \brief Using the Simulator species list, collect the names of all Plasmid
	 * species that could be hosted by this PlasmidBac species.
	 * 
	 * <p>
	 * This list will be used when writing output and reading in from output
	 * agent_State file.
	 * </p>
	 * 
	 * @param aSim
	 *            The Simulator this is running in.
	 */
	private void collectPlasmidSpeciesNames(Simulator aSim) {
		LogFile.writeLog("collectPlasmidSpeciesNames for PlasmidBac " + aSim.speciesDic.get(this.speciesIndex));
		for (Species aSpecies : aSim.speciesList) {
			if (!(aSpecies.getProgenitor() instanceof Plasmid))
				continue;
			LogFile.writeLog("Found plasmid " + aSpecies.speciesName);
			if (!((Plasmid) aSpecies.getProgenitor()).isHostCompatible(this)) {
				LogFile.writeLog("\tHost not compatible");
				continue;
			}
			LogFile.writeLog("\tHost compatible, adding plasmid to list of potential plasmids");
			getSpeciesParam().addPotentialPlasmidName((Plasmid) aSpecies.getProgenitor());
		}
	}

	/**
	 * \brief Provides a list of all plasmid species names that this PlasmidBac
	 * species could host.
	 * 
	 * @return ArrayList<String> of plasmid species names.
	 */
	private ArrayList<String> getPotentialPlasmidNames() {
		// a list of plasmids was checked
		return this.getSpeciesParam().potentialPlasmids;
	}

	public PlasmidMemory getPlasmidMemories(Plasmid pl) {
		PlasmidMemory plasmidMemory=null;
		for (PlasmidMemory memory : _plasmidMemories) {
			if (memory.getPlasmidID() == pl.getPlasmidID()) {
				plasmidMemory = memory;
			}
		}
		return plasmidMemory;
	}

	/**
	 * \brief Update the header for report output.
	 * 
	 * <p>
	 * For every plasmid species that could be hosted by this PlasmidBac
	 * species, the copy number, last reception time, and last donation time
	 * will always be reported, even if this is zero for some cells in this
	 * species.
	 * </p>
	 * 
	 * @see simulator.agent.LocatedAgent#sendHeader()
	 */
	@Override
	public StringBuffer sendHeader() {
		StringBuffer header = super.sendHeader();
		for (int i=0;i<simulator.numberOfDifferentPlasmids();i++){
		header.append(",plasmidID,tEntry,genealogy,numHT,numVT");
		}
		return header;
	}

	/**
	 * \brief Creates an output string of information generated on this
	 * particular agent.
	 * 
	 * Used in creation of results files. Writes the data matching the header
	 * file.
	 * 
	 * @return String containing results associated with this agent.
	 */
	@Override
	
	
	public StringBuffer writeOutput() {
		// LogFile.writeLog("start of PlasmidBac.writeOutput");
		StringBuffer tempString = super.writeOutput();
		// String plasName; replaced plasName with plasID because parsing
		// strings in the XML output when almost all output are numbers is a
		// pain
		//LogFile.writeLog("number of different plasmids in writeOutput: "+ simulator.numberOfDifferentPlasmids());
		int plasID = -2;
		double tEntry = -2.0;
		int numHT = -2;
		int numVT = -2;
		StringBuffer genealogy = new StringBuffer("-2");
		if (!_plasmidHosted.isEmpty()) {
			for (Plasmid aPlasmid : _plasmidHosted) {
				plasID = aPlasmid.getPlasmidID();
				// birthDay is the time the plasmid is created, eg, due to entry or cell division
				//tEntry = aPlasmid.getBirthday();
				tEntry = aPlasmid._tReceived;
				// TODO: read this flag from protocol file
				boolean writeGenealogy = false;
				if (writeGenealogy) {
					genealogy = aPlasmid.getGenealogy();
				}
				numHT = aPlasmid.getNumHT();
				numVT = aPlasmid.getGeneration();
				tempString.append("," + plasID + "," + tEntry + "," + genealogy + "," + numHT + "," + numVT);
			}
			if(_plasmidHosted.size()!=simulator.numberOfDifferentPlasmids()) {
				for (int i=0;i<simulator.numberOfDifferentPlasmids()-_plasmidHosted.size();i++){
				plasID = -2;
				tEntry = -2.0;
				numHT = -2;
				numVT = -2;
				tempString.append("," + plasID + "," + tEntry + ","  + genealogy + "," + numHT + "," + numVT);
				}
			}
		}
		else {
			for(int i=0;i<simulator.numberOfDifferentPlasmids();i++){
			tempString.append("," + plasID + "," + tEntry + "," + genealogy + "," + numHT + "," + numVT);
			}
		}
		return tempString;
	}

	/*************************************************************************
	 * POV-RAY
	 ************************************************************************/

	@Override
	public String getName() {
		StringBuffer out = new StringBuffer(_species.speciesName);
		// TODO
		return out.toString();
	}
	
	/**
	 * For use by PlasmidBac to change name of the PlasmidBac when it gains/loses a plasmid
	 * Required for grouping of agents containing the same (set of) plasmid in agent_state file
	 * Makes bacterium belong to different species than it originally belonged 
	 * We don't check but assume that old and new species are of the same class
	 */
	@Override
	public void setNewSpecies(SpecialisedAgent aTarget, String newName){
		for(Species species : simulator.speciesList)
			if ( species.getSpeciesName().equals(newName)){
				_species.notifyDeath();
				_species = species;
				_species.notifyBirth();
				
				aTarget.setHostName((PlasmidBac) aTarget);
			}
	}

	@Override
	public Color getColor() {
		// TODO
		PlasmidBacParam param = getSpeciesParam();
		/*
		 * Recipients have no plasmid. Transconjugant received the plasmid after
		 * birth. Donor received the plasmid before/at birth. if (
		 * _plasmidHosted.isEmpty() ) return param.rColor; else if (
		 * (_plasmidHosted_numHT == 0 ) return param.tColor; else
		 */
		return param.dColor;
	}

	@Override
	public void writePOVColorDefinition(FileWriter fr) throws IOException {
		// TODO
	}
	
	// Method  adds plasmid to new host plasmid list
	public void addPlasmid(Plasmid aPlasmidToAdd){
		_plasmidHosted.add(aPlasmidToAdd);
	}
	
	/**
	 * Renames the host name of the target cell if a plasmid was successfully sent to it
	 * since the recipient becomes a donor
	 * doesn't work, just renames, but doesn't change the register in Species
	 * 
	 * @param aPlasmid
	 * @param aTarget
	 */
	public boolean sendPlasmid(Plasmid aPlasmid, Bacterium aTarget) {
		boolean plasmidDonated = aPlasmid.tryToSendPlasmid(aTarget);

		if (plasmidDonated) {
			// this can be the host and plasmid name combination, not just the 'hostName'
			String hostName = aTarget.getHostName();
			String[] plasNames = hostName.split("_");
			// the very first element of array is name of bacterium, not plasmid
			String newName = plasNames[0];
			//the first element of array is set to be the name of new plasmid being sent
			plasNames[0] = aPlasmid.getName();
			// Sort plasmid names alphabetically so that we don't get different names for cells with the same set of plasmids
			LinkedList<String> plasListAlph = new LinkedList<>(Arrays.asList(plasNames));
			plasListAlph.sort(Comparator.naturalOrder());
			for (String name : plasListAlph)
				newName +="_"+name; 
			
			setNewSpecies(aTarget,newName);
			
		}
		return plasmidDonated;
	}	

}
