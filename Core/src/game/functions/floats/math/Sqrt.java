package game.functions.floats.math;

import java.util.BitSet;

import game.Game;
import game.functions.floats.BaseFloatFunction;
import game.functions.floats.FloatFunction;
import other.concept.Concept;
import other.context.Context;

/**
 * Computes the square root of a value.
 * 
 * @author Eric.Piette
 */
public final class Sqrt extends BaseFloatFunction
{
	private static final long serialVersionUID = 1L;

	// -------------------------------------------------------------------------

	/** The value. */
	private final FloatFunction a;

	// -------------------------------------------------------------------------

	/**
	 * @param a The first value.
	 * @example (sqrt 5.5)
	 */
	public Sqrt(final FloatFunction a)
	{
		this.a = a;
	}

	// -------------------------------------------------------------------------

	@Override
	public float eval(Context context)
	{
		final float value = a.eval(context);
		if (value < 0)
			throw new IllegalArgumentException("Sqrt of a negative value is undefined.");

		return (float) Math.sqrt(value);
	}

	@Override
	public long gameFlags(Game game)
	{
		long flag = 0l;

		if (a != null)
			flag |= a.gameFlags(game);

		return flag;
	}

	@Override
	public BitSet concepts(Game game)
	{
		final BitSet concepts = new BitSet();

		if (a != null)
			concepts.or(a.concepts(game));

		concepts.set(Concept.Float.id(), true);
		concepts.set(Concept.Roots.id(), true);

		return concepts;
	}

	@Override
	public boolean isStatic()
	{
		return false;
	}

	@Override
	public void preprocess(Game game)
	{
		if (a != null)
			a.preprocess(game);
	}

	@Override
	public boolean missingRequirement(final Game game)
	{
		boolean missingRequirement = false;
		missingRequirement |= a.missingRequirement(game);
		return missingRequirement;
	}

	@Override
	public boolean willCrash(final Game game)
	{
		boolean willCrash = false;
		willCrash |= a.willCrash(game);
		return willCrash;
	}
}
