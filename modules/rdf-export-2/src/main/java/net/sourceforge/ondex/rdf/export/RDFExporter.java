package net.sourceforge.ondex.rdf.export;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Model;

import info.marcobrandizi.rdfutils.jena.elt.RDFProcessor;
import net.sourceforge.ondex.core.ONDEXConcept;
import net.sourceforge.ondex.core.ONDEXGraph;
import net.sourceforge.ondex.core.ONDEXGraphMetaData;
import net.sourceforge.ondex.core.ONDEXRelation;
import net.sourceforge.ondex.rdf.export.mappers.RDFXFactory;

/**
 * The RDFExporter machinery. 
 * 
 * This triggers the exporting of an {@link ONDEXGraph}, passing it to the {@link RDFXFactory java2rdf-based RDF mappers}
 * and calling an handler parameter for the RDF chunks generated during the process.
 * 
 * The handler is supposed to do some concrete job, such as saving to a file or sending the RDF to a
 * triple store. 
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>21 Nov 2017</dd></dl>
 *
 */
public class RDFExporter extends RDFProcessor<ONDEXGraph>
{
	@Override
	public final void process ( ONDEXGraph graph, Object...opts ) {
		this.export ( graph );
	}
	
	public void export ( ONDEXGraph graph )
	{
		RDFXFactory xfact = new RDFXFactory ( this.getDestinationSupplier ().get () );
		
		// TODO: Graph
		final RDFXFactory xfactf = xfact; 
		ONDEXGraphMetaData metaData = graph.getMetaData ();
		Stream.of ( 
			metaData.getConceptClasses (), 
			metaData.getRelationTypes (), 
			metaData.getAttributeNames (), 
			metaData.getEvidenceTypes (),
			metaData.getUnits ()
		)
		.flatMap ( Collection::stream )
		.forEach ( meta -> xfactf.map ( meta ) );
								
		
		// We export all metadata in one chunk. This is typically small at this point and flushing it out 
		// allows a client to handle the whole T-Box first.
		xfact = this.handleNewXTask ( xfact, true );

		Set<ONDEXConcept> concepts = graph.getConcepts ();
		int ntotal = concepts.size ();
		int ctr = 0;

		for ( ONDEXConcept concept: concepts )
		{
			xfact.map ( concept );
			xfact = this.handleNewXTask ( xfact );
			if ( ++ctr % 100000 == 0 ) log.info ( "{}/{} concepts submitted for export", ctr, ntotal );
		}

		Set<ONDEXRelation> relations = graph.getRelations ();
		ntotal = relations.size ();
		ctr = 0;
				
		for ( ONDEXRelation relation: relations )
		{
			xfact.map ( relation );
			xfact = this.handleNewXTask ( xfact );
			if ( ++ctr % 100000 == 0 ) log.info ( "{}/{} relations submitted for export", ctr, ntotal );
		}
		
		this.handleNewXTask ( xfact, true );
		
		ExecutorService executor = this.getExecutor ();
		
		executor.shutdown ();

		this.waitExecutor ( "Waiting for RDF export completion, please wait" );
		log.info ( "RDF export finished, a total of {} concepts+relations exported", concepts.size () + relations.size () );
	}
	
	private RDFXFactory handleNewXTask ( RDFXFactory xfact ) {
		return handleNewXTask ( xfact, false );
	}

	/**
	 * Wraps the model generated by {@link #getDestinationSupplier()} into a a new {@link RDFXFactory}, which 
	 * is the destination to which {@link #export(ONDEXGraph)} sends mappings instructions.
	 *  
	 */
	private RDFXFactory handleNewXTask ( RDFXFactory xfact, boolean forceFlush )
	{
		Model currentModel = xfact.getJenaModel ();
		Model newModel = super.handleNewTask ( currentModel , forceFlush );

		if ( currentModel == newModel ) return xfact;
		
		long newCount = currentModel.size () + xfact.getTriplesCountTracker ();
		log.debug ( "{} RDF triples submitted for export", newCount );
		return new RDFXFactory ( newModel, newCount );
	}
		
}
