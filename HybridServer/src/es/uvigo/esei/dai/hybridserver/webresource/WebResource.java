package es.uvigo.esei.dai.hybridserver.webresource;

/**
 * Models a web resource, which has associated, runtime dependent and
 * non-persistent optional metadata. Equality comparisons among instances of
 * this class will only take into account the content of the resources.
 *
 * @author Alejandro González García
 */
// In the second iteration, which includes a P2P system, this is the ideal place to
// store the resource origin server, needed to display the resource list :)
public final class WebResource {
	private final String content;

	public WebResource(final String content) {
		if (content == null) {
			throw new IllegalArgumentException("A web resource can't have null content");
		}

		this.content = content;
	}

	/**
	 * Gets the content (payload) of this web resource. It never is null.
	 *
	 * @return The described value.
	 */
	public String getContent() {
		return content;
	}

	@Override
	public String toString() {
		return content;
	}

	@Override
	public boolean equals(final Object other) {
		return other instanceof WebResource
			&& content.equals(((WebResource) other).content);
	}

	@Override
	public int hashCode() {
		return content.hashCode();
	}
}
