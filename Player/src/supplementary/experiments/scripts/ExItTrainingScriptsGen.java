package supplementary.experiments.scripts;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import game.Game;
import main.CommandLineArgParse;
import main.CommandLineArgParse.ArgOption;
import main.CommandLineArgParse.OptionTypes;
import main.FileHandling;
import main.StringRoutines;
import main.UnixPrintWriter;
import main.options.Ruleset;
import other.GameLoader;
import search.minimax.AlphaBetaSearch;

/**
 * Script to generate scripts for ExIt training runs on cluster for all rulesets of all games.
 *
 * @author Dennis Soemers
 */
public class ExItTrainingScriptsGen
{
	/** Memory to assign per CPU, in MB */
	private static final String MEM_PER_CPU = "5120";
	
	/** Memory to assign to JVM, in MB */
	private static final String JVM_MEM = "4096";
	
	/** Max wall time (in minutes) */
	private static final int MAX_WALL_TIME = 3000;
	
	/** Don't submit more than this number of jobs at a single time */
	private static final int MAX_JOBS_PER_BATCH = 800;
	
	//-------------------------------------------------------------------------
	
	/**
	 * Constructor (don't need this)
	 */
	private ExItTrainingScriptsGen()
	{
		// Do nothing
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * Generates our scripts
	 * @param argParse
	 */
	private static void generateScripts(final CommandLineArgParse argParse)
	{
		final List<String> jobScriptNames = new ArrayList<String>();
		
		String scriptsDir = argParse.getValueString("--scripts-dir");
		scriptsDir = scriptsDir.replaceAll(Pattern.quote("\\"), "/");
		if (!scriptsDir.endsWith("/"))
			scriptsDir += "/";
		
		final String userName = argParse.getValueString("--user-name");
		
		//final BestBaseAgents bestBaseAgents = BestBaseAgents.loadData();
		
		final String[] allGameNames = Arrays.stream(FileHandling.listGames()).filter(s -> (
				!(s.replaceAll(Pattern.quote("\\"), "/").contains("/lud/bad/")) &&
				!(s.replaceAll(Pattern.quote("\\"), "/").contains("/lud/wip/")) &&
				!(s.replaceAll(Pattern.quote("\\"), "/").contains("/lud/WishlistDLP/")) &&
				!(s.replaceAll(Pattern.quote("\\"), "/").contains("/lud/test/")) &&
				!(s.replaceAll(Pattern.quote("\\"), "/").contains("/lud/wishlist/")) &&
				!(s.replaceAll(Pattern.quote("\\"), "/").contains("/lud/reconstruction/")) &&
				!(s.replaceAll(Pattern.quote("\\"), "/").contains("/lud/simulation/")) &&
				!(s.replaceAll(Pattern.quote("\\"), "/").contains("/lud/proprietary/"))
			)).toArray(String[]::new);
		
		for (final String fullGamePath : allGameNames)
		{
			final String[] gamePathParts = fullGamePath.replaceAll(Pattern.quote("\\"), "/").split(Pattern.quote("/"));
			final String gameName = gamePathParts[gamePathParts.length - 1].replaceAll(Pattern.quote(".lud"), "");
			final Game gameNoRuleset = GameLoader.loadGameFromName(gameName + ".lud");
			final List<Ruleset> gameRulesets = new ArrayList<Ruleset>(gameNoRuleset.description().rulesets());
			gameRulesets.add(null);
			boolean foundRealRuleset = false;
			
			for (final Ruleset ruleset : gameRulesets)
			{
				final Game game;
				String fullRulesetName = "";
				if (ruleset == null && foundRealRuleset)
				{
					// Skip this, don't allow game without ruleset if we do have real implemented ones
					continue;
				}
				else if (ruleset != null && !ruleset.optionSettings().isEmpty())
				{
					fullRulesetName = ruleset.heading();
					foundRealRuleset = true;
					game = GameLoader.loadGameFromName(gameName + ".lud", fullRulesetName);
				}
				else if (ruleset != null && ruleset.optionSettings().isEmpty())
				{
					// Skip empty ruleset
					continue;
				}
				else
				{
					game = gameNoRuleset;
				}
				
				if (game.isDeductionPuzzle())
					continue;
				
				if (game.isSimulationMoveGame())
					continue;
				
				if (!game.isAlternatingMoveGame())
					continue;
				
				if (game.hasSubgames())
					continue;
				
				if (game.isStacking())
					continue;
				
				if (game.hiddenInformation())
					continue;
				
				final String filepathsGameName = StringRoutines.cleanGameName(gameName);
				final String filepathsRulesetName = StringRoutines.cleanRulesetName(fullRulesetName.replaceAll(Pattern.quote("Ruleset/"), ""));
				
				final String jobScriptFilename = "ExIt" + filepathsGameName + filepathsRulesetName + ".sh";
					
				try (final PrintWriter writer = new UnixPrintWriter(new File(scriptsDir + jobScriptFilename), "UTF-8"))
				{
					writer.println("#!/usr/local_rwth/bin/zsh");
					writer.println("#SBATCH -J ExIt_" + filepathsGameName + filepathsRulesetName);
					writer.println("#SBATCH -o /work/" + userName + "/ExIt/Out"
							+ filepathsGameName + filepathsRulesetName + "_%J.out");
					writer.println("#SBATCH -e /work/" + userName + "/ExIt/Err"
							+ filepathsGameName + filepathsRulesetName + "_%J.err");
					writer.println("#SBATCH -t " + MAX_WALL_TIME);
					writer.println("#SBATCH --mem-per-cpu=" + MEM_PER_CPU);
					writer.println("#SBATCH -A " + argParse.getValueString("--project"));
					writer.println("unset JAVA_TOOL_OPTIONS");
					
					String javaCall = StringRoutines.join
							(
								" ", 
								"java",
								"-Xms" + JVM_MEM + "M",
								"-Xmx" + JVM_MEM + "M",
								"-XX:+HeapDumpOnOutOfMemoryError",
								"-da",
								"-dsa",
								"-XX:+UseStringDeduplication",
								"-jar",
								StringRoutines.quote("/home/" + userName + "/ExIt/Ludii.jar"),
								"--expert-iteration",
								"--game",
								StringRoutines.quote(gameName + ".lud"),
								"--ruleset",
				                StringRoutines.quote(fullRulesetName),
								"--best-agents-data-dir",
								StringRoutines.quote("/home/" + userName + "/BestAgentsData/" + filepathsGameName + filepathsRulesetName),
								"-n 100",
								"--game-length-cap 1000",
								"--thinking-time 1",
								"--is-episode-durations",
								"--prioritized-experience-replay",
								"--wis",
								"--handle-aliasing",
								"--playout-features-epsilon 0.5",
								"--checkpoint-freq 10",
								"--out-dir",
								StringRoutines.quote
								(
									"/work/" + 
									userName + 
									"/ExIt/" + 
									filepathsGameName + filepathsRulesetName + "/"
								),
								"--no-logging",
								"--max-wall-time",
								"" + MAX_WALL_TIME
							);
					
					if (!new AlphaBetaSearch().supportsGame(game))
						javaCall += " --no-value-learning";

					writer.println(javaCall);
					
					jobScriptNames.add(jobScriptFilename);
				}
				catch (final FileNotFoundException | UnsupportedEncodingException e)
				{
					e.printStackTrace();
				}
			}
		}
		
		final List<List<String>> jobScriptsLists = new ArrayList<List<String>>();
		List<String> remainingJobScriptNames = jobScriptNames;

		while (remainingJobScriptNames.size() > 0)
		{
			if (remainingJobScriptNames.size() > MAX_JOBS_PER_BATCH)
			{
				final List<String> subList = new ArrayList<String>();

				for (int i = 0; i < MAX_JOBS_PER_BATCH; ++i)
				{
					subList.add(remainingJobScriptNames.get(i));
				}

				jobScriptsLists.add(subList);
				remainingJobScriptNames = remainingJobScriptNames.subList(MAX_JOBS_PER_BATCH, remainingJobScriptNames.size());
			}
			else
			{
				jobScriptsLists.add(remainingJobScriptNames);
				remainingJobScriptNames = new ArrayList<String>();
			}
		}

		for (int i = 0; i < jobScriptsLists.size(); ++i)
		{
			try (final PrintWriter writer = new UnixPrintWriter(new File(scriptsDir + "SubmitJobs_Part" + i + ".sh"), "UTF-8"))
			{
				for (final String jobScriptName : jobScriptsLists.get(i))
				{
					writer.println("sbatch " + jobScriptName);
				}
			}
			catch (final FileNotFoundException | UnsupportedEncodingException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * Main method to generate all our scripts
	 * @param args
	 */
	public static void main(final String[] args)
	{
		// define options for arg parser
		final CommandLineArgParse argParse = 
				new CommandLineArgParse
				(
					true,
					"Generates self-play training scripts for all games, all game options."
				);
		
		argParse.addOption(new ArgOption()
				.withNames("--project")
				.help("Project for which to submit the job on cluster.")
				.withNumVals(1)
				.withType(OptionTypes.String)
				.setRequired());
		
		argParse.addOption(new ArgOption()
				.withNames("--user-name")
				.help("Username on the cluster.")
				.withNumVals(1)
				.withType(OptionTypes.String)
				.setRequired());
		
		argParse.addOption(new ArgOption()
				.withNames("--scripts-dir")
				.help("Directory in which to store generated scripts.")
				.withNumVals(1)
				.withType(OptionTypes.String)
				.setRequired());
		
		// parse the args
		if (!argParse.parseArguments(args))
			return;
		
		generateScripts(argParse);
	}
	
	//-------------------------------------------------------------------------

}
