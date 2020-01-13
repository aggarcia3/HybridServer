<?xml version="1.0" encoding="UTF-8"?>
<!-- XML Stylesheet for displaying Hybrid Server configuration files, by Alejandro González García -->
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:tns="http://www.esei.uvigo.es/dai/hybridserver"
	exclude-result-prefixes="tns"
>
	<!-- HTML output (web page) -->
	<xsl:output method="html" encoding="UTF-8" indent="yes" media-type="text/html"/>

	<xsl:variable name="pageTitle">Hybrid Server configuration file</xsl:variable>

	<!-- General document layout -->
	<xsl:template match="/">
		<!-- Renders as text with Firefox 71 builtin XSLT processor -->
		<!-- <xsl:text disable-output-escaping="yes">&lt;!DOCTYPE html&gt;</xsl:text> -->

		<title><xsl:value-of select="$pageTitle"/></title>

		<meta charset="UTF-8"/>

		<style>
			* {
				text-align: center;
				font-family: serif;
			}

			h1, h2 {
				font-family: sans-serif;
			}

			dt, dd {
				margin: 0 0 1em 0;
			}

			dt {
				font-size: large;
				font-style: italic;
			}

			dd {
				font-style: normal;
				font-weight: bold;
			}

			pre {
				font-family: monospace;
			}
		</style>

		<h1><xsl:value-of select="$pageTitle"/></h1>

		<hr/>

		<h2>Connections settings</h2>
		<xsl:apply-templates select="tns:configuration/tns:connections"/>

		<hr/>

		<h2>Database settings</h2>
		<xsl:apply-templates select="tns:configuration/tns:database"/>

		<xsl:variable name="swarmServers" select="tns:configuration/tns:servers"/>
		<xsl:choose>
			<xsl:when test="count($swarmServers) &gt; 0">
				<hr/>

				<h2><xsl:value-of select="count($swarmServers/tns:server)"/> Hybrid Servers in the swarm</h2>
				<xsl:apply-templates select="$swarmServers"/>
			</xsl:when>
			<xsl:otherwise>
				<h4>No other Hybrid Servers were defined in the swarm. P2P networking will be disabled.</h4>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<!-- Connections settings layout -->
	<xsl:template match="tns:connections">
		<dl>
			<dt>Port to listen for incoming HTTP web resource requests</dt>
			<dd><pre><xsl:value-of select="tns:http"/></pre></dd>

			<dt>URL to listen for incoming HTTP SOAP web service requests</dt>
			<dd><pre><xsl:value-of select="tns:webservice"/></pre></dd>

			<dt>Maximum worker threads</dt>
			<dd><pre><xsl:value-of select="tns:numClients"/></pre></dd>
		</dl>
	</xsl:template>

	<!-- Database settings layout -->
	<xsl:template match="tns:database">
		<dl>
			<dt>Username</dt>
			<dd><pre><xsl:value-of select="tns:user"/></pre></dd>

			<dt>Password</dt>
			<dd>
				<pre id="password"><noscript><xsl:value-of select="tns:password"/></noscript></pre>
				<script>
					var actualPassword = "<xsl:value-of select="tns:password"/>";
					var maskedPassword = "*".repeat(actualPassword.length);
					var passwordElement = document.getElementById("password");

					// Hide the password
					passwordElement.innerHTML = maskedPassword;

					// Show it when we hover over the element
					function handleHover(event) {
						if (event.type == "pointerover") {
							passwordElement.innerHTML = actualPassword;
						} else {
							passwordElement.innerHTML = maskedPassword;
						}
					}

					passwordElement.addEventListener("pointerover", handleHover);
					passwordElement.addEventListener("pointerout", handleHover);
				</script>
			</dd>

			<dt>JDBC connection URL</dt>
			<dd><pre><xsl:value-of select="tns:url"/></pre></dd>
		</dl>
	</xsl:template>

	<!-- Servers settings layout -->
	<xsl:template match="tns:servers">
		<xsl:for-each select="tns:server">
			<xsl:sort select="@name"/>

			<xsl:apply-templates select="."/>
		</xsl:for-each>
	</xsl:template>

	<!-- Server layout -->
	<xsl:template match="tns:server">
		<h3><xsl:value-of select="@name"/></h3>
		<dl>
			<dt>Web service name</dt>
			<dd><pre><xsl:value-of select="@service"/></pre></dd>

			<dt>Web service namespace</dt>
			<dd><pre><xsl:value-of select="@namespace"/></pre></dd>

			<dt>HTTP URL where the WSDL is available</dt>
			<dd><pre><xsl:value-of select="@wsdl"/></pre></dd>

			<dt>Base HTTP URL for incoming web resource requests</dt>
			<dd><pre><xsl:value-of select="@httpAddress"/></pre></dd>
		</dl>
	</xsl:template>
</xsl:stylesheet>
