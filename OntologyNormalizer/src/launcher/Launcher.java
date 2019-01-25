package launcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Set;

import org.semanticweb.owlapi.formats.OWLXMLDocumentFormat;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.model.MissingImportHandlingStrategy;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import normalizers.MainNormalizer;

public class Launcher {

	public static void main(final String[] args) throws FileNotFoundException, UnsupportedEncodingException,
			OWLOntologyCreationException, OWLOntologyStorageException {

		if (Arrays.asList(args).contains("-help") || Arrays.asList(args).contains("help") || args.length != 2) {

			// The following program takes two arguments:
			// 1. The path of the input ontology file (this must be a valid OWL file that
			// can be parsed by the OWLAPI).
			// 2. The path where the normalized ontology will be stored.
			//
			// To run the project from the command line using a .jar file, you would have to
			// type something like the following:
			// java -jar OntologyNormalizer.jar Input/exampleOntology.owl
			// Output/normalizedOntology.owl
			//
			// Note the following comments:
			// 1. All axioms in the input ontology which are not logical axioms, or contain
			// datatype properties, datatypes or builtin atoms, or contain empty
			// UnionOf, OneOf or IntersectionOf expressions, or minCardinality 0 are
			// filtered out.
			// 2. All axioms in the resulting normalized ontology are of one of the
			// following forms: A1 sqcap ... sqcap An sqsubseteq B1 sqcup ... sqcup Bm, A
			// sqs forall R.B, A sqs exists R.Self, exists R.Self sqs B, A sqs >= n R.B, A
			// sqs <= n R.B, A sqs {a1} sqcup ... sqcup {an}, R sqs S, R1 o ... o Rn
			// sqs S, A(a), R(a, b), a1 = ... = an or a1 neq ... neq an where A(i), B are
			// concept names, R(i), S are (possibly inverse) roles, and a(i), b are
			// named individuals.
			// 3. SWRLRules in the normalized ontology only feature atoms of the form A(t)
			// or R(t, u) where A is a concept name, R is a (possibly inverse) role
			// and t, u are variables or named individuals.

			System.out.println("The following program takes two arguments:");
			System.out.println(
					"    1. The path of the input ontology file (this must be a valid OWL file that can be parsed by the OWLAPI).");
			System.out.println("    2. The path where the normalized ontology will be stored.");
			System.out.println("");
			System.out.println("Proper Usage is: java -jar ontologyNormalizer.jar arg1 arg2");
			System.out.println(" - arg1 is input ontology file path (ex: ./input/ontology1.owl.xml)");
			System.out.println(
					" - arg2 is output normalized ontology file path (ex: ./output/ontology1_normalized.owl.xml)");
			System.out.println("");
			System.out.println("Note the following comments:");
			System.out.println(
					"	 1. All axioms in the input ontology which are not logical axioms, or contain datatype properties, datatypes or builtin atoms, or contain empty UnionOf, OneOf or IntersectionOf expressions, or minCardinality 0  are filtered out.");
			System.out.println(
					"	 2. All axioms in the resulting normalized ontology are of one of the following forms: \r\n"
							+ "   A1 sqcap ... sqcap An sqsubseteq B1 sqcup ... sqcup Bm, A sqs forall R.B, A sqs exists R.Self, exists R.Self sqs B, A sqs >= n R.B, A sqs <= n R.B, A sqs {a1} sqcup ... sqcup {an}, \r\n"
							+ "   R sqs S, R1 o ... o Rn sqs S, A(a), R(a, b), a1 = ... = an or a1 neq ... neq an  \r\n"
							+ "   where A(i), B are concept names, R(i), S are (possibly inverse) roles, and a(i), b are named individuals.");
			System.out.println(
					"	 3. SWRLRules in the normalized ontology only feature atoms of the form A(t) or R(t, u) where A is a concept name, R is a (possibly inverse) role and t, u are variables or named individuals.");

			System.exit(0);
		}

		final String inputOntologyPath = args[0];
		final String outputOntologyPath = args[1];

		System.out.println("Normalizing Ontology (" + LocalTime.now() + ")");
		System.out.println(" * Input path:" + " " + inputOntologyPath);

		final OWLOntology ontology = loadOntology(inputOntologyPath);
		if (ontology != null) {
			System.out.println("  - Input ontology size:" + " " + ontology.getAxiomCount());
			final MainNormalizer mainNormalizer = new MainNormalizer();
			final Set<OWLLogicalAxiom> normalizedAxs = mainNormalizer.filterAndNormalizeAxioms(ontology);
			System.out.println(" * Output path:" + " " + outputOntologyPath);
			saveAxsAsOWLOntology(normalizedAxs, outputOntologyPath);
			System.out.println("  - Normalized axioms count:" + " " + normalizedAxs.size());
		}
	}

	public static OWLOntology loadOntology(final String ontologyFilePath) {
		OWLOntology ontology = null;
		try {
			final OWLOntologyLoaderConfiguration config = new OWLOntologyLoaderConfiguration()
					.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.THROW_EXCEPTION);
			final OWLOntologyDocumentSource documentSource = new FileDocumentSource(new File(ontologyFilePath));
			ontology = Srd.owlOntologyManager.loadOntologyFromOntologyDocument(documentSource, config);
		} catch (final OWLOntologyCreationException e) {
			System.out.println(e);
			System.out.println("WARNING!!! Error loading ontology.");
			System.out.println(" -> " + ontologyFilePath + "\n");
		}
		return ontology;
	}

	public static void saveOWLOntologyToFile(final OWLOntology owlAPIOntology, final String filePath)
			throws OWLOntologyStorageException, FileNotFoundException {
		saveToOWLXMLDocument(filePath, owlAPIOntology);
	}

	public static void saveAxsAsOWLOntology(final Set<OWLLogicalAxiom> normalizedAxs, final String filePath)
			throws OWLOntologyCreationException, OWLOntologyStorageException, FileNotFoundException {

		final OWLOntology owlAPIOntology = Srd.owlOntologyManager.createOntology();
		for (final OWLAxiom axiom : normalizedAxs) {
			Srd.owlOntologyManager.addAxiom(owlAPIOntology, axiom);
		}
		saveToOWLXMLDocument(filePath, owlAPIOntology);

	}

	private static void saveToOWLXMLDocument(final String filePath, final OWLOntology owlOntology)
			throws OWLOntologyStorageException, FileNotFoundException {
		Srd.owlOntologyManager.saveOntology(owlOntology, new OWLXMLDocumentFormat(), new FileOutputStream(filePath));
	}
}
