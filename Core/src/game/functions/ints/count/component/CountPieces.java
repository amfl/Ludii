package game.functions.ints.count.component;

import java.util.BitSet;
import java.util.List;

import annotations.Hide;
import annotations.Name;
import annotations.Opt;
import annotations.Or;
import game.Game;
import game.equipment.component.Component;
import game.functions.ints.BaseIntFunction;
import game.functions.ints.IntFunction;
import game.functions.region.RegionFunction;
import game.types.board.SiteType;
import game.types.play.RoleType;
import game.types.state.GameType;
import gnu.trove.list.array.TIntArrayList;
import other.PlayersIndices;
import other.context.Context;
import other.location.Location;
import other.state.container.ContainerState;

/**
 * Returns the number of pieces of a player.
 * 
 * @author Eric.Piette
 */
@Hide
public final class CountPieces extends BaseIntFunction
{
	private static final long serialVersionUID = 1L;

	//-------------------------------------------------------------------------

	/** Cell/Edge/Vertex. */
	private SiteType type;

	/** The index of the player. */
	private final IntFunction whoFn;

	/** The name of the item (Container or Component) to count. */
	private final String name;

	/** The RoleType of the player. */
	private final RoleType role;
	
	/** The region to count the pieces. */
	private final RegionFunction whereFn;

	/**
	 * 
	 * @param type The graph element type [default SiteType of the board].
	 * @param role The role of the player [All].
	 * @param of   The index of the player.
	 * @param name The name of the piece to count only these pieces.
	 * @param in   The region where to count the pieces.
	 */
	public CountPieces
	(
		@Opt           final SiteType        type,
		@Opt @Or       final RoleType        role,
		@Opt @Or @Name final IntFunction     of,
		@Opt           final String          name,
		@Opt     @Name final RegionFunction  in
	)
	{
		this.type = type;
		this.role = (role != null) ? role : (of == null ? RoleType.All : null);
		this.whoFn = (of != null) ? of : RoleType.toIntFunction(this.role);
		this.name = name;
		this.whereFn = in;
	}

	//-------------------------------------------------------------------------

	@Override
	public int eval(final Context context)
	{
		if (name != null && name.equals("Bag"))
			return context.state().remainingDominoes().size();
		
		int count = 0;
		final int whoId = whoFn.eval(context);
		
		// Get the player condition.
		final TIntArrayList idPlayers = PlayersIndices.getIdPlayers(context, role, whoId);
		
		// Get the region condition.
		final TIntArrayList whereSites = (whereFn != null) ? new TIntArrayList(whereFn.eval(context).sites()) : null;
		
		// Get the component condition.
		TIntArrayList componentIds = null;
		
		if(name != null)
		{
			componentIds = new TIntArrayList();
			for(int compId = 1 ; compId < context.components().length; compId++)
			{
				final Component component = context.components()[compId];
				if (component.name().contains(name))
					componentIds.add(compId);
			}
		}
		
		for (int index = 0; index < idPlayers.size(); index++)
		{
			final int pid = idPlayers.get(index);
			final TIntArrayList sitesOccupied = new TIntArrayList();
			
			final List<? extends Location>[] positions = context.state().owned().positions(pid);
			for (final List<? extends Location> locs : positions)
				for (final Location loc : locs)
					if(type == null || type != null && type.equals(loc.siteType()))
						if(!sitesOccupied.contains(loc.site()))
							sitesOccupied.add(loc.site());
			
			for(int i = 0; i < sitesOccupied.size(); i++)
			{
				final int site = sitesOccupied.get(i);

				// Check region condition
				if(whereSites != null && !whereSites.contains(site))
					continue;

				SiteType realType = type;
				int cid = 0;
				if(type == null)
				{
					cid = site >= context.containerId().length ? 0 : context.containerId()[site];
					if (cid > 0)
						realType = SiteType.Cell;
					else
						realType = context.board().defaultSite();
				}
					
				final ContainerState cs = context.containerState(cid);
				if(context.game().isStacking())
				{
					for(int level = 0 ; level < cs.sizeStack(site, realType); level++)
					{
						final int who = cs.who(site, level, realType);
							
						if(!idPlayers.contains(who))
							continue;
							
						// Check component condition
						if(componentIds != null)
						{
							final int what = cs.what(site, level, realType);
							if(!componentIds.contains(what))
								continue;
						}
						count++;
					}
				}
				else
				{
					final int who = cs.who(site, realType);
						
					if(!idPlayers.contains(who))
						continue;

					// Check component condition
					if(componentIds != null)
					{
						final int what = cs.what(site, realType);
						if(!componentIds.contains(what))
							continue;
					}
					count +=  cs.count(site, realType);
				}
			}
		}

		return count;
	}

	//-------------------------------------------------------------------------

	@Override
	public boolean isStatic()
	{
		return false;
	}

	@Override
	public String toString()
	{
		return "Pieces()";
	}

	@Override
	public long gameFlags(final Game game)
	{
		if (name != null && name.equals("Bag"))
			return GameType.Dominoes | GameType.LargePiece;

		long gameFlags = whoFn.gameFlags(game);

		if (whereFn != null)
			gameFlags |= whereFn.gameFlags(game);

		gameFlags |= SiteType.gameFlags(type);

		return gameFlags;
	}

	@Override
	public BitSet concepts(final Game game)
	{
		final BitSet concepts = new BitSet();
		concepts.or(SiteType.concepts(type));
		concepts.or(whoFn.concepts(game));

		if (whereFn != null)
			concepts.or(whereFn.concepts(game));

		return concepts;
	}

	@Override
	public BitSet writesEvalContextRecursive()
	{
		final BitSet writeEvalContext = new BitSet();
		writeEvalContext.or(whoFn.writesEvalContextRecursive());

		if (whereFn != null)
			writeEvalContext.or(whereFn.writesEvalContextRecursive());
		return writeEvalContext;
	}

	@Override
	public BitSet readsEvalContextRecursive()
	{
		final BitSet readEvalContext = new BitSet();
		readEvalContext.or(whoFn.readsEvalContextRecursive());

		if (whereFn != null)
			readEvalContext.or(whereFn.readsEvalContextRecursive());
		return readEvalContext;
	}

	@Override
	public boolean missingRequirement(final Game game)
	{
		boolean missingRequirement = false;

		missingRequirement |= whoFn.missingRequirement(game);

		if (whereFn != null)
			missingRequirement |= whereFn.missingRequirement(game);
		return missingRequirement;
	}

	@Override
	public boolean willCrash(final Game game)
	{
		boolean willCrash = false;
		willCrash |= whoFn.willCrash(game);

		if (whereFn != null)
			willCrash |= whereFn.willCrash(game);
		return willCrash;
	}

	@Override
	public void preprocess(final Game game)
	{
		whoFn.preprocess(game);
		if (whereFn != null)
			whereFn.preprocess(game);
	}

	// -------------------------------------------------------------------------

	/**
	 * @return The roletype of the owner of the pieces to count.
	 */
	public RoleType roleType()
	{
		return role;
	}
}
