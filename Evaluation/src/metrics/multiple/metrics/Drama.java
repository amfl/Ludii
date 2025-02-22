package metrics.multiple.metrics;

import java.util.ArrayList;
import java.util.Collections;

import metrics.Utils;
import metrics.multiple.MultiMetricFramework;
import other.concept.Concept;
import other.context.Context;
import other.trial.Trial;

/**
 * Difference between the winning players state evaluation and the 'maximum state evaluation of any player.
 * 
 * @author matthew.stephenson
 */
public class Drama extends MultiMetricFramework
{

	//-------------------------------------------------------------------------

	/**
	 * Constructor
	 */
	public Drama(final MultiMetricValue multiMetricValue, final Concept concept)
	{
		super
		(
			"Drama " + multiMetricValue.name(), 
			"Difference between the winning players state evaluation and the 'maximum state evaluation of any player.", 
			0.0, 
			1.0,
			concept,
			multiMetricValue
		);
	}
	
	//-------------------------------------------------------------------------

	@Override
	public Double[] getMetricValueList(final Trial trial, final Context context)
	{
		final ArrayList<Double> valueList = new ArrayList<>();
		
		// Get the highest ranked players based on the final player rankings.
		final ArrayList<Integer> highestRankedPlayers = Utils.highestRankedPlayers(trial, context);
		
		if (highestRankedPlayers.size() > 0)
		{
			for (int i = trial.numInitialPlacementMoves(); i < trial.numMoves(); i++)
			{
				// Get the highest state evaluation for any player.
				final ArrayList<Double> allPlayerStateEvaulations = Utils.allPlayerStateEvaulations(context);
				final double highestStateEvaluation = Collections.max(allPlayerStateEvaulations);
				
				// Get the average difference between the winning player(s) and the highest state evaluation.
				double differenceBetweenWinnersAndMax = 0.0;
				for (final int highestRankedPlayer : highestRankedPlayers)
				{
					final double playerStateEvaluation = allPlayerStateEvaulations.get(highestRankedPlayer);
					differenceBetweenWinnersAndMax += (highestStateEvaluation-playerStateEvaluation)/highestRankedPlayers.size();
				}
				
				valueList.add(Double.valueOf(differenceBetweenWinnersAndMax));
				context.game().apply(context, trial.getMove(i));
			}
		}
		else
		{
			System.out.println("ERROR, highestRankedPlayers list is empty");
		}
		
		return valueList.toArray(new Double[0]);
	}

	//-------------------------------------------------------------------------

}
