package org.mustangproject.toecount;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBException;
import javax.xml.transform.TransformerException;

import org.mustangproject.ZUGFeRD.ZUGFeRDExporter;
import org.mustangproject.ZUGFeRD.ZUGFeRDExporterFromA1Factory;
import org.mustangproject.ZUGFeRD.ZUGFeRDExporterFromA3Factory;
import org.mustangproject.ZUGFeRD.ZUGFeRDImporter;
import org.mustangproject.ZUGFeRD.ZUGFeRDMigrator;

import com.sanityinc.jargs.CmdLineParser;
import com.sanityinc.jargs.CmdLineParser.Option;

public class Toecount {
	// build with: /opt/local/bin/mvn clean compile assembly:single
	private static void printUsage() {
		System.err.println(getUsage());
	}

	private static String getUsage() {
		return "Usage: Toecount [-d,--directory] [-l,--listfromstdin] [-i,--ignorefileextension] | [-c,--combine] | [-e,--extract] | [-u,--upgrade] | [-a,--a3only] | [-h,--help]\r\n";
	}

	private static void printHelp() {
		System.out.println("Mustangproject.org's Toecount 0.2.0 \r\n"
				+ "A Apache Public License command line tool for statistics on PDF invoices with\r\n"
				+ "ZUGFeRD Metadata (http://www.zugferd.org)\r\n" + "\r\n" + getUsage() + "Count operations"
				+ "\t--directory= count ZUGFeRD files in directory to be scanned\r\n"
				+ "\t\tIf it is a directory, it will recurse.\r\n"
				+ "\t--listfromstdin=count ZUGFeRD files from a list of linefeed separated files on runtime.\r\n"
				+ "\t\tIt will start once a blank line has been entered.\r\n"
				+ "\t--ignorefileextension=if PDF files are counted check *.* instead of *.pdf files"
				+ "Merge operations"
				+ "\t--combine= combine XML and PDF file to ZUGFeRD PDF file\r\n"
				+ "\t--extract= extract ZUGFeRD PDF to XML file\r\n"
				+ "\t--upgrade= upgrade ZUGFeRD XML to ZUGFeRD 2 XML\r\n"
				+ "\t--a3only= upgrade from PDF/A1 to A3 only \r\n"

		);
	}

	/***
	 * Prompts the user for a input or output filename
	 * @param prompt
	 * @param defaultFilename
	 * @param ensureFileExists
	 * @return String
	 */
	protected static String getFilenameFromUser(String prompt, String defaultFilename, boolean ensureFileExists) {
		boolean fileExists = false;
		String selectedName = "";
		do {
			// for a more sophisticated dialogue maybe https://github.com/mabe02/lanterna/ could be taken into account
			System.out.print(prompt + " (default: " + defaultFilename + "):");
			BufferedReader buffer=new BufferedReader(new InputStreamReader(System.in));
			try {
				selectedName=buffer.readLine();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			if (selectedName.isEmpty()) {
				// pressed return without entering anything
				selectedName = defaultFilename;
			}

			if (ensureFileExists) {
				File f = new File(selectedName);
				if (f.exists()) {
					fileExists = true;
				} else {
					System.out.println("File does not exist, try again or CTRL+C to cancel");
					// discard the input, a scanner.reset is not sufficient
				    
				}
				
			} else {
				fileExists=true;
			}
		} while (!fileExists);

		return selectedName;
	}

	// /opt/local/bin/mvn clean compile assembly:single
	public static void main(String[] args) {
		CmdLineParser parser = new CmdLineParser();
		Option<String> dirnameOption = parser.addStringOption('d', "directory");
		Option<Boolean> filesFromStdInOption = parser.addBooleanOption('l', "listfromstdin");
		Option<Boolean> ignoreFileExtOption = parser.addBooleanOption('i', "ignorefileextension");
		Option<Boolean> combineOption = parser.addBooleanOption('c', "combine");
		Option<Boolean> extractOption = parser.addBooleanOption('e', "extract");
		Option<Boolean> helpOption = parser.addBooleanOption('h', "help");
		Option<Boolean> upgradeOption = parser.addBooleanOption('u', "upgrade");
		Option<Boolean> a3onlyOption = parser.addBooleanOption('a', "a3only");

		try {
			parser.parse(args);
		} catch (CmdLineParser.OptionException e) {
			System.err.println(e.getMessage());
			printUsage();
			System.exit(2);
		}

		String directoryName = parser.getOptionValue(dirnameOption);

		Boolean filesFromStdIn = parser.getOptionValue(filesFromStdInOption, Boolean.FALSE);

		Boolean combineRequested = parser.getOptionValue(combineOption, Boolean.FALSE);

		Boolean extractRequested = parser.getOptionValue(extractOption, Boolean.FALSE);

		Boolean helpRequested = parser.getOptionValue(helpOption, Boolean.FALSE);

		Boolean upgradeRequested = parser.getOptionValue(upgradeOption, Boolean.FALSE);

		Boolean ignoreFileExt = parser.getOptionValue(ignoreFileExtOption, Boolean.FALSE);

		Boolean a3only = parser.getOptionValue(a3onlyOption, Boolean.FALSE);

		if (helpRequested) {
			printHelp();
		}

		else if (((directoryName != null) && (directoryName.length() > 0)) || filesFromStdIn.booleanValue()) {

			StatRun sr = new StatRun();
			if (ignoreFileExt) {
				sr.ignoreFileExtension();
			}
			if (directoryName != null) {
				Path startingDir = Paths.get(directoryName);

				if (Files.isRegularFile(startingDir)) {
					String filename = startingDir.toString();
					FileChecker fc = new FileChecker(filename, sr);

					fc.checkForZUGFeRD();
					System.out.print(fc.getOutputLine());

				} else if (Files.isDirectory(startingDir)) {
					FileTraverser pf = new FileTraverser(sr);
					try {
						Files.walkFileTree(startingDir, pf);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}

			}

			if (filesFromStdIn) {
				BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
				String s;
				try {
					while ((s = in.readLine()) != null && s.length() != 0) {
						FileChecker fc = new FileChecker(s, sr);

						fc.checkForZUGFeRD();
						System.out.print(fc.getOutputLine());

					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
			System.out.println(sr.getSummaryLine());
		} else if (combineRequested) {
			/*
			 * ZUGFeRDExporter ze= new ZUGFeRDExporterFromA1Factory()
			 * .setProducer("toecount") .setCreator(System.getProperty("user.name"))
			 * .loadFromPDFA1("invoice.pdf");
			 */
			String pdfName="";
			String xmlName="";
			String outName="";

			try {
				
				pdfName=getFilenameFromUser("Source PDF", "invoice.pdf", true);
				xmlName=getFilenameFromUser("ZUGFeRD XML", "ZUGFeRD-invoice.xml", true);
				outName=getFilenameFromUser("Ouput PDF", "invoice.ZUGFeRD.pdf", false);

				ZUGFeRDExporter ze = new ZUGFeRDExporterFromA3Factory().setProducer("Toecount")
						.setCreator(System.getProperty("user.name")).load(pdfName);
				ze.setZUGFeRDXMLData(Files.readAllBytes(Paths.get(xmlName)));

				ze.export(outName);
			} catch (IOException e) {
				e.printStackTrace();
				// } catch (JAXBException e) {
				// e.printStackTrace();
			}
			System.out.println("Written to "+outName);
		} else if (extractRequested) {
			ZUGFeRDImporter zi = new ZUGFeRDImporter();
			String pdfName=getFilenameFromUser("Source PDF", "invoice.pdf", true);
			String xmlName=getFilenameFromUser("ZUGFeRD XML", "ZUGFeRD-invoice.xml", false);

			zi.extract(pdfName);
			try {
				Files.write(Paths.get(xmlName), zi.getRawXML());
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("Written to "+xmlName);

		} else if (a3only) {
			/*
			 * ZUGFeRDExporter ze= new ZUGFeRDExporterFromA1Factory()
			 * .setProducer("toecount") .setCreator(System.getProperty("user.name"))
			 * .loadFromPDFA1("invoice.pdf");
			 */
			String pdfName="";
			String outName="";
			try {
				pdfName=getFilenameFromUser("Source PDF", "invoice.pdf", true);
				outName=getFilenameFromUser("Target PDF", "invoice.a3.pdf", false);
				
				ZUGFeRDExporter ze = new ZUGFeRDExporterFromA1Factory().setAttachZUGFeRDHeaders(false)
						.load(pdfName);

				ze.export(outName);
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("Written to "+outName);
		} else if (upgradeRequested) {
			String xmlName="";
			String outName="";

			try {
				xmlName=getFilenameFromUser("ZUGFeRD 1.0 XML source", "ZUGFeRD-invoice.xml", true);
				outName=getFilenameFromUser("ZUGFeRD 2.0 XML target", "ZUGFeRD-2-invoice.xml", false);

				String xml = new String(Files.readAllBytes(Paths.get(xmlName)), StandardCharsets.UTF_8);
				ZUGFeRDMigrator zmi=new ZUGFeRDMigrator();
				xml=zmi.migrateFromV1ToV2(xml);
				Files.write(Paths.get(outName), xml.getBytes());
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("Written to "+outName);

		} else {
			// no argument or argument unknown
			printUsage();
			System.exit(2);

		}
	}
}