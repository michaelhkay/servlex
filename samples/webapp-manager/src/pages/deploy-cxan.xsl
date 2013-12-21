<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:pkg="http://expath.org/ns/pkg"
                xmlns:web="http://expath.org/ns/webapp"
                xmlns:hc="http://expath.org/ns/http-client"
                xmlns:app="http://servlex.net/ns/webapp-manager"
                exclude-result-prefixes="#all"
                version="2.0">

   <xsl:import href="http://expath.org/ns/http-client.xsl"/>
   <xsl:import href="deploy-lib.xsl"/>

   <pkg:import-uri>http://servlex.net/app/manager/pages/deploy-cxan.xsl</pkg:import-uri>

   <xsl:param name="web:input" required="yes"/>

   <xsl:template match="document-node()">
      <xsl:message terminate="yes">
         <xsl:text>Unexpected document?!?: </xsl:text>
         <xsl:value-of select="name(*)"/>
         <xsl:text> (expect a web:request).</xsl:text>
      </xsl:message>
   </xsl:template>

   <xsl:template match="document-node(element(web:request))">
      <xsl:apply-templates/>
   </xsl:template>

   <xsl:template match="web:request">
      <xsl:variable name="repo"                    select="web:repository()"/>
      <xsl:variable name="server"  as="xs:string?" select="web:param[@name eq 'server']/@value/string(.)[.]"/>
      <xsl:variable name="id"      as="xs:string?" select="web:param[@name eq 'id']/@value/string(.)[.]"/>
      <xsl:variable name="name"    as="xs:string?" select="web:param[@name eq 'name']/@value/string(.)[.]"/>
      <xsl:variable name="version" as="xs:string?" select="web:param[@name eq 'version']/@value/string(.)[.]"/>
      <page menu="deploy">
         <title>Deploy from CXAN</title>
         <xsl:choose>
            <xsl:when test="not(web:install-enabled($repo))">
               <xsl:sequence select="
                   error(
                     xs:QName('app:not-implemented'),
                     'Install not supported, storage is read-only')"/>
            </xsl:when>
            <xsl:when test="empty($server)">
               <xsl:sequence select="
                   error(
                     xs:QName('app:missing-param'),
                     'The CXAN server to use has not been provided (param ''server'')')"/>
            </xsl:when>
            <xsl:when test="not($server = ('prod', 'sandbox'))">
               <xsl:sequence select="
                   error(
                     xs:QName('app:bad-request'),
                     concat('The CXAN server to use must be either ''prod'' or ''sandbox'', but is ''', $server, '''.'))"/>
            </xsl:when>
            <xsl:when test="empty($id) and empty($name)">
               <xsl:sequence select="
                   error(
                     xs:QName('app:bad-request'),
                     'Neither CXAN ID or package name provided, at least one is required.')"/>
            </xsl:when>
            <xsl:when test="exists($id) and exists($name)">
               <xsl:sequence select="
                   error(
                     xs:QName('app:bad-request'),
                     concat('Both CXAN ID and package name provided: resp. ''', $id, ''' and ''', $name, '''.'))"/>
            </xsl:when>
            <xsl:otherwise>
               <xsl:apply-templates select="." mode="cxan">
                  <xsl:with-param name="repo"    select="$repo"/>
                  <xsl:with-param name="server"  select="$server"/>
                  <xsl:with-param name="id"      select="$id"/>
                  <xsl:with-param name="name"    select="$name"/>
                  <xsl:with-param name="version" select="$version"/>
               </xsl:apply-templates>
            </xsl:otherwise>
         </xsl:choose>
      </page>
   </xsl:template>

   <xsl:template match="web:request" mode="cxan">
      <xsl:param name="repo"    required="yes"/>
      <xsl:param name="server"  required="yes" as="xs:string"/>
      <xsl:param name="id"      required="yes" as="xs:string?"/>
      <xsl:param name="name"    required="yes" as="xs:string?"/>
      <xsl:param name="version" required="yes" as="xs:string?"/>
      <xsl:variable name="url" as="xs:string">
         <xsl:value-of>
            <xsl:text>http://</xsl:text>
            <xsl:if test="$server eq 'sandbox'">
               <xsl:text>test.</xsl:text>
            </xsl:if>
            <xsl:text>cxan.org/file?</xsl:text>
            <xsl:choose>
               <xsl:when test="exists($id)">
                  <xsl:text>id=</xsl:text>
                  <xsl:value-of select="encode-for-uri($id)"/>
               </xsl:when>
               <xsl:otherwise>
                  <xsl:text>name=</xsl:text>
                  <xsl:value-of select="encode-for-uri($name)"/>
               </xsl:otherwise>
            </xsl:choose>
            <xsl:if test="exists($version)">
               <xsl:text>&amp;version=</xsl:text>
               <xsl:value-of select="encode-for-uri($version)"/>
            </xsl:if>
         </xsl:value-of>
      </xsl:variable>
      <xsl:variable name="cxan-req" as="element()">
         <hc:request href="{ $url }" method="get"/>
      </xsl:variable>
      <xsl:variable name="cxan-resp" select="hc:send-request($cxan-req)"/>
      <xsl:choose>
         <xsl:when test="not($cxan-resp[1]/@status/xs:integer(.) eq 200)">
            <xsl:sequence select="
                error(
                  xs:QName('app:http-error'),
                  concat(
                     'HTTP return code is not 200 for ', $url, ': ',
                     $cxan-resp[1]/@status, ' ', $cxan-resp[1]/@message))"/>
         </xsl:when>
         <xsl:when test="empty($cxan-resp[2])">
            <xsl:sequence select="
                error(
                  xs:QName('app:http-error'),
                  concat('CXAN response does not contain body for ', $url))"/>
         </xsl:when>
         <xsl:when test="exists($cxan-resp[3])">
            <xsl:sequence select="
                error(
                  xs:QName('app:http-error'),
                  concat('CXAN response contains more than one body for ', $url))"/>
         </xsl:when>
         <xsl:when test="not($cxan-resp[2] instance of xs:base64Binary)">
            <xsl:sequence select="
                error(
                  xs:QName('app:http-error'),
                  concat(
                     'CXAN response content is not binary for: ', $url, ', type is: ',
                     $cxan-resp[1]/hc:header[@name eq 'content-type']/@value))"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:apply-templates select="." mode="install">
               <xsl:with-param name="repo" select="$repo"/>
               <xsl:with-param name="xar"  select="$cxan-resp[2]"/>
            </xsl:apply-templates>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>

</xsl:stylesheet>
