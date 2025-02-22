
package main.grammar;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import annotations.Alias;
import main.StringRoutines;

//-----------------------------------------------------------------------------

/**
 * Instance of an item actually compiled.
 * @author cambolbro and matthew.stephenson
 */
public class Call
{
	public enum CallType
	{
		Null,
		Class,
		Array,
		Terminal
	}
	
	private CallType type = null; 
	
	//---------------------------------------------------------

	/** 
	 * Record of instance that created this call.
	 * Only keep necessary info from Instance to minimise memory usage.
	 */
	//private final Instance instance;
	private final Symbol symbol;
	private final Object object;
	private final String constant;
	
	/** Expected type for this call. */
	private final Class<?> expected;
	
	/** Arguments for the call (if class). */
	private final List<Call> args = new ArrayList<>();

	/** Indent level for formatting. */
	private final int TAB_SIZE = 4;
	
	/** Named parameter in grammar for this argument, if any. */
	private String label = null;

	//-------------------------------------------------------------------------

	/**
	 * Default constructor for Array call. 
	 */
	public Call(final CallType type)
	{
		this.type     = type;
		symbol   = null;
		object   = null;
		constant = null;
		expected = null;
	}

	/**
	 * Constructor for Terminal call. 
	 */
	public Call(final CallType type, final Instance instance, final Class<?> expected)
	{
		this.type     = type;
		symbol   = instance.symbol();
		object   = instance.object();
		constant = instance.constant();
		this.expected = expected;
	}

	//-------------------------------------------------------------------------

	public CallType type()
	{
		return type;
	}
	
	public Symbol symbol()
	{
		return symbol;
	}
	
	public Class<?> cls()
	{
		return symbol == null ? null : symbol.cls();
	}
	
	public Object object()
	{
		return object;
	}

	public String constant()
	{
		return constant;
	}

	public List<Call> args()
	{
		return Collections.unmodifiableList(args);
	}

	public Class<?> expected()
	{
		return expected;
	}

	public String label()
	{
		return label;
	}
	
	public void setLabel(final String str)
	{
		label = new String(str);
	}
	
	//-------------------------------------------------------------------------

	/**
	 * Add argument to the list.
	 */
	public void addArg(final Call arg)
	{
		args.add(arg);
	}
	
//	/**
//	 * Remove the last argument (if any).
//	 */
//	public void removeLastArg()
//	{
//		if (args.size() == 0)
//			System.out.println("** Call.removeLastArg(): No args to remove!");
//		
//		args.remove(args.size() - 1);
//	}

	//-------------------------------------------------------------------------

	/**
	 * @return Number of tokens in the tree from this token down.
	 */
	public int count()
	{
		int count = 1;
		for (final Call sub : args)
			count += sub.count();
		return count;
	}
	
	/**
	 * @return Number of tokens in the tree from this token down.
	 */
	public int countClasses()
	{
		int count = type() == CallType.Class ? 1 : 0;
		for (final Call sub : args)
			count += sub.countClasses();
		return count; 
	}
	
	/**
	 * @return Number of tokens in the tree from this token down.
	 */
	public int countTerminals()
	{
		int count = type() == CallType.Terminal ? 1 : 0;
		for (final Call sub : args)
			count += sub.countTerminals();
		return count; 
	}
	
	/**
	 * @return Number of tokens in the tree from this token down.
	 */
	public int countClassesAndTerminals()
	{
		int count = type() != CallType.Array ? 1 : 0;
		for (final Call sub : args)
			count += sub.countClassesAndTerminals();
		return count; 
	}

	//-------------------------------------------------------------------------

	/**
	 * Export this call node (and its args) to file.
	 * @param fileName
	 */
	public void export(final String fileName)
	{
        try (final FileWriter writer = new FileWriter(fileName))
        {
        	final String str = toString();
        	writer.write(str);
        }
        catch (final IOException e) 
        { 
        	e.printStackTrace(); 
        }
	}

	//-------------------------------------------------------------------------
	
	@Override
	public String toString()
	{
		return format(0, true);
	}
	
	//-------------------------------------------------------------------------
	
	@Override
	public boolean equals(final Object o)
	{
		return toString().equals(((Call) o).toString());
	}
	
	@Override
	public int hashCode()
	{
		return toString().hashCode();
	}
	
	//-------------------------------------------------------------------------

	/**
	 * @return String representation of callTree for display purposes.
	 */
	String format(final int depth, final boolean includeLabels)
	{
		final StringBuilder sb = new StringBuilder();
		
		final String indent = StringRoutines.indent(TAB_SIZE, depth);
		
		switch (type())
		{
		case Null:
			sb.append(indent + "-\n");
			break;
		case Array:
			sb.append(indent + "{\n");
			for (final Call arg : args)
				sb.append(arg.format(depth, includeLabels));

			if (includeLabels && label != null)
				sb.append(" \"" + label + ":\"");

			break;
		case Class:
			sb.append(indent + cls().getName());
			if (!cls().getName().equals(expected.getName()))
				sb.append(" (" + expected.getName() + ")");
			
			if (includeLabels && label != null)
				sb.append(" \"" + label + ":\"");
			
			sb.append("\n");
				
			for (final Call arg : args)
				sb.append(arg.format(depth + 1, includeLabels));
			break;
		case Terminal:
			if (object().getClass().getSimpleName().equals("String"))
				sb.append(indent + "\"" + object() + "\" (" + expected.getName() + ")");
			else
				sb.append(indent + object() + " (" + expected.getName() + ")");
			
			if (includeLabels && label != null)
				sb.append(" \"" + label + ":\"");

			if (constant != null)
				sb.append(" Constant=" + constant);

			sb.append("\n");
			break;
		default:
			System.out.println("** Call.format() should never hit default.");
			break;
		}
			
		if (type() == CallType.Array)
			sb.append(indent + "}\n");
		
		return sb.toString();
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @return LudemeInfo set representation of callTree for ludemplex analysis purposes.
	 */
	public Set<LudemeInfo> analysisFormat(final int depth, final List<LudemeInfo> ludemes)
	{
		final Set<LudemeInfo> ludemesFound = new HashSet<>();
		
		switch (type)
		{
		case Null:
			break;
		case Array:
			for (final Call arg : args)
				ludemesFound.addAll(arg.analysisFormat(depth, ludemes));
			break;
		case Class:
			ludemesFound.addAll(LudemeInfo.findLudemeInfo(this, ludemes));
			if (args.size() > 0)
				for (final Call arg : args)
					ludemesFound.addAll(arg.analysisFormat(depth + 1, ludemes));
			break;
		case Terminal:
			ludemesFound.addAll(LudemeInfo.findLudemeInfo(this, ludemes));
			break;
		default:
			System.out.println("** Call.format() should never hit default.");
			break;
		}
		
		return ludemesFound;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @return String representation of callTree for database storing purposes (mimics game description style).
	 */
	public List<String> ludemeFormat(final int depth)
	{
		List<String> stringList = new ArrayList<String>();
		
		switch (type)
		{
		case Null:
			break;
		case Array:
			if (label != null && depth > 0)
				stringList.add(label + ":");
			
			stringList.add("{");
			
			for (final Call arg : args)
			{
				stringList.addAll(arg.ludemeFormat(depth));
				stringList.add(" ");
			}
			break;
		case Class:
			final Annotation[] annotations = cls().getAnnotations();
			String name = cls().getName().split("\\.")[cls().getName().split("\\.").length-1];
			for (final Annotation annotation : annotations)
				if (annotation instanceof Alias)
					name = ((Alias)annotation).alias();
			name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
			
			if (label != null && depth > 0)
				stringList.add(label + ":");
			
			stringList.add("(");
			stringList.add(name);

			if (args.size() > 0)
			{
				stringList.add(" ");
				
				for (final Call arg : args)
					stringList.addAll(arg.ludemeFormat(depth + 1));

				stringList = removeCharsFromStringList(stringList, 1);
			}
			
			stringList.add(") ");
			break;
		case Terminal:
			if (label != null)
				stringList.add(label + ":");
			
			if (constant != null)
				stringList.add(constant + " ");
			else if (object().getClass().getSimpleName().equals("String"))
				stringList.add("\"" + object() + "\" ");
			else
				stringList.add(object() + " ");

			break;
		default:
			System.out.println("** Call.format() should never hit default.");
			break;
		}
			
		if (type == CallType.Array)
		{
			if (!stringList.get(stringList.size()-1).equals("{"))
					stringList = removeCharsFromStringList(stringList, 2);
			
			stringList.add("} ");
		}
		
		return stringList;
	}
	
	//-------------------------------------------------------------------------	

	/**
	 * Removes a specified number of chars from a list of Strings, starting at the end and working backwards.
	 */
	private static List<String> removeCharsFromStringList(final List<String> originalStringList, final int numChars) 
	{
		final List<String> stringList = originalStringList;
		for (int i = 0; i < numChars; i++)
		{
			final String oldString = stringList.get(stringList.size()-1);
			final String newString = oldString.substring(0, oldString.length()-1);
			stringList.remove(stringList.size()-1);
			if (newString.length() > 0)
				stringList.add(newString);
		}
		return stringList;
	}

	//-------------------------------------------------------------------------	

}
