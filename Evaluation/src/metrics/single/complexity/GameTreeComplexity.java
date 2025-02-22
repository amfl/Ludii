package metrics.single.complexity;

import org.apache.commons.rng.RandomProviderState;

import game.Game;
import metrics.Metric;
import metrics.Utils;
import other.concept.Concept;
import other.context.Context;
import other.trial.Trial;

/**
 * Game Tree Complexity Estimate.
 * 
 * @author matthew.stephenson
 */
public class GameTreeComplexity extends Metric
{

	//-------------------------------------------------------------------------

	/**
	 * Constructor
	 */
	public GameTreeComplexity()
	{
		super
		(
			"Game Tree Complexity", 
			"Game Tree Complexity Estimate.", 
			0.0, 
			-1,
			Concept.GameTreeComplexity
		);
	}
	
	//-------------------------------------------------------------------------
	
	@Override
	public double apply
	(
			final Game game,
			final Trial[] trials,
			final RandomProviderState[] randomProviderStates
	)
	{
		double gameTreeComplexity = 0.0;
		for (int trialIndex = 0; trialIndex < trials.length; trialIndex++)
		{
			// Get trial and RNG information
			final Trial trial = trials[trialIndex];
			final RandomProviderState rngState = randomProviderStates[trialIndex];
			
			// Setup a new instance of the game
			final Context context = Utils.setupNewContext(game, rngState);
			
			double branchingFactor = 0.0;
			for (int i = trial.numInitialPlacementMoves(); i < trial.numMoves(); i++)
			{
				branchingFactor += Double.valueOf(context.game().moves(context).moves().size()) / trial.numMoves();
				context.game().apply(context, trial.getMove(i));
			}

			gameTreeComplexity += trial.numMoves() * Math.log10(branchingFactor);
		}

		return gameTreeComplexity / trials.length;
	}

}
