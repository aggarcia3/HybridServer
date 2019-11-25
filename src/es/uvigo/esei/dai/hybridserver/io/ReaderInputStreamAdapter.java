package es.uvigo.esei.dai.hybridserver.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/**
 * Adapts a {@code Reader} so it behaves like a {@code InputStream}. To do so,
 * it reads characters from the underlying {@code Reader} into a buffer,
 * converts them to their binary representation in a given character encoding
 * and returns those bytes in sequence.
 * <p>
 * Because readers don't provide an interface to get the character set they're
 * using, as that would go against the abstraction they provide, this conversion
 * process can be lossy if the returned bytes are to be interpreted again as
 * text with a different, non-compatible encoding.
 * <p>
 * This implementation of {@code InputStream} returns {@code false} for
 * {@link markSupported}.
 *
 * @author Alejandro González García
 * @implNote The implementation of this class is not thread safe.
 */
public class ReaderInputStreamAdapter extends InputStream {
	private static final short CHAR_BUFFER_SIZE = 512;

	private final Reader reader;
	private final CharsetEncoder charEncoder;
	private CharBuffer charBuffer = CharBuffer.allocate(CHAR_BUFFER_SIZE);
	private ByteBuffer byteBuffer;

	/**
	 * Creates a new input stream that provides a byte-oriented interface to the
	 * characters of a {@code Reader}.
	 *
	 * @param reader  The reader that will serve as a source of data.
	 * @param charset The character set for encoding the characters returned by the
	 *                {@code Reader}.
	 */
	public ReaderInputStreamAdapter(final Reader reader, final Charset charset) {
		if (reader == null) {
			throw new IllegalArgumentException(
				"The reader associated with a reader input stream adapter can't be null"
			);
		}

		if (charset == null) {
			throw new IllegalArgumentException(
				"The character set associated with a reader input stream adapter can't be null"
			);
		}

		this.reader = reader;

		this.charEncoder = charset.newEncoder()
			.onMalformedInput(CodingErrorAction.REPLACE)
			.onUnmappableCharacter(CodingErrorAction.REPLACE);

		this.byteBuffer = ByteBuffer.allocate(
			(int) Math.ceil(CHAR_BUFFER_SIZE * charEncoder.maxBytesPerChar())
		).limit(0); // Limit 0 so it has no remaining elements on the first read
	}

	/**
	 * Creates a new input stream that provides a byte-oriented interface to the
	 * characters of a {@code Reader}, encoding those characters in the ISO-8859-1
	 * encoding.
	 *
	 * @param reader The reader that will serve as a source of data.
	 */
	public ReaderInputStreamAdapter(final Reader reader) {
		this(reader, StandardCharsets.ISO_8859_1);
	}

	@Override
	public int read() throws IOException {
		if (!byteBuffer.hasRemaining()) {
			fillByteBuffer();
		}

		return byteBuffer.hasRemaining() ? ((int) byteBuffer.get()) & 0xFF : -1;
	}

	@Override
	public void close() throws IOException {
		reader.close();
	}

	@Override
	public int available() {
		return byteBuffer.remaining();
	}

	/**
	 * Reads characters from the underlying reader to try to fill an internal
	 * character buffer, and then converts the characters in that buffer to their
	 * byte representation, storing them in a byte buffer.
	 * <p>
	 * When this method finishes execution, a best effort has been made to fill the buffer,
	 * waiting 
	 * 
	 * @throws IOException
	 */
	private void fillByteBuffer() throws IOException {
		CoderResult result;

		int charactersRead = reader.read(charBuffer);

		// The read should block until one char is available or
		// EOF is detected. Otherwise, we'd report EOF when we really
		// didn't get to EOF
		assert charactersRead != 0 || charBuffer.limit() == 0 :
			"The reader didn't block until a character was read or EOF was detected. " +
			"This possibly violates its contract."
		;

		// Do we have something to fill the buffer with?
		charBuffer.flip();
		byteBuffer.clear();
		if (charactersRead > 0) {
			result = charEncoder.encode(charBuffer, byteBuffer, false);

			assert byteBuffer.position() > 0 :
				"Converting at least one character to bytes should generate at least one byte"
			;

			byteBuffer.flip();

			// We allocated the worst case scenario output buffer before, and the buffer should be empty
			assert !result.isOverflow() :
				"Reading at most " + charBuffer.capacity() + " characters should not result in needing more " +
				"than " + byteBuffer.capacity() + " bytes of room in a byte buffer";

			// "In any case, if this method is to be reinvoked in the same encoding operation then care
			// should be taken to preserve any characters remaining in the input buffer so that they are
			// available to the next invocation."
			if (charBuffer.hasRemaining()) {
				final CharBuffer newCharBuffer = CharBuffer.allocate(CHAR_BUFFER_SIZE + charBuffer.remaining());
				newCharBuffer.put(charBuffer);
				charBuffer = newCharBuffer;
			} else {
				charBuffer.clear();
			}
		} else {
			charEncoder.encode(charBuffer, byteBuffer, true);

			do {
				result = charEncoder.flush(byteBuffer);

				// Make more room for the final bytes
				if (result.isOverflow()) {
					final ByteBuffer newByteBuffer = ByteBuffer.allocate(
						(int) Math.ceil(charEncoder.maxBytesPerChar() + byteBuffer.capacity())
					);
					byteBuffer.flip();
					newByteBuffer.put(byteBuffer);
					byteBuffer = newByteBuffer;
				}
			} while (result.isOverflow());

			// Flip it, so we can read all its bytes later
			byteBuffer.flip();
		}
	}
}
