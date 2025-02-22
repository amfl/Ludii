package game.functions.booleans.is.integer;

import java.util.BitSet;

import annotations.Hide;
import game.Game;
import game.functions.booleans.BaseBooleanFunction;
import game.functions.ints.IntFunction;
import other.concept.Concept;
import other.context.Context;

/**
 * Tests if an integer is even.
 * 
 * @author mrraow and cambolbro
 * 
 */
@Hide
public final class IsEven extends BaseBooleanFunction
{
	private static final long serialVersionUID = 1L;
	
	//-------------------------------------------------------------------------

	/** Boolean condition. */
	private final IntFunction value;

	/** Precomputed boolean. */
	private Boolean precomputedBoolean;

	//-------------------------------------------------------------------------

	/**
	 * @param value The value to test.
	 * @example (isEven (mover))
	 */
	public IsEven(final IntFunction value)
	{
		this.value = value;
	}

	//-------------------------------------------------------------------------

	@Override
	public boolean eval(final Context context)
	{
		if (precomputedBoolean != null)
			return precomputedBoolean.booleanValue();

		final int resolvedValue = value.eval(context);
		return (resolvedValue & 1) == 0;
	}

	//-------------------------------------------------------------------------

	@Override
	public boolean isStatic()
	{
		return value.isStatic();
	}

	@Override
	public long gameFlags(final Game game)
	{
		return value.gameFlags(game);
	}
	
	@Override
	public BitSet concepts(final Game game)
	{
		final BitSet concepts = new BitSet();
		concepts.or(value.concepts(game));
		concepts.set(Concept.Even.id(), true);
		return concepts;
	}

	@Override
	public BitSet writesEvalContextRecursive()
	{
		final BitSet writeEvalContext = new BitSet();
		writeEvalContext.or(value.writesEvalContextRecursive());
		return writeEvalContext;
	}

	@Override
	public BitSet readsEvalContextRecursive()
	{
		final BitSet readEvalContext = new BitSet();
		readEvalContext.or(value.readsEvalContextRecursive());
		return readEvalContext;
	}

	@Override
	public void preprocess(final Game game)
	{
		value.preprocess(game);

		if (isStatic())
			precomputedBoolean = Boolean.valueOf(eval(new Context(game, null)));
	}

	@Override
	public boolean missingRequirement(final Game game)
	{
		boolean missingRequirement = false;
		missingRequirement |= value.missingRequirement(game);
		return missingRequirement;
	}

	@Override
	public boolean willCrash(final Game game)
	{
		boolean willCrash = false;
		willCrash |= value.willCrash(game);
		return willCrash;
	}
}
