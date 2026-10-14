package hudson.plugins.robot;

import jenkins.util.SystemProperties;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.util.StreamReaderDelegate;

import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;

/**
 * An XMLStreamReader for an output file that cannot be read to its end. After
 * the wrapped reader throws, the events come from a generated document that
 * closes every open element, so the parser finishes the same way the parser
 * finishes a complete file.
 */
class RecoveringXMLStreamReader extends StreamReaderDelegate {

	/**
	 * The Robot output XML schema requires suite, test and kw elements to have
	 * a nested status element.
	 */
	private static final Set<String> HAS_STATUS = Set.of("suite", "test", "kw");

	/** The system property that turns recovery on. */
	static final String RECOVER_PARTIAL_OUTPUT = "hudson.plugins.robot.recoverPartialOutput";

	private final Deque<String> elementStack = new ArrayDeque<>();
	private int stoppedAfter;
	private String parseError;

	private boolean recoveryTried;

	static String parseError(XMLStreamReader reader) {
		return reader instanceof RecoveringXMLStreamReader recovering ? recovering.parseError : null;
	}

	static XMLStreamReader recovering(XMLStreamReader reader) {
		return SystemProperties.getBoolean(RECOVER_PARTIAL_OUTPUT, false)
				? new RecoveringXMLStreamReader(reader) : reader;
	}

	private RecoveringXMLStreamReader(XMLStreamReader reader) {
		super(reader);
	}

	@Override
	public int next() throws XMLStreamException {
		int event;
		try {
			event = super.next();
		} catch (XMLStreamException stopped) {
			setParent(endingReader(stopped));
			event = super.next();
		}
		track(event);
		return event;
	}

	private void track(int event) {
		stoppedAfter = event;
		if (event == START_ELEMENT)
			elementStack.push(super.getLocalName());
		else if (event == END_ELEMENT)
			elementStack.pop();
	}

	/**
	 * When no element is open there is nothing to close, so the exception is
	 * rethrown. When recovery was already tried, the exception comes from reading
	 * the donor document, and recovering again would loop, so the exception is
	 * rethrown.
	 */
	private XMLStreamReader endingReader(XMLStreamException stopped) throws XMLStreamException {
		if (elementStack.isEmpty() || recoveryTried)
			throw stopped;
		recoveryTried = true;
		parseError = "could not be read to the end: " + stopped.getMessage();
		XMLStreamReader donor = XMLInputFactory.newInstance()
				.createXMLStreamReader(new StringReader(endingDonor()));
		for (String element : elementStack)
			donor.nextTag();
		return donor;
	}

	/**
	 * An XML reader reports an end element only after the matching start
	 * element, so the document opens every open element again before closing
	 * the element. The closing tags are the end elements the parser still needs
	 * to finish parsing the file.
	 */
	private String endingDonor() {
		return openingTags() + lineEndAfterClose() + closingTags();
	}

	/** Robot writes a line end after every closing tag. */
	private String lineEndAfterClose() {
		return stoppedAfter == END_ELEMENT ? "\n" : "";
	}

	private String openingTags() {
		StringBuilder xml = new StringBuilder();
		for (String element : (Iterable<String>) elementStack::descendingIterator)
			xml.append("<" + element + ">");
		return xml.toString();
	}

	private String closingTags() {
		StringBuilder xml = new StringBuilder();
		for (String element : elementStack) {
			if (HAS_STATUS.contains(element))
				xml.append(statusElement());
			//Robot writes a line end after every closing tag
			xml.append("</" + element + ">\n");
		}
		return xml.toString();
	}

	private String statusElement() {
		//Robot Framework 7 names the attribute elapsed and older versions name the attribute elapsedtime.
		//The donor does not know the schema version.
		return "<status status='FAIL' elapsed='0' elapsedtime='0'>" + escaped(parseError) + "</status>";
	}

	/** The parser message can name an element in angle brackets. */
	private static String escaped(String text) {
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
