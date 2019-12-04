package es.uvigo.esei.dai.hybridserver.xml;

/**
 * Contains the general result of performing processing steps on the documents
 * related to a XSLT transformation.
 *
 * @author Alejandro González García
 */
public final class XMLProcessingResult {
	private final boolean wasSuccessful;
	private final String failureReason;

	/**
	 * Creates a new XSLT processing result.
	 *
	 * @param wasSuccessful True if and only if the processing was successful and a
	 *                      result could be generated, false otherwise.
	 * @param failureReason A human-friendly string describing why the processing
	 *                      failed, if and only if it failed.
	 * @throws IllegalArgumentException If {@code failureReason} is {@code null} but
	 *                                  the processing steps were not successful, or
	 *                                  {@code failureReason} is not {@code null}
	 *                                  and the processing steps were successful.
	 */
	XMLProcessingResult(final boolean wasSuccessful, final String failureReason) {
		if (!wasSuccessful && failureReason == null || wasSuccessful && failureReason != null) {
			throw new IllegalArgumentException(
				"A XML validation result can contain a failure reason if and only if it was not successful"
			);
		}

		this.wasSuccessful = wasSuccessful;
		this.failureReason = failureReason;
	}

	/**
	 * Checks whether the processing steps on the documents related to the XSLT
	 * transformation were successful. If they were, then they have an associated
	 * result.
	 *
	 * @return True if the processing steps carried out were successful, false
	 *         otherwise.
	 */
	public final boolean wasSuccessful() {
		return wasSuccessful;
	}

	/**
	 * Returns the reason for the processing steps to fail, if they failed.
	 *
	 * @return The described reason string. If the processing steps did not fail,
	 *         the value returned is {@code null}.
	 */
	public final String getFailureReason() {
		return failureReason;
	}
}
