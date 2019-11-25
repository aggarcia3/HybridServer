package es.uvigo.esei.dai.hybridserver.io;

import java.io.InputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/**
 * Extends a byte oriented input stream with methods reminiscent of readers,
 * which work on character streams. The design of this class allows mixing read
 * operations of binary data with text data in a seamless manner, while
 * providing stronger guarantees about the final position in the underlying
 * input stream after each operation than {@link InputStreamReader}.
 *
 * @author Alejandro González García
 * @implNote This class is thread-safe if and only if the underlying input
 *           stream is thread safe.
 */
public class HybridInputStream extends FilterInputStream {
	private final CharsetDecoder charDecoder;

	/**
	 * Creates a new hybrid input stream from an underlying input stream and a
	 * charset, used when interpreting bytes as text.
	 *
	 * @param inputStream The input stream to read bytes from.
	 * @param charset     The character set that specifies how the bytes should be
	 *                    interpreted as text, when necessary.
	 * @throws IllegalArgumentException If any parameter is null.
	 */
	public HybridInputStream(final InputStream inputStream, final Charset charset) {
		super(inputStream);

		if (inputStream == null) {
			throw new IllegalArgumentException(
				"The input stream associated with a hybrid input stream can't be null"
			);
		}

		if (charset == null) {
			throw new IllegalArgumentException(
				"The character set associated with a hybrid input stream can't be null"
			);
		}

		this.charDecoder = charset.newDecoder()
			.onMalformedInput(CodingErrorAction.REPLACE)
			.onUnmappableCharacter(CodingErrorAction.REPLACE);
	}

	/**
	 * Creates a new hybrid input stream from an underlying input stream and a ISO-8859-1
	 * charset, used when interpreting bytes as text.
	 *
	 * @param inputStream The input stream to read bytes from.
	 * @throws IllegalArgumentException If {@code inputStream} is null.
	 */
	public HybridInputStream(final InputStream inputStream) {
		this(inputStream, StandardCharsets.ISO_8859_1);
	}

	/**
	 * Reads a single line of text, delimited by the carriage return and line feed
	 * ASCII characters, from the buffered input stream associated to this byte
	 * reader. This method will read only one byte at a time from the stream until
	 * the end of line sequence is found. This behavior is different from
	 * {@link InputStreamReader}, who might read more bytes and therefore advance
	 * the underlying stream more positions than strictly necessary. It is also less
	 * lenient than {@link BufferedReader}, which allows more line ending sequences.
	 *
	 * @throws IOException If an I/O error occurred.
	 * @return The line of text read, whose composing bytes are interpreted as
	 *         UTF-8. The resulting string doesn't contain the line delimiting
	 *         sequence. If the line was empty or no characters were read, this
	 *         method returns an empty string. If the end of the stream was reached
	 *         before invoking this method, the string is null. Otherwise, if the
	 *         end of the stream is reached while this method reads bytes, this
	 *         method will return all the characters read until that happened.
	 */
	public String readLine() throws IOException {
		boolean eolReached = false;
		boolean eofReached = false;
		boolean atLeastOneByteRead = false;

		CoderResult coderResult;
		ByteBuffer conversionBuffer = ByteBuffer.allocate(1);
		final CharBuffer outputBuffer = CharBuffer.allocate(1);
		final StringBuilder line = new StringBuilder();
		int lastByteRead = -1;

		charDecoder.reset();

		do {
			// Read a byte if possible
			int byteRead = in.read();

			// According to the byte read, calculate whether we reached
			// EOL or EOF
			eolReached = lastByteRead == '\r' && byteRead == '\n';
			eofReached = byteRead < 0;

			// If the byte did not complete a line delimiter, then append it
			// to the result
			if (!eolReached && !eofReached) {
				// Put the byte read in actual conversion buffer, and flip it
				// so the decoder reads it
				conversionBuffer.put((byte) byteRead);
				conversionBuffer.flip();

				coderResult = charDecoder.decode(conversionBuffer, outputBuffer, false);
				assert !coderResult.isOverflow() :
					"Reading at most 1 byte of input should not result in needing more " +
					"than 1 character of room in a char buffer"
				;

				// Flip the output buffer so we read any content written by the decoder,
				// and append that to the line
				outputBuffer.flip();
				line.append(outputBuffer);

				// Now we clean the buffers to make room for the next input
				outputBuffer.clear();

				// If there are remaining bytes in the input for some reason,
				// allocate a bigger buffer so we can copy the remaining
				// bytes and put the next ones. The decode method contract requires
				// any remaining input bytes to be available on next invocations
				if (conversionBuffer.hasRemaining()) {
					final ByteBuffer newInputBuffer = ByteBuffer.allocate(1 + conversionBuffer.remaining());
					newInputBuffer.put(conversionBuffer);
					conversionBuffer = newInputBuffer;
				} else {
					conversionBuffer.clear();
				}
			}

			atLeastOneByteRead = atLeastOneByteRead || !eofReached;
			lastByteRead = byteRead;
		} while (!eolReached && !eofReached);

		charDecoder.decode(conversionBuffer, outputBuffer, true);

		// Flush any remaining characters
		do {
			coderResult = charDecoder.flush(outputBuffer);
			line.append(outputBuffer);

			// Make more room for trying again
			if (coderResult.isOverflow()) {
				line.append(outputBuffer);
				outputBuffer.clear();
			}
		} while (coderResult.isOverflow());

		// Discard carriage return
		if (eolReached) {
			line.setLength(line.length() - 1);
		}

		return atLeastOneByteRead ? line.toString() : null;
	}
}
