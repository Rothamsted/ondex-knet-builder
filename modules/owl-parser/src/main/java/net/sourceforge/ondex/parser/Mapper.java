package net.sourceforge.ondex.parser;

/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>12 Apr 2017</dd></dl>
 *
 */
public interface Mapper<O, S>
{
	public O map ( S source );
}