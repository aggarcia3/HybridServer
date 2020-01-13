package es.uvigo.esei.dai.hybridserver.xml;

import es.uvigo.esei.dai.hybridserver.http.MIME;

/**
 * Encapsulates the result of trying to apply a XSLT stylesheet to a XML
 * document, after validating it with a XSD.
 *
 * @author Alejandro González García
 */
public final class XSLTTransformResult {
	private final XMLProcessingResult processingResult;
	private final MIME mime;
	private final String content;

	/**
	 * Creates a new XSLT transformation result.
	 *
	 * @param processingResult The result of performing processing steps on the
	 *                         related documents.
	 * @param mime             The MIME type of the resulting transformation, if it
	 *                         was successful.
	 * @param content          The content of the resulting transformation, if it
	 *                         was successful.
	 * @throws IllegalArgumentException If {@code processingResult} is {@code null},
	 *                                  or {@code mime} or {@code content} are
	 *                                  {@code null} or not when it does not make
	 *                                  sense according to the successfulness of the
	 *                                  result.
	 */
	XSLTTransformResult(final XMLProcessingResult processingResult, final MIME mime, final String content) {
		if (processingResult == null) {
			throw new IllegalArgumentException(
				"Can't create a XSLT transformation result for a null XML validation result"
			);
		}

		if (
			(processingResult.wasSuccessful() && (mime == null || content == null)) ||
			(!processingResult.wasSuccessful() && (mime != null || content != null))
		) {
			throw new IllegalArgumentException(
				"A XSLT transformation result can have null MIME and content if and only if it was not successful"
			);
		}

		this.processingResult = processingResult;
		this.mime = mime;
		this.content = content;
	}

	/**
	 * Retrieves the general result of performing processing steps on the documents
	 * related to the XSLT transformation.
	 *
	 * @return The described result.
	 */
	public final XMLProcessingResult getProcessingResult() {
		return processingResult;
	}

	/**
	 * Returns the MIME type of the resulting XSLT transformation content, if it was
	 * processed successfully.
	 *
	 * @return The described MIME type. If the XSLT transformation was not carried
	 *         out successfully, this method returns {@code null}.
	 */
	public final MIME getMime() {
		return mime;
	}

	/**
	 * Returns the resulting XSLT transformation content, if it was processed
	 * successfully.
	 *
	 * @return The described XSLT transformation content. If the XSLT transformation
	 *         was not carried out successfully, this method returns {@code null}.
	 */
	public final String getContent() {
		return content;
	}
}
